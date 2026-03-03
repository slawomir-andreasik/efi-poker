# EFI Poker
### Estimate. Focus. Improve.

## Goal

Lightweight Sprint Planning Poker for Scrum teams. Admin creates projects,
shares link, team estimates in rooms (async with deadline or live real-time
sessions) with hidden votes (classic poker reveal).

## Stack

Java 25 / Spring Boot 4.0 / PostgreSQL 17 / React 19 / Vite 7 / Bun / Tailwind 4
OpenAPI First (spec -> Java interfaces + TypeScript client)

## Directory Map

```
api/       -> Modular OpenAPI specs + codegen pipeline
backend/   -> Spring Boot (Spring Modulith modular monolith)
frontend/  -> React (Vite + Bun + Tailwind)
```

## Key Commands

```bash
make dev             # start dev (DB + backend + frontend)
make build           # full build (backend + frontend + tests)
make server-test     # backend tests only
make client-dev      # Vite dev server
make api-generate    # regenerate API clients from OpenAPI
make spotless        # auto-fix Java formatting
make lint            # all linters
make pre-push        # auto-fix formatting + all checks
```

## Code Style

- English for ALL code: variables, functions, comments, docstrings, log messages
- Java: Google Java Style (enforced by Spotless), no wildcard imports
- TypeScript: strict mode, no `any` types
- SQL: uppercase keywords, snake_case columns, explicit column lists

## Code Guidelines

- **API First**: OpenAPI spec is source of truth. Edit modular YAML in `api/src/main/resources/openapi/`, run `make api-generate`, then implement
- **No manual DTOs**: generated from OpenAPI
- **MapStruct** for entity-DTO mapping (compile-time, constructor injection)
- **JPA entities**: `*Entity` suffix. Domain objects: Java records without suffix
- **JPA relationships**: `@ManyToOne(fetch = FetchType.LAZY)` - never EAGER
- **Repository queries**: explicit `@Query` with `JOIN FETCH` for LAZY relations
- **spring.jpa.open-in-view=false** - always
- **Error responses**: RFC 7807 `application/problem+json`
- **Input validation**: Bean Validation `@Valid` on all request DTOs
- **UUIDs** for all entity IDs
- **Parameterized queries** ONLY (Spring Data JPA)

## Testing

- Backend: `./gradlew check` (tests + Spotless + JaCoCo 50%)
- Frontend: `bun run lint && bun run test`
- Test base classes: `BaseUnitTest`, `BaseArchUnitTest`, `BaseComponentTest`, `BaseModuleTest`
- All test classes MUST extend a Base*Test class (enforced by ArchUnit)
