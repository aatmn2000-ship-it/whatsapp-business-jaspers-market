# Architecture

## Design goals

1. **No-backend principle.** Generated applications are local-first and
   self-contained (local database, local files, local processing). If a user
   specifically needs cloud functionality, the AI generates a backend *as part
   of that particular project* — "no backend" is a default, not a restriction.
2. **Modularity.** Every generated project splits into `app/`, `modules/`,
   `database/`, `tests/`, `docs/`, `config/` with a machine-readable
   `project.json`. This is what makes projects portable, inspectable and
   repairable.
3. **Repairability.** Errors are localized: the debugging agent may modify
   only the affected module, never the whole application, and repair attempts
   are bounded (default 3).
4. **Provider independence.** All AI access goes through one gateway with a
   pluggable provider list (mock, local, remote). Models can be swapped as
   technology improves.
5. **Safe by construction.** Imported ZIPs are validated, scanned and never
   auto-executed; generated code is scanned for secrets before packaging.

## Modules

```
core  (Kotlin/JVM, no Android dependency)
├── ai/         AI gateway + providers (mock, Ollama, OpenAI-compatible)
├── agent/      Agent interface, the 12 specialized agents, Orchestrator
├── project/    ProjectManifest (project.json), codec, ZIP packager/importer
├── pipeline/   Build pipeline: compile → test → debug loop → verify → package
└── security/   Path traversal guard, secret scanner, dependency scanner,
                sandbox policy

app  (Android, Kotlin + Jetpack Compose)
├── data/       ProjectRepository (file storage), SettingsRepository
│               (encrypted keys), OkHttp transport for the AI gateway
├── ui/         Compose screens: home, builder chat, project viewer, settings
└── share/      ZIP export via FileProvider + Android share sheet
```

## The agent swarm

Each agent is an object implementing `Agent`:

```kotlin
interface Agent {
    val role: AgentRole
    fun run(context: AgentContext): AgentResult
}
```

* `AgentContext` carries the user request, the existing project (if this is a
  modification), the domain profile (entity/service names) and the project
  name.
* `AgentResult` carries a human-readable summary plus the artifacts (files)
  the agent produced.
* Agents are stateless and only talk to the AI through the gateway; the same
  agent works with any provider. The mock provider makes the whole swarm
  deterministic, which is what unit tests rely on.

### Orchestrator planning rules (V1)

* **New project:** Requirement → Architecture → Database → UI → Python →
  Testing → Security → Documentation.
* **Modification of an imported project:** Requirement → Python (affected
  modules) → Testing → Documentation.

The plan is returned as `AgentPlan(steps: List<AgentStep(role, reason)>)` so
the UI can show *why* each agent runs. V2 adds language-specific branches
(JavaScript/Java/C++/Kotlin agents) selected by the requirement agent.

## Build pipeline

```kotlin
BuildPipeline(validator, debugger, scanner, maxDebugAttempts)
    .run(project, onEvent) : BuildResult
```

Stages:

1. **compile** — `PythonStaticValidator.compile(project)`: placeholder
   detection, indentation rules, triple-quote balance, JSON validity, and
   cross-file import resolution (`from modules.x import y` must resolve to a
   real file and a real `def y`/`class y`).
2. **test** — `PythonStaticValidator.runTests(project)`: every test file must
   contain real `test_` functions and must reference names that exist in the
   project. (V2 replaces this with real execution in a sandbox.)
3. **debug loop** — while issues exist and attempts < max: the Debugging
   agent receives the issues and returns a *patched project* in which only
   the affected files changed. Each repair is appended to the project
   history (`EditRecord`).
4. **verify** — security scan: secrets in code, suspicious dependencies,
   network access in build commands, sandbox policy check.
5. **package** — `ZipPackager` produces the ZIP (files + `project.json`).

`BuildResult.Success` / `BuildResult.Failed` carry the final project, the
issue lists and the number of repair attempts. Progress is streamed as
`BuildEvent`s so the UI can render live status.

## `project.json` (schema v1)

| Field | Meaning |
|---|---|
| `schemaVersion` | Manifest schema version (1). |
| `id` | Stable UUID identifying the project across imports. |
| `name` / `description` | Human-readable identity. |
| `version` | Semver of the project itself. |
| `language` | `python` in V1. |
| `domain` | Application domain key (e.g. `clinic_appointments`) that pins the entity/service names used by the agents — survives import so modifications target the right modules. |
| `platform` | Target platforms, e.g. `["local"]`, `["android"]`. |
| `entryPoint` | File to run, e.g. `app/main.py`. |
| `modules[]` | `name`, `path`, `purpose` per module. |
| `dependencies[]` | `name`, `version`, `scope` (runtime/test). |
| `build` | `requirements[]` + `commands.run` / `commands.test`. |
| `storage` | Optional: `kind` (`sqlite`), `path`. |
| `createdAt` / `updatedAt` | ISO-8601 timestamps. |

The manifest is the contract between the importer, the AI and the user: an
imported ZIP without a valid `project.json` is rejected.

## ZIP import pipeline (safety)

```
ZIP bytes
  → entry walk with budgets (entry count, per-entry size, total size)
  → PathTraversalGuard (no absolute paths, no "..", no drive letters)
  → project.json present? → ManifestCodec.decode + validate
  → entry point file exists?
  → SecretScanner + DependencyScanner (warnings)
  → GeneratedProject (code is NEVER executed here)
```

## AI gateway

```kotlin
class AiGateway(providers: List<AiProvider>, preferredId: String)
```

* `AiProvider` = `id`, `displayName`, `isAvailable()`, `complete(AiRequest)`.
* `AiRequest` = system prompt, user prompt, `role` (which agent is asking)
  and generation parameters. Agents append machine-readable tokens to the
  user prompt (`path: …`, `domain: …`, `project_name: …`) so any provider —
  including the deterministic mock — can answer file generation requests.
* The gateway tries the preferred provider first, then falls back to the
  remaining available providers.
* `AiTransport` is the HTTP seam: the Android app provides an OkHttp
  implementation; unit tests use a fake. The core module itself has no
  network dependency.

## Storage on the device

* Projects live under `filesDir/projects/<id>/` mirroring the ZIP layout,
  plus `project.json` and `history.json`.
* AI provider settings live in plain `SharedPreferences`; **API keys live in
  `EncryptedSharedPreferences`** and are never persisted anywhere else.
* Export writes the ZIP to `cacheDir/shared/` and hands a `FileProvider`
  URI to the Android share sheet. Import uses the Storage Access Framework
  (`ACTION_OPEN_DOCUMENT`) — no broad storage permissions.
