package com.rstagit.rstadns.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rstagit.rstadns.domain.model.DnsCategory
import com.rstagit.rstadns.domain.model.DnsServer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rsta_dns_prefs")

class DnsRepository(private val context: Context) {

    private val FAVORITES_KEY = stringPreferencesKey("favorites")
    private val CUSTOM_DNS_KEY = stringPreferencesKey("custom_dns")

    private val _servers = MutableStateFlow(buildDefaultServers())
    val servers: StateFlow<List<DnsServer>> = _servers.asStateFlow()

    val favorites: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[FAVORITES_KEY]?.split(",")?.toSet() ?: emptySet()
    }

    suspend fun toggleFavorite(serverId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[FAVORITES_KEY]?.split(",")?.filter { it.isNotEmpty() }?.toMutableSet() ?: mutableSetOf()
            if (current.contains(serverId)) current.remove(serverId) else current.add(serverId)
            prefs[FAVORITES_KEY] = current.joinToString(",")
        }
    }

    fun updatePingResults(serverId: String, primaryPing: Long, secondaryPing: Long) {
        _servers.value = _servers.value.map { server ->
            if (server.id == serverId) {
                server.copy(isPinged = true, primaryPing = primaryPing, secondaryPing = secondaryPing)
            } else server
        }
    }

    fun resetPings() {
        _servers.value = _servers.value.map { it.copy(isPinged = false, primaryPing = -1, secondaryPing = -1) }
    }

    fun addCustomServer(name: String, primary: String, secondary: String): Boolean {
        if (primary.isBlank()) return false
        val newServer = DnsServer(
            id = "custom_${System.currentTimeMillis()}",
            name = name.ifBlank { "Custom DNS" },
            primaryIp = primary.trim(),
            secondaryIp = secondary.trim(),
            category = DnsCategory.CUSTOM,
            isCustom = true
        )
        _servers.value = _servers.value + newServer
        return true
    }

    fun removeCustomServer(serverId: String) {
        _servers.value = _servers.value.filter { !(it.id == serverId && it.isCustom) }
    }

    private fun buildDefaultServers(): List<DnsServer> = listOf(
        DnsServer("google_dns", "Google Public DNS", "8.8.8.8", "8.8.4.4", DnsCategory.DEFAULT),
        DnsServer("cloudflare", "Cloudflare", "1.1.1.1", "1.0.0.1", DnsCategory.DEFAULT),
        DnsServer("cloudflare_secure", "Cloudflare Malware Blocking", "1.1.1.2", "1.0.0.2", DnsCategory.SECURE),
        DnsServer("cloudflare_family", "Cloudflare Family", "1.1.1.3", "1.0.0.3", DnsCategory.FAMILY),
        DnsServer("opendns", "OpenDNS", "208.67.222.222", "208.67.220.220", DnsCategory.DEFAULT),
        DnsServer("opendns_2", "OpenDNS Alt", "208.67.222.220", "208.67.220.222", DnsCategory.DEFAULT),
        DnsServer("opendns_family", "OpenDNS Family", "208.67.222.123", "208.67.220.123", DnsCategory.FAMILY),
        DnsServer("quad9", "Quad9 Security", "9.9.9.9", "149.112.112.112", DnsCategory.SECURE),
        DnsServer("quad9_nosec", "Quad9 No Security", "9.9.9.10", "149.112.112.10", DnsCategory.DEFAULT),
        DnsServer("adguard", "AdGuard DNS", "94.140.14.14", "94.140.15.15", DnsCategory.SECURE),
        DnsServer("adguard_family", "AdGuard Family", "94.140.14.15", "94.140.15.16", DnsCategory.FAMILY),
        DnsServer("yandex", "Yandex DNS", "77.88.8.1", "77.88.8.8", DnsCategory.DEFAULT),
        DnsServer("yandex_safe", "Yandex Safe", "77.88.8.88", "77.88.8.2", DnsCategory.SECURE),
        DnsServer("yandex_family", "Yandex Family", "77.88.8.7", "77.88.8.3", DnsCategory.FAMILY),
        DnsServer("nextdns", "NextDNS", "45.90.28.230", "45.90.30.230", DnsCategory.SECURE),
        DnsServer("cleanbrowsing_sec", "CleanBrowsing Secure", "185.228.168.9", "185.228.169.9", DnsCategory.SECURE),
        DnsServer("cleanbrowsing_family", "CleanBrowsing Family", "185.228.168.168", "185.228.169.168", DnsCategory.FAMILY),
        DnsServer("cleanbrowsing_adult", "CleanBrowsing Adult", "185.228.168.10", "185.228.169.11", DnsCategory.FAMILY),
        DnsServer("norton_basic", "Norton ConnectSafe Basic", "199.85.126.10", "199.85.127.10", DnsCategory.SECURE),
        DnsServer("norton_secure", "Norton ConnectSafe Secure", "199.85.126.20", "199.85.127.20", DnsCategory.SECURE),
        DnsServer("norton_family", "Norton ConnectSafe Family", "199.85.126.30", "199.85.127.30", DnsCategory.FAMILY),
        DnsServer("level3_a", "Level3 DNS A", "209.244.0.3", "209.244.0.4", DnsCategory.DEFAULT),
        DnsServer("level3_b", "Level3 DNS B", "4.2.2.1", "4.2.2.2", DnsCategory.DEFAULT),
        DnsServer("level3_c", "Level3 DNS C", "4.2.2.3", "4.2.2.4", DnsCategory.DEFAULT),
        DnsServer("level3_d", "Level3 DNS D", "4.2.2.5", "4.2.2.6", DnsCategory.DEFAULT),
        DnsServer("comodo_secure", "Comodo Secure DNS", "8.26.56.26", "8.20.247.20", DnsCategory.SECURE),
        DnsServer("verisign", "VeriSign Public DNS", "64.6.64.6", "64.6.65.6", DnsCategory.DEFAULT),
        DnsServer("dyn", "Dyn DNS", "216.146.35.35", "216.146.36.36", DnsCategory.DEFAULT),
        DnsServer("neustar1", "Neustar DNS 1", "156.154.70.1", "156.154.71.1", DnsCategory.DEFAULT),
        DnsServer("neustar2", "Neustar DNS 2", "156.154.70.5", "156.154.71.5", DnsCategory.DEFAULT),
        DnsServer("neustar_threat", "Neustar Threat Protection", "156.154.70.2", "156.154.71.2", DnsCategory.SECURE),
        DnsServer("neustar_family", "Neustar Family Secure", "156.154.70.3", "156.154.71.3", DnsCategory.FAMILY),
        DnsServer("neustar_biz", "Neustar Business Secure", "156.154.70.4", "156.154.71.4", DnsCategory.SECURE),
        DnsServer("safedns", "Safe DNS", "195.46.39.39", "195.46.39.40", DnsCategory.SECURE),
        DnsServer("hurricane", "Hurricane Electric", "74.82.42.42", "", DnsCategory.DEFAULT),
        DnsServer("dns4eu", "DNS4EU Default", "86.54.11.100", "86.54.11.200", DnsCategory.DEFAULT),
        DnsServer("dns4eu_secure", "DNS4EU Secure", "86.54.11.13", "86.54.11.213", DnsCategory.SECURE),
        DnsServer("dns4eu_family", "DNS4EU Family", "86.54.11.11", "86.54.11.211", DnsCategory.FAMILY),
        DnsServer("ultradns", "UltraDNS", "204.69.234.1", "204.74.101.1", DnsCategory.DEFAULT),
        DnsServer("zen", "Zen Internet", "212.23.8.1", "212.23.3.1", DnsCategory.DEFAULT),
        DnsServer("orange", "Orange DNS", "195.92.195.94", "195.92.195.95", DnsCategory.DEFAULT),
        DnsServer("controld", "ControlD DNS", "76.76.2.0", "76.76.10.0", DnsCategory.SECURE),
        DnsServer("mullvad", "Mullvad DNS", "194.242.2.2", "194.242.2.3", DnsCategory.SECURE),
        DnsServer("digitalcourage", "DigitalCourage DNS", "46.182.19.48", "46.182.19.49", DnsCategory.SECURE),
        DnsServer("switch_ch", "Switch.ch DNS", "130.59.31.251", "130.59.31.248", DnsCategory.DEFAULT)
    )
}
