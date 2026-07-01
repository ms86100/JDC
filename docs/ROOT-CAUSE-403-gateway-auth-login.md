# Root Cause: 403 Forbidden on Gateway /auth/login

## Date
2026-06-10

## Issue Summary
`POST http://localhost:8080/auth/login` returned **403 Forbidden** while `POST http://localhost:8080/api/auth/login` returned **200 OK**, even though both should route to the same auth service (port 8081).

## Symptoms
- Direct auth service works: `curl -X POST localhost:8081/auth/login` → 200 ✓
- Gateway /api/auth/login works: `curl -X POST localhost:8080/api/auth/login` → 200 ✓
- Gateway /auth/login fails: `curl -X POST localhost:8080/auth/login` → 403 ✗

## Root Cause

### What Happened
The running gateway JAR had a **RewritePath filter** baked into the `auth-service-noprefix` route that was NOT present in the source YAML file:

```
gatewayFilters=[[[RewritePath /auth(?<path>/?.*) = '/api/auth${path}'], order = 1]]
```

This filter rewrote the incoming path `/auth/login` → `/api/auth/login` before forwarding to the auth service.

### Why It Caused 403
The auth service (port 8081) has controllers at:
- `/auth/login`
- `/auth/register`
- `/auth/refresh`
- `/auth/me`

It does **NOT** have controllers at `/api/auth/*`. When the gateway rewrote the path to `/api/auth/login`, the request went to a non-existent endpoint, which fell through to Spring Security's `.anyRequest().authenticated()` rule and returned **403 Forbidden**.

## Timeline
1. **Previous agent** modified `jira-gateway/src/main/resources/application-local.yml` to remove the RewritePath filter from `auth-service-noprefix` route
2. **Previous agent** rebuilt the gateway (`mvn clean package -pl jira-gateway -DskipTests`)
3. **Previous agent** attempted to restart the gateway, but the restart command failed silently because port 8080 was already in use by the **old** gateway process
4. The **old** gateway continued running with the stale JAR that still had the RewritePath filter
5. **This agent** discovered the old process was still running, killed it, and started a fresh gateway

## How to Identify This Issue

### Check 1: Is the Gateway Using the Right Configuration?
```bash
# See what route matched for /auth/login
tail -5 /home/ubuntu/workspace/JDC/logs/gateway.log | grep -E "(Route matched|auth-service|gatewayFilters)"
```

If you see `gatewayFilters=[[[RewritePath /auth(?<path>/?.*) = '/api/auth${path}']]`, the JAR has the stale filter.

### Check 2: Is There a Stale Gateway Process?
```bash
# Check what's listening on port 8080
ss -tlpn | grep :8080

# If it shows java process from earlier today, it's stale
# Example output: *:8080 users:(("java",pid=9271,fd=20))
# PID 9271 started at 10:48 means old
```

### Check 3: Compare YAML in JAR vs Source
```bash
# Source YAML (should have NO filters on auth-service-noprefix)
grep -A5 "auth-service-noprefix" jira-gateway/src/main/resources/application-local.yml

# Built YAML in JAR
jar -tf jira-gateway/target/jira-gateway-1.0.0.jar | grep application-local.yml
jar -xf jira-gateway/target/jira-gateway-1.0.0.jar BOOT-INF/classes/application-local.yml -C /tmp/
cat /tmp/BOOT-INF/classes/application-local.yml | grep -A5 "auth-service-noprefix"
```

## How to Fix

### Step 1: Kill Any Stale Gateway Processes
```bash
# Find the gateway process
ps -ef | grep -E "java.*jira-gateway" | grep -v grep

# Kill ALL of them (parent bash and java)
kill -9 <PID>  # kill parent bash PID and java PID
# OR use pkill
pkill -9 -f "jira-gateway"
```

### Step 2: Verify Port 8080 is Free
```bash
ss -tlpn | grep :8080
# Should show nothing or time wait
```

### Step 3: Rebuild Gateway
```bash
mvn clean package -pl jira-gateway -DskipTests
```

### Step 4: Start Gateway from /tmp (Required for Classpath)
```bash
cd /tmp && nohup java -Xmx256m -jar /home/ubuntu/workspace/JDC/jira-gateway/target/jira-gateway-1.0.0.jar --spring.profiles.active=local > /home/ubuntu/workspace/JDC/logs/gateway.log 2>&1 &
echo $! > /home/ubuntu/workspace/JDC/platform-runtime/gateway.pid
```

### Step 5: Wait for Gateway to Start
```bash
sleep 25
curl -s http://localhost:8080/actuator/health
# Should return: {"status":"UP"}
```

### Step 6: Test Both Login Endpoints
```bash
curl -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" -d '{"username":"testuser","password":"Test1234!"}'
# Should return 200 with tokens

curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"username":"testuser","password":"Test1234!"}'
# Should also return 200 with tokens
```

### Step 7: Verify No RewritePath in Route
```bash
# Make a request and check logs
curl -s -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" -d '{"username":"testuser","password":"Test1234!"}' > /dev/null
sleep 1
tail -3 /home/ubuntu/workspace/JDC/logs/gateway.log | grep -E "http.uri.*auth/login"
# Should show: http.uri='http://localhost:8080/auth/login' (NOT /api/auth/login)
```

## Prevention
- Always verify the gateway restarts successfully after a rebuild
- Always check `ss -tlpn | grep :8080` before starting a new gateway
- Always test `/auth/login` AND `/api/auth/login` after a gateway restart
- Update the PID file after every successful restart

## Related Files
- `jira-gateway/src/main/resources/application-local.yml` - Gateway config
- `jira-gateway/target/jira-gateway-1.0.0.jar` - Built JAR
- `/home/ubuntu/workspace/JDC/platform-runtime/gateway.pid` - Process ID
- `/home/ubuntu/workspace/JDC/logs/gateway.log` - Gateway logs

