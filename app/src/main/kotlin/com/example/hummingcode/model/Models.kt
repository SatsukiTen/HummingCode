package com.example.hummingcode.model

enum class ChordType(val displayName: String, val intervals: List<Int>) {
    MAJOR("", listOf(0, 4, 7)),
    MINOR("m", listOf(0, 3, 7)),
    MAJOR7("M7", listOf(0, 4, 7, 11)),
    MINOR7("m7", listOf(0, 3, 7, 10)),
    DOMINANT7("7", listOf(0, 4, 7, 10)),
    SUS4("sus4", listOf(0, 5, 7)),
    ADD9("add9", listOf(0, 4, 7, 14)),
    DIM("dim", listOf(0, 3, 6)),
    AUG("aug", listOf(0, 4, 8)),
    SUS2("sus2", listOf(0, 2, 7))
}

data class Chord(
    val rootName: String,
    val type: ChordType
) {
    val displayName: String get() = "$rootName${type.displayName}"

    fun getMidiNotes(baseOctave: Int = 4): List<Int> {
        val rootMidi = noteNameToMidi(rootName, baseOctave)
        return type.intervals.map { rootMidi + it }
    }

    private fun noteNameToMidi(name: String, octave: Int): Int {
        val semitone = NOTE_SEMITONES[name] ?: 0
        return (octave + 1) * 12 + semitone
    }

    companion object {
        val NOTE_SEMITONES = mapOf(
            "C" to 0, "C#" to 1, "D" to 2, "D#" to 3,
            "E" to 4, "F" to 5, "F#" to 6, "G" to 7,
            "G#" to 8, "A" to 9, "A#" to 10, "B" to 11
        )
    }
}

data class NoteSegment(
    val noteName: String,
    val durationMs: Long,
    val beatCount: Int,
    val suggestedChords: List<Chord>,
    val selectedChordIndex: Int = 0,
    val octaveShift: Int = 0
)

data class DetectedNote(
    val name: String,
    val frequency: Float,
    val startTimeMs: Long,
    val endTimeMs: Long
)

sealed class AppScreen {
    object Home : AppScreen()
    object Recording : AppScreen()
    object ChordSelection : AppScreen()
    object SavedList : AppScreen()
}

data class TimeSignature(val numerator: Int, val denominator: Int) {
    override fun toString() = "$numerator/$denominator"

    companion object {
        val PRESETS = listOf(
            TimeSignature(4, 4), TimeSignature(3, 4),
            TimeSignature(2, 4), TimeSignature(6, 8)
        )
    }
}

data class UiState(
    val screen: AppScreen = AppScreen.Home,
    val isRecording: Boolean = false,
    val isCountingDown: Boolean = false,
    val countdownBeatsRemaining: Int = 0,
    val currentDetectedNote: String? = null,
    val segments: List<NoteSegment> = emptyList(),
    val isPlaying: Boolean = false,
    val playingChordIndex: Int = -1,
    val errorMessage: String? = null,
    val bpm: Int = 120,
    val timeSignature: TimeSignature = TimeSignature(4, 4),
    val currentBeat: Int = -1,
    val savedProgressions: List<SavedProgression> = emptyList()
)
