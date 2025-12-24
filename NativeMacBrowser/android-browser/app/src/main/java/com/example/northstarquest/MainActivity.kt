package com.example.northstarquest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.northstarquest.ui.BrowserScreen
import com.example.northstarquest.ui.theme.NorthStarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val startUrl = intent?.dataString ?: "https://www.google.com"

        setContent {
            NorthStarTheme {
                BrowserScreen(initialUrl = startUrl)
            }
        }
    }
}
