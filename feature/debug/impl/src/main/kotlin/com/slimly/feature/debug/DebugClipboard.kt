package com.slimly.feature.debug

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/**
 * Буфер обмена.
 *
 * Своим воркером, а не вызовом системы из вьюмодели: экрану нужен один
 * метод, а не весь `Context`, — и подставить вместо него запись в список
 * в тесте тогда ничего не стоит.
 */
fun interface DebugClipboard {
    fun copy(text: String)
}

class SystemClipboard(private val context: Context) : DebugClipboard {
    override fun copy(text: String) {
        val manager = context.getSystemService(ClipboardManager::class.java) ?: return
        manager.setPrimaryClip(ClipData.newPlainText(LABEL, text))
    }

    private companion object {
        /** Подпись куска в буфере — её показывает системная панель вставки. */
        const val LABEL = "Slimly"
    }
}
