# Roadmap

The roadmap follows the product plan. Each version must work **reliably**
before the next starts. The engineering principle that must hold from day
one: *AI-generated projects must be modular, portable, testable, versioned
and independently repairable.*

## V1 — AI Python Builder (current)

* Android app (Kotlin + Jetpack Compose).
* User describes a program in natural language (text, later voice).
* Agent swarm generates a **Python** project (stdlib only, local-first).
* Static compile/test validation + bounded AI error-correction loop.
* Package as `.zip` → share via Android share sheet.
* Import `.zip` → validate → AI analyzes → modify with natural language.
* AI gateway: mock provider (offline, deterministic) + Ollama + any
  OpenAI-compatible API.

Exit criteria: create → build → debug → package → share → import → modify
works end-to-end on a real device, offline (mock) and with a real model.

## V2 — Multi-language Builder

* JavaScript/TypeScript, Java, C++ and Kotlin agents behind the same
  `Agent` interface.
* Real execution of generated code in a sandbox (compile/test for real,
  replacing the static validator).
* Per-language validators and debuggers.
* Dependency scanning with real vulnerability data.

## V3 — Visual Software Builder

* AI generates complete UI (not just CLI), database schema and application
  architecture from the requirement.
* In-app preview of the generated UI before packaging.
* Design token / theme generation.

## V4 — Project Continuation

* Deep import understanding: the AI reads `project.json` + all modules and
  answers questions about an imported project ("how does the storage work?").
* Targeted modifications ("add a billing module") that extend modules,
  schema and tests instead of regenerating files.
* Diff view of every AI modification with accept/reject.

## V5 — Application Builder

* Generate runnable desktop/mobile applications rather than just source
  code (packaged binaries inside the ZIP where the platform allows).

## V6 — Natural-language computer assistant

* "Organize these files." / "Create a report from this Excel file." /
  "Build an application for my clinic."
* The AI converts these instructions into actual software/actions on the
  user's device, using the same build + package + share machinery.

## Long-term positioning

Not "English → Python translator" (too narrow, easily commoditized), but:
**"Describe the software you need. AI builds it, tests it, fixes it,
packages it, and lets you share and continue developing it."**

Technically: an AI software-development environment + project packaging
system + natural-language interface for non-programmers.
