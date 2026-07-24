# JDC Platform — Bug Tracking Ledger

**Created:** 2026-07-24
**Last Updated:** 2026-07-24
**Active Bug:** Multiple fixes in progress — hardcoded URLs, RestTemplate violations

---

## Legend

- **Status:** `[ ] Pending` | `[~] In Progress` | `[x] Fixed` | `[✓] Verified`
- **Severity:** Critical (stop-ship) | Major (high-impact) | Minor (low-impact)
- **Scope:** Systemic (all services) | Per-Service (single service)

---

## SYSTEMIC BUGS (Affect All/Most Services)

### S-001: Hardcoded Production DB Credentials in application-external.yml
- **Severity:** Critical
- **Scope:** Systemic — ALL 21 backend services
- **Current Behavior:** Every service's `application-external.yml` contains plaintext production credentials: `username: systems_admin`, `password: Hcu4ieD8R13qaf7JVSsu`, host `in0-eplmdb-v01:5432`.
- **Expected Behavior:** Credentials must be supplied via environment variables or secrets manager. No plaintext secrets in source control.
- **Root Cause:** Copy-pasted config template across all services without externalizing secrets.
- **Dependencies:** All services' datasource configuration.
- **Status:** `[x] Fixed`
- **Fix Specification:** Replace hardcoded values with `${DB_USERNAME}` and `${DB_PASSWORD}` (no defaults) in all 21 `application-external.yml` files.

### S-002: Default DB Password Fallback in application.yml
- **Severity:** Critical
- **Scope:** Systemic — ALL 21 backend services + 5 docker profiles
- **Current Behavior:** All services use `${DB_PASSWORD:jirapass123}` with a guessable fallback default.
- **Expected Behavior:** No default password; application should fail fast if `DB_PASSWORD` env var is not set.
- **Root Cause:** Convenience defaults left in production config.
- **Dependencies:** All services' datasource configuration, Docker Compose.
- **Status:** `[x] Fixed`
- **Fix Specification:** Remove default values: change `${DB_PASSWORD:jirapass123}` to `${DB_PASSWORD}` and `${DB_USERNAME:jiraadmin}` to `${DB_USERNAME}` in all 21 `application.yml` files.

### S-003: GlobalExceptionHandler Unsafe FieldError Cast
- **Severity:** Major
- **Scope:** Systemic — 16 services with GlobalExceptionHandler
- **Current Behavior:** `((FieldError) error).getField()` casts all `ObjectError` to `FieldError`. Class-level validation errors throw `ClassCastException`, returning 500 instead of 400.
- **Expected Behavior:** Properly handle both `FieldError` and `ObjectError` types.
- **Root Cause:** Copy-pasted exception handler template without handling all error subtypes.
- **Dependencies:** Every controller endpoint that uses `@Valid`.
- **Status:** `[x] Fixed`
- **Fix Specification:** Replace unsafe cast with `instanceof` check:
  ```java
  String fieldName = (error instanceof FieldError fe) ? fe.getField() : error.getObjectName();
  ```

### S-004: Service URL Defaults Point to PostgreSQL Host
- **Severity:** Critical
- **Scope:** 5 services (workflow, issue, search, project, test) — 15 config entries
- **Current Behavior:** Service URLs default to `http://${DB_HOST:-postgres}:${DB_PORT:-5432}` — REST calls sent to the database port.
- **Expected Behavior:** Default to actual HTTP service endpoints (e.g., `http://jira-issue-service:8084`).
- **Root Cause:** Config template uses DB host/port variables for service URLs.
- **Dependencies:** All inter-service REST communication.
- **Status:** `[x] Fixed`
- **Fix Specification:** Set correct default URLs per service port mapping from the architecture diagram.

### S-005: Wildcard CORS on All Controllers
- **Severity:** Major
- **Scope:** Systemic — 50 controller classes across 12 services
- **Current Behavior:** `@CrossOrigin(origins = "*")` allows any origin to make requests.
- **Expected Behavior:** CORS restricted to known frontend origins via centralized config.
- **Root Cause:** Development convenience annotation never removed.
- **Dependencies:** All REST controllers, frontend integration.
- **Status:** `[x] Fixed`
- **Fix Specification:** Remove `@CrossOrigin(origins = "*")` from all controllers. Each service already has or should have a centralized CORS config via `WebMvcConfigurer` or `OpenApiConfig`.

