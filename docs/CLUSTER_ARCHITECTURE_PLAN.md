# Jira Data Center — Cluster Architecture Implementation Plan

## Overview

This document defines the roadmap for making the JDC platform cluster-ready: multiple identical nodes behind a load balancer, sharing a database and storage, with distributed caching, locking, scheduling, and event propagation.

---

## Current State Assessment

| # | Area | Status | Evidence |
|---|------|--------|----------|
| 1 | JWT Sessions | READY | `SessionCreationPolicy.STATELESS` in auth-service |
| 2 | PostgreSQL Search | READY | `tsvector` in search-service — inherently shared |
| 3 | Shared Database | READY | Single PostgreSQL, 21 schemas in `jira_platform` |
| 4 | Distributed Locking | PARTIAL | Only in `jira-migration-service` (DB + Redis backends) |
| 5 | Health Checks | PARTIAL | Actuator endpoints on all services, Docker healthchecks |
| 6 | K8s/Helm Configs | PARTIAL | Exist in `enterprise-architecture/` but app code not ready |
| 7 | Load Balancer | DONE | Gateway routes use Docker DNS service names; `container_name` removed from all app services |
| 8 | Distributed Caching | DONE | ClusterCacheManager (Caffeine L1 + Redis L2) wired in gateway + issue-service |
| 9 | Event Bus / Messaging | DONE | ClusterEventBus wired in IssueRealtimeBroadcaster + test-service WebSocket |
| 10 | Scheduler Coordination | DONE | ShedLock `@SchedulerLock` on all 23 `@Scheduled` methods across 8 services |
| 11 | Shared Storage | DONE | AttachmentService + PluginController refactored to use StorageProvider |
| 12 | Horizontal Scaling | DONE | `container_name` + fixed ports removed; `docker-compose.cluster.yml` overlay created |

---

## IMPLEMENTED: Node & Load Balancer Architecture

### What is a Node?
In our microservices architecture, each **service instance** is a node. When you run `docker compose up --scale issue-service=3`, you get 3 nodes of the issue service — each fully capable of serving requests independently.

### How Load Balancing Works

```
                    Internet
                        |
                  Load Balancer (nginx/haproxy — external)
                        |
              +---------+---------+
              |                   |
          Gateway-1           Gateway-2         <-- Docker DNS round-robin
              |                   |
              +---------+---------+
                        |
         Docker DNS round-robin per service
                        |
    +---+---+---+---+---+---+---+---+
    |       |       |       |       |
 Issue-1 Issue-2 Issue-3  Auth-1 Auth-2  ...    <-- Application Nodes
    |       |       |       |       |
    +---+---+---+---+---+---+---+---+
                        |
                  Shared State
              +----+----+----+
              |    |    |    |
           Postgres Redis MinIO Zipkin          <-- Infrastructure (singletons)
```

### Implementation Details

1. **docker-compose.yml**: All `container_name` and fixed `ports` removed from application services. Only infrastructure (postgres, redis, minio, zipkin) and user-facing services (gateway:8080, frontend:3000) retain them.

2. **Gateway routing**: All routes updated from `http://jira-issue-service:8084` to `http://issue-service:8084` (Docker Compose service names). Docker DNS automatically round-robins across all instances of a scaled service.

3. **Scaling**: `docker compose up --scale issue-service=3 --scale gateway=2`

4. **Cluster overlay**: `docker-compose.cluster.yml` pre-configures multi-node deployment:
   - Gateway: 2 replicas
   - Issue Service: 3 replicas  
   - Workflow Service: 2 replicas
   - Notification Service: 2 replicas
   - Auth Service: 2 replicas

5. **Inter-service communication**: All `*_SERVICE_URL` environment variables updated to use Docker Compose service names.

6. **Redis**: Added to all services via `REDIS_HOST: redis` environment variable for distributed caching, locking, and event bus.

---

## Phase 1: Foundation — Cluster Commons Library + Infrastructure (COMPLETE)

> **Goal**: Create shared library + add Redis/MinIO infrastructure. Zero functional change to services.

### Task 1.1: Create `jira-cluster-commons` Maven Module
- [ ] **1.1.1** Create module directory structure under `/jira-cluster-commons/`
- [ ] **1.1.2** Create `pom.xml` (library JAR, no Spring Boot plugin)
- [ ] **1.1.3** Add module to parent POM `<modules>` list
- [ ] **1.1.4** Add ShedLock, Redis, Caffeine, ArchUnit dependencies

