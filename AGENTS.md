# AGENTS.md - Cricket HCMS Project

## Project Context
This is a **Quarkus 3.38.1** Java 21 application - **Cricket HCMS** (Headless Content Management System).

### Core Stack
- **Framework**: Quarkus with Arc (CDI), REST, WebSockets
- **Database**: H2 via JDBC (Agroal connection pool)
- **Translation**: DeepL Java API
- **Observability**: OpenTelemetry with JDBC instrumentation
- **Caching**: Quarkus Cache
- **Scheduling**: Quarkus Scheduler
- **API Docs**: SmallRye OpenAPI
- **Markdown**: Flexmark for processing

### Project Structure
```
src/main/java/pl/experiot/hcms/app/
├── logic/              # Core business logic (DocumentLogic, TranslatorLogic, etc.)
├── ports/
│   ├── driven/         # Repository/interfaces for external services
│   └── driving/        # API interfaces for consumers
├── rest/               # REST endpoints
└── websockets/         # WebSocket handlers
```

## Agent Instructions

### Architecture
- Follow **Hexagonal Architecture** principles
- Use **Repository Pattern** for data access
- Use **DTOs** for data transfer between layers

### Code Standards
- Follow existing Java 21 conventions (records where appropriate, sealed classes, etc.)
- Match Quarkus patterns: `@ApplicationScoped`, `@Inject`, CDI
- Use Lombok-style patterns if present, otherwise traditional Java beans
- All REST endpoints should have OpenAPI annotations (`@Tag`, `@Operation`, `@APIResponse`)

### Testing
- Use Quarkus JUnit 5 extensions
- REST API tests: use RestAssured
- Mock external services (DeepL) in unit tests
- Integration tests disabled by default (`skipITs=true` in pom.xml)

### Build & Execution
- Build: `mvn compile quarkus:dev`
- Test: `mvn test` (unit tests only)
- Integration test: `mvn verify -Pnative` (if needed)
- Native build: `mvn package -Pnative`
- Container build: JIB plugin configured

### Database
- H2 in-memory by default
- Configuration in `application.properties`
- Schema managed via Quarkus/JPA

### Git Workflow
- Feature branches preferred
- Main branch protected
- Version in pom.xml (currently 1.2.1, updated to 1.6.0 in recent commits)

### Special Directories
- `src/attic/`: Deprecated/archived code (reference only)
- `src/scripts/`: Helper scripts
- `src/main/docker/`: Docker configurations
- `src/main/js/`: Frontend JavaScript (if any)

### External Dependencies
- DeepL API: Translation service integration
- OpenTelemetry: Distributed tracing
- Flexmark: Markdown processing
- WebSockets: Real-time communication

## Important Patterns

### Repository Pattern
- `For*Iface` interfaces in `ports/driven/`
- Implementations typically in `logic/` with `@ApplicationScoped`
- Use `ForDocumentRepositoryIface`, `ForTranslatorIface`, etc.

### DTO Pattern
- DTO classes in `logic/dto/` (User.java, Site.java, Document.java, Entry.java, Dictionary.java)
- Keep DTOs simple, use logic classes for business operations

### Logic Layer
- `*Logic.java` classes contain business logic
- Inject repositories via constructor or field injection
- Use `@Transactional` where appropriate for database operations

### API Layer
- Follow REST conventions
- Return appropriate HTTP status codes
- Use Jackson for JSON serialization

## Constraints
- Do not modify `src/attic/` - this is archived code
- Respect existing package structure
- New REST endpoints should follow existing naming conventions
- Database changes should be compatible with H2
- Consider caching strategy for expensive operations (translation, etc.)

## Development Notes
- OpenTelemetry recently enabled (commit: Enable Quarkus OpenTelemetry and disable tests)
- Version bump to 1.6.0 recent
- AdministratorApi.reload recently implemented
- DocumentApi moved to attic and removed from active development

---
*This file provides context and constraints for AI agents working on the Cricket HCMS project.*