### S-006: UUID.randomUUID() Fallback for Missing X-User-Id
- **Severity:** Critical
- **Scope:** 26+ controller methods across 8 services
- **Current Behavior:** When `X-User-Id` header is absent, a random UUID is used as the actor. Unauthenticated requests proceed silently with phantom identities.
- **Expected Behavior:** Return 401 when user identity is missing on mutation endpoints.
- **Root Cause:** Development convenience code left in production.
- **Dependencies:** All controllers using `X-User-Id` header.
- **Status:** `[x] Fixed`
- **Fix Specification:** Make `X-User-Id` required (`required = true`) on all mutation endpoints. Remove `UUID.randomUUID()` fallback.

### S-007: Inline `new RestTemplate()` Bypasses Spring Context
- **Severity:** Major
- **Scope:** 40+ service classes across 12 services
- **Current Behavior:** `private final RestTemplate restTemplate = new RestTemplate()` — no timeouts, no connection pooling, no circuit breakers.
- **Expected Behavior:** Injected Spring-managed `RestTemplate` bean with configured timeouts.
- **Root Cause:** Each service class creates its own unmanaged instance.
- **Dependencies:** All inter-service REST communication.
- **Status:** `[x] Fixed`
- **Fix Specification:** In each service, create a `@Bean RestTemplate` in a config class with timeouts, then inject via constructor (remove inline `= new RestTemplate()`).

---

## PER-SERVICE CRITICAL BUGS

### AUTH-001: SAML Response Signatures Never Verified
- **Severity:** Critical
- **Service:** jira-auth-service
- **Current Behavior:** `SamlResponseHandler.parseResponse()` base64-decodes SAML XML but never verifies the digital signature. Any attacker can forge a SAML response.
- **Expected Behavior:** Verify XML signature against IdP's certificate before trusting assertions.
- **Root Cause:** Signature verification step was never implemented.
- **File:** `jira-auth-service/src/main/java/com/jira/auth/security/SamlResponseHandler.java:21-59`
- **Dependencies:** SAML SSO login flow (Task 1.4).
- **Status:** `[ ] Pending`
- **Fix:** Add `javax.xml.crypto.dsig.XMLSignatureFactory` verification against `SamlConfiguration.idpCertificate`.

### AUTH-002: JWT Tokens Passed in URL Query Parameters
- **Severity:** Critical
- **Service:** jira-auth-service
- **Current Behavior:** SAML success handler redirects with `?token=...&refreshToken=...` in the URL. Tokens leak via browser history, referer headers, access logs.
- **Expected Behavior:** Use short-lived authorization code exchangeable for tokens via POST.
- **Root Cause:** Quick implementation without considering token leakage vectors.
- **File:** `jira-auth-service/src/main/java/com/jira/auth/security/SamlAuthenticationSuccessHandler.java:80-83`
- **Dependencies:** SAML SSO flow, frontend auth callback.
- **Status:** `[x] Fixed`
- **Fix:** Implement auth-code exchange pattern: store tokens server-side keyed by random code, redirect with `?code=<random>`, client exchanges via POST.

### AUTH-003: Hardcoded Admin Credentials & Password Logged
- **Severity:** Critical
- **Service:** jira-auth-service
- **Current Behavior:** `DataInitializer` creates admin user with `admin123` password and logs the plaintext password.
- **Expected Behavior:** Admin credentials from env vars; never log passwords.
- **File:** `jira-auth-service/src/main/java/com/jira/auth/config/DataInitializer.java:32,79`
- **Dependencies:** Auth service startup.
- **Status:** `[x] Fixed`
- **Fix:** Read password from `${ADMIN_PASSWORD}` env var. Remove password from log statement.

### AUTH-004: Refresh Token Not Validated as Refresh Type
- **Severity:** Major
- **Service:** jira-auth-service
- **Current Behavior:** `refreshToken()` validates signature/expiry but doesn't check `type` claim. Access tokens can be used as refresh tokens.
- **Expected Behavior:** Verify `claims.get("type").equals("refresh")` before issuing new tokens.
- **File:** `jira-auth-service/src/main/java/com/jira/auth/service/AuthService.java:82-108`
- **Status:** `[x] Fixed`
- **Fix:** Add token type validation in `refreshToken()`.

