# AI Software Builder

> **Describe the software you need. AI builds it, tests it, fixes it, packages
> it, and lets you share and continue developing it.**

AI Software Builder is an Android application for non-technical users. You
describe a program in ordinary language — text or voice — and the app converts
the description into a complete, modular software project: code, tests,
database design, documentation and a version history. The finished project is
packaged as a portable `.zip` that can be shared with another person, imported
on another device, and continued with AI ("add a billing module").

The generated projects are **local-first and self-contained**: they run without
this application's backend, and usually without any backend at all. The AI is
the *development assistant*, not a runtime dependency of the generated
software.

```
User
  │  “Create a small clinic appointment system.”  (text or voice)
  ▼
Requirement Understanding ──► AI Software Planner
  ▼
UI + Database + Architecture Design
  ▼
Code Generation (specialized agents)
  ▼
Automatic Testing ──► Error? ──► AI Error Correction (bounded loop)
  ▼
Build / Package ──► MyClinicApp.zip
  ▼
Share / Save / Import ──► AI continues development on the same project
```

## Repository layout

| Path | Description |
|---|---|
| `app/` | The Android application (Kotlin + Jetpack Compose). The control center: chat with the AI, watch the build, browse code, share/import ZIPs. |
| `core/` | Pure Kotlin (JVM) library with all product logic: project model, `project.json` codec, ZIP packaging/import, the AI gateway, the agent swarm and the build pipeline. Fully unit-testable, no Android dependency. |
| `docs/` | Architecture and roadmap documentation. |

## How it works

### The agent swarm

One giant prompt is not used. An **AI Orchestrator** decides which specialized
agents a task needs and hands each one a focused job:

```
AI Orchestrator
├── Requirement Agent      understands what the user actually wants
├── Architecture Agent     designs modules, data flow, storage
├── UI Agent               presentation layer (CLI/terminal UI in V1)
├── Database Agent         storage layer (local SQLite in V1)
├── Python Agent           business-logic modules (V1 language)
├── JavaScript Agent       (V2)
├── Java Agent             (V2)
├── C++ Agent              (V2)
├── Kotlin Agent           (V2)
├── Testing Agent          writes the test suite
├── Security Agent         scans for secrets, dangerous deps, network use
├── Debugging Agent        fixes errors, touching only affected modules
└── Documentation Agent    writes README + docs
```

### The build pipeline with automatic debugging

```
Generate ─► Compile ─► Test ─► Error?
                              ├── No  → Verify (security) → Package ZIP
                              └── Yes → Diagnose
                                         → Modify ONLY affected module
                                         → Test again
                                         → Repeat (max attempts, default 3)
```

The AI never rewrites the whole application for a small error. Every
automatic repair is recorded in the project's version history.

### The portable project format

Every generated project is modular and self-describing:

```
MyClinicApp.zip
├── app/                 entry point / presentation
├── modules/             business logic, one module per concern
├── database/            local storage layer
├── assets/              (optional)
├── tests/               test suite
├── docs/                architecture + usage documentation
├── project.json         machine-readable project description
└── README.md            human-readable documentation
```

`project.json` describes name, version, language, dependencies, modules,
entry point, platform and build requirements — which allows the AI to
understand an *imported* project and modify it instead of rebuilding it.

### The AI gateway

The app is not locked to one AI vendor. All agents talk to an **AI gateway**
that can route to multiple providers:

* **Mock provider** — deterministic, offline, always available (default).
  Ideal for development, demos and unit tests.
* **Ollama** — local AI on the user's own machine / LAN.
* **Any OpenAI-compatible API** — Provider A/B/C as the technology improves.

The active provider is chosen in Settings; API keys are stored with Android
encrypted storage and are **never** written into generated projects.

## Security

Security is a day-one requirement:

* API keys never appear in generated code (scanned on every build).
* Imported ZIPs are validated before use: entry-name/path-traversal checks,
  size limits (zip-bomb protection), manifest validation.
* Imported code is **never executed automatically** — it is only parsed and
  displayed. Execution always happens with explicit user consent, and
  generated projects are sandboxed by design (local files, local database,
  no network).
* Dependencies and build commands are scanned for network access.
* Every project keeps an explicit version/edit history.

## MVP scope (V1 — AI Python Builder)

* Describe a program in text or voice (English first).
* AI generates a **Python** project (stdlib-only, runs anywhere Python runs).
* Static compile + test validation with a bounded AI repair loop.
* Package as `.zip`, share via Android share sheet.
* Import a `.zip`, AI analyzes it, modify it with natural language.
* Works fully offline with the mock provider; real AI via Ollama or any
  OpenAI-compatible API.

Later languages (JavaScript/TypeScript, Java, C++, Kotlin) plug in behind the
same agent interface — see `docs/roadmap.md`.

## Building

Requirements: Android Studio (Ladybug or newer) or JDK 17 + Android SDK 35.

```bash
# run unit tests (core module, pure JVM)
./gradlew :core:test

# build the debug APK
./gradlew :app:assembleDebug
```

Open the project in Android Studio and press **Run**, or install the built APK
on a device with Android 8.0 (API 26) or newer.

## Roadmap

* **V1** — AI Python Builder: natural language → Python project → ZIP.
* **V2** — Multi-language builder: Python + JavaScript + Java + C++.
* **V3** — Visual software builder: full UI + database + app architecture.
* **V4** — Project continuation: import ZIP → AI understands it → modify it.
* **V5** — Application builder: generate runnable desktop/mobile apps.
* **V6** — Natural-language computer assistant.

Details in [`docs/roadmap.md`](docs/roadmap.md). The long-term vision is not
"English → Python translator" but: an AI software-development environment +
project packaging system + natural-language interface for non-programmers.

The engineering principle that must hold from day one:
**AI-generated projects must be modular, portable, testable, versioned and
independently repairable.**

## License

MIT — see [LICENSE](LICENSE).
