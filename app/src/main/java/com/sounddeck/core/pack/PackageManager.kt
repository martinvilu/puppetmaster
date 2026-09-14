package com.sounddeck.core.pack

import android.content.Context
import android.net.Uri
import android.util.Log
import com.sounddeck.core.model.ActionPipeline
import com.sounddeck.core.model.AudioAction
import com.sounddeck.core.model.GlobalSettings
import com.sounddeck.core.model.GridPosition
import com.sounddeck.core.model.HttpAction
import com.sounddeck.core.model.Manifest
import com.sounddeck.core.model.ObsAction
import com.sounddeck.core.model.ObsConfig
import com.sounddeck.core.model.PadConfig
import com.sounddeck.core.model.PageConfig
import com.sounddeck.core.model.PollingConfig
import com.sounddeck.core.model.ResponseMapping
import com.sounddeck.core.model.VisualConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class SoundDeckPackageManager(private val context: Context) {
    private val tag = "SoundDeckPackageManager"

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val manifestFile: File
        get() = File(context.filesDir, "sounddeck_manifest.json")

    fun getAudioMediaDir(): File {
        return File(context.filesDir, "media/audio").apply { if (!exists()) mkdirs() }
    }

    fun getIconsMediaDir(): File {
        return File(context.filesDir, "media/icons").apply { if (!exists()) mkdirs() }
    }

    suspend fun loadManifest(): Manifest = withContext(Dispatchers.IO) {
        if (manifestFile.exists()) {
            try {
                val content = manifestFile.readText()
                val manifest = json.decodeFromString<Manifest>(content)
                if (manifest.pages.isNotEmpty()) {
                    return@withContext manifest
                }
            } catch (e: Exception) {
                Log.e(tag, "Error reading saved manifest: ${e.message}", e)
            }
        }
        val defaultManifest = createDefaultManifest()
        saveManifest(defaultManifest)
        defaultManifest
    }

    suspend fun saveManifest(manifest: Manifest) = withContext(Dispatchers.IO) {
        try {
            val content = json.encodeToString(manifest)
            manifestFile.writeText(content)
            performDailyBackup(content)
        } catch (e: Exception) {
            Log.e(tag, "Error saving manifest: ${e.message}", e)
        }
    }

    fun getBackupsDir(): File {
        return File(context.filesDir, "backups").apply { if (!exists()) mkdirs() }
    }

    private fun performDailyBackup(manifestJson: String) {
        try {
            val backupsDir = getBackupsDir()
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val today = dateFormat.format(java.util.Date())
            val todayFile = File(backupsDir, "backup_$today.json")
            if (!todayFile.exists()) {
                todayFile.writeText(manifestJson)
            }
            // Rotate: keep last 14 daily backups
            val allBackups = backupsDir.listFiles { _, name -> name.startsWith("backup_") && name.endsWith(".json") }
                ?.sortedByDescending { it.lastModified() } ?: emptyList()
            if (allBackups.size > 14) {
                allBackups.drop(14).forEach { it.delete() }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error performing daily backup: ${e.message}", e)
        }
    }

    suspend fun listBackups(): List<com.sounddeck.core.model.BackupEntry> = withContext(Dispatchers.IO) {
        val backupsDir = getBackupsDir()
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        backupsDir.listFiles { _, name -> name.startsWith("backup_") && name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?.map { file ->
                com.sounddeck.core.model.BackupEntry(
                    fileName = file.name,
                    timestamp = file.lastModified(),
                    dateString = dateFormat.format(java.util.Date(file.lastModified())),
                    fileSizeKb = (file.length() / 1024L).coerceAtLeast(1L)
                )
            } ?: emptyList()
    }

    suspend fun createManualBackup(): Boolean = withContext(Dispatchers.IO) {
        try {
            val content = manifestFile.readText()
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd_HHmmss", java.util.Locale.US)
            val now = dateFormat.format(java.util.Date())
            val file = File(getBackupsDir(), "backup_manual_$now.json")
            file.writeText(content)
            true
        } catch (e: Exception) {
            Log.e(tag, "Error creating manual backup: ${e.message}", e)
            false
        }
    }

    suspend fun restoreBackup(fileName: String): Manifest? = withContext(Dispatchers.IO) {
        try {
            val backupFile = File(getBackupsDir(), fileName)
            if (backupFile.exists()) {
                val content = backupFile.readText()
                val manifest = json.decodeFromString<Manifest>(content)
                manifestFile.writeText(content)
                return@withContext manifest
            }
        } catch (e: Exception) {
            Log.e(tag, "Error restoring backup: ${e.message}", e)
        }
        null
    }

    suspend fun deleteBackup(fileName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val backupFile = File(getBackupsDir(), fileName)
            if (backupFile.exists()) {
                backupFile.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(tag, "Error deleting backup: ${e.message}", e)
            false
        }
    }

    /**
     * Exports current manifest and internal assets to a standalone ZIP archive.
     */
    suspend fun exportPackage(targetFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val manifest = loadManifest()
            val manifestJsonString = json.encodeToString(manifest)

            ZipOutputStream(FileOutputStream(targetFile)).use { zos ->
                // 1. Add manifest.json
                val manifestEntry = ZipEntry("manifest.json")
                zos.putNextEntry(manifestEntry)
                zos.write(manifestJsonString.toByteArray())
                zos.closeEntry()

                // 2. Add audio assets
                val audioDir = getAudioMediaDir()
                audioDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        val entry = ZipEntry("assets/audio/${file.name}")
                        zos.putNextEntry(entry)
                        FileInputStream(file).use { fis -> fis.copyTo(zos) }
                        zos.closeEntry()
                    }
                }

                // 3. Add icon assets
                val iconsDir = getIconsMediaDir()
                iconsDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        val entry = ZipEntry("assets/icons/${file.name}")
                        zos.putNextEntry(entry)
                        FileInputStream(file).use { fis -> fis.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e(tag, "Error exporting package: ${e.message}", e)
            false
        }
    }

    /**
     * Imports a SoundDeck .zip package safely:
     * 1. Extract to import_temp/
     * 2. Validate manifest.json
     * 3. Move media files
     * 4. Atomic clean temp
     */
    suspend fun importPackage(zipUri: Uri): Manifest? = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "import_temp").apply {
            if (exists()) deleteRecursively()
            mkdirs()
        }

        try {
            // 1. Extract
            context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val newFile = File(tempDir, entry.name)
                        // Zip Slip prevention
                        if (!newFile.canonicalPath.startsWith(tempDir.canonicalPath)) {
                            throw SecurityException("Zip traversal vulnerability detected")
                        }

                        if (entry.isDirectory) {
                            newFile.mkdirs()
                        } else {
                            newFile.parentFile?.mkdirs()
                            FileOutputStream(newFile).use { fos -> zis.copyTo(fos) }
                        }
                        entry = zis.nextEntry
                    }
                }
            }

            // 2. Validate manifest.json
            val tempManifestFile = File(tempDir, "manifest.json")
            if (!tempManifestFile.exists()) {
                Log.e(tag, "Import failed: manifest.json missing in package")
                return@withContext null
            }

            val importedManifest = json.decodeFromString<Manifest>(tempManifestFile.readText())

            // 3. Migrate binary assets
            val tempAudioDir = File(tempDir, "assets/audio")
            if (tempAudioDir.exists()) {
                val targetAudioDir = getAudioMediaDir()
                tempAudioDir.listFiles()?.forEach { src ->
                    if (src.isFile) {
                        src.copyTo(File(targetAudioDir, src.name), overwrite = true)
                    }
                }
            }

            val tempIconsDir = File(tempDir, "assets/icons")
            if (tempIconsDir.exists()) {
                val targetIconsDir = getIconsMediaDir()
                tempIconsDir.listFiles()?.forEach { src ->
                    if (src.isFile) {
                        src.copyTo(File(targetIconsDir, src.name), overwrite = true)
                    }
                }
            }

            // Save new manifest
            saveManifest(importedManifest)
            importedManifest
        } catch (e: Exception) {
            Log.e(tag, "Failed to import package: ${e.message}", e)
            null
        } finally {
            tempDir.deleteRecursively()
        }
    }

    fun createDefaultManifest(): Manifest {
        return Manifest(
            version = 1,
            globalSettings = GlobalSettings(
                obs = ObsConfig(
                    host = "192.168.1.100",
                    port = 4455,
                    password = "",
                    autoReconnect = true,
                    reconnectIntervalMs = 3000L
                )
            ),
            pages = listOf(
                PageConfig(
                    id = "page_main",
                    name = "Sound & Live",
                    gridRows = 3,
                    gridCols = 4,
                    pads = listOf(
                        PadConfig(
                            id = "pad_airhorn",
                            position = GridPosition(row = 0, col = 0),
                            visual = VisualConfig(
                                label = "AIR HORN",
                                backgroundColor = "#E53935",
                                textColor = "#FFFFFF",
                                iconAsset = "volume_up"
                            ),
                            onTap = ActionPipeline(audio = AudioAction(assetPath = "builtin:airhorn"))
                        ),
                        PadConfig(
                            id = "pad_applause",
                            position = GridPosition(row = 0, col = 1),
                            visual = VisualConfig(
                                label = "APPLAUSE",
                                backgroundColor = "#FB8C00",
                                textColor = "#FFFFFF",
                                iconAsset = "celebration"
                            ),
                            onTap = ActionPipeline(audio = AudioAction(assetPath = "builtin:applause"))
                        ),
                        PadConfig(
                            id = "pad_laser",
                            position = GridPosition(row = 0, col = 2),
                            visual = VisualConfig(
                                label = "LASER",
                                backgroundColor = "#00ACC1",
                                textColor = "#FFFFFF",
                                iconAsset = "bolt"
                            ),
                            onTap = ActionPipeline(audio = AudioAction(assetPath = "builtin:laser"))
                        ),
                        PadConfig(
                            id = "pad_buzzer",
                            position = GridPosition(row = 0, col = 3),
                            visual = VisualConfig(
                                label = "BUZZER",
                                backgroundColor = "#8E24AA",
                                textColor = "#FFFFFF",
                                iconAsset = "error"
                            ),
                            onTap = ActionPipeline(audio = AudioAction(assetPath = "builtin:buzzer"))
                        ),
                        PadConfig(
                            id = "pad_scene_main",
                            position = GridPosition(row = 1, col = 0),
                            visual = VisualConfig(
                                label = "SCENE: MAIN",
                                backgroundColor = "#1E88E5",
                                textColor = "#FFFFFF",
                                iconAsset = "videocam"
                            ),
                            onTap = ActionPipeline(
                                obsAction = ObsAction(
                                    requestType = "SetCurrentProgramScene",
                                    requestData = buildJsonObject { put("sceneName", "Main") }
                                )
                            )
                        ),
                        PadConfig(
                            id = "pad_scene_chat",
                            position = GridPosition(row = 1, col = 1),
                            visual = VisualConfig(
                                label = "SCENE: CHAT",
                                backgroundColor = "#3949AB",
                                textColor = "#FFFFFF",
                                iconAsset = "chat"
                            ),
                            onTap = ActionPipeline(
                                obsAction = ObsAction(
                                    requestType = "SetCurrentProgramScene",
                                    requestData = buildJsonObject { put("sceneName", "Chat") }
                                )
                            )
                        ),
                        PadConfig(
                            id = "pad_mute_mic",
                            position = GridPosition(row = 1, col = 2),
                            visual = VisualConfig(
                                label = "TOGGLE MIC",
                                backgroundColor = "#D81B60",
                                textColor = "#FFFFFF",
                                iconAsset = "mic_off"
                            ),
                            onTap = ActionPipeline(
                                obsAction = ObsAction(
                                    requestType = "ToggleInputMute",
                                    requestData = buildJsonObject { put("inputName", "Mic/Aux") }
                                )
                            )
                        ),
                        PadConfig(
                            id = "pad_toggle_rec",
                            position = GridPosition(row = 1, col = 3),
                            visual = VisualConfig(
                                label = "REC OBS",
                                backgroundColor = "#C62828",
                                textColor = "#FFFFFF",
                                iconAsset = "fiber_manual_record"
                            ),
                            onTap = ActionPipeline(
                                obsAction = ObsAction(requestType = "ToggleRecord")
                            )
                        ),
                        PadConfig(
                            id = "pad_http_test",
                            position = GridPosition(row = 2, col = 0),
                            visual = VisualConfig(
                                label = "HTTP PING",
                                backgroundColor = "#00897B",
                                textColor = "#FFFFFF",
                                iconAsset = "cloud_sync"
                            ),
                            onTap = ActionPipeline(
                                httpAction = HttpAction(
                                    url = "https://httpbin.org/get",
                                    method = "GET"
                                )
                            )
                        ),
                        PadConfig(
                            id = "pad_polling_monitor",
                            position = GridPosition(row = 2, col = 1),
                            visual = VisualConfig(
                                label = "LAN MONITOR",
                                backgroundColor = "#43A047",
                                textColor = "#FFFFFF",
                                iconAsset = "speed"
                            ),
                            onTap = ActionPipeline(
                                httpAction = HttpAction(
                                    url = "https://httpbin.org/get",
                                    method = "GET"
                                )
                            ),
                            polling = PollingConfig(
                                enabled = true,
                                intervalMs = 5000L,
                                request = HttpAction(
                                    url = "https://httpbin.org/get",
                                    method = "GET"
                                ),
                                responseMapping = ResponseMapping(
                                    jsonPathTextBadge = "$.origin",
                                    jsonPathPercentage = "85"
                                )
                            )
                        ),
                        PadConfig(
                            id = "pad_success",
                            position = GridPosition(row = 2, col = 2),
                            visual = VisualConfig(
                                label = "CHIME",
                                backgroundColor = "#00ACC1",
                                textColor = "#FFFFFF",
                                iconAsset = "check_circle"
                            ),
                            onTap = ActionPipeline(audio = AudioAction(assetPath = "builtin:success"))
                        ),
                        PadConfig(
                            id = "pad_stop_audio",
                            position = GridPosition(row = 2, col = 3),
                            visual = VisualConfig(
                                label = "MUTE AUDIO",
                                backgroundColor = "#263238",
                                textColor = "#FF5252",
                                iconAsset = "stop"
                            ),
                            onTap = ActionPipeline(audio = AudioAction(assetPath = ""))
                        )
                    )
                ),
                PageConfig(
                    id = "page_obs",
                    name = "Broadcast Console",
                    gridRows = 3,
                    gridCols = 4,
                    pads = listOf(
                        PadConfig(
                            id = "pad_live_stream",
                            position = GridPosition(row = 0, col = 0, rowSpan = 1, colSpan = 2),
                            visual = VisualConfig(
                                label = "TOGGLE STREAM",
                                backgroundColor = "#6A1B9A",
                                textColor = "#FFFFFF",
                                iconAsset = "live_tv"
                            ),
                            onTap = ActionPipeline(obsAction = ObsAction("ToggleStream"))
                        ),
                        PadConfig(
                            id = "pad_rec_stream",
                            position = GridPosition(row = 0, col = 2, rowSpan = 1, colSpan = 2),
                            visual = VisualConfig(
                                label = "TOGGLE RECORD",
                                backgroundColor = "#AD1457",
                                textColor = "#FFFFFF",
                                iconAsset = "fiber_smart_record"
                            ),
                            onTap = ActionPipeline(obsAction = ObsAction("ToggleRecord"))
                        ),
                        PadConfig(
                            id = "pad_scene_1",
                            position = GridPosition(row = 1, col = 0),
                            visual = VisualConfig(label = "INTRO SCENE", backgroundColor = "#1565C0"),
                            onTap = ActionPipeline(obsAction = ObsAction("SetCurrentProgramScene", buildJsonObject { put("sceneName", "Intro") }))
                        ),
                        PadConfig(
                            id = "pad_scene_2",
                            position = GridPosition(row = 1, col = 1),
                            visual = VisualConfig(label = "GAMEPLAY", backgroundColor = "#2E7D32"),
                            onTap = ActionPipeline(obsAction = ObsAction("SetCurrentProgramScene", buildJsonObject { put("sceneName", "Game") }))
                        ),
                        PadConfig(
                            id = "pad_scene_3",
                            position = GridPosition(row = 1, col = 2),
                            visual = VisualConfig(label = "BRB SCREEN", backgroundColor = "#EF6C00"),
                            onTap = ActionPipeline(obsAction = ObsAction("SetCurrentProgramScene", buildJsonObject { put("sceneName", "BRB") }))
                        ),
                        PadConfig(
                            id = "pad_scene_4",
                            position = GridPosition(row = 1, col = 3),
                            visual = VisualConfig(label = "OUTRO SCENE", backgroundColor = "#4527A0"),
                            onTap = ActionPipeline(obsAction = ObsAction("SetCurrentProgramScene", buildJsonObject { put("sceneName", "Outro") }))
                        ),
                        PadConfig(
                            id = "pad_mic_aux",
                            position = GridPosition(row = 2, col = 0, rowSpan = 1, colSpan = 2),
                            visual = VisualConfig(label = "MUTE MIC / AUX", backgroundColor = "#37474F"),
                            onTap = ActionPipeline(obsAction = ObsAction("ToggleInputMute", buildJsonObject { put("inputName", "Mic/Aux") }))
                        ),
                        PadConfig(
                            id = "pad_desktop_audio",
                            position = GridPosition(row = 2, col = 2, rowSpan = 1, colSpan = 2),
                            visual = VisualConfig(label = "MUTE DESKTOP", backgroundColor = "#263238"),
                            onTap = ActionPipeline(obsAction = ObsAction("ToggleInputMute", buildJsonObject { put("inputName", "Desktop Audio") }))
                        )
                    )
                ),
                PageConfig(
                    id = "page_sfx",
                    name = "Full SFX Soundboard",
                    gridRows = 3,
                    gridCols = 3,
                    pads = listOf(
                        PadConfig(
                            id = "sfx_1",
                            position = GridPosition(row = 0, col = 0),
                            visual = VisualConfig(label = "AIR HORN", backgroundColor = "#D50000"),
                            onTap = ActionPipeline(audio = AudioAction(assetPath = "builtin:airhorn"))
                        ),
                        PadConfig(
                            id = "sfx_2",
                            position = GridPosition(row = 0, col = 1),
                            visual = VisualConfig(label = "LASER", backgroundColor = "#0091EA"),
                            onTap = ActionPipeline(audio = AudioAction(assetPath = "builtin:laser"))
                        ),
                        PadConfig(
                            id = "sfx_3",
                            position = GridPosition(row = 0, col = 2),
                            visual = VisualConfig(label = "APPLAUSE", backgroundColor = "#FF6D00"),
                            onTap = ActionPipeline(audio = AudioAction(assetPath = "builtin:applause"))
                        ),
                        PadConfig(
                            id = "sfx_4",
                            position = GridPosition(row = 1, col = 0),
                            visual = VisualConfig(label = "BUZZER", backgroundColor = "#AA00FF"),
                            onTap = ActionPipeline(audio = AudioAction(assetPath = "builtin:buzzer"))
                        ),
                        PadConfig(
                            id = "sfx_5",
                            position = GridPosition(row = 1, col = 1),
                            visual = VisualConfig(label = "CHIME", backgroundColor = "#00C853"),
                            onTap = ActionPipeline(audio = AudioAction(assetPath = "builtin:success"))
                        ),
                        PadConfig(
                            id = "sfx_6",
                            position = GridPosition(row = 1, col = 2),
                            visual = VisualConfig(label = "NOTIFY", backgroundColor = "#00B0FF"),
                            onTap = ActionPipeline(audio = AudioAction(assetPath = "builtin:notification"))
                        ),
                        PadConfig(
                            id = "sfx_7",
                            position = GridPosition(row = 2, col = 0),
                            visual = VisualConfig(label = "GLITCH", backgroundColor = "#FFD600", textColor = "#121212"),
                            onTap = ActionPipeline(audio = AudioAction(assetPath = "builtin:glitch"))
                        ),
                        PadConfig(
                            id = "sfx_8",
                            position = GridPosition(row = 2, col = 1),
                            visual = VisualConfig(label = "RETRO JUMP", backgroundColor = "#00E676", textColor = "#121212"),
                            onTap = ActionPipeline(audio = AudioAction(assetPath = "builtin:retro_jump"))
                        ),
                        PadConfig(
                            id = "sfx_9",
                            position = GridPosition(row = 2, col = 2),
                            visual = VisualConfig(label = "STOP ALL", backgroundColor = "#212121", textColor = "#FF1744"),
                            onTap = ActionPipeline(audio = AudioAction(assetPath = ""))
                        )
                    )
                )
            )
        )
    }
}
