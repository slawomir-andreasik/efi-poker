# Java Conventions

## Entity / Domain Split

- JPA entities use `*Entity` suffix (e.g. `RoomEntity`). Key Lombok: `@ToString(onlyExplicitlyIncluded = true)`
- Manual `equals()/hashCode()` with `Hibernate.getClass()` for proxy safety
- Domain records with `@Builder(toBuilder = true)`, no suffix (e.g. `Room`)
- Record accessors: `room.project().id()` not `room.getProject().getId()`
- `*EntityMapper` (MapStruct) per feature: entity <-> domain record
- Services use entities internally, return domain records at the boundary: `roomEntityMapper.toDomain(roomRepository.save(entity))`
- Controllers implement generated `*Api` interfaces from OpenAPI (see [api-contract.md](api-contract.md)), work with generated DTOs

## JPA / Repositories

- All relations: `@ManyToOne(fetch = FetchType.LAZY)` with entity references
- Load children via repository queries
- `spring.jpa.open-in-view=false` (set in `application.yml`)
- All finder queries: explicit `@Query` text blocks with `JOIN FETCH` for LAZY relations
- `LEFT JOIN FETCH` for nullable, `JOIN FETCH` for non-nullable
- Fetch the full chain needed by the mapper (e.g. `e.task.room.project.createdBy`)

## Query Performance

- Load all data BEFORE loops: `findByXxxIn(Collection<UUID>)` -> `Map<UUID, List<Y>>` -> loop uses map
- Validation methods return the fetched entity so callers reuse it (e.g. `validateAdminCode()` returns `Project`)
- Filter in memory when collection is already loaded (`stream().filter()` over extra DB query)
- Combine fetch + validate into one method when controllers always do both (e.g. `validateAdminAndGetRoom(id, code)` replaces `getRoom()` + `validateAdmin()` - single DB fetch instead of two)
- Delete: `findById()` + `delete(entity)`, never `existsById()` + `deleteById()` (TOCTOU race + double query)

## MapStruct Gotchas

- `EfiMapperConfig` as shared `@MapperConfig` - all project mappers reference it via `config = EfiMapperConfig.class`
- Boolean `is*` fields (e.g. `isPublic`): MapStruct strips "is" prefix -> add explicit `@Mapping(target = "isPublic", expression = "java(entity.isPublic())")`
- String-to-generated-enum: add null-guarded `default` method per enum type using `EnumType.fromValue(value)` (not `valueOf()` - numeric enum constants like StoryPoints use prefixed names `_0`, `_5`). See also [api-contract.md](api-contract.md) Schema Rules for enum file structure
- When a single enum is shared by multiple DTOs (e.g. `UserRole` used in both `UserResponse` and `AdminUserResponse`), use one `default` mapping method for the standalone enum - not separate methods per DTO inner class

## Testing

### Base Classes (enforced by ArchUnit)

| Base class | Tag | Spring | Use for |
|------------|-----|--------|---------|
| `BaseUnitTest` | `unit` | No | Services, mappers, domain logic (Mockito) |
| `BaseArchUnitTest` | `arch` | No | Architecture rules, convention enforcement |
| `BaseComponentTest` | `component` | `@SpringBootTest` + Testcontainers | Controller integration, repository |
| `BaseModuleTest` | `module` | `@ApplicationModuleTest` + Testcontainers | Spring Modulith module interaction |

### Conventions

- Method names: `should_` + snake_case, `@Nested` + `@DisplayName` for grouping
- BDDMockito: `given()` / `then()` - not `when()` / `verify()`
- Final fields over `@BeforeEach` (JUnit 5 creates new instance per test)
- Each test creates its own data - no shared mutable state between tests
- Unit tests: `@Mock` + `@InjectMocks`, assert return values AND entity state after mutation
- Integration tests: shared Testcontainers PostgreSQL, `@Transactional` (auto-rollback), `MockMvc`
- Module tests: all in one class (`AllModulesIntegrationTest`), `@Nested` per module boundary
- Mapper tests: leaf mappers use `Mappers.getMapper()`, non-leaf use `new XxxMapperImpl(dep)`
- Never use `@DirtiesContext` - clean up state manually instead
- Protect Spring context cache: `@MockBean`, `@SpyBean`, `@DynamicPropertySource` on individual test classes each force a new context. If you need a mock, add it to `BaseComponentTest` so all integration tests share it. If you need dynamic properties (e.g. embedded server port), accept a separate context but document why