### AUTH-005: /auth/me Allows Unauthenticated User Enumeration
- **Severity:** Major
- **Service:** jira-auth-service
- **Current Behavior:** `/auth/me` is in `permitAll()` and accepts any UUID via `X-User-Id` header, returning user details.
- **Expected Behavior:** Requires authentication; user ID from JWT, not header.
- **File:** `jira-auth-service/src/main/java/com/jira/auth/config/SecurityConfig.java:65`
- **Status:** `[x] Fixed`
- **Fix:** Remove `/auth/me` from `permitAll()`.

### AUTH-006: Unbounded In-Memory Audit Log (OOM)
- **Severity:** Major
- **Service:** jira-auth-service
- **Current Behavior:** `SecurityAuditService.userAuditLog` ConcurrentHashMap grows without eviction. Will OOM over time.
- **Expected Behavior:** Bounded cache with TTL or persist to database.
- **File:** `jira-auth-service/src/main/java/com/jira/auth/security/audit/SecurityAuditService.java:24`
- **Status:** `[x] Fixed`
- **Fix:** Replace with bounded Caffeine cache or persist events to DB.

### AUTH-007: Test Endpoint Issues Real JWT Tokens
- **Severity:** Major
- **Service:** jira-auth-service
- **Current Behavior:** `/api/admin/sso/saml/test-auth` generates real tokens for arbitrary user identity.
- **Expected Behavior:** Return dry-run result without actual token generation.
- **File:** `jira-auth-service/src/main/java/com/jira/auth/controller/SamlConfigController.java:98-107`
- **Status:** `[x] Fixed`
- **Fix:** Return mock response without calling `authenticateSamlUser()`.

### USER-001: Fake Bcrypt Password Hashing
- **Severity:** Critical
- **Service:** jira-user-service
- **Current Behavior:** `hashPassword()` returns `"$2b$10$" + password.hashCode()` — looks like bcrypt but uses `String.hashCode()` (32-bit, no salt, trivially reversible).
- **Expected Behavior:** Real bcrypt hashing via `BCryptPasswordEncoder`.
- **File:** `jira-user-service/src/main/java/com/jira/user/service/JiraUserManagementService.java:303-305`
- **Dependencies:** All user authentication, password storage.
- **Status:** `[x] Fixed`
- **Fix:** Replace with `new BCryptPasswordEncoder().encode(password)`.

### USER-002: Generated Password Stored Unhashed
- **Severity:** Critical
- **Service:** jira-user-service
- **Current Behavior:** When no password provided, `generatePassword()` returns raw UUID substring stored directly in `passwordHash` without hashing.
- **Expected Behavior:** Hash the generated password before storing.
- **File:** `jira-user-service/src/main/java/com/jira/user/service/JiraUserManagementService.java:77`
- **Status:** `[x] Fixed`
- **Fix:** Wrap: `.passwordHash(hashPassword(password != null ? password : generateRandomPassword()))`

### USER-003: Inner Exception Classes Shadow Global Handler
- **Severity:** Major
- **Service:** jira-user-service
- **Current Behavior:** Inner `ResourceNotFoundException` class shadows the package-level one. `GlobalExceptionHandler` catches the wrong type — all 404/409 become 500.
- **Expected Behavior:** Use the package-level exception classes.
- **File:** `jira-user-service/src/main/java/com/jira/user/service/JiraUserManagementService.java:311-322`
- **Status:** `[x] Fixed`
- **Fix:** Delete inner classes, import `com.jira.user.exception.*`.

### SEARCH-001: SQL Injection in JQL Parser
- **Severity:** Critical
- **Service:** jira-search-service
- **Current Behavior:** `JQLParser.clauseToSql()` builds SQL via string concatenation with raw user input. All handler methods (`handleProject`, `handleStatus`, etc.) concatenate values directly.
- **Expected Behavior:** Parameterized queries or properly escaped values.
- **File:** `jira-search-service/src/main/java/com/jira/search/service/JQLParser.java:368-645`
- **Dependencies:** All JQL search functionality.
- **Status:** `[x] Fixed`
- **Fix:** Escape all user values with single-quote doubling and validate types. Long-term: use parameterized queries.

