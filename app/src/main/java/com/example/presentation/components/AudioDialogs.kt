package com.example.presentation.components

import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun AudioRecordDialog(
    onDismiss: () -> Unit,
    onRecordingFinished: (String) -> Unit
) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var outputFile by remember { mutableStateOf<File?>(null) }

    AlertDialog(
        onDismissRequest = {
            if (isRecording) {
                try { mediaRecorder?.stop() } catch (_: Exception) {}
                mediaRecorder?.release()
            }
            onDismiss()
        },
        title = { Text("Запись голоса") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(if (isRecording) "Идёт запись..." else "Нажмите Старт для записи")
            }
        },
        confirmButton = {
            if (!isRecording) {
                Button(onClick = {
                    try {
                        val file = File(context.cacheDir, "audio_${System.currentTimeMillis()}.mp3")
                        outputFile = file
                        val recorder = MediaRecorder().apply {
                            setAudioSource(MediaRecorder.AudioSource.MIC)
                            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                            setOutputFile(file.absolutePath)
                            prepare()
                            start()
                        }
                        mediaRecorder = recorder
                        isRecording = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }) {
                    Text("Старт")
                }
            } else {
                Button(
                    onClick = {
                        try {
                            mediaRecorder?.stop()
                            mediaRecorder?.release()
                            mediaRecorder = null
                            isRecording = false
                            outputFile?.let { onRecordingFinished(it.absolutePath) }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Стоп и сохранить")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (isRecording) {
                    try { mediaRecorder?.stop() } catch (_: Exception) {}
                    mediaRecorder?.release()
                }
                onDismiss()
            }) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun AudioPlaybackCard(
    audioUri: String,
    onDelete: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(audioUri) {
        onDispose {
            player?.release()
            player = null
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (isPlaying) {
                        player?.pause()
                        isPlaying = false
                    } else {
                        try {
                            if (player == null) {
                                player = MediaPlayer().apply {
                                    setDataSource(audioUri)
                                    prepare()
                                    setOnCompletionListener {
                                        isPlaying = false
                                    }
                                }
                            }
                            player?.start()
                            isPlaying = true
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Остановить" else "Воспроизвести"
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Аудиозаметка")
            }

            TextButton(onClick = {
                player?.stop()
                player?.release()
                player = null
                onDelete()
            }) {
                Text("Удалить", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
