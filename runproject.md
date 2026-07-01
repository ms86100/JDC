# JDC Platform — Startup Guide

## Prerequisites

- Docker Desktop running (check system tray icon)
- At least 8 GB RAM allocated to Docker (Settings > Resources > Memory)
- All ports listed below must be free

## Service Map

| Service | Container | Port | Health Check |
|---------|-----------|------|--------------|
| PostgreSQL | jira-postgres | 5432 | `pg_isready` |
| Auth Service | jira-auth-service | 8081 | `/actuator/health` |
| User Service | jira-user-service | 8082 | `/actuator/health` |
| Project Service | jira-project-service | 8083 | `/actuator/health` |
| Issue Service | jira-issue-service | 8084 | `/actuator/health` |
| Workflow Service | jira-workflow-service | 8085 | `/actuator/health` |
| Comment Service | jira-comment-service | 8086 | `/actuator/health` |
| Notification Service | jira-notification-service | 8087 | `/actuator/health` |
| Search Service | jira-search-service | 8088 | `/actuator/health` |
| Audit Service | jira-audit-service | 8089 | `/actuator/health` |
| Attachment Service | jira-attachment-service | 8090 | `/actuator/health` |
| Sprint Service | jira-sprint-service | 8091 | `/actuator/health` |
| Plan Service | jira-plan-service | 8092 | `/actuator/health` |
| Admin Service | jira-admin-service | 8093 | `/actuator/health` |
| Migration Service | jira-migration-service | 8094 | `/actuator/health` |
| Version Service | jira-version-service | 8096 | `/actuator/health` |
| Component Service | jira-component-service | 8097 | `/actuator/health` |
| Gateway | jira-gateway | 8080 | `/actuator/health` |
| Frontend (nginx) | jira-frontend | 3000 | HTTP 200 |
| Zipkin (tracing) | jira-zipkin | 9411 | HTTP 200 |

---

## Quick Start (All Services)

Open a terminal in the `JDC-main` folder and run:

```bash
docker compose up -d
```

This starts everything but Docker Desktop may struggle with 20 containers at once. If services crash or Docker becomes unresponsive, use the staged startup below.

---

## Staged Startup (Recommended)

Start services in dependency order, waiting for each tier to become healthy before proceeding.

### Step 1: Database
```bash
docker compose up -d postgres
```
Wait until healthy:
```bash
docker exec jira-postgres pg_isready -U jiraadmin
```

### Step 2: Auth Service (all other services depend on it)
```bash
docker compose up -d auth-service
```
Wait ~30s, then verify:
```bash
curl -s http://localhost:8081/actuator/health
```
Expected: `{"status":"UP"}`

### Step 3: Core Backend Services
```bash
docker compose up -d user-service project-service issue-service workflow-service
```
Wait ~60s for Java services to boot. Verify:
```bash
curl -s http://localhost:8082/actuator/health
curl -s http://localhost:8083/actuator/health
curl -s http://localhost:8084/actuator/health
curl -s http://localhost:8085/actuator/health
```

### Step 4: Supporting Services
```bash
docker compose up -d admin-service migration-service sprint-service comment-service
```
Wait ~60s. Verify:
```bash
curl -s http://localhost:8093/actuator/health
curl -s http://localhost:8094/actuator/health
curl -s http://localhost:8091/actuator/health
```

### Step 5: Remaining Services
```bash
docker compose up -d notification-service search-service audit-service attachment-service version-service component-service plan-service
```

### Step 6: Gateway + Frontend
```bash
docker compose up -d gateway frontend
```
Wait ~10s. Verify:
```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:3000
```
Expected: `200`

---

## Health Check Script (Run After Startup)

Paste this to check all services at once:

```bash
echo "=== Service Health Check ==="
for svc in 8081 8082 8083 8084 8085 8086 8087 8088 8089 8090 8091 8092 8093 8094 8096 8097; do
  status=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$svc/actuator/health 2>/dev/null)
  echo "Port $svc: $status"
done
echo "Gateway 8080: $(curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/actuator/health 2>/dev/null)"
echo "Frontend 3000: $(curl -s -o /dev/null -w '%{http_code}' http://localhost:3000 2>/dev/null)"
```

All should return `200`. If a service returns `000`, it's still starting or crashed — check with `docker logs <container-name>`.

---

## Load Seed Data (First Time Only)

After all services are running, load the user management test data:

```bash
docker exec -i jira-postgres psql -U jiraadmin -d jira_platform < postgres/seed-user-management.sql
```

This creates 8 test users, 4 custom groups, and realistic memberships. All test users can log in with password `password123`.

---

## Demo Login Credentials

| Username | Password | Role |
|----------|----------|------|
| ms86100 | admin123 | Admin (system default) |
| admin | password123 | Admin |
| john.smith | password123 | Developer (team-backend) |
| jane.doe | password123 | Developer (team-frontend) |
| bob.wilson | password123 | QA Lead (qa-team) |
| alice.johnson | password123 | Project Manager |
| charlie.brown | password123 | Developer (team-backend) |
| diana.prince | password123 | QA Engineer (qa-team) |
| eve.williams | password123 | Designer (team-frontend) |
| frank.miller | password123 | DevOps (team-backend) |

---

## Key Demo URLs

| Page | URL |
|------|-----|
| Login | http://localhost:3000/login |
| Dashboard | http://localhost:3000/dashboard |
| Projects | http://localhost:3000/projects |
| Boards | http://localhost:3000/boards |
| Admin — Users | http://localhost:3000/admin/users |
| Admin — Create User | http://localhost:3000/admin/users/create |
| Admin — Groups | http://localhost:3000/admin/groups |
| Admin — Roles | http://localhost:3000/admin/roles |
| Admin — Permissions | http://localhost:3000/admin/permissions |
| Admin — Custom Fields | http://localhost:3000/admin/custom-fields |
| Swagger (Migration) | http://localhost:8094/swagger-ui.html |
| Swagger (User) | http://localhost:8082/swagger-ui.html |

---

## Troubleshooting

### Container keeps restarting
```bash
docker logs <container-name> --tail 50
```
Common causes: database not ready yet, port conflict, out of memory.

### Docker Desktop unresponsive / 500 errors
Too many containers starting at once. Stop everything and use staged startup:
```bash
docker compose down
# Then follow Staged Startup above
```

### Frontend shows 502 Bad Gateway
The backend service it's trying to reach isn't running yet. Check which path failed in browser DevTools Network tab, then start that service.

### "host not found in upstream" nginx error
The frontend nginx config was fixed to use variable-based proxy_pass (deferred DNS). If you see this error, rebuild the frontend image:
```bash
docker compose build frontend
docker compose up -d frontend
```

### Java service takes >90s to start
Normal on first boot — Hibernate schema validation and Flyway migrations run. Subsequent starts are faster.

### Port already in use
```bash
# Find what's using a port (Windows)
netstat -ano | findstr :8080
# Kill it
taskkill /PID <pid> /F
```

---

## Stop Everything

```bash
docker compose down
```

To also remove volumes (database data):
```bash
docker compose down -v
```

---

## Rebuild After Code Changes

If you modify Java source code:
```bash
# Rebuild specific service (needs Maven / Docker multi-stage build)
docker compose build <service-name>
docker compose up -d <service-name>
```

If you modify frontend code:
```bash
# Rebuild React app + nginx
docker compose build frontend
docker compose up -d frontend
```

If you only modify nginx.conf or gateway YAML (no Java changes):
```bash
docker compose build frontend   # for nginx.conf changes
docker compose up -d gateway     # gateway reads YAML at runtime, just restart
```