### SEARCH-002: NOT IN Operator Silently Ignored
- **Severity:** Critical
- **Service:** jira-search-service
- **Current Behavior:** In `handleProject`, `handleStatus`, `handleIssueType`, `handlePriority` — the `not` variable is computed but never used in the returned SQL. `NOT IN` queries behave as `IN`.
- **Expected Behavior:** `NOT IN` queries should negate the result.
- **File:** `jira-search-service/src/main/java/com/jira/search/service/JQLParser.java:388-425`
- **Status:** `[x] Fixed`
- **Fix:** Include `not` variable in the return: `"project_id " + not + "IN (SELECT ...)"`.

### TEST-001: Remote Code Execution via ScriptEngine
- **Severity:** Critical
- **Service:** jira-test-service
- **Current Behavior:** `DatasetService.evaluateExpression()` passes user-supplied expressions to `ScriptEngine.eval()`. Arbitrary Java/JavaScript code execution.
- **Expected Behavior:** Safe arithmetic-only expression evaluation.
- **File:** `jira-test-service/src/main/java/com/jira/test/service/DatasetService.java:651-656`
- **Status:** `[x] Fixed`
- **Fix:** Replace ScriptEngine with regex-validated arithmetic-only evaluation.

### TEST-002: XXE Injection in XML Parser
- **Severity:** Critical
- **Service:** jira-test-service
- **Current Behavior:** `CiCdImportService` creates `DocumentBuilderFactory.newInstance()` with no security features disabled. External entities resolved.
- **Expected Behavior:** Disable DTDs, external entities, and external parameter entities.
- **File:** `jira-test-service/src/main/java/com/jira/test/service/CiCdImportService.java:224-225`
- **Status:** `[x] Fixed`
- **Fix:** Add `factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)` and related features.

### GATEWAY-001: Massive Auth Bypass via Excessive Public Paths
- **Severity:** Critical
- **Service:** jira-gateway
- **Current Behavior:** JWT filter exempts `/api/migration/**`, `/api/plans/**`, `/api/programs/**`, `/api/templates/**`, `/api/sprints/**`, `/api/boards/**` from authentication.
- **Expected Behavior:** Only auth endpoints (`/api/auth/login`, `/api/auth/register`, `/api/auth/refresh`) and health checks should be public.
- **File:** `jira-gateway/src/main/java/com/jira/gateway/filter/JwtAuthenticationFilter.java:44-65`
- **Status:** `[x] Fixed`
- **Fix:** Remove all resource-service paths from `PUBLIC_PATHS`.

### GATEWAY-002: Wildcard CORS in Gateway Filter
- **Severity:** Critical
- **Service:** jira-gateway
- **Current Behavior:** `CorsWebFilter` sets `Access-Control-Allow-Origin: *` unconditionally. Also conflicts with YAML CORS config (duplicate headers).
- **Expected Behavior:** Explicit origin allowlist; single CORS config source.
- **File:** `jira-gateway/src/main/java/com/jira/gateway/filter/CorsWebFilter.java:30`
- **Status:** `[x] Fixed`
- **Fix:** Remove `CorsWebFilter` class entirely; use YAML-based `globalcors` config with explicit origins.

### GATEWAY-003: Rate Limiter Integer Division Breaks Refill
- **Severity:** Major
- **Service:** jira-gateway
- **Current Behavior:** `1000 / 60 = 16` (integer division) for hourly rate. Users throttled to 16 req/hour instead of 1000.
- **Expected Behavior:** Correct token refill rates.
- **File:** `jira-gateway/src/main/java/com/jira/gateway/config/RateLimiterConfig.java:51-58`
- **Status:** `[x] Fixed`
- **Fix:** Use capacity directly: `Refill.greedy(DEFAULT_REQUESTS_PER_HOUR, Duration.ofHours(1))`.

### GATEWAY-004: GlobalExceptionHandler Not Registered as Bean
- **Severity:** Major
- **Service:** jira-gateway
- **Current Behavior:** Missing `@Component` — class is dead code. All errors use Spring default handler.
- **Expected Behavior:** Custom error handling active.
- **File:** `jira-gateway/src/main/java/com/jira/gateway/exception/GlobalExceptionHandler.java:17`
- **Status:** `[x] Fixed`
- **Fix:** Add `@Component` annotation.

