package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.PedalboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.GuitarFxViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: GuitarFxViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PedalboardScreen(viewModel = viewModel)
            }
        }

        // Show AdMob App Open Ad if available
        (application as? GuitarFxApplication)?.appOpenAdManager?.showAdIfAvailable(this)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshAudioDevices()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopEngine()
    }
}
