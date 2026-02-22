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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hummingcode.model.AppScreen
import com.example.hummingcode.model.NoteSegment
import com.example.hummingcode.model.TimeSignature
import com.example.hummingcode.model.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: UiState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onSelectChord: (Int, Int) -> Unit,
    onPlayProgression: () -> Unit,
    onStopPlayback: () -> Unit,
    onGoHome: () -> Unit,
    onBpmChange: (Int) -> Unit,
    onTimeSignatureChange: (TimeSignature) -> Unit,
    onTapTempo: () -> Unit
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
                    onTapTempo = onTapTempo
                )
                AppScreen.Recording -> RecordingScreen(
                    currentNote = uiState.currentDetectedNote,
                    currentBeat = uiState.currentBeat,
                    beatsPerMeasure = uiState.timeSignature.numerator,
                    onStopRecording = onStopRecording
                )
                AppScreen.ChordSelection -> ChordSelectionScreen(
                    segments = uiState.segments,
                    playingIndex = uiState.playingChordIndex,
                    isPlaying = uiState.isPlaying,
                    onSelectChord = onSelectChord,
                    onPlayProgression = onPlayProgression,
                    onStopPlayback = onStopPlayback,
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
    onTapTempo: () -> Unit
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "録音中...",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(32.dp))

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

        Spacer(Modifier.height(16.dp))
        Text(
            text = if (currentNote != null) "検出: $currentNote" else "音を検出しています...",
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

        // 停止ボタン（アニメーション付き）
        Box(
            modifier = Modifier
                .scale(scale)
                .size(80.dp)
                .background(MaterialTheme.colorScheme.error, CircleShape)
                .clickable(onClick = onStopRecording),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MicOff,
                contentDescription = "録音停止",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onError
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "タップして録音停止",
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
    onPlayProgression: () -> Unit,
    onStopPlayback: () -> Unit = {},
    onGoHome: () -> Unit
) {
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

        Text(
            text = "コードを選択してください",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(segments) { segmentIndex, segment ->
                val isCurrentlyPlaying = playingIndex == segmentIndex

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrentlyPlaying)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isCurrentlyPlaying) 4.dp else 1.dp
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 音名バッジ
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = segment.noteName,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "フレーズ ${segmentIndex + 1}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            if (isCurrentlyPlaying) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "♪ 再生中",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))

                        // コード候補チップ
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(segment.suggestedChords) { chordIndex, chord ->
                                val isSelected = segment.selectedChordIndex == chordIndex
                                ChordChip(
                                    label = chord.displayName,
                                    isSelected = isSelected,
                                    onClick = {
                                        if (!isPlaying) {
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
    onClick: () -> Unit
) {
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
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}