### ISSUE-001: Race Condition in Issue Key Generation
- **Severity:** Critical
- **Service:** jira-issue-service
- **Current Behavior:** `synchronized(this)` only works in single JVM. Multi-instance deployment produces duplicate keys.
- **Expected Behavior:** Database-level sequence or `SELECT FOR UPDATE`.
- **File:** `jira-issue-service/src/main/java/com/jira/issue/service/IssueService.java:886-898`
- **Status:** `[x] Fixed`
- **Fix:** Use `findMaxIssueNumberByProjectKeyForUpdate()` (already exists in clone/move services).

### ISSUE-002: Security Fail-Open on Access Control
- **Severity:** Critical
- **Service:** jira-issue-service
- **Current Behavior:** `SecurityLevelService.canUserAccessLevel()` returns `true` on any exception. Network glitch grants all users access.
- **Expected Behavior:** Fail-closed by default.
- **File:** `jira-issue-service/src/main/java/com/jira/issue/service/SecurityLevelService.java:87-92`
- **Status:** `[x] Fixed`
- **Fix:** Change `return true` to `return false` in catch block.

### ISSUE-003: Hardcoded Fallback User ID
- **Severity:** Critical
- **Service:** jira-issue-service
- **Current Behavior:** `resolveUserId()` returns `00000000-0000-0000-0000-000000000001` when no `X-User-Id` header.
- **Expected Behavior:** Reject unauthenticated requests.
- **File:** `jira-issue-service/src/main/java/com/jira/issue/controller/IssueController.java:269-271`
- **Status:** `[x] Fixed`
- **Fix:** Throw 401 when `userId` is null.

### DASH-001: Wrong Repository in Dashboard Existence Check
- **Severity:** Critical
- **Service:** jira-dashboard-service
- **Current Behavior:** `addGadgetToDashboard` calls `gadgetInstanceRepository.findById(dashboardId)` — queries wrong table.
- **Expected Behavior:** Should use `dashboardRepository.findById(dashboardId)`.
- **File:** `jira-dashboard-service/src/main/java/com/jira/dashboard/service/DashboardService.java:180-181`
- **Status:** `[x] Fixed`
- **Fix:** Change to `dashboardRepository.findById(dashboardId)`.

### DOC-001: JPQL Queries Use PostgreSQL Native Operators (Startup Crash)
- **Severity:** Critical
- **Service:** jira-document-service
- **Current Behavior:** `@Query` uses `@>` operator and `::uuid[]` cast in JPQL mode. Causes `QuerySyntaxException` at startup.
- **Expected Behavior:** Use `nativeQuery = true` for PostgreSQL-specific syntax.
- **File:** `jira-document-service/src/main/java/com/jira/document/repository/LegalHoldRepository.java:23-24,32-33`
- **Status:** `[x] Fixed`
- **Fix:** Add `nativeQuery = true` and rewrite as native SQL.

### DOC-002: Delete Ignores Active Legal Holds
- **Severity:** Critical
- **Service:** jira-document-service
- **Current Behavior:** `deleteDocument` hard-deletes without checking for active legal holds. Compliance violation.
- **Expected Behavior:** Check for active holds before allowing deletion.
- **File:** `jira-document-service/src/main/java/com/jira/document/service/DocumentService.java:106-109`
- **Status:** `[x] Fixed`
- **Fix:** Query `LegalHoldAssignment` for active holds; throw exception if any exist.

### NOTIFY-001: Missing @EnableAsync Makes All @Async No-Ops
- **Severity:** Critical
- **Service:** jira-notification-service
- **Current Behavior:** 6 email-sending methods annotated `@Async` but `@EnableAsync` is missing. All execute synchronously, blocking HTTP threads.
- **Expected Behavior:** Async email sending.
- **File:** `jira-notification-service/src/main/java/com/jira/notification/JiraNotificationServiceApplication.java:9-11`
- **Status:** `[x] Fixed`
- **Fix:** Add `@EnableAsync` to application class.

