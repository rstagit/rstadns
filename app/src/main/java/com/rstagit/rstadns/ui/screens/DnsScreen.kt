package com.rstagit.rstadns.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.core.net.toUri
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rstagit.rstadns.domain.model.AppEvent
import com.rstagit.rstadns.domain.model.ConnectionStatus
import com.rstagit.rstadns.domain.model.DnsCategory
import com.rstagit.rstadns.domain.model.DnsServer
import com.rstagit.rstadns.domain.model.PingStatus
import com.rstagit.rstadns.ui.DnsUiState
import com.rstagit.rstadns.ui.DnsViewModel
import com.rstagit.rstadns.ui.components.*
import com.rstagit.rstadns.ui.theme.*
import kotlinx.coroutines.flow.collectLatest

@Composable
fun DnsScreen(viewModel: DnsViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val servers by viewModel.allServers.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is AppEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                else -> {}
            }
        }
    }

    Scaffold(
        containerColor = BackgroundDeep,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = BackgroundElevated,
                    contentColor = TextPrimary,
                    actionColor = AccentPrimary,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        bottomBar = {
            BottomNavBar(activeTab = uiState.activeTab, onTabSelected = viewModel::setActiveTab)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState.activeTab) {
                0 -> HomeTab(uiState = uiState, viewModel = viewModel, servers = servers)
                1 -> ServersTab(
                    uiState = uiState,
                    viewModel = viewModel,
                    servers = servers,
                    context = context,
                    listState = listState
                )
                2 -> AboutTab()
            }
        }
    }

    if (uiState.showAddCustomDialog) {
        AddCustomDnsDialog(
            onDismiss = { viewModel.toggleAddCustomDialog() },
            onAdd = { name, primary, secondary -> viewModel.addCustomServer(name, primary, secondary) }
        )
    }
}

@Composable
private fun BottomNavBar(activeTab: Int, onTabSelected: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundCard)
            .border(0.5.dp, BorderSubtle, RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BottomNavItem(
                icon = Icons.Default.Home,
                label = "Home",
                selected = activeTab == 0,
                onClick = { onTabSelected(0) }
            )
            BottomNavItem(
                icon = Icons.AutoMirrored.Filled.List,
                label = "Servers",
                selected = activeTab == 1,
                onClick = { onTabSelected(1) }
            )
            BottomNavItem(
                icon = Icons.Default.Info,
                label = "About",
                selected = activeTab == 2,
                onClick = { onTabSelected(2) }
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = if (selected) AccentPrimary else TextMuted
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(AccentPrimary.copy(alpha = 0.15f))
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
            }
        } else {
            Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(3.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun HomeTab(uiState: DnsUiState, viewModel: DnsViewModel, servers: List<DnsServer>) {
    val bestServer = viewModel.getBestServer()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF100820), BackgroundDeep, BackgroundDeep),
                    startY = 0f,
                    endY = 900f
                )
            ),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            HomeHeader()
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                VpnStyleConnectionButton(
                    connectionStatus = uiState.connectionStatus,
                    connectedServer = uiState.connectedServer,
                    onConnectClick = {
                        val target = bestServer ?: servers.firstOrNull()
                        if (target != null) viewModel.connectToDns(target)
                    },
                    onDisconnectClick = { viewModel.disconnectDns() }
                )
            }
        }

        if (bestServer != null && uiState.connectionStatus == ConnectionStatus.DISCONNECTED) {
            item {
                BestServerSuggestion(
                    server = bestServer,
                    onConnectClick = { viewModel.connectToDns(bestServer) }
                )
            }
        }

        item {
            QuickStatsSection(servers = servers)
        }

        item {
            PingAllSection(uiState = uiState, viewModel = viewModel)
        }
    }
}

@Composable
private fun HomeHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 56.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "RSTA DNS",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "version 1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(BackgroundElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = AccentPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun BestServerSuggestion(server: DnsServer, onConnectClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF1A0D35), Color(0xFF0D1A35))
                    )
                )
                .border(1.dp, AccentPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFBBF24).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Fastest Free Server", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(text = server.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text(text = server.primaryIp, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = AccentSecondary)
                }
                Spacer(Modifier.width(8.dp))
                if (server.isPinged) {
                    PingBadge(server.averagePing, server.pingStatus)
                    Spacer(Modifier.width(8.dp))
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentPrimary)
                        .clickable(onClick = onConnectClick)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Connect",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickStatsSection(servers: List<DnsServer>) {
    val pingedServers = servers.filter { it.isPinged }
    val excellent = pingedServers.count { it.pingStatus == PingStatus.EXCELLENT }
    val good = pingedServers.count { it.pingStatus == PingStatus.GOOD }
    val failed = pingedServers.count { it.pingStatus == PingStatus.FAILED }
    val bestPing = pingedServers.filter { it.averagePing >= 0 }.minByOrNull { it.averagePing }?.averagePing

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "Network Stats",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(label = "عالی", value = excellent.toString(), color = PingExcellent, modifier = Modifier.weight(1f))
            StatCard(label = "خوب", value = good.toString(), color = PingGood, modifier = Modifier.weight(1f))
            StatCard(label = "بهترین پینگ", value = bestPing?.toString() ?: "—", color = AccentSecondary, modifier = Modifier.weight(1f))
            StatCard(label = "ناموفق", value = failed.toString(), color = PingPoor, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PingAllSection(uiState: DnsUiState, viewModel: DnsViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Test All Servers", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (uiState.pingState.isRunning) "Testing ${uiState.pingState.currentServer ?: "..."}"
                    else "پیدا کردن سریع ترین دی ان اس",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            GlowingPingButton(
                isRunning = uiState.pingState.isRunning,
                progress = uiState.pingState.progress,
                onClick = viewModel::pingAll
            )
        }
        if (uiState.pingState.isRunning) {
            LinearProgressIndicator(
                progress = { uiState.pingState.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp)
                    .clip(CircleShape),
                color = AccentPrimary,
                trackColor = BackgroundElevated
            )
        }
    }
}

