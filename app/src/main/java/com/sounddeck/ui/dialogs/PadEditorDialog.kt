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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import com.sounddeck.ui.KeyUtils
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.font.FontFamily
import com.sounddeck.core.model.LastHttpTransaction
import com.sounddeck.ui.IconRegistry
import com.sounddeck.ui.components.IconSelector
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sounddeck.core.model.ActionPipeline
import com.sounddeck.core.model.AudioAction
import com.sounddeck.core.model.ConditionType
import com.sounddeck.core.model.GridPosition
import com.sounddeck.core.model.HttpAction
import com.sounddeck.core.model.LabelAlignment
import com.sounddeck.core.model.ObsAction
import com.sounddeck.core.model.PadConfig
import com.sounddeck.core.model.PadStateStep
import com.sounddeck.core.model.PipelineCondition
import com.sounddeck.core.model.PollingConfig
import com.sounddeck.core.model.ResponseMapping
import com.sounddeck.core.model.VisualConfig
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object ColorHistoryHolder {
    val recentColors = mutableListOf(
        "#00E5FF", "#E53935", "#1E88E5", "#43A047", "#FB8C00", "#9C27B0"
    )
    fun addColor(hex: String) {
        if (hex.isBlank()) return
        recentColors.remove(hex)
        recentColors.add(0, hex)
        if (recentColors.size > 8) {
            recentColors.removeLast()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PadEditorDialog(
    pad: PadConfig,
    builtInSounds: List<String>,
    obsScenes: List<String> = emptyList(),
    obsInputs: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (PadConfig) -> Unit,
    onDelete: (String) -> Unit,
    onDuplicate: ((PadConfig) -> Unit)? = null,
    onTestHttpAction: (suspend (HttpAction) -> LastHttpTransaction)? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    // Visual fields
    var label by remember { mutableStateOf(pad.visual.label) }
    var selectedColor by remember { mutableStateOf(pad.visual.backgroundColor) }
    var secondaryColor by remember { mutableStateOf(pad.visual.secondaryColor ?: "") }
    var borderColor by remember { mutableStateOf(pad.visual.borderColor ?: "") }
    var borderWidth by remember { mutableIntStateOf(pad.visual.borderWidthDp) }
    var imageUri by remember { mutableStateOf(pad.visual.imageUri ?: "") }
    var textColor by remember { mutableStateOf(pad.visual.textColor) }
    var selectedIcon by remember { mutableStateOf(pad.visual.iconAsset ?: "") }
    var hapticFeedback by remember { mutableStateOf(pad.visual.hapticFeedback) }
    var cooldownMs by remember { mutableStateOf(if (pad.visual.cooldownMs > 0) pad.visual.cooldownMs.toString() else "") }
    var labelAlignment by remember { mutableStateOf(pad.visual.labelAlignment) }
    var labelFontSizeSp by remember { mutableIntStateOf(pad.visual.labelFontSizeSp) }
    var keyShortcut by remember { mutableStateOf(pad.keyShortcut ?: "") }
    var isLearningKey by remember { mutableStateOf(false) }
    val learningFocusRequester = remember { FocusRequester() }

    // Grid fields
    var rowSpan by remember { mutableIntStateOf(pad.position.rowSpan) }
    var colSpan by remember { mutableIntStateOf(pad.position.colSpan) }

    // Logic: Condition & MultiState fields
    var conditionType by remember {
        mutableStateOf(pad.onTap?.condition?.type ?: ConditionType.NONE)
    }
    var conditionExpectedValue by remember {
        mutableStateOf(pad.onTap?.condition?.expectedValue ?: "")
    }
    var isMultiStateEnabled by remember { mutableStateOf(pad.multiStates.isNotEmpty()) }
    var multiStatesList by remember {
        mutableStateOf(
            if (pad.multiStates.isNotEmpty()) pad.multiStates
            else listOf(
                PadStateStep(label = "Paso 1", backgroundColor = pad.visual.backgroundColor),
                PadStateStep(label = "Paso 2", backgroundColor = "#00ACC1")
            )
        )
    }

    // Audio fields
    var hasAudio by remember { mutableStateOf(pad.onTap?.audio != null) }
    var audioAssetPath by remember { mutableStateOf(pad.onTap?.audio?.assetPath ?: "builtin:airhorn") }
    var audioGain by remember { mutableStateOf(pad.onTap?.audio?.gain ?: 1.0f) }
    var audioFadeInMs by remember { mutableStateOf(if ((pad.onTap?.audio?.fadeInMs ?: 0L) > 0) pad.onTap?.audio?.fadeInMs.toString() else "") }
    var holdToPlay by remember { mutableStateOf(pad.holdToPlay) }

    // Loop & Pipeline fields
    var macroDelayMs by remember { mutableStateOf(if ((pad.onTap?.macroDelayMs ?: 0L) > 0) pad.onTap?.macroDelayMs.toString() else "") }
    var isLooping by remember { mutableStateOf(pad.isLooping) }
    var loopIntervalSeconds by remember { mutableStateOf(if (pad.loopIntervalSeconds > 0) pad.loopIntervalSeconds.toString() else "2") }

    // OBS fields
    var hasObs by remember { mutableStateOf(pad.onTap?.obsAction != null) }
    var obsRequestType by remember {
        mutableStateOf(pad.onTap?.obsAction?.requestType ?: "SetCurrentProgramScene")
    }
    var obsParam by remember {
        val data = pad.onTap?.obsAction?.requestData
        val sceneOrInput = data?.get("sceneName") ?: data?.get("inputName")
        mutableStateOf(sceneOrInput?.toString()?.replace("\"", "") ?: "Main")
    }

    // HTTP fields
    var hasHttp by remember { mutableStateOf(pad.onTap?.httpAction != null) }
    var httpUrl by remember { mutableStateOf(pad.onTap?.httpAction?.url ?: "https://httpbin.org/get") }
    var httpMethod by remember { mutableStateOf(pad.onTap?.httpAction?.method ?: "GET") }
    var jsonPathPercentage by remember {
        mutableStateOf(pad.onTap?.httpAction?.responseMapping?.jsonPathPercentage ?: "")
    }
    var jsonPathBadge by remember {
        mutableStateOf(pad.onTap?.httpAction?.responseMapping?.jsonPathTextBadge ?: "")
    }

    // Polling fields
    var pollingEnabled by remember { mutableStateOf(pad.polling?.enabled ?: false) }
    var pollingIntervalMs by remember { mutableStateOf((pad.polling?.intervalMs ?: 3000L).toString()) }

    val presetColors = listOf(
        "#E53935", "#D81B60", "#8E24AA", "#5E35B1",
        "#1E88E5", "#00ACC1", "#00897B", "#43A047",
        "#FB8C00", "#F4511E", "#37474F", "#1E1E1E"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Pad: ${pad.visual.label}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onDuplicate != null) {
                        IconButton(onClick = {
                            onDuplicate(pad)
                            onDismiss()
                        }) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Duplicar pad",
                                tint = Color(0xFF00E5FF)
                            )
                        }
                    }
                    IconButton(onClick = { onDelete(pad.id) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete pad",
                            tint = Color(0xFFFF5252)
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
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
                        text = { Text("Visual", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Audio", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("OBS", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("HTTP", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        text = { Text("Logic", fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedTab) {
                    0 -> {
                        // Visual tab
                        // Live Preview Card
                        val previewBg = try {
                            Color((0xFF000000 or selectedColor.removePrefix("#").toLong(16)).toInt())
                        } catch (e: Exception) {
                            Color(0xFF1E88E5)
                        }
                        val previewIcon = IconRegistry.resolveIcon(selectedIcon.ifBlank { null })

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F1218))
                                .border(1.dp, Color(0xFF2A3140), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(previewBg)
                                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    if (previewIcon != null) {
                                        Icon(
                                            imageVector = previewIcon,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Text(
                                        text = label.ifBlank { "Pad" },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "VISTA PREVIA",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5FF)
                                )
                                Text(
                                    text = "Color: $selectedColor",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "Ícono: ${selectedIcon.ifBlank { "Sin ícono" }}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = label,
                            onValueChange = { label = it },
                            label = { Text("Pad Label") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00E5FF),
                                focusedLabelColor = Color(0xFF00E5FF)
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Alineación del Texto (D3)", fontSize = 11.sp, color = Color.LightGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            LabelAlignment.values().forEach { align ->
                                val isSel = labelAlignment == align
                                Button(
                                    onClick = { labelAlignment = align },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSel) Color(0xFF00E5FF) else Color(0xFF232834)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = when (align) {
                                            LabelAlignment.TOP -> "Arriba"
                                            LabelAlignment.CENTER -> "Centro"
                                            LabelAlignment.BOTTOM -> "Abajo"
                                        },
                                        fontSize = 11.sp,
                                        color = if (isSel) Color.Black else Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tamaño Fuente: ${labelFontSizeSp}sp", fontSize = 11.sp, color = Color.LightGray)
                            Slider(
                                value = labelFontSizeSp.toFloat(),
                                onValueChange = { labelFontSizeSp = it.toInt() },
                                valueRange = 8f..20f,
                                steps = 11,
                                modifier = Modifier.width(150.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF00E5FF),
                                    activeTrackColor = Color(0xFF00E5FF)
                                )
                            )
                        }

                        if (ColorHistoryHolder.recentColors.isNotEmpty()) {
                            Text("Colores Recientes (QoL 1.4)", fontSize = 11.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ColorHistoryHolder.recentColors.take(6).forEach { hex ->
                                    ColorCircle(
                                        hex = hex,
                                        isSelected = selectedColor.equals(hex, ignoreCase = true),
                                        onClick = { selectedColor = hex }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Text("Color Scheme", fontSize = 12.sp, color = Color.LightGray)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            presetColors.take(6).forEach { hex ->
                                ColorCircle(
                                    hex = hex,
                                    isSelected = selectedColor.equals(hex, ignoreCase = true),
                                    onClick = { selectedColor = hex }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            presetColors.drop(6).forEach { hex ->
                                ColorCircle(
                                    hex = hex,
                                    isSelected = selectedColor.equals(hex, ignoreCase = true),
                                    onClick = { selectedColor = hex }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Selector de Íconos", fontSize = 12.sp, color = Color.LightGray, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        IconSelector(
                            selectedIconId = selectedIcon.ifBlank { null },
                            onIconSelected = { selectedIcon = it },
                            onClearIcon = { selectedIcon = "" }
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Personalización Avanzada (Gradientes y Bordes)", fontSize = 12.sp, color = Color.LightGray, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = secondaryColor,
                                onValueChange = { secondaryColor = it },
                                label = { Text("Color Secundario (Gradiente)") },
                                placeholder = { Text("#000000") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = borderColor,
                                onValueChange = { borderColor = it },
                                label = { Text("Color Borde") },
                                placeholder = { Text("#00E5FF") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = imageUri,
                            onValueChange = { imageUri = it },
                            label = { Text("URI Imagen de Fondo / Carátula") },
                            placeholder = { Text("https://... o file://...") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = cooldownMs,
                                onValueChange = { cooldownMs = it },
                                label = { Text("Cooldown Enfriamiento (ms)") },
                                placeholder = { Text("1000") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = macroDelayMs,
                                onValueChange = { macroDelayMs = it },
                                label = { Text("Macro Delay (ms)") },
                                placeholder = { Text("0") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Atajo Teclado Físico / Pedalera (QoL 6.4)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF00E5FF)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = keyShortcut,
                                onValueChange = { keyShortcut = it.trim().uppercase() },
                                label = { Text("Tecla / Atajo") },
                                placeholder = { Text("ej: 1, SPACE, ENTER, F1") },
                                modifier = Modifier.weight(1f),
                                trailingIcon = {
                                    if (keyShortcut.isNotEmpty()) {
                                        IconButton(onClick = { keyShortcut = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Limpiar atajo",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00E5FF)
                                )
                            )

                            Button(
                                onClick = { isLearningKey = !isLearningKey },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isLearningKey) Color(0xFFFF9100) else Color(0xFF00E5FF)
                                ),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLearningKey) Icons.Default.Hearing else Icons.Default.Keyboard,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isLearningKey) "Escuchando..." else "Aprender",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (isLearningKey) {
                            androidx.compose.runtime.LaunchedEffect(Unit) {
                                learningFocusRequester.requestFocus()
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(learningFocusRequester)
                                    .focusable()
                                    .onKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown) {
                                            val formatted = com.sounddeck.ui.KeyUtils.formatKeyEvent(keyEvent.nativeKeyEvent)
                                            if (formatted.isNotBlank()) {
                                                if (formatted == "ESC") {
                                                    isLearningKey = false
                                                } else {
                                                    keyShortcut = formatted
                                                    isLearningKey = false
                                                }
                                                true
                                            } else {
                                                false
                                            }
                                        } else false
                                    }
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF261D12))
                                    .border(1.5.dp, Color(0xFFFF9100), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = Color(0xFFFF9100),
                                            strokeWidth = 2.dp
                                        )
                                        Column {
                                            Text(
                                                text = "Modo Aprendizaje Activo",
                                                color = Color(0xFFFF9100),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Presiona cualquier tecla o pedal en tu hardware...",
                                                color = Color.LightGray,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    TextButton(
                                        onClick = { isLearningKey = false }
                                    ) {
                                        Text("Cancelar", color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Col Span (Width in cells)", fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { if (colSpan > 1) colSpan-- }) { Text("-") }
                                Text("$colSpan", fontWeight = FontWeight.Bold)
                                TextButton(onClick = { if (colSpan < 4) colSpan++ }) { Text("+") }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Row Span (Height in cells)", fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { if (rowSpan > 1) rowSpan-- }) { Text("-") }
                                Text("$rowSpan", fontWeight = FontWeight.Bold)
                                TextButton(onClick = { if (rowSpan < 3) rowSpan++ }) { Text("+") }
                            }
                        }
                    }

                    1 -> {
                        // Audio tab
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Enable Audio Action", fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = hasAudio,
                                onCheckedChange = { hasAudio = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                            )
                        }

                        if (hasAudio) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Preset Sound Effect:", fontSize = 12.sp, color = Color.LightGray)
                            Spacer(modifier = Modifier.height(4.dp))

                            builtInSounds.chunked(3).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    row.forEach { sound ->
                                        val isSelected = audioAssetPath == "builtin:$sound"
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { audioAssetPath = "builtin:$sound" },
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.25f) else Color(0xFF263238)
                                            ),
                                            border = if (isSelected) CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF00E5FF))) else null
                                        ) {
                                            Text(
                                                text = sound.uppercase(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                                modifier = Modifier.padding(6.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = audioAssetPath,
                                onValueChange = { audioAssetPath = it },
                                label = { Text("Asset Path or Sound Name") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Sound Gain: ${(audioGain * 100).toInt()}%", fontSize = 12.sp)
                            Slider(
                                value = audioGain,
                                onValueChange = { audioGain = it },
                                valueRange = 0.0f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF00E5FF),
                                    activeTrackColor = Color(0xFF00E5FF)
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = audioFadeInMs,
                                onValueChange = { audioFadeInMs = it },
                                label = { Text("Fade In Duration (ms)") },
                                placeholder = { Text("0") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Modo 'Hold to Play' (QoL 2.3)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Text("Reproduce mientras se mantenga presionado y para al soltarlo", fontSize = 10.sp, color = Color.Gray)
                                }
                                Switch(
                                    checked = holdToPlay,
                                    onCheckedChange = { holdToPlay = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                                )
                            }
                        }
                    }

                    2 -> {
                        // OBS tab
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Enable OBS Action", fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = hasObs,
                                onCheckedChange = { hasObs = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                            )
                        }

                        if (hasObs) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val obsActions = listOf(
                                "SetCurrentProgramScene",
                                "SetCurrentPreviewScene",
                                "ToggleInputMute",
                                "SetInputMute",
                                "ToggleRecord",
                                "ToggleStream",
                                "StartRecord",
                                "StopRecord",
                                "SaveReplayBuffer"
                            )

                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = obsRequestType,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Request Type") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    obsActions.forEach { action ->
                                        DropdownMenuItem(
                                            text = { Text(action) },
                                            onClick = {
                                                obsRequestType = action
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            if (obsRequestType.contains("Scene") || obsRequestType.contains("Mute")) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = obsParam,
                                    onValueChange = { obsParam = it },
                                    label = {
                                        Text(if (obsRequestType.contains("Scene")) "Scene Name" else "Input / Source Name")
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (obsRequestType.contains("Scene") && obsScenes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Escenas detectadas en OBS:", fontSize = 10.sp, color = Color(0xFF00E5FF))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        obsScenes.forEach { sceneName ->
                                            val isSelected = obsParam == sceneName
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { obsParam = sceneName },
                                                label = { Text(sceneName, fontSize = 10.sp) }
                                            )
                                        }
                                    }
                                } else if (obsRequestType.contains("Mute") && obsInputs.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Fuentes de audio detectadas en OBS:", fontSize = 10.sp, color = Color(0xFF00E5FF))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        obsInputs.forEach { inputName ->
                                            val isSelected = obsParam == inputName
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { obsParam = inputName },
                                                label = { Text(inputName, fontSize = 10.sp) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // HTTP tab
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Enable HTTP Action", fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = hasHttp,
                                onCheckedChange = { hasHttp = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                            )
                        }

                        if (hasHttp) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = httpUrl,
                                onValueChange = { httpUrl = it },
                                label = { Text("URL Endpoint") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf("GET", "POST", "PUT", "DELETE").forEach { method ->
                                    val isSelected = httpMethod.equals(method, ignoreCase = true)
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { httpMethod = method },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.3f) else Color(0xFF263238)
                                        )
                                    ) {
                                        Text(
                                            text = method,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = jsonPathPercentage,
                                onValueChange = { jsonPathPercentage = it },
                                label = { Text("JSONPath Gauge (e.g. $.cpu.percent)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = jsonPathBadge,
                                onValueChange = { jsonPathBadge = it },
                                label = { Text("JSONPath Text Badge (e.g. $.status)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Periodic Background Polling", fontSize = 12.sp)
                                Switch(
                                    checked = pollingEnabled,
                                    onCheckedChange = { pollingEnabled = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            val coroutineScope = rememberCoroutineScope()
                            var isTestingHttp by remember { mutableStateOf(false) }
                            var testHttpResult by remember { mutableStateOf<LastHttpTransaction?>(null) }

                            Button(
                                onClick = {
                                    if (onTestHttpAction != null) {
                                        coroutineScope.launch {
                                            isTestingHttp = true
                                            val tx = onTestHttpAction(
                                                HttpAction(
                                                    url = httpUrl,
                                                    method = httpMethod,
                                                    responseMapping = if (jsonPathPercentage.isNotBlank() || jsonPathBadge.isNotBlank()) {
                                                        ResponseMapping(
                                                            jsonPathPercentage = jsonPathPercentage.ifBlank { null },
                                                            jsonPathTextBadge = jsonPathBadge.ifBlank { null }
                                                        )
                                                    } else null
                                                )
                                            )
                                            testHttpResult = tx
                                            isTestingHttp = false
                                        }
                                    }
                                },
                                enabled = !isTestingHttp && httpUrl.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Test",
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isTestingHttp) "Probando petición..." else "Probar Petición Ahora (Test HTTP 4.1)",
                                    fontSize = 11.sp,
                                    color = Color(0xFF00E5FF),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (testHttpResult != null) {
                                val tx = testHttpResult!!
                                val badgeColor = if (tx.statusCode in 200..299) Color(0xFF00E676) else Color(0xFFFF5252)
                                Spacer(modifier = Modifier.height(6.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B202A)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = "${tx.method} ${tx.statusCode} (${tx.durationMs}ms)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = badgeColor,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        if (tx.message.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = tx.message,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.LightGray,
                                                maxLines = 3
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Variables globales soportadas: {{API_KEY}}, {{HOST}}, {timestamp}, {random}",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    4 -> {
                        // Logic tab: A1 (Condition) & A2 (Multi-State toggle)
                        Text(
                            text = "A1: Ejecución Condicional (If-Else)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF00E5FF)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ejecutar las acciones del pad solo si se cumple la condición en vivo:",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(
                                ConditionType.NONE to "Siempre ejecutar (Sin condición)",
                                ConditionType.OBS_STREAMING to "Solo si OBS está transmitiendo en vivo",
                                ConditionType.OBS_NOT_STREAMING to "Solo si OBS NO está transmitiendo",
                                ConditionType.OBS_RECORDING to "Solo si OBS está grabando",
                                ConditionType.OBS_NOT_RECORDING to "Solo si OBS NO está grabando",
                                ConditionType.OBS_SCENE_EQUALS to "Solo si la escena activa de OBS coincide"
                            ).forEach { (type, desc) ->
                                val isSelected = conditionType == type
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color(0xFF1E222D))
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFF00E5FF) else Color.Transparent,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { conditionType = type }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = desc,
                                        fontSize = 11.sp,
                                        color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        if (conditionType == ConditionType.OBS_SCENE_EQUALS) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = conditionExpectedValue,
                                onValueChange = { conditionExpectedValue = it },
                                label = { Text("Nombre de Escena OBS Esperada") },
                                placeholder = { Text("ej. En Vivo, Gaming, Camara") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00E5FF)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "A2: Ciclo Multi-Estado (Toggle Secuencial)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF00E5FF)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Activar Secuencia Multi-Paso", fontSize = 12.sp)
                            Switch(
                                checked = isMultiStateEnabled,
                                onCheckedChange = { isMultiStateEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                            )
                        }

                        if (isMultiStateEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            multiStatesList.forEachIndexed { index, step ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${index + 1}.", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    OutlinedTextField(
                                        value = step.label ?: "",
                                        onValueChange = { newLbl ->
                                            val updated = multiStatesList.toMutableList()
                                            updated[index] = step.copy(label = newLbl)
                                            multiStatesList = updated
                                        },
                                        label = { Text("Etiqueta Paso ${index + 1}") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = step.backgroundColor ?: "",
                                        onValueChange = { newBg ->
                                            val updated = multiStatesList.toMutableList()
                                            updated[index] = step.copy(backgroundColor = newBg)
                                            multiStatesList = updated
                                        },
                                        label = { Text("Color Hex") },
                                        modifier = Modifier.width(100.dp)
                                    )
                                    IconButton(onClick = {
                                        if (multiStatesList.size > 1) {
                                            val updated = multiStatesList.toMutableList()
                                            updated.removeAt(index)
                                            multiStatesList = updated
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove step",
                                            tint = Color(0xFFFF5252),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    multiStatesList = multiStatesList + PadStateStep(
                                        label = "Paso ${multiStatesList.size + 1}",
                                        backgroundColor = "#5E35B1"
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF232834)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("+ Añadir Paso de Estado", fontSize = 12.sp, color = Color(0xFF00E5FF))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "A3: Modo Bucle Periódico (Loop 4.5)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF00E5FF)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Ejecutar en bucle recurrente", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("Repite periódicamente la acción del pad al activarlo", fontSize = 10.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = isLooping,
                                onCheckedChange = { isLooping = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                            )
                        }
                        if (isLooping) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = loopIntervalSeconds,
                                onValueChange = { loopIntervalSeconds = it },
                                label = { Text("Intervalo de bucle en segundos (mínimo 1s)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    ColorHistoryHolder.addColor(selectedColor)

                    val newTapAudio = if (hasAudio) {
                        AudioAction(
                            assetPath = audioAssetPath,
                            gain = audioGain,
                            fadeInMs = audioFadeInMs.toLongOrNull() ?: 0L
                        )
                    } else null

                    val newObsAction = if (hasObs) {
                        val data = buildJsonObject {
                            if (obsRequestType.contains("Scene")) {
                                put("sceneName", obsParam)
                            } else if (obsRequestType.contains("Mute")) {
                                put("inputName", obsParam)
                            }
                        }
                        ObsAction(obsRequestType, data)
                    } else null

                    val responseMapping = if (jsonPathPercentage.isNotBlank() || jsonPathBadge.isNotBlank()) {
                        ResponseMapping(
                            jsonPathPercentage = jsonPathPercentage.ifBlank { null },
                            jsonPathTextBadge = jsonPathBadge.ifBlank { null }
                        )
                    } else null

                    val newHttpAction = if (hasHttp) {
                        HttpAction(
                            url = httpUrl,
                            method = httpMethod,
                            responseMapping = responseMapping
                        )
                    } else null

                    val newPolling = if (hasHttp && pollingEnabled) {
                        PollingConfig(
                            enabled = true,
                            intervalMs = pollingIntervalMs.toLongOrNull()?.coerceAtLeast(250L) ?: 3000L,
                            request = newHttpAction ?: HttpAction(httpUrl, httpMethod),
                            responseMapping = responseMapping ?: ResponseMapping()
                        )
                    } else null

                    val condition = if (conditionType != ConditionType.NONE) {
                        PipelineCondition(
                            type = conditionType,
                            expectedValue = conditionExpectedValue.ifBlank { null }
                        )
                    } else null

                    val updatedPad = pad.copy(
                        position = pad.position.copy(rowSpan = rowSpan, colSpan = colSpan),
                        visual = pad.visual.copy(
                            label = label.ifBlank { "Pad" },
                            backgroundColor = selectedColor,
                            secondaryColor = secondaryColor.ifBlank { null },
                            borderColor = borderColor.ifBlank { null },
                            borderWidthDp = borderWidth,
                            imageUri = imageUri.ifBlank { null },
                            textColor = textColor,
                            iconAsset = selectedIcon.ifBlank { null },
                            hapticFeedback = hapticFeedback,
                            cooldownMs = cooldownMs.toLongOrNull() ?: 0L,
                            labelAlignment = labelAlignment,
                            labelFontSizeSp = labelFontSizeSp
                        ),
                        onTap = ActionPipeline(
                            audio = newTapAudio,
                            obsAction = newObsAction,
                            httpAction = newHttpAction,
                            macroDelayMs = macroDelayMs.toLongOrNull() ?: 0L,
                            condition = condition
                        ),
                        polling = newPolling,
                        multiStates = if (isMultiStateEnabled) multiStatesList else emptyList(),
                        holdToPlay = holdToPlay,
                        isLooping = isLooping,
                        loopIntervalSeconds = loopIntervalSeconds.toIntOrNull()?.coerceAtLeast(1) ?: 2,
                        keyShortcut = keyShortcut.ifBlank { null }
                    )
                    onSave(updatedPad)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF161920)
    )
}

@Composable
private fun ColorCircle(
    hex: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = try {
        Color((0xFF000000 or hex.removePrefix("#").toLong(16)).toInt())
    } catch (e: Exception) {
        Color.Gray
    }

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color.White else Color.Black.copy(alpha = 0.4f),
                shape = CircleShape
            )
            .clickable { onClick() }
    )
}