### NOTIFY-002: Duplicate Notification Path Prefix
- **Severity:** Critical
- **Service:** jira-notification-service
- **Current Behavior:** Class `@RequestMapping("/api/notifications")` + method `@PostMapping("/notifications")` = actual path `/api/notifications/notifications`.
- **Expected Behavior:** Path should be `/api/notifications`.
- **File:** `jira-notification-service/src/main/java/com/jira/notification/controller/NotificationController.java:17,24,31`
- **Status:** `[x] Fixed`
- **Fix:** Remove `/notifications` from `@PostMapping` and `@GetMapping`.

### ADMIN-001: Permission Check Always Returns True
- **Severity:** Critical
- **Service:** jira-admin-service
- **Current Behavior:** `PermissionResolutionService.hasPermissionKey()` always returns `true`. All permission checks bypassed.
- **Expected Behavior:** Actual permission validation against database.
- **File:** `jira-admin-service/src/main/java/com/jira/admin/service/PermissionResolutionService.java:319-324`
- **Status:** `[ ] Pending`
- **Fix:** Implement actual permission lookup from the permissions table.

### ADMIN-002: Group Membership Operations Are No-Ops
- **Severity:** Critical
- **Service:** jira-admin-service
- **Current Behavior:** `GroupEntity.users` is `@Transient` — JPA never loads or persists it. `addUserToGroup()` modifies in-memory list only, never persisted.
- **Expected Behavior:** Use proper JPA relationship or `UserGroupMembershipRepository`.
- **File:** `jira-admin-service/src/main/java/com/jira/admin/entity/GroupEntity.java:29-31`
- **Status:** `[ ] Pending`
- **Fix:** Replace `@Transient` list with `UserGroupMembershipRepository` operations.

### PROJECT-001: Permission Check Always Returns True
- **Severity:** Critical
- **Service:** jira-project-service
- **Current Behavior:** `PermissionCheckService.hasPermission()` grants access unconditionally when `permissionKey.equals("ADMINISTER_PROJECTS")`.
- **Expected Behavior:** Verify actual admin role membership.
- **File:** `jira-project-service/src/main/java/com/jira/project/service/PermissionCheckService.java:52`
- **Status:** `[ ] Pending`
- **Fix:** Check actual role membership instead of string match.

### PROJECT-002: Duplicate Field Shadows Injected Dependency (NPE)
- **Severity:** Critical
- **Service:** jira-project-service
- **Current Behavior:** Non-final `private ProjectRepository projectRepository;` at line 477 shadows the constructor-injected field. Always `null`.
- **Expected Behavior:** Single injected field.
- **File:** `jira-project-service/src/main/java/com/jira/project/service/ProjectSchemeService.java:477`
- **Status:** `[x] Fixed`
- **Fix:** Delete line 477. Add `private final ProjectRepository projectRepository;` to top-level final fields.

### PROJECT-003: Service URL Points to Database
- **Severity:** Critical
- **Service:** jira-project-service
- **Current Behavior:** `issue.service.url` defaults to `http://${DB_HOST:-postgres}:${DB_PORT:-5432}` — cascadeDelete sends HTTP to PostgreSQL.
- **Expected Behavior:** Points to actual issue service.
- **File:** `jira-project-service/src/main/resources/application.yml:61-62`
- **Status:** `[x] Fixed`
- **Fix:** Change default to `http://jira-issue-service:8084`.

### WORKFLOW-001: All Endpoints permitAll() — Zero Authentication
- **Severity:** Critical
- **Service:** jira-workflow-service
- **Current Behavior:** SecurityConfig marks all endpoints as `permitAll()`. Hardcoded `admin/admin123` credentials.
- **Expected Behavior:** Proper authentication for workflow/admin endpoints.
- **File:** `jira-workflow-service/src/main/java/com/jira/workflow/config/SecurityConfig.java:37-46`
- **Status:** `[x] Fixed`
- **Fix:** Remove `permitAll()` from resource endpoints. Remove hardcoded credentials.

### WORKFLOW-002: SSRF via Unvalidated Webhook URL
- **Severity:** Critical
- **Service:** jira-workflow-service
- **Current Behavior:** `PostFunctionExecutor.triggerWebhook()` fetches arbitrary user-configured URLs. Can reach internal network, cloud metadata.
- **Expected Behavior:** URL validation against allowlist; block private IPs.
- **File:** `jira-workflow-service/src/main/java/com/jira/workflow/engine/PostFunctionExecutor.java:228-244`
- **Status:** `[ ] Pending`
- **Fix:** Validate URL scheme (https only) and block private/internal IP ranges.

