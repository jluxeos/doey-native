package com.doey.agent

import android.content.Context

data class CredentialRequirement(
    val id: String,
    val label: String,
    val type: String, // oauth | api_key | password
    val authProvider: String? = null
)

data class SkillInfo(
    val name: String,
    val description: String,
    val category: String,
    val content: String,
    val testPrompt: String? = null,
    val androidPackage: String? = null,
    val exclusiveTool: String? = null,
    val permissions: List<String> = emptyList(),
    val credentials: List<CredentialRequirement> = emptyList()
)

class SkillLoader(private val context: Context) {

    private val skills = mutableMapOf<String, SkillInfo>()
    private val summaryCache = mutableMapOf<String, String>()

    init {
        loadBundledSkills()
        loadCustomSkills()
    }

    private fun loadCustomSkills() {
        val customSkills = SettingsStore(context).getCustomSkills()
        for ((name, content) in customSkills) {
            try {
                val skill = parseSkill(name, content)
                skills[skill.name] = skill
            } catch (e: Exception) {
                // Skip unreadable custom skills
            }
        }
    }

    private fun loadBundledSkills() {
        try {
            val skillDirs = context.assets.list("skills") ?: return
            for (dir in skillDirs) {
                try {
                    val content = context.assets.open("skills/$dir/SKILL.md")
                        .bufferedReader().readText()
                    val skill = parseSkill(dir, content)
                    skills[skill.name] = skill
                } catch (e: Exception) {
                    // Skip unreadable skill files
                }
            }
        } catch (e: Exception) {
            // No skills dir in assets
        }
    }

    private fun parseSkill(key: String, raw: String): SkillInfo {
        val match = Regex("^---\\n([\\s\\S]*?)\\n---\\n([\\s\\S]*)", RegexOption.MULTILINE).find(raw)

        var name = key
        var description = ""
        var category = "other"
        var testPrompt: String? = null
        var androidPackage: String? = null
        var exclusiveTool: String? = null
        val permissions = mutableListOf<String>()
        val credentials = mutableListOf<CredentialRequirement>()
        val body: String

        if (match != null) {
            val yamlBlock = match.groupValues[1]
            body = match.groupValues[2]

            val yamlLines = yamlBlock.lines()
            val yamlMap = parseYamlToMap(yamlLines)

            name = (yamlMap["name"] as? String)?.trimQuotes() ?: key
            description = (yamlMap["description"] as? String)?.trimQuotes() ?: ""
            category = (yamlMap["category"] as? String)?.trimQuotes() ?: "other"
            testPrompt = (yamlMap["test_prompt"] as? String)?.trimQuotes()
            androidPackage = (yamlMap["android_package"] as? String)?.trimQuotes()
            exclusiveTool = (yamlMap["exclusive_tool"] as? String)?.trimQuotes()

            (yamlMap["permissions"] as? List<String>)?.let { permissions.addAll(it) }
            (yamlMap["credentials"] as? List<Map<String, String>>)?.forEach { credMap ->
                credMap.toCredential()?.let { credentials.add(it) }
            }
        } else {
            body = raw
        }

        return SkillInfo(
            name = name,
            description = description,
            category = category,
            content = body,
            testPrompt = testPrompt,
            androidPackage = androidPackage,
            exclusiveTool = exclusiveTool,
            permissions = permissions,
            credentials = credentials
        )
    }

    fun getSkill(name: String): SkillInfo? = skills[name]

    fun getAllSkills(): List<SkillInfo> = skills.values.toList()

    fun buildSkillsSummary(enabledNames: List<String>): String {
        val key = enabledNames.sorted().joinToString(",")
        summaryCache[key]?.let { return it }

        val enabled = enabledNames.mapNotNull { skills[it] }
        if (enabled.isEmpty()) return ""

        val sb = StringBuilder("<skills>\n")
        for (s in enabled) {
            sb.append("  <skill>\n")
            sb.append("    <n>${s.name.escapeXml()}</n>\n")
            sb.append("    <description>${s.description.escapeXml()}</description>\n")
            sb.append("  </skill>\n")
        }
        sb.append("</skills>")
        val result = sb.toString()
        summaryCache[key] = result
        return result
    }

    fun getDisabledExclusiveTools(enabledNames: List<String>): List<String> {
        val enabled = enabledNames.toSet()
        return skills.values
            .filter { it.exclusiveTool != null && it.name !in enabled }
            .mapNotNull { it.exclusiveTool }
    }

    private fun String.trimQuotes() = trim().removeSurrounding("\"").removeSurrounding("'")
    private fun String.escapeXml() = replace("&", "&amp;")
        .replace("<", "&lt;").replace(">", "&gt;")

    private fun parseYamlToMap(lines: List<String>): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val kvMatch = Regex("^\\s*(\\w+):\\s*(.*)$").find(line)
            if (kvMatch != null) {
                val key = kvMatch.groupValues[1]
                val value = kvMatch.groupValues[2].trim()
                if (value.isEmpty()) {
                    // It's a block or array
                    val blockItems = mutableListOf<Any?>()
                    i++
                    while (i < lines.size && lines[i].startsWith(" ")) {
                        val itemLine = lines[i].trim()
                        if (itemLine.startsWith("-")) {
                            // Array item
                            val listItem = itemLine.removePrefix("-").trim()
                            val subKvMatch = Regex("^(\\w+):\\s*(.*)$").find(listItem)
                            if (subKvMatch != null) {
                                // Map within array
                                val subMap = mutableMapOf<String, String>()
                                subMap[subKvMatch.groupValues[1]] = subKvMatch.groupValues[2].trimQuotes()
                                blockItems.add(subMap)
                            } else {
                                blockItems.add(listItem.trimQuotes())
                            }
                        } else {
                            // Simple block item (e.g., multiline string)
                            blockItems.add(itemLine.trimQuotes())
                        }
                        i++
                    }
                    map[key] = blockItems
                    i-- // Adjust index for next iteration
                } else {
                    map[key] = value.trimQuotes()
                }
            }
            i++
        }
        return map
    }

    private fun Map<String, String>.toCredential(): CredentialRequirement? {
        val id = this["id"] ?: return null
        val type = this["type"] ?: "api_key"
        val label = this["label"] ?: id

        return CredentialRequirement(
            id = id,
            label = label,
            type = type,
            authProvider = this["auth_provider"]
        )
    }
}
