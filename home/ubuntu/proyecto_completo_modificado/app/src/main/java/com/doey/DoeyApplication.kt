package com.doey

import android.app.Application
import com.doey.agent.SettingsStore
import com.doey.agent.SkillLoader
import com.doey.services.DoeyTTSEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class DoeyApplication : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var skillLoader: SkillLoader
    lateinit var settingsStore: SettingsStore

    override fun onCreate() {
        super.onCreate()
        instance = this
        skillLoader = SkillLoader(this)
        settingsStore = SettingsStore(this)
        DoeyTTSEngine.init(this)
    }

    companion object {
        lateinit var instance: DoeyApplication
            private set
    }
}
