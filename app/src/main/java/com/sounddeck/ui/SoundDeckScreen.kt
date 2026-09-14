package com.sounddeck.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sounddeck.core.model.PadConfig
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Warning
import com.sounddeck.ui.dialogs.DiagnosticLogsDialog
import com.sounddeck.ui.dialogs.ExportImportDialog
import com.sounddeck.ui.dialogs.GeneralSettingsDialog
import com.sounddeck.ui.dialogs.HttpHistoryDialog
import com.sounddeck.ui.dialogs.ObsSettingsDialog
import com.sounddeck.ui.dialogs.PadEditorDialog
import com.sounddeck.ui.dialogs.PageManagerDialog

@Composable
fun SoundDeckScreen(
    viewModel: SoundDeckViewModel,
    modifier: Modifier = Modifier
) {
    val manifest by viewModel.manifest.collectAsState()
    val activePageIndex by viewModel.activePageIndex.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val isForegroundActive by viewModel.isForegroundActive.collectAsState()
    val masterVolume by viewModel.masterVolume.collectAsState()
    val obsState by viewModel.obsState.collectAsState()
    val obsMessage by viewModel.obsMessage.collectAsState()
    val lastHttpTx by viewModel.lastHttpTx.collectAsState()
    val httpHistory by viewModel.httpHistory.collectAsState()
    val dynamicPadStates by viewModel.dynamicPadStates.collectAsState()
    val exportStatusMessage by viewModel.exportStatusMessage.collectAsState()
    val isPadLockActive by viewModel.isPadLockActive.collectAsState()
    val isKioskMode by viewModel.isKioskMode.collectAsState()
    val isCueSoloMode by viewModel.isCueSoloMode.collectAsState()
    val padCooldowns by viewModel.padCooldowns.collectAsState()
    val obsStreamStats by viewModel.obsManager.streamStats.collectAsState()
    val isLeftHandedMode by viewModel.isLeftHandedMode.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val backups by viewModel.backups.collectAsState()
    val discoveredObsInstances by viewModel.discoveredObsInstances.collectAsState()
    val isObsDiscovering by viewModel.isObsDiscovering.collectAsState()
    val obsScenes by viewModel.obsScenes.collectAsState()
    val obsInputs by viewModel.obsInputs.collectAsState()
    val copiedVisualConfig by viewModel.copiedVisualConfig.collectAsState()
    val activeMacroDelays by viewModel.activeMacroDelays.collectAsState()
    val activeLoopingPads by viewModel.activeLoopingPads.collectAsState()
    val pendingStopStreamConfirmation by viewModel.pendingStopStreamConfirmation.collectAsState()

    var editingPad by remember { mutableStateOf<PadConfig?>(null) }
    var showObsSettings by remember { mutableStateOf(false) }
    var showGeneralSettings by remember { mutableStateOf(false) }
    var showExportImport by remember { mutableStateOf(false) }
    var showHttpHistory by remember { mutableStateOf(false) }
    var showAddPageDialog by remember { mutableStateOf(false) }
    var showManagePagesDialog by remember { mutableStateOf(false) }
    var showDiagnosticLogs by remember { mutableStateOf(false) }
    var swapSourcePadId by remember { mutableStateOf<String?>(null) }

    val currentManifest = manifest

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0F13))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 1. Compact Status Bar
        SoundDeckStatusBar(
            obsState = obsState,
            obsMessage = obsMessage,
            lastHttpTx = lastHttpTx,
            masterVolume = masterVolume,
            isEditMode = isEditMode,
            isForegroundActive = isForegroundActive,
            isLeftHanded = isLeftHandedMode,
            onObsClick = {
                if (obsState == com.sounddeck.core.model.ObsConnectionState.DISCONNECTED) {
                    viewModel.reconnectObs()
                } else {
                    showObsSettings = true
                }
            },
            onHttpClick = { showHttpHistory = true },
            onVolumeChange = { viewModel.setMasterVolume(it) },
            onToggleEditMode = { viewModel.toggleEditMode() },
            onOpenObsSettings = { showObsSettings = true },
            onOpenGeneralSettings = { showGeneralSettings = true },
            onOpenExportImport = { showExportImport = true },
            onOpenLogs = { showDiagnosticLogs = true },
            onToggleLeftHanded = { viewModel.toggleLeftHandedMode() }
        )

        // 2. Tab Row for Pages
        if (currentManifest != null && currentManifest.pages.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF14171E)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScrollableTabRow(
                    selectedTabIndex = activePageIndex.coerceIn(0, currentManifest.pages.size - 1),
                    containerColor = Color(0xFF14171E),
                    contentColor = Color(0xFF00E5FF),
                    edgePadding = 8.dp,
                    indicator = { tabPositions ->
                        val safeIdx = activePageIndex.coerceIn(0, tabPositions.size - 1)
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[safeIdx]),
                            color = Color(0xFF00E5FF)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    currentManifest.pages.forEachIndexed { index, page ->
                        Tab(
                            selected = activePageIndex == index,
                            onClick = { viewModel.selectPage(index) },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = page.name,
                                        fontWeight = if (activePageIndex == index) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = if (activePageIndex == index) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = "(${page.gridRows}×${page.gridCols})",
                                        fontSize = 9.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        )
                    }
                }

                // Manage Pages Button (Reorder, Rename, Delete)
                IconButton(
                    onClick = { showManagePagesDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Administrar Páginas",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Add Page Button
                IconButton(
                    onClick = { showAddPageDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Page",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 3. Center Matrix: StaticSpanGrid Layout
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            val activePage = currentManifest?.pages?.getOrNull(activePageIndex)
            if (activePage != null) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (isEditMode && swapSourcePadId != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF00E5FF).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(6.dp))
                                .clickable { swapSourcePadId = null }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pad seleccionado para mover. Toca otro pad para intercambiar.",
                                color = Color(0xFF00E5FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Cancelar [X]",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    StaticSpanGrid(
                        rows = activePage.gridRows,
                        cols = activePage.gridCols,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        activePage.pads.forEach { pad ->
                            val cooldownRemainingMs = padCooldowns[pad.id] ?: 0L
                            val maxCooldown = if (pad.visual.cooldownMs > 0) pad.visual.cooldownMs.toFloat() else 1000f
                            val cooldownFraction = (cooldownRemainingMs / maxCooldown).coerceIn(0f, 1f)

                            PadView(
                                pad = pad,
                                dynamicState = dynamicPadStates[pad.id],
                                isEditMode = isEditMode,
                                isLocked = isPadLockActive,
                                cooldownRemainingFraction = cooldownFraction,
                                isSwapSource = (swapSourcePadId == pad.id),
                                hasCopiedStyle = (copiedVisualConfig != null),
                                macroDelayProgress = activeMacroDelays[pad.id] ?: 0f,
                                isLoopingActive = activeLoopingPads.contains(pad.id),
                                onTap = {
                                    if (isEditMode && swapSourcePadId != null) {
                                        if (swapSourcePadId != pad.id) {
                                            viewModel.swapPads(swapSourcePadId!!, pad.id)
                                        }
                                        swapSourcePadId = null
                                    } else {
                                        viewModel.triggerPadTap(pad)
                                    }
                                },
                                onLongPress = { viewModel.triggerPadLongPress(pad) },
                                onEditClick = { editingPad = pad },
                                onDuplicateClick = { viewModel.duplicatePad(pad) },
                                onSelectForSwap = {
                                    if (swapSourcePadId == null) {
                                        swapSourcePadId = pad.id
                                    } else if (swapSourcePadId == pad.id) {
                                        swapSourcePadId = null
                                    } else {
                                        viewModel.swapPads(swapSourcePadId!!, pad.id)
                                        swapSourcePadId = null
                                    }
                                },
                                onCopyStyle = { viewModel.copyPadStyle(pad) },
                                onPasteStyle = { viewModel.pastePadStyle(pad) },
                                onRelease = { viewModel.triggerPadRelease(pad) },
                                modifier = Modifier.gridPosition(pad.position)
                            )
                        }
                    }
                }
            }
        }

        // 4. Compact Bottom Control Bar (Responsive to D4 Left-Handed Mode)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF101216))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val stopAudioButton = @Composable {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFB71C1C))
                        .clickable { viewModel.audioEngine.stopPlayback() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop all audio",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "STOP AUDIO",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            val serviceControls = @Composable {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isForegroundActive) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF1F2430))
                            .clickable { viewModel.toggleForegroundService() }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Podcasts,
                            contentDescription = null,
                            tint = if (isForegroundActive) Color(0xFF00E5FF) else Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isForegroundActive) "Bg ON" else "Bg OFF",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isForegroundActive) Color(0xFF00E5FF) else Color.Gray
                        )
                    }

                    // Pad Lock Toggle (2.2 Pad Lock)
                    IconButton(
                        onClick = { viewModel.togglePadLock() },
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isPadLockActive) Color(0xFFFF5252).copy(alpha = 0.25f) else Color(0xFF1F2430))
                    ) {
                        Icon(
                            imageVector = if (isPadLockActive) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Pad Lock",
                            tint = if (isPadLockActive) Color(0xFFFF5252) else Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Cue / Solo Mode Toggle (5.4 Cue Solo)
                    IconButton(
                        onClick = { viewModel.toggleCueSoloMode() },
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isCueSoloMode) Color(0xFFFFD600).copy(alpha = 0.25f) else Color(0xFF1F2430))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = "Cue Solo Mode",
                            tint = if (isCueSoloMode) Color(0xFFFFD600) else Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Replay Buffer trigger (4.3 Replay Buffer)
                    if (obsState == com.sounddeck.core.model.ObsConnectionState.AUTHENTICATED) {
                        IconButton(
                            onClick = { viewModel.obsManager.saveReplayBuffer() },
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1F2430))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay,
                                contentDescription = "Save Replay Buffer",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            if (isLeftHandedMode) {
                stopAudioButton()
            } else {
                serviceControls()
            }

            // Edit Mode Controls: Undo, Redo (E3), and Add Pad
            if (isEditMode) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = canUndo,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (canUndo) Color(0xFF1F2430) else Color(0xFF14171E))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Deshacer (E3)",
                            tint = if (canUndo) Color(0xFF00E5FF) else Color.DarkGray,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = canRedo,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (canRedo) Color(0xFF1F2430) else Color(0xFF14171E))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Redo,
                            contentDescription = "Rehacer (E3)",
                            tint = if (canRedo) Color(0xFF00E5FF) else Color.DarkGray,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    val activePage = currentManifest?.pages?.getOrNull(activePageIndex)
                    Button(
                        onClick = {
                            val row = (activePage?.pads?.size ?: 0) % (activePage?.gridRows ?: 3)
                            val col = (activePage?.pads?.size ?: 0) / (activePage?.gridRows ?: 3)
                            viewModel.addPadAt(row = row, col = col.coerceAtMost((activePage?.gridCols ?: 4) - 1))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600)),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Pad", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (isLeftHandedMode) {
                serviceControls()
            } else {
                stopAudioButton()
            }
        }
    }

    // Dialogs
    editingPad?.let { pad ->
        PadEditorDialog(
            pad = pad,
            builtInSounds = viewModel.audioEngine.getBuiltInSounds(),
            obsScenes = obsScenes,
            obsInputs = obsInputs,
            onDismiss = { editingPad = null },
            onSave = { updated ->
                viewModel.updatePad(updated)
                editingPad = null
            },
            onDelete = { padId ->
                viewModel.deletePad(padId)
                editingPad = null
            },
            onDuplicate = { padToDup ->
                viewModel.duplicatePad(padToDup)
                editingPad = null
            },
            onTestHttpAction = { action ->
                viewModel.testHttpRequest(action)
            }
        )
    }

    if (showObsSettings && currentManifest != null) {
        ObsSettingsDialog(
            config = currentManifest.globalSettings.obs,
            connectionState = obsState,
            lastMessage = obsMessage,
            streamStats = obsStreamStats,
            discoveredInstances = discoveredObsInstances,
            isDiscovering = isObsDiscovering,
            obsScenes = obsScenes,
            obsInputs = obsInputs,
            onStartDiscovery = { viewModel.startObsDiscovery() },
            onStopDiscovery = { viewModel.stopObsDiscovery() },
            onDismiss = { showObsSettings = false },
            onConnect = { config -> viewModel.connectObs(config) },
            onDisconnect = { viewModel.disconnectObs() }
        )
    }

    if (showGeneralSettings && currentManifest != null) {
        GeneralSettingsDialog(
            globalSettings = currentManifest.globalSettings,
            manifest = currentManifest,
            onSaveHapticIntensity = { intensity ->
                viewModel.setHapticIntensity(intensity)
            },
            onSaveDimScreenTimeout = { timeoutSec ->
                viewModel.setDimScreenTimeout(timeoutSec)
            },
            onSaveConfirmStopStream = { confirm ->
                viewModel.setConfirmStopStream(confirm)
            },
            onSaveEnvironmentVariables = { vars ->
                viewModel.updateEnvironmentVariables(vars)
            },
            onDismiss = { showGeneralSettings = false }
        )
    }

    if (pendingStopStreamConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelStopStream() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF5252))
                    Text("¿Detener Streaming?", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "OBS Studio está emitiendo en vivo. ¿Deseas detener la transmisión?",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmStopStream() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3D00))
                ) {
                    Text("Detener Emisión", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelStopStream() }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF161920)
        )
    }

    if (showExportImport) {
        ExportImportDialog(
            onDismiss = { showExportImport = false },
            onExportPackage = { viewModel.exportPackage() },
            onImportPackage = { uri ->
                viewModel.importPackage(uri)
                showExportImport = false
            },
            onRestoreDefault = {
                viewModel.restoreDefaultManifest()
                showExportImport = false
            },
            exportStatusMessage = exportStatusMessage,
            backups = backups,
            onCreateBackup = { viewModel.createManualBackup() },
            onRestoreBackup = { viewModel.restoreBackup(it) },
            onDeleteBackup = { viewModel.deleteBackup(it) }
        )
    }

    if (showDiagnosticLogs) {
        DiagnosticLogsDialog(
            onDismiss = { showDiagnosticLogs = false }
        )
    }

    if (showHttpHistory) {
        HttpHistoryDialog(
            transactions = httpHistory,
            onDismiss = { showHttpHistory = false }
        )
    }

    if (showAddPageDialog) {
        var pageName by remember { mutableStateOf("") }
        var rows by remember { mutableStateOf("3") }
        var cols by remember { mutableStateOf("4") }

        AlertDialog(
            onDismissRequest = { showAddPageDialog = false },
            title = { Text("Add New Board Page", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = pageName,
                        onValueChange = { pageName = it },
                        label = { Text("Page Name (e.g. Stream FX, Soundboard)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = rows,
                            onValueChange = { rows = it },
                            label = { Text("Rows (1-12)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = cols,
                            onValueChange = { cols = it },
                            label = { Text("Cols (1-12)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val r = rows.toIntOrNull()?.coerceIn(1, 12) ?: 3
                        val c = cols.toIntOrNull()?.coerceIn(1, 12) ?: 4
                        viewModel.addPage(pageName.ifBlank { "Page ${currentManifest?.pages?.size?.plus(1) ?: 1}" }, r, c)
                        showAddPageDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("Create Page", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPageDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF161920)
        )
    }

    if (showManagePagesDialog && currentManifest != null) {
        PageManagerDialog(
            pages = currentManifest.pages,
            activePageIndex = activePageIndex,
            onSelectPage = { idx ->
                viewModel.selectPage(idx)
            },
            onReorderPage = { from, to ->
                viewModel.movePage(from, to)
            },
            onRenamePage = { pageId, newName ->
                viewModel.renamePage(pageId, newName)
            },
            onDeletePage = { pageId ->
                viewModel.deletePage(pageId)
            },
            onAddNewPage = {
                showAddPageDialog = true
            },
            onUpdatePageColor = { pageId, colorHex ->
                viewModel.updatePageColor(pageId, colorHex)
            },
            onDismiss = { showManagePagesDialog = false }
        )
    }
}
