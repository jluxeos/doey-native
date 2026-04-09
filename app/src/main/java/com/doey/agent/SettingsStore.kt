package com.doey.agent

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "doey_prefs")

class SettingsStore(private val context: Context) {

    // ── DataStore keys ────────────────────────────────────────────────────────
    private val KEY_PROVIDER       = stringPreferencesKey("provider")
    private val KEY_MODEL          = stringPreferencesKey("model")
    private val KEY_LANGUAGE       = stringPreferencesKey("language")
    private val KEY_DRIVING_MODE   = booleanPreferencesKey("driving_mode")
    private val KEY_WAKE_WORD      = booleanPreferencesKey("wake_word_enabled")
    private val KEY_WAKE_PHRASE    = stringPreferencesKey("wake_phrase")
    private val KEY_STT_MODE       = stringPreferencesKey("stt_mode")
    private val KEY_ENABLED_SKILLS = stringPreferencesKey("enabled_skills")
    private val KEY_MAX_ITERATIONS = intPreferencesKey("max_iterations")
    private val KEY_SOUL           = stringPreferencesKey("soul_md")
    private val KEY_PERSONAL_MEM   = stringPreferencesKey("personal_memory_md")
    private val KEY_EXPERT_MODE          = booleanPreferencesKey("expert_mode")
    private val KEY_MAX_HISTORY           = intPreferencesKey("max_history_messages")
    private val KEY_BUBBLE_POSITION       = stringPreferencesKey("bubble_position")
    private val KEY_THEME                 = stringPreferencesKey("theme")
    private val KEY_FRIENDLY_MODE         = booleanPreferencesKey("friendly_mode_enabled")
    private val KEY_FRIENDLY_CONTEXT      = booleanPreferencesKey("friendly_context_read")
    private val KEY_TOKEN_OPTIMIZER       = booleanPreferencesKey("token_optimizer_enabled")
    private val KEY_PROMPT_CACHE          = booleanPreferencesKey("system_prompt_cache")
    private val KEY_HISTORY_COMPRESSION   = booleanPreferencesKey("history_compression")
    private val KEY_DEBUG_MODE            = booleanPreferencesKey("debug_mode")
    private val KEY_OVERLAY_ENABLED       = booleanPreferencesKey("overlay_enabled")
    private val KEY_NOTIF_ENABLED         = booleanPreferencesKey("notif_enabled")
    private val KEY_AUTO_START_FRIENDLY   = booleanPreferencesKey("auto_start_friendly")
    private val KEY_FRIENDLY_BAR_HEIGHT   = floatPreferencesKey("friendly_bar_height")
    private val KEY_FRIENDLY_BAR_OPACITY  = floatPreferencesKey("friendly_bar_opacity")

    // ── Encrypted SharedPreferences (API keys, secrets) ───────────────────────
    private val encPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "doey_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ── Secure getters/setters ────────────────────────────────────────────────
    fun getApiKey(provider: String): String =
        encPrefs.getString("apikey_$provider", "") ?: ""

    fun setApiKey(provider: String, key: String) =
        encPrefs.edit().putString("apikey_$provider", key).apply()

    fun getCredential(id: String): String =
        encPrefs.getString("cred_$id", "") ?: ""

    fun setCredential(id: String, value: String) =
        encPrefs.edit().putString("cred_$id", value).apply()

    fun clearCredential(id: String) =
        encPrefs.edit().remove("cred_$id").apply()

    fun getCustomModelUrl(): String =
        encPrefs.getString("custom_model_url", "") ?: ""

    fun setCustomModelUrl(url: String) =
        encPrefs.edit().putString("custom_model_url", url).apply()

