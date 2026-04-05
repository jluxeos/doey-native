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
        val match = Regex("^---\\n([\\s\\S]*?)\\n---\\n?([\\s\\S]*)\$").find(raw)

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

            val lines = yamlBlock.lines()
            var i = 0
            while (i < lines.size) {
                val line = lines[i]
                val kv = Regex("^(\\w+):\\s*(.*)$").find(line)
                if (kv != null) {
                    val k = kv.groupValues[1]
                    val v = kv.groupValues[2].trim().trimQuotes()
                    when {
                        v.isEmpty() -> {
                            // Block / array
                            val items = mutableListOf<String>()
                            i++
                            while (i < lines.size &&
                                (lines[i].startsWith(" ") || lines[i].startsWith("\t"))) {
                                items.add(lines[i].trim().removePrefix("- "))
                                i++
                            }
                            when (k) {
                                "permissions" -> permissions.addAll(items)
                                "credentials" -> {
                                    var cur = mutableMapOf<String, String>()
                                    for (item in items) {
                                        val m2 = Regex("^(\\w+):\\s*(.+)$").find(item)
                                        if (m2 != null) {
                                            val ck = m2.groupValues[1]
                                            val cv = m2.groupValues[2].trimQuotes()
                                            if (ck == "id" && cur.isNotEmpty()) {
                                                cur.toCredential()?.let { credentials.add(it) }
                                                cur = mutableMapOf()
                                            }
                                            cur[ck] = cv
                                        }
                                    }
                                    cur.toCredential()?.let { credentials.add(it) }
                                }
                            }
                            continue
                        }
                        else -> when (k) {
                            "name" -> name = v
                            "description" -> description = v
                            "category" -> category = v
                            "test_prompt" -> testPrompt = v
                            "android_package" -> androidPackage = v
                            "exclusive_tool" -> exclusiveTool = v
                        }
                    }
                }
                i++
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

    private fun Map<String, String>.toCredential(): CredentialRequirement? {
        val id = this["id"] ?: return null
        return CredentialRequirement(
            id = id,
            label = this["label"] ?: id,
            type = this["type"] ?: "api_key",
            authProvider = this["auth_provider"]
        )
    }
}
