# Frontend Issue Creation Error - Root Cause Analysis

## Date
2026-06-10

## Issue Summary
When attempting to create an issue in the frontend, users encounter multiple errors:

### Primary Errors:
1. **Project not found**: `{"timestamp":"2026-06-10T13:18:49.54798776","status":404,"error":"Not Found","message":"Project not found with id: '77585282-64a2-4c8f-97b7-baea62880dba'","path":"/api/issues","service":"jira-issue-service"}`
2. **Missing Version/Component endpoints**: `404 Not Found` for `/versions/project/{id}` and `/components/project/{id}`
3. **500 Internal Server Error**: `/api/issues/types` and `/api/issues/priorities` work but `/api/users/profiles` returns 500
4. **WebSocket connection failed**: Stomp WebSocket to `ws://34.235.170.193:3000/ws/issues` failed

## Root Cause Analysis

### Component Architecture
The system uses a **microservices architecture** where:
- **Gateway** (Port 8080): Routes all HTTP requests to appropriate services
- **Frontend** (Port 3000): Single Page App that calls backend via proxy
- **Multiple Services**: Each service handles specific functionality

### 1. Missing Version/Component Services (404 Errors)

**Root Cause**: The gateway routes `/api/versions/**` to `http://localhost:8096` and `/api/components/**` to `http://localhost:8097`, but:

