package com.prog7313.buglogger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import com.prog7313.buglogger.ui.navigation.AppNavGraph
import com.prog7313.buglogger.ui.theme.BugLoggerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BugLoggerTheme {
                Surface {
                    AppNavGraph()
                }
            }
        }
    }
}