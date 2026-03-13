package com.example.madprojectactivity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.madprojectactivity.ui.theme.MADProjectActivityTheme
import com.example.madprojectactivity.navigation.AppNavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MADProjectActivityTheme {
                AppNavGraph()
            }
        }
    }
}