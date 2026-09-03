package com.aatmn2000.aibuilder.ui.nav

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aatmn2000.aibuilder.AiBuilderApp
import com.aatmn2000.aibuilder.ui.builder.BuilderScreen
import com.aatmn2000.aibuilder.ui.builder.BuilderViewModel
import com.aatmn2000.aibuilder.ui.containerFactory
import com.aatmn2000.aibuilder.ui.home.HomeScreen
import com.aatmn2000.aibuilder.ui.home.HomeViewModel
import com.aatmn2000.aibuilder.ui.project.ProjectScreen
import com.aatmn2000.aibuilder.ui.project.ProjectViewModel
import com.aatmn2000.aibuilder.ui.settings.SettingsScreen
import com.aatmn2000.aibuilder.ui.settings.SettingsViewModel

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

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val context: Context = LocalContext.current
    val container = (context.applicationContext as AiBuilderApp).container
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screens.HOME,
        modifier = modifier
    ) {
        composable(Screens.HOME) {
            HomeScreen(
                viewModel = viewModel(
                    containerFactory(container) {
                        HomeViewModel(it, context.applicationContext)
                    }
                ),
                onNewProject = { navController.navigate(Screens.builder(null)) },
                onModifyProject = { navController.navigate(Screens.builder(it)) },
                onOpenProject = { navController.navigate(Screens.project(it)) },
                onOpenSettings = { navController.navigate(Screens.SETTINGS) }
            )
        }
        composable(Screens.BUILDER) {
            BuilderScreen(
                viewModel = viewModel(
                    containerFactory(container) {
                        BuilderViewModel(it, context.applicationContext, null)
                    }
                ),
                onNavigateHome = { navController.popBackStack() },
                onOpenProject = { navController.navigate(Screens.project(it)) }
            )
        }
        composable(
            route = Screens.BUILDER_WITH_PROJECT,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { entry ->
            val projectId = entry.arguments?.getString("projectId").orEmpty()
            BuilderScreen(
                viewModel = viewModel(
                    containerFactory(container) {
                        BuilderViewModel(it, context.applicationContext, projectId)
                    }
                ),
                onNavigateHome = { navController.popBackStack() },
                onOpenProject = { navController.navigate(Screens.project(it)) }
            )
        }
        composable(
            route = Screens.PROJECT,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { entry ->
            val projectId = entry.arguments?.getString("projectId").orEmpty()
            ProjectScreen(
                viewModel = viewModel(
                    containerFactory(container) {
                        ProjectViewModel(it, context.applicationContext, projectId)
                    }
                ),
                onBack = { navController.popBackStack() },
                onModify = { navController.navigate(Screens.builder(it)) }
            )
        }
        composable(Screens.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel(containerFactory(container) { SettingsViewModel(it) }),
                onBack = { navController.popBackStack() }
            )
        }
    }
}
