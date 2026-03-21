# Security Conventions

Security rules for Spring Boot + React + Nginx stack. Based on OWASP Top 10:2025.

## A01: Broken Access Control

- Every new endpoint: authorization check in the controller (`permitAll()` delegates responsibility to controller code)
- Admin operations: validate admin code/role BEFORE business logic - never after
- Multi-level validation for scoped resources: verify entity exists → belongs to parent → user has access to specific scope
- Secrets (admin codes, API keys): store as BCrypt hashes, compare via `passwordEncoder.matches()`
- Never trust client-supplied identity headers without server-side validation against the authenticated session
- CORS: explicit allowed origins, never wildcard `*` with credentials. Restrict allowed methods and headers to what's actually needed

## A02: Security Misconfiguration

- Startup validator MUST reject default/placeholder credentials in production profile (passwords, JWT secrets, DB credentials, LDAP/OAuth config)
- Safe defaults: registration disabled, features opt-in not opt-out
- Security headers: serve via reverse proxy (Nginx), disable Spring Security defaults in production to avoid duplication. Keep Spring defaults active in dev profile via a config property
- Nginx `add_header` not inherited into `location` blocks - repeat in every block, use variables for long values (CSP, HSTS)
- Required Nginx headers: CSP (`default-src 'self'`), X-Frame-Options (`DENY`), X-Content-Type-Options (`nosniff`), Referrer-Policy, Permissions-Policy, HSTS (`max-age=31536000; includeSubDomains; preload`)
- Actuator/management endpoints: restricted to `health`, `prometheus`, `info` - management on separate port in production, never exposed to public network
- Debug endpoints, Swagger UI, API docs: disabled in production profile

## A03: Software Supply Chain Failures

- Lock files MUST be committed (`bun.lockb`, `gradle.lockfile`, `package-lock.json`) - reproducible builds
- No wildcard (`*`) or `latest` version ranges in dependency declarations
- Dependency vulnerability scanning in CI (Trivy, Snyk, Dependabot, or equivalent)
- Only trusted registries (Maven Central, npmjs.com) - no custom/mirror registries without review
- Review transitive dependencies when adding new libraries - check for known CVEs
- Docker base images: use specific version tags (not `latest`), prefer `-alpine` variants, run as non-root user

## A04: Cryptographic Failures

- Passwords and secrets: BCrypt with strength 12+ - never plaintext, never MD5/SHA
- Admin/API codes: hash before storage, return raw value only at creation time. BCrypt default strength (10) is acceptable for high-entropy codes like UUIDs (128-bit) where brute-force is infeasible
- Timing-safe comparison: `passwordEncoder.matches()` or `MessageDigest.isEqual()` - never `String.equals()`
- TLS everywhere: enforce HTTPS in production, HSTS with `preload`
- JWT: HS512 or RS256 minimum, enforce expiration, rotate secrets periodically. MUST include `iss` (issuer) and `aud` (audience) standard claims for defense-in-depth
- Refresh tokens: httpOnly/Secure/SameSite=Strict cookies, SHA-256 hash before DB storage, rotate on every use, revoke all on logout. DB lookup for rotation MUST use pessimistic locking (`@Lock(PESSIMISTIC_WRITE)`) to prevent concurrent rotation race conditions
- Never log or include secrets, tokens, or password hashes in API responses or error messages

## A05: Injection

- OpenAPI text fields: `pattern: '^([^<]|<(?![a-zA-Z/]))*$'` - blocks HTML tags (`<script>`, `<img`) but allows comparison operators (`< 13`, `3 > 2`)
- Password fields: `pattern: '^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$'`
- Email fields: `format: email` (no extra pattern)
- All string fields: require `minLength` + `maxLength`
- SQL: parameterized queries only (`@Query` with `:namedParams`) - never string concatenation
- LDAP: use `LdapEncoder.filterEncode()` and `LdapEncoder.nameEncode()`
- Frontend: no `dangerouslySetInnerHTML`, `innerHTML`, `eval()`, `document.write`
- URL rendering: validate with `new URL()` constructor, allow only `http:` / `https:` protocols
- After OpenAPI schema changes: regenerate code (`make api-generate`) and update frontend types

