package com.rstagit.rstadns.domain.model

data class PingResult(
    val serverId: String,
    val primaryPing: Long,
    val secondaryPing: Long,
    val timestamp: Long = System.currentTimeMillis()
)

data class PingState(
    val isRunning: Boolean = false,
    val progress: Float = 0f,
    val currentServer: String? = null
)

sealed class AppEvent {
    data class ShowSnackbar(val message: String) : AppEvent()
    data class PingComplete(val results: List<PingResult>) : AppEvent()
}
