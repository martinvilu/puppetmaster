package com.sounddeck.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Manifest(
    val version: Int = 1,
    val globalSettings: GlobalSettings,
    val pages: List<PageConfig>
)

@Serializable
enum class HapticIntensity {
    OFF, LIGHT, MEDIUM, STRONG
}

@Serializable
data class GlobalSettings(
    val obs: ObsConfig,
    val httpDefaults: HttpDefaults = HttpDefaults(),
    val audioEngine: AudioEngineConfig = AudioEngineConfig(),
    val leftHandedMode: Boolean = false,
    val environmentVariables: Map<String, String> = emptyMap(),
    val hapticIntensity: HapticIntensity = HapticIntensity.MEDIUM,
    val dimScreenAfterSeconds: Int = 120,
    val confirmStopStream: Boolean = true
)

@Serializable
data class ObsConfig(
    val host: String = "192.168.1.100",
    val port: Int = 4455,
    val password: String = "",
    val autoReconnect: Boolean = true,
    val reconnectIntervalMs: Long = 3000L
)

@Serializable
data class HttpDefaults(
    val defaultTimeoutMs: Long = 5000L,
    val baseHeaders: Map<String, String> = emptyMap()
)

@Serializable
data class AudioEngineConfig(
    val mode: String = "EXCLUSIVE_MONO",
    val masterVolume: Float = 1.0f
)

@Serializable
data class GridPosition(
    val row: Int,
    val col: Int,
    val rowSpan: Int = 1,
    val colSpan: Int = 1
)

@Serializable
data class PageConfig(
    val id: String,
    val name: String,
    val gridRows: Int = 3,
    val gridCols: Int = 4,
    val pads: List<PadConfig> = emptyList(),
    val color: String = "#00E5FF"
)

@Serializable
data class PadConfig(
    val id: String,
    val position: GridPosition,
    val visual: VisualConfig,
    val onTap: ActionPipeline? = null,
    val onLongPress: ActionPipeline? = null,
    val polling: PollingConfig? = null,
    val multiStates: List<PadStateStep> = emptyList(),
    val holdToPlay: Boolean = false,
    val isLooping: Boolean = false,
    val loopIntervalSeconds: Int = 5,
    val keyShortcut: String? = null
)

@Serializable
data class PadStateStep(
    val label: String? = null,
    val backgroundColor: String? = null,
    val iconAsset: String? = null,
    val pipeline: ActionPipeline? = null
)

@Serializable
enum class LabelAlignment {
    TOP, CENTER, BOTTOM
}

@Serializable
data class VisualConfig(
    val label: String,
    val backgroundColor: String = "#1E1E1E",
    val secondaryColor: String? = null,
    val borderColor: String? = null,
    val borderWidthDp: Int = 1,
    val textColor: String = "#FFFFFF",
    val iconAsset: String? = null,
    val imageUri: String? = null,
    val hapticFeedback: Boolean = true,
    val cooldownMs: Long = 0L,
    val longPressTimeoutMs: Long = 500L,
    val labelAlignment: LabelAlignment = LabelAlignment.BOTTOM,
    val labelFontSizeSp: Int = 11
)

@Serializable
enum class ConditionType {
    NONE,
    OBS_STREAMING,
    OBS_NOT_STREAMING,
    OBS_RECORDING,
    OBS_NOT_RECORDING,
    OBS_SCENE_EQUALS
}

@Serializable
data class PipelineCondition(
    val type: ConditionType = ConditionType.NONE,
    val expectedValue: String? = null
)

@Serializable
data class ActionPipeline(
    val audio: AudioAction? = null,
    val obsAction: ObsAction? = null,
    val httpAction: HttpAction? = null,
    val macroDelayMs: Long = 0L,
    val condition: PipelineCondition? = null,
    val elsePipeline: ActionPipeline? = null
)

@Serializable
data class AudioAction(
    val assetPath: String,
    val gain: Float = 1.0f,
    val fadeInMs: Long = 0L,
    val fadeOutMs: Long = 0L
)

@Serializable
data class ObsAction(
    val requestType: String,
    val requestData: JsonObject? = null
)

@Serializable
data class HttpAction(
    val url: String,
    val method: String = "GET",
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val timeoutMs: Long? = null,
    val responseMapping: ResponseMapping? = null
)

@Serializable
data class PollingConfig(
    val enabled: Boolean = false,
    val intervalMs: Long = 2000L,
    val request: HttpAction,
    val responseMapping: ResponseMapping
)

@Serializable
data class ResponseMapping(
    val jsonPathPercentage: String? = null,
    val jsonPathTextBadge: String? = null,
    val jsonPathIcon: String? = null
)

enum class ObsConnectionState {
    DISCONNECTED,
    CONNECTING,
    AUTHENTICATED
}

data class ObsStreamStats(
    val isStreaming: Boolean = false,
    val isRecording: Boolean = false,
    val fps: Double = 0.0,
    val cpuUsage: Double = 0.0,
    val outputTimecode: String = "",
    val outputBytes: Long = 0L,
    val droppedFrames: Int = 0,
    val totalFrames: Int = 0
)

data class LastHttpTransaction(
    val method: String,
    val url: String,
    val statusCode: Int,
    val durationMs: Long,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val requestHeaders: Map<String, String> = emptyMap(),
    val requestBody: String? = null
)

data class DynamicPadState(
    val percentage: Float? = null,
    val textBadge: String? = null,
    val icon: String? = null
)

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

data class DiagnosticLog(
    val id: Long = System.currentTimeMillis() + (0..999).random(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel = LogLevel.INFO,
    val tag: String,
    val message: String
)

data class BackupEntry(
    val fileName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String = "",
    val fileSizeKb: Long = 0L,
    val sizeBytes: Long = fileSizeKb * 1024L,
    val filePath: String = fileName
) {
    val name: String get() = fileName
}

