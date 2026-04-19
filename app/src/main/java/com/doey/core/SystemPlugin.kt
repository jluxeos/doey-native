interface SystemPlugin {
    fun initialize(): Boolean
    fun destroy(): Boolean
}