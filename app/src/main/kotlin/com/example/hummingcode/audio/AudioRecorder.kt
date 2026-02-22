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

                    val frequency = pitchDetector.detectPitch(orderedBuffer)
                    val noteName = if (frequency > 0) NoteMapper.frequencyToNoteName(frequency) else null

                    emit(DetectedPitch.Success(frequency, noteName))
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
