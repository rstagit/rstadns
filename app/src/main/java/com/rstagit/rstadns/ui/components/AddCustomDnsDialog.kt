package com.rstagit.rstadns.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rstagit.rstadns.ui.theme.*

@Composable
fun AddCustomDnsDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, primary: String, secondary: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var primary by remember { mutableStateOf("") }
    var secondary by remember { mutableStateOf("") }
    var primaryError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .background(BackgroundCard)
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccentPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Dns, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(text = "Add Custom DNS", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Text(text = "Enter your DNS server addresses", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                }
                Spacer(Modifier.height(20.dp))

                DnsTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Server Name (optional)",
                    placeholder = "My Custom DNS",
                    isError = false
                )
                Spacer(Modifier.height(12.dp))
                DnsTextField(
                    value = primary,
                    onValueChange = {
                        primary = it
                        primaryError = false
                    },
                    label = "Primary DNS *",
                    placeholder = "e.g. 1.1.1.1",
                    isError = primaryError,
                    isIp = true
                )
                if (primaryError) {
                    Spacer(Modifier.height(4.dp))
                    Text(text = "Primary IP is required", style = MaterialTheme.typography.labelSmall, color = PingPoor)
                }
                Spacer(Modifier.height(12.dp))
                DnsTextField(
                    value = secondary,
                    onValueChange = { secondary = it },
                    label = "Secondary DNS (optional)",
                    placeholder = "e.g. 1.0.0.1",
                    isError = false,
                    isIp = true,
                    imeAction = ImeAction.Done
                )

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (primary.isBlank()) {
                                primaryError = true
                            } else {
                                onAdd(name, primary, secondary)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                    ) {
                        Text("Add DNS", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun DnsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isError: Boolean,
    isIp: Boolean = false,
    imeAction: ImeAction = ImeAction.Next
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        placeholder = { Text(placeholder, color = TextMuted, fontFamily = if (isIp) FontFamily.Monospace else null) },
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isIp) KeyboardType.Ascii else KeyboardType.Text,
            imeAction = imeAction
        ),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = if (isIp) FontFamily.Monospace else null,
            color = TextPrimary
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentPrimary,
            unfocusedBorderColor = BorderSubtle,
            errorBorderColor = PingPoor,
            focusedLabelColor = AccentPrimary,
            unfocusedLabelColor = TextMuted,
            cursorColor = AccentPrimary
        ),
        shape = RoundedCornerShape(10.dp)
    )
}
