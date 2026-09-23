package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

class LectureTranscriptionManager(private val context: Context) {

    var isRecording by mutableStateOf(false)
        private set

    var isPaused by mutableStateOf(false)
        private set

    var durationSeconds by mutableLongStateOf(0L)
        private set

    var partialHypothesis by mutableStateOf("")
        private set

    private var speechRecognizer: SpeechRecognizer? = null
    private var onTextAppendedCallback: ((String) -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isRecording && !isPaused) {
                durationSeconds++
            }
            if (isRecording) {
                mainHandler.postDelayed(this, 1000)
            }
        }
    }

    private val restartRunnable = Runnable {
        if (isRecording && !isPaused) {
            safeStartListening()
        }
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            // Android SpeechRecognizer stops on silence timeout (ERROR_NO_MATCH, ERROR_SPEECH_TIMEOUT, etc.)
            // In lecture mode, we automatically restart listening after a tiny pause!
            if (isRecording && !isPaused) {
                mainHandler.removeCallbacks(restartRunnable)
                mainHandler.postDelayed(restartRunnable, 250)
            }
        }

        override fun onResults(results: Bundle?) {
            partialHypothesis = ""
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognizedChunk = matches?.firstOrNull()?.trim()
            if (!recognizedChunk.isNullOrBlank()) {
                val formatted = formatRecognizedChunk(recognizedChunk)
                onTextAppendedCallback?.invoke(formatted)
            }

            if (isRecording && !isPaused) {
                mainHandler.removeCallbacks(restartRunnable)
                mainHandler.postDelayed(restartRunnable, 200)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = partialMatches?.firstOrNull()?.trim() ?: ""
            partialHypothesis = partial
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun startRecording(onTextAppended: (String) -> Unit): Boolean {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            return false
        }

        onTextAppendedCallback = onTextAppended
        durationSeconds = 0L
        partialHypothesis = ""
        isPaused = false
        isRecording = true

        mainHandler.removeCallbacks(timerRunnable)
        mainHandler.postDelayed(timerRunnable, 1000)

        initAndListen()
        return true
    }

    fun togglePause() {
        if (!isRecording) return
        isPaused = !isPaused
        if (isPaused) {
            partialHypothesis = ""
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {}
        } else {
            safeStartListening()
        }
    }

    fun stopRecording() {
        isRecording = false
        isPaused = false
        partialHypothesis = ""
        mainHandler.removeCallbacks(timerRunnable)
        mainHandler.removeCallbacks(restartRunnable)

        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }

    private fun initAndListen() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(recognitionListener)
            safeStartListening()
        } catch (_: Exception) {
            isRecording = false
        }
    }

    private fun safeStartListening() {
        if (!isRecording || isPaused) return
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
        } catch (_: Exception) {
            // If failed to start, re-create and retry
            mainHandler.postDelayed({
                if (isRecording && !isPaused) {
                    initAndListen()
                }
            }, 500)
        }
    }

    private fun formatRecognizedChunk(raw: String): String {
        if (raw.isBlank()) return ""
        val capitalized = raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val withPunctuation = if (!capitalized.endsWith(".") && !capitalized.endsWith("?") && !capitalized.endsWith("!")) {
            "$capitalized."
        } else {
            capitalized
        }
        return withPunctuation
    }

    fun formattedDuration(): String {
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    fun release() {
        stopRecording()
    }
}
