package com.doey.ui

import com.doey.agent.PipelineState
import com.doey.services.NowPlayingInfo

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class MainUiState(
    val messages: List<ChatMessage> = emptyList(),
    val pipelineState: PipelineState = PipelineState.IDLE,
    val partialSpeech: String = "",
    val errorMessage: String? = null,
    val isDrivingMode: Boolean = false,
    val isWakeWordActive: Boolean = false,
    val isListening: Boolean = false,
    val settingsSaved: Boolean = false,
    val isExpertMode: Boolean = false,
    val nowPlaying: NowPlayingInfo = NowPlayingInfo()
)
