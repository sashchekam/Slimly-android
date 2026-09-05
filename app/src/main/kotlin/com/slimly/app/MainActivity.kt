package com.slimly.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.slimly.feature.home.HomeScreen

/**
 * Composition root приложения. На Android это одна Activity на всё
 * приложение: стек экранов держит роутер, а не система.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            HomeScreen()
        }
    }
}
