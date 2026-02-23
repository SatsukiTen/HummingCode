package com.example.hummingcode.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

/**
 * マイクから音声を録音してピッチを解析するクラス。
 * Flowとして検出された音名を流す。
 */
class AudioRecorder {
    companion object {
        const val SAMPLE_RATE = 44100
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        // 解析ウィンドウサイズ: 約100ms
        const val ANALYSIS_FRAME_SIZE = 4096
        // ステップサイズ: 約50ms (50%オーバーラップ)
        const val STEP_SIZE = 2048
        // 中央値フィルタのウィンドウ幅 (約320ms)
        const val MEDIAN_WINDOW = 7
        // 有効検出とみなすのに必要な最小フレーム数 (ウィンドウの過半数)
        const val MIN_STABLE_COUNT = 5
    }

    private val pitchDetector = PitchDetector(SAMPLE_RATE)
    private val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

    /**
     * 録音を開始し、検出された音名をFlowとして返す。
     * Flowのコレクションをキャンセルすると録音が停止する。
     */
    fun recordAndDetect(): Flow<DetectedPitch> = flow {
        val bufferSize = maxOf(minBufferSize, ANALYSIS_FRAME_SIZE * 2)
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            emit(DetectedPitch.Error("AudioRecordの初期化に失敗しました"))
            return@flow
        }

        audioRecord.startRecording()

        try {
            val readBuffer = ShortArray(STEP_SIZE)
            val analysisBuffer = ShortArray(ANALYSIS_FRAME_SIZE)
            var fillIndex = 0
            // 中央値フィルタ用: 直近 MEDIAN_WINDOW フレームの生周波数 (-1f = 未検出)
            val freqHistory = ArrayDeque<Float>()

            while (coroutineContext.isActive) {
                val readCount = audioRecord.read(readBuffer, 0, STEP_SIZE)
                if (readCount <= 0) continue

                // 解析バッファに追記（循環バッファ的に使う）
                for (i in 0 until readCount) {
                    analysisBuffer[fillIndex % ANALYSIS_FRAME_SIZE] = readBuffer[i]
                    fillIndex++
                }

                // バッファが満杯になったら解析
                if (fillIndex >= ANALYSIS_FRAME_SIZE) {
                    val orderedBuffer = ShortArray(ANALYSIS_FRAME_SIZE)
                    val startIdx = fillIndex % ANALYSIS_FRAME_SIZE
                    for (i in 0 until ANALYSIS_FRAME_SIZE) {
                        orderedBuffer[i] = analysisBuffer[(startIdx + i) % ANALYSIS_FRAME_SIZE]
                    }

                    val rawFrequency = pitchDetector.detectPitch(orderedBuffer)

                    // 中央値フィルタ: 直近 MEDIAN_WINDOW フレームを保持
                    freqHistory.addLast(rawFrequency)
                    if (freqHistory.size > MEDIAN_WINDOW) freqHistory.removeFirst()

                    // 有効検出が MIN_STABLE_COUNT 以上あれば中央値を使用、なければ未検出
                    val positives = freqHistory.filter { it > 0f }.sorted()
                    val smoothedFrequency = if (positives.size >= MIN_STABLE_COUNT) {
                        positives[positives.size / 2]
                    } else {
                        -1f
                    }

                    val noteName = if (smoothedFrequency > 0f) {
                        NoteMapper.frequencyToNoteName(smoothedFrequency)
                    } else {
                        null
                    }

                    emit(DetectedPitch.Success(smoothedFrequency, noteName))
                }
            }
        } finally {
            audioRecord.stop()
            audioRecord.release()
        }
    }.flowOn(Dispatchers.IO)
}

sealed class DetectedPitch {
    data class Success(val frequency: Float, val noteName: String?) : DetectedPitch()
    data class Error(val message: String) : DetectedPitch()
}
