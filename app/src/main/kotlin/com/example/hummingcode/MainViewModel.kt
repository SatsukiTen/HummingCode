package com.example.hummingcode

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hummingcode.audio.AudioRecorder
import com.example.hummingcode.audio.DetectedPitch
import com.example.hummingcode.audio.Metronome
import com.example.hummingcode.chord.ChordPlayer
import com.example.hummingcode.chord.ChordSuggester
import com.example.hummingcode.data.ProgressionStorage
import com.example.hummingcode.model.AppScreen
import com.example.hummingcode.model.NoteSegment
import com.example.hummingcode.model.SavedProgression
import com.example.hummingcode.model.SavedSegment
import com.example.hummingcode.model.TimeSignature
import com.example.hummingcode.model.UiState
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val audioRecorder = AudioRecorder()
    private val chordPlayer = ChordPlayer()
    private val metronome = Metronome()
    private val storage = ProgressionStorage(application)

    private val noteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

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
    // 1拍内で支配的音名として採用するのに必要な最小検出フレーム数
    private val minBeatDetections = 2

    // ホーム画面プレビュー用メトロノーム
    private var previewMetronomeJob: Job? = null
    private var previewDebounceJob: Job? = null

    // タップテンポ: 直近タップ時刻リスト
    private val tapTimes = mutableListOf<Long>()

    init {
        startMetronomePreview()
        viewModelScope.launch {
            val progressions = storage.loadAll()
            _uiState.update { it.copy(savedProgressions = progressions) }
        }
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
        if (_uiState.value.isRecording || _uiState.value.isCountingDown) return

        stopMetronomePreview()
        currentBeatNotes.clear()
        finishedBeats.clear()
        beatDurationMs = 60_000L / _uiState.value.bpm
        lastBeatIndex = -1

        val beatsPerMeasure = _uiState.value.timeSignature.numerator
        val totalCountdownBeats = 2 * beatsPerMeasure
        var countdownBeatsLeft = totalCountdownBeats

        _uiState.update {
            it.copy(
                screen = AppScreen.Recording,
                isRecording = false,
                isCountingDown = true,
                countdownBeatsRemaining = totalCountdownBeats,
                currentDetectedNote = null,
                segments = emptyList(),
                errorMessage = null
            )
        }

        metronomeJob = viewModelScope.launch {
            metronome.start(
                bpm = _uiState.value.bpm,
                beatsPerMeasure = beatsPerMeasure
            ).collect { beatIndex ->
                if (countdownBeatsLeft > 0) {
                    countdownBeatsLeft--
                    if (countdownBeatsLeft == 0) {
                        // カウントダウン終了 → 録音開始
                        _uiState.update {
                            it.copy(
                                currentBeat = beatIndex,
                                countdownBeatsRemaining = 0,
                                isCountingDown = false,
                                isRecording = true
                            )
                        }
                        recordingJob = viewModelScope.launch {
                            audioRecorder.recordAndDetect().collect { pitch ->
                                when (pitch) {
                                    is DetectedPitch.Success -> {
                                        val note = pitch.noteName
                                        _uiState.update { it.copy(currentDetectedNote = note) }
                                        if (note != null) currentBeatNotes.add(note)
                                    }
                                    is DetectedPitch.Error -> {
                                        _uiState.update { it.copy(errorMessage = pitch.message) }
                                    }
                                }
                            }
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                currentBeat = beatIndex,
                                countdownBeatsRemaining = countdownBeatsLeft
                            )
                        }
                    }
                } else {
                    // 録音フェーズのビート処理
                    if (lastBeatIndex >= 0) {
                        finishedBeats.add(dominantNote(currentBeatNotes))
                        currentBeatNotes.clear()
                    }
                    lastBeatIndex = beatIndex
                    _uiState.update { it.copy(currentBeat = beatIndex) }
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
            finishedBeats.add(dominantNote(currentBeatNotes))
        }
        currentBeatNotes.clear()

        val segments = ChordSuggester.buildSegmentsFromBeats(finishedBeats, beatDurationMs)

        val nextScreen = if (segments.isNotEmpty()) AppScreen.ChordSelection else AppScreen.Home
        _uiState.update {
            it.copy(
                isRecording = false,
                isCountingDown = false,
                countdownBeatsRemaining = 0,
                currentBeat = -1,
                screen = nextScreen,
                segments = segments,
                currentDetectedNote = null
            )
        }
        if (nextScreen == AppScreen.Home) startMetronomePreview()
    }

    /**
     * 拍内の音名リストから支配的音名を返す。
     * 最頻の音名が minBeatDetections 回以上出現しない場合は null（無音扱い）。
     */
    private fun dominantNote(notes: List<String>): String? {
        val counts = notes.groupingBy { it }.eachCount()
        val entry = counts.maxByOrNull { it.value } ?: return null
        return if (entry.value >= minBeatDetections) entry.key else null
    }

    /**
     * セグメントの拍数を更新する。0 を指定するとそのセグメントをスキップ扱いにする。
     */
    fun updateBeatCount(segmentIndex: Int, beatCount: Int) {
        val newCount = beatCount.coerceAtLeast(0)
        val segments = _uiState.value.segments.toMutableList()
        if (segmentIndex < segments.size) {
            val seg = segments[segmentIndex]
            segments[segmentIndex] = seg.copy(
                beatCount = newCount,
                durationMs = beatDurationMs * newCount
            )
            _uiState.update { it.copy(segments = segments) }
        }
    }

    /**
     * 特定セグメントのオクターブを1段階上下する (-2〜+2 の範囲でクランプ)。
     */
    fun adjustSegmentOctave(segmentIndex: Int, delta: Int) {
        val segments = _uiState.value.segments.toMutableList()
        if (segmentIndex < segments.size) {
            val seg = segments[segmentIndex]
            segments[segmentIndex] = seg.copy(
                octaveShift = (seg.octaveShift + delta).coerceIn(-2, 2)
            )
            _uiState.update { it.copy(segments = segments) }
        }
    }

    /**
     * セグメントの基準音名を半音単位で上下する。
     * 音名が変わるとコード候補も自動的に更新される。
     */
    fun shiftSegmentNote(segmentIndex: Int, semitones: Int) {
        val segments = _uiState.value.segments.toMutableList()
        if (segmentIndex >= segments.size) return
        val seg = segments[segmentIndex]
        val currentIdx = noteNames.indexOf(seg.noteName)
        if (currentIdx < 0) return
        val newIdx = (currentIdx + semitones + 12) % 12
        val newNoteName = noteNames[newIdx]
        val newChords = ChordSuggester.getSuggestionsFor(newNoteName)
        segments[segmentIndex] = seg.copy(
            noteName = newNoteName,
            suggestedChords = newChords,
            selectedChordIndex = 0
        )
        _uiState.update { it.copy(segments = segments) }
    }

    /**
     * セグメントのコード選択を更新する。
     */
    fun selectChord(segmentIndex: Int, chordIndex: Int) {
        val segments = _uiState.value.segments.toMutableList()
        if (segmentIndex < segments.size) {
            segments[segmentIndex] = segments[segmentIndex].copy(selectedChordIndex = chordIndex)
            _uiState.update { it.copy(segments = segments) }

            // 選択したコードをプレビュー再生 (セグメントのオクターブで)
            val chord = segments[segmentIndex].suggestedChords.getOrNull(chordIndex)
            if (chord != null) {
                val baseOctave = 3 + segments[segmentIndex].octaveShift
                viewModelScope.launch {
                    chordPlayer.previewChord(chord, baseOctave)
                }
            }
        }
    }

    /**
     * 選択したコード進行を再生する。各セグメントの durationMs を使用する。
     */
    fun playProgression() {
        if (_uiState.value.isPlaying) return

        // 元のセグメントインデックスを保持しながらスキップセグメントを除外
        val playableEntries = _uiState.value.segments
            .mapIndexedNotNull { index, segment ->
                if (segment.beatCount == 0) return@mapIndexedNotNull null
                val chord = segment.suggestedChords.getOrNull(segment.selectedChordIndex)
                    ?: return@mapIndexedNotNull null
                Triple(index, chord, segment.durationMs)
            }

        if (playableEntries.isEmpty()) return

        val chords = playableEntries.map { it.second }
        val durations = playableEntries.map { it.third }
        val originalIndices = playableEntries.map { it.first }
        val baseOctaves = originalIndices.map { origIdx ->
            3 + _uiState.value.segments[origIdx].octaveShift
        }

        _uiState.update { it.copy(isPlaying = true, playingChordIndex = -1) }

        playbackJob = viewModelScope.launch {
            chordPlayer.playProgression(chords, durations, baseOctaves) { filteredIndex ->
                // filteredIndex を元のセグメントインデックスに変換 (-1 は再生終了)
                val segmentIndex = originalIndices.getOrElse(filteredIndex) { -1 }
                _uiState.update { it.copy(playingChordIndex = segmentIndex) }
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
        // BPM・拍子・保存済みリストは引き継ぐ
        val savedBpm = _uiState.value.bpm
        val savedTs = _uiState.value.timeSignature
        val savedProgressions = _uiState.value.savedProgressions
        _uiState.update {
            UiState(
                screen = AppScreen.Home,
                bpm = savedBpm,
                timeSignature = savedTs,
                savedProgressions = savedProgressions
            )
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

    /** 保存済みリスト画面へ遷移する。 */
    fun goToSavedList() {
        stopMetronomePreview()
        _uiState.update { it.copy(screen = AppScreen.SavedList) }
    }

    /**
     * 現在のコード進行にタイトルをつけて保存する。
     */
    fun saveProgression(title: String) {
        val state = _uiState.value
        val savedSegments = state.segments.map { seg ->
            val chord = seg.suggestedChords.getOrNull(seg.selectedChordIndex)
            SavedSegment(
                noteName = seg.noteName,
                durationMs = seg.durationMs,
                beatCount = seg.beatCount,
                selectedChordIndex = seg.selectedChordIndex,
                octaveShift = seg.octaveShift,
                selectedChordDisplay = chord?.displayName ?: seg.noteName
            )
        }
        val progression = SavedProgression(
            id = UUID.randomUUID().toString(),
            title = title,
            savedAt = System.currentTimeMillis(),
            bpm = state.bpm,
            timeSignatureNumerator = state.timeSignature.numerator,
            timeSignatureDenominator = state.timeSignature.denominator,
            segments = savedSegments
        )
        viewModelScope.launch {
            val updated = _uiState.value.savedProgressions + progression
            storage.saveAll(updated)
            _uiState.update { it.copy(savedProgressions = updated) }
        }
    }

    /**
     * 保存済みコード進行を読み込んでコード選択画面へ遷移する。
     */
    fun loadProgression(progression: SavedProgression) {
        val segments = progression.segments.map { saved ->
            val chords = ChordSuggester.getSuggestionsFor(saved.noteName)
            NoteSegment(
                noteName = saved.noteName,
                durationMs = saved.durationMs,
                beatCount = saved.beatCount,
                suggestedChords = chords,
                selectedChordIndex = saved.selectedChordIndex.coerceIn(0, (chords.size - 1).coerceAtLeast(0)),
                octaveShift = saved.octaveShift
            )
        }
        val ts = TimeSignature(progression.timeSignatureNumerator, progression.timeSignatureDenominator)
        _uiState.update {
            it.copy(
                screen = AppScreen.ChordSelection,
                segments = segments,
                bpm = progression.bpm,
                timeSignature = ts,
                isPlaying = false,
                playingChordIndex = -1,
                currentBeat = -1
            )
        }
    }

    /**
     * 保存済みコード進行を削除する。
     */
    fun deleteProgression(id: String) {
        viewModelScope.launch {
            val updated = _uiState.value.savedProgressions.filter { it.id != id }
            storage.saveAll(updated)
            _uiState.update { it.copy(savedProgressions = updated) }
        }
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