### SPRINT-001: HashMap Used as Concurrent Data Store
- **Severity:** Critical
- **Service:** jira-sprint-service
- **Current Behavior:** `BulkOperationService`, `DashboardService`, `SavedFilterService` use plain `HashMap` accessed from concurrent HTTP threads. Causes infinite loops during rehash.
- **Expected Behavior:** Thread-safe data structure.
- **File:** `jira-sprint-service/src/main/java/com/jira/sprint/service/BulkOperationService.java:21`
- **Status:** `[x] Fixed`
- **Fix:** Replace `HashMap` with `ConcurrentHashMap`.

### COMP-001: Repository in Wrong Directory Path
- **Severity:** Critical
- **Service:** jira-component-service
- **Current Behavior:** `ComponentOwnershipHistoryRepository.java` is in `com/ira/component/` (missing "j"). Package declaration says `com.jira.component`. Spring won't find it.
- **Expected Behavior:** File in correct directory `com/jira/component/repository/`.
- **File:** `jira-component-service/src/main/java/com/ira/component/repository/ComponentOwnershipHistoryRepository.java`
- **Status:** `[x] Fixed`
- **Fix:** Move file to correct directory.

### PLAN-001: Backward Scheduling Broken (addWorkingDays infinite/no-op)
- **Severity:** Critical
- **Service:** jira-plan-service
- **Current Behavior:** `addWorkingDays()` only handles positive days. Negative days (backward scheduling) returns start date unchanged.
- **Expected Behavior:** Handle negative days by iterating backward.
- **File:** `jira-plan-service/src/main/java/com/jira/plan/service/WorkingDaysService.java:213-225`
- **Status:** `[x] Fixed`
- **Fix:** Use `Math.abs(days)` and iterate with `plusDays(-1)` when days is negative.

### PORTAL-001: Request Key Counter Resets on Restart
- **Severity:** Critical
- **Service:** jira-portal-service
- **Current Behavior:** `AtomicInteger` counter resets to 0 on restart. Produces duplicate keys (e.g., `PORTAL-1` again).
- **Expected Behavior:** Database-backed sequence for unique keys.
- **File:** `jira-portal-service/src/main/java/com/jira/portal/service/PortalService.java:30,247-249`
- **Status:** `[x] Fixed`
- **Fix:** Query `MAX(request_key)` from DB or use a database sequence.

### FRONTEND-001: XSS via Unsanitized dangerouslySetInnerHTML
- **Severity:** Critical
- **Service:** jira-frontend
- **Current Behavior:** `issue.description` passed directly to `dangerouslySetInnerHTML` with no sanitization. Script injection possible.
- **Expected Behavior:** Sanitize HTML with DOMPurify before rendering.
- **File:** `jira-frontend/src/features/issues/pages/IssueDetailPage.tsx:828`
- **Status:** `[x] Fixed`
- **Fix:** Use `DOMPurify.sanitize(issue.description)` before rendering.

### FRONTEND-002: XSS Bypass in Regex Sanitizer
- **Severity:** Critical
- **Service:** jira-frontend
- **Current Behavior:** `DynamicReadOnly.tsx` regex strips `<script>` and quoted `on*=""` but misses unquoted handlers like `<img src=x onerror=alert(1)>`.
- **Expected Behavior:** Use DOMPurify instead of regex sanitization.
- **File:** `jira-frontend/src/features/issues/components/renderers/DynamicReadOnly.tsx:43-48`
- **Status:** `[x] Fixed`
- **Fix:** Replace regex sanitizer with DOMPurify.

### FRONTEND-003: Hardcoded Fallback User-ID in Axios
- **Severity:** Critical
- **Service:** jira-frontend
- **Current Behavior:** Axios interceptor injects `X-User-Id: 11111111-1111-1111-1111-111111111111` when no user is authenticated.
- **Expected Behavior:** Do not send `X-User-Id` when unauthenticated.
- **File:** `jira-frontend/src/api/axiosClient.ts:41`
- **Status:** `[x] Fixed`
- **Fix:** Remove fallback; redirect to login when no user.

