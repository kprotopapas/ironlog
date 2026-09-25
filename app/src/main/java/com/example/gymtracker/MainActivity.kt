package com.example.gymtracker

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.gymtracker.ui.GymApp
import com.example.gymtracker.ui.theme.GymTheme

class MainActivity : ComponentActivity() {
    private val vm: GymViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // The app is always light, so force dark system-bar icons regardless of the system theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            GymTheme {
                GymApp(vm)
            }
        }
    }
}