### Task 1.2: Extract Distributed Lock Primitives
- [ ] **1.2.1** Copy `DistributedLockService` interface to `com.jira.cluster.lock`
- [ ] **1.2.2** Copy `LockHandle`, `LockInfo`, `LockAcquisitionException` to `com.jira.cluster.lock`
- [ ] **1.2.3** Create `ClusterProperties` config class in `com.jira.cluster.config`
- [ ] **1.2.4** Update `jira-migration-service` to depend on cluster-commons (and remove duplicated classes)

### Task 1.3: Add ShedLock Scheduler Coordination
- [ ] **1.3.1** Add `ShedLockAutoConfiguration` to cluster-commons
- [ ] **1.3.2** Provide JDBC-based `LockProvider` bean (auto-configured)
- [ ] **1.3.3** Include `V_shedlock__create_shedlock_table.sql` reusable migration script

### Task 1.4: Add StorageProvider Abstraction
- [ ] **1.4.1** Create `StorageProvider` interface (`store`, `retrieve`, `delete`, `exists`, `getUrl`)
- [ ] **1.4.2** Create `LocalStorageProvider` implementation
- [ ] **1.4.3** Create `S3StorageProvider` implementation (MinIO-compatible)
- [ ] **1.4.4** Create `StorageAutoConfiguration` with `@ConditionalOnProperty`

### Task 1.5: Add ClusterCacheManager
- [ ] **1.5.1** Create `ClusterCacheManager` (Caffeine L1 + Redis L2)
- [ ] **1.5.2** Create `CacheInvalidationService` (Redis pub/sub for cross-node eviction)
- [ ] **1.5.3** Create `CacheAutoConfiguration`

### Task 1.6: Add ClusterEventBus
- [ ] **1.6.1** Create `ClusterEventBus` interface
- [ ] **1.6.2** Create `RedisClusterEventBus` implementation (Redis pub/sub)
- [ ] **1.6.3** Create `LocalClusterEventBus` fallback (single-node mode)

### Task 1.7: Add ArchUnit Cluster Safety Tests
- [ ] **1.7.1** Create `ClusterSafetyArchTest` base class with 5 rules
- [ ] **1.7.2** Rule 1: `@Scheduled` must have `@SchedulerLock`
- [ ] **1.7.3** Rule 2: No mutable static collections in Spring beans
- [ ] **1.7.4** Rule 3: No direct `Files.copy/write/move` in service layer
- [ ] **1.7.5** Rule 4: No `ConcurrentHashMap` fields in `@Component/@Service`
- [ ] **1.7.6** Rule 5: No `enableSimpleBroker()` in WebSocket configs

### Task 1.8: Add Redis to docker-compose.yml
- [ ] **1.8.1** Add Redis 7 Alpine service with healthcheck
- [ ] **1.8.2** Add `REDIS_HOST`/`REDIS_PORT` environment variables to all services
- [ ] **1.8.3** Add `redis_data` volume

### Task 1.9: Add MinIO to docker-compose.yml
- [ ] **1.9.1** Add MinIO service with healthcheck
- [ ] **1.9.2** Add `MINIO_ENDPOINT`/`MINIO_ACCESS_KEY`/`MINIO_SECRET_KEY` env vars
- [ ] **1.9.3** Add `minio_data` volume

### Task 1.10: Write CLAUDE.md Cluster Safety Rules
- [ ] **1.10.1** R1: No `@Scheduled` without `@SchedulerLock`
- [ ] **1.10.2** R2: No mutable static collections for cross-request state
- [ ] **1.10.3** R3: No direct filesystem access for user data
- [ ] **1.10.4** R4: WebSocket broadcasts must use ClusterEventBus
- [ ] **1.10.5** R5: No `container_name` on application services
- [ ] **1.10.6** R6: No fixed host port mappings on application services
- [ ] **1.10.7** R7: Outbox pollers must be idempotent
- [ ] **1.10.8** R8: Cache evictions must propagate across nodes

### Task 1.11: Create Validation Tools
- [ ] **1.11.1** Create `scripts/validate-cluster-readiness.sh`
- [ ] **1.11.2** Create `docs/CLUSTER_REVIEW_CHECKLIST.md`

---

## Phase 2: Scheduler Safety — Prevent Double Execution

> **Goal**: Add `@SchedulerLock` to all `@Scheduled` methods. This is the highest-risk fix.