### MARKETPLACE-001: Plugin Won't Compile (5 Build Failures)
- **Severity:** Critical
- **Service:** jira-marketplace-plugin
- **Current Behavior:** Malformed XML (`<-or>`), 5 classes have method bodies missing in non-abstract classes, wrong superclass name, missing import, circular self-injection.
- **Expected Behavior:** Plugin compiles and loads.
- **Files:** `atlassian-plugin.xml:90`, `ImportResource.java`, `ReportsResource.java`, `TestExecutionResource.java`, `TestPlanResource.java`, `TestSetResource.java`, `UpdateTestStatusFunction.java:12`, `TestManagementDao.java:11`, `TestImportProcessor.java:10`
- **Status:** `[x] Fixed`
- **Fix:** Fix XML tag, change classes to interfaces, fix superclass name, add import, remove circular injection.

---

## PER-SERVICE MAJOR BUGS

### VERSION-001: Duplicate Name Check Broken (null excludeId)
- **Severity:** Major
- **Service:** jira-version-service
- **File:** `jira-version-service/src/main/java/com/jira/version/service/VersionService.java:56`
- **Status:** `[ ] Pending`
- **Fix:** Add separate `existsByProjectIdAndName()` for create case.

### REPORT-001: Delete Without Ownership Check
- **Severity:** Major
- **Service:** jira-report-service
- **File:** `jira-report-service/src/main/java/com/jira/report/service/ReportService.java:186-189`
- **Status:** `[ ] Pending`
- **Fix:** Accept userId, verify ownership before delete.

### REPORT-002: Weekly Period Calculation Wrong
- **Severity:** Major
- **Service:** jira-report-service
- **File:** `jira-report-service/src/main/java/com/jira/report/service/StandardReportService.java:194`
- **Status:** `[ ] Pending`
- **Fix:** Use `LocalDate.parse().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)`.

### COMMENT-001: Internal Filter Parameter Ignored
- **Severity:** Major
- **Service:** jira-comment-service
- **File:** `jira-comment-service/src/main/java/com/jira/comment/service/CommentService.java:148-156`
- **Status:** `[ ] Pending`
- **Fix:** Add repository method that actually filters by `internal` field.

### ATTACH-001: MIME Type Validation Trusts Client Header
- **Severity:** Major
- **Service:** jira-attachment-service
- **File:** `jira-attachment-service/src/main/java/com/jira/attachment/service/AttachmentService.java:66`
- **Status:** `[ ] Pending`
- **Fix:** Detect MIME type server-side via `Files.probeContentType()`.

### ATTACH-002: Hardcoded Allowed MIME Types (Config Ignored)
- **Severity:** Major
- **Service:** jira-attachment-service
- **File:** `jira-attachment-service/src/main/java/com/jira/attachment/service/AttachmentService.java:39-47`
- **Status:** `[ ] Pending`
- **Fix:** Inject YAML config: `@Value("${jira.attachment.allowed-types}")`.

### MIGRATION-001: String.format Path Corruption (Crash)
- **Severity:** Critical
- **Service:** jira-migration-service
- **File:** `jira-migration-service/src/main/java/com/jira/migration/storage/LocalStorageService.java:317-328`
- **Status:** `[x] Fixed`
- **Fix:** Fix format string to match argument count and types.

### SPRINT-002: BulkOperationService All Operations Are Stubs
- **Severity:** Major
- **Service:** jira-sprint-service
- **File:** `jira-sprint-service/src/main/java/com/jira/sprint/service/BulkOperationService.java:68-206`
- **Status:** `[ ] Pending`
- **Fix:** Implement actual service calls in each process method.

### SPRINT-003: Missing @EnableScheduling
- **Severity:** Major
- **Service:** jira-sprint-service
- **File:** `jira-sprint-service/src/main/java/com/jira/sprint/JiraSprintServiceApplication.java:15`
- **Status:** `[x] Fixed`
- **Fix:** Add `@EnableScheduling` to application class.

---

## ACTIVE WORK LOG

| Timestamp | Bug ID | Action | Status |
|-----------|--------|--------|--------|
| 2026-07-24 | S-001 | Starting systemic credential fix | In Progress |
