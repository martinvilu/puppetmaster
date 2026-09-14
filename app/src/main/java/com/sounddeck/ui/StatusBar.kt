package com.sounddeck.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sounddeck.core.model.LastHttpTransaction
import com.sounddeck.core.model.ObsConnectionState

import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.PanTool

@Composable
fun SoundDeckStatusBar(
    obsState: ObsConnectionState,
    obsMessage: String,
    lastHttpTx: LastHttpTransaction?,
    masterVolume: Float,
    isEditMode: Boolean,
    isForegroundActive: Boolean,
    isLeftHanded: Boolean = false,
    onObsClick: () -> Unit,
    onHttpClick: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleEditMode: () -> Unit,
    onOpenObsSettings: () -> Unit,
    onOpenGeneralSettings: () -> Unit = {},
    onOpenExportImport: () -> Unit,
    onOpenLogs: () -> Unit = {},
    onToggleLeftHanded: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF101216))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Section: OBS State Indicator + Last HTTP Monitor
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            // OBS State Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1B1E26))
                    .clickable { onObsClick() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("obs_status_indicator"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                val (dotColor, alpha) = when (obsState) {
                    ObsConnectionState.AUTHENTICATED -> Color(0xFF00E676) to 1f
                    ObsConnectionState.CONNECTING -> Color(0xFFFFD600) to pulseAlpha
                    ObsConnectionState.DISCONNECTED -> Color(0xFFFF3D00) to 1f
                }

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .alpha(alpha)
                        .clip(CircleShape)
                        .background(dotColor)
                )

                Text(
                    text = when (obsState) {
                        ObsConnectionState.AUTHENTICATED -> "OBS ON"
                        ObsConnectionState.CONNECTING -> "OBS..."
                        ObsConnectionState.DISCONNECTED -> "OBS OFF"
                    },
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // HTTP Monitor Badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1B1E26))
                    .clickable { onHttpClick() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("http_status_indicator"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (lastHttpTx == null) {
                    Text(
                        text = "HTTP --",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    val badgeColor = when {
                        lastHttpTx.statusCode in 200..299 -> Color(0xFF00E676)
                        lastHttpTx.statusCode == 408 -> Color(0xFFFF9100)
                        lastHttpTx.statusCode >= 400 || lastHttpTx.statusCode == 0 -> Color(0xFFFF1744)
                        else -> Color(0xFF00B0FF)
                    }

                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(badgeColor)
                    )

                    val label = if (lastHttpTx.statusCode > 0) {
                        "${lastHttpTx.method} ${lastHttpTx.statusCode} (${lastHttpTx.durationMs}ms)"
                    } else {
                        "HTTP ERR (${lastHttpTx.durationMs}ms)"
                    }

                    Text(
                        text = label,
                        color = badgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Foreground Service indicator
            if (isForegroundActive) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF263238))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Podcasts,
                            contentDescription = "Background Keep-Alive Active",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "BG",
                            color = Color(0xFF00E5FF),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Right Section: Master Volume Slider & Action Buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Master Volume Slider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .width(110.dp)
                    .padding(end = 4.dp)
            ) {
                Icon(
                    imageVector = if (masterVolume > 0.05f) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                    contentDescription = "Master Volume",
                    tint = if (masterVolume > 0.05f) Color.White else Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
                Slider(
                    value = masterVolume,
                    onValueChange = onVolumeChange,
                    valueRange = 0.0f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00E5FF),
                        activeTrackColor = Color(0xFF00E5FF),
                        inactiveTrackColor = Color(0xFF37474F)
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }

            // Edit Mode Toggle
            IconButton(
                onClick = onToggleEditMode,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Toggle Pad Edit Mode",
                    tint = if (isEditMode) Color(0xFFFFD600) else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(17.dp)
                )
            }

            // OBS Settings
            IconButton(
                onClick = onOpenObsSettings,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "OBS Settings",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(17.dp)
                )
            }

            // General Settings (QoL 6.1, 6.2, 3.4, 4.3)
            IconButton(
                onClick = onOpenGeneralSettings,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "General Settings",
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(17.dp)
                )
            }

            // Left-Handed Mode Toggle (D4)
            IconButton(
                onClick = onToggleLeftHanded,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PanTool,
                    contentDescription = "Modo Zurdo",
                    tint = if (isLeftHanded) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(17.dp)
                )
            }

            // Diagnostic Logs (E5)
            IconButton(
                onClick = onOpenLogs,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = "Logs & Diagnóstico",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(17.dp)
                )
            }

            // Import/Export Archive
            IconButton(
                onClick = onOpenExportImport,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Archive,
                    contentDescription = "Backup and Restore Packages",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}
