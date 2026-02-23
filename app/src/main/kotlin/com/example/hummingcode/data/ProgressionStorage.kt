package com.example.hummingcode.data

import android.content.Context
import com.example.hummingcode.model.SavedProgression
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class ProgressionStorage(context: Context) {

    private val file = File(context.filesDir, "progressions.json")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun loadAll(): List<SavedProgression> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext emptyList()
        try {
            json.decodeFromString<List<SavedProgression>>(file.readText())
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun saveAll(progressions: List<SavedProgression>) = withContext(Dispatchers.IO) {
        file.writeText(json.encodeToString(progressions))
    }
}
