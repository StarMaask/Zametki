package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

class NoteSpeechManager(context: Context) {

    var isSpeaking by mutableStateOf(false)
        private set

    var speechRate by mutableFloatStateOf(1.0f)
        private set

    var currentSpeakingTitle by mutableStateOf("")
        private set

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingText: String? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                val langResult = tts?.setLanguage(Locale("ru"))
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.getDefault())
                }
                pendingText?.let { text ->
                    pendingText = null
                    speak(text, currentSpeakingTitle)
                }
            }
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
            }

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                isSpeaking = false
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                isSpeaking = false
            }
        })
    }

    fun speak(text: String, title: String = "") {
        val cleanText = cleanMarkdownForSpeech(text)
        if (cleanText.isBlank()) return

        currentSpeakingTitle = title
        if (!isInitialized) {
            pendingText = cleanText
            return
        }

        tts?.setSpeechRate(speechRate)
        val fullSpeechText = if (title.isNotBlank()) "$title. $cleanText" else cleanText
        tts?.speak(fullSpeechText, TextToSpeech.QUEUE_FLUSH, null, "NOTE_READ_ALOUD_${System.currentTimeMillis()}")
        isSpeaking = true
    }

    fun stop() {
        tts?.stop()
        isSpeaking = false
        currentSpeakingTitle = ""
    }

    fun cycleSpeechRate(): Float {
        val nextRate = when (speechRate) {
            1.0f -> 1.25f
            1.25f -> 1.5f
            1.5f -> 2.0f
            2.0f -> 0.75f
            else -> 1.0f
        }
        speechRate = nextRate
        tts?.setSpeechRate(nextRate)
        return nextRate
    }

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isSpeaking = false
    }

    private fun cleanMarkdownForSpeech(markdown: String): String {
        return markdown
            .replace(Regex("\\[color=#[0-9a-fA-F]{6}\\]"), "")
            .replace("[/color]", "")
            .replace(Regex("<[^>]*>"), "")
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("\\*(.*?)\\*"), "$1")
            .replace(Regex("~~(.*?)~~"), "$1")
            .replace(Regex("==(.*?)=="), "$1")
            .replace(Regex("__(.*?)__"), "$1")
            .replace(Regex("^#+\\s*", RegexOption.MULTILINE), "")
            .replace(Regex("^[•\\-*+]\\s*", RegexOption.MULTILINE), "")
            .replace(Regex("^>\\s*", RegexOption.MULTILINE), "")
            .trim()
    }
}
