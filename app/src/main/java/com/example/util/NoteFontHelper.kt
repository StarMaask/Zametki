package com.example.util

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.R
import com.example.domain.model.NoteFontFamily
import java.io.File

object NoteFontHelper {

    fun getFontFamily(context: Context, format: NoteFontFamily, customFontPath: String? = null): FontFamily {
        return when (format) {
            NoteFontFamily.DEFAULT -> FontFamily.Default
            NoteFontFamily.HANDWRITING_CAVEAT -> try {
                FontFamily(
                    Font(R.font.caveat, FontWeight.Normal),
                    Font(R.font.caveat, FontWeight.Bold),
                    Font(R.font.caveat, FontWeight.SemiBold)
                )
            } catch (_: Exception) {
                FontFamily.Cursive
            }
            NoteFontFamily.HANDWRITING_MARCK -> try {
                FontFamily(
                    Font(R.font.marck_script, FontWeight.Normal),
                    Font(R.font.marck_script, FontWeight.Bold)
                )
            } catch (_: Exception) {
                FontFamily.Cursive
            }
            NoteFontFamily.SERIF_PLAYFAIR -> try {
                FontFamily(
                    Font(R.font.playfair_display, FontWeight.Normal),
                    Font(R.font.playfair_display, FontWeight.Bold)
                )
            } catch (_: Exception) {
                FontFamily.Serif
            }
            NoteFontFamily.MONOSPACE -> try {
                FontFamily(
                    Font(R.font.roboto_mono, FontWeight.Normal),
                    Font(R.font.roboto_mono, FontWeight.Bold)
                )
            } catch (_: Exception) {
                FontFamily.Monospace
            }
            NoteFontFamily.CUSTOM_DIGITIZED -> {
                val customFile = customFontPath?.let { File(it) }
                    ?: File(context.filesDir, "custom_fonts/active_font.ttf")
                if (customFile.exists() && customFile.length() > 0) {
                    try {
                        val typeface = Typeface.createFromFile(customFile)
                        FontFamily(typeface)
                    } catch (_: Exception) {
                        getFontFamily(context, NoteFontFamily.HANDWRITING_CAVEAT)
                    }
                } else {
                    getFontFamily(context, NoteFontFamily.HANDWRITING_CAVEAT)
                }
            }
        }
    }

    fun getTypeface(context: Context, format: NoteFontFamily, customFontPath: String? = null): Typeface {
        return when (format) {
            NoteFontFamily.DEFAULT -> Typeface.DEFAULT
            NoteFontFamily.HANDWRITING_CAVEAT -> try {
                androidx.core.content.res.ResourcesCompat.getFont(context, R.font.caveat) ?: Typeface.SANS_SERIF
            } catch (_: Exception) {
                Typeface.SANS_SERIF
            }
            NoteFontFamily.HANDWRITING_MARCK -> try {
                androidx.core.content.res.ResourcesCompat.getFont(context, R.font.marck_script) ?: Typeface.SERIF
            } catch (_: Exception) {
                Typeface.SERIF
            }
            NoteFontFamily.SERIF_PLAYFAIR -> try {
                androidx.core.content.res.ResourcesCompat.getFont(context, R.font.playfair_display) ?: Typeface.SERIF
            } catch (_: Exception) {
                Typeface.SERIF
            }
            NoteFontFamily.MONOSPACE -> try {
                androidx.core.content.res.ResourcesCompat.getFont(context, R.font.roboto_mono) ?: Typeface.MONOSPACE
            } catch (_: Exception) {
                Typeface.MONOSPACE
            }
            NoteFontFamily.CUSTOM_DIGITIZED -> {
                val customFile = customFontPath?.let { File(it) }
                    ?: File(context.filesDir, "custom_fonts/active_font.ttf")
                if (customFile.exists() && customFile.length() > 0) {
                    try {
                        Typeface.createFromFile(customFile)
                    } catch (_: Exception) {
                        getTypeface(context, NoteFontFamily.HANDWRITING_CAVEAT)
                    }
                } else {
                    getTypeface(context, NoteFontFamily.HANDWRITING_CAVEAT)
                }
            }
        }
    }
}