- `jira-version-service` and `jira-component-service` don't exist as runnable Java services
- These services only exist as Docker containers (Dockerfile in their directories)
- In local development, these ports (8096, 8097) have no running services
- The frontend calls `/versions/project/{id}` and `/components/project/{id}` (no /api prefix)
- The gateway proxy config handles `/versions` → `/api/versions` → port 8096 (which doesn't exist)

**Service Mapping Problem**:
```
Frontend Calls               Gateway Routes          Target Service
---------------------------------------------------------------------
/versions/project/{id}       → /api/versions/**      → Port 8096 (MISSING)
/components/project/{id}     → /api/components/**    → Port 8097 (MISSING)
```

### 2. Project Not Found Error (404)

**Root Cause**: The `jira-issue-service` has incorrect project service URL configuration.

**Configuration**: In `application.yml`:
```yaml
project:
  service:
    url: ${PROJECT_SERVICE_URL:http://${DB_HOST:-postgres}:${DB_PORT:-5432}}
```

**Problem**: The default `${DB_HOST:-postgres}:${DB_PORT:-5432}` becomes `http://postgres:5432` when no `PROJECT_SERVICE_URL` is set. The project service runs on port 8083, not the database port.

**Expected URL**: `http://localhost:8083/api/projects/{id}`
**Actual URL used**: `http://postgres:5432/api/projects/{id}` → Connection timeout

### 3. User Service 500 Errors

**Root Cause**: The `jira-user-service` tries to access tables in `jira_user` schema, but:

- `jpa.hibernate.ddl-auto: update` can't create schemas
- `spring.sql.init.mode: always` with `schema-locations` only runs AFTER Hibernate initialization
- The `jira_user` schema doesn't exist, so Hibernate fails during startup

**Missing Configuration**: The user service local profile doesn't include:
```yaml
spring:
  sql:
    init:
      mode: always
      continue-on-error: true
      schema-locations: classpath:db/init-local.sql
```

### 4. Frontend Routing Architecture

**Proxy Configuration**: The frontend uses a proxy in `vite.config.ts`:

```typescript
proxy: {
  '/versions': {
    target: 'http://localhost:8080',
    changeOrigin: true,
  },
  '/api/versions': {
    target: 'http://localhost:8080', 
    changeOrigin: true,
  },
}
```

**API Client**: `axiosClient.ts` has `baseURL: ''`, so all requests are relative paths.

**Issue Creation Flow**:
1. Frontend calls `/issues` (no `/api` prefix)
2. Proxy forwards to `http://localhost:8080/issues`
3. Gateway route `issues-noprefix` rewrites to `/api/issues` 
4. Issue service receives POST `/api/issues`
5. Issue service calls project service to verify project exists
6. Project service returns 404 because wrong URL

### 5. Missing Services Architecture

**Expected Microservices**:
```
├── Gateway ──┐
├── Project Service
├── Issue Service ────┐
├── User Service     │
├── Version Service ──┤
├── Component Service┤
└── Admin Service
```

**Actual Services Running**:
```
├── Gateway ───┐
├── Project Service
├── Issue Service ─┐
├── User Service   │
├── Sprint Service │
├── Migration Service
└── Missing: Version/Component/Admin Services
```

## Recommended Solutions

### Short-term Fix (Local Development)

#### Fix Project Service URL
Add to `jira-issue-service/src/main/resources/application-local.yml`:
```yaml
jira:
  project:
    service:
      url: http://localhost:8083
```

#### Fix User Service Schema
Update `jira-user-service/src/main/resources/application-local.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/jira_platform
    username: jiraadmin
    password: UNIpay@123
  jpa:
    hibernate:
      ddl-auto: update
  flyway:
    enabled: false
  sql:
    init:
      mode: always
      continue-on-error: true
      schema-locations: classpath:db/init-local.sql
```

#### Rebuild Routes to Issue Service
Update `jira-gateway/src/main/resources/application-local.yml`:

```yaml
# Change from:
#   id: version-service
#   uri: http://localhost:8096
#   predicates: Path=/api/versions/**
# 
# To:
- id: version-service
  uri: http://localhost:8084
  predicates:
    - Path=/api/versions/**
    filters:
      - StripPrefix=1

# Change from:
#   id: component-service  
#   uri: http://localhost:8097
#   predicates: Path=/api/components/**
#
# To:
- id: component-service
  uri: http://localhost:8084
  predicates:
    - Path=/api/components/**
    filters:
      - StripPrefix=1
```

#### Add Missing SQL
Create `jira-user-service/src/main/resources/db/init-local.sql`:
```sql
CREATE SCHEMA IF NOT EXISTS jira_user;
```

### Long-term Fix (Production-Ready)

#### Option A: Extract Version/Component Services
1. Build and run `jira-version-service` and `jira-component-service` Docker containers
2. Update gateway routes to point to correct ports
3. Create dedicated APIs for version/management operations

#### Option B: Consolidate Issue Service
1. Keep version/component operations in issue service
2. Remove gateway routes to version/component services
3. Update frontend to use `/api/versions` and `/api/components` paths

#### Option C: Hybrid Approach
1. Create lightweight version/component controllers in issue service
2. Keep gateway routing intact but point to issue service
3. Remove unused service dependencies

## Tasks Implementation Plan

### Task 1: Fix Immediate Project Not Found Error
- [ ] Update `application-local.yml` in issue service
- [ ] Rebuild and restart issue service
- [ ] Verify project creation works

### Task 2: Fix Version/Component 404 Errors
- [ ] Update gateway routes to point to issue service
- [ ] Rebuild and restart gateway
- [ ] Verify version/component endpoints work

### Task 3: Fix User Service 500 Errors
- [ ] Add missing schema configuration
- [ ] Rebuild and restart user service
- [ ] Verify user profiles work

### Task 4: Verify All Services Work
- [ ] Test issue creation end-to-end
- [ ] Test version/component loading
- [ ] Test user profiles and permissions

### Task 5: Production Deployment Strategy
- [ ] Choose long-term fix approach
- [ ] Build Docker images for missing services
- [ ] Update production gateway configuration

## Testing Checklist

- [ ] Can create issues in project
- [ ] Project selection dropdown loads
- [ ] Issue type/priority dropdowns load
- [ ] Version/Component dropdowns load
- [ ] User profiles load correctly
- [ ] WebSocket connections establish
- [ ] All services respond with 200 status codes

## Impact Analysis

**User Impact**: Issue creation completely broken for all users
**Business Impact**: Cannot track bugs/development work
**Technical Debt**: Missing microservices architecture not properly tested in local dev
**Risk**: Local dev doesn't match production architecture