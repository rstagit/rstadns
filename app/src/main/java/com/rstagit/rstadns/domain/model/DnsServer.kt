package com.rstagit.rstadns.domain.model

data class DnsServer(
    val id: String,
    val name: String,
    val primaryIp: String,
    val secondaryIp: String,
    val category: DnsCategory,
    val isPinged: Boolean = false,
    val primaryPing: Long = -1,
    val secondaryPing: Long = -1,
    val isCustom: Boolean = false,
    val isFavorite: Boolean = false
) {
    val averagePing: Long
        get() {
            val pings = listOfNotNull(
                primaryPing.takeIf { it >= 0 },
                secondaryPing.takeIf { it >= 0 }
            )
            return if (pings.isEmpty()) -1L else pings.average().toLong()
        }

    val pingStatus: PingStatus
        get() = when {
            !isPinged -> PingStatus.UNKNOWN
            averagePing < 0 -> PingStatus.FAILED
            averagePing < 50 -> PingStatus.EXCELLENT
            averagePing < 100 -> PingStatus.GOOD
            averagePing < 200 -> PingStatus.MODERATE
            else -> PingStatus.POOR
        }
}

enum class PingStatus { UNKNOWN, EXCELLENT, GOOD, MODERATE, POOR, FAILED }

enum class DnsCategory(val label: String) {
    DEFAULT("Default"),
    SECURE("Secure"),
    FAMILY("Family"),
    CUSTOM("Custom")
}

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}
