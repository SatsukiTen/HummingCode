package com.example.hummingcode.model

import kotlinx.serialization.Serializable

@Serializable
data class SavedSegment(
    val noteName: String,
    val durationMs: Long,
    val beatCount: Int,
    val selectedChordIndex: Int,
    val octaveShift: Int,
    val selectedChordDisplay: String  // 表示用に保存時に確定しておく
)

@Serializable
data class SavedProgression(
    val id: String,
    val title: String,
    val savedAt: Long,             // epoch millis
    val bpm: Int,
    val timeSignatureNumerator: Int,
    val timeSignatureDenominator: Int,
    val segments: List<SavedSegment>
)
