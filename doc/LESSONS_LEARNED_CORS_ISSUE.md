# CORS Multiple Origin Values Error - Root Cause Analysis & Lessons Learned

**Date:** 2026-05-28
**Issue:** Login blocked by CORS policy - "Access-Control-Allow-Origin header contains multiple values"
**Severity:** Critical (Login broken)
**Duration:** ~2 hours

---

## Problem Description

When users attempted to login at `http://localhost:3000`, the browser showed:
```
Access to XMLHttpRequest at 'http://localhost:8080/api/auth/login'
from origin 'http://localhost:3000' has been blocked by CORS policy:
The 'Access-Control-Allow-Origin' header contains multiple values
'*, http://localhost:3000', but only one is allowed.
```

The actual HTTP response showed TWO `Access-Control-Allow-Origin` headers:
```
Access-Control-Allow-Origin: *
Access-Control-Allow-Origin: http://localhost:3000
```

---

## Root Cause

**Two separate components were both adding CORS headers to responses.**

### Component 1: Gateway (`jira-gateway`)
**File:** `jira-gateway/src/main/resources/application-docker.yml`
```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins:
              - "*"
```

This adds `Access-Control-Allow-Origin: *` to ALL responses passing through the gateway.

### Component 2: Auth Service (`jira-auth-service`)
**File:** `jira-auth-service/src/main/java/com/jira/auth/config/SecurityConfig.java`
```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .cors(cors -> cors.configurationSource(request -> {
            var config = new org.springframework.web.cors.CorsConfiguration();
            config.setAllowedOriginPatterns(List.of("*"));  // This also sets Access-Control-Allow-Origin
            // ...
        }))
```

The auth service's Spring Security CORS configuration was ALSO adding CORS headers to responses.

### The Flow

When a browser request to `/api/auth/login` goes through:
1. Gateway receives request, adds `Access-Control-Allow-Origin: *`
2. Gateway forwards to auth-service (with StripPrefix=1)
3. Auth service processes the request
4. Auth service ALSO adds its own CORS header: `Access-Control-Allow-Origin: http://localhost:3000`
5. Response returns to browser with TWO different `Access-Control-Allow-Origin` values
6. Browser blocks the request because multiple values for the same header is not allowed

---

## Why This Was Hard to Diagnose

### 1. Error Message Confusion
The error says "multiple values" but doesn't specify WHERE in the chain they come from. You have to trace through multiple services.

### 2. Conflicting Previous Fix Attempts
The codebase had multiple attempted fixes:
- `CorsWebFilter.java` in gateway (later deleted - also added headers)
- Various globalcors configurations in different application-*.yml files
- Both `allowedOrigins` and `allowedOriginPatterns` used

### 3. Test Environment vs Browser
When testing with curl (without Origin header), everything worked fine:
```bash
curl http://localhost:8080/api/auth/login  # Works, no CORS error
```
The issue only appeared when browser sends `Origin: http://localhost:3000` header.

### 4. Caching Issues
Docker build cache sometimes kept old classes. Had to:
- Delete target directories
- Clear builder cache
- Force `--no-cache` builds

---

## The Fix

**Remove CORS configuration from auth-service SecurityConfig.java**

The gateway's `globalcors` is designed to handle CORS for the entire platform. Individual services should NOT add their own CORS headers.

### Files Changed

1. **Deleted:** `jira-gateway/src/main/java/com/jira/gateway/filter/CorsWebFilter.java`
   - Was manually adding CORS headers (redundant)

2. **Modified:** `jira-auth-service/src/main/java/com/jira/auth/config/SecurityConfig.java`
   - Removed `.cors(cors -> cors.configurationSource(...))` block
   - Let the gateway handle all CORS

### Verification

After fix, the response has only ONE CORS header:
```
Access-Control-Allow-Origin: *
```
And login returns a valid JWT token.

---

## Key Lessons

