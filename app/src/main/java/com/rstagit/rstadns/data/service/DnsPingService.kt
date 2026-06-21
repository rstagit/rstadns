package com.rstagit.rstadns.data.service

import com.rstagit.rstadns.domain.model.PingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class DnsPingService {

    suspend fun pingServer(serverId: String, primaryIp: String, secondaryIp: String): PingResult {
        return withContext(Dispatchers.IO) {
            val primaryPing = measurePing(primaryIp)
            val secondaryPing = if (secondaryIp.isNotBlank()) measurePing(secondaryIp) else -1L
            PingResult(
                serverId = serverId,
                primaryPing = primaryPing,
                secondaryPing = secondaryPing
            )
        }
    }

    private fun measurePing(ip: String): Long {
        if (ip.isBlank()) return -1L
        return try {
            val cleanIp = ip.trim()
            val tcpPing = measureTcpPing(cleanIp, 53)
            if (tcpPing >= 0) return tcpPing
            measureIcmpPing(cleanIp)
        } catch (e: Exception) {
            -1L
        }
    }

    private fun measureTcpPing(ip: String, port: Int): Long {
        return try {
            val start = System.currentTimeMillis()
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), TIMEOUT_MS)
            }
            System.currentTimeMillis() - start
        } catch (e: Exception) {
            -1L
        }
    }

    private fun measureIcmpPing(ip: String): Long {
        return try {
            val start = System.currentTimeMillis()
            val address = InetAddress.getByName(ip)
            val reachable = address.isReachable(TIMEOUT_MS)
            if (reachable) System.currentTimeMillis() - start else -1L
        } catch (e: Exception) {
            -1L
        }
    }

    companion object {
        private const val TIMEOUT_MS = 3000
    }
}
