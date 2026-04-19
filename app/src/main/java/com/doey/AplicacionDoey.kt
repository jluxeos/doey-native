package com.doey

import android.app.Application
import com.doey.NEXUS.DoeyLogger
import com.doey.VAULT.SettingsStore
import com.doey.NEXUS.SkillLoader
import com.doey.ECHO.DoeyTTSEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AplicacionDoey : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var skillLoader: SkillLoader
    lateinit var settingsStore: SettingsStore

    override fun onCreate() {
        super.onCreate()
        instance = this
        CrashHandler.init(this)
        DoeyLogger.init(this)   // carga historial de log desde disco
        skillLoader = SkillLoader(this)
        settingsStore = SettingsStore(this)
        DoeyTTSEngine.init(this)
    }

    companion object {
        lateinit var instance: AplicacionDoey
            private set
    }
}
