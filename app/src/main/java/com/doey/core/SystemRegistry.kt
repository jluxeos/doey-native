package com.doey.core

class SystemRegistry {
    private val plugins: MutableMap<String, Plugin> = mutableMapOf()

    fun registerPlugin(name: String, plugin: Plugin) {
        plugins[name] = plugin
        println("Plugin $name registered.")
    }

    fun unregisterPlugin(name: String) {
        plugins.remove(name)
        println("Plugin $name unregistered.")
    }

    fun getPlugin(name: String): Plugin? {
        return plugins[name]
    }

    fun listPlugins(): List<String> {
        return plugins.keys.toList()
    }
}

interface Plugin {
    fun start()
    fun stop()
}