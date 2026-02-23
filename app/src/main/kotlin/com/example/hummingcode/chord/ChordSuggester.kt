package com.example.hummingcode.chord

import com.example.hummingcode.model.Chord
import com.example.hummingcode.model.ChordType
import com.example.hummingcode.model.NoteSegment

/**
 * 検出された音名からコード候補を提案するクラス。
 * 音楽理論に基づき、その音を構成音に含むコードを提案する。
 */
object ChordSuggester {

    // 各音名に対するコード候補
    // ルート10種(M, m, M7, m7, 7, sus4, add9, dim, aug, sus2) + その音が5th/min3rdになるコード
    private val chordSuggestionsMap: Map<String, List<Chord>> = mapOf(
        "C" to listOf(
            Chord("C", ChordType.MAJOR),   Chord("C", ChordType.MINOR),
            Chord("C", ChordType.MAJOR7),  Chord("C", ChordType.MINOR7),
            Chord("C", ChordType.DOMINANT7), Chord("C", ChordType.SUS4),
            Chord("C", ChordType.ADD9),    Chord("C", ChordType.DIM),
            Chord("C", ChordType.AUG),     Chord("C", ChordType.SUS2),
            Chord("F", ChordType.MAJOR),   Chord("A", ChordType.MINOR)
        ),
        "C#" to listOf(
            Chord("C#", ChordType.MAJOR),   Chord("C#", ChordType.MINOR),
            Chord("C#", ChordType.MAJOR7),  Chord("C#", ChordType.MINOR7),
            Chord("C#", ChordType.DOMINANT7), Chord("C#", ChordType.SUS4),
            Chord("C#", ChordType.ADD9),    Chord("C#", ChordType.DIM),
            Chord("C#", ChordType.AUG),     Chord("C#", ChordType.SUS2),
            Chord("F#", ChordType.MAJOR),   Chord("A#", ChordType.MINOR)
        ),
        "D" to listOf(
            Chord("D", ChordType.MAJOR),   Chord("D", ChordType.MINOR),
            Chord("D", ChordType.MAJOR7),  Chord("D", ChordType.MINOR7),
            Chord("D", ChordType.DOMINANT7), Chord("D", ChordType.SUS4),
            Chord("D", ChordType.ADD9),    Chord("D", ChordType.DIM),
            Chord("D", ChordType.AUG),     Chord("D", ChordType.SUS2),
            Chord("G", ChordType.MAJOR),   Chord("B", ChordType.MINOR)
        ),
        "D#" to listOf(
            Chord("D#", ChordType.MAJOR),   Chord("D#", ChordType.MINOR),
            Chord("D#", ChordType.MAJOR7),  Chord("D#", ChordType.MINOR7),
            Chord("D#", ChordType.DOMINANT7), Chord("D#", ChordType.SUS4),
            Chord("D#", ChordType.ADD9),    Chord("D#", ChordType.DIM),
            Chord("D#", ChordType.AUG),     Chord("D#", ChordType.SUS2),
            Chord("G#", ChordType.MAJOR),   Chord("C", ChordType.MINOR)
        ),
        "E" to listOf(
            Chord("E", ChordType.MAJOR),   Chord("E", ChordType.MINOR),
            Chord("E", ChordType.MAJOR7),  Chord("E", ChordType.MINOR7),
            Chord("E", ChordType.DOMINANT7), Chord("E", ChordType.SUS4),
            Chord("E", ChordType.ADD9),    Chord("E", ChordType.DIM),
            Chord("E", ChordType.AUG),     Chord("E", ChordType.SUS2),
            Chord("A", ChordType.MAJOR),   Chord("C#", ChordType.MINOR)
        ),
        "F" to listOf(
            Chord("F", ChordType.MAJOR),   Chord("F", ChordType.MINOR),
            Chord("F", ChordType.MAJOR7),  Chord("F", ChordType.MINOR7),
            Chord("F", ChordType.DOMINANT7), Chord("F", ChordType.SUS4),
            Chord("F", ChordType.ADD9),    Chord("F", ChordType.DIM),
            Chord("F", ChordType.AUG),     Chord("F", ChordType.SUS2),
            Chord("A#", ChordType.MAJOR),  Chord("D", ChordType.MINOR)
        ),
        "F#" to listOf(
            Chord("F#", ChordType.MAJOR),   Chord("F#", ChordType.MINOR),
            Chord("F#", ChordType.MAJOR7),  Chord("F#", ChordType.MINOR7),
            Chord("F#", ChordType.DOMINANT7), Chord("F#", ChordType.SUS4),
            Chord("F#", ChordType.ADD9),    Chord("F#", ChordType.DIM),
            Chord("F#", ChordType.AUG),     Chord("F#", ChordType.SUS2),
            Chord("B", ChordType.MAJOR),    Chord("D#", ChordType.MINOR)
        ),
        "G" to listOf(
            Chord("G", ChordType.MAJOR),   Chord("G", ChordType.MINOR),
            Chord("G", ChordType.MAJOR7),  Chord("G", ChordType.MINOR7),
            Chord("G", ChordType.DOMINANT7), Chord("G", ChordType.SUS4),
            Chord("G", ChordType.ADD9),    Chord("G", ChordType.DIM),
            Chord("G", ChordType.AUG),     Chord("G", ChordType.SUS2),
            Chord("C", ChordType.MAJOR),   Chord("E", ChordType.MINOR)
        ),
        "G#" to listOf(
            Chord("G#", ChordType.MAJOR),   Chord("G#", ChordType.MINOR),
            Chord("G#", ChordType.MAJOR7),  Chord("G#", ChordType.MINOR7),
            Chord("G#", ChordType.DOMINANT7), Chord("G#", ChordType.SUS4),
            Chord("G#", ChordType.ADD9),    Chord("G#", ChordType.DIM),
            Chord("G#", ChordType.AUG),     Chord("G#", ChordType.SUS2),
            Chord("C#", ChordType.MAJOR),   Chord("F", ChordType.MINOR)
        ),
        "A" to listOf(
            Chord("A", ChordType.MAJOR),   Chord("A", ChordType.MINOR),
            Chord("A", ChordType.MAJOR7),  Chord("A", ChordType.MINOR7),
            Chord("A", ChordType.DOMINANT7), Chord("A", ChordType.SUS4),
            Chord("A", ChordType.ADD9),    Chord("A", ChordType.DIM),
            Chord("A", ChordType.AUG),     Chord("A", ChordType.SUS2),
            Chord("D", ChordType.MAJOR),   Chord("F#", ChordType.MINOR)
        ),
        "A#" to listOf(
            Chord("A#", ChordType.MAJOR),   Chord("A#", ChordType.MINOR),
            Chord("A#", ChordType.MAJOR7),  Chord("A#", ChordType.MINOR7),
            Chord("A#", ChordType.DOMINANT7), Chord("A#", ChordType.SUS4),
            Chord("A#", ChordType.ADD9),    Chord("A#", ChordType.DIM),
            Chord("A#", ChordType.AUG),     Chord("A#", ChordType.SUS2),
            Chord("D#", ChordType.MAJOR),   Chord("G", ChordType.MINOR)
        ),
        "B" to listOf(
            Chord("B", ChordType.MAJOR),   Chord("B", ChordType.MINOR),
            Chord("B", ChordType.MAJOR7),  Chord("B", ChordType.MINOR7),
            Chord("B", ChordType.DOMINANT7), Chord("B", ChordType.SUS4),
            Chord("B", ChordType.ADD9),    Chord("B", ChordType.DIM),
            Chord("B", ChordType.AUG),     Chord("B", ChordType.SUS2),
            Chord("E", ChordType.MAJOR),   Chord("G#", ChordType.MINOR)
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
                segments.add(createSegment(currentNote, count * 100L, 1))
                currentNote = noteHistory[i]
                count = 1
            }
        }
        segments.add(createSegment(currentNote, count * 100L, 1))

        return segments
    }

    private fun createSegment(noteName: String, durationMs: Long, beatCount: Int): NoteSegment {
        val chords = chordSuggestionsMap[noteName] ?: listOf(
            Chord(noteName, ChordType.MAJOR),
            Chord(noteName, ChordType.MINOR)
        )
        return NoteSegment(
            noteName = noteName,
            durationMs = durationMs,
            beatCount = beatCount,
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
                    segments.add(createSegment(currentNote, beatDurationMs * beatCount, beatCount))
                    currentNote = null
                    beatCount = 0
                }
            } else if (note == currentNote) {
                beatCount++
            } else {
                if (currentNote != null) {
                    segments.add(createSegment(currentNote, beatDurationMs * beatCount, beatCount))
                }
                currentNote = note
                beatCount = 1
            }
        }
        if (currentNote != null && beatCount > 0) {
            segments.add(createSegment(currentNote, beatDurationMs * beatCount, beatCount))
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
