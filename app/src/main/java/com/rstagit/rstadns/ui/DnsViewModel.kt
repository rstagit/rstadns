package com.rstagit.rstadns.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rstagit.rstadns.data.repository.DnsRepository
import com.rstagit.rstadns.data.service.DnsPingService
import com.rstagit.rstadns.domain.model.AppEvent
import com.rstagit.rstadns.domain.model.ConnectionStatus
import com.rstagit.rstadns.domain.model.DnsCategory
import com.rstagit.rstadns.domain.model.DnsServer
import com.rstagit.rstadns.domain.model.PingState
import com.rstagit.rstadns.domain.model.PingStatus
import com.rstagit.rstadns.domain.usecase.PingServersUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DnsUiState(
    val selectedCategory: DnsCategory? = null,
    val searchQuery: String = "",
    val showFavoritesOnly: Boolean = false,
    val sortByPing: Boolean = false,
    val showAddCustomDialog: Boolean = false,
    val pingState: PingState = PingState(),
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val connectedServer: DnsServer? = null,
    val activeTab: Int = 0
)

class DnsViewModel(
    private val repository: DnsRepository,
    private val pingUseCase: PingServersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DnsUiState())
    val uiState: StateFlow<DnsUiState> = _uiState.asStateFlow()

    private val _events = Channel<AppEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val allServers: StateFlow<List<DnsServer>> = combine(
        repository.servers,
        repository.favorites,
        _uiState
    ) { servers, favorites, state ->
        var filtered = servers.map { it.copy(isFavorite = favorites.contains(it.id)) }

        if (state.showFavoritesOnly) {
            filtered = filtered.filter { it.isFavorite }
        }

        if (state.selectedCategory != null) {
            filtered = filtered.filter { it.category == state.selectedCategory }
        }

        if (state.searchQuery.isNotBlank()) {
            val query = state.searchQuery.lowercase()
            filtered = filtered.filter {
                it.name.lowercase().contains(query) ||
                it.primaryIp.contains(query) ||
                it.secondaryIp.contains(query)
            }
        }

        if (state.sortByPing) {
            filtered = filtered.sortedWith(
                compareBy(
                    { if (it.averagePing < 0) Long.MAX_VALUE else it.averagePing },
                    { it.name }
                )
            )
        }

        filtered
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setCategory(category: DnsCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleFavoritesOnly() {
        _uiState.update { it.copy(showFavoritesOnly = !it.showFavoritesOnly) }
    }

    fun toggleSortByPing() {
        _uiState.update { it.copy(sortByPing = !it.sortByPing) }
    }

    fun toggleAddCustomDialog() {
        _uiState.update { it.copy(showAddCustomDialog = !it.showAddCustomDialog) }
    }

    fun setActiveTab(tab: Int) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun toggleFavorite(serverId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(serverId)
        }
    }

    fun pingAll() {
        val servers = allServers.value
        if (servers.isEmpty() || _uiState.value.pingState.isRunning) return

        viewModelScope.launch {
            repository.resetPings()
            _uiState.update { it.copy(pingState = PingState(isRunning = true, progress = 0f)) }

            var completed = 0
            pingUseCase.pingAll(servers).collect { result ->
                completed++
                _uiState.update {
                    it.copy(
                        pingState = PingState(
                            isRunning = true,
                            progress = completed.toFloat() / servers.size,
                            currentServer = servers.find { s -> s.id == result.serverId }?.name
                        )
                    )
                }
            }

            _uiState.update { it.copy(pingState = PingState(isRunning = false, progress = 1f), sortByPing = true) }
            _events.send(AppEvent.ShowSnackbar("پینگ تمام شد دسته بندی بر اساس پینگ"))
        }
    }

    fun pingSingle(server: DnsServer) {
        viewModelScope.launch {
            pingUseCase.pingSingle(server).collect { }
        }
    }

    fun connectToDns(server: DnsServer) {
        if (_uiState.value.connectionStatus == ConnectionStatus.CONNECTING) return

        viewModelScope.launch {
            _uiState.update { it.copy(connectionStatus = ConnectionStatus.CONNECTING) }
            delay(1800)
            _uiState.update {
                it.copy(
                    connectionStatus = ConnectionStatus.CONNECTED,
                    connectedServer = server
                )
            }
            _events.send(AppEvent.ShowSnackbar("Connected to ${server.name}"))
        }
    }

    fun disconnectDns() {
        viewModelScope.launch {
            _uiState.update { it.copy(connectionStatus = ConnectionStatus.CONNECTING) }
            delay(800)
            _uiState.update {
                it.copy(
                    connectionStatus = ConnectionStatus.DISCONNECTED,
                    connectedServer = null
                )
            }
            _events.send(AppEvent.ShowSnackbar("سرویس قطع شد"))
        }
    }

    fun addCustomServer(name: String, primary: String, secondary: String) {
        val success = repository.addCustomServer(name, primary, secondary)
        viewModelScope.launch {
            if (success) {
                _uiState.update { it.copy(showAddCustomDialog = false) }
                _events.send(AppEvent.ShowSnackbar("Custom DNS added successfully."))
            } else {
                _events.send(AppEvent.ShowSnackbar("Please enter a valid primary IP address."))
            }
        }
    }

    fun removeCustomServer(serverId: String) {
        repository.removeCustomServer(serverId)
        viewModelScope.launch {
            _events.send(AppEvent.ShowSnackbar("Custom DNS removed."))
        }
    }

    fun copyToClipboard(context: Context, server: DnsServer) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = if (server.secondaryIp.isNotBlank()) "${server.primaryIp}, ${server.secondaryIp}" else server.primaryIp
        val clip = ClipData.newPlainText("DNS", text)
        clipboard.setPrimaryClip(clip)
        viewModelScope.launch {
            _events.send(AppEvent.ShowSnackbar("DNS copied: $text"))
        }
    }

    fun getBestServer(): DnsServer? {
        return allServers.value
            .filter { it.pingStatus == PingStatus.EXCELLENT || it.pingStatus == PingStatus.GOOD }
            .minByOrNull { it.averagePing }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repository = DnsRepository(context)
            val pingService = DnsPingService()
            val pingUseCase = PingServersUseCase(repository, pingService)
            @Suppress("UNCHECKED_CAST")
            return DnsViewModel(repository, pingUseCase) as T
        }
    }
}
