package com.aatmn2000.aibuilder.ui.builder

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aatmn2000.aibuilder.AppContainer
import com.aatmn2000.aibuilder.core.agent.AgentEvent
import com.aatmn2000.aibuilder.core.agent.AgentStep
import com.aatmn2000.aibuilder.core.pipeline.BuildEvent
import com.aatmn2000.aibuilder.core.pipeline.BuildResult
import com.aatmn2000.aibuilder.core.project.ZipPackager
import com.aatmn2000.aibuilder.core.security.SecurityIssue
import com.aatmn2000.aibuilder.ui.share.ShareZip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LogEntry(
    val text: String,
    val kind: Kind
) {
    enum class Kind {
        INFO,
        AGENT,
        PROGRESS,
        WARNING,
        ERROR
    }
}

data class SuccessInfo(
    val projectId: String,
    val projectName: String,
    val zipBytes: ByteArray,
    val securityNotes: Int,
    val repairAttempts: Int
)

data class BuilderUiState(
    val input: String = "",
    val projectId: String? = null,
    val projectName: String? = null,
    val isRunning: Boolean = false,
    val plan: List<AgentStep> = emptyList(),
    val log: List<LogEntry> = emptyList(),
    val error: String? = null,
    val success: SuccessInfo? = null
)

/**
 * Drives the full workflow for one task:
 *
 *   describe → orchestrator (agents) → pipeline (compile/test/debug loop/
 *   verify/package) → save + share/import.
 */
class BuilderViewModel(
    private val container: AppContainer,
    private val appContext: Context,
    projectId: String?
) : ViewModel() {

    private val _state = MutableStateFlow(
        BuilderUiState(
            projectId = projectId,
            projectName = projectId?.let { container.projectRepository.load(it)?.manifest?.name }
        )
    )
    val state: StateFlow<BuilderUiState> = _state.asStateFlow()

    fun onInputChanged(text: String) {
        _state.update { it.copy(input = text) }
    }

    /** Appends a voice-recognized sentence to the input field. */
    fun appendVoice(text: String) {
        _state.update { it.copy(input = (it.input + " " + text).trim()) }
    }

    fun reset() {
        _state.update {
            it.copy(
                input = "",
                isRunning = false,
                plan = emptyList(),
                log = emptyList(),
                error = null,
                success = null
            )
        }
    }

    fun build() {
        val request = _state.value.input.trim()
        if (request.isEmpty() || _state.value.isRunning) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isRunning = true,
                    plan = emptyList(),
                    log = emptyList(),
                    error = null,
                    success = null
                )
            }
            runCatching {
                val settings = container.settingsRepository.load()
                val gateway = container.buildGateway()
                val orchestrator = container.orchestrator(gateway)

                val baseProject = _state.value.projectId
                    ?.let { container.projectRepository.load(it) }
                log(
                    if (baseProject == null) "Planner: analyzing your request…"
                    else "Planner: modifying ${baseProject.manifest.name}…",
                    LogEntry.Kind.INFO
                )

                val project = if (baseProject == null) {
                    orchestrator.createProject(request, gateway) { event ->
                        onAgentEvent(event)
                    }
                } else {
                    orchestrator.modifyProject(baseProject, request, gateway) { event ->
                        onAgentEvent(event)
                    }
                }

                log("Pipeline: compiling and testing…", LogEntry.Kind.PROGRESS)
                val pipeline = container.buildPipeline(gateway, settings.maxDebugAttempts)
                val result = pipeline.run(project) { event -> onBuildEvent(event) }

                when (result) {
                    is BuildResult.Success -> {
                        container.projectRepository.save(result.project)
                        val zip = ZipPackager.packageProject(result.project)
                        log(
                            "Done: ${result.project.manifest.name} packaged " +
                                "(${zip.size / 1024} KB)",
                            LogEntry.Kind.INFO
                        )
                        _state.update {
                            it.copy(
                                isRunning = false,
                                success = SuccessInfo(
                                    projectId = result.project.manifest.id,
                                    projectName = result.project.manifest.name,
                                    zipBytes = zip,
                                    securityNotes = result.securityIssues.count {
                                        it.severity == SecurityIssue.Severity.WARNING
                                    },
                                    repairAttempts = result.repairAttempts
                                )
                            )
                        }
                    }
                    is BuildResult.Failure -> {
                        container.projectRepository.save(result.project)
                        _state.update {
                            it.copy(
                                isRunning = false,
                                error = "The build could not finish: " +
                                    result.issues.joinToString("; ") { "${it.file}: ${it.message}" }
                            )
                        }
                    }
                }
            }.onFailure { throwable ->
                _state.update {
                    it.copy(isRunning = false, error = throwable.message ?: "Build failed")
                }
            }
        }
    }

    fun exportZip() {
        val success = _state.value.success ?: return
        ShareZip.share(
            appContext,
            container.projectRepository,
            success.projectName,
            success.zipBytes
        )
    }

    private fun log(text: String, kind: LogEntry.Kind) {
        _state.update { it.copy(log = it.log + LogEntry(text, kind)) }
    }

    private fun onAgentEvent(event: AgentEvent) {
        when (event) {
            is AgentEvent.PlanReady -> {
                _state.update { it.copy(plan = event.plan.steps) }
                event.plan.steps.forEach { step ->
                    log("Plan: ${step.role.name} — ${step.reason}", LogEntry.Kind.INFO)
                }
            }
            is AgentEvent.AgentStarted ->
                log("▸ ${event.role.name}: ${event.reason}", LogEntry.Kind.AGENT)
            is AgentEvent.AgentFinished ->
                log(
                    "✓ ${event.role.name}: ${event.summary.take(120)}",
                    if (event.error) LogEntry.Kind.ERROR else LogEntry.Kind.PROGRESS
                )
        }
    }

    private fun onBuildEvent(event: BuildEvent) {
        when (event) {
            is BuildEvent.StageStarted -> log("Pipeline: ${event.stage}…", LogEntry.Kind.PROGRESS)
            is BuildEvent.CompileReport ->
                if (event.issues.isNotEmpty()) {
                    log("Compile: ${event.issues.size} issue(s)", LogEntry.Kind.WARNING)
                }
            is BuildEvent.TestReport ->
                if (event.issues.isNotEmpty()) {
                    log("Tests: ${event.issues.size} issue(s)", LogEntry.Kind.WARNING)
                }
            is BuildEvent.DebugAttempt ->
                log(
                    "Auto-repair (attempt ${event.attempt}): ${event.issues.first().message}",
                    LogEntry.Kind.WARNING
                )
            is BuildEvent.SecurityReport ->
                if (event.issues.isNotEmpty()) {
                    log("Security: ${event.issues.size} note(s)", LogEntry.Kind.WARNING)
                }
            is BuildEvent.Packaged ->
                log("Packaged ZIP: ${event.zipSizeBytes / 1024} KB", LogEntry.Kind.INFO)
            is BuildEvent.Failed -> log(event.reason, LogEntry.Kind.ERROR)
        }
    }
}
