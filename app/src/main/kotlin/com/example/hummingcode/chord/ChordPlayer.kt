package com.example.hummingcode.chord

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.hummingcode.model.Chord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/**
 * コードを音声合成して再生するクラス。
 * AudioTrackを使ってサイン波を合成し、コード音を生成する。
 */
class ChordPlayer {
    companion object {
        const val SAMPLE_RATE = 44100
        const val CHORD_DURATION_MS = 1500L
        const val FADE_MS = 100L
    }

    @Volatile
    private var audioTrack: AudioTrack? = null

    @Volatile
    private var stopRequested = false

    /**
     * コード進行を順番に再生する。
     * @param chords 再生するコードのリスト
     * @param onChordChange 次のコードに移った時のコールバック（インデックスを引数に取る）
     */
    suspend fun playProgression(
        chords: List<Chord>,
        durationsMs: List<Long>,
        baseOctaves: List<Int>,
        onChordChange: (Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        stopRequested = false
        for ((index, chord) in chords.withIndex()) {
            if (!isActive || stopRequested) break
            onChordChange(index)
            val duration = durationsMs.getOrElse(index) { CHORD_DURATION_MS }
            val octave = baseOctaves.getOrElse(index) { 3 }
            playChord(chord, duration, octave)
        }
        onChordChange(-1)
    }

    /**
     * 単一のコードを再生する。
     */
    suspend fun playChord(
        chord: Chord,
        durationMs: Long = CHORD_DURATION_MS,
        baseOctave: Int = 3
    ) = withContext(Dispatchers.IO) {
        val midiNotes = chord.getMidiNotes(baseOctave = baseOctave)
        val samples = synthesizeChord(midiNotes, durationMs)
        playPcmData(samples)
    }

    /**
     * 単音をプレビュー再生する（コード選択時のフィードバック用）
     * 前の再生中なら停止してから再生する。
     */
    suspend fun previewChord(chord: Chord, baseOctave: Int = 3) = withContext(Dispatchers.IO) {
        stop()
        stopRequested = false
        val midiNotes = chord.getMidiNotes(baseOctave = baseOctave)
        val samples = synthesizeChord(midiNotes, 600L)
        playPcmData(samples)
    }

    private fun synthesizeChord(midiNotes: List<Int>, durationMs: Long): ShortArray {
        val totalSamples = (SAMPLE_RATE * durationMs / 1000).toInt()
        val fadeSamples = (SAMPLE_RATE * FADE_MS / 1000).toInt()
        val output = ShortArray(totalSamples)
        val noteCount = midiNotes.size

        for (midiNote in midiNotes) {
            val frequency = midiToFrequency(midiNote)
            for (i in 0 until totalSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                // 基音 + 倍音でピアノ風の音色を作る
                var sample = sin(2.0 * PI * frequency * t) * 0.6
                sample += sin(2.0 * PI * frequency * 2.0 * t) * 0.2
                sample += sin(2.0 * PI * frequency * 3.0 * t) * 0.1
                sample += sin(2.0 * PI * frequency * 4.0 * t) * 0.05

                // エンベロープ: アタック + ディケイ + サステイン + リリース
                val envelope = calculateEnvelope(i, totalSamples, fadeSamples)
                output[i] = (output[i] + (sample * envelope * 32767.0 / noteCount).toInt())
                    .coerceIn(-32768, 32767).toShort()
            }
        }

        return output
    }

    private fun calculateEnvelope(sampleIndex: Int, total: Int, fadeSamples: Int): Double {
        val attackSamples = (SAMPLE_RATE * 0.01).toInt() // 10ms アタック
        val decaySamples = (SAMPLE_RATE * 0.1).toInt()   // 100ms ディケイ
        val sustainLevel = 0.7

        return when {
            sampleIndex < attackSamples -> {
                // アタック: 0 → 1
                sampleIndex.toDouble() / attackSamples
            }
            sampleIndex < attackSamples + decaySamples -> {
                // ディケイ: 1 → sustainLevel
                val decayProgress = (sampleIndex - attackSamples).toDouble() / decaySamples
                1.0 - decayProgress * (1.0 - sustainLevel)
            }
            sampleIndex >= total - fadeSamples -> {
                // リリース: sustainLevel → 0
                val releaseProgress = (total - sampleIndex).toDouble() / fadeSamples
                sustainLevel * releaseProgress
            }
            else -> sustainLevel
        }
    }

    private fun midiToFrequency(midiNote: Int): Double {
        return 440.0 * 2.0.pow((midiNote - 69).toDouble() / 12.0)
    }

    private fun playPcmData(samples: ShortArray) {
        if (stopRequested) return

        val minBufSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        // getMinBufferSize がエラーを返した場合はスキップ
        if (minBufSize <= 0) return

        // 書き込みバッファは minBufSize の倍数で十分な大きさにする
        val bufferSize = maxOf(minBufSize * 4, 8192)

        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (e: Exception) {
            return
        }

        // 正常に初期化されているか確認
        if (track.state != AudioTrack.STATE_INITIALIZED) {
            track.release()
            return
        }

        audioTrack = track

        try {
            track.play()
            var offset = 0
            val chunkSize = bufferSize / 2 / 2  // Short は 2バイトなので Short 数に換算
            while (offset < samples.size && !stopRequested) {
                val writeCount = minOf(chunkSize, samples.size - offset)
                if (writeCount <= 0) break
                val written = track.write(samples, offset, writeCount)
                if (written < 0) break  // エラーコードが返ったら中断
                offset += written
            }
            if (!stopRequested) {
                track.stop()
            }
        } catch (_: IllegalStateException) {
            // 外部から stop() が呼ばれた場合など
        } finally {
            try {
                track.release()
            } catch (_: Exception) {}
            if (audioTrack == track) audioTrack = null
        }
    }

    fun stop() {
        stopRequested = true
        val track = audioTrack
        audioTrack = null
        try {
            track?.pause()
            track?.flush()
            track?.stop()
        } catch (_: Exception) {}
        try {
            track?.release()
        } catch (_: Exception) {}
    }
}
