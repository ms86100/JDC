# Architecture Next Phases — Beyond Enterprise Hardening

## Why These Phases Matter

The platform now has clustering, security, performance, and resilience fundamentals. But to support **millions of users across industries** and be **sold as a product**, these additional architectural capabilities are essential. Each phase addresses a specific gap that becomes critical at scale.

---

## Phase A: Event-Driven Architecture (Async Inter-Service Communication)

### Why?
Currently all inter-service calls are synchronous REST. At 4K+ users, a slow downstream service blocks the calling thread. Circuit breakers help, but the real fix is decoupling services via async events.

### Tasks

#### A.1: Deploy Kafka as Core Message Broker
- [ ] **A.1.1** Add Kafka + Zookeeper to docker-compose.yml
- [ ] **A.1.2** Add Kafka to K8s manifests (StatefulSet + PVC)
- [ ] **A.1.3** Create shared Kafka config in cluster-commons (KafkaAutoConfiguration)
- [ ] **A.1.4** Define topic naming convention: `jira.{service}.{event-type}`

#### A.2: Convert Outbox Patterns to Kafka
- [ ] **A.2.1** Issue-service: publish issue events to `jira.issue.events` topic instead of HTTP dispatch
- [ ] **A.2.2** Workflow-service: publish workflow events to `jira.workflow.events`
- [ ] **A.2.3** Search-service: consume `jira.issue.events` for real-time index updates
- [ ] **A.2.4** Notification-service: consume events for email triggers
- [ ] **A.2.5** Audit-service: consume all topics for audit trail

#### A.3: Kafka Dead Letter Topics
- [ ] **A.3.1** Configure `DeadLetterPublishingRecoverer` for all consumers
- [ ] **A.3.2** Create DLT monitoring endpoints
- [ ] **A.3.3** Create DLT replay mechanism

---

## Phase B: CQRS for Read-Heavy Services

### Why?
Search, dashboard, and report services are read-heavy. They compete with write operations on the same database. CQRS separates read and write models, enabling optimized read replicas and materialized views.

### Tasks

#### B.1: Read-Replica Database Routing
- [ ] **B.1.1** Create `ReadOnlyRoutingDataSource` in cluster-commons (extends `AbstractRoutingDataSource`)
- [ ] **B.1.2** Route `@Transactional(readOnly = true)` to read replica
- [ ] **B.1.3** Add read-replica PostgreSQL to docker-compose.yml (streaming replication)
- [ ] **B.1.4** Wire routing in search-service, dashboard-service, report-service

#### B.2: Materialized Views for Dashboards
- [ ] **B.2.1** Create materialized views for common dashboard queries (issue counts by status, sprint burndown)
- [ ] **B.2.2** Schedule periodic refresh via ShedLock-coordinated job
- [ ] **B.2.3** Dashboard-service reads from materialized views instead of live queries

---

## Phase C: Multi-Tenancy

### Why?
To sell the platform across industries, each customer needs isolated data. Multi-tenancy enables a single deployment to serve multiple organizations securely.

### Tasks

#### C.1: Tenant-Aware Data Isolation
- [ ] **C.1.1** Add `tenant_id` column to all primary tables
- [ ] **C.1.2** Create Hibernate `TenantIdentifierResolver` (extracts tenant from JWT claims)
- [ ] **C.1.3** Create Hibernate `MultiTenantConnectionProvider` (sets `search_path` per tenant)
- [ ] **C.1.4** Add Row-Level Security (RLS) policies in PostgreSQL for defense-in-depth

#### C.2: Tenant-Aware Gateway Routing
- [ ] **C.2.1** Extract tenant ID from JWT or X-Tenant-ID header in gateway
- [ ] **C.2.2** Propagate tenant context through inter-service calls
- [ ] **C.2.3** Tenant-scoped rate limiting in gateway

#### C.3: Tenant Administration
- [ ] **C.3.1** Tenant CRUD in admin-service
- [ ] **C.3.2** Tenant-scoped configuration (custom fields, workflows, permissions)
- [ ] **C.3.3** Tenant data export/import

---

## Phase D: Database Partitioning & Archival

### Why?
With millions of issues and billions of audit log entries, single-table queries degrade. Partitioning keeps hot data fast while archiving cold data reduces storage costs.

### Tasks

#### D.1: Table Partitioning
- [ ] **D.1.1** Partition `issues` table by `project_id` (list partitioning)
- [ ] **D.1.2** Partition `audit_logs` table by `created_at` (range partitioning, monthly)
- [ ] **D.1.3** Partition `comments` table by `created_at` (range partitioning, quarterly)
- [ ] **D.1.4** Create partition management Flyway migrations

#### D.2: Data Archival Strategy
- [ ] **D.2.1** Create archive schema per service (e.g., `jira_issue_archive`)
- [ ] **D.2.2** Scheduled job to move resolved issues older than N months to archive
- [ ] **D.2.3** Archive query endpoint (search both live and archive tables)
- [ ] **D.2.4** Archive restoration endpoint

---

## Phase E: OAuth2 / OIDC Federation

### Why?
Enterprise customers use identity providers (Azure AD, Okta, Keycloak). The current JWT-based auth requires users to register locally. OAuth2/OIDC federation enables SSO with existing enterprise identity.

### Tasks

