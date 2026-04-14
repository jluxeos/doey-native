package com.doey.agente

import android.content.Context
import android.content.SharedPreferences

/**
 * Almacena el perfil de usuario y configuración de rendimiento.
 * Separado de SettingsStore para mayor claridad y acceso rápido.
 */
class ProfileStore(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("doey_profile", Context.MODE_PRIVATE)
    }

    companion object {
        const val KEY_ONBOARDING_DONE = "onboarding_done"
        const val KEY_USER_NAME = "user_name"
        const val KEY_USER_PROFILE = "user_profile"
        const val KEY_PERFORMANCE_MODE = "performance_mode"
        const val KEY_OVERLAY_ENABLED = "overlay_enabled"
        const val KEY_OVERLAY_AUTO_SHOW = "overlay_auto_show"
        const val KEY_ASSISTANT_DEFAULT = "assistant_default_shown"

        const val PROFILE_BASIC = "basic"
        const val PROFILE_ADVANCED = "advanced"

        const val PERF_LOW_POWER = "low_power"
        const val PERF_HIGH = "high_performance"
    }

    fun isOnboardingDone(): Boolean = prefs.getBoolean(KEY_ONBOARDING_DONE, false)

    fun setOnboardingDone(done: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, done).apply()
    }

    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "") ?: ""

    fun setUserName(name: String) {
        prefs.edit().putString(KEY_USER_NAME, name).apply()
    }

    fun getUserProfile(): String = prefs.getString(KEY_USER_PROFILE, PROFILE_BASIC) ?: PROFILE_BASIC

    fun setUserProfile(profile: String) {
        prefs.edit().putString(KEY_USER_PROFILE, profile).apply()
    }

    fun isBasicProfile(): Boolean = getUserProfile() == PROFILE_BASIC

    fun isAdvancedProfile(): Boolean = getUserProfile() == PROFILE_ADVANCED

    fun getPerformanceMode(): String = prefs.getString(KEY_PERFORMANCE_MODE, PERF_LOW_POWER) ?: PERF_LOW_POWER

    fun setPerformanceMode(mode: String) {
        prefs.edit().putString(KEY_PERFORMANCE_MODE, mode).apply()
    }

    fun isLowPowerMode(): Boolean = getPerformanceMode() == PERF_LOW_POWER

    fun isHighPerformanceMode(): Boolean = getPerformanceMode() == PERF_HIGH

    fun isOverlayEnabled(): Boolean = prefs.getBoolean(KEY_OVERLAY_ENABLED, false)

    fun setOverlayEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_OVERLAY_ENABLED, enabled).apply()
    }

    fun isOverlayAutoShow(): Boolean = prefs.getBoolean(KEY_OVERLAY_AUTO_SHOW, true)

    fun setOverlayAutoShow(auto: Boolean) {
        prefs.edit().putBoolean(KEY_OVERLAY_AUTO_SHOW, auto).apply()
    }

    fun hasShownAssistantDefaultPrompt(): Boolean = prefs.getBoolean(KEY_ASSISTANT_DEFAULT, false)

    fun setAssistantDefaultPromptShown() {
        prefs.edit().putBoolean(KEY_ASSISTANT_DEFAULT, true).apply()
    }
}
