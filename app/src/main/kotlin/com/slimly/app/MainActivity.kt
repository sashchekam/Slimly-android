package com.slimly.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.slimly.core.deeplink.DeeplinkUrl

/**
 * Единственная Activity приложения: стек экранов держит роутер, а не система.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Nocturne — тёмная система без светлого варианта, поэтому значки
        // системных полос фиксируем светлыми: иначе на светлой теме телефона
        // они приедут тёмными поверх тёмного фона.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        // Пересозданная Activity получает тот же Intent, что и при запуске:
        // обработать его повторно — значит сбросить навигацию после поворота.
        if (savedInstanceState == null) {
            receive(intent)
        }

        setContent {
            SlimlyApp(container = (application as SlimlyApplication).container)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        receive(intent)
    }

    private fun receive(intent: Intent) {
        val url = intent.data ?: return
        DeeplinkUrl.of(url)?.let((application as SlimlyApplication).container::receive)
    }
}
