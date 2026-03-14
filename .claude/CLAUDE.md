# EFI Poker
### Estimate. Focus. Improve.

## Goal

Lightweight Sprint Planning Poker for Scrum teams. Admin creates projects,
shares link, team estimates in rooms (async or live) with hidden votes and
classic poker reveal.

## Stack

Java 25 / Spring Boot 4.0 / PostgreSQL 17 / React 19 / Vite 7 / Bun / Tailwind 4

## Architecture

OpenAPI spec (modular YAML) -> generated Java interfaces + DTOs -> Spring Boot
controllers -> services (return domain records) -> JPA entities -> PostgreSQL.
Frontend calls API via hand-maintained TypeScript types (`src/api/types.ts`).
MapStruct bridges entities <-> domain records. Liquibase manages schema migrations.

## Directory Map

```
api/       -> Modular OpenAPI specs + codegen pipeline
  src/main/resources/openapi/  -> Source YAML (edit here)
backend/   -> Spring Boot (Spring Modulith modular monolith)
frontend/  -> React (Vite + Bun + Tailwind)
```

## Key Commands

```bash
make dev             # start dev (DB + backend + frontend)
make build           # full build (backend + frontend + tests)
make server-test     # backend tests only
make client-dev      # Vite dev server
make api-generate    # regenerate from OpenAPI (run after spec changes)
make spotless        # auto-fix Java formatting (run before commit)
make lint            # all linters
make pre-push        # formatting + all checks
make db-reset        # drop + recreate database
```

## Code Style

Commits: `type: summary` (max 72 chars). Types: feat, fix, chore, docs, refactor, test

## Conventions (loaded automatically per file type)

- [rules/api-contract.md](rules/api-contract.md) - OpenAPI modular workflow, schema rules.
  Changes affect both backend (generated interfaces) and frontend (api/types.ts)
- [rules/java.md](rules/java.md) - Entity/domain split, JPA, MapStruct gotchas, query performance, testing.
  API spec changes require `make api-generate` before implementation
- [rules/frontend.md](rules/frontend.md) - Project utilities, state management, layout, error handling

## Contributing

1. Fork + clone, create feature branch from `dev`
2. `make dev` to start local environment
3. Follow existing patterns - read adjacent code before writing new code
4. `make pre-push` (must pass before PR)
5. PR against `dev` branch

When introducing a new pattern or convention, update the relevant `.claude/rules/` file in the same PR.

## Testing

- After code changes: `make build` (or `make server-test` / `bun run lint && bun run test`)
- Backend: `./gradlew check` = tests + Spotless + JaCoCo coverage (50% gate)
- Frontend: `bun run lint` + `bun run test` (vitest + happy-dom)