    // ── DataStore flows ───────────────────────────────────────────────────────
    val provider: Flow<String>         = context.dataStore.data.map { it[KEY_PROVIDER] ?: "openrouter" }
    val model: Flow<String>            = context.dataStore.data.map { it[KEY_MODEL] ?: "openrouter/auto" }
    val language: Flow<String>         = context.dataStore.data.map { it[KEY_LANGUAGE] ?: "system" }
    val drivingMode: Flow<Boolean>     = context.dataStore.data.map { it[KEY_DRIVING_MODE] ?: false }
    val wakeWordEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_WAKE_WORD] ?: false }
    val wakePhrase: Flow<String>       = context.dataStore.data.map { it[KEY_WAKE_PHRASE] ?: "hey doey" }
    val sttMode: Flow<String>          = context.dataStore.data.map { it[KEY_STT_MODE] ?: "auto" }
    val enabledSkills: Flow<String>    = context.dataStore.data.map { it[KEY_ENABLED_SKILLS] ?: "" }
    val maxIterations: Flow<Int>       = context.dataStore.data.map { it[KEY_MAX_ITERATIONS] ?: 10 }
    val soul: Flow<String>             = context.dataStore.data.map { it[KEY_SOUL] ?: "" }
    val personalMemory: Flow<String>   = context.dataStore.data.map { it[KEY_PERSONAL_MEM] ?: "" }
    val expertMode: Flow<Boolean>      = context.dataStore.data.map { it[KEY_EXPERT_MODE] ?: false }
    val debugMode: Flow<Boolean>       = context.dataStore.data.map { it[KEY_DEBUG_MODE] ?: false }

    // ── DataStore setters ─────────────────────────────────────────────────────
    suspend fun setProvider(v: String)          = context.dataStore.edit { it[KEY_PROVIDER] = v }
    suspend fun setModel(v: String)             = context.dataStore.edit { it[KEY_MODEL] = v }
    suspend fun setLanguage(v: String)          = context.dataStore.edit { it[KEY_LANGUAGE] = v }
    suspend fun setDrivingMode(v: Boolean)      = context.dataStore.edit { it[KEY_DRIVING_MODE] = v }
    suspend fun setWakeWordEnabled(v: Boolean)  = context.dataStore.edit { it[KEY_WAKE_WORD] = v }
    suspend fun setWakePhrase(v: String)        = context.dataStore.edit { it[KEY_WAKE_PHRASE] = v }
    suspend fun setSttMode(v: String)           = context.dataStore.edit { it[KEY_STT_MODE] = v }
    suspend fun setEnabledSkills(v: String)     = context.dataStore.edit { it[KEY_ENABLED_SKILLS] = v }
    suspend fun setMaxIterations(v: Int)        = context.dataStore.edit { it[KEY_MAX_ITERATIONS] = v }
    suspend fun setSoul(v: String)              = context.dataStore.edit { it[KEY_SOUL] = v }
    suspend fun setPersonalMemory(v: String)    = context.dataStore.edit { it[KEY_PERSONAL_MEM] = v }
    suspend fun setExpertMode(v: Boolean)               = context.dataStore.edit { it[KEY_EXPERT_MODE] = v }
    suspend fun setMaxHistoryMessages(v: Int)           = context.dataStore.edit { it[KEY_MAX_HISTORY] = v }
    suspend fun setBubblePosition(v: String)            = context.dataStore.edit { it[KEY_BUBBLE_POSITION] = v }
    suspend fun setTheme(v: String)                     = context.dataStore.edit { it[KEY_THEME] = v }
    suspend fun setFriendlyModeEnabled(v: Boolean)      = context.dataStore.edit { it[KEY_FRIENDLY_MODE] = v }
    suspend fun setFriendlyContextRead(v: Boolean)      = context.dataStore.edit { it[KEY_FRIENDLY_CONTEXT] = v }
    suspend fun setTokenOptimizerEnabled(v: Boolean)    = context.dataStore.edit { it[KEY_TOKEN_OPTIMIZER] = v }
    suspend fun setSystemPromptCacheEnabled(v: Boolean) = context.dataStore.edit { it[KEY_PROMPT_CACHE] = v }
    suspend fun setHistoryCompressionEnabled(v: Boolean)= context.dataStore.edit { it[KEY_HISTORY_COMPRESSION] = v }
    suspend fun setDebugMode(v: Boolean)                = context.dataStore.edit { it[KEY_DEBUG_MODE] = v }
    suspend fun setOverlayEnabled(v: Boolean)           = context.dataStore.edit { it[KEY_OVERLAY_ENABLED] = v }
    suspend fun setNotifEnabled(v: Boolean)             = context.dataStore.edit { it[KEY_NOTIF_ENABLED] = v }
    suspend fun setAutoStartFriendly(v: Boolean)        = context.dataStore.edit { it[KEY_AUTO_START_FRIENDLY] = v }
    suspend fun setFriendlyBarHeight(v: Float)          = context.dataStore.edit { it[KEY_FRIENDLY_BAR_HEIGHT] = v }
    suspend fun setFriendlyBarOpacity(v: Float)         = context.dataStore.edit { it[KEY_FRIENDLY_BAR_OPACITY] = v }

    // ── Suspend getters (first emission) ──────────────────────────────────────
    suspend fun getProvider()        = provider.first()
    suspend fun getModel()           = model.first()
    suspend fun getLanguage()        = language.first()
    suspend fun getDrivingMode()     = drivingMode.first()
    suspend fun getWakeWordEnabled() = wakeWordEnabled.first()
    suspend fun getWakePhrase()      = wakePhrase.first()
    suspend fun getSttMode()         = sttMode.first()
    suspend fun getSoul()            = soul.first()
    suspend fun getPersonalMemory()  = personalMemory.first()
    suspend fun getMaxIterations()   = maxIterations.first()
    suspend fun getExpertMode()      = expertMode.first()

    suspend fun getMaxHistoryMessages()         = context.dataStore.data.map { it[KEY_MAX_HISTORY] ?: 20 }.first()
    suspend fun getBubblePosition()             = context.dataStore.data.map { it[KEY_BUBBLE_POSITION] ?: "right" }.first()
    suspend fun getTheme()                      = context.dataStore.data.map { it[KEY_THEME] ?: "tau" }.first()
    suspend fun getFriendlyModeEnabled()        = context.dataStore.data.map { it[KEY_FRIENDLY_MODE] ?: true }.first()
    suspend fun getFriendlyContextRead()        = context.dataStore.data.map { it[KEY_FRIENDLY_CONTEXT] ?: true }.first()
    suspend fun getTokenOptimizerEnabled()      = context.dataStore.data.map { it[KEY_TOKEN_OPTIMIZER] ?: true }.first()
    suspend fun getSystemPromptCacheEnabled()   = context.dataStore.data.map { it[KEY_PROMPT_CACHE] ?: true }.first()
    suspend fun getHistoryCompressionEnabled()  = context.dataStore.data.map { it[KEY_HISTORY_COMPRESSION] ?: true }.first()
    suspend fun getDebugMode()                  = context.dataStore.data.map { it[KEY_DEBUG_MODE] ?: false }.first()
    suspend fun getOverlayEnabled()             = context.dataStore.data.map { it[KEY_OVERLAY_ENABLED] ?: false }.first()
    suspend fun getNotifEnabled()               = context.dataStore.data.map { it[KEY_NOTIF_ENABLED] ?: false }.first()
    suspend fun getAutoStartFriendly()          = context.dataStore.data.map { it[KEY_AUTO_START_FRIENDLY] ?: false }.first()
    suspend fun getFriendlyBarHeight()          = context.dataStore.data.map { it[KEY_FRIENDLY_BAR_HEIGHT] ?: 72f }.first()
    suspend fun getFriendlyBarOpacity()         = context.dataStore.data.map { it[KEY_FRIENDLY_BAR_OPACITY] ?: 0.95f }.first()

    suspend fun getEnabledSkillsList(): List<String> =
        enabledSkills.first().let { raw ->
            if (raw.isBlank()) emptyList()
            else raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
        }

    // ── Custom Skills ─────────────────────────────────────────────────────────
    suspend fun saveCustomSkill(name: String, content: String) {
        val prefs = context.getSharedPreferences("custom_skills", Context.MODE_PRIVATE)
        prefs.edit().putString(name, content).apply()
        
        // Add to enabled skills if not already there
        val currentEnabled = getEnabledSkillsList().toMutableList()
        if (!currentEnabled.contains(name)) {
            currentEnabled.add(name)
            setEnabledSkills(currentEnabled.joinToString(","))
        }
    }
    
    fun getCustomSkills(): Map<String, String> {
        val prefs = context.getSharedPreferences("custom_skills", Context.MODE_PRIVATE)
        val skills = mutableMapOf<String, String>()
        prefs.all.forEach { (key, value) ->
            if (value is String) {
                skills[key] = value
            }
        }
        return skills
    }
}
