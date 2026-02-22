package com.example.hummingcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hummingcode.audio.AudioRecorder
import com.example.hummingcode.audio.DetectedPitch
import com.example.hummingcode.audio.Metronome
import com.example.hummingcode.chord.ChordPlayer
import com.example.hummingcode.chord.ChordSuggester
import com.example.hummingcode.model.AppScreen
import com.example.hummingcode.model.TimeSignature
import com.example.hummingcode.model.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val audioRecorder = AudioRecorder()
    private val chordPlayer = ChordPlayer()
    private val metronome = Metronome()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var recordingJob: Job? = null
    private var playbackJob: Job? = null
    private var metronomeJob: Job? = null

    // 拍単位録音: 現在の拍内で検出した音名バッファ
    private val currentBeatNotes = mutableListOf<String>()
    // 各拍の支配的音名 (null=無音)
    private val finishedBeats = mutableListOf<String?>()
    // 1拍の長さ (ms) — 録音開始時に確定
    private var beatDurationMs = 500L
    // 最後に処理した拍インデックス (-1=拍未発火)
    private var lastBeatIndex = -1

    // ホーム画面プレビュー用メトロノーム
    private var previewMetronomeJob: Job? = null
    private var previewDebounceJob: Job? = null

    // タップテンポ: 直近タップ時刻リスト
    private val tapTimes = mutableListOf<Long>()

    init {
        startMetronomePreview()
    }

    /** BPM を 40〜240 の範囲でセットし、プレビューメトロノームを再起動する。 */
    fun setBpm(bpm: Int) {
        _uiState.update { it.copy(bpm = bpm.coerceIn(40, 240)) }
        if (_uiState.value.screen == AppScreen.Home) restartPreviewDebounced()
    }

    /** 拍子をセットし、プレビューメトロノームを再起動する。 */
    fun setTimeSignature(ts: TimeSignature) {
        _uiState.update { it.copy(timeSignature = ts) }
        if (_uiState.value.screen == AppScreen.Home) restartPreviewDebounced()
    }

    /**
     * タップ間隔の平均からBPMを算出してセットする。
     * 2.5秒以上間が空いたらタップ履歴をリセット。
     */
    fun tapTempo() {
        val now = System.currentTimeMillis()
        tapTimes.removeAll { now - it > 2500 }
        tapTimes.add(now)
        if (tapTimes.size >= 2) {
            val intervals = tapTimes.zipWithNext { a, b -> b - a }
            val avgInterval = intervals.takeLast(8).average()
            val newBpm = (60_000.0 / avgInterval).toInt().coerceIn(40, 240)
            _uiState.update { it.copy(bpm = newBpm) }
            startMetronomePreview() // 即時反映（デバウンスなし）
        }
    }

    private fun restartPreviewDebounced() {
        previewDebounceJob?.cancel()
        previewDebounceJob = viewModelScope.launch {
            delay(400)
            startMetronomePreview()
        }
    }

    private fun startMetronomePreview() {
        previewDebounceJob?.cancel()
        previewDebounceJob = null
        previewMetronomeJob?.cancel()
        previewMetronomeJob = viewModelScope.launch {
            metronome.start(
                bpm = _uiState.value.bpm,
                beatsPerMeasure = _uiState.value.timeSignature.numerator
            ).collect { beatIndex ->
                _uiState.update { it.copy(currentBeat = beatIndex) }
            }
        }
    }

    private fun stopMetronomePreview() {
        previewDebounceJob?.cancel()
        previewDebounceJob = null
        previewMetronomeJob?.cancel()
        previewMetronomeJob = null
        _uiState.update { it.copy(currentBeat = -1) }
    }

    /**
     * 録音を開始する。
     */
    fun startRecording() {
        if (_uiState.value.isRecording) return

        stopMetronomePreview()
        currentBeatNotes.clear()
        finishedBeats.clear()
        beatDurationMs = 60_000L / _uiState.value.bpm
        lastBeatIndex = -1

        _uiState.update {
            it.copy(
                screen = AppScreen.Recording,
                isRecording = true,
                currentDetectedNote = null,
                segments = emptyList(),
                errorMessage = null
            )
        }

        metronomeJob = viewModelScope.launch {
            metronome.start(
                bpm = _uiState.value.bpm,
                beatsPerMeasure = _uiState.value.timeSignature.numerator
            ).collect { beatIndex ->
                // 前の拍のウィンドウを確定
                if (lastBeatIndex >= 0) {
                    val dominant = currentBeatNotes
                        .groupingBy { it }.eachCount()
                        .maxByOrNull { it.value }?.key
                    finishedBeats.add(dominant)
                    currentBeatNotes.clear()
                }
                lastBeatIndex = beatIndex
                _uiState.update { it.copy(currentBeat = beatIndex) }
            }
        }

        recordingJob = viewModelScope.launch {
            audioRecorder.recordAndDetect().collect { pitch ->
                when (pitch) {
                    is DetectedPitch.Success -> {
                        val note = pitch.noteName
                        _uiState.update { it.copy(currentDetectedNote = note) }
                        // 拍ウィンドウ内の音をすべて蓄積; 拍境界で多数決をとる
                        if (note != null) currentBeatNotes.add(note)
                    }
                    is DetectedPitch.Error -> {
                        _uiState.update { it.copy(errorMessage = pitch.message) }
                    }
                }
            }
        }
    }

    /**
     * 録音を停止し、コード候補を生成する。
     */
    fun stopRecording() {
        metronomeJob?.cancel()
        metronomeJob = null
        recordingJob?.cancel()
        recordingJob = null

        // 最後の（未完了の）拍をフラッシュ
        if (lastBeatIndex >= 0) {
            val dominant = currentBeatNotes
                .groupingBy { it }.eachCount()
                .maxByOrNull { it.value }?.key
            finishedBeats.add(dominant)
        }
        currentBeatNotes.clear()

        _uiState.update { it.copy(currentBeat = -1) }

        val segments = ChordSuggester.buildSegmentsFromBeats(finishedBeats, beatDurationMs)

        val nextScreen = if (segments.isNotEmpty()) AppScreen.ChordSelection else AppScreen.Home
        _uiState.update {
            it.copy(
                isRecording = false,
                screen = nextScreen,
                segments = segments,
                currentDetectedNote = null
            )
        }
        if (nextScreen == AppScreen.Home) startMetronomePreview()
    }

    /**
     * セグメントのコード選択を更新する。
     */
    fun selectChord(segmentIndex: Int, chordIndex: Int) {
        val segments = _uiState.value.segments.toMutableList()
        if (segmentIndex < segments.size) {
            segments[segmentIndex] = segments[segmentIndex].copy(selectedChordIndex = chordIndex)
            _uiState.update { it.copy(segments = segments) }

            // 選択したコードをプレビュー再生
            val chord = segments[segmentIndex].suggestedChords.getOrNull(chordIndex)
            if (chord != null) {
                viewModelScope.launch {
                    chordPlayer.previewChord(chord)
                }
            }
        }
    }

    /**
     * 選択したコード進行を再生する。
     */
    fun playProgression() {
        if (_uiState.value.isPlaying) return

        val chords = _uiState.value.segments.mapNotNull { segment ->
            segment.suggestedChords.getOrNull(segment.selectedChordIndex)
        }

        if (chords.isEmpty()) return

        _uiState.update { it.copy(isPlaying = true, playingChordIndex = -1) }

        playbackJob = viewModelScope.launch {
            chordPlayer.playProgression(chords) { chordIndex ->
                _uiState.update { it.copy(playingChordIndex = chordIndex) }
            }
            _uiState.update { it.copy(isPlaying = false, playingChordIndex = -1) }
        }
    }

    /**
     * 再生を停止する。
     */
    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        chordPlayer.stop()
        _uiState.update { it.copy(isPlaying = false, playingChordIndex = -1) }
    }

    /**
     * ホーム画面に戻る。
     */
    fun goHome() {
        stopMetronomePreview()
        metronomeJob?.cancel()
        metronomeJob = null
        stopPlayback()
        recordingJob?.cancel()
        recordingJob = null
        // BPM・拍子設定はリセットせず引き継ぐ
        val savedBpm = _uiState.value.bpm
        val savedTs = _uiState.value.timeSignature
        _uiState.update {
            UiState(screen = AppScreen.Home, bpm = savedBpm, timeSignature = savedTs)
        }
        startMetronomePreview()
    }

    /**
     * コード選択画面に戻る（再生中に使用）。
     */
    fun goToChordSelection() {
        stopPlayback()
        _uiState.update { it.copy(screen = AppScreen.ChordSelection) }
    }

    override fun onCleared() {
        super.onCleared()
        previewDebounceJob?.cancel()
        previewMetronomeJob?.cancel()
        metronomeJob?.cancel()
        recordingJob?.cancel()
        playbackJob?.cancel()
        chordPlayer.stop()
        metronome.release()
    }
}