### Task 2.1: Notification Service (CRITICAL RISK)
- [ ] **2.1.1** Add `jira-cluster-commons` dependency to POM
- [ ] **2.1.2** Create ShedLock Flyway migration `V__shedlock.sql` in `jira_notification` schema
- [ ] **2.1.3** Add `@EnableSchedulerLock` to Spring config
- [ ] **2.1.4** Add `@SchedulerLock` to `EmailService.processQueue` — **duplicate emails to real users**
- [ ] **2.1.5** Add `@SchedulerLock` to `IncomingMailScheduler` — **duplicate mail processing**
- [ ] **2.1.6** Remove `synchronized (queueLock)` from `EmailService` (ShedLock replaces it)
- [ ] **2.1.7** Add ArchUnit test class `NotificationServiceClusterArchTest`

### Task 2.2: Issue Service (HIGH RISK)
- [ ] **2.2.1** Add `jira-cluster-commons` dependency to POM
- [ ] **2.2.2** Create ShedLock Flyway migration in `jira_issue` schema
- [ ] **2.2.3** Add `@EnableSchedulerLock` to Spring config
- [ ] **2.2.4** Add `@SchedulerLock` to `IssueEventOutboxPoller` — **duplicate event dispatch**
- [ ] **2.2.5** Add ArchUnit test class `IssueServiceClusterArchTest`

### Task 2.3: Workflow Service (HIGH RISK)
- [ ] **2.3.1** Add `jira-cluster-commons` dependency to POM
- [ ] **2.3.2** Create ShedLock Flyway migration in `jira_workflow` schema
- [ ] **2.3.3** Add `@EnableSchedulerLock` to Spring config
- [ ] **2.3.4** Add `@SchedulerLock` to `WorkflowEventOutboxProcessor` — **duplicate workflow events**
- [ ] **2.3.5** Add `@SchedulerLock` to `ScheduledScriptExecutor` — **duplicate user script execution**
- [ ] **2.3.6** Add `@SchedulerLock` to `ScriptLogCleanupJob` — harmless but fix anyway
- [ ] **2.3.7** Add ArchUnit test class

### Task 2.4: User Service (MEDIUM RISK)
- [ ] **2.4.1** Add `jira-cluster-commons` dependency to POM
- [ ] **2.4.2** Create ShedLock migration in `jira_user` schema
- [ ] **2.4.3** Add `@SchedulerLock` to `DirectorySyncScheduler` — **concurrent LDAP syncs**
- [ ] **2.4.4** Add ArchUnit test class

### Task 2.5: Sprint Service (MEDIUM RISK)
- [ ] **2.5.1** Add `jira-cluster-commons` dependency to POM
- [ ] **2.5.2** Create ShedLock migration in `jira_sprint` schema
- [ ] **2.5.3** Add `@SchedulerLock` to `CFDSnapshotScheduler` — **duplicate snapshot rows**
- [ ] **2.5.4** Add ArchUnit test class

### Task 2.6: Plan Service (MEDIUM RISK)
- [ ] **2.6.1** Add `jira-cluster-commons` dependency to POM
- [ ] **2.6.2** Create ShedLock migration in `jira_plan` schema
- [ ] **2.6.3** Add `@SchedulerLock` to `SprintSnapshotService` — **duplicate plan snapshots**
- [ ] **2.6.4** Add ArchUnit test class

### Task 2.7: Test Service (LOW RISK)
- [ ] **2.7.1** Add `jira-cluster-commons` dependency to POM
- [ ] **2.7.2** Create ShedLock migration in `jira_test` schema
- [ ] **2.7.3** Add `@SchedulerLock` to `AuditService.processArchivalPolicies`
- [ ] **2.7.4** Add ArchUnit test class

**Verification**: `docker compose up --scale issue-service=2` — check logs for "Acquired lock" messages, verify outbox events processed exactly once.

---

## Phase 3: Shared Storage — Cluster-Safe File Operations

> **Goal**: All file I/O goes through `StorageProvider`, backed by MinIO (S3-compatible).

### Task 3.1: Attachment Service Refactor
- [ ] **3.1.1** Add `jira-cluster-commons` dependency
- [ ] **3.1.2** Replace `Files.copy()` in `AttachmentService.java` with `StorageProvider.store()`
- [ ] **3.1.3** Replace `Files.deleteIfExists()` with `StorageProvider.delete()`
- [ ] **3.1.4** Replace direct file reads with `StorageProvider.retrieve()`
- [ ] **3.1.5** Update docker-compose: replace `attachment_data` volume with MinIO config
- [ ] **3.1.6** Add integration test: upload on node A, download on node B

