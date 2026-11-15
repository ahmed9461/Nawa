package com.ahmed9461.nawa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.ahmed9461.nawa.ui.NawaApp
import com.ahmed9461.nawa.ui.theme.NawaTheme

class MainActivity : ComponentActivity() {
    private val model: NawaViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { NawaTheme { NawaApp(model) } }
    }
    override fun onStop() {
        if (!isChangingConfigurations) model.onBackground()
        super.onStop()
    }
}