@Composable
private fun ServersTab(
    uiState: DnsUiState,
    viewModel: DnsViewModel,
    servers: List<DnsServer>,
    context: android.content.Context,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                ServersHeader(uiState = uiState, viewModel = viewModel, count = servers.size)
            }
            item {
                FilterRow(uiState = uiState, viewModel = viewModel)
            }
            item {
                ServerSearchBar(query = uiState.searchQuery, onQueryChange = viewModel::setSearchQuery)
            }

            if (servers.isEmpty()) {
                item { EmptyState() }
            } else {
                items(servers, key = { it.id }) { server ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 6 }
                    ) {
                        DnsServerCard(
                            server = server,
                            onFavoriteClick = { viewModel.toggleFavorite(server.id) },
                            onCopyClick = { viewModel.copyToClipboard(context, server) },
                            onPingClick = { viewModel.pingSingle(server) },
                            onConnectClick = {
                                if (uiState.connectedServer?.id == server.id && uiState.connectionStatus == ConnectionStatus.CONNECTED) {
                                    viewModel.disconnectDns()
                                } else {
                                    viewModel.connectToDns(server)
                                }
                            },
                            isConnected = uiState.connectedServer?.id == server.id && uiState.connectionStatus == ConnectionStatus.CONNECTED,
                            onDeleteClick = if (server.isCustom) {
                                { viewModel.removeCustomServer(server.id) }
                            } else null,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { viewModel.toggleAddCustomDialog() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
            containerColor = AccentPrimary,
            shape = RoundedCornerShape(16.dp),
            elevation = FloatingActionButtonDefaults.elevation(0.dp)
        ) {
            Icon(Icons.Outlined.Add, contentDescription = "Add DNS", tint = Color.White)
        }
    }
}

@Composable
private fun ServersHeader(uiState: DnsUiState, viewModel: DnsViewModel, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 52.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "DNS Servers",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "$count servers available",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
        GlowingPingButton(
            isRunning = uiState.pingState.isRunning,
            progress = uiState.pingState.progress,
            onClick = viewModel::pingAll
        )
    }
}

@Composable
private fun FilterRow(uiState: DnsUiState, viewModel: DnsViewModel) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        item {
            CategoryChip(
                label = "All",
                selected = uiState.selectedCategory == null && !uiState.showFavoritesOnly,
                onClick = {
                    viewModel.setCategory(null)
                    if (uiState.showFavoritesOnly) viewModel.toggleFavoritesOnly()
                }
            )
        }
        item {
            CategoryChip(label = "⭐ Favorites", selected = uiState.showFavoritesOnly, onClick = viewModel::toggleFavoritesOnly)
        }
        DnsCategory.values().forEach { category ->
            item {
                CategoryChip(
                    label = category.label,
                    selected = uiState.selectedCategory == category,
                    onClick = { viewModel.setCategory(if (uiState.selectedCategory == category) null else category) }
                )
            }
        }
        item {
            CategoryChip(
                label = if (uiState.sortByPing) "⚡ Sorted" else "Sort by Ping",
                selected = uiState.sortByPing,
                onClick = viewModel::toggleSortByPing
            )
        }
    }
}

@Composable
private fun ServerSearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        placeholder = {
            Text("Search servers...", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
        },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(18.dp))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentPrimary,
            unfocusedBorderColor = BorderSubtle,
            cursorColor = AccentPrimary,
            focusedContainerColor = BackgroundCard,
            unfocusedContainerColor = BackgroundCard
        ),
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
    )
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = Icons.Default.SearchOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(text = "No servers found", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Text(text = "Try adjusting your filters", style = MaterialTheme.typography.bodySmall, color = TextMuted)
    }
}

@Composable
private fun AboutTab() {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "درباره برنامه",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Black,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundCard),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "RSTA DNS",
                    style = MaterialTheme.typography.titleLarge,
                    color = AccentPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "لطفا پیشنهاد یا باگ های برنامه را در تلگرام بفرستید",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        SettingsCard(
            icon = Icons.Default.Info,
            title = "نسخه برنامه",
            subtitle = "v1.0.0",
            onClick = {}
        )
        
        Spacer(Modifier.height(8.dp))
        
        SettingsCard(
            icon = Icons.AutoMirrored.Filled.Send,
            title = "کانال تلگرام",
            subtitle = "برای رفع اشکال و آپدیت‌ها",
            onClick = {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, "https://t.me/rstadns".toUri())
                context.startActivity(intent)
            }
        )
        
        Spacer(Modifier.height(8.dp))
        
        SettingsCard(
            icon = Icons.Default.Code,
            title = "گیت‌هاب پروژه",
            subtitle = "مشاهده سورس کد",
            onClick = {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, "https://github.com/rstagit/rstadns".toUri())
                context.startActivity(intent)
            }
        )
    }
}

@Composable
private fun SettingsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AccentPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
        }
    }
}
