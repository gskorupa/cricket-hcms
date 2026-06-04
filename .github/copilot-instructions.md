Purpose
Provide concise repository-specific guidance to help Copilot-style agents and future contributors understand build/test/lint commands, the high-level architecture, and repository conventions.

Build, test, and CI commands
- Build (full): mvn -B package --file pom.xml
- Run in dev mode (hot reload, Quarkus): mvn quarkus:dev
- Run unit tests (full): mvn test
- Run a single unit test (class or method): mvn -Dtest=TestClassName test
  - Method variant: mvn -Dtest=TestClassName#methodName test
  - Use fully qualified classname if ambiguous: mvn -Dtest=pl.experiot.hcms.SomeTest test
- Run integration tests (Failsafe): mvn -DskipITs=false verify
  - Note: property skipITs defaults to true in pom.xml; set to false to run ITs
- Native/profile build: activate the "native" profile by passing -Dnative, e.g. mvn -Dnative package
- CI: GitHub Actions uses .github/workflows/maven.yml (JDK 17, mvn -B package)
- Docker / demo: docker-compose.yml and docker-compose-dev.yml mount the local `documents` folder and expose the service at http://localhost:8080 (see README.md / documents/documentation/en/development)

Linting / static analysis
- No dedicated linting/checkstyle/spotless configuration found in pom.xml. Use the compiler and CI to catch issues; add static analysis plugins if required.

High-level architecture (big picture)
- Framework: Quarkus-based Java application (Quarkus BOM in pom.xml).
- Architectural style: Hexagonal / Ports-and-Adapters pattern. Conventions:
  - adapters.driving: REST controllers / HTTP entry points (endpoints live under src/main/java/pl/experiot/hcms/adapters/driving)
  - adapters.driven: infrastructure implementations (persistence, filesystem loader, translators)
  - app.logic: core business logic / use-cases
- Document model: file-based content is loaded from a configured documents root (default: ./documents), parsed (Markdown/HTML), and stored/served via DocumentRepository implementations (in-memory / H2-backed). The filesystem loader classes are under adapters/driven/loader/fs.
- Features surfaced in code and config:
  - Markdown parsing: flexmark
  - Translation: DeepL client optional (deepl-java)
  - Websockets, caching, scheduling provided via Quarkus extensions
  - H2 is used by default in tests / local runs (quarkus datasource configured in application.properties)

Key repository conventions and important tokens
- Config patterns: application.properties uses hcms.*, document.*, and quarkus.* prefixes. Test overrides use the `%test.` profile keys.
- Document folder config: document.folders.root (default: documents). Document sites are listed in document.folders.sites. The demo and Docker compose mount the local `documents` folder into the container and set FOLDERS_ROOT.
- Hot-reload trigger: the service watches version files (document.watcher.file, defaults to version.txt); updating that file forces a reload of the document tree.
- API auth: OpenAPI is configured to expect an API key in header X-app-token (see smallrye-openapi config in application.properties).
- Integration tests gating: the property skipITs controls running of integration tests (default true). CI runs a plain package build; to include ITs explicitly set -DskipITs=false.
- Package layout: look in src/main/java/pl/experiot/hcms for adapters.driving (REST APIs), adapters.driven (repositories, loaders), and app/logic (use-cases) — useful when asking Copilot where to make changes.
- Documentation and demo: full documentation lives in documents/documentation/en. The demo website and running instructions are under that folder; the repository includes docker-compose.yml and docker-compose-dev.yml for testing locally.

Where to look for specific tasks
- REST endpoints: src/main/java/pl/experiot/hcms/adapters/driving
- Repository & DB code: src/main/java/pl/experiot/hcms/adapters/driven/repo
- Filesystem loader: src/main/java/pl/experiot/hcms/adapters/driven/loader/fs
- Tests: src/test/java and standard Maven layout

Other AI assistant / agent configs
- No CLAUDE.md, AGENTS.md, .cursorrules, .windsurfrules, or AIDER_CONVENTIONS.md were found. CI uses GitHub Actions (.github/workflows/maven.yml).

Notes for Copilot-style sessions
- When suggesting changes that touch persistence vs API, follow the adapters pattern: change adapters.driving for controllers, app.logic for business logic, adapters.driven for infra.
- Prefer modifying application.properties for configuration changes and respect `%test.` overrides when running tests.
- For single-test iteration use mvn -Dtest=... test to keep feedback fast; use quarkus:dev for iterative development.

MCP servers
- Would you like an MCP server configuration for running the service (e.g., Quarkus dev / Maven) or Docker Compose for integration testing? (Yes/No)

Summary
Created .github/copilot-instructions.md with build/test commands, high-level architecture, and repository-specific conventions. Ask if any section should include more detail or additional run/debug examples.