# JDC Platform — Consolidated Production Bug Audit Report

**Date:** 2026-07-24
**Scope:** 25 microservices + frontend (1,700+ source files scanned)
**Auditor:** Adversarial Code Review (Principal Distributed Systems Engineer level)

---

## Executive Summary

| Severity | Count | Description |
|----------|-------|-------------|
| **CRITICAL** | 78 | RCE, XXE, SQL injection, XSS, SAML bypass, credential exposure, compilation failures |
| **HIGH** | 200+ | Authorization failures, SSRF, race conditions, resource leaks, logic errors |
| **MEDIUM** | 240+ | Missing validation, N+1 queries, exception swallowing, config issues |
| **LOW** | 95+ | Dead code, minor logic errors, logging concerns |
| **TOTAL** | **613+** | Across all 25 services + frontend (ALL scans complete) |

### Stop-Ship Issues (Must Fix Before Any Production Deployment)

1. **Hardcoded production DB password** (`Hcu4ieD8R13qaf7JVSsu`) — committed in EVERY service's `application-external.yml`. **Rotate immediately.**
2. **Remote Code Execution** — `jira-test-service` DatasetService evaluates arbitrary JavaScript via `ScriptEngine.eval()` (Bug: test-service #2)
3. **XXE Injection** — `jira-test-service` CiCdImportService parses XML without disabling external entities (Bug: test-service #1)
4. **SQL Injection** — `jira-search-service` JQLParser builds SQL via string concatenation from user input (Bug: search-service #1-3)
5. **SAML Authentication Bypass** — `jira-auth-service` never verifies SAML response signatures (Bug: auth-service #3)
6. **Zero Authentication on 15+ services** — Most services accept spoofable `X-User-Id` header or fall back to `UUID.randomUUID()`
7. **Fake bcrypt password hashing** — `jira-user-service` uses `String.hashCode()` prefixed with `$2b$10$` (Bug: user-service #1)

---

## Systemic Issues (Present Across All Services)

### S1: Hardcoded Credentials in Source Control (ALL 22 services)

Every service has `application-external.yml` with:
```yaml
username: systems_admin
password: Hcu4ieD8R13qaf7JVSsu  # PRODUCTION PASSWORD
```
And `application-local.yml` with `password: UNIpay@123`.

**Remediation:** Delete all hardcoded passwords. Use `${DB_PASSWORD}` without fallback defaults. Rotate ALL exposed credentials. Add `application-external.yml` and `application-local.yml` to `.gitignore`. Scrub from git history.

### S2: No Authentication / Spoofable Identity (20+ services)

Pattern found everywhere:
```java
@RequestHeader(value = "X-User-Id", required = false) UUID userId
UUID actor = userId != null ? userId : UUID.randomUUID();
```
Any unauthenticated caller can perform any operation. Phantom random UUIDs break audit trails.

**Remediation:** Make `X-User-Id` required on all mutation endpoints. Add JWT validation at gateway + service level. Extract user identity from security context, not headers.

### S3: Wildcard CORS on Every Controller (ALL services)

```java
@CrossOrigin(origins = "*")
```
Allows any malicious website to make cross-origin requests.

**Remediation:** Remove all `@CrossOrigin(origins = "*")`. Configure CORS centrally via `WebMvcConfigurer` with explicit allowed origins from configuration.

### S4: `new RestTemplate()` Bypasses Spring Context (15+ services)

```java
private final RestTemplate restTemplate = new RestTemplate();
```
No timeouts, no connection pooling, no circuit breakers. Threads hang indefinitely when downstream services are slow.

**Remediation:** Define a `@Bean RestTemplate` with configured timeouts (5s connect, 30s read) and inject everywhere.

### S5: GlobalExceptionHandler Unsafe FieldError Cast (ALL services)

```java
ex.getBindingResult().getAllErrors().forEach(error -> {
    String fieldName = ((FieldError) error).getField(); // ClassCastException if ObjectError
});
```
Causes 500 errors for class-level validation failures.

**Remediation:** Check `error instanceof FieldError` before casting in every service's `GlobalExceptionHandler`.

### S6: Service URL Defaults Point to Database Host (5+ services)

```yaml
issue:
  service:
    url: ${ISSUE_SERVICE_URL:http://${DB_HOST:-postgres}:${DB_PORT:-5432}}
```
REST calls intended for microservices are sent to PostgreSQL.

**Remediation:** Set correct HTTP service defaults (e.g., `http://localhost:8084`).

---

## Per-Service Bug Summary

| # | Service | CRIT | HIGH | MED | LOW | Total | Top Issue |
|---|---------|------|------|-----|-----|-------|-----------|
| 1 | **jira-auth-service** | 4 | 12 | 11 | 4 | **31** | SAML signature bypass, JWT in URL params |
| 2 | **jira-test-service** | 3 | 5 | 7 | 0 | **15** | RCE via ScriptEngine, XXE injection |
| 3 | **jira-issue-service** | 5 | 8 | 16 | 6 | **35** | Race condition in key generation, fail-open security |
| 4 | **jira-sprint-service** | 3 | 16 | 19 | 4 | **42** | HashMap concurrency, stub bulk ops |
| 5 | **jira-workflow-service** | 4 | 12 | 15 | 4 | **35** | SSRF in webhooks, all endpoints permitAll |
| 6 | **jira-search-service** | 4 | 9 | 9 | 4 | **26** | SQL injection in JQL parser |
| 7 | **jira-project-service** | 4 | 8 | 12 | 8 | **32** | Permission check always returns true, null dependency |
| 8 | **jira-plan-service** | 3 | 9 | 16 | 7 | **35** | Backward scheduling broken, infinite loop risk |
| 9 | **jira-admin-service** | 4 | 10 | 14 | 2 | **30** | Permission always true, group ops are no-ops |
| 10 | **jira-report-service** | 5 | 11 | 10 | 4 | **30** | Delete without auth, data truncation at 1000 |
| 11 | **jira-dashboard-service** | 4 | 9 | 12 | 4 | **29** | Wrong repository in existence check |
| 12 | **jira-document-service** | 5 | 8 | 12 | 1 | **26** | Delete ignores legal holds, JPQL startup crash |
| 13 | **jira-migration-service** | 3 | 8 | 10 | 6 | **27** | String.format crash, JWT secret, InputStream leak |
| 14 | **jira-component-service** | 2 | 9 | 12 | 3 | **26** | Repository in wrong directory, audit log wrong values |
| 15 | **jira-version-service** | 2 | 8 | 10 | 5 | **25** | Duplicate name check broken, orphaned data on merge |
| 16 | **jira-notification-service** | 3 | 7 | 11 | 3 | **24** | Missing @EnableAsync, duplicate path prefix |
| 17 | **jira-user-service** | 2 | 5 | 10 | 6 | **23** | Fake bcrypt, inner class shadows global handler |
| 18 | **jira-gateway** | 3 | 9 | 7 | 2 | **21** | JWT secrets in VCS, massive auth bypass, rate limiter broken |
| 19 | **jira-attachment-service** | 0 | 6 | 7 | 4 | **17** | MIME type bypass, file-before-DB delete |
| 20 | **jira-comment-service** | 2 | 6 | 7 | 2 | **17** | Hardcoded fallback user, internal filter ignored |
| 21 | **jira-portal-service** | 3 | 7 | 4 | 0 | **14** | Counter resets on restart, stored XSS |
| 22 | **jira-audit-service** | 1 | 4 | 4 | 3 | **12** | Search filters silently ignored |
| 23 | **jira-marketplace-plugin** | 5 | 8 | 3 | 0 | **16** | Won't compile (5 build failures), malformed XML |
| 24 | **jira-frontend** | 3 | 6 | 6 | 0 | **15** | XSS via dangerouslySetInnerHTML, auth bypass |
| 25 | **jira-backend** (cross-svc) | 3 | 9 | 9 | 0 | **21** | Confirms systemic patterns across all services |

---

## CRITICAL Bugs — Detailed Breakdown

### Category 1: Remote Code Execution & Injection

| Bug | Service | File | Description |
|-----|---------|------|-------------|
| **RCE** | test-service | DatasetService.java:651 | `ScriptEngine.eval()` executes arbitrary JavaScript from user input |
| **XXE** | test-service | CiCdImportService.java:224 | XML parser allows external entity resolution |
| **SQLi** | search-service | JQLParser.java:368-645 | Entire JQL-to-SQL conversion uses string concatenation |
| **SQLi** | search-service | JqlParserService.java:151 | Second parser also concatenates user input into SQL |
| **SSRF** | auth-service | SamlConfigController.java:76 | Test connection fetches user-supplied URL |
| **SSRF** | workflow-service | PostFunctionExecutor.java:228 | Webhook URL from config is fetched without validation |
| **XSS** | frontend | IssueDetailPage.tsx:828 | `dangerouslySetInnerHTML` with unsanitized issue description |
| **XSS bypass** | frontend | DynamicReadOnly.tsx:43 | Regex sanitizer bypassed by unquoted event handlers |

### Category 2: Authentication & Authorization Bypass

| Bug | Service | File | Description |
|-----|---------|------|-------------|
| **SAML bypass** | auth-service | SamlResponseHandler.java:21 | SAML responses never verified against IdP certificate |
| **JWT forgery** | auth-service | SamlAuthSuccessHandler.java:80 | Tokens passed in URL query params (logged, cached, leaked) |
| **Auth bypass** | gateway | JwtAuthFilter.java:44-65 | Sprints, boards, plans, migrations all marked PUBLIC |
| **Perm bypass** | admin-service | PermissionResolutionService.java:319 | `hasPermissionKey()` always returns `true` |
| **Perm bypass** | project-service | PermissionCheckService.java:52 | `ADMINISTER_PROJECTS` check always returns `true` |
| **Auth bypass** | workflow-service | SecurityConfig.java:37-46 | All endpoints configured `permitAll()` |
| **Auth skip** | project-service | ProjectController.java:161-267 | Permission check skipped when `X-User-Id` absent |
| **Fake hash** | user-service | JiraUserManagementService.java:303 | `hashPassword()` uses `String.hashCode()` not bcrypt |
| **Plaintext** | user-service | JiraUserManagementService.java:77 | Generated passwords stored unhashed |
| **Hardcoded ID** | frontend | axiosClient.ts:41 | Fallback `X-User-Id` sent when unauthenticated |
| **Plugin broken** | marketplace-plugin | 5 files | Won't compile — wrong superclass, missing imports, circular injection |

### Category 3: Data Corruption & Logic Errors

| Bug | Service | File | Description |
|-----|---------|------|-------------|
| **Wrong repo** | dashboard-service | DashboardService.java:180 | `gadgetInstanceRepository.findById(dashboardId)` — queries wrong table |
| **Null dep** | project-service | ProjectSchemeService.java:477 | Duplicate non-final `projectRepository` field is always `null` |
| **Startup crash** | document-service | LegalHoldRepository.java:23 | PostgreSQL `@>` operators in JPQL (not native query) |
| **Key collision** | issue-service | IssueService.java:886 | `synchronized(this)` only works in single-JVM |
| **Key collision** | issue-service | TestManagementService.java:61 | Uses `count()+1` for key generation |
| **Key collision** | portal-service | PortalService.java:30 | AtomicInteger resets on restart |
| **NOT IN ignored** | search-service | JQLParser.java:388-425 | `not` variable computed but never used in SQL |
| **Legal hold violated** | document-service | DocumentService.java:106 | `deleteDocument` ignores active legal holds |
| **Path mismatch** | component-service | Repository dir | File in `com/ira/` instead of `com/jira/` |

---

## Prioritized Remediation Plan

### Sprint 0 — Emergency (Do Now, Before Next Deploy)

| Priority | Action | Effort | Services Affected |
|----------|--------|--------|-------------------|
| **P0** | Rotate DB password `Hcu4ieD8R13qaf7JVSsu` | 1h | ALL |
| **P0** | Remove all hardcoded secrets from git + history | 4h | ALL |
| **P0** | Disable `ScriptEngine.eval()` in test-service | 1h | test-service |
| **P0** | Add XXE protections to XML parsers | 1h | test-service |
| **P0** | Replace `String.hashCode()` with real BCrypt | 2h | user-service |
| **P0** | Implement SAML signature verification | 4h | auth-service |
| **P0** | Parameterize JQL-to-SQL conversion | 8h | search-service |

### Sprint 1 — Authentication & Authorization (Week 1-2)

| Priority | Action | Effort | Services Affected |
|----------|--------|--------|-------------------|
| **P1** | Make `X-User-Id` required on all mutation endpoints | 4h | ALL |
| **P1** | Remove `permitAll()` from workflow-service SecurityConfig | 1h | workflow-service |
| **P1** | Fix gateway PUBLIC_PATHS to only allow auth endpoints | 2h | gateway |
| **P1** | Implement `hasPermissionKey()` in admin-service | 4h | admin-service |
| **P1** | Fix `PermissionCheckService` in project-service | 2h | project-service |
| **P1** | Remove hardcoded `admin/admin123` from workflow SecurityConfig | 1h | workflow-service |
| **P1** | Fix `refreshToken()` to validate token type claim | 1h | auth-service |
| **P1** | Remove CORS wildcard from all controllers | 4h | ALL |

### Sprint 2 — Data Integrity & Logic Fixes (Week 3-4)

| Priority | Action | Effort | Services Affected |
|----------|--------|--------|-------------------|
| **P2** | Fix dashboard wrong-repository bug | 1h | dashboard-service |
| **P2** | Fix project-service duplicate `projectRepository` field | 1h | project-service |
| **P2** | Add `nativeQuery=true` to document-service JPQL | 1h | document-service |
| **P2** | Fix issue key generation with DB-level sequence | 4h | issue-service |
| **P2** | Fix `@Transient` fields in workflow entity | 4h | workflow-service |
| **P2** | Fix group membership `@Transient` in admin-service | 4h | admin-service |
| **P2** | Add legal hold check before document deletion | 2h | document-service |
| **P2** | Fix `NOT IN` operator in search JQL parser | 1h | search-service |
| **P2** | Fix component-service repository path (`ira` → `jira`) | 1h | component-service |
| **P2** | Add `@EnableAsync` to notification-service | 1h | notification-service |
| **P2** | Fix duplicate `/notifications/notifications` path | 1h | notification-service |

### Sprint 3 — Infrastructure & Reliability (Week 5-6)

| Priority | Action | Effort | Services Affected |
|----------|--------|--------|-------------------|
| **P3** | Replace all `new RestTemplate()` with Spring-managed bean | 8h | 15+ services |
| **P3** | Fix service URL defaults (DB host → HTTP service) | 2h | issue, project, workflow, search, report |
| **P3** | Fix `GlobalExceptionHandler` FieldError cast in all services | 4h | ALL |
| **P3** | Add circuit breakers (Resilience4j) for inter-service calls | 8h | ALL |
| **P3** | Fix rate limiter integer division in gateway | 1h | gateway |
| **P3** | Add bounded eviction to gateway rate limiter bucket map | 2h | gateway |
| **P3** | Fix duplicate CORS headers in gateway (filter vs YAML) | 1h | gateway |

### Sprint 4 — Validation & API Contracts (Week 7-8)

| Priority | Action | Effort | Services Affected |
|----------|--------|--------|-------------------|
| **P4** | Add `@Valid` + DTO validation annotations across all services | 16h | ALL |
| **P4** | Replace raw entity returns with DTOs | 16h | ALL |
| **P4** | Add page size limits (max 100) on all paginated endpoints | 4h | ALL |
| **P4** | Fix IDOR checks (verify resource belongs to requested parent) | 8h | plan, dashboard, component, version |

---

## Cross-Reference: Roadmap Phase Implementations

The following recently-implemented roadmap tasks (Phases 1-4, ~85% backend) have bugs that were introduced during implementation:

| Roadmap Task | Service | Bug Found |
|--------------|---------|-----------|
| **Task 1.1** Move Issue | issue-service | `MoveIssueService` key generation race (separate `generateIssueKey`) |
| **Task 1.2** Notification Dispatch | notification-service | Missing `@EnableAsync`, duplicate URL path, email queue race condition |
| **Task 1.4** SAML SSO | auth-service | No signature verification, tokens in URL, test-auth issues real tokens |
| **Task 2.1** Dev Info Panel | issue-service | Webhook endpoints have no signature validation |
| **Task 2.2** Release Hub | version-service | `ReleaseHubService` RestTemplate no timeouts, exception swallowing |
| **Task 2.3** Parallel Sprints | sprint-service | COMPLETED vs CLOSED status inconsistency |
| **Task 3.6** Working Days | sprint-service | Missing `@EnableScheduling` for CFD snapshots |
| **Task 3.8** Scope Tracking | sprint-service | Sprint report N+1 HTTP calls (300 requests per report) |
| **Task 4.1** Standard Reports | report-service | Data truncated at 1000, week calculation wrong |
| **Task 4.2** CFD | sprint-service | CFD snapshot scheduler never fires |
| **Task 5.1** LDAP Sync | user-service | LDAP password stored plaintext, race condition on sync status |
| **Task 5.2** Mail Handlers | notification-service | Mail password stored plaintext, processedMessageIds unbounded |
| **Task 5.3** Backup/Restore | admin-service | Restore failures silently swallowed |

---

## Methodology

- **25 services** scanned in parallel using dedicated code auditors per service
- **Focus areas:** Concurrency, distributed boundaries, data consistency, security/validation, injection
- **Ignored:** Style, naming, formatting
- **Each bug verified** with file path, line number, code snippet, failure scenario, and minimal patch
- **Cross-referenced** against the 6-Phase, 33-Task, 147-Subtask implementation roadmap