### Task 3.2: Test Service Plugin Storage
- [ ] **3.2.1** Replace `Files.createDirectories()` + temp file writes in `PluginController.java`
- [ ] **3.2.2** Use `StorageProvider` for plugin JAR storage

**Verification**: Upload attachment via node A, download via node B through gateway.

---

## Phase 4: Distributed Caching & Rate Limiting

> **Goal**: Cache evictions propagate across nodes. Rate limits are shared.

### Task 4.1: Gateway Distributed Rate Limiting
- [ ] **4.1.1** Add `bucket4j-redis` dependency to gateway POM
- [ ] **4.1.2** Replace in-memory `ConcurrentHashMap<String, Bucket>` in `RateLimiterConfig.java` with `RedisBasedProxyManager`
- [ ] **4.1.3** Add Redis connection config to gateway `application-docker.yml`
- [ ] **4.1.4** Test rate limiting shared across 2 gateway instances

### Task 4.2: Issue Service Cache Coordination
- [ ] **4.2.1** Replace Caffeine-only `CacheConfig` with `ClusterCacheManager` from cluster-commons
- [ ] **4.2.2** Ensure `@CacheEvict` propagates via Redis pub/sub
- [ ] **4.2.3** Test: create issue on node A, evict cache on node B, verify node A cache also evicted

### Task 4.3: Gateway Cache Coordination
- [ ] **4.3.1** Replace `CacheConfig.java` with `ClusterCacheManager`
- [ ] **4.3.2** Make Redis `@Primary` when available (currently Caffeine is primary)

**Verification**: Create issue on node A, verify cache eviction propagates to node B.

---

## Phase 5: WebSocket Cluster Relay

> **Goal**: Real-time events broadcast to all clients regardless of which node they connect to.

### Task 5.1: Issue Service WebSocket
- [ ] **5.1.1** Inject `ClusterEventBus` into `IssueRealtimeBroadcaster`
- [ ] **5.1.2** Publish events to Redis pub/sub in `publish()` method
- [ ] **5.1.3** Subscribe to Redis channel and forward to local `WebSocketSession` set
- [ ] **5.1.4** Test: connect WebSocket on node A, trigger event on node B, verify client A receives it

### Task 5.2: Test Service STOMP
- [ ] **5.2.1** Replace `enableSimpleBroker("/topic")` in `WebSocketConfig.java`
- [ ] **5.2.2** Use Redis-backed STOMP relay (`StompBrokerRelayMessageHandler`)

**Verification**: Connect WebSocket on node A, trigger event on node B, verify client on A receives it.

---

## Phase 6: Docker Compose Scaling & Load Balancer

> **Goal**: Remove all scaling blockers. Validate end-to-end multi-node deployment.

### Task 6.1: Remove Scaling Blockers
- [ ] **6.1.1** Remove `container_name` from all 20 application services in `docker-compose.yml`
- [ ] **6.1.2** Remove fixed port mappings (`"8084:8084"`) from application services
- [ ] **6.1.3** Keep ports only for: gateway (8080), frontend (3000), infrastructure (postgres, redis, minio, zipkin)
- [ ] **6.1.4** Keep `container_name` only for infrastructure services

### Task 6.2: Service Discovery / Load Balancing
- [ ] **6.2.1** Docker Compose: rely on Docker DNS round-robin (works automatically once container_name removed and services scaled)
- [ ] **6.2.2** Gateway: verify routes work with Docker DNS load balancing (hardcoded service names still resolve correctly)
- [ ] **6.2.3** K8s: update K8s Services (ClusterIP) for all backend services

### Task 6.3: Create Cluster Test Profile
- [ ] **6.3.1** Create `docker-compose.cluster.yml` override for multi-node testing
- [ ] **6.3.2** Define scale targets (gateway: 2, issue-service: 3, etc.)
- [ ] **6.3.3** Full integration test: `docker compose -f docker-compose.yml -f docker-compose.cluster.yml up`

### Task 6.4: K8s Finalization
- [ ] **6.4.1** Update K8s deployments for all 20 services (currently only 4 have manifests)
- [ ] **6.4.2** Add Redis and MinIO K8s deployments
- [ ] **6.4.3** Update network policy to include Redis (6379) and MinIO (9000) ports
- [ ] **6.4.4** Configure HPA for critical services

**Verification**: `docker compose up --scale issue-service=3 --scale gateway=2` — full integration test passes.

---

## No-Regression Guardrails

### Layer 1: CLAUDE.md Rules (Claude enforced)

Every code change by Claude is checked against these 8 rules:

