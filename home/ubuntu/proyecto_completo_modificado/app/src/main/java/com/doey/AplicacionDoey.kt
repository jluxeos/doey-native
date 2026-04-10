package com.doey

import android.app.Application
import com.doey.agente.AlmacenAjustes
import com.doey.agente.CargadorHabilidades
import com.doey.servicios.comun.MotorTTSDoey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AplicacionDoey : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var skillLoader: CargadorHabilidades
    lateinit var settingsStore: AlmacenAjustes

    override fun onCreate() {
        super.onCreate()
        instance = this
        skillLoader = CargadorHabilidades(this)
        settingsStore = AlmacenAjustes(this)
        MotorTTSDoey.init(this)
    }

    companion object {
        lateinit var instance: AplicacionDoey
            private set
    }
}
