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
    private val KEY_STT_MODE       = stringPreferencesKey("stt_mode")
    private val KEY_ENABLED_SKILLS = stringPreferencesKey("enabled_skills")
    private val KEY_MAX_ITERATIONS = intPreferencesKey("max_iterations")
    private val KEY_SOUL           = stringPreferencesKey("soul_md")
    private val KEY_PERSONAL_MEM   = stringPreferencesKey("personal_memory_md")
    private val KEY_EXPERT_MODE    = booleanPreferencesKey("expert_mode")

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

    fun getPicovoiceKey(): String =
        encPrefs.getString("picovoice_key", "") ?: ""

    fun setPicovoiceKey(key: String) =
        encPrefs.edit().putString("picovoice_key", key).apply()

    fun getCustomModelUrl(): String =
        encPrefs.getString("custom_model_url", "") ?: ""

    fun setCustomModelUrl(url: String) =
        encPrefs.edit().putString("custom_model_url", url).apply()

    // ── DataStore flows ───────────────────────────────────────────────────────
    val provider: Flow<String>       = context.dataStore.data.map { it[KEY_PROVIDER] ?: "gemini" }
    val model: Flow<String>          = context.dataStore.data.map { it[KEY_MODEL] ?: "gemini-2.5-flash-preview-04-17" }
    val language: Flow<String>       = context.dataStore.data.map { it[KEY_LANGUAGE] ?: "system" }
    val drivingMode: Flow<Boolean>   = context.dataStore.data.map { it[KEY_DRIVING_MODE] ?: false }
    val wakeWordEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_WAKE_WORD] ?: false }
    val sttMode: Flow<String>        = context.dataStore.data.map { it[KEY_STT_MODE] ?: "auto" }
    val enabledSkills: Flow<String>  = context.dataStore.data.map { it[KEY_ENABLED_SKILLS] ?: "" }
    val maxIterations: Flow<Int>     = context.dataStore.data.map { it[KEY_MAX_ITERATIONS] ?: 10 }
    val soul: Flow<String>           = context.dataStore.data.map { it[KEY_SOUL] ?: "" }
    val personalMemory: Flow<String> = context.dataStore.data.map { it[KEY_PERSONAL_MEM] ?: "" }
    val expertMode: Flow<Boolean>    = context.dataStore.data.map { it[KEY_EXPERT_MODE] ?: false }

    // ── DataStore setters ─────────────────────────────────────────────────────
    suspend fun setProvider(v: String)       = context.dataStore.edit { it[KEY_PROVIDER] = v }
    suspend fun setModel(v: String)          = context.dataStore.edit { it[KEY_MODEL] = v }
    suspend fun setLanguage(v: String)       = context.dataStore.edit { it[KEY_LANGUAGE] = v }
    suspend fun setDrivingMode(v: Boolean)   = context.dataStore.edit { it[KEY_DRIVING_MODE] = v }
    suspend fun setWakeWordEnabled(v: Boolean) = context.dataStore.edit { it[KEY_WAKE_WORD] = v }
    suspend fun setSttMode(v: String)        = context.dataStore.edit { it[KEY_STT_MODE] = v }
    suspend fun setEnabledSkills(v: String)  = context.dataStore.edit { it[KEY_ENABLED_SKILLS] = v }
    suspend fun setMaxIterations(v: Int)     = context.dataStore.edit { it[KEY_MAX_ITERATIONS] = v }
    suspend fun setSoul(v: String)           = context.dataStore.edit { it[KEY_SOUL] = v }
    suspend fun setPersonalMemory(v: String) = context.dataStore.edit { it[KEY_PERSONAL_MEM] = v }
    suspend fun setExpertMode(v: Boolean)    = context.dataStore.edit { it[KEY_EXPERT_MODE] = v }

    // ── Suspend getters (first emission) ──────────────────────────────────────
    suspend fun getProvider()        = provider.first()
    suspend fun getModel()           = model.first()
    suspend fun getLanguage()        = language.first()
    suspend fun getDrivingMode()     = drivingMode.first()
    suspend fun getWakeWordEnabled() = wakeWordEnabled.first()
    suspend fun getSttMode()         = sttMode.first()
    suspend fun getSoul()            = soul.first()
    suspend fun getPersonalMemory()  = personalMemory.first()
    suspend fun getMaxIterations()   = maxIterations.first()
    suspend fun getExpertMode()      = expertMode.first()

    suspend fun getEnabledSkillsList(): List<String> =
        enabledSkills.first().let { raw ->
            if (raw.isBlank()) emptyList()
            else raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
        }
}