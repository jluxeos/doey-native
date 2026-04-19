package com.doey.NEXUS

import android.content.Context

// ── Skills eliminadas en v4 ───────────────────────────────────────────────────
// Toda la lógica que antes estaba en skills ahora está integrada directamente
// en IRIS (ProcesadorIntencionLocal) o en el system prompt ultra-compacto.
// Este stub mantiene compatibilidad de compilación sin lógica real.

data class SkillInfo(
    val name: String,
    val description: String = "",
    val category: String = "",
    val content: String = "",
    val exclusiveTool: String? = null
)

class SkillLoader(private val context: Context) {
    fun buildSkillsSummary(enabledNames: List<String>): String = ""
    fun getDisabledExclusiveTools(enabledNames: List<String>): List<String> = emptyList()
    fun getSkill(name: String): SkillInfo? = null
    fun getAllSkills(): List<SkillInfo> = emptyList()
    fun getCustomSkills(): Map<String, String> = emptyMap()
}
