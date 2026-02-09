// PERCORSO: C:/Users/erick/AndroidStudioProjects/Esame/app/src/main/java/com/example/esame/MainActivity.kt

package com.example.esame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.esame.navigation.AppNavigation
import com.example.esame.ui.theme.EsameTheme
import dagger.hilt.android.AndroidEntryPoint // <-- 1. ASSICURATI CHE QUESTO IMPORT SIA PRESENTE

// --- 2. AGGIUNGI QUESTA ANNOTAZIONE QUI ---
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EsameTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}
