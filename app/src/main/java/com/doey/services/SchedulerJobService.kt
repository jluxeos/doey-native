package com.doey.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.doey.DoeyApplication
import com.doey.agent.ConversationPipeline
import com.doey.llm.LLMProviderFactory
import com.doey.tools.*
import kotlinx.coroutines.*

private const val TAG = "SchedulerJob"

class SchedulerJobService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val scheduleId  = intent?.getStringExtra("schedule_id")  ?: return START_NOT_STICKY
        val instruction = intent.getStringExtra("instruction")   ?: return START_NOT_STICKY

        scope.launch {
            try {
                Log.d(TAG, "Executing: $instruction")
                run(instruction)
            } catch (e: Exception) {
                Log.e(TAG, "Execution failed: ${e.message}")
            } finally {
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun run(instruction: String) {
        val app      = DoeyApplication.instance
        val settings = app.settingsStore

        val provider = settings.getProvider()
        val apiKey   = settings.getApiKey(provider)
        if (apiKey.isBlank()) { Log.w(TAG, "No API key"); return }

        val model         = settings.getModel()
        val customUrl     = settings.getCustomModelUrl()
        val language      = settings.getLanguage()
        val soul          = settings.getSoul()
        val personalMem   = settings.getPersonalMemory()
        val enabledSkills = settings.getEnabledSkillsList()

        val llm     = LLMProviderFactory.create(provider, apiKey, model, customUrl)
        val tools   = ToolRegistry().apply {
            register(IntentTool()); register(SmsTool());   register(BeepTool())
            register(DateTimeTool()); register(DeviceTool()); register(QueryContactsTool())
            register(QuerySmsTool()); register(HttpTool()); register(TTSTool())
            register(AppSearchTool()); register(FileStorageTool())
            register(SkillDetailTool(app.skillLoader))
            register(PersonalMemoryTool()); register(JournalTool())
            register(TimerTool()); register(NotificationListenerTool())

            removeDisabledSkillTools(app.skillLoader.getDisabledExclusiveTools(enabledSkills))
        }

        val pipeline = ConversationPipeline(
            ctx            = app,
            provider       = llm,
            tools          = tools,
            skillLoader    = app.skillLoader,
            language       = language,
            soul           = soul,
            personalMemory = personalMem,
            userName       = com.doey.agent.ProfileStore(app).getUserName(),
            maxIterations  = 10
        )
        pipeline.setEnabledSkills(enabledSkills)

        val result = pipeline.processUtterance(instruction, silent = true)

        if (result.isNotBlank() && !result.contains("__SILENT__")) {
            withContext(Dispatchers.Main) { DoeyTTSEngine.speakAsync(result, language.ifBlank { "en-US" }) }
        }
        Log.d(TAG, "Done: ${result.take(80)}")
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }
    override fun onBind(i: Intent?): IBinder? = null
}
