package com.example.hummingcode.audio

import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.roundToInt

/**
 * YINアルゴリズムを使用してピッチ（基本周波数）を検出するクラス。
 * de Cheveigné & Kawahara (2002) "YIN, a fundamental frequency estimator for speech and music" に基づく。
 */
class PitchDetector(private val sampleRate: Int = 44100) {

    private val threshold = 0.15f
    // 検出する周波数範囲: 80Hz (低いD2) ～ 1000Hz (高いB5)
    private val minFreq = 80f
    private val maxFreq = 1000f
    private val minTau get() = (sampleRate / maxFreq).toInt()
    private val maxTau get() = (sampleRate / minFreq).toInt()

    /**
     * 音声サンプルから基本周波数を検出する。
     * @param samples 音声サンプル (Short配列)
     * @return 検出された周波数 [Hz]、検出失敗時は -1f
     */
    fun detectPitch(samples: ShortArray): Float {
        if (samples.isEmpty()) return -1f
        val floatSamples = FloatArray(samples.size) { samples[it] / 32768f }
        return detectPitch(floatSamples)
    }

    /**
     * 音声サンプルから基本周波数を検出する。
     * @param samples 正規化された音声サンプル (-1.0 ～ 1.0)
     * @return 検出された周波数 [Hz]、検出失敗時は -1f
     */
    fun detectPitch(samples: FloatArray): Float {
        if (samples.size < maxTau * 2) return -1f

        // 音量チェック: 無音区間はスキップ
        val rms = calculateRms(samples)
        if (rms < 0.01f) return -1f

        val halfSize = minOf(samples.size / 2, maxTau + 1)

        // Step 1: 差分関数を計算
        val diff = FloatArray(halfSize)
        for (tau in minTau until halfSize) {
            var sum = 0f
            for (i in 0 until halfSize) {
                val delta = samples[i] - samples[i + tau]
                sum += delta * delta
            }
            diff[tau] = sum
        }

        // Step 2: 累積平均正規化差分関数 (CMNDF)
        val cmndf = FloatArray(halfSize)
        cmndf[0] = 1f
        var runningSum = 0f
        for (tau in 1 until halfSize) {
            runningSum += diff[tau]
            cmndf[tau] = if (runningSum == 0f) 0f else diff[tau] * tau / runningSum
        }

        // Step 3: 絶対閾値法でラグを探索
        var tau = minTau
        while (tau < halfSize) {
            if (cmndf[tau] < threshold) {
                // 局所最小値を探す
                while (tau + 1 < halfSize && cmndf[tau + 1] < cmndf[tau]) {
                    tau++
                }
                break
            }
            tau++
        }

        if (tau >= halfSize || cmndf[tau] >= threshold) {
            return -1f
        }

        // Step 4: 放物線補間で精度を上げる
        val betterTau = if (tau > 0 && tau < halfSize - 1) {
            val s0 = cmndf[tau - 1]
            val s1 = cmndf[tau]
            val s2 = cmndf[tau + 1]
            val denom = 2 * (2 * s1 - s2 - s0)
            if (abs(denom) > 1e-6f) {
                tau + (s2 - s0) / denom
            } else {
                tau.toFloat()
            }
        } else {
            tau.toFloat()
        }

        val frequency = sampleRate / betterTau
        return if (frequency in minFreq..maxFreq) frequency else -1f
    }

    private fun calculateRms(samples: FloatArray): Float {
        var sum = 0f
        for (s in samples) sum += s * s
        return kotlin.math.sqrt(sum / samples.size)
    }
}

/**
 * 周波数を音名に変換するクラス。
 */
object NoteMapper {
    private val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    /**
     * 周波数を音名に変換する (例: 440Hz → "A")
     */
    fun frequencyToNoteName(frequency: Float): String? {
        if (frequency <= 0) return null

        // A4 = 440Hz = MIDI note 69
        val midiNote = 12.0 * log2(frequency / 440.0) + 69
        val roundedMidi = midiNote.roundToInt()

        if (roundedMidi < 0 || roundedMidi > 127) return null

        return noteNames[roundedMidi % 12]
    }

    fun frequencyToFullNote(frequency: Float): String? {
        if (frequency <= 0) return null
        val midiNote = 12.0 * log2(frequency / 440.0) + 69
        val roundedMidi = midiNote.roundToInt()
        if (roundedMidi < 0 || roundedMidi > 127) return null
        val noteName = noteNames[roundedMidi % 12]
        val octave = (roundedMidi / 12) - 1
        return "$noteName$octave"
    }
}
