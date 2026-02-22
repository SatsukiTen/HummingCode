package com.example.hummingcode

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.hummingcode.model.AppScreen
import com.example.hummingcode.ui.HummingCodeTheme
import com.example.hummingcode.ui.MainScreen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private var onPermissionResult: ((Boolean) -> Unit)? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onPermissionResult?.invoke(isGranted)
        onPermissionResult = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HummingCodeTheme {
                val uiState by viewModel.uiState.collectAsState()
                var showPermissionDeniedDialog by remember { mutableStateOf(false) }

                if (showPermissionDeniedDialog) {
                    AlertDialog(
                        onDismissRequest = { showPermissionDeniedDialog = false },
                        title = { Text("マイクのアクセス許可が必要です") },
                        text = { Text("このアプリはマイクを使って鼻歌を録音します。設定からマイクのアクセスを許可してください。") },
                        confirmButton = {
                            Button(onClick = { showPermissionDeniedDialog = false }) {
                                Text("OK")
                            }
                        }
                    )
                }

                MainScreen(
                    uiState = uiState,
                    onStartRecording = {
                        requestMicPermission(
                            onGranted = { viewModel.startRecording() },
                            onDenied = { showPermissionDeniedDialog = true }
                        )
                    },
                    onStopRecording = { viewModel.stopRecording() },
                    onSelectChord = { segmentIndex, chordIndex ->
                        viewModel.selectChord(segmentIndex, chordIndex)
                    },
                    onPlayProgression = {
                        // Playing画面に遷移
                        viewModel.playProgression()
                    },
                    onStopPlayback = { viewModel.stopPlayback() },
                    onGoHome = { viewModel.goHome() },
                    onBpmChange = { viewModel.setBpm(it) },
                    onTimeSignatureChange = { viewModel.setTimeSignature(it) },
                    onTapTempo = { viewModel.tapTempo() }
                )
            }
        }
    }

    private fun requestMicPermission(
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                onGranted()
            }
            else -> {
                onPermissionResult = { isGranted ->
                    if (isGranted) onGranted() else onDenied()
                }
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
}