#### E.1: OAuth2 Resource Server
- [ ] **E.1.1** Add `spring-boot-starter-oauth2-resource-server` to auth-service
- [ ] **E.1.2** Configure JWT issuer validation (support multiple issuers)
- [ ] **E.1.3** Map OIDC claims to internal roles/permissions
- [ ] **E.1.4** Support both local JWT and external OIDC tokens simultaneously

#### E.2: OAuth2 Client (Frontend)
- [ ] **E.2.1** Add PKCE authorization code flow in frontend
- [ ] **E.2.2** Configure redirect URIs and token handling
- [ ] **E.2.3** Support multiple IdP selection on login page

---

## Phase F: Observability Platform

### Why?
Distributed tracing and structured logging are now in place, but without dashboards, alerts, and SLO tracking they're just raw data. An observability platform turns data into actionable insights.

### Tasks

#### F.1: Grafana Dashboards
- [ ] **F.1.1** Service health dashboard (all 22 services at a glance)
- [ ] **F.1.2** Request latency dashboard (P50/P95/P99 per service)
- [ ] **F.1.3** Database performance dashboard (pool usage, slow queries, connections)
- [ ] **F.1.4** Business metrics dashboard (issues created/hr, workflows executed/hr)

#### F.2: Alerting Rules
- [ ] **F.2.1** Prometheus alerting rules for circuit breaker open state
- [ ] **F.2.2** Alert on HikariCP pool exhaustion (>80% utilization)
- [ ] **F.2.3** Alert on error rate spike (>5% 5xx responses)
- [ ] **F.2.4** Alert on Kafka consumer lag

#### F.3: SLI/SLO Definitions
- [ ] **F.3.1** Define SLIs: availability (99.9%), latency (P99 < 500ms), error rate (<0.1%)
- [ ] **F.3.2** Implement error budget tracking
- [ ] **F.3.3** SLO dashboard in Grafana

---

## Phase G: CI/CD Pipeline & Testing Infrastructure

### Why?
No automated pipeline exists. Manual builds and deployments are error-prone. Contract testing prevents inter-service breakage. Performance testing validates capacity claims.

### Tasks

#### G.1: CI Pipeline
- [ ] **G.1.1** GitHub Actions workflow: build + test all services on PR
- [ ] **G.1.2** Docker image build and push to registry
- [ ] **G.1.3** ArchUnit cluster safety tests as CI gate
- [ ] **G.1.4** Docker Compose validation script as CI gate

#### G.2: Contract Testing
- [ ] **G.2.1** Add Spring Cloud Contract or Pact to inter-service APIs
- [ ] **G.2.2** Producer contracts for issue-service, workflow-service, project-service
- [ ] **G.2.3** Consumer contract verification in dependent services

#### G.3: Performance Testing
- [ ] **G.3.1** Gatling test suite for critical paths (issue CRUD, search, workflow transitions)
- [ ] **G.3.2** Load test: 4K concurrent users sustained for 1 hour
- [ ] **G.3.3** Stress test: find breaking point per service
- [ ] **G.3.4** Performance regression gate in CI

---

## Phase Priority Matrix

| Phase | Impact | Effort | Priority | Reason |
|-------|--------|--------|----------|--------|
| A: Event-Driven | HIGH | HIGH | P1 | Eliminates synchronous coupling — biggest scalability bottleneck |
| B: CQRS + Read Replicas | HIGH | MEDIUM | P1 | Doubles read capacity overnight |
| F: Observability Platform | HIGH | MEDIUM | P2 | Can't operate at scale without dashboards and alerts |
| G: CI/CD + Testing | HIGH | MEDIUM | P2 | Manual deployment is unsustainable |
| D: Partitioning | MEDIUM | MEDIUM | P3 | Becomes critical past 10M records |
| C: Multi-Tenancy | HIGH | HIGH | P3 | Required for SaaS/product model |
| E: OAuth2/OIDC | MEDIUM | MEDIUM | P4 | Required for enterprise sales |

---

## What's Already Done (For Reference)

| Area | Status |
|------|--------|
| Multi-node clustering (Load Balancer, Nodes, Scaling) | DONE |
| Distributed caching (Caffeine L1 + Redis L2) | DONE |
| Distributed locking (ShedLock, DB + Redis backends) | DONE |
| Shared storage (StorageProvider, MinIO/S3) | DONE |
| WebSocket cluster relay (Redis pub/sub) | DONE |
| Scheduler coordination (23 methods, 8 services) | DONE |
| Security hardening (CORS, JWT, Redis auth, fail-close) | DONE |
| Performance tuning (HikariCP, Hibernate batch, LAZY fetch) | DONE |
| Circuit breakers + retry (Resilience4j) | DONE |
| Distributed tracing (Micrometer + Zipkin) | DONE |
| Structured logging (JSON + correlation ID) | DONE |
| Idempotency framework (Redis-backed) | DONE |
| Health indicators (Redis, Storage) | DONE |
| Async error handling (ClusterAsyncExceptionHandler) | DONE |
| K8s manifests + HPA (all 17 services + Redis + MinIO) | DONE |
| ArchUnit cluster safety tests (8 services) | DONE |
| No-regression guardrails (CLAUDE.md, validation script, review checklist) | DONE |
| Architecture page (16 tabs including Cluster + Enterprise Hardening) | DONE |