| Rule | Violation | Correct Pattern |
|------|-----------|-----------------|
| R1 | `@Scheduled` alone | `@Scheduled` + `@SchedulerLock` |
| R2 | `static ConcurrentHashMap` for state | Redis-backed store |
| R3 | `Files.copy()` for user data | `StorageProvider.store()` |
| R4 | `sessions.forEach(send)` for WebSocket | `ClusterEventBus.publish()` |
| R5 | `container_name: jira-my-service` | Remove `container_name` |
| R6 | `ports: "8084:8084"` | Expose only container port internally |
| R7 | Outbox processing without idempotency | Unique constraint / idempotency key |
| R8 | `@CacheEvict` with Caffeine only | `ClusterCacheManager` with Redis L2 |

### Layer 2: ArchUnit Tests (Compile-Time)

Build fails if any of these 5 rules are violated:

1. `@Scheduled` methods must also have `@SchedulerLock`
2. No mutable static `Map`/`Set`/`List` fields in `@Component`/`@Service` classes
3. No `java.nio.file.Files.copy/write/move` calls in `..service..` packages
4. No `ConcurrentHashMap` fields in Spring-managed beans
5. No `enableSimpleBroker()` in `WebSocketMessageBrokerConfigurer` implementations

### Layer 3: Docker Compose Validation Script

`scripts/validate-cluster-readiness.sh` run in CI:
- Fails if application services have `container_name`
- Fails if application services have fixed port mappings
- Warns if Redis/MinIO are missing

### Layer 4: Code Review Checklist

`docs/CLUSTER_REVIEW_CHECKLIST.md`:
- Scheduling: `@SchedulerLock` present, lock timing correct
- State: no new mutable statics, no `ConcurrentHashMap` for coordination
- Caching: `@CacheEvict` propagates, uses `ClusterCacheManager`
- Filesystem: uploads go through `StorageProvider`
- WebSocket: broadcasts use `ClusterEventBus`
- Docker: no scaling blockers introduced

---

## Architecture Diagrams

### Target Multi-Node Architecture
```
                    Internet
                        |
                 Reverse Proxy
                        |
                  Load Balancer
                        |
          +-------------+-------------+
          |             |             |
    +-----------+ +-----------+ +-----------+
    | Gateway 1 | | Gateway 2 | | Gateway 3 |
    +-----------+ +-----------+ +-----------+
          |             |             |
          +------+------+------+------+
                 |             |
    +------------+------+------+------------+
    |            |      |      |            |
  Issue-1    Issue-2  Auth-1  Auth-2     ...
    |            |      |      |
    +-----+------+------+------+
          |             |
     +----------+  +--------+
     | Redis    |  | MinIO  |
     +----------+  +--------+
          |
     +----------+
     | Postgres |
     +----------+
```

### Request Lifecycle (Clustered)
```
User Request
    |
    v
Load Balancer --> Gateway-2
    |
    v
Gateway validates JWT (stateless - any node works)
    |
    v
Docker DNS round-robin --> Issue-Service-3
    |
    v
Business Logic + DB Query
    |
    v
Cache Miss? --> Read from PostgreSQL --> Cache in Caffeine L1 + Redis L2
Cache Hit?  --> Return from Caffeine L1
    |
    v
WebSocket Update? --> ClusterEventBus (Redis pub/sub) --> All connected clients
    |
    v
Response back through Gateway
```

### No-Regression Safety Net
```
Developer / Claude writes code
    |
    v
CLAUDE.md Rules -----> Prevents writing cluster-unsafe patterns
    |
    v
ArchUnit Tests ------> Build FAILS on rule violation
    |
    v
CI Pipeline ----------> Docker Compose validation script
    |
    v
PR Review Checklist --> Human verification of cluster safety
    |
    v
Code merged (cluster-safe guaranteed)
```

---

## Progress Tracking

| Phase | Tasks | Status | Blocking |
|-------|-------|--------|----------|
| Node & Load Balancer | docker-compose + gateway | COMPLETE | — |
| Phase 1: Foundation | 11 tasks, 32 subtasks | COMPLETE | — |
| Phase 2: Scheduler Safety | 8 services, 23 methods | COMPLETE | — |
| Phase 3: Shared Storage | 2 tasks, 8 subtasks | COMPLETE | — |
| Phase 4: Caching & Rate Limiting | 3 tasks, 8 subtasks | COMPLETE | — |
| Phase 5: WebSocket Relay | 2 tasks, 6 subtasks | COMPLETE | — |

**Total: 29 tasks, 92 subtasks**
