package com.aatmn2000.aibuilder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aatmn2000.aibuilder.ui.nav.AppNavHost
import com.aatmn2000.aibuilder.ui.theme.AiSoftwareBuilderTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AiSoftwareBuilderTheme {
                AppNavHost()
            }
        }
    }
}
