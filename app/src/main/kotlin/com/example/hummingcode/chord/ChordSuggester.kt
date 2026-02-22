package com.example.hummingcode.chord

import com.example.hummingcode.model.Chord
import com.example.hummingcode.model.ChordType
import com.example.hummingcode.model.NoteSegment

/**
 * 検出された音名からコード候補を提案するクラス。
 * 音楽理論に基づき、その音を構成音に含むコードを提案する。
 */
object ChordSuggester {

    // 各音名に対するコード候補 (音楽的に自然な選択)
    // 音がルート音・3度・5度になるコードを優先
    private val chordSuggestionsMap: Map<String, List<Chord>> = mapOf(
        "C" to listOf(
            Chord("C", ChordType.MAJOR),
            Chord("C", ChordType.MINOR),
            Chord("F", ChordType.MAJOR),
            Chord("A", ChordType.MINOR),
            Chord("A", ChordType.MAJOR),
            Chord("G", ChordType.DOMINANT7)
        ),
        "C#" to listOf(
            Chord("C#", ChordType.MAJOR),
            Chord("C#", ChordType.MINOR),
            Chord("F#", ChordType.MAJOR),
            Chord("A#", ChordType.MINOR),
            Chord("A", ChordType.MAJOR),
            Chord("G#", ChordType.DOMINANT7)
        ),
        "D" to listOf(
            Chord("D", ChordType.MAJOR),
            Chord("D", ChordType.MINOR),
            Chord("G", ChordType.MAJOR),
            Chord("B", ChordType.MINOR),
            Chord("A", ChordType.DOMINANT7),
            Chord("F#", ChordType.MINOR)
        ),
        "D#" to listOf(
            Chord("D#", ChordType.MAJOR),
            Chord("D#", ChordType.MINOR),
            Chord("G#", ChordType.MAJOR),
            Chord("C", ChordType.MINOR),
            Chord("A#", ChordType.DOMINANT7),
            Chord("G", ChordType.MINOR)
        ),
        "E" to listOf(
            Chord("E", ChordType.MAJOR),
            Chord("E", ChordType.MINOR),
            Chord("A", ChordType.MAJOR),
            Chord("C#", ChordType.MINOR),
            Chord("B", ChordType.DOMINANT7),
            Chord("G#", ChordType.MINOR)
        ),
        "F" to listOf(
            Chord("F", ChordType.MAJOR),
            Chord("F", ChordType.MINOR),
            Chord("A#", ChordType.MAJOR),
            Chord("D", ChordType.MINOR),
            Chord("C", ChordType.DOMINANT7),
            Chord("A", ChordType.MINOR)
        ),
        "F#" to listOf(
            Chord("F#", ChordType.MAJOR),
            Chord("F#", ChordType.MINOR),
            Chord("B", ChordType.MAJOR),
            Chord("D#", ChordType.MINOR),
            Chord("C#", ChordType.DOMINANT7),
            Chord("A#", ChordType.MINOR)
        ),
        "G" to listOf(
            Chord("G", ChordType.MAJOR),
            Chord("G", ChordType.MINOR),
            Chord("C", ChordType.MAJOR),
            Chord("E", ChordType.MINOR),
            Chord("D", ChordType.DOMINANT7),
            Chord("B", ChordType.MINOR)
        ),
        "G#" to listOf(
            Chord("G#", ChordType.MAJOR),
            Chord("G#", ChordType.MINOR),
            Chord("C#", ChordType.MAJOR),
            Chord("F", ChordType.MINOR),
            Chord("D#", ChordType.DOMINANT7),
            Chord("C", ChordType.MINOR)
        ),
        "A" to listOf(
            Chord("A", ChordType.MAJOR),
            Chord("A", ChordType.MINOR),
            Chord("D", ChordType.MAJOR),
            Chord("F#", ChordType.MINOR),
            Chord("E", ChordType.DOMINANT7),
            Chord("C#", ChordType.MINOR)
        ),
        "A#" to listOf(
            Chord("A#", ChordType.MAJOR),
            Chord("A#", ChordType.MINOR),
            Chord("D#", ChordType.MAJOR),
            Chord("G", ChordType.MINOR),
            Chord("F", ChordType.DOMINANT7),
            Chord("D", ChordType.MINOR)
        ),
        "B" to listOf(
            Chord("B", ChordType.MAJOR),
            Chord("B", ChordType.MINOR),
            Chord("E", ChordType.MAJOR),
            Chord("G#", ChordType.MINOR),
            Chord("F#", ChordType.DOMINANT7),
            Chord("D#", ChordType.MINOR)
        )
    )

    /**
     * 音名のリストからNoteSegmentのリストを生成する。
     * 連続して同じ音が続く場合はひとつのセグメントにまとめる。
     */
    fun buildSegments(noteHistory: List<String>): List<NoteSegment> {
        if (noteHistory.isEmpty()) return emptyList()

        val segments = mutableListOf<NoteSegment>()
        var currentNote = noteHistory[0]
        var count = 1

        for (i in 1 until noteHistory.size) {
            if (noteHistory[i] == currentNote) {
                count++
            } else {
                segments.add(createSegment(currentNote, count * 100L))
                currentNote = noteHistory[i]
                count = 1
            }
        }
        segments.add(createSegment(currentNote, count * 100L))

        return segments
    }

    private fun createSegment(noteName: String, durationMs: Long): NoteSegment {
        val chords = chordSuggestionsMap[noteName] ?: listOf(
            Chord(noteName, ChordType.MAJOR),
            Chord(noteName, ChordType.MINOR)
        )
        return NoteSegment(
            noteName = noteName,
            durationMs = durationMs,
            suggestedChords = chords,
            selectedChordIndex = 0
        )
    }

    /**
     * 拍ごとの支配的な音名リスト（null=無音拍）からセグメントを生成する。
     * 連続する同じ音名の拍はひとつのセグメントにまとめる。最小単位は1拍。
     */
    fun buildSegmentsFromBeats(beatNotes: List<String?>, beatDurationMs: Long): List<NoteSegment> {
        if (beatNotes.isEmpty()) return emptyList()

        val segments = mutableListOf<NoteSegment>()
        var currentNote: String? = null
        var beatCount = 0

        for (note in beatNotes) {
            if (note == null) {
                // 無音拍: 直前のセグメントを確定
                if (currentNote != null) {
                    segments.add(createSegment(currentNote, beatDurationMs * beatCount))
                    currentNote = null
                    beatCount = 0
                }
            } else if (note == currentNote) {
                beatCount++
            } else {
                if (currentNote != null) {
                    segments.add(createSegment(currentNote, beatDurationMs * beatCount))
                }
                currentNote = note
                beatCount = 1
            }
        }
        if (currentNote != null && beatCount > 0) {
            segments.add(createSegment(currentNote, beatDurationMs * beatCount))
        }
        return segments
    }

    /**
     * コード候補を返す（音名指定）
     */
    fun getSuggestionsFor(noteName: String): List<Chord> {
        return chordSuggestionsMap[noteName] ?: listOf(
            Chord(noteName, ChordType.MAJOR),
            Chord(noteName, ChordType.MINOR)
        )
    }
}
