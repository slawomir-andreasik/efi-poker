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
- Enums in own files in `schemas/enums/`, referenced via `$ref`. Note: enums generate as Java inner classes - see [java.md](java.md) MapStruct Gotchas for null-guard pattern
- Always use `$ref` to schema files in path files
