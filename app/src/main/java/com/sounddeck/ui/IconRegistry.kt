package com.sounddeck.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Airplay
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SensorsOff
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.ui.graphics.vector.ImageVector

data class SoundDeckIconItem(
    val id: String,
    val name: String,
    val category: String,
    val icon: ImageVector,
    val aliases: List<String> = emptyList()
)

object IconRegistry {
    val CATEGORY_ALL = "Todos"
    val CATEGORY_AUDIO = "Audio & Mic"
    val CATEGORY_STREAM = "OBS & Video"
    val CATEGORY_ALERTS = "Alertas & FX"
    val CATEGORY_CONTROLS = "Controles"

    val categories = listOf(
        CATEGORY_ALL,
        CATEGORY_AUDIO,
        CATEGORY_STREAM,
        CATEGORY_ALERTS,
        CATEGORY_CONTROLS
    )

    val icons: List<SoundDeckIconItem> = listOf(
        // Audio & Mic
        SoundDeckIconItem("volume_up", "Volumen +", CATEGORY_AUDIO, Icons.Default.VolumeUp, listOf("speaker", "audio")),
        SoundDeckIconItem("volume_down", "Volumen -", CATEGORY_AUDIO, Icons.Default.VolumeDown),
        SoundDeckIconItem("volume_off", "Mute Audio", CATEGORY_AUDIO, Icons.Default.VolumeOff, listOf("silencio")),
        SoundDeckIconItem("mic", "Micrófono", CATEGORY_AUDIO, Icons.Default.Mic, listOf("microfono", "talk")),
        SoundDeckIconItem("mic_off", "Mute Mic", CATEGORY_AUDIO, Icons.Default.MicOff, listOf("mute", "silence")),
        SoundDeckIconItem("headset", "Auriculares", CATEGORY_AUDIO, Icons.Default.Headset, listOf("headphones", "audifonos")),
        SoundDeckIconItem("music_note", "Música", CATEGORY_AUDIO, Icons.Default.MusicNote, listOf("soundtrack", "song")),
        SoundDeckIconItem("graphic_eq", "Ecualizador", CATEGORY_AUDIO, Icons.Default.GraphicEq, listOf("equalizer", "spectrum")),
        SoundDeckIconItem("radio", "Radio", CATEGORY_AUDIO, Icons.Default.Radio, listOf("broadcast")),
        SoundDeckIconItem("hearing", "Monitoreo", CATEGORY_AUDIO, Icons.Default.Hearing, listOf("listen")),
        SoundDeckIconItem("podcasts", "Podcast", CATEGORY_AUDIO, Icons.Default.Podcasts),

        // OBS & Video
        SoundDeckIconItem("videocam", "Cámara", CATEGORY_STREAM, Icons.Default.Videocam, listOf("camera", "scene")),
        SoundDeckIconItem("videocam_off", "Cámara Off", CATEGORY_STREAM, Icons.Default.VideocamOff),
        SoundDeckIconItem("live_tv", "En Vivo", CATEGORY_STREAM, Icons.Default.LiveTv, listOf("stream", "broadcast")),
        SoundDeckIconItem("fiber_manual_record", "Grabar (REC)", CATEGORY_STREAM, Icons.Default.FiberManualRecord, listOf("rec", "record")),
        SoundDeckIconItem("screen_share", "Compartir", CATEGORY_STREAM, Icons.Default.ScreenShare, listOf("display", "capture")),
        SoundDeckIconItem("play", "Reproducir", CATEGORY_STREAM, Icons.Default.PlayArrow, listOf("play_arrow", "start")),
        SoundDeckIconItem("pause", "Pausar", CATEGORY_STREAM, Icons.Default.Pause),
        SoundDeckIconItem("stop", "Detener", CATEGORY_STREAM, Icons.Default.Stop),
        SoundDeckIconItem("fast_forward", "Adelantar", CATEGORY_STREAM, Icons.Default.FastForward),
        SoundDeckIconItem("replay", "Repetición", CATEGORY_STREAM, Icons.Default.Replay, listOf("instant_replay")),
        SoundDeckIconItem("camera_alt", "Captura", CATEGORY_STREAM, Icons.Default.CameraAlt, listOf("screenshot")),
        SoundDeckIconItem("airplay", "Airplay", CATEGORY_STREAM, Icons.Default.Airplay),

        // Alertas & FX
        SoundDeckIconItem("celebration", "Celebración", CATEGORY_ALERTS, Icons.Default.Celebration, listOf("party", "applause", "fiesta")),
        SoundDeckIconItem("bolt", "Rayo / FX", CATEGORY_ALERTS, Icons.Default.Bolt, listOf("laser", "thunder")),
        SoundDeckIconItem("error", "Buzzer Error", CATEGORY_ALERTS, Icons.Default.Error, listOf("buzzer", "alert", "fail")),
        SoundDeckIconItem("warning", "Advertencia", CATEGORY_ALERTS, Icons.Default.Warning, listOf("peligro")),
        SoundDeckIconItem("campaign", "Megáfono", CATEGORY_ALERTS, Icons.Default.Campaign, listOf("anuncio", "shout")),
        SoundDeckIconItem("whatshot", "Fuego / Hype", CATEGORY_ALERTS, Icons.Default.Whatshot, listOf("fire", "hype")),
        SoundDeckIconItem("star", "Estrella", CATEGORY_ALERTS, Icons.Default.Star, listOf("highlight")),
        SoundDeckIconItem("notifications", "Campana", CATEGORY_ALERTS, Icons.Default.Notifications, listOf("bell", "ring")),
        SoundDeckIconItem("notifications_active", "Alarma", CATEGORY_ALERTS, Icons.Default.NotificationsActive),
        SoundDeckIconItem("favorite", "Corazón", CATEGORY_ALERTS, Icons.Default.Favorite, listOf("love", "like")),
        SoundDeckIconItem("thumb_up", "Pulgar Arriba", CATEGORY_ALERTS, Icons.Default.ThumbUp, listOf("gg")),
        SoundDeckIconItem("flash_on", "Flash", CATEGORY_ALERTS, Icons.Default.FlashOn),
        SoundDeckIconItem("auto_fix_high", "Efecto Mágico", CATEGORY_ALERTS, Icons.Default.AutoFixHigh, listOf("magic", "sparkle")),

        // Controles & Gaming
        SoundDeckIconItem("sports_esports", "Juegos", CATEGORY_CONTROLS, Icons.Default.SportsEsports, listOf("gamepad", "gaming")),
        SoundDeckIconItem("chat", "Chat", CATEGORY_CONTROLS, Icons.Default.Chat, listOf("chatting", "message")),
        SoundDeckIconItem("cloud_sync", "Sync Webhook", CATEGORY_CONTROLS, Icons.Default.CloudSync, listOf("sync", "http")),
        SoundDeckIconItem("speed", "Velocímetro", CATEGORY_CONTROLS, Icons.Default.Speed, listOf("gauge")),
        SoundDeckIconItem("check_circle", "Éxito / OK", CATEGORY_CONTROLS, Icons.Default.CheckCircle, listOf("success", "check")),
        SoundDeckIconItem("timer", "Temporizador", CATEGORY_CONTROLS, Icons.Default.Timer, listOf("countdown", "clock")),
        SoundDeckIconItem("lightbulb", "Luz de Escena", CATEGORY_CONTROLS, Icons.Default.Lightbulb, listOf("lights", "keylight")),
        SoundDeckIconItem("power_settings_new", "Encender / Apagar", CATEGORY_CONTROLS, Icons.Default.PowerSettingsNew, listOf("power")),
        SoundDeckIconItem("refresh", "Recargar", CATEGORY_CONTROLS, Icons.Default.Refresh, listOf("reload")),
        SoundDeckIconItem("smart_toy", "Bot / AI", CATEGORY_CONTROLS, Icons.Default.SmartToy, listOf("robot", "bot")),
        SoundDeckIconItem("sensors", "Sensores On", CATEGORY_CONTROLS, Icons.Default.Sensors),
        SoundDeckIconItem("sensors_off", "Sensores Off", CATEGORY_CONTROLS, Icons.Default.SensorsOff)
    )

    fun resolveIcon(name: String?): ImageVector? {
        if (name.isNullOrBlank()) return null
        val normalized = name.lowercase().trim()
        val directMatch = icons.find { it.id == normalized || it.aliases.contains(normalized) }
        if (directMatch != null) return directMatch.icon

        // Legacy / fallback mappings
        return when (normalized) {
            "speaker", "audio" -> Icons.Default.VolumeUp
            "applause", "party" -> Icons.Default.Celebration
            "laser" -> Icons.Default.Bolt
            "buzzer", "alert" -> Icons.Default.Error
            "scene", "camera" -> Icons.Default.Videocam
            "chatting" -> Icons.Default.Chat
            "mute" -> Icons.Default.MicOff
            "rec", "record" -> Icons.Default.FiberManualRecord
            "stream" -> Icons.Default.LiveTv
            "sync", "http" -> Icons.Default.CloudSync
            "gauge" -> Icons.Default.Speed
            "success", "check" -> Icons.Default.CheckCircle
            "play_arrow" -> Icons.Default.PlayArrow
            "volume_mute" -> Icons.Default.VolumeMute
            else -> null
        }
    }
}
