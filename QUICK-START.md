# Quick Start Guide

Complete setup guide to get the platform running from a fresh clone.

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 21 |
| Maven | 3.9+ |
| Node.js | 18+ |
| Docker & Docker Compose | Latest |

## Setup (Docker — recommended)

### 1. Clone and build

```bash
git clone <repo-url>
cd JDC-main

# Build all Java services (from root)
mvn clean package -DskipTests

# Build frontend
cd jira-frontend
npm ci
npm run build
cd ..
```

### 2. Start everything

```bash
docker compose up --build -d
```

This starts all services. Wait ~90 seconds for health checks to pass.

### 3. Apply the user-service migration (one-time manual step)

The user-service and admin-service share the `jira_admin` schema. The user-service's V2 migration (CWD tables for users/groups) must be applied manually because the admin-service's Flyway history already owns the schema version counter.

```bash
cat jira-user-service/src/main/resources/db/migration/V2__add_jira_user_management.sql \
  | docker exec -i jira-postgres psql -U jiraadmin -d jira_platform
```

Two "already exists" errors for `notification_schemes` and `permission_schemes` are expected — ignore them.

### 4. Add the missing `is_active` column (one-time)

```bash
docker exec jira-postgres psql -U jiraadmin -d jira_platform \
  -c "ALTER TABLE jira_issue.issue_link_types ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT true;"
```

### 5. Restart services to pick up schema changes

```bash
docker compose restart user-service gateway
```

### 6. Verify

Open http://localhost:3000 in your browser.

Default login: `ms86100` / `admin123`

## Services & Ports

| Service | Port | Purpose |
|---------|------|---------|
| Frontend | 3000 | React SPA (nginx) |
| Gateway | 8080 | API Gateway (Spring Cloud Gateway) |
| Auth | 8081 | Authentication, JWT |
| User | 8082 | User & group management (CWD) |
| Project | 8083 | Projects, members, schemes |
| Issue | 8084 | Issues, types, priorities, statuses |
| Workflow | 8085 | Workflow definitions, transitions |
| Comment | 8086 | Issue comments |
| Search | 8088 | Full-text search |
| Audit | 8089 | Audit logging |
| Attachment | 8090 | File attachments |
| Sprint | 8091 | Sprints, boards |
| Plan | 8092 | Plans, programs, roadmaps |
| Admin | 8093 | Admin settings, schemes, roles |
| Migration | 8094 | CSV/XML import, migration wizard |
| Version | 8096 | Project versions |
| PostgreSQL | 5432 | Shared database (`jira_platform`) |

## Database

Single PostgreSQL database `jira_platform` with per-service schemas:

```
jira_auth      — auth-service (users, roles, JWT)
jira_user      — user-service (profiles, organizations)
jira_admin     — admin-service + user-service (schemes, CWD users/groups, settings)
jira_project   — project-service (projects, members)
jira_issue     — issue-service (issues, types, priorities, statuses)
jira_workflow  — workflow-service (workflows, transitions)
jira_comment   — comment-service
jira_sprint    — sprint-service (sprints, boards)
jira_plan      — plan-service
jira_migration — migration-service (jobs, wizard sessions)
```

Credentials: `jiraadmin` / `jirapass123`

## Seed Data (created automatically)

The Docker init scripts and Flyway migrations create:

- **Auth users**: `ms86100` (admin), `testuser`
- **CWD admin user**: `admin` / `admin123`
- **Groups**: `administrators`, `software-users`, `system-administrators`
- **Issue Types**: Bug, Story, Task, Epic, Subtask, Improvement, New Feature, Question, Technical Task
- **Priorities**: Highest, High, Medium, Low, Lowest
- **Statuses**: Backlog, To Do, In Progress, In Review, Done, Open, Resolved, Closed
- **Workflows**: Scrum, Kanban, Bug, Task, Portfolio (with transitions)
- **Project Templates**: Scrum, Kanban, Bug Tracking, Task Management, Portfolio, Basic
- **Project Roles**: Administrators, Developers, Users, Viewers
- **32 Permissions**: BROWSE_PROJECTS, CREATE_ISSUES, EDIT_ISSUES, etc.
- **Default schemes**: Permission, Notification, Screen schemes

## Rebuilding After Code Changes

### Backend service (e.g., migration-service)

```bash
cd jira-migration-service
mvn package -DskipTests
cd ..
docker compose build --no-cache migration-service
docker compose up -d migration-service
docker compose restart gateway    # required — gateway caches container IPs
```

### Frontend

```bash
cd jira-frontend
npm run build
cd ..
docker compose build --no-cache frontend
docker compose up -d frontend
```

### Issue-service (has cross-module dependencies)

```bash
# Must build from root with reactor
mvn package -pl jira-issue-service -am -DskipTests
docker compose build --no-cache issue-service
docker compose up -d issue-service
docker compose restart gateway
```

## Troubleshooting

### "Cannot read properties of undefined" on admin pages
API returned non-array data. All `useQuery` hooks should wrap responses with `Array.isArray(res.data) ? res.data : []`.

### 405 Not Allowed from nginx
The frontend API call is missing the `/api/` prefix. All backend calls should go through `/api/...` which nginx proxies to the gateway.

### 401 on page refresh
SPA route (e.g., `/workflows`) is being caught by the nginx proxy regex and sent to the gateway instead of serving `index.html`. The nginx regex must require a trailing `/` after service names.

### 500 "Project not found" during CSV import
The issue-service needs `PROJECT_SERVICE_URL=http://project-service:8083` in docker-compose.yml.

### 500 "Permission denied: CREATE_ISSUES"
Add `JIRA_PERMISSIONS_FAILOPEN=true` to issue-service environment in docker-compose.yml.

### Flyway checksum mismatch
```sql
-- Connect to postgres and clear checksums
docker exec jira-postgres psql -U jiraadmin -d jira_platform \
  -c "UPDATE jira_issue.flyway_schema_history SET checksum = NULL WHERE success = true;"
```
Then restart the affected service.

### Gateway returns 500 after container restart
The gateway caches DNS/IPs. Always restart it after recreating other containers:
```bash
docker compose restart gateway
```

### CSV import: issue keys become DEMO-XX instead of SST1-XX
Ensure `ImportJobProcessor.convertRowToMap()` replaces spaces with underscores in headers. The issue-service `CreateIssueRequest` must have an `issueKey` field, and `IssueService.createIssue()` must use it when provided.
