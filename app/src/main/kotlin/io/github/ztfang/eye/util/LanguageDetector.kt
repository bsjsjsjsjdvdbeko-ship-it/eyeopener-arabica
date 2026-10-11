package io.github.ztfang.eye.util

import com.google.mlkit.nl.languageid.LanguageIdentification
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * كاشف اللغة باستخدام ML Kit Language Identification.
 * يعمل محلياً بدون إنترنت.
 */
object LanguageDetector {

    private val identifier = LanguageIdentification.getClient()

    /**
     * كشف لغة نص معين.
     * @return رمز اللغة (مثل "en", "fr")، أو null إذا فشل الكشف.
     */
    suspend fun detect(text: String): String? = suspendCancellableCoroutine { cont ->
        if (text.isBlank() || text.length < 3) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        identifier.identifyLanguage(text)
            .addOnSuccessListener { code ->
                if (code == "und") {
                    cont.resume(null)
                } else {
                    cont.resume(code)
                }
            }
            .addOnFailureListener {
                cont.resume(null)
            }
    }

    /**
     * كشف اللغة مع احتمالية الثقة.
     */
    suspend fun detectWithConfidence(text: String): Pair<String, Float>? = suspendCancellableCoroutine { cont ->
        if (text.isBlank() || text.length < 3) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        identifier.identifyPossibleLanguages(text)
            .addOnSuccessListener { languages ->
                val top = languages.firstOrNull()
                if (top != null && top.languageTag != "und") {
                    cont.resume(top.languageTag to top.confidence)
                } else {
                    cont.resume(null)
                }
            }
            .addOnFailureListener {
                cont.resume(null)
            }
    }
}
