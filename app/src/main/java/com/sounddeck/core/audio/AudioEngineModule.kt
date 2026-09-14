package com.sounddeck.core.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

class AudioEngineModule(private val context: Context) {

    private val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(context.applicationContext).build()
    }

    init {
        ensureBuiltInSounds()
    }

    /**
     * Monophonic exclusive playback as mandated by the SoundDeck specification.
     * Stops any previous audio immediately before starting the new track.
     */
    @Synchronized
    fun playExclusive(
        audioFile: File,
        gain: Float = 1.0f,
        masterVolume: Float = 1.0f,
        fadeInMs: Long = 0L
    ) {
        if (!audioFile.exists() || !audioFile.canRead()) {
            return
        }
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        val mediaItem = MediaItem.fromUri(Uri.fromFile(audioFile))
        exoPlayer.setMediaItem(mediaItem)
        val targetVolume = (gain * masterVolume).coerceIn(0.0f, 1.0f)

        if (fadeInMs > 0) {
            exoPlayer.volume = 0f
            exoPlayer.prepare()
            exoPlayer.play()
            // Quick fade-in routine
            android.os.Handler(android.os.Looper.getMainLooper()).post(object : Runnable {
                var step = 0
                val totalSteps = 10
                val interval = (fadeInMs / totalSteps).coerceAtLeast(10L)
                override fun run() {
                    step++
                    val frac = (step.toFloat() / totalSteps).coerceIn(0f, 1f)
                    exoPlayer.volume = targetVolume * frac
                    if (step < totalSteps && exoPlayer.isPlaying) {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this, interval)
                    }
                }
            })
        } else {
            exoPlayer.volume = targetVolume
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    /**
     * Resolves an asset path string (built-in sound or user file in media/audio).
     */
    fun resolveAudioFile(assetPath: String): File? {
        if (assetPath.isBlank()) return null

        // If it starts with builtin:
        if (assetPath.startsWith("builtin:")) {
            val soundName = assetPath.removePrefix("builtin:")
            val file = File(getBuiltInDir(), "$soundName.wav")
            if (file.exists()) return file
        }

        // Check in media/audio dir
        val mediaDir = File(context.filesDir, "media/audio")
        val fileInMedia = File(mediaDir, assetPath)
        if (fileInMedia.exists()) return fileInMedia

        // Check if full path
        val directFile = File(assetPath)
        if (directFile.exists()) return directFile

        // Fallback: check inside builtins by name
        val fallbackBuiltin = File(getBuiltInDir(), "$assetPath.wav")
        if (fallbackBuiltin.exists()) return fallbackBuiltin

        return null
    }

    fun playByAssetPath(
        assetPath: String,
        gain: Float = 1.0f,
        masterVolume: Float = 1.0f,
        fadeInMs: Long = 0L,
        fadeOutMs: Long = 0L
    ) {
        val file = resolveAudioFile(assetPath)
        if (file != null) {
            playExclusive(file, gain, masterVolume, fadeInMs)
        }
    }

    fun stopPlayback() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
    }

    fun setMasterVolume(masterVolume: Float) {
        exoPlayer.volume = masterVolume.coerceIn(0.0f, 1.0f)
    }

    fun release() {
        exoPlayer.release()
    }

    fun getBuiltInSounds(): List<String> {
        return listOf(
            "airhorn",
            "laser",
            "applause",
            "buzzer",
            "success",
            "notification",
            "glitch",
            "retro_jump"
        )
    }

    private fun getBuiltInDir(): File {
        val dir = File(context.filesDir, "media/audio/builtins")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun ensureBuiltInSounds() {
        val dir = getBuiltInDir()
        getBuiltInSounds().forEach { soundName ->
            val soundFile = File(dir, "$soundName.wav")
            if (!soundFile.exists() || soundFile.length() == 0L) {
                generateSynthesizedWav(soundName, soundFile)
            }
        }
    }

    /**
     * Synthesizes low-latency PCM WAV files so the soundboard is 100% functional
     * offline on first launch.
     */
    private fun generateSynthesizedWav(type: String, targetFile: File) {
        val sampleRate = 22050
        val durationSeconds: Double
        val pcmData: ShortArray

        when (type) {
            "airhorn" -> {
                durationSeconds = 0.8
                val totalSamples = (sampleRate * durationSeconds).toInt()
                pcmData = ShortArray(totalSamples)
                // Major chords layered together for classic airhorn blast
                val f1 = 466.16 // Bb4
                val f2 = 587.33 // D5
                val f3 = 698.46 // F5
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val envelope = when {
                        t < 0.05 -> t / 0.05
                        t > 0.65 -> ((0.8 - t) / 0.15).coerceAtLeast(0.0)
                        else -> 1.0
                    }
                    val sample = (sin(2 * Math.PI * f1 * t) +
                            0.8 * sin(2 * Math.PI * f2 * t) +
                            0.7 * sin(2 * Math.PI * f3 * t) +
                            0.3 * sin(2 * Math.PI * f1 * 2 * t)) / 2.8
                    pcmData[i] = (sample * envelope * 32000).toInt().coerceIn(-32767, 32767).toShort()
                }
            }
            "laser" -> {
                durationSeconds = 0.4
                val totalSamples = (sampleRate * durationSeconds).toInt()
                pcmData = ShortArray(totalSamples)
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val freq = 2000.0 * Math.exp(-t * 8.0) + 120.0
                    val envelope = (1.0 - t / durationSeconds).coerceAtLeast(0.0)
                    val sample = sin(2 * Math.PI * freq * t)
                    pcmData[i] = (sample * envelope * 28000).toInt().coerceIn(-32767, 32767).toShort()
                }
            }
            "applause" -> {
                durationSeconds = 1.2
                val totalSamples = (sampleRate * durationSeconds).toInt()
                pcmData = ShortArray(totalSamples)
                var noise = 0.0
                val random = java.util.Random(42)
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    // Filtered noise with periodic claps
                    val clapPulse = sin(2 * Math.PI * 18.0 * t).let { if (it > 0.6) 1.5 else 0.4 }
                    noise = 0.85 * noise + 0.15 * (random.nextDouble() * 2.0 - 1.0)
                    val envelope = when {
                        t < 0.1 -> t / 0.1
                        t > 0.9 -> (1.2 - t) / 0.3
                        else -> 1.0
                    }.coerceIn(0.0, 1.0)
                    pcmData[i] = (noise * clapPulse * envelope * 24000).toInt().coerceIn(-32767, 32767).toShort()
                }
            }
            "buzzer" -> {
                durationSeconds = 0.6
                val totalSamples = (sampleRate * durationSeconds).toInt()
                pcmData = ShortArray(totalSamples)
                val freq = 130.81 // C3 buzz
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    // Sawtooth buzz
                    val sample = 2.0 * ((t * freq) - Math.floor((t * freq) + 0.5))
                    val envelope = if (t > 0.5) (0.6 - t) / 0.1 else 1.0
                    pcmData[i] = (sample * envelope * 26000).toInt().coerceIn(-32767, 32767).toShort()
                }
            }
            "success" -> {
                durationSeconds = 0.7
                val totalSamples = (sampleRate * durationSeconds).toInt()
                pcmData = ShortArray(totalSamples)
                val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50) // C5, E5, G5, C6
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val noteIndex = (t / 0.15).toInt().coerceAtMost(3)
                    val freq = notes[noteIndex]
                    val sample = sin(2 * Math.PI * freq * t) + 0.3 * sin(2 * Math.PI * freq * 2 * t)
                    val noteTime = t % 0.15
                    val decay = Math.exp(-noteTime * 6.0)
                    pcmData[i] = (sample * decay * 28000).toInt().coerceIn(-32767, 32767).toShort()
                }
            }
            "notification" -> {
                durationSeconds = 0.5
                val totalSamples = (sampleRate * durationSeconds).toInt()
                pcmData = ShortArray(totalSamples)
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val freq = if (t < 0.2) 880.0 else 1760.0
                    val sample = sin(2 * Math.PI * freq * t)
                    val env = (1.0 - (t % 0.2) / 0.2).coerceAtLeast(0.0)
                    pcmData[i] = (sample * env * 24000).toInt().coerceIn(-32767, 32767).toShort()
                }
            }
            "glitch" -> {
                durationSeconds = 0.35
                val totalSamples = (sampleRate * durationSeconds).toInt()
                pcmData = ShortArray(totalSamples)
                val random = java.util.Random(99)
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val square = if (sin(2 * Math.PI * 440.0 * t * (1.0 + (t * 20 % 3))) > 0) 1.0 else -1.0
                    val sample = square * (if (random.nextDouble() > 0.4) 1.0 else -0.5)
                    pcmData[i] = (sample * 22000).toInt().coerceIn(-32767, 32767).toShort()
                }
            }
            else -> { // retro_jump
                durationSeconds = 0.4
                val totalSamples = (sampleRate * durationSeconds).toInt()
                pcmData = ShortArray(totalSamples)
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val freq = 220.0 + 1200.0 * (t / durationSeconds)
                    val sample = if (sin(2 * Math.PI * freq * t) > 0) 0.8 else -0.8
                    pcmData[i] = (sample * 22000).toInt().coerceIn(-32767, 32767).toShort()
                }
            }
        }

        writeWavFile(targetFile, pcmData, sampleRate)
    }

    private fun writeWavFile(file: File, pcmData: ShortArray, sampleRate: Int) {
        val totalAudioLen = pcmData.size * 2
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = sampleRate * channels * 2

        val header = ByteArray(44)
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray())
        buffer.putInt(totalDataLen)
        buffer.put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16) // SubChunk1Size (16 for PCM)
        buffer.putShort(1) // AudioFormat (1 for PCM)
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort((channels * 2).toShort()) // BlockAlign
        buffer.putShort(16) // BitsPerSample
        buffer.put("data".toByteArray())
        buffer.putInt(totalAudioLen)

        FileOutputStream(file).use { fos ->
            fos.write(header)
            val byteBuffer = ByteBuffer.allocate(pcmData.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (sample in pcmData) {
                byteBuffer.putShort(sample)
            }
            fos.write(byteBuffer.array())
        }
    }
}
