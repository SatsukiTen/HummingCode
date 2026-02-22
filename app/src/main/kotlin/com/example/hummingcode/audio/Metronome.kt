package com.example.hummingcode.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class Metronome {

    private val sampleRate = 44100
    private var accentTrack: AudioTrack? = null
    private var normalTrack: AudioTrack? = null

    private fun generateClickPcm(
        frequencyHz: Float,
        amplitude: Float,
        durationMs: Int
    ): ShortArray {
        val numSamples = sampleRate * durationMs / 1000
        val samples = ShortArray(numSamples)
        val twoPiF = 2.0 * PI * frequencyHz / sampleRate
        // Decay constant: reaches ~e^-5 ≈ 0.007 by the end
        val decayRate = 5.0 / numSamples
        for (i in 0 until numSamples) {
            val envelope = exp(-decayRate * i)
            samples[i] = (amplitude * envelope * sin(twoPiF * i) * Short.MAX_VALUE).toInt().toShort()
        }
        return samples
    }

    private fun buildStaticTrack(pcm: ShortArray): AudioTrack {
        val bufferSize = pcm.size * 2 // bytes
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(bufferSize)
            .build()
            .also { track ->
                track.write(pcm, 0, pcm.size)
            }
    }

    private fun reloadStaticData() {
        accentTrack?.release()
        normalTrack?.release()
        // Accent: 880 Hz, 0.9 amplitude, 20 ms
        accentTrack = buildStaticTrack(generateClickPcm(880f, 0.9f, 20))
        // Normal: 660 Hz, 0.55 amplitude, 15 ms
        normalTrack = buildStaticTrack(generateClickPcm(660f, 0.55f, 15))
    }

    private fun playClick(isAccent: Boolean) {
        val track = (if (isAccent) accentTrack else normalTrack) ?: return
        if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
            track.stop()
        }
        track.reloadStaticData()
        track.play()
    }

    /**
     * Starts the metronome and returns a [Flow] that emits the current beat index (0-based)
     * on every tick. The flow runs on [Dispatchers.IO] and uses drift-corrected timing to
     * stay in sync over long durations.
     */
    fun start(bpm: Int, beatsPerMeasure: Int): Flow<Int> = flow {
        reloadStaticData()
        val intervalMs = 60_000L / bpm
        val startTime = System.currentTimeMillis()
        var beatCount = 0L

        while (true) {
            val beatIndex = (beatCount % beatsPerMeasure).toInt()
            playClick(beatIndex == 0)
            emit(beatIndex)

            beatCount++
            // Drift-corrected delay: always schedule relative to the original start time
            val nextBeatTime = startTime + beatCount * intervalMs
            val delayMs = nextBeatTime - System.currentTimeMillis()
            if (delayMs > 0) delay(delayMs)
        }
    }.flowOn(Dispatchers.IO)

    fun release() {
        accentTrack?.release()
        normalTrack?.release()
        accentTrack = null
        normalTrack = null
    }
}
