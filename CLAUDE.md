# CLAUDE.md — Jira Platform Architecture Rules

## Build & Test
- Java 21, Spring Boot 3.4.5, Maven multi-module
- Parent POM: `pom.xml` (packaging: pom)
- Shared cluster library: `jira-cluster-commons` (library JAR — no Spring Boot plugin)
- Build: `mvn clean package -DskipTests` from root
- Test single service: `cd jira-issue-service && mvn test`
- Docker: `docker compose up --build`
- Each service owns its Flyway-managed schema (jira_issue, jira_project, etc.)

## Architecture
- Microservices: 21 services + gateway + frontend
- Database: single PostgreSQL with schema-per-service (21 schemas in `jira_platform`)
- Auth: stateless JWT (SessionCreationPolicy.STATELESS)
- Search: PostgreSQL tsvector (inherently shared)
- Gateway: Spring Cloud Gateway (path-based routing)
- Cluster library: `jira-cluster-commons` provides distributed locking, ShedLock scheduling, StorageProvider, ClusterCacheManager, ClusterEventBus

## Business Logic
- All business values (issue types, priorities, statuses, resolutions, link types) must be dynamic from admin master data CRUD — never hardcode business values

## Cluster Safety Rules (MANDATORY)

These rules exist because this application runs in a multi-node cluster.
Violating them causes data corruption, duplicate processing, or lost updates.

### R1: No @Scheduled Without @SchedulerLock
Every method annotated with `@Scheduled` MUST also have `@SchedulerLock`.
Without it, every node in the cluster will execute the task independently.
- Correct: `@Scheduled(cron = "0 0 2 * * *") @SchedulerLock(name = "myTask", lockAtMostFor = "PT30M")`
- Wrong: `@Scheduled(cron = "0 0 2 * * *")` alone
- ShedLock dependency comes from `jira-cluster-commons`

### R2: No In-Memory State for Cross-Request Coordination
Never use `static Map`, `static Set`, `ConcurrentHashMap`, or similar in-memory
collections to store state that must be consistent across requests or nodes.
- Rate limiters must use Redis (Bucket4j Redis proxy)
- Caches must use ClusterCacheManager (Caffeine L1 + Redis L2) from jira-cluster-commons
- Counters/sequences must use database or Redis atomic operations
- Exception: immutable static constants (Map.of(), List.of(), Set.of()) are fine

### R3: No Direct Filesystem Access for User Data
Never use `java.nio.file.Files.copy()`, `Files.write()`, `Files.move()` for
user-uploaded data (attachments, plugins, exports). Always use `StorageProvider`
from `jira-cluster-commons`.
- Exception: temporary files consumed within the same request
- Exception: test code (src/test/)

### R4: WebSocket Broadcasts Must Use ClusterEventBus
Never broadcast to `CopyOnWriteArraySet<WebSocketSession>` or use
`enableSimpleBroker()` without a Redis relay. Messages only reach clients
connected to the local node otherwise.
- Use `ClusterEventBus.publish()` from jira-cluster-commons for all real-time notifications

### R5: No container_name in docker-compose.yml for Application Services
Application services must NOT have `container_name` set, as this prevents
`docker compose up --scale service=N`. Only infrastructure services (postgres,
redis, minio, zipkin) may have container_name.

### R6: No Fixed Host Port Mappings for Application Services
Application services must NOT map host ports (e.g., `"8084:8084"`).
Use Docker network DNS for inter-service communication.
Only gateway (8080), frontend (3000), and infrastructure may expose host ports.

### R7: Outbox Pollers Must Be Idempotent
All outbox polling patterns must handle the case where the same event is
processed twice (at-least-once delivery). Use unique constraint checks or
idempotency keys.

### R8: Cache Evictions Must Propagate
When using `@CacheEvict`, ensure the eviction propagates to all nodes.
Use `ClusterCacheManager` from jira-cluster-commons, not bare `CaffeineCacheManager`.
