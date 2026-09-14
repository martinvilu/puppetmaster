package com.sounddeck.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sounddeck.core.model.ObsConfig
import com.sounddeck.core.model.ObsConnectionState

import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.DisposableEffect
import com.sounddeck.core.obs.DiscoveredObsInstance

@Composable
fun ObsSettingsDialog(
    config: ObsConfig,
    connectionState: ObsConnectionState,
    lastMessage: String,
    streamStats: com.sounddeck.core.model.ObsStreamStats? = null,
    discoveredInstances: List<DiscoveredObsInstance> = emptyList(),
    isDiscovering: Boolean = false,
    obsScenes: List<String> = emptyList(),
    obsInputs: List<String> = emptyList(),
    onStartDiscovery: () -> Unit = {},
    onStopDiscovery: () -> Unit = {},
    onDismiss: () -> Unit,
    onConnect: (ObsConfig) -> Unit,
    onDisconnect: () -> Unit
) {
    var host by remember { mutableStateOf(config.host) }
    var port by remember { mutableStateOf(config.port.toString()) }
    var password by remember { mutableStateOf(config.password) }
    var autoReconnect by remember { mutableStateOf(config.autoReconnect) }
    var reconnectIntervalMs by remember { mutableStateOf((config.reconnectIntervalMs / 1000).toString()) }

    DisposableEffect(Unit) {
        onStartDiscovery()
        onDispose {
            onStopDiscovery()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val dotColor = when (connectionState) {
                    ObsConnectionState.AUTHENTICATED -> Color(0xFF00E676)
                    ObsConnectionState.CONNECTING -> Color(0xFFFFD600)
                    ObsConnectionState.DISCONNECTED -> Color(0xFFFF3D00)
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Text(
                    text = "OBS Studio WebSocket v5",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Configure LAN connection to OBS Studio (obs-websocket v5 with SHA-256 auth).",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(10.dp))

                // C6: Auto-Discovery (mDNS / Zeroconf)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F1218))
                        .border(1.dp, Color(0xFF232834), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Auto-Detección mDNS (C6)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                        if (isDiscovering) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF00E5FF)
                                )
                                Text("Buscando...", fontSize = 10.sp, color = Color(0xFF00E5FF))
                            }
                        } else {
                            TextButton(
                                onClick = onStartDiscovery,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text("Escanear red", fontSize = 11.sp, color = Color(0xFF00E5FF))
                            }
                        }
                    }

                    if (discoveredInstances.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Instancias OBS encontradas en la LAN:",
                            fontSize = 10.sp,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        discoveredInstances.forEach { inst ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1B1E26))
                                    .clickable {
                                        host = inst.host
                                        port = inst.port.toString()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = inst.serviceName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${inst.host}:${inst.port}",
                                        fontSize = 10.sp,
                                        color = Color(0xFF00E5FF)
                                    )
                                }
                                Text(
                                    text = "Usar",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5FF)
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                        }
                    } else if (isDiscovering) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Escaneando '_obs-websocket._tcp.' en la red Wi-Fi...",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Host / IP Address") },
                    placeholder = { Text("192.168.1.100") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        focusedLabelColor = Color(0xFF00E5FF)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Port (default 4455)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        focusedLabelColor = Color(0xFF00E5FF)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Server Password (if required)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        focusedLabelColor = Color(0xFF00E5FF)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-Reconnect", fontSize = 12.sp, color = Color.White)
                    Switch(
                        checked = autoReconnect,
                        onCheckedChange = { autoReconnect = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                    )
                }

                if (streamStats != null && connectionState == ObsConnectionState.AUTHENTICATED) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F1218))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "OBS LIVE STATS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("FPS: ${String.format("%.1f", streamStats.fps)}", fontSize = 11.sp, color = Color.White)
                            Text("CPU: ${String.format("%.1f", streamStats.cpuUsage)}%", fontSize = 11.sp, color = Color.White)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Bitrate: ${(streamStats.outputBytes / 1024)} KB/s", fontSize = 11.sp, color = Color.LightGray)
                            Text(
                                "Perdidos: ${streamStats.droppedFrames}",
                                fontSize = 11.sp,
                                color = if (streamStats.droppedFrames > 0) Color(0xFFFF5252) else Color.LightGray
                            )
                        }
                    }
                }

                if (connectionState == ObsConnectionState.AUTHENTICATED && (obsScenes.isNotEmpty() || obsInputs.isNotEmpty())) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F1218))
                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "SINCRONIZACIÓN OBS STUDIO (QoL 3)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                        if (obsScenes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Escenas detectadas (${obsScenes.size}):", fontSize = 10.sp, color = Color.LightGray)
                            Text(
                                text = obsScenes.joinToString(", "),
                                fontSize = 10.sp,
                                color = Color.White,
                                maxLines = 2
                            )
                        }
                        if (obsInputs.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Fuentes de audio (${obsInputs.size}):", fontSize = 10.sp, color = Color.LightGray)
                            Text(
                                text = obsInputs.joinToString(", "),
                                fontSize = 10.sp,
                                color = Color.White,
                                maxLines = 2
                            )
                        }
                    }
                }

                if (lastMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Status: $lastMessage",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF00E5FF)
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (connectionState != ObsConnectionState.DISCONNECTED) {
                    Button(
                        onClick = onDisconnect,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3D00))
                    ) {
                        Text("Disconnect", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Button(
                    onClick = {
                        val parsedPort = port.toIntOrNull() ?: 4455
                        val parsedInterval = (reconnectIntervalMs.toLongOrNull() ?: 3L) * 1000L
                        val updatedConfig = config.copy(
                            host = host.trim(),
                            port = parsedPort,
                            password = password,
                            autoReconnect = autoReconnect,
                            reconnectIntervalMs = parsedInterval
                        )
                        onConnect(updatedConfig)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("Connect", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF161920)
    )
}