## A06: Insecure Design

- Threat model for new features: identify assets, entry points, trust boundaries before implementation
- Rate limiting on all public write endpoints (POST, PUT, PATCH, DELETE) - per IP, configurable window
- Deny by default: new endpoints require explicit authorization, new features default to disabled
- Business logic abuse: validate state transitions server-side (e.g., can't reveal results before voting, can't vote after reveal)
- Input length limits as defense against DoS - all fields bounded by `maxLength`
- Separation of concerns: authentication (who are you?) vs authorization (what can you do?) - different exception types, different HTTP codes

## A07: Authentication Failures

- Authentication failure (bad credentials) → `401 Unauthorized` - use a dedicated exception class
- Authorization failure (insufficient permissions) → `403 Forbidden` - separate exception class
- Never reuse the same exception for both - HTTP status codes must be distinguishable
- Generic error messages for auth failures (`"Invalid credentials"`) - no username enumeration
- Session management: stateless JWT for APIs, short expiration, no sensitive data in token payload
- Refresh token flow: access token (24h) in localStorage + refresh token (30/90 days) in httpOnly cookie. Silent refresh on 401. POST /auth/refresh requires no Authorization header (cookie-based)
- Guest tokens: project-scoped JWT (90 days) with minimal claims (projectId, participantId, admin flag). Stored in localStorage per project, no refresh cookie
- All `UUID.fromString()` calls on JWT claims MUST be wrapped in try-catch - return null on `IllegalArgumentException` (defense against malformed token payloads)
- Account lockout or progressive delay after repeated failed login attempts
- Admin code validation failures: log with context (slug/projectId, userId, hasCode flag) for brute-force detection
- Production startup: reject default passwords, secrets, and placeholder OAuth/LDAP config

## A08: Software or Data Integrity Failures

- Database migrations (Liquibase): review for data corruption risk, test rollback path, never auto-apply DDL (`ddl-auto: validate` only)
- Deserialization: never deserialize untrusted data without validation - use allowlists for polymorphic types
- Frontend: no `eval()`, no dynamic `<script>` injection, CSP `script-src 'self'` blocks inline scripts
- CI/CD: protected main branch, require PR reviews, no force push
- OpenAPI code generation: review generated code after spec changes, don't blindly trust generated validators
- Sub-resource integrity (SRI) for any externally-loaded scripts or stylesheets

## A09: Security Logging and Alerting Failures

- Error responses: RFC 9457 `ProblemDetail` format with `traceId` for correlation
- Never expose framework internals in errors (class names, SQL, stack traces, file paths)
- Generic 404 for unmapped paths (`"Not found"`) - not Spring's `"No static resource..."` message
- Health endpoint: status only, no version field (prevents fingerprinting)
- Log security-relevant events: failed login attempts, access denied, admin operations, password changes. Include actor identity (userId or "anonymous") and action context
- Never log passwords, tokens, secrets, or full request bodies containing sensitive data
- Structured logging (JSON) for machine parsing, correlation IDs across services

## A10: Mishandling of Exceptional Conditions

- File exports: explicit `Content-Type` header (`text/csv; charset=UTF-8`, not framework default)
- Public endpoints: never return internal IDs (UUIDs, database PKs) - return display names or opaque identifiers
- Results/analytics endpoints: gate by status (e.g., only after reveal/completion) + require authorization
- Null/empty responses: return proper HTTP status (204 No Content, 404 Not Found) - not 200 with null body
- Timeout handling: set `proxy_read_timeout` in Nginx, connection timeouts on database and HTTP clients
- Graceful degradation: health check fails → 503 Service Unavailable with meaningful status, not stack trace
