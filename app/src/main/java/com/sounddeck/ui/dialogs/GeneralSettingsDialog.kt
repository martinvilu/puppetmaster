package com.sounddeck.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sounddeck.core.model.GlobalSettings
import com.sounddeck.core.model.HapticIntensity
import com.sounddeck.core.model.Manifest

@Composable
fun GeneralSettingsDialog(
    globalSettings: GlobalSettings,
    manifest: Manifest?,
    onSaveHapticIntensity: (HapticIntensity) -> Unit,
    onSaveDimScreenTimeout: (Int) -> Unit,
    onSaveConfirmStopStream: (Boolean) -> Unit,
    onSaveEnvironmentVariables: (Map<String, String>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    // 6.1 Haptic Intensity
    var currentHaptic by remember { mutableStateOf(globalSettings.hapticIntensity) }

    // 6.2 Dim screen timeout
    var currentDimTimeout by remember { mutableIntStateOf(globalSettings.dimScreenAfterSeconds) }

    // 3.4 Confirm stop stream
    var currentConfirmStopStream by remember { mutableStateOf(globalSettings.confirmStopStream) }

    // 4.3 Environment variables
    var envVars by remember { mutableStateOf(globalSettings.environmentVariables.toMutableMap()) }
    var newVarKey by remember { mutableStateOf("") }
    var newVarValue by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14171E),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Ajustes",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Configuración Global & QoL",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF1B1E26),
                    contentColor = Color(0xFF00E5FF),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFF00E5FF)
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("General", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Variables HTTP", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Atajos Físicos", fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedTab) {
                    0 -> {
                        // General: 6.1 Háptica, 6.2 Atenuación Pantalla, 3.4 Confirm Stop Stream
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // 6.1 Haptic Settings
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B202A)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Vibration,
                                            contentDescription = null,
                                            tint = Color(0xFF00E5FF),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Intensidad Háptica (Vibración)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Controla la respuesta táctil al pulsar los pads.",
                                        fontSize = 11.sp,
                                        color = Color.LightGray
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf(
                                            HapticIntensity.OFF to "Apagado",
                                            HapticIntensity.LIGHT to "Suave",
                                            HapticIntensity.MEDIUM to "Medio",
                                            HapticIntensity.STRONG to "Fuerte"
                                        ).forEach { (intensity, label) ->
                                            val isSelected = currentHaptic == intensity
                                            Button(
                                                onClick = {
                                                    currentHaptic = intensity
                                                    onSaveHapticIntensity(intensity)
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isSelected) Color(0xFF00E5FF) else Color(0xFF262D3D)
                                                ),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) Color.Black else Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 6.2 Atenuación de Pantalla (Dim Screen Timeout)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B202A)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = Color(0xFFFFD600),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Atenuación Automática de Pantalla",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Ahorra batería atenuando la pantalla si no hay toques recientes.",
                                        fontSize = 11.sp,
                                        color = Color.LightGray
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf(
                                            0 to "Nunca",
                                            30 to "30 seg",
                                            60 to "1 min",
                                            180 to "3 min",
                                            300 to "5 min"
                                        ).forEach { (secs, label) ->
                                            val isSelected = currentDimTimeout == secs
                                            Button(
                                                onClick = {
                                                    currentDimTimeout = secs
                                                    onSaveDimScreenTimeout(secs)
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isSelected) Color(0xFFFFD600) else Color(0xFF262D3D)
                                                ),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) Color.Black else Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 3.4 Confirmación para detener stream en OBS
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B202A)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Confirmar antes de detener stream",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Evita apagar transmisiones en vivo por error al pulsar un pad.",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    Switch(
                                        checked = currentConfirmStopStream,
                                        onCheckedChange = {
                                            currentConfirmStopStream = it
                                            onSaveConfirmStopStream(it)
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        // 4.3 Variables de Entorno Globales (HTTP & Macros)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "Variables de Entorno Globales (HTTP)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF00E5FF)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Define variables para URLs, tokens o headers. Úsalas en cualquier pad como {{NOMBRE}} o {NOMBRE}.",
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // New variable inputs
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newVarKey,
                                    onValueChange = { newVarKey = it.trim().uppercase() },
                                    label = { Text("CLAVE (ej: API_KEY)", fontSize = 10.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF00E5FF),
                                        unfocusedBorderColor = Color(0xFF2E3440),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                                OutlinedTextField(
                                    value = newVarValue,
                                    onValueChange = { newVarValue = it },
                                    label = { Text("VALOR", fontSize = 10.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.2f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF00E5FF),
                                        unfocusedBorderColor = Color(0xFF2E3440),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                                IconButton(
                                    onClick = {
                                        if (newVarKey.isNotBlank()) {
                                            val updated = envVars.toMutableMap()
                                            updated[newVarKey] = newVarValue
                                            envVars = updated
                                            onSaveEnvironmentVariables(updated)
                                            newVarKey = ""
                                            newVarValue = ""
                                        }
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF00E5FF))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Agregar Variable",
                                        tint = Color.Black
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (envVars.isEmpty()) {
                                Text(
                                    text = "No hay variables de entorno configuradas aún.",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    envVars.forEach { (key, value) ->
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B202A)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "{{$key}}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = Color(0xFF00E5FF)
                                                    )
                                                    Text(
                                                        text = value.ifBlank { "(vacío)" },
                                                        fontSize = 11.sp,
                                                        color = Color.White.copy(alpha = 0.8f),
                                                        maxLines = 1
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val updated = envVars.toMutableMap()
                                                        updated.remove(key)
                                                        envVars = updated
                                                        onSaveEnvironmentVariables(updated)
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Eliminar variable",
                                                        tint = Color(0xFFFF5252),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // 6.4 Atajos por Teclado Físico / Pedalera Bluetooth
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Keyboard,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Atajos de Teclado Físico & Pedalera",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "SoundDeck admite control directo mediante teclados USB, Bluetooth o pedales de pie. Configura la tecla de atajo en el editor de cada pad (por ejemplo: F13, F14, 1, 2, SPACE, ENTER).",
                                fontSize = 11.sp,
                                color = Color.LightGray,
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            val padsWithShortcuts = manifest?.pages?.flatMap { it.pads }?.filter { !it.keyShortcut.isNullOrBlank() } ?: emptyList()

                            if (padsWithShortcuts.isEmpty()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B202A)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Ningún pad tiene un atajo de teclado asignado todavía.\nPara asignar uno, abre el modo de edición y edita un pad.",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    padsWithShortcuts.forEach { pad ->
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B202A)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = pad.visual.label,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.White
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0xFF00E5FF).copy(alpha = 0.2f))
                                                        .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = pad.keyShortcut ?: "",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = Color(0xFF00E5FF)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Text("Listo", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}