### 1. Only ONE place should handle CORS in a gateway architecture
In a microservices architecture with a gateway, there are two options:
- **Option A:** Gateway handles all CORS (services don't add CORS headers)
- **Option B:** Services handle CORS, gateway passes through headers

We chose Option A. Once chosen, it must be consistent.

### 2. Never use `allowedOrigins: "*"` with credentials
If you need `allowCredentials: true`, you cannot use `*` as origin. Must use specific origins or origin patterns.

### 3. Debug with verbose curl
```bash
curl -v -X POST http://localhost:8080/api/auth/login \
  -H "Origin: http://localhost:3000" \
  -H "Content-Type: application/json" \
  -d '{"username":"ms86100","password":"admin123"}'
```
Check for duplicate `Access-Control-Allow-Origin` headers.

### 4. Check all relevant files
The issue was NOT just in one file. Check:
- Gateway globalcors config
- Gateway custom filters (CorsWebFilter.java)
- Service SecurityConfig
- nginx proxy config (if used)
- Any interceptor or middleware

### 5. Build from clean when debugging
```bash
rm -rf target/
docker builder prune -f
docker compose build --no-cache <service>
```

---

## Architecture Recommendation

For future CORS handling in this platform:

1. **Gateway (`jira-gateway`)** is the single CORS authority:
   - `application.yml` globalcors handles all origins
   - No custom CorsFilter classes

2. **Backend services** should NOT configure CORS:
   - No `.cors()` in SecurityConfig
   - Let gateway pass through

3. **If service needs CORS config**, it should be minimal:
   - Only for development/testing
   - Never in production configuration

---

# AUTH-Lesson Learnt: Gateway Service Name Resolution & Nginx Configuration

**Date:** 2026-05-30
**Issue:** Login returns 502 Bad Gateway / 500 Internal Server Error / Connection Refused
**Severity:** Critical (Authentication broken)
**Duration:** ~1 hour

---

## Problem Description

Users attempting to login at `http://<host>:3000` received various errors:

1. **Initial Error (500):**
   ```json
   {
     "timestamp": "2026-05-30T16:56:05.967+00:00",
     "path": "/api/auth/login",
     "status": 500,
     "error": "Internal Server Error"
   }
   ```

2. **After Gateway Restart (502):**
   ```
   502 Bad Gateway
   nginx/1.31.1
   ```

---

## Root Cause Analysis

### Issue 1: Gateway Using Wrong Profile (localhost instead of Docker service names)

**File:** `jira-gateway/Dockerfile`

The gateway was being built with a stale JAR that was compiled with `localhost:8081` routes instead of Docker service names like `auth-service:8081`.

**Investigation steps:**
```bash
# Check what routes gateway is using
docker logs jira-gateway | grep "RouteDefinition"

# Found: uri=http://localhost:8081 (WRONG - should be auth-service)
# Expected: uri=http://auth-service:8081
```

**Why it happened:**
- The gateway Dockerfile uses `COPY target/*.jar app.jar` from the gateway subdirectory
- When `docker compose build gateway` was run from the root directory, the context was wrong
- The build failed, but an old/stale image was being used

**Fix:**
```bash
# Build from the correct directory (jira-gateway subdirectory)
cd /home/ubuntu/workspace/JDC/jira-gateway
docker build -t jdc-gateway:latest .

# Or rebuild with proper context
docker compose build gateway  # Must run from correct context
```

### Issue 2: Nginx Hardcoded IP Address

**File:** `jira-frontend/nginx.conf`

```nginx
location /api/ {
    proxy_pass http://172.18.0.9:8080/api/;  # HARDCODED IP - BAD!
```

The nginx config had the gateway IP `172.18.0.9` hardcoded. When containers restart, Docker assigns NEW IP addresses:

```
Before restart:
  jira-gateway: 172.18.0.9

After restart:
  jira-gateway: 172.20.0.4  (IP changed!)
```

**Investigation steps:**
```bash
# Check current IP assignments
docker network inspect jdc_jira-network --format='{{range .Containers}}{{.Name}}: {{.IPv4Address}}{{println}}{{end}}'

# Result:
# jira-gateway: 172.20.0.4/16
# jira-auth-service: 172.20.0.3/16
# jira-frontend: 172.20.0.7/16
```

**The Flow:**
```
Browser -> Frontend (nginx) -> [hardcoded 172.18.0.9] -> OLD gateway IP (BROKEN)
                                        ↓
                              Should be: jira-gateway:8080
```

---

## The Fixes Applied

### Fix 1: Rebuild Gateway with Correct Configuration

```bash
# Step 1: Build the JAR with Maven
cd /home/ubuntu/workspace/JDC
mvn clean package -pl jira-gateway -am -DskipTests -q

# Step 2: Build Docker image with correct context
cd /home/ubuntu/workspace/JDC/jira-gateway
docker build -t jdc-gateway:latest .

# Step 3: Start container with docker profile
docker run -d \
  --name jira-gateway \
  --network jdc_jira-network \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=docker \
  jdc-gateway:latest
```

### Fix 2: Change Nginx to Use Docker Service Names

**Before (BAD - hardcoded IP):**
```nginx
location /api/ {
    proxy_pass http://172.18.0.9:8080/api/;
    ...
}
```

**After (GOOD - uses Docker DNS):**
```nginx
location /api/ {
    proxy_pass http://jira-gateway:8080/api/;
    ...
}
```

**Why service names work:**
- Docker DNS automatically resolves `jira-gateway` to the correct container IP
- No more hardcoded IPs that change on restart
- Consistent naming across all environments

### Fix 3: Verify All Containers on Same Network

```bash
# Check all containers are on the same Docker network
docker network inspect jdc_jira-network --format='{{range .Containers}}{{.Name}}: {{.IPv4Address}}{{println}}{{end}}'

# All services must be on jdc_jira-network for DNS resolution to work
```

---

## Key Lessons

### 1. NEVER Hardcode IP Addresses in Configuration Files

**Anti-pattern:**
```nginx
proxy_pass http://172.18.0.9:8080/api/;  # BAD - will break on restart
```

**Correct pattern:**
```nginx
proxy_pass http://jira-gateway:8080/api/;  # GOOD - uses Docker DNS
```

**Why:**
- Docker containers get new IPs on restart
- Service names are resolved by Docker's internal DNS
- Environment variables like `${GATEWAY_HOST:jira-gateway}` add flexibility

### 2. Verify Gateway Profile Before Debugging Downstream Services

When authentication fails:
1. First check: Is the gateway receiving the request?
2. Second check: Is the gateway using the correct profile (docker vs local)?
3. Third check: Are the route URIs correct (service names vs localhost)?

```bash
# Check what profile and routes gateway is using
docker logs jira-gateway | grep "RouteDefinition" | head -5

# Good (docker profile):
uri=http://auth-service:8081  ✓

# Bad (local profile):
uri=http://localhost:8081  ✗
```

### 3. Ensure Docker Build Context is Correct

When running `docker compose build` from the project root:
- The build context determines where `COPY` commands look for files
- Each service's Dockerfile should be self-contained OR the build context should match the expected paths

**Check the build context in docker-compose.yml:**
```yaml
gateway:
  build:
    context: ./jira-gateway  # Correct - looks for Dockerfile here
    dockerfile: Dockerfile
```

### 4. Test the Full Stack, Not Just Components

- `curl localhost:8081/auth/login` - tests auth service directly (worked)
- `curl localhost:8080/api/auth/login` - tests gateway routing (failed with old config)
- `curl localhost:3000/api/auth/login` - tests full stack including nginx (failed)

Always test the path the browser actually takes.

---

## Commands for Verification

```bash
# 1. Verify all containers are running
docker ps

# 2. Check containers are on the same network
docker network inspect jdc_jira-network --format='{{range .Containers}}{{.Name}} {{end}}'

# 3. Check gateway is using correct service names (not localhost)
docker logs jira-gateway 2>&1 | grep "uri=http://"

# 4. Test from frontend to gateway
curl -v http://localhost:3000/api/auth/login

# 5. Test gateway to auth service
curl -v http://localhost:8080/api/auth/login

# 6. Test auth service directly
curl -v http://localhost:8081/auth/login

# 7. Check nginx config is correct
docker exec jira-frontend cat /etc/nginx/conf.d/default.conf | grep proxy_pass

# 8. Reload nginx after config changes
docker exec jira-frontend nginx -s reload
```

---

## Service Name Reference

Use these Docker service names for inter-container communication:

| Service | Internal URL | Container Name |
|---------|-------------|---------------|
| Gateway | `http://jira-gateway:8080` | jira-gateway |
| Auth Service | `http://jira-auth-service:8081` | jira-auth-service |
| Project Service | `http://jira-project-service:8083` | jira-project-service |
| Migration Service | `http://jira-migration-service:8094` | jira-migration-service |
| PostgreSQL | `postgres:5432` | jira-postgres |

---

## Prevention

1. **Add IP hardcoding to code review checklist** - Any hardcoded IP in config files is a red flag
2. **Use environment variables** for service URLs when possible
3. **Document service names** in a shared location
4. **Test after restart** - Always verify services work after `docker compose down/up`

---

## Related Files Changed

1. **Modified:** `jira-frontend/nginx.conf`
   - Changed `proxy_pass http://172.18.0.9:8080` to `proxy_pass http://jira-gateway:8080`

2. **Modified:** `jira-project-service/Dockerfile` and `jira-migration-service/Dockerfile`
   - Added `apk add --no-cache curl` for health checks

3. **Modified:** `jira-frontend/Dockerfile`
   - Fixed incorrect COPY path from `jira-frontend/nginx.conf` to `nginx.conf`

---

## Original Error Signs

Watch for these symptoms:
- `Connection refused: localhost/127.0.0.1:8081` in gateway logs
- Nginx `502 Bad Gateway`
- Services healthy individually but fail when accessed through gateway
- Login works via curl to port 8081 but fails via browser on port 3000

---

# PROJECT-Template & API Routing Issues - Root Cause Analysis

**Date:** 2026-05-30
**Issue:** Create project fails with 502/404/500 errors, templates don't load
**Severity:** Critical (Core functionality broken)
**Duration:** ~30 minutes

---

## Problem Description

When attempting to create a project, the following errors appear in browser console:

```
Failed to load resource: the server responded with a status of 502 (Bad Gateway)
api/api/projects:1 Failed to load resource: the server responded with a status of 404 (Not Found)
api/plans:1 Failed to load resource: the server responded with a status of 500 (Internal Server Error)
api/issues:1 Failed to load resource: the server responded with a status of 500 (Internal Server Error)
api/api/templates/catalog:1 Failed to load resource: the server responded with a status of 404 (Not Found)
```

---

## Root Cause Analysis

### Issue 1: Missing Docker Containers (Primary Cause)

**Not all services are running.** Only 6 containers exist:

```
Running containers:
- jira-auth-service
- jira-frontend
- jira-gateway
- jira-migration-service
- jira-postgres
- jira-project-service

MISSING containers (required by gateway routes):
- jira-plan-service       -> /api/plans returns 500
- jira-issue-service      -> /api/issues returns 500
- jira-sprint-service     -> /api/sprints returns 500
- jira-workflow-service   -> /api/workflows returns 500
- jira-user-service       -> may be missing
```

**How to diagnose:**
```bash
# List all running containers
docker ps --format '{{.Names}}\t{{.Status}}'

# Check what's on the Docker network
docker network inspect jdc_jira-network --format='{{range .Containers}}{{.Name}}: {{.IPv4Address}}{{println}}{{end}}'

# Check what services the gateway is trying to route to
docker logs jira-gateway 2>&1 | grep "uri=http://" | sed 's/.*uri=\(http:\/\/[^,]*\).*/\1/' | sort -u
```

### Issue 2: Gateway Routes Point to Wrong Hostnames

The gateway's `application-docker.yml` defines routes like:
```yaml
- id: plan-service
  uri: http://plan-service:8092      # WRONG - uses short name
  predicates:
    - Path=/api/plans/**

- id: issue-service
  uri: http://issue-service:8084     # WRONG - uses short name
  predicates:
    - Path=/api/issues/**
```

But if these services exist, they would be named `jira-plan-service`, not `plan-service`.

**Correct format should be:**
```yaml
- id: plan-service
  uri: http://jira-plan-service:8092
  predicates:
    - Path=/api/plans/**
```

### Issue 3: Double `/api/` Prefix in Browser Console

The browser console shows `api/api/projects` - this indicates the frontend is making requests to `/api/api/projects` instead of `/api/projects`.

**Root cause:** Frontend API calls hardcode `/api/` prefix:
```typescript
// projectApi.ts
apiClient.post('/api/projects', data)

// templateApi.ts
apiClient.get('/api/templates/catalog')
```

The `axiosClient.ts` has `baseURL: ''` (empty), so requests use the full path. In production with nginx, this works because:
```
Browser -> /api/projects -> nginx -> /api/projects -> gateway -> project-service:8083
```

However, if there's a vite proxy config or the path is constructed incorrectly, you get double `/api`.

**Verify the actual request URL:**
1. Open browser DevTools (F12)
2. Go to Network tab
3. Click on the failing request
4. Check "General" -> "Request URL"

### Issue 4: Services Return 404 Instead of 401 for Unauthenticated Requests

When a service isn't running or the route is wrong, the gateway returns:
- **404** - Route not found (service container doesn't exist or wrong path)
- **500** - Internal error (service exists but can't connect to it)
- **502** - Bad gateway (upstream service not reachable)

---

## The Fixes Applied

### Fix 1: Ensure All Required Services Are Running

All backend services must be running for the platform to work. Run:
```bash
# Start all services via docker compose
docker compose up -d

# Or individually start missing services
docker compose up -d plan-service issue-service sprint-service
```

### Fix 2: Verify Gateway Routes Match Container Names

Check the gateway configuration:
```bash
# View current gateway routes
curl -s http://localhost:8080/actuator/gateway/routes | jq '.[] | {id, uri}'
```

Ensure the URIs in `application-docker.yml` match the actual Docker container names:
```yaml
# application-docker.yml - CORRECT format
spring:
  cloud:
    gateway:
      routes:
        - id: plan-service
          uri: http://jira-plan-service:8092    # Use full container name
          predicates:
            - Path=/api/plans/**
```

### Fix 3: Debug the Double `/api/` Issue

If you see `api/api/projects` in the console:

1. **Check the actual request URL** in browser DevTools Network tab
2. **Check nginx config** for any incorrect rewrites:
   ```bash
   docker exec jira-frontend cat /etc/nginx/conf.d/default.conf | grep -A 5 "location /api"
   ```
3. **Check if vite proxy is interfering** (dev mode only):
   ```bash
   grep -A 5 "proxy" vite.config.ts
   ```

### Fix 4: Test Each Endpoint Individually

```bash
# Test through gateway (may return 401 if auth required)
curl -v http://localhost:8080/api/projects
curl -v http://localhost:8080/api/plans
curl -v http://localhost:8080/api/issues
curl -v http://localhost:8080/api/templates/catalog

# Test directly to service (if container is running)
curl -v http://localhost:8083/api/projects
```

---

## Key Lessons

### 1. Always Check Which Containers Are Running First

Before debugging routing, verify all required services are up:
```bash
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

### 2. Docker Container Names Must Match Gateway Route URIs

**Anti-pattern:**
```yaml
uri: http://plan-service:8092    # Container might be named jira-plan-service
```

**Correct pattern:**
```yaml
uri: http://jira-plan-service:8092
```

### 3. Understand the Request Flow

```
Browser -> nginx:3000 -> /api/projects -> gateway:8080 -> project-service:8083 -> /api/projects
                                              (StripPrefix=1)
                                              strips /api -> /projects
```

### 4. Don't Assume Services Are Running

Even if `docker ps` shows some services as "healthy", always verify:
1. The specific service you need is in the list
2. The gateway route for that service exists
3. The hostname in the gateway route matches the container name

---

## Required Containers for Basic Functionality

| Container | Port | Purpose | Gateway Route |
|-----------|------|---------|---------------|
| jira-postgres | 5432 | Database | N/A |
| jira-gateway | 8080 | API Gateway | N/A |
| jira-auth-service | 8081 | Authentication | /api/auth/** |
| jira-project-service | 8083 | Projects | /api/projects/** |
| jira-issue-service | 8084 | Issues | /api/issues/** |
| jira-plan-service | 8092 | Plans | /api/plans/** |
| jira-sprint-service | 8091 | Sprints | /api/sprints/** |
| jira-user-service | 8082 | Users | /api/users/** |
| jira-frontend | 3000:80 | Web UI | N/A |

---

## Prevention Checklist

Before reporting routing issues, verify:
- [ ] All required containers are running (`docker ps`)
- [ ] Containers are on the same Docker network (`docker network inspect jdc_jira-network`)
- [ ] Gateway routes use correct container names
- [ ] No duplicate `/api/` in request paths
- [ ] Services return expected status codes (200, 401, not 404/500/502)

---

## Commands for Troubleshooting

```bash
# 1. List all containers and their status
docker ps -a

# 2. Check containers on the network
docker network inspect jdc_jira-network --format='{{range .Containers}}{{.Name}}: {{.IPv4Address}}{{println}}{{end}}'

# 3. Check what URIs gateway is trying to route to
docker logs jira-gateway 2>&1 | grep "uri=http://" | head -10

# 4. Test each endpoint
curl -v http://localhost:8080/api/projects
curl -v http://localhost:8080/api/plans
curl -v http://localhost:8080/api/issues

# 5. Check for DNS resolution from gateway
docker exec jira-gateway getent hosts jira-project-service
docker exec jira-gateway getent hosts jira-plan-service

# 6. View nginx config
docker exec jira-frontend cat /etc/nginx/conf.d/default.conf

# 7. Reload nginx after config changes
docker exec jira-frontend nginx -s reload
```

---

## Related Files That Need Updates

1. **docker-compose.yml** - Ensure all services are defined and can start
2. **jira-gateway/src/main/resources/application-docker.yml** - Ensure route URIs match container names
3. **jira-frontend/nginx.conf** - Ensure proxy_pass uses correct service names
4. **jira-frontend/src/api/*.ts** - API paths should be consistent (with or without `/api` prefix)

---

## Commands Used for Debugging

```bash
# Check response headers
curl -s -D - -X POST http://localhost:8080/api/auth/login \
  -H "Origin: http://localhost:3000" \
  -H "Content-Type: application/json" \
  -d '{"username":"ms86100","password":"admin123"}'

# Check OPTIONS preflight
curl -v -X OPTIONS http://localhost:8080/api/auth/login \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: POST"

# Verify gateway config is active
curl -s http://localhost:8080/actuator/configprops | grep -i cors

# Check which image is running
docker ps --format "table {{.Names}}\t{{.Image}}"

# Force clean rebuild
rm -rf target/
docker builder prune -f
docker compose build --no-cache gateway
```

---

## Related Files (After Fix)

**Gateway:**
- `jira-gateway/src/main/resources/application-docker.yml` - globalcors config
- `jira-gateway/src/main/java/com/jira/gateway/filter/JwtAuthenticationFilter.java` - (no CORS)
- `jira-gateway/src/main/java/com/jira/gateway/filter/RateLimitFilter.java` - (no CORS)
- CorsWebFilter.java - DELETED

**Auth Service:**
- `jira-auth-service/src/main/java/com/jira/auth/config/SecurityConfig.java` - CORS removed

---

**Prevention:** Add CORS review to code review checklist. Any new filter or SecurityConfig change should be checked for CORS header addition.