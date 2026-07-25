# Cluster Safety Code Review Checklist

Use this checklist for every PR that touches backend services.

## Scheduling
- [ ] Any new `@Scheduled` method has a corresponding `@SchedulerLock`
- [ ] `lockAtMostFor` is set to a value slightly less than the schedule interval
- [ ] `lockAtLeastFor` prevents rapid re-execution during failover
- [ ] ShedLock `shedlock` table exists in the service's schema (Flyway migration)

## State Management
- [ ] No new `static` mutable fields for request-scoped state
- [ ] No `ConcurrentHashMap` used for cross-request coordination
- [ ] Counters/metrics use Micrometer (already distributed) not custom fields
- [ ] Rate limiters use Redis-backed implementation

## Caching
- [ ] `@Cacheable` uses `ClusterCacheManager`, not bare Caffeine
- [ ] `@CacheEvict` propagates to all nodes via Redis pub/sub
- [ ] Cache keys are deterministic and collision-free across services

## Filesystem
- [ ] No `Files.copy/write/move/create` for user data in production code
- [ ] Uploads go through `StorageProvider` from `jira-cluster-commons`
- [ ] Temporary files are cleaned up within the same request

## WebSocket / Real-time
- [ ] WebSocket broadcasts use `ClusterEventBus`
- [ ] No `CopyOnWriteArraySet<WebSocketSession>` without Redis relay
- [ ] STOMP configs use Redis-backed broker relay, not `enableSimpleBroker`

## Docker / Deployment
- [ ] No `container_name` on application services in docker-compose.yml
- [ ] No fixed port mappings on application services
- [ ] Environment variables use `${VAR:default}` pattern
- [ ] New services added to K8s manifests if applicable

## Database
- [ ] New schemas added to `init-schemas.sql`
- [ ] ShedLock table created in service's Flyway migration if service has `@Scheduled` tasks
- [ ] No `SELECT ... FOR UPDATE` without timeout consideration
- [ ] Writes are idempotent where possible (upsert patterns)

## Dependencies
- [ ] Service depends on `jira-cluster-commons` if it uses scheduling, caching, storage, or event bus
- [ ] No direct Caffeine/Redis cache config — use cluster-commons auto-configuration
