package com.aatmn2000.aibuilder.ui.nav

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

object Screens {
    const val HOME = "home"
    const val BUILDER = "builder"
    const val BUILDER_WITH_PROJECT = "builder?projectId={projectId}"
    const val PROJECT = "project/{projectId}"
    const val SETTINGS = "settings"

    fun builder(projectId: String? = null): String =
        if (projectId == null) BUILDER else "builder?projectId=$projectId"

    fun project(projectId: String): String = "project/$projectId"
}

/**
 * Navigation graph. Screens are placeholders until the full UI lands.
 */
@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screens.HOME,
        modifier = modifier
    ) {
        composable(Screens.HOME) {
            Scaffold { padding ->
                PlaceholderScreen("Home", "Your generated projects will appear here.", padding)
            }
        }
        composable(Screens.BUILDER) {
            Scaffold { padding ->
                PlaceholderScreen("Builder", "Describe the software you want to build.", padding)
            }
        }
        composable(Screens.BUILDER_WITH_PROJECT) {
            Scaffold { padding ->
                PlaceholderScreen("Builder", "Modify an imported project.", padding)
            }
        }
        composable(Screens.PROJECT) {
            Scaffold { padding ->
                PlaceholderScreen("Project", "Browse the generated code.", padding)
            }
        }
        composable(Screens.SETTINGS) {
            Scaffold { padding ->
                PlaceholderScreen("Settings", "Configure the AI provider.", padding)
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(
    title: String,
    subtitle: String,
    padding: androidx.compose.foundation.layout.PaddingValues
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
