# Enterprise Hardening Plan — 4K Concurrent Users, Millions of Records

## Context

Full security, performance, and resilience audit of 22 microservices identified **15 CRITICAL, 33 HIGH, 20 MEDIUM** issues across 3 domains. This plan addresses all findings to make the platform enterprise-grade, sellable across industries.

---

## Audit Summary

### Security (6 CRITICAL, 9 HIGH)
| # | Finding | Severity | Fix |
|---|---------|----------|-----|
| S1 | JWT_SECRET hardcoded in 7+ files | CRITICAL | Remove defaults, env-var only |
| S2 | CORS wildcard `*` in gateway | CRITICAL | Restrict to frontend domain |
| S3 | No TLS anywhere | CRITICAL | TLS termination at gateway |
| S4 | DB/MinIO passwords hardcoded | CRITICAL | Docker secrets / .env |
| S5 | Permissions fail-open in production | CRITICAL | Set FAILOPEN=false |
| S6 | Enterprise compose hardcoded passwords | CRITICAL | Parameterize |
| S7 | No JWT token revocation | HIGH | Redis blacklist with jti claim |
| S8 | No refresh token rotation | HIGH | DB-stored refresh tokens |
| S9 | No brute-force protection on login | HIGH | Per-account lockout |
| S10 | API tokens stored plaintext | HIGH | Hash API tokens |
| S11 | Redis no authentication | HIGH | --requirepass flag |
| S12 | Actuator endpoints public | HIGH | Restrict to health only |
| S13 | Migration/Workflow services permitAll | HIGH | Require authentication |
| S14 | Password policy not enforced | HIGH | Wire PasswordPolicy to registration |
| S15 | Swagger exposed in production | HIGH | Disable in prod profile |

### Database & Performance (7 CRITICAL, 17 HIGH)
| # | Finding | Severity | Fix |
|---|---------|----------|-----|
| D1 | HikariCP pool=10 (need 30-50) | CRITICAL | Increase per service tier |
| D2 | Auth/Admin/Sprint/Version no HikariCP | CRITICAL | Add explicit config |
| D3 | Unbounded findAll() in workflow-service | CRITICAL | Targeted queries |
| D4 | No Hibernate batch config | CRITICAL | jdbc.batch_size=50 |
| D5 | DryRun save() in loops | CRITICAL | Use saveAll() |
| D6 | Issue entity EAGER fetch (3 joins) | CRITICAL | Change to LAZY |
| D7 | Admin service ddl-auto: update | HIGH | Change to none |
| D8 | Missing indexes on issue priority/type | HIGH | Add @Index |
| D9 | No pagination on comment/issue endpoints | HIGH | Add Pageable |
| D10 | No @EntityGraph or @BatchSize | HIGH | Add to repos |
| D11 | No CHECK constraints | HIGH | Add in migrations |
| D12 | No read-replica config | HIGH | AbstractRoutingDataSource |
| D13 | No slow query logging | HIGH | LOG_QUERIES_SLOWER_THAN_MS |

### API Resilience (2 CRITICAL, 7 HIGH)
| # | Finding | Severity | Fix |
|---|---------|----------|-----|
| R1 | Circuit breakers in 2/22 services | CRITICAL | Add to all services |
| R2 | No distributed tracing | CRITICAL | Micrometer + Zipkin |
| R3 | No retry policies (except migration) | HIGH | Resilience4j retry |
| R4 | Shallow health checks | HIGH | Custom HealthIndicators |
| R5 | No structured logging | HIGH | logback-spring.xml + JSON |
| R6 | No graceful shutdown | HIGH | server.shutdown: graceful |
| R7 | No async error handler | HIGH | AsyncUncaughtExceptionHandler |
| R8 | No idempotency on POST endpoints | HIGH | X-Idempotency-Key + Redis |
| R9 | Unbounded CachedThreadPools | HIGH | Bounded FixedThreadPool |

---

## Implementation Batches

### Batch 1: Config-Only Hardening (DONE)
Zero regression risk — only YAML changes.
- HikariCP pool sizes: 50 (hot), 30 (medium), 20 (light)
- Graceful shutdown: `server.shutdown: graceful` + 30s timeout
- Hibernate batch: `batch_size: 50`, `order_inserts: true`, `order_updates: true`
- Slow query logging: `LOG_QUERIES_SLOWER_THAN_MS: 500`
- Stacktrace suppression: `include-stacktrace: never`
- Actuator restriction: health, info, prometheus only; `show-details: when-authorized`
- Tomcat threads: `max: 200`, `min-spare: 20`, `max-connections: 8192`

### Batch 2: Security Hardening (DONE)
- CORS: `allowedOrigins: "${CORS_ALLOWED_ORIGINS:http://localhost:3000}"`
- JWT expiration: 15 min access, 8 hour refresh
- Permissions: `FAILOPEN=false`
- Redis authentication: `--requirepass`
- Remove hardcoded JWT secret defaults from application.yml
- Admin service: `ddl-auto: none`

### Batch 3: Code-Level Performance (DONE)
- Issue/TestIssue/User entities: `EAGER -> LAZY` fetch
- Missing indexes: `idx_issue_priority`, `idx_issue_issue_type`
- Bare RestTemplate: add timeout (5s connect, 30s read)
- CachedThreadPool -> FixedThreadPool (bounded to CPU*2, max 20)

### Batch 4: Resilience (FUTURE)
Items that require larger refactoring — planned for next sprint:
- Circuit breakers across all services (Resilience4j)
- Structured logging (logback-spring.xml + JSON encoder)
- Distributed tracing (Micrometer + Zipkin/OTLP)
- Idempotency framework (X-Idempotency-Key + Redis store)
- Custom HealthIndicators per service
- AsyncUncaughtExceptionHandler

---

## Verification

| What | How |
|------|-----|
| Config changes | `mvn compile` all services — must succeed |
| HikariCP | Check `actuator/metrics/hikaricp.connections.max` |
| Graceful shutdown | `docker compose stop` — check in-flight requests complete |
| CORS | Browser DevTools → Network → check CORS headers |
| JWT | Decode token → verify 15min expiry |
| Redis auth | `redis-cli ping` fails without AUTH |
| EAGER->LAZY | Query logs show no unnecessary JOINs |
| Threads | `jstack` shows bounded thread pool sizes |
