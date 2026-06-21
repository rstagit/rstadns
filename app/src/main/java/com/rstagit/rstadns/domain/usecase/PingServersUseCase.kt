package com.rstagit.rstadns.domain.usecase

import com.rstagit.rstadns.data.repository.DnsRepository
import com.rstagit.rstadns.data.service.DnsPingService
import com.rstagit.rstadns.domain.model.DnsServer
import com.rstagit.rstadns.domain.model.PingResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class PingServersUseCase(
    private val repository: DnsRepository,
    private val pingService: DnsPingService
) {

    fun pingAll(servers: List<DnsServer>): Flow<PingResult> = flow {
        coroutineScope {
            servers.map { server ->
                async {
                    val result = pingService.pingServer(server.id, server.primaryIp, server.secondaryIp)
                    repository.updatePingResults(server.id, result.primaryPing, result.secondaryPing)
                    result
                }
            }.awaitAll().forEach { emit(it) }
        }
    }

    fun pingSingle(server: DnsServer): Flow<PingResult> = flow {
        val result = pingService.pingServer(server.id, server.primaryIp, server.secondaryIp)
        repository.updatePingResults(server.id, result.primaryPing, result.secondaryPing)
        emit(result)
    }
}
