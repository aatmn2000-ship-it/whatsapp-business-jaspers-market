package com.aatmn2000.aibuilder

import android.content.Context
import com.aatmn2000.aibuilder.core.agent.DebuggingAgent
import com.aatmn2000.aibuilder.core.agent.Orchestrator
import com.aatmn2000.aibuilder.core.ai.AiGateway
import com.aatmn2000.aibuilder.core.pipeline.BuildPipeline
import com.aatmn2000.aibuilder.core.pipeline.PythonStaticValidator
import com.aatmn2000.aibuilder.data.AiFactory
import com.aatmn2000.aibuilder.data.OkHttpTransport
import com.aatmn2000.aibuilder.data.ProjectRepository
import com.aatmn2000.aibuilder.data.SettingsRepository

/**
 * Poor-man's dependency injection. Created once in [AiBuilderApp.onCreate]
 * and handed to ViewModels through the application instance.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val transport = OkHttpTransport()

    val settingsRepository: SettingsRepository = SettingsRepository(appContext)
    val projectRepository: ProjectRepository = ProjectRepository(appContext)

    /**
     * Rebuilt on every build so provider changes in Settings take effect
     * immediately (and a dead provider can never cache a dead gateway).
     */
    fun buildGateway(): AiGateway = AiFactory.buildGateway(settingsRepository.load(), transport)

    fun orchestrator(gateway: AiGateway): Orchestrator = Orchestrator.createDefault(gateway)

    fun buildPipeline(gateway: AiGateway, maxDebugAttempts: Int): BuildPipeline =
        BuildPipeline(
            validator = PythonStaticValidator,
            debugger = DebuggingAgent(gateway),
            maxDebugAttempts = maxDebugAttempts
        )
}
