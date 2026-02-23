package com.example.hummingcode.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hummingcode.chord.ChordSuggester
import com.example.hummingcode.model.AppScreen
import com.example.hummingcode.model.NoteSegment
import com.example.hummingcode.model.SavedProgression
import com.example.hummingcode.model.TimeSignature
import com.example.hummingcode.model.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: UiState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onSelectChord: (Int, Int) -> Unit,
    onBeatCountChange: (Int, Int) -> Unit,
    onOctaveChange: (Int, Int) -> Unit,
    onNoteShift: (Int, Int) -> Unit,
    onPlayProgression: () -> Unit,
    onStopPlayback: () -> Unit,
    onGoHome: () -> Unit,
    onBpmChange: (Int) -> Unit,
    onTimeSignatureChange: (TimeSignature) -> Unit,
    onTapTempo: () -> Unit,
    onGoToSavedList: () -> Unit,
    onSaveProgression: (String) -> Unit,
    onDeleteProgression: (String) -> Unit,
    onLoadProgression: (SavedProgression) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "HummingCode",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                navigationIcon = {
                    if (uiState.screen != AppScreen.Home) {
                        IconButton(onClick = onGoHome) {
                            Icon(Icons.Default.Home, contentDescription = "ホームへ戻る")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = uiState.screen,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
            modifier = Modifier.padding(paddingValues),
            label = "screen_transition"
        ) { screen ->
            when (screen) {
                AppScreen.Home -> HomeScreen(
                    bpm = uiState.bpm,
                    timeSignature = uiState.timeSignature,
                    currentBeat = uiState.currentBeat,
                    beatsPerMeasure = uiState.timeSignature.numerator,
                    onStartRecording = onStartRecording,
                    onBpmChange = onBpmChange,
                    onTimeSignatureChange = onTimeSignatureChange,
                    onTapTempo = onTapTempo,
                    onGoToSavedList = onGoToSavedList
                )
                AppScreen.Recording -> RecordingScreen(
                    currentNote = uiState.currentDetectedNote,
                    currentBeat = uiState.currentBeat,
                    beatsPerMeasure = uiState.timeSignature.numerator,
                    isCountingDown = uiState.isCountingDown,
                    countdownBeatsRemaining = uiState.countdownBeatsRemaining,
                    onStopRecording = onStopRecording
                )
                AppScreen.ChordSelection -> ChordSelectionScreen(
                    segments = uiState.segments,
                    playingIndex = uiState.playingChordIndex,
                    isPlaying = uiState.isPlaying,
                    onSelectChord = onSelectChord,
                    onBeatCountChange = onBeatCountChange,
                    onOctaveChange = onOctaveChange,
                    onNoteShift = onNoteShift,
                    onSaveProgression = onSaveProgression,
                    onPlayProgression = onPlayProgression,
                    onStopPlayback = onStopPlayback,
                    onGoHome = onGoHome
                )
                AppScreen.SavedList -> SavedListScreen(
                    progressions = uiState.savedProgressions,
                    onLoadProgression = onLoadProgression,
                    onDeleteProgression = onDeleteProgression,
                    onGoHome = onGoHome
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// ホーム画面
// ────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    bpm: Int,
    timeSignature: TimeSignature,
    currentBeat: Int,
    beatsPerMeasure: Int,
    onStartRecording: () -> Unit,
    onBpmChange: (Int) -> Unit,
    onTimeSignatureChange: (TimeSignature) -> Unit,
    onTapTempo: () -> Unit,
    onGoToSavedList: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "鼻歌からコードを作ろう",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "録音ボタンを押して鼻歌を歌うと\nコード候補を提示します",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        // テンポ・拍子設定カード
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // BPM セクション
                Text("BPM", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { onBpmChange(bpm - 1) }) {
                        Text("−", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "$bpm",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(72.dp),
                        textAlign = TextAlign.Center
                    )
                    IconButton(onClick = { onBpmChange(bpm + 1) }) {
                        Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(
                        onClick = onTapTempo,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("TAP", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                Slider(
                    value = bpm.toFloat(),
                    onValueChange = { onBpmChange(it.toInt()) },
                    valueRange = 40f..240f,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // 拍子セクション
                Text("拍子", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TimeSignature.PRESETS.forEach { ts ->
                        FilterChip(
                            selected = timeSignature == ts,
                            onClick = { onTimeSignatureChange(ts) },
                            label = { Text(ts.toString()) }
                        )
                    }
                }
            }
        }

        // ビートインジケーター（プレビューメトロノーム動作中のみ表示）
        if (currentBeat >= 0) {
            Spacer(Modifier.height(20.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(beatsPerMeasure) { beatIndex ->
                    val isCurrentBeat = beatIndex == currentBeat
                    val circleColor by animateColorAsState(
                        targetValue = if (isCurrentBeat)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        animationSpec = tween(100),
                        label = "home_beat_$beatIndex"
                    )
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(circleColor, CircleShape)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        } else {
            Spacer(Modifier.height(32.dp))
        }
        RecordButton(
            isRecording = false,
            onClick = onStartRecording
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "タップして録音開始",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onGoToSavedList) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text("保存済みコード進行")
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// 録音画面
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun RecordingScreen(
    currentNote: String?,
    currentBeat: Int,
    beatsPerMeasure: Int,
    isCountingDown: Boolean = false,
    countdownBeatsRemaining: Int = 0,
    onStopRecording: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // カウントダウン表示用: 残り小節数 (ceiling division)
    val countdownMeasure = if (countdownBeatsRemaining > 0)
        (countdownBeatsRemaining + beatsPerMeasure - 1) / beatsPerMeasure
    else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ステータステキスト
        AnimatedContent(
            targetState = isCountingDown,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = "status_text"
        ) { counting ->
            Text(
                text = if (counting) "準備中..." else "録音中...",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (counting)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
        }
        Spacer(Modifier.height(32.dp))

        // 中央表示: カウントダウン数字 or 音名サークル
        AnimatedContent(
            targetState = isCountingDown,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = "center_display"
        ) { counting ->
            if (counting) {
                // 残り小節数を大きく表示
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$countdownMeasure",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            } else {
                // 検出中の音名を表示
                AnimatedContent(
                    targetState = currentNote,
                    transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                    label = "note_display"
                ) { note ->
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(
                                color = if (note != null)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = note ?: "—",
                            fontSize = if (note != null) 48.sp else 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (note != null)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = when {
                isCountingDown -> "あと $countdownMeasure 小節で録音開始"
                currentNote != null -> "検出: $currentNote"
                else -> "音を検出しています..."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 拍インジケーター
        if (currentBeat >= 0) {
            Spacer(Modifier.height(24.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(beatsPerMeasure) { beatIndex ->
                    val isCurrentBeat = beatIndex == currentBeat
                    val circleColor by animateColorAsState(
                        targetValue = if (isCurrentBeat)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        animationSpec = tween(100),
                        label = "beat_color_$beatIndex"
                    )
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(circleColor, CircleShape)
                    )
                }
            }
        }

        Spacer(Modifier.height(48.dp))

        // 停止 / キャンセルボタン
        Box(
            modifier = Modifier
                .scale(if (isCountingDown) 1f else scale)
                .size(80.dp)
                .background(
                    if (isCountingDown)
                        MaterialTheme.colorScheme.surfaceVariant
                    else
                        MaterialTheme.colorScheme.error,
                    CircleShape
                )
                .clickable(onClick = onStopRecording),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MicOff,
                contentDescription = if (isCountingDown) "キャンセル" else "録音停止",
                modifier = Modifier.size(40.dp),
                tint = if (isCountingDown)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onError
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (isCountingDown) "タップしてキャンセル" else "タップして録音停止",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ────────────────────────────────────────────────────────────────────────────
// コード選択画面
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChordSelectionScreen(
    segments: List<NoteSegment>,
    playingIndex: Int = -1,
    isPlaying: Boolean = false,
    onSelectChord: (Int, Int) -> Unit,
    onBeatCountChange: (Int, Int) -> Unit,
    onOctaveChange: (Int, Int) -> Unit,
    onNoteShift: (Int, Int) -> Unit,
    onSaveProgression: (String) -> Unit,
    onPlayProgression: () -> Unit,
    onStopPlayback: () -> Unit = {},
    onGoHome: () -> Unit
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveTitle by remember { mutableStateOf("") }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("コード進行を保存") },
            text = {
                OutlinedTextField(
                    value = saveTitle,
                    onValueChange = { saveTitle = it },
                    label = { Text("タイトル") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (saveTitle.isNotBlank()) {
                            onSaveProgression(saveTitle.trim())
                            showSaveDialog = false
                        }
                    },
                    enabled = saveTitle.isNotBlank()
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("キャンセル") }
            }
        )
    }
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        if (segments.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "音が検出されませんでした",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onGoHome) {
                        Text("もう一度録音する")
                    }
                }
            }
            return
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "コードを選択してください",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    saveTitle = ""
                    showSaveDialog = true
                },
                enabled = !isPlaying && segments.isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = "保存",
                    tint = if (!isPlaying && segments.isNotEmpty())
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(segments) { segmentIndex, segment ->
                val isCurrentlyPlaying = playingIndex == segmentIndex
                val isSkipped = segment.beatCount == 0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isCurrentlyPlaying -> MaterialTheme.colorScheme.primaryContainer
                            isSkipped -> MaterialTheme.colorScheme.surface
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isCurrentlyPlaying) 4.dp else 1.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .then(if (isSkipped) Modifier.background(Color.Transparent) else Modifier)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 音名バッジ（◀ 音名 ▶ でピッチ調整）
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (!isPlaying && !isSkipped) onNoteShift(segmentIndex, -1) },
                                    enabled = !isPlaying && !isSkipped,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Text(
                                        "◀",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isPlaying && !isSkipped)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            if (isSkipped)
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                            else
                                                MaterialTheme.colorScheme.primary,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = segment.noteName,
                                        color = if (isSkipped)
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        else
                                            MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                IconButton(
                                    onClick = { if (!isPlaying && !isSkipped) onNoteShift(segmentIndex, +1) },
                                    enabled = !isPlaying && !isSkipped,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Text(
                                        "▶",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isPlaying && !isSkipped)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                }
                            }
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "フレーズ ${segmentIndex + 1}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = if (isSkipped)
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                            if (isCurrentlyPlaying) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "♪ 再生中",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            // 拍数コントロール
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        if (!isPlaying) {
                                            onBeatCountChange(segmentIndex, segment.beatCount - 1)
                                        }
                                    },
                                    enabled = !isPlaying && segment.beatCount > 0,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text(
                                        "−",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isPlaying && segment.beatCount > 0)
                                            MaterialTheme.colorScheme.onSurface
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                }
                                Text(
                                    text = if (isSkipped) "スキップ" else "${segment.beatCount}拍",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSkipped)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.width(52.dp),
                                    textAlign = TextAlign.Center
                                )
                                IconButton(
                                    onClick = {
                                        if (!isPlaying) {
                                            onBeatCountChange(segmentIndex, segment.beatCount + 1)
                                        }
                                    },
                                    enabled = !isPlaying,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // オクターブコントロール
                        if (!isSkipped) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "Oct",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        if (!isPlaying) onOctaveChange(segmentIndex, -1)
                                    },
                                    enabled = !isPlaying && segment.octaveShift > -2,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Text(
                                        "−",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isPlaying && segment.octaveShift > -2)
                                            MaterialTheme.colorScheme.onSurface
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                }
                                Text(
                                    text = when {
                                        segment.octaveShift == 0 -> "標準"
                                        segment.octaveShift > 0  -> "+${segment.octaveShift}"
                                        else                     -> "${segment.octaveShift}"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(36.dp),
                                    textAlign = TextAlign.Center
                                )
                                IconButton(
                                    onClick = {
                                        if (!isPlaying) onOctaveChange(segmentIndex, +1)
                                    },
                                    enabled = !isPlaying && segment.octaveShift < 2,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Text(
                                        "+",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isPlaying && segment.octaveShift < 2)
                                            MaterialTheme.colorScheme.onSurface
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))

                        // コード候補チップ (スキップ中は操作無効)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(segment.suggestedChords) { chordIndex, chord ->
                                val isSelected = segment.selectedChordIndex == chordIndex
                                ChordChip(
                                    label = chord.displayName,
                                    isSelected = isSelected,
                                    enabled = !isPlaying && !isSkipped,
                                    onClick = {
                                        if (!isPlaying && !isSkipped) {
                                            onSelectChord(segmentIndex, chordIndex)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 再生コントロール
        Surface(
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onGoHome,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("録音し直す")
                }

                Button(
                    onClick = if (isPlaying) onStopPlayback else onPlayProgression,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (isPlaying) "停止" else "演奏する")
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// 共通コンポーネント
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun RecordButton(
    isRecording: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isRecording)
            MaterialTheme.colorScheme.error
        else
            MaterialTheme.colorScheme.primary,
        animationSpec = tween(300),
        label = "record_button_color"
    )

    Box(
        modifier = Modifier
            .size(80.dp)
            .background(backgroundColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isRecording) Icons.Default.MicOff else Icons.Default.Mic,
            contentDescription = if (isRecording) "録音停止" else "録音開始",
            modifier = Modifier.size(40.dp),
            tint = Color.White
        )
    }
}

@Composable
private fun ChordChip(
    label: String,
    isSelected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.35f
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.surface,
        animationSpec = tween(200),
        label = "chip_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.onPrimary
        else
            MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(200),
        label = "chip_text"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(backgroundColor.copy(alpha = alpha))
            .border(
                width = 1.dp,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                else
                    MaterialTheme.colorScheme.outline.copy(alpha = alpha),
                shape = RoundedCornerShape(50)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor.copy(alpha = alpha),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}

// ────────────────────────────────────────────────────────────────────────────
// 保存済みコード進行リスト画面
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun SavedListScreen(
    progressions: List<SavedProgression>,
    onLoadProgression: (SavedProgression) -> Unit,
    onDeleteProgression: (String) -> Unit,
    onGoHome: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "保存済みコード進行",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )

        if (progressions.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "保存されたコード進行がありません",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(progressions.sortedByDescending { it.savedAt }) { prog ->
                    ProgressionCard(
                        progression = prog,
                        onLoad = { onLoadProgression(prog) },
                        onDelete = { onDeleteProgression(prog.id) }
                    )
                }
            }
        }

        Surface(shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onGoHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Home, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("ホームへ戻る")
            }
        }
    }
}

@Composable
private fun ProgressionCard(
    progression: SavedProgression,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("削除の確認") },
            text = { Text("「${progression.title}」を削除しますか？") },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("削除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("キャンセル") }
            }
        )
    }

    val dateStr = remember(progression.savedAt) {
        SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
            .format(Date(progression.savedAt))
    }

    val chordPreview = remember(progression.segments) {
        progression.segments
            .filter { it.beatCount > 0 }
            .joinToString("  →  ") { it.selectedChordDisplay }
            .ifEmpty { "（コードなし）" }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = progression.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "BPM ${progression.bpm}  ${progression.timeSignatureNumerator}/${progression.timeSignatureDenominator}拍子",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "削除",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = chordPreview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onLoad,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("読み込んで編集")
            }
        }
    }
}
