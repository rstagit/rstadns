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

        // ═══════════════════════════════════════════════════════════════════
        // 1. TIER-1 GLOBAL HIGH-PERFORMANCE
        // ═══════════════════════════════════════════════════════════════════
        DnsServer("cloudflare",            "Cloudflare",                    "1.1.1.1",          "1.0.0.1",          DnsCategory.DEFAULT),
        DnsServer("cloudflare_malware",    "Cloudflare (No Malware)",       "1.1.1.2",          "1.0.0.2",          DnsCategory.SECURE),
        DnsServer("cloudflare_family",     "Cloudflare Family",             "1.1.1.3",          "1.0.0.3",          DnsCategory.SECURE),
        DnsServer("google_dns",            "Google Public DNS",             "8.8.8.8",          "8.8.4.4",          DnsCategory.DEFAULT),
        DnsServer("opendns",               "OpenDNS Home",                  "208.67.222.222",   "208.67.220.220",   DnsCategory.DEFAULT),
        DnsServer("opendns_family",        "OpenDNS FamilyShield",          "208.67.222.123",   "208.67.220.123",   DnsCategory.SECURE),
        DnsServer("quad9",                 "Quad9 Secure",                  "9.9.9.9",          "149.112.112.112",  DnsCategory.SECURE),
        DnsServer("quad9_unsecured",       "Quad9 (Unfiltered)",            "9.9.9.10",         "149.112.112.10",   DnsCategory.DEFAULT),
        DnsServer("quad9_edns",            "Quad9 + ECS",                   "9.9.9.11",         "149.112.112.11",   DnsCategory.SECURE),
        DnsServer("controld",              "ControlD DNS",                  "76.76.2.0",        "76.76.10.0",       DnsCategory.SECURE),
        DnsServer("controld_unfiltered",   "ControlD (Unfiltered)",         "76.76.2.1",        "76.76.10.1",       DnsCategory.DEFAULT),
        DnsServer("controld_malware",      "ControlD (No Malware)",         "76.76.2.2",        "76.76.10.2",       DnsCategory.SECURE),
        DnsServer("adguard",               "AdGuard DNS",                   "94.140.14.14",     "94.140.15.15",     DnsCategory.SECURE),
        DnsServer("adguard_family",        "AdGuard Family",                "94.140.14.15",     "94.140.15.16",     DnsCategory.SECURE),
        DnsServer("adguard_unfiltered",    "AdGuard (Unfiltered)",          "94.140.14.140",    "94.140.14.141",    DnsCategory.DEFAULT),
        DnsServer("nextdns",               "NextDNS",                       "45.90.28.230",     "45.90.30.230",     DnsCategory.SECURE),
        DnsServer("mullvad",               "Mullvad DNS",                   "194.242.2.2",      "194.242.2.3",      DnsCategory.SECURE),
        DnsServer("mullvad_adblock",       "Mullvad DNS (AdBlock)",         "194.242.2.4",      "194.242.2.5",      DnsCategory.SECURE),
        DnsServer("mullvad_family",        "Mullvad DNS Family",            "194.242.2.6",      "194.242.2.7",      DnsCategory.SECURE),
        DnsServer("verisign",              "VeriSign Public DNS",           "64.6.64.6",        "64.6.65.6",        DnsCategory.DEFAULT),
        DnsServer("gcore",                 "Gcore Public DNS",              "95.85.95.85",      "2.56.220.2",       DnsCategory.DEFAULT),
        DnsServer("alternate_dns",         "Alternate DNS",                 "76.76.19.19",      "76.223.122.150",   DnsCategory.SECURE),

        // ═══════════════════════════════════════════════════════════════════
        // 2. NORTH AMERICA OPTIMIZED
        // ═══════════════════════════════════════════════════════════════════
        DnsServer("level3_a",              "Level3 DNS A (4.2.2.1)",        "4.2.2.1",          "4.2.2.2",          DnsCategory.DEFAULT),
        DnsServer("level3_b",              "Level3 DNS B (4.2.2.3)",        "4.2.2.3",          "4.2.2.4",          DnsCategory.DEFAULT),
        DnsServer("level3_c",              "Level3 DNS C (209.244)",        "209.244.0.3",      "209.244.0.4",      DnsCategory.DEFAULT),
        DnsServer("centurylink",           "CenturyLink DNS",               "205.171.3.66",     "205.171.202.166",  DnsCategory.DEFAULT),
        DnsServer("att_dns",               "AT&T DNS",                      "68.94.156.1",      "68.94.157.1",      DnsCategory.DEFAULT),
        DnsServer("comcast_dns",           "Comcast / Xfinity DNS",         "75.75.75.75",      "75.75.76.76",      DnsCategory.DEFAULT),
        DnsServer("dyn",                   "Oracle Dyn DNS",                "216.146.35.35",    "216.146.36.36",    DnsCategory.DEFAULT),
        DnsServer("cira_shield",           "CIRA Canadian Shield",          "149.112.121.10",   "149.112.122.10",   DnsCategory.DEFAULT),
        DnsServer("cira_protected",        "CIRA Canadian Protected",       "149.112.121.20",   "149.112.122.20",   DnsCategory.SECURE),
        DnsServer("cira_family",           "CIRA Canadian Family",          "149.112.121.30",   "149.112.122.30",   DnsCategory.SECURE),
        DnsServer("neustar",               "Neustar UltraDNS",              "156.154.70.1",     "156.154.71.1",     DnsCategory.DEFAULT),
        DnsServer("neustar_threat",        "Neustar Threat Protection",     "156.154.70.2",     "156.154.71.2",     DnsCategory.SECURE),
        DnsServer("neustar_family",        "Neustar Family Secure",         "156.154.70.3",     "156.154.71.3",     DnsCategory.SECURE),
        DnsServer("comodo",                "Comodo Secure DNS",             "8.26.56.26",       "8.20.247.20",      DnsCategory.SECURE),
        DnsServer("cleanbrowsing_sec",     "CleanBrowsing Security",        "185.228.168.9",    "185.228.169.9",    DnsCategory.SECURE),
        DnsServer("cleanbrowsing_fam",     "CleanBrowsing Family",          "185.228.168.168",  "185.228.169.168",  DnsCategory.SECURE),
        DnsServer("cleanbrowsing_adu",     "CleanBrowsing Adult",           "185.228.168.10",   "185.228.169.11",   DnsCategory.SECURE),
        DnsServer("safedns",               "SafeDNS",                       "195.46.39.39",     "195.46.39.40",     DnsCategory.SECURE),
        DnsServer("rogers_ca",             "Rogers Canada DNS",             "207.172.15.236",   "207.172.15.237",   DnsCategory.DEFAULT),
        DnsServer("bell_ca",               "Bell Canada DNS",               "209.226.175.4",    "209.226.175.5",    DnsCategory.DEFAULT),
        DnsServer("shaw_ca",               "Shaw Canada DNS",               "24.66.65.65",      "24.66.65.66",      DnsCategory.DEFAULT),

        // ═══════════════════════════════════════════════════════════════════
        // 3. EUROPE OPTIMIZED
        // ═══════════════════════════════════════════════════════════════════
        DnsServer("dns_watch",             "DNS.Watch",                     "84.200.69.80",     "84.200.70.40",     DnsCategory.DEFAULT),
        // DNS4EU - سرویس اتحادیه اروپا (راه‌اندازی ژوئن ۲۰۲۵) - هر پروفایل IP منحصر دارد، secondary همان primary است (single-anycast)
        DnsServer("dns4eu_unfiltered",     "DNS4EU Unfiltered (EU)",        "86.54.11.100",     "86.54.11.100",     DnsCategory.DEFAULT),
        DnsServer("dns4eu_malware",        "DNS4EU Malware Block (EU)",     "86.54.11.101",     "86.54.11.101",     DnsCategory.SECURE),
        DnsServer("dns4eu_safe",           "DNS4EU Safe Browsing (EU)",     "86.54.11.102",     "86.54.11.102",     DnsCategory.SECURE),
        DnsServer("dns4eu_adblock",        "DNS4EU Ad Block (EU)",          "86.54.11.103",     "86.54.11.103",     DnsCategory.SECURE),
        DnsServer("dns4eu_full",           "DNS4EU Full Security (EU)",     "86.54.11.104",     "86.54.11.104",     DnsCategory.SECURE),
        DnsServer("switch_dns",            "SWITCH DNS (Switzerland)",      "130.59.31.248",    "130.59.31.251",    DnsCategory.SECURE),
        DnsServer("digitale_gesellschaft", "Digitale Gesellschaft (CH)",    "185.95.218.42",    "185.95.218.43",    DnsCategory.SECURE),
        DnsServer("ccc_de",               "CCC Berlin DNS",                "213.73.91.35",     "213.73.91.35",     DnsCategory.DEFAULT),
        DnsServer("dnsfilter",             "DNSFilter",                     "103.247.36.36",    "103.247.37.37",    DnsCategory.SECURE),
        DnsServer("he_net",                "Hurricane Electric DNS",        "74.82.42.42",      "74.82.42.42",      DnsCategory.DEFAULT),
        DnsServer("uncensored_dns",        "UncensoredDNS (Denmark)",       "91.239.100.100",   "89.233.43.71",     DnsCategory.DEFAULT),
        DnsServer("freifunk",              "Freifunk Munich DNS",           "195.30.94.28",     "77.109.139.29",    DnsCategory.DEFAULT),
        DnsServer("restena_lu",            "RESTENA Luxembourg",            "158.64.1.29",      "158.64.1.30",      DnsCategory.DEFAULT),
        DnsServer("telia_se",              "Telia Sweden DNS",              "195.54.10.1",      "194.132.236.1",    DnsCategory.DEFAULT),
        DnsServer("telenor_no",            "Telenor Norway DNS",            "193.213.112.4",    "128.39.0.4",       DnsCategory.DEFAULT),
        DnsServer("tdc_dk",               "TDC Denmark DNS",               "62.107.0.1",       "62.107.0.2",       DnsCategory.DEFAULT),
        DnsServer("swisscom_ch",           "Swisscom Switzerland DNS",      "195.186.1.111",    "195.186.4.111",    DnsCategory.DEFAULT),
        DnsServer("a1_at",                 "A1 Austria DNS",                "195.3.96.67",      "195.3.96.68",      DnsCategory.DEFAULT),
        DnsServer("proximus_be",           "Proximus Belgium DNS",          "195.238.2.21",     "195.238.2.22",     DnsCategory.DEFAULT),
        DnsServer("xs4all_nl",             "XS4ALL Netherlands DNS",        "194.109.6.66",     "194.109.9.99",     DnsCategory.DEFAULT),
        DnsServer("kpn_nl",               "KPN Netherlands DNS",           "194.151.228.73",   "194.151.228.74",   DnsCategory.DEFAULT),
        DnsServer("bt_uk",                "BT UK DNS",                     "194.74.65.68",     "194.74.65.69",     DnsCategory.DEFAULT),
        DnsServer("virgin_uk",             "Virgin Media UK DNS",           "80.77.224.33",     "80.77.225.33",     DnsCategory.DEFAULT),
        DnsServer("sky_uk",               "Sky UK DNS",                    "90.207.238.97",    "90.207.238.98",    DnsCategory.DEFAULT),
        DnsServer("telekom_de",            "Deutsche Telekom DNS",          "194.25.0.60",      "194.25.2.129",     DnsCategory.DEFAULT),
        DnsServer("vodafone_de",           "Vodafone Germany DNS",          "212.18.7.14",      "212.18.7.15",      DnsCategory.DEFAULT),
        DnsServer("o2_de",                "O2 Germany DNS",                "62.134.11.4",      "62.134.11.5",      DnsCategory.DEFAULT),
        DnsServer("free_fr",              "Free France DNS",               "212.27.40.240",    "212.27.40.241",    DnsCategory.DEFAULT),
        DnsServer("orange_fr",             "Orange France DNS",             "80.10.246.2",      "80.10.246.130",    DnsCategory.DEFAULT),
        DnsServer("sfr_fr",               "SFR France DNS",                "109.0.66.10",      "109.0.66.20",      DnsCategory.DEFAULT),
        DnsServer("telecom_it",            "Telecom Italia DNS",            "213.216.2.250",    "213.216.2.245",    DnsCategory.DEFAULT),
        DnsServer("fastweb_it",            "Fastweb Italy DNS",             "81.199.226.241",   "81.199.226.242",   DnsCategory.DEFAULT),
        DnsServer("telefonica_es",         "Telefónica Spain DNS",          "212.166.132.110",  "212.166.132.111",  DnsCategory.DEFAULT),

        // ═══════════════════════════════════════════════════════════════════
        // 4. ASIA-PACIFIC OPTIMIZED
        // ═══════════════════════════════════════════════════════════════════
        DnsServer("alidns",                "AliDNS / Alibaba",              "223.5.5.5",        "223.6.6.6",        DnsCategory.DEFAULT),
        DnsServer("dnspod",                "DNSPod / Tencent",              "119.29.29.29",     "119.28.28.28",     DnsCategory.DEFAULT),
        DnsServer("tencent_dns2",          "Tencent DNS Alt",               "182.254.116.116",  "182.254.118.118",  DnsCategory.DEFAULT),
        DnsServer("baidu_dns",             "Baidu DNS",                     "180.76.76.76",     "180.76.76.76",     DnsCategory.DEFAULT),
        DnsServer("114dns",                "114DNS China",                  "114.114.114.114",  "114.114.115.115",  DnsCategory.DEFAULT),
        DnsServer("114dns_safe",           "114DNS Safe",                   "114.114.114.119",  "114.114.115.119",  DnsCategory.SECURE),
        DnsServer("114dns_family",         "114DNS Family",                 "114.114.114.110",  "114.114.115.110",  DnsCategory.SECURE),
        DnsServer("cnnic",                 "CNNIC SDNS",                    "1.2.4.8",          "210.2.4.8",        DnsCategory.DEFAULT),
        DnsServer("bezeq_il",              "Bezeq International (IL)",      "80.179.55.55",     "80.179.52.52",     DnsCategory.DEFAULT),
        DnsServer("ntt_jp",                "NTT Japan DNS",                 "129.250.35.250",   "129.250.35.251",   DnsCategory.DEFAULT),
        DnsServer("kddi_jp",               "KDDI Japan DNS",                "157.7.10.30",      "157.7.10.31",      DnsCategory.DEFAULT),
        DnsServer("softbank_jp",           "SoftBank Japan DNS",            "210.130.2.2",      "210.130.2.3",      DnsCategory.DEFAULT),
        DnsServer("sk_telecom_kr",         "SK Telecom Korea",              "210.220.163.82",   "219.250.36.130",   DnsCategory.DEFAULT),
        DnsServer("kt_kr",                "KT Korea DNS",                  "168.126.63.1",     "168.126.63.2",     DnsCategory.DEFAULT),
        DnsServer("lg_uplus_kr",           "LG U+ Korea DNS",               "164.124.101.2",    "203.248.252.2",    DnsCategory.DEFAULT),
        DnsServer("hinet_tw",              "HiNet Taiwan DNS",              "168.95.1.1",       "168.95.192.1",     DnsCategory.DEFAULT),
        DnsServer("fetnet_tw",             "FET Taiwan DNS",                "139.175.55.244",   "139.175.55.246",   DnsCategory.DEFAULT),
        DnsServer("singtel_sg",            "SingTel Singapore DNS",         "165.21.83.88",     "165.21.100.88",    DnsCategory.DEFAULT),
        DnsServer("starhub_sg",            "StarHub Singapore DNS",         "58.181.103.7",     "58.181.103.8",     DnsCategory.DEFAULT),
        DnsServer("ais_th",               "AIS Thailand DNS",              "203.150.2.15",     "203.150.2.72",     DnsCategory.DEFAULT),
        DnsServer("true_th",              "True Online Thailand DNS",      "203.113.1.1",      "203.113.1.2",      DnsCategory.DEFAULT),
        DnsServer("indosat_id",            "Indosat / IOH Indonesia",       "124.81.247.3",     "122.122.122.122",  DnsCategory.DEFAULT),
        DnsServer("telkom_id",             "Telkom Indonesia DNS",          "203.130.196.5",    "203.130.196.6",    DnsCategory.DEFAULT),
        DnsServer("vnpt_vn",              "VNPT Vietnam DNS",              "203.162.4.190",    "203.162.4.191",    DnsCategory.DEFAULT),
        DnsServer("viettel_vn",            "Viettel Vietnam DNS",           "203.113.131.1",    "203.113.131.2",    DnsCategory.DEFAULT),
        DnsServer("jio_in",               "Jio India DNS",                 "182.79.129.145",   "182.79.128.43",    DnsCategory.DEFAULT),
        DnsServer("bsnl_in",              "BSNL India DNS",                "210.212.5.6",      "210.212.7.5",      DnsCategory.DEFAULT),
        DnsServer("hk_broadband",          "HKBN Hong Kong DNS",            "103.107.36.1",     "103.107.36.2",     DnsCategory.DEFAULT),
        DnsServer("telstra_au",            "Telstra Australia DNS",         "139.130.4.4",      "139.130.4.5",      DnsCategory.DEFAULT),
        DnsServer("optus_au",              "Optus Australia DNS",           "198.142.0.51",     "198.142.0.52",     DnsCategory.DEFAULT),
        DnsServer("spark_nz",              "Spark New Zealand DNS",         "114.31.248.1",     "114.31.248.2",     DnsCategory.DEFAULT),

        // ═══════════════════════════════════════════════════════════════════
        // 5. MIDDLE EAST & TURKEY OPTIMIZED
        // ═══════════════════════════════════════════════════════════════════
        DnsServer("turk_telekom",          "Türk Telekom DNS",              "195.175.39.39",    "195.175.39.40",    DnsCategory.DEFAULT),
        DnsServer("superonline_tr",        "Superonline Turkey DNS",        "195.175.37.154",   "195.175.37.155",   DnsCategory.DEFAULT),
        DnsServer("ttnet_tr",              "TTNet Turkey DNS",              "195.175.36.200",   "195.175.36.201",   DnsCategory.DEFAULT),
        DnsServer("stc_sa",               "STC Saudi Arabia DNS",          "212.131.128.1",    "212.131.128.2",    DnsCategory.DEFAULT),
        DnsServer("etisalat_ae",           "Etisalat UAE DNS",              "213.42.20.20",     "195.229.241.222",  DnsCategory.DEFAULT),
        DnsServer("du_ae",                "Du Telecom UAE DNS",            "217.165.138.100",  "217.165.138.200",  DnsCategory.DEFAULT),
        DnsServer("zain_kw",              "Zain Kuwait DNS",               "78.96.0.1",        "78.96.0.2",        DnsCategory.DEFAULT),

        // ═══════════════════════════════════════════════════════════════════
        // 6. RUSSIA & CIS
        // ═══════════════════════════════════════════════════════════════════
        DnsServer("yandex_basic",          "Yandex DNS Basic",              "77.88.8.8",        "77.88.8.1",        DnsCategory.DEFAULT),
        DnsServer("yandex_safe",           "Yandex DNS Safe",               "77.88.8.88",       "77.88.8.2",        DnsCategory.SECURE),
        DnsServer("yandex_family",         "Yandex DNS Family",             "77.88.8.7",        "77.88.8.3",        DnsCategory.SECURE),
        DnsServer("skydns_ru",             "SkyDNS Russia",                 "193.58.251.251",   "93.185.106.161",   DnsCategory.SECURE),
        DnsServer("mts_ru",               "MTS Russia DNS",                "212.152.27.45",    "212.152.27.46",    DnsCategory.DEFAULT),
        DnsServer("beeline_ru",            "Beeline Russia DNS",            "217.118.66.243",   "217.118.66.244",   DnsCategory.DEFAULT),
        DnsServer("megafon_ru",            "MegaFon Russia DNS",            "195.98.208.30",    "195.98.209.30",    DnsCategory.DEFAULT),

        // ═══════════════════════════════════════════════════════════════════
        // 7. LATIN AMERICA OPTIMIZED
        // ═══════════════════════════════════════════════════════════════════
        DnsServer("claro_br",              "Claro Brazil DNS",              "200.188.20.1",     "200.188.20.2",     DnsCategory.DEFAULT),
        DnsServer("vivo_br",               "Vivo Brazil DNS",               "200.204.0.1",      "200.204.0.2",      DnsCategory.DEFAULT),
        DnsServer("oi_br",                "Oi Brazil DNS",                 "200.181.105.3",    "200.181.2.1",      DnsCategory.DEFAULT),
        DnsServer("telmex_mx",             "Telmex Mexico DNS",             "201.130.108.240",  "201.130.108.241",  DnsCategory.DEFAULT),
        DnsServer("claro_ar",              "Claro Argentina DNS",           "200.114.155.1",    "200.114.155.2",    DnsCategory.DEFAULT),
        DnsServer("telecentro_ar",         "Telecentro Argentina DNS",      "200.32.39.2",      "200.32.39.3",      DnsCategory.DEFAULT),

        // ═══════════════════════════════════════════════════════════════════
        // 8. AFRICA OPTIMIZED
        // ═══════════════════════════════════════════════════════════════════
        DnsServer("vodacom_za",            "Vodacom South Africa DNS",      "196.25.1.1",       "196.25.1.2",       DnsCategory.DEFAULT),
        DnsServer("mtn_za",               "MTN South Africa DNS",          "196.11.240.241",   "196.11.240.250",   DnsCategory.DEFAULT),

        // ═══════════════════════════════════════════════════════════════════
        // 9. GAMING-SPECIFIC & LOW-LATENCY
        // ═══════════════════════════════════════════════════════════════════
        DnsServer("opennic_1",             "OpenNIC DNS 1",                 "138.197.140.189",  "137.220.55.93",    DnsCategory.DEFAULT),
        DnsServer("opennic_2",             "OpenNIC DNS 2",                 "185.121.177.177",  "169.239.202.202",  DnsCategory.DEFAULT),
        DnsServer("opennic_3",             "OpenNIC DNS 3",                 "78.47.114.161",    "87.98.175.85",     DnsCategory.DEFAULT),
        DnsServer("opennic_4",             "OpenNIC DNS 4",                 "104.168.119.168",  "104.43.71.193",    DnsCategory.DEFAULT),

        // ═══════════════════════════════════════════════════════════════════
        // 10. IRANIAN ANTI-SANCTION & GAMING (سرویس‌های داخلی ایران)
        // ═══════════════════════════════════════════════════════════════════
        DnsServer("radar_game",            "Radar Game",                    "10.202.10.10",     "10.202.10.11",     DnsCategory.DEFAULT),
        DnsServer("electro",               "Electro Service",               "78.157.42.100",    "78.157.42.101",    DnsCategory.DEFAULT),
        DnsServer("shecan",                "Shecan DNS",                    "178.22.122.100",   "185.51.200.2",     DnsCategory.DEFAULT),
        DnsServer("begzar",                "Begzar DNS",                    "185.55.226.26",    "185.55.225.25",    DnsCategory.DEFAULT),
        DnsServer("403online",             "403 Online",                    "10.202.10.202",    "10.202.10.102",    DnsCategory.DEFAULT),
        DnsServer("taklol",                "TakLol DNS",                    "195.188.217.16",   "195.188.217.17",   DnsCategory.DEFAULT),
        DnsServer("pars_online",           "Pars Online DNS",               "5.202.100.100",    "5.202.100.101",    DnsCategory.DEFAULT),
        DnsServer("host_ir",               "Host.ir DNS",                   "217.218.155.155",  "217.218.127.127",  DnsCategory.DEFAULT),

        // ═══════════════════════════════════════════════════════════════════
        // 11. IRANIAN ISP DNS (اینترنت‌سرویس‌دهندگان ایران)
        // ═══════════════════════════════════════════════════════════════════
        DnsServer("shatel",                "Shatel DNS",                    "85.15.1.14",       "85.15.1.15",       DnsCategory.DEFAULT),
        DnsServer("asiatech",              "Asiatech DNS",                  "87.107.120.120",   "89.28.28.28",      DnsCategory.DEFAULT),
        DnsServer("pishgaman",             "Pishgaman DNS",                 "5.200.200.200",    "5.200.200.201",    DnsCategory.DEFAULT),
        DnsServer("tci_ir",                "TCI / Mokhaberat DNS",          "217.218.127.127",  "217.218.155.155",  DnsCategory.DEFAULT),
        DnsServer("sabanet_ir",            "Saba Net DNS",                  "5.144.129.200",    "5.144.129.201",    DnsCategory.DEFAULT),
        DnsServer("fanava_ir",             "Fanava DNS",                    "188.229.116.16",   "188.229.116.17",   DnsCategory.DEFAULT),
        DnsServer("parsian_ir",            "Parsian Data DNS",              "194.225.0.100",    "194.225.0.101",    DnsCategory.DEFAULT),
        DnsServer("neda_ir",               "Neda Communications DNS",       "194.225.24.8",     "194.225.24.9",     DnsCategory.DEFAULT)
    )
}
