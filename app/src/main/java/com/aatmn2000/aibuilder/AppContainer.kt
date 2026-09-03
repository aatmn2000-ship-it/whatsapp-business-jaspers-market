package com.aatmn2000.aibuilder

import android.content.Context
import com.aatmn2000.aibuilder.data.ProjectRepository
import com.aatmn2000.aibuilder.data.SettingsRepository

/**
 * Poor-man's dependency injection. Created once in [AiBuilderApp.onCreate]
 * and handed to ViewModels through the application instance.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val settingsRepository: SettingsRepository = SettingsRepository(appContext)
    val projectRepository: ProjectRepository = ProjectRepository(appContext)
}
