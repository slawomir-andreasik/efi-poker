# API Contract (OpenAPI First)

## Workflow

1. Edit source YAML in `api/src/main/resources/openapi/` (paths/, schemas/, parameters/)
2. Run `make api-generate` - flattens all `$ref`s + generates Java interfaces and DTOs
3. Implement controllers by implementing the generated `*Api` interface (e.g. `RoomsApi`)
4. Update `frontend/src/api/types.ts` manually to match new response shapes

## Modular YAML Structure

Root template (`api-definition-template.yaml`) references paths via `$ref: './paths/rooms/room.yaml'`.
Path files reference schemas with relative `$ref: '../../schemas/request/CreateRoomRequest.yaml'`.

## Schema Rules

- Separate `request/` and `response/` directories
- Enums in own files in `schemas/enums/`, referenced via `$ref`. Standalone enum files generate as standalone Java enums; inline enums generate as inner classes. Prefer standalone.
- Always use `$ref` to schema files in path files
- Use `allOf` to compose shared fields across response schemas (e.g. `RoomBase.yaml` shared by `RoomResponse`, `RoomDetailResponse`, `RoomAdminResponse`). This eliminates field duplication while the generator inlines all properties into each DTO
- All response schemas MUST have a `required` array listing non-nullable fields
- Optional fields that can be null MUST have `nullable: true`
- Response fields that are constrained to enum values (e.g. `storyPoints`, `finalEstimate`) should use `$ref` to the enum schema, not `type: string`
- All path operations SHOULD declare error responses: `400` (request body validation), `401` (JWT-protected), `404` (ID-lookup), with `application/problem+json` content
- Reusable path/header parameters go in `parameters/` directory, referenced via `$ref`. Don't define inline params that repeat across operations
- After allOf changes, run `make api-generate` and verify contract tests pass (they resolve allOf recursively)
