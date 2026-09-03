package com.aatmn2000.aibuilder.core.ai

import com.aatmn2000.aibuilder.core.agent.DomainProfile

/**
 * Deterministic Python project templates used by [MockAiProvider].
 *
 * The generated code is stdlib-only, local-first (SQLite) and designed to
 * pass the static validator: no placeholders, 4-space indentation, balanced
 * quotes and resolvable imports.
 *
 * Note: Python docstrings use `'''` (not `"""`) on purpose — the templates
 * are Kotlin raw strings, which cannot contain a `"""` sequence.
 */
object MockPythonTemplates {

    fun mainPy(projectName: String, profile: DomainProfile): String = """
        '''Entry point for $projectName.

        Local-first CLI application: all data is stored in a local SQLite
        database, so the program runs without any backend or network access.
        '''
        import sys

        from database.storage import Storage
        from modules.${profile.key} import ${profile.service}


        def main(argv=None) -> int:
            '''Run the command line interface.'''
            args = list(sys.argv[1:] if argv is None else argv)
            storage = Storage("data/app.db")
            service = ${profile.service}(storage)
            if not args:
                service.print_help()
                return 0
            service.run_command(args[0], args[1:])
            return 0


        if __name__ == "__main__":
            raise SystemExit(main())
    """.trimIndent()

    fun modulePy(profile: DomainProfile): String = """
        '''Domain module: ${profile.description}.'''
        from dataclasses import dataclass, field
        from datetime import datetime


        @dataclass
        class ${profile.entity}:
            '''Core entity for this application.'''

            name: str
            created_at: str = field(default_factory=lambda: datetime.now().isoformat())


        class ${profile.service}:
            '''Business logic for ${profile.description}.

            Kept free of IO so it can be unit tested without a database.
            '''

            def __init__(self, storage) -> None:
                self._storage = storage

            def create(self, name: str) -> ${profile.entity}:
                '''Create a new ${profile.entityLower} and persist it.'''
                item = ${profile.entity}(name=name)
                self._storage.save(item.name, name)
                return item

            def list_all(self) -> list:
                '''Return every stored ${profile.entityLower}.'''
                return [${profile.entity}(name=row) for row in self._storage.load_all()]

            def run_command(self, command: str, args: list) -> None:
                '''Dispatch a command coming from the CLI.'''
                if command == "add" and args:
                    self.create(args[0])
                    print(f"Created: {args[0]}")
                elif command == "list":
                    for item in self.list_all():
                        print(f"- {item.name}")
                else:
                    self.print_help()

            def print_help(self) -> None:
                '''Print usage instructions.'''
                print("Usage: add <name> | list | help")
    """.trimIndent()

    fun storagePy(): String = """
        '''Local-first storage layer.

        Uses the standard library sqlite3 module, so the application runs
        without any backend, service or third-party dependency.
        '''
        import sqlite3


        class Storage:
            '''Small SQLite backed key/value store.'''

            def __init__(self, path: str) -> None:
                self._connection = sqlite3.connect(path)
                self._create_schema()

            def _create_schema(self) -> None:
                '''Create the application tables if they do not exist yet.'''
                self._connection.execute(
                    "CREATE TABLE IF NOT EXISTS items (key TEXT PRIMARY KEY, value TEXT)"
                )
                self._connection.commit()

            def save(self, key: str, value: str) -> None:
                '''Insert or replace a single value.'''
                self._connection.execute(
                    "INSERT OR REPLACE INTO items (key, value) VALUES (?, ?)",
                    (key, value),
                )
                self._connection.commit()

            def load_all(self) -> list:
                '''Return every stored value.'''
                cursor = self._connection.execute("SELECT value FROM items")
                return [row[0] for row in cursor.fetchall()]

            def close(self) -> None:
                '''Release the database connection.'''
                self._connection.close()
    """.trimIndent()

    fun testPy(profile: DomainProfile): String = """
        '''Unit tests for the ${profile.key} module.'''
        import unittest

        from modules.${profile.key} import ${profile.service}


        class FakeStorage:
            '''In-memory stand-in for the SQLite storage layer.'''

            def __init__(self) -> None:
                self._data = {}

            def save(self, key, value) -> None:
                self._data[key] = value

            def load_all(self) -> list:
                return list(self._data.values())


        class ${profile.service}Test(unittest.TestCase):
            '''Exercise the main business rules.'''

            def setUp(self) -> None:
                self.service = ${profile.service}(FakeStorage())

            def test_create_returns_entity(self) -> None:
                item = self.service.create("first")
                self.assertEqual(item.name, "first")

            def test_list_all_is_persisted(self) -> None:
                self.service.create("first")
                items = self.service.list_all()
                self.assertEqual(len(items), 1)


        if __name__ == "__main__":
            unittest.main()
    """.trimIndent()

    fun configJson(projectName: String, profile: DomainProfile): String = """
        {
          "app_name": "$projectName",
          "version": "0.1.0",
          "domain": "${profile.key}",
          "storage": {
            "backend": "sqlite",
            "path": "data/app.db"
          },
          "features": []
        }
    """.trimIndent()

    fun readme(projectName: String, profile: DomainProfile): String = """
        # $projectName

        ${profile.description}.

        ## Run

        ```
        python app/main.py help
        python app/main.py add "example"
        python app/main.py list
        ```

        ## Test

        ```
        python -m unittest discover -s tests
        ```

        ## Project layout

        - `app/` — CLI entry point
        - `modules/` — business logic (${profile.key})
        - `database/` — local SQLite storage
        - `tests/` — unit tests
        - `docs/` — architecture notes
        - `config/` — runtime configuration

        Generated with AI Software Builder — local-first, no backend required.
    """.trimIndent()

    fun architectureDoc(projectName: String, profile: DomainProfile): String = """
        # $projectName — Architecture

        ## Overview

        ${profile.description}.

        ## Modules

        | Module | Path | Purpose |
        |---|---|---|
        | ${profile.key} | modules/${profile.key}.py | Business logic |
        | storage | database/storage.py | Local SQLite storage |

        ## Data flow

        1. `app/main.py` parses the command line.
        2. The service in `modules/${profile.key}.py` applies the business rules.
        3. `database/storage.py` persists data in a local SQLite file.

        ## Design rules

        - Local-first: no network, no backend.
        - Modules are independently testable (the service is IO-free).
        - Every module is separately repairable by the AI debugger.
    """.trimIndent()

    fun overview(projectName: String, profile: DomainProfile): String = """
        # $projectName

        ${profile.description}.

        Local-first project generated by AI Software Builder.
        See README.md for usage and docs/architecture.md for the design.
    """.trimIndent()

    /**
     * Regenerates a single file for the debugging agent. The debugger only
     * ever asks for files that exist in the standard project layout.
     */
    fun fixedFile(path: String, profile: DomainProfile, projectName: String): String = when {
        path == "app/main.py" -> mainPy(projectName, profile)
        path == "database/storage.py" -> storagePy()
        path == "modules/${profile.key}.py" -> modulePy(profile)
        path == "tests/test_${profile.key}.py" -> testPy(profile)
        path == "tests" || path == "tests/" -> testPy(profile)
        path.endsWith(".py") -> "# $path\nVALUE = 0\n"
        else -> "# $path\nvalue = 0\n"
    }
}
