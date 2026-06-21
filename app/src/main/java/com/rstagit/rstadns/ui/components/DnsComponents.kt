package com.rstagit.rstadns.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rstagit.rstadns.domain.model.ConnectionStatus
import com.rstagit.rstadns.domain.model.DnsServer
import com.rstagit.rstadns.domain.model.PingStatus
import com.rstagit.rstadns.ui.theme.*

@Composable
fun PingBadge(ping: Long, status: PingStatus, modifier: Modifier = Modifier) {
    val color = pingStatusColor(status)
    val label = when {
        status == PingStatus.UNKNOWN -> "—"
        status == PingStatus.FAILED -> "FAIL"
        else -> "${ping}ms"
    }

    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(400),
        label = "ping_color"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(animatedColor.copy(alpha = 0.12f))
            .border(1.dp, animatedColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = animatedColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PingDot(status: PingStatus, size: Dp = 8.dp) {
    val color = pingStatusColor(status)
    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(400),
        label = "dot_color"
    )
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(animatedColor)
    )
}

fun pingStatusColor(status: PingStatus): Color = when (status) {
    PingStatus.EXCELLENT -> PingExcellent
    PingStatus.GOOD -> PingGood
    PingStatus.MODERATE -> PingModerate
    PingStatus.POOR -> PingPoor
    PingStatus.FAILED -> PingFailed
    PingStatus.UNKNOWN -> PingUnknown
}

@Composable
fun DnsServerCard(
    server: DnsServer,
    onFavoriteClick: () -> Unit,
    onCopyClick: () -> Unit,
    onPingClick: () -> Unit,
    onConnectClick: () -> Unit,
    isConnected: Boolean = false,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            isConnected -> AccentPrimary.copy(alpha = 0.7f)
            server.isFavorite -> AccentPrimary.copy(alpha = 0.4f)
            else -> BorderSubtle
        },
        animationSpec = tween(300),
        label = "border"
    )

    val cardBg = if (isConnected)
        Brush.linearGradient(listOf(Color(0xFF1A0D35), BackgroundCard))
    else
        Brush.linearGradient(listOf(BackgroundCard, BackgroundCard))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBg)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PingDot(server.pingStatus, size = 9.dp)
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = server.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = server.primaryIp,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted
                        )
                    }
                    if (server.isPinged) {
                        PingBadge(server.averagePing, server.pingStatus)
                    }
                }

                if (server.secondaryIp.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.width(17.dp))
                        Text(
                            text = "Alt: ",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Text(
                            text = server.secondaryIp,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted
                        )
                        if (server.secondaryPing >= 0) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "${server.secondaryPing}ms",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = pingStatusColor(server.pingStatus).copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onDeleteClick != null && server.isCustom) {
                        ActionIconButton(
                            icon = Icons.Default.Delete,
                            tint = PingPoor.copy(alpha = 0.7f),
                            onClick = onDeleteClick,
                            contentDescription = "Delete"
                        )
                        Spacer(Modifier.width(2.dp))
                    }
                    ActionIconButton(
                        icon = if (server.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        tint = if (server.isFavorite) Color(0xFFEC4899) else TextSecondary,
                        onClick = onFavoriteClick,
                        contentDescription = "Favorite"
                    )
                    Spacer(Modifier.width(2.dp))
                    ActionIconButton(
                        icon = Icons.Default.Refresh,
                        tint = AccentSecondary,
                        onClick = onPingClick,
                        contentDescription = "Ping"
                    )
                    Spacer(Modifier.width(2.dp))
                    ActionIconButton(
                        icon = Icons.Default.ContentCopy,
                        tint = TextSecondary,
                        onClick = onCopyClick,
                        contentDescription = "Copy"
                    )
                    Spacer(Modifier.weight(1f))
                    ConnectSmallButton(
                        isConnected = isConnected,
                        onClick = onConnectClick
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectSmallButton(isConnected: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(
        targetValue = if (isConnected) ConnectedGreen else AccentPrimary,
        animationSpec = tween(300),
        label = "conn_btn"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isConnected) "Connected" else "Connect",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
    contentDescription: String
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) AccentPrimary else BackgroundElevated,
        animationSpec = tween(200),
        label = "chip_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else TextSecondary,
        animationSpec = tween(200),
        label = "chip_text"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, if (selected) Color.Transparent else BorderSubtle, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun GlowingPingButton(
    isRunning: Boolean,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !isRunning,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentPrimary,
            disabledContainerColor = AccentPrimary.copy(alpha = 0.6f)
        )
    ) {
        if (isRunning) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = Color.White,
                strokeWidth = 2.dp,
                progress = { progress }
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        } else {
            Icon(
                imageVector = Icons.Default.NetworkCheck,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color.White
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "گرفتن پینگ همه",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = color,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

@Composable
fun VpnStyleConnectionButton(
    connectionStatus: ConnectionStatus,
    connectedServer: DnsServer?,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val isConnecting = connectionStatus == ConnectionStatus.CONNECTING
    val isConnected = connectionStatus == ConnectionStatus.CONNECTED

    val glowColor = when (connectionStatus) {
        ConnectionStatus.CONNECTED -> GlowGreen
        ConnectionStatus.CONNECTING -> Color(0xFFF59E0B).copy(alpha = 0.25f)
        ConnectionStatus.DISCONNECTED -> GlowPurple
    }

    val mainColor = when (connectionStatus) {
        ConnectionStatus.CONNECTED -> ConnectedGreen
        ConnectionStatus.CONNECTING -> ConnectingYellow
        ConnectionStatus.DISCONNECTED -> AccentPrimary
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(130.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(glowColor)
            )
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .scale(if (isConnecting) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(BackgroundElevated)
                    .border(2.dp, mainColor.copy(alpha = 0.7f), CircleShape)
                    .clickable {
                        if (isConnected) onDisconnectClick() else if (!isConnecting) onConnectClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = ConnectingYellow,
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = mainColor,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = when (connectionStatus) {
                ConnectionStatus.CONNECTED -> "متصل"
                ConnectionStatus.CONNECTING -> "در حال اتصال..."
                ConnectionStatus.DISCONNECTED -> "متصل نیست"

            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = mainColor
        )

        Spacer(Modifier.height(4.dp))

        if (isConnected && connectedServer != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(BackgroundElevated)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${connectedServer.name}  ·  ${connectedServer.primaryIp}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )
                }
            }
        } else if (!isConnecting) {
            Text(
                text = "سرویس در حال اجرا نیست",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }
}
