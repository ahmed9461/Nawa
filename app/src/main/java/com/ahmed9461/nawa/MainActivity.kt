package com.ahmed9461.nawa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ahmed9461.nawa.ui.NawaApp
import com.ahmed9461.nawa.ui.theme.NawaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NawaTheme {
                val viewModel: NawaViewModel = viewModel()
                NawaApp(viewModel)
            }
        }
    }
}
