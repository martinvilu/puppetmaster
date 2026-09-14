package com.sounddeck.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sounddeck.core.audio.AudioEngineModule
import com.sounddeck.core.http.HttpDispatcher
import com.sounddeck.core.model.DynamicPadState
import com.sounddeck.core.model.GridPosition
import com.sounddeck.core.model.HttpAction
import com.sounddeck.core.model.LastHttpTransaction
import com.sounddeck.core.model.Manifest
import com.sounddeck.core.model.ObsConfig
import com.sounddeck.core.model.ObsConnectionState
import com.sounddeck.core.model.PadConfig
import com.sounddeck.core.model.PageConfig
import com.sounddeck.core.model.VisualConfig
import com.sounddeck.core.obs.ObsManager
import com.sounddeck.core.pack.SoundDeckPackageManager
import com.sounddeck.service.AutomationService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class SoundDeckViewModel(application: Application) : AndroidViewModel(application) {
    private val tag = "SoundDeckViewModel"

    private val packageManager = SoundDeckPackageManager(application)
    val audioEngine = AudioEngineModule(application)
    val obsManager = ObsManager(viewModelScope)
    val httpDispatcher = HttpDispatcher()

    private val _manifest = MutableStateFlow<Manifest?>(null)
    val manifest: StateFlow<Manifest?> = _manifest.asStateFlow()

    private val _activePageIndex = MutableStateFlow(0)
    val activePageIndex: StateFlow<Int> = _activePageIndex.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _isForegroundActive = MutableStateFlow(false)
    val isForegroundActive: StateFlow<Boolean> = _isForegroundActive.asStateFlow()

    private val _masterVolume = MutableStateFlow(1.0f)
    val masterVolume: StateFlow<Float> = _masterVolume.asStateFlow()

    private val _isPadLockActive = MutableStateFlow(false)
    val isPadLockActive: StateFlow<Boolean> = _isPadLockActive.asStateFlow()

    private val _isKioskMode = MutableStateFlow(false)
    val isKioskMode: StateFlow<Boolean> = _isKioskMode.asStateFlow()

    private val _isCueSoloMode = MutableStateFlow(false)
    val isCueSoloMode: StateFlow<Boolean> = _isCueSoloMode.asStateFlow()

    private val _isLeftHandedMode = MutableStateFlow(false)
    val isLeftHandedMode: StateFlow<Boolean> = _isLeftHandedMode.asStateFlow()

    // E3: Undo / Redo history
    private val undoStack = mutableListOf<Manifest>()
    private val redoStack = mutableListOf<Manifest>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // A2: Multi-state toggle index per pad
    private val _padMultiStateIndices = MutableStateFlow<Map<String, Int>>(emptyMap())
    val padMultiStateIndices: StateFlow<Map<String, Int>> = _padMultiStateIndices.asStateFlow()

    // C6: OBS Auto-discovery
    val obsDiscovery = com.sounddeck.core.obs.ObsDiscoveryManager(application)
    val discoveredObsInstances = obsDiscovery.discoveredInstances
    val isDiscoveringObs = obsDiscovery.isDiscovering
    val isObsDiscovering: StateFlow<Boolean> = isDiscoveringObs

    // E5: Live diagnostics
    val diagnosticLogs: StateFlow<List<com.sounddeck.core.model.DiagnosticLog>> =
        com.sounddeck.core.log.DiagnosticLogManager.logsFlow

    // E6: Backups list
    private val _backupsList = MutableStateFlow<List<com.sounddeck.core.model.BackupEntry>>(emptyMap<String, Any>().let { emptyList() })
    val backupsList: StateFlow<List<com.sounddeck.core.model.BackupEntry>> = _backupsList.asStateFlow()
    val backups: StateFlow<List<com.sounddeck.core.model.BackupEntry>> = backupsList

    private val _padCooldowns = MutableStateFlow<Map<String, Long>>(emptyMap())
    val padCooldowns: StateFlow<Map<String, Long>> = _padCooldowns.asStateFlow()

    private val _dynamicPadStates = MutableStateFlow<Map<String, DynamicPadState>>(emptyMap())
    val dynamicPadStates: StateFlow<Map<String, DynamicPadState>> = _dynamicPadStates.asStateFlow()

    private val _exportStatusMessage = MutableStateFlow<String?>(null)
    val exportStatusMessage: StateFlow<String?> = _exportStatusMessage.asStateFlow()

    val obsState: StateFlow<ObsConnectionState> = obsManager.connectionState
    val obsMessage: StateFlow<String> = obsManager.lastMessage
    val obsStreamStats = obsManager.streamStats
    val obsInputMutes = obsManager.inputMuteStates
    val obsCurrentScene = obsManager.currentScene
    val obsScenes = obsManager.scenesList
    val obsInputs = obsManager.inputsList
    val obsVolumeLevels = obsManager.inputVolumeLevels

    val lastHttpTx: StateFlow<LastHttpTransaction?> = httpDispatcher.lastTransaction
    val httpHistory: StateFlow<List<LastHttpTransaction>> = httpDispatcher.transactionHistory

    // QoL States
    private val _copiedVisualConfig = MutableStateFlow<VisualConfig?>(null)
    val copiedVisualConfig: StateFlow<VisualConfig?> = _copiedVisualConfig.asStateFlow()

    private val _activeMacroDelays = MutableStateFlow<Map<String, Float>>(emptyMap())
    val activeMacroDelays: StateFlow<Map<String, Float>> = _activeMacroDelays.asStateFlow()

    private val activeLoopJobs = mutableMapOf<String, Job>()
    private val _activeLoopingPads = MutableStateFlow<Set<String>>(emptySet())
    val activeLoopingPads: StateFlow<Set<String>> = _activeLoopingPads.asStateFlow()

    private val _pendingStopStreamConfirmation = MutableStateFlow(false)
    val pendingStopStreamConfirmation: StateFlow<Boolean> = _pendingStopStreamConfirmation.asStateFlow()

    private val pollingJobs = mutableMapOf<String, Job>()

    init {
        loadInitialData()
        observeObsStateForService()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val loaded = packageManager.loadManifest()
            _manifest.value = loaded
            _masterVolume.value = loaded.globalSettings.audioEngine.masterVolume
            audioEngine.setMasterVolume(loaded.globalSettings.audioEngine.masterVolume)
            _isLeftHandedMode.value = loaded.globalSettings.leftHandedMode
            loadBackups()

            // Auto-connect OBS if configured
            if (loaded.globalSettings.obs.autoReconnect && loaded.globalSettings.obs.host.isNotBlank()) {
                obsManager.connect(loaded.globalSettings.obs)
            }

            restartPollingEngine(loaded)
            com.sounddeck.core.log.DiagnosticLogManager.i("System", "SoundDeck iniciado correctamente")
        }
    }

    private fun recordUndoState() {
        _manifest.value?.let {
            undoStack.add(it)
            if (undoStack.size > 30) undoStack.removeAt(0)
            redoStack.clear()
            _canUndo.value = undoStack.isNotEmpty()
            _canRedo.value = false
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val current = _manifest.value ?: return
        redoStack.add(current)
        val previous = undoStack.removeAt(undoStack.lastIndex)
        _manifest.value = previous
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        saveCurrentManifest()
        restartPollingEngine(previous)
        com.sounddeck.core.log.DiagnosticLogManager.i("UndoRedo", "Deshacer ejecutado")
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val current = _manifest.value ?: return
        undoStack.add(current)
        val next = redoStack.removeAt(redoStack.lastIndex)
        _manifest.value = next
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        saveCurrentManifest()
        restartPollingEngine(next)
        com.sounddeck.core.log.DiagnosticLogManager.i("UndoRedo", "Rehacer ejecutado")
    }

    fun toggleLeftHandedMode() {
        val current = _manifest.value ?: return
        val next = !_isLeftHandedMode.value
        _isLeftHandedMode.value = next
        val updated = current.copy(
            globalSettings = current.globalSettings.copy(leftHandedMode = next)
        )
        _manifest.value = updated
        saveCurrentManifest()
        com.sounddeck.core.log.DiagnosticLogManager.i("Settings", "Modo zurdo / mano izquierda: $next")
    }

    fun loadBackups() {
        viewModelScope.launch {
            _backupsList.value = packageManager.listBackups()
        }
    }

    fun createManualBackup() {
        viewModelScope.launch {
            recordUndoState()
            val success = packageManager.createManualBackup()
            if (success) {
                _exportStatusMessage.value = "Copia de seguridad manual creada"
                com.sounddeck.core.log.DiagnosticLogManager.i("Backup", "Copia de seguridad manual creada")
                loadBackups()
            } else {
                _exportStatusMessage.value = "Error al crear copia de seguridad"
                com.sounddeck.core.log.DiagnosticLogManager.e("Backup", "Fallo al crear copia")
            }
        }
    }

    fun restoreBackup(fileName: String) {
        viewModelScope.launch {
            recordUndoState()
            val restored = packageManager.restoreBackup(fileName)
            if (restored != null) {
                _manifest.value = restored
                _activePageIndex.value = 0
                _exportStatusMessage.value = "Copia restaurada: $fileName"
                com.sounddeck.core.log.DiagnosticLogManager.i("Backup", "Restaurada copia $fileName")
                restartPollingEngine(restored)
            } else {
                _exportStatusMessage.value = "Error al restaurar copia"
                com.sounddeck.core.log.DiagnosticLogManager.e("Backup", "Fallo al restaurar $fileName")
            }
        }
    }

    fun deleteBackup(fileName: String) {
        viewModelScope.launch {
            val success = packageManager.deleteBackup(fileName)
            if (success) {
                _exportStatusMessage.value = "Copia eliminada: $fileName"
                com.sounddeck.core.log.DiagnosticLogManager.i("Backup", "Eliminada copia $fileName")
                loadBackups()
            }
        }
    }

    fun startObsDiscovery() {
        obsDiscovery.startDiscovery()
        com.sounddeck.core.log.DiagnosticLogManager.i("OBS-mDNS", "Iniciando escaneo mDNS...")
    }

    fun stopObsDiscovery() {
        obsDiscovery.stopDiscovery()
    }

    fun clearDiagnosticLogs() {
        com.sounddeck.core.log.DiagnosticLogManager.clear()
    }

    private fun observeObsStateForService() {
        viewModelScope.launch {
            obsManager.connectionState.collect { state ->
                if (_isForegroundActive.value) {
                    val activePollingCount = countActivePollingPads()
                    AutomationService.updateService(
                        getApplication(),
                        state.name,
                        activePollingCount
                    )
                }
            }
        }
    }

    fun selectPage(index: Int) {
        val count = _manifest.value?.pages?.size ?: 0
        if (index in 0 until count) {
            _activePageIndex.value = index
        }
    }

    fun addPage(name: String, rows: Int = 3, cols: Int = 4) {
        val current = _manifest.value ?: return
        recordUndoState()
        val newPage = PageConfig(
            id = "page_${UUID.randomUUID().toString().take(8)}",
            name = name.ifBlank { "New Page" },
            gridRows = rows,
            gridCols = cols,
            pads = emptyList()
        )
        val updated = current.copy(pages = current.pages + newPage)
        _manifest.value = updated
        _activePageIndex.value = updated.pages.size - 1
        saveCurrentManifest()
    }

    fun renamePage(pageId: String, newName: String) {
        val current = _manifest.value ?: return
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        recordUndoState()
        val updatedPages = current.pages.map {
            if (it.id == pageId) it.copy(name = trimmed) else it
        }
        val updated = current.copy(pages = updatedPages)
        _manifest.value = updated
        saveCurrentManifest()
    }

    fun movePage(fromIndex: Int, toIndex: Int) {
        val current = _manifest.value ?: return
        val pages = current.pages.toMutableList()
        if (fromIndex !in pages.indices || toIndex !in pages.indices || fromIndex == toIndex) return

        recordUndoState()
        val activePageId = pages.getOrNull(_activePageIndex.value)?.id
        val item = pages.removeAt(fromIndex)
        pages.add(toIndex, item)

        val updated = current.copy(pages = pages)
        _manifest.value = updated

        val newActiveIndex = if (activePageId != null) {
            pages.indexOfFirst { it.id == activePageId }.takeIf { it >= 0 } ?: 0
        } else {
            0
        }
        _activePageIndex.value = newActiveIndex
        saveCurrentManifest()
    }

    fun deletePage(pageId: String) {
        val current = _manifest.value ?: return
        if (current.pages.size <= 1) return
        recordUndoState()
        val updatedPages = current.pages.filter { it.id != pageId }
        val updated = current.copy(pages = updatedPages)
        _manifest.value = updated
        _activePageIndex.value = (_activePageIndex.value).coerceAtMost(updated.pages.size - 1)
        saveCurrentManifest()
    }

    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
    }

    fun setMasterVolume(volume: Float) {
        val clamped = volume.coerceIn(0.0f, 1.0f)
        _masterVolume.value = clamped
        audioEngine.setMasterVolume(clamped)
        val current = _manifest.value ?: return
        val updated = current.copy(
            globalSettings = current.globalSettings.copy(
                audioEngine = current.globalSettings.audioEngine.copy(masterVolume = clamped)
            )
        )
        _manifest.value = updated
        saveCurrentManifest()
    }

    fun togglePadLock() {
        _isPadLockActive.value = !_isPadLockActive.value
    }

    fun toggleKioskMode() {
        _isKioskMode.value = !_isKioskMode.value
    }

    fun toggleCueSoloMode() {
        _isCueSoloMode.value = !_isCueSoloMode.value
    }

    fun triggerPadTap(pad: PadConfig) {
        if (_isPadLockActive.value) return // Blocked if Pad Lock active

        // Check Cooldown
        val now = System.currentTimeMillis()
        val expiry = _padCooldowns.value[pad.id] ?: 0L
        if (now < expiry) return // Pad is on cooldown

        if (pad.visual.cooldownMs > 0L) {
            val updatedMap = _padCooldowns.value.toMutableMap()
            updatedMap[pad.id] = now + pad.visual.cooldownMs
            _padCooldowns.value = updatedMap
            viewModelScope.launch {
                delay(pad.visual.cooldownMs)
                val m = _padCooldowns.value.toMutableMap()
                m.remove(pad.id)
                _padCooldowns.value = m
            }
        }

        // 4.5 Loop mode handling
        if (pad.isLooping) {
            togglePadLoop(pad)
            return
        }

        // A2: Ciclos de Acciones Secuenciales (Toggle Multi-Estado)
        if (pad.multiStates.isNotEmpty()) {
            val currentIndex = _padMultiStateIndices.value[pad.id] ?: 0
            val currentStep = pad.multiStates[currentIndex % pad.multiStates.size]
            val nextIndex = (currentIndex + 1) % pad.multiStates.size
            _padMultiStateIndices.value = _padMultiStateIndices.value + (pad.id to nextIndex)
            com.sounddeck.core.log.DiagnosticLogManager.i(
                "MultiState",
                "Pad ${pad.id} transitioned to state ${nextIndex + 1}/${pad.multiStates.size}: ${currentStep.label ?: "Step $nextIndex"}"
            )
            val pipeline = currentStep.pipeline ?: pad.onTap ?: return
            executePipeline(pipeline, pad.id)
            return
        }

        val pipeline = pad.onTap ?: return
        executePipeline(pipeline, pad.id)
    }

    // 2.3 Hold-To-Play Support
    fun onPadPressStart(pad: PadConfig) {
        if (pad.holdToPlay) {
            val audio = pad.onTap?.audio ?: return
            if (audio.assetPath.isNotBlank()) {
                val effectiveMaster = if (_isCueSoloMode.value) 0.5f else _masterVolume.value
                audioEngine.playByAssetPath(
                    assetPath = audio.assetPath,
                    gain = audio.gain,
                    masterVolume = effectiveMaster,
                    fadeInMs = audio.fadeInMs
                )
            }
        }
    }

    fun onPadPressEnd(pad: PadConfig) {
        if (pad.holdToPlay) {
            audioEngine.stopPlayback()
        }
    }

    // 4.5 Loop Mode Support
    fun togglePadLoop(pad: PadConfig) {
        if (activeLoopJobs.containsKey(pad.id)) {
            activeLoopJobs[pad.id]?.cancel()
            activeLoopJobs.remove(pad.id)
            _activeLoopingPads.value = _activeLoopingPads.value - pad.id
            com.sounddeck.core.log.DiagnosticLogManager.i("Loop", "Loop detenido para pad ${pad.id}")
        } else {
            _activeLoopingPads.value = _activeLoopingPads.value + pad.id
            val intervalMs = (pad.loopIntervalSeconds * 1000L).coerceAtLeast(500L)
            val job = viewModelScope.launch {
                com.sounddeck.core.log.DiagnosticLogManager.i("Loop", "Iniciando loop cada ${pad.loopIntervalSeconds}s para ${pad.id}")
                while (isActive) {
                    pad.onTap?.let { executePipeline(it, pad.id) }
                    delay(intervalMs)
                }
            }
            activeLoopJobs[pad.id] = job
        }
    }

    // 1.1 Swap Pads (Drag & Drop Reordering)
    fun swapPads(padAId: String, padBId: String) {
        val current = _manifest.value ?: return
        val pageIdx = _activePageIndex.value
        if (pageIdx !in current.pages.indices) return
        val page = current.pages[pageIdx]
        val padA = page.pads.find { it.id == padAId } ?: return
        val padB = page.pads.find { it.id == padBId } ?: return

        recordUndoState()
        val posA = padA.position
        val posB = padB.position
        val updatedPads = page.pads.map {
            when (it.id) {
                padAId -> it.copy(position = posB)
                padBId -> it.copy(position = posA)
                else -> it
            }
        }
        val updatedPages = current.pages.toMutableList()
        updatedPages[pageIdx] = page.copy(pads = updatedPads)
        _manifest.value = current.copy(pages = updatedPages)
        saveCurrentManifest()
        _exportStatusMessage.value = "Posiciones intercambiadas"
        com.sounddeck.core.log.DiagnosticLogManager.i("Grid", "Pads intercambiados: ${padA.visual.label} <-> ${padB.visual.label}")
    }

    // 1.3 Copy & Paste Styles
    fun copyPadStyle(pad: PadConfig) {
        _copiedVisualConfig.value = pad.visual
        _exportStatusMessage.value = "Estilo copiado de \"${pad.visual.label.ifBlank { "Pad" }}\""
        com.sounddeck.core.log.DiagnosticLogManager.i("Style", "Estilo copiado de ${pad.id}")
    }

    fun pastePadStyle(targetPad: PadConfig) {
        val style = _copiedVisualConfig.value ?: return
        val current = _manifest.value ?: return
        val pageIdx = _activePageIndex.value
        if (pageIdx !in current.pages.indices) return

        recordUndoState()
        val page = current.pages[pageIdx]
        val updatedPads = page.pads.map {
            if (it.id == targetPad.id) {
                it.copy(
                    visual = style.copy(label = it.visual.label)
                )
            } else it
        }
        val updatedPages = current.pages.toMutableList()
        updatedPages[pageIdx] = page.copy(pads = updatedPads)
        _manifest.value = current.copy(pages = updatedPages)
        saveCurrentManifest()
        _exportStatusMessage.value = "Estilo pegado en \"${targetPad.visual.label.ifBlank { "Pad" }}\""
        com.sounddeck.core.log.DiagnosticLogManager.i("Style", "Estilo pegado en ${targetPad.id}")
    }

    // 3.4 Confirm Stop Stream
    fun confirmStopStream() {
        _pendingStopStreamConfirmation.value = false
        obsManager.toggleStream()
        com.sounddeck.core.log.DiagnosticLogManager.w("OBS", "Transmisión en vivo detenida tras confirmación")
    }

    fun cancelStopStream() {
        _pendingStopStreamConfirmation.value = false
    }

    // 6.4 Key Binding Shortcuts
    fun triggerPadByKeyShortcut(shortcutKey: String): Boolean {
        val current = _manifest.value ?: return false
        val pageIdx = _activePageIndex.value
        if (pageIdx !in current.pages.indices) return false
        val page = current.pages[pageIdx]
        val matched = page.pads.find {
            it.keyShortcut != null && it.keyShortcut.equals(shortcutKey, ignoreCase = true)
        } ?: return false

        triggerPadTap(matched)
        com.sounddeck.core.log.DiagnosticLogManager.i("KeyBinding", "Acción disparada por atajo: $shortcutKey")
        return true
    }

    // 4.3 Environment Variables
    fun updateEnvironmentVariables(vars: Map<String, String>) {
        val current = _manifest.value ?: return
        recordUndoState()
        val updated = current.copy(
            globalSettings = current.globalSettings.copy(environmentVariables = vars)
        )
        _manifest.value = updated
        saveCurrentManifest()
        com.sounddeck.core.log.DiagnosticLogManager.i("Settings", "Variables de entorno guardadas (${vars.size})")
    }

    // 4.1 Test HTTP Request
    suspend fun testHttpRequest(action: HttpAction): LastHttpTransaction {
        val envVars = _manifest.value?.globalSettings?.environmentVariables ?: emptyMap()
        return httpDispatcher.testRequest(
            action = action,
            contextVariables = envVars
        )
    }

    // 5.5 Page Color
    fun updatePageColor(pageId: String, colorHex: String) {
        val current = _manifest.value ?: return
        recordUndoState()
        val updatedPages = current.pages.map {
            if (it.id == pageId) it.copy(color = colorHex) else it
        }
        _manifest.value = current.copy(pages = updatedPages)
        saveCurrentManifest()
    }

    // 6.1 Haptic Intensity
    fun setHapticIntensity(intensity: com.sounddeck.core.model.HapticIntensity) {
        val current = _manifest.value ?: return
        val updated = current.copy(
            globalSettings = current.globalSettings.copy(hapticIntensity = intensity)
        )
        _manifest.value = updated
        saveCurrentManifest()
        com.sounddeck.core.log.DiagnosticLogManager.i("Settings", "Intensidad háptica: $intensity")
    }

    // 6.2 Screen Dim Timeout
    fun setDimScreenTimeout(seconds: Int) {
        val current = _manifest.value ?: return
        val updated = current.copy(
            globalSettings = current.globalSettings.copy(dimScreenAfterSeconds = seconds)
        )
        _manifest.value = updated
        saveCurrentManifest()
    }

    // 3.4 Confirm Stop Stream
    fun setConfirmStopStream(enabled: Boolean) {
        val current = _manifest.value ?: return
        val updated = current.copy(
            globalSettings = current.globalSettings.copy(confirmStopStream = enabled)
        )
        _manifest.value = updated
        saveCurrentManifest()
    }

    // 5.5 Hold to Play (Release stops audio)
    fun triggerPadRelease(pad: PadConfig) {
        if (pad.holdToPlay) {
            audioEngine.stopPlayback()
        }
    }

    fun triggerPadLongPress(pad: PadConfig) {
        if (_isPadLockActive.value) return

        val pipeline = pad.onLongPress ?: pad.onTap ?: return
        executePipeline(pipeline, pad.id)
    }

    private fun executePipeline(pipeline: com.sounddeck.core.model.ActionPipeline, padId: String) {
        viewModelScope.launch {
            // A1: Acciones Condicionales (If-Else en Pipeline)
            val cond = pipeline.condition
            if (cond != null && cond.type != com.sounddeck.core.model.ConditionType.NONE) {
                val isMet = when (cond.type) {
                    com.sounddeck.core.model.ConditionType.OBS_STREAMING -> obsStreamStats.value.isStreaming
                    com.sounddeck.core.model.ConditionType.OBS_NOT_STREAMING -> !obsStreamStats.value.isStreaming
                    com.sounddeck.core.model.ConditionType.OBS_RECORDING -> obsStreamStats.value.isRecording
                    com.sounddeck.core.model.ConditionType.OBS_NOT_RECORDING -> !obsStreamStats.value.isRecording
                    com.sounddeck.core.model.ConditionType.OBS_SCENE_EQUALS -> {
                        obsCurrentScene.value.equals(cond.expectedValue ?: "", ignoreCase = true)
                    }
                    else -> true
                }

                if (!isMet) {
                    com.sounddeck.core.log.DiagnosticLogManager.w(
                        "Pipeline",
                        "Condition ${cond.type} NOT met. Running ELSE pipeline if present."
                    )
                    pipeline.elsePipeline?.let { executePipeline(it, padId) }
                    return@launch
                } else {
                    com.sounddeck.core.log.DiagnosticLogManager.i(
                        "Pipeline",
                        "Condition ${cond.type} MET. Executing main action."
                    )
                }
            }

            // 4.4 Check macro delay before pipeline execution with visual progress
            if (pipeline.macroDelayMs > 0L) {
                val totalMs = pipeline.macroDelayMs
                val steps = 20
                val interval = (totalMs / steps).coerceAtLeast(15L)
                for (i in 1..steps) {
                    val progress = i.toFloat() / steps
                    _activeMacroDelays.value = _activeMacroDelays.value + (padId to progress)
                    delay(interval)
                }
                _activeMacroDelays.value = _activeMacroDelays.value - padId
            }

            // 1. Audio Action
            pipeline.audio?.let { audio ->
                if (audio.assetPath.isBlank()) {
                    audioEngine.stopPlayback()
                } else {
                    // In Cue / Solo mode, play at lower or headphone-style solo gain
                    val effectiveMaster = if (_isCueSoloMode.value) 0.5f else _masterVolume.value
                    audioEngine.playByAssetPath(
                        assetPath = audio.assetPath,
                        gain = audio.gain,
                        masterVolume = effectiveMaster,
                        fadeInMs = audio.fadeInMs,
                        fadeOutMs = audio.fadeOutMs
                    )
                }
            }

            // 2. OBS Action (with 3.4 Confirm Stop Stream)
            pipeline.obsAction?.let { obsAction ->
                val confirmRequired = _manifest.value?.globalSettings?.confirmStopStream ?: true
                if (confirmRequired && obsStreamStats.value.isStreaming &&
                    (obsAction.requestType == "StopStream" || obsAction.requestType == "ToggleStream")
                ) {
                    _pendingStopStreamConfirmation.value = true
                } else {
                    obsManager.sendRequest(
                        requestType = obsAction.requestType,
                        requestData = obsAction.requestData
                    )
                }
            }

            // 3. HTTP Action (with 4.3 Environment Variables)
            pipeline.httpAction?.let { httpAction ->
                val envVars = _manifest.value?.globalSettings?.environmentVariables ?: emptyMap()
                val dynamicState = httpDispatcher.execute(
                    action = httpAction,
                    contextVariables = mapOf("pad_id" to padId) + envVars
                )
                updatePadDynamicState(padId, dynamicState)
            }
        }
    }

    fun retryHttpTransaction(tx: LastHttpTransaction) {
        viewModelScope.launch {
            val action = HttpAction(
                url = tx.url,
                method = tx.method,
                headers = tx.requestHeaders,
                body = tx.requestBody
            )
            httpDispatcher.execute(action)
        }
    }

    fun duplicatePad(pad: PadConfig) {
        val current = _manifest.value ?: return
        val pageIdx = _activePageIndex.value
        if (pageIdx !in current.pages.indices) return
        val page = current.pages[pageIdx]

        // Find next available slot
        val occupied = page.pads.map { it.position.row to it.position.col }.toSet()
        var targetRow = pad.position.row
        var targetCol = pad.position.col + 1
        if (targetCol >= page.gridCols) {
            targetCol = 0
            targetRow++
        }
        while (occupied.contains(targetRow to targetCol) && targetRow < page.gridRows) {
            targetCol++
            if (targetCol >= page.gridCols) {
                targetCol = 0
                targetRow++
            }
        }
        if (targetRow >= page.gridRows) {
            targetRow = pad.position.row
            targetCol = pad.position.col
        }

        val duplicated = pad.copy(
            id = "pad_${UUID.randomUUID().toString().take(8)}",
            position = GridPosition(
                row = targetRow,
                col = targetCol,
                rowSpan = pad.position.rowSpan,
                colSpan = pad.position.colSpan
            ),
            visual = pad.visual.copy(label = "${pad.visual.label} (Copy)")
        )
        updatePad(duplicated)
    }

    fun triggerPadByKeyIndex(index: Int) {
        val current = _manifest.value ?: return
        val pageIdx = _activePageIndex.value
        if (pageIdx !in current.pages.indices) return
        val pads = current.pages[pageIdx].pads
        if (index in pads.indices) {
            triggerPadTap(pads[index])
        }
    }

    private fun updatePadDynamicState(padId: String, state: DynamicPadState) {
        val current = _dynamicPadStates.value.toMutableMap()
        current[padId] = state
        _dynamicPadStates.value = current
    }

    fun updatePad(updatedPad: PadConfig) {
        val current = _manifest.value ?: return
        recordUndoState()
        val currentPages = current.pages.toMutableList()
        val pageIdx = _activePageIndex.value
        if (pageIdx !in currentPages.indices) return

        val page = currentPages[pageIdx]
        val existingIndex = page.pads.indexOfFirst { it.id == updatedPad.id }

        val updatedPads = if (existingIndex >= 0) {
            page.pads.toMutableList().apply { set(existingIndex, updatedPad) }
        } else {
            page.pads + updatedPad
        }

        currentPages[pageIdx] = page.copy(pads = updatedPads)
        val newManifest = current.copy(pages = currentPages)
        _manifest.value = newManifest
        saveCurrentManifest()
        restartPollingEngine(newManifest)
    }

    fun addPadAt(row: Int, col: Int) {
        val current = _manifest.value ?: return
        val pageIdx = _activePageIndex.value
        if (pageIdx !in current.pages.indices) return

        val newPad = PadConfig(
            id = "pad_${UUID.randomUUID().toString().take(8)}",
            position = GridPosition(row = row, col = col),
            visual = VisualConfig(
                label = "New Pad",
                backgroundColor = "#1E1E1E",
                textColor = "#FFFFFF"
            )
        )
        updatePad(newPad)
    }

    fun deletePad(padId: String) {
        val current = _manifest.value ?: return
        val pageIdx = _activePageIndex.value
        if (pageIdx !in current.pages.indices) return

        recordUndoState()
        val page = currentPages_safe(current, pageIdx)
        val updatedPads = page.pads.filter { it.id != padId }
        val currentPages = current.pages.toMutableList()
        currentPages[pageIdx] = page.copy(pads = updatedPads)

        val newManifest = current.copy(pages = currentPages)
        _manifest.value = newManifest
        saveCurrentManifest()
        pollingJobs[padId]?.cancel()
        pollingJobs.remove(padId)
    }

    private fun currentPages_safe(manifest: Manifest, pageIdx: Int): PageConfig {
        return manifest.pages[pageIdx]
    }

    fun connectObs(config: ObsConfig) {
        val current = _manifest.value ?: return
        val updated = current.copy(
            globalSettings = current.globalSettings.copy(obs = config)
        )
        _manifest.value = updated
        saveCurrentManifest()
        obsManager.connect(config)
    }

    fun disconnectObs() {
        obsManager.disconnect()
    }

    fun reconnectObs() {
        obsManager.reconnect()
    }

    fun toggleForegroundService() {
        val next = !_isForegroundActive.value
        _isForegroundActive.value = next
        if (next) {
            val activePollingCount = countActivePollingPads()
            AutomationService.startService(
                getApplication(),
                obsState.value.name,
                activePollingCount
            )
        } else {
            AutomationService.stopService(getApplication())
        }
    }

    fun exportPackage() {
        viewModelScope.launch {
            val exportDir = getApplication<Application>().filesDir
            val targetFile = File(exportDir, "SoundDeck_Export.zip")
            val success = packageManager.exportPackage(targetFile)
            if (success) {
                _exportStatusMessage.value = "Exported successfully: ${targetFile.absolutePath} (${targetFile.length() / 1024} KB)"
            } else {
                _exportStatusMessage.value = "Export failed."
            }
        }
    }

    fun importPackage(uri: Uri) {
        viewModelScope.launch {
            val imported = packageManager.importPackage(uri)
            if (imported != null) {
                _manifest.value = imported
                _activePageIndex.value = 0
                _exportStatusMessage.value = "Package imported successfully!"
                restartPollingEngine(imported)
            } else {
                _exportStatusMessage.value = "Import failed: invalid package or schema."
            }
        }
    }

    fun restoreDefaultManifest() {
        viewModelScope.launch {
            val defaultManifest = packageManager.createDefaultManifest()
            packageManager.saveManifest(defaultManifest)
            _manifest.value = defaultManifest
            _activePageIndex.value = 0
            _exportStatusMessage.value = "Reset to default soundboard template."
            restartPollingEngine(defaultManifest)
        }
    }

    private fun restartPollingEngine(manifest: Manifest) {
        pollingJobs.values.forEach { it.cancel() }
        pollingJobs.clear()

        manifest.pages.forEach { page ->
            page.pads.forEach { pad ->
                val polling = pad.polling
                if (polling != null && polling.enabled) {
                    val job = viewModelScope.launch {
                        while (isActive) {
                            try {
                                val state = httpDispatcher.execute(polling.request)
                                updatePadDynamicState(pad.id, state)
                            } catch (e: Exception) {
                                Log.e(tag, "Polling failed for pad ${pad.id}: ${e.message}")
                            }
                            delay(polling.intervalMs.coerceAtLeast(250L))
                        }
                    }
                    pollingJobs[pad.id] = job
                }
            }
        }

        if (_isForegroundActive.value) {
            AutomationService.updateService(
                getApplication(),
                obsState.value.name,
                countActivePollingPads()
            )
        }
    }

    private fun countActivePollingPads(): Int {
        return _manifest.value?.pages?.flatMap { it.pads }?.count { it.polling?.enabled == true } ?: 0
    }

    private fun saveCurrentManifest() {
        viewModelScope.launch {
            _manifest.value?.let { packageManager.saveManifest(it) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
        obsManager.disconnect()
        pollingJobs.values.forEach { it.cancel() }
    }
}