---

# Additional Root Cause: 500 Errors on Issue, Sprint, Migration Services

## Date
2026-06-10

## Issue Summary
Multiple services returned 500 errors when accessing endpoints like `/api/issues/priorities`, `/api/issues/types`, `/api/issues/statuses`, `/api/sprints`, `/api/migration/jobs`. Browser console showed:
```
GET http://34.235.170.193:3000/issues/priorities 500 (Internal Server Error)
GET http://34.235.170.193:3000/issues/types 500 (Internal Server Error)
```

## Root Cause

### What Happened
Three services (`jira-issue-service`, `jira-sprint-service`, `jira-migration-service`) all required database tables that didn't exist in PostgreSQL:

- `jira_issue.issue_priorities` — **MISSING**
- `jira_issue.issue_types` — **MISSING**
- `jira_issue.issue_statuses` — **MISSING**
- `jira_sprint.sprints` — **MISSING**
- `jira_sprint.agile_boards` — **MISSING**
- `jira_migration.cluster_nodes` — **EXISTS but missing columns** (`state`, `metadata`, `version`, `max_jobs`, `current_jobs`)

### Why Tables Were Missing
The base `application.yml` for these services has:
- `spring.flyway.enabled: false` (default)
- `spring.jpa.hibernate.ddl-auto: none` (default)
- The `application-local.yml` files were minimal and didn't override these

This meant:
1. **Flyway migrations NEVER ran** to create tables
2. **Hibernate never auto-created** them either
3. Services started successfully but failed on any data access

### Why Migration Service Had a Different Error
Migration service was the only one with Flyway ENABLED in its base config, but with `ddl-auto: validate`. The Flyway migrations created partial tables, but didn't include all columns the entities expected (e.g., `cluster_nodes` was missing `max_jobs`, `current_jobs`). Schema validation then failed at startup.

## Files Modified
- `jira-issue-service/src/main/resources/application-local.yml` — Enabled ddl-auto:update, disabled Flyway, added init SQL
- `jira-issue-service/src/main/resources/db/init-local.sql` — NEW: ALTER TABLE for missing columns
- `jira-sprint-service/src/main/resources/application-local.yml` — Same as above
- `jira-sprint-service/src/main/resources/db/init-local.sql` — NEW: CREATE SCHEMA + ALTER TABLE
- `jira-migration-service/src/main/resources/application-local.yml` — Enabled ddl-auto:update, disabled Flyway
- `jira-migration-service/src/main/java/com/jira/migration/config/FlywayStatusLogger.java` — Made bean conditional on Flyway presence

## How to Identify This Issue

### Check 1: Service Logs
```bash
# Look for these in service logs:
grep "does not exist" /home/ubuntu/workspace/JDC/logs/issue-service.log
# Shows: relation "jira_issue.issue_priorities" does not exist
```

### Check 2: Test Endpoints
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"Test1234!"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

curl -w "HTTP %{http_code}\n" -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/issues/priorities
# If 500 with "relation does not exist" → tables missing
```

## How to Fix

### Step 1: Update application-local.yml
For each affected service (issue, sprint, migration), add to `application-local.yml`:
```yaml
spring:
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

### Step 2: Add init-local.sql
Create `src/main/resources/db/init-local.sql` with `CREATE SCHEMA IF NOT EXISTS` and `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` for any columns that entities expect but the DB doesn't have.

### Step 3: Rebuild the Service
```bash
mvn clean package -pl jira-<service-name> -DskipTests
```

### Step 4: Restart the Service
```bash
# Find and kill the old process
ps -ef | grep "jira-<service-name>" | grep -v grep
kill -9 <PID>

# Start the new one (must run from /tmp due to classpath quirk)
cd /tmp && nohup java -Xmx256m -jar \
  /home/ubuntu/workspace/JDC/jira-<service-name>/target/jira-<service-name>-1.0.0.jar \
  --spring.profiles.active=local \
  > /home/ubuntu/workspace/JDC/logs/<service>.log 2>&1 &
```

### Step 5: Verify
```bash
# Wait for startup
sleep 30-50

# Check port
ss -tlpn | grep <service-port>

# Test endpoints
curl -w "HTTP %{http_code}\n" -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/<endpoint>
```

## Prevention
- When adding new entities, ensure the database has the corresponding schema/tables
- For local dev, prefer `ddl-auto: update` over `none` to auto-create from entities
- For production, ALWAYS use Flyway migrations to maintain schema
- Use `init-local.sql` for column additions that Hibernate misses (it can't add all missing columns)
- Test endpoint health after every service restart

## Services Fixed
1. **jira-issue-service** (port 8084) — `/api/issues/priorities`, `/api/issues/types`, `/api/issues/statuses`, `/api/issues`
2. **jira-sprint-service** (port 8091) — `/api/sprints`, `/api/boards/project/{id}`
3. **jira-migration-service** (port 8094) — `/api/migration/jobs`, `/api/migration/fields`, `/api/custom-fields`

## Known Remaining Issues (Not Fixed)
- **Admin service (port 8093) is not running** — `/api/admin/issues/resolutions` returns 500 because there's no service on 8093. The gateway has a route for `/api/admin/**` → 8093 but the service isn't started. To fix: start the admin service.
- **`/api/admin/issues/issue-types` returns 500** — issue service has the endpoint but the column `is_subtask` is missing for new rows. Already partially fixed by init-local.sql but the seeding data uses old column names.
- **No `psql` client installed** — To drop schemas or run raw SQL, use the gateway's auth service or write a one-time Java migration script.