# User Management Module — Fix Plan & Progress

## Context

The User Management module spans multiple services (user-service, admin-service, auth-service, gateway, frontend) and was partially implemented. The goal is to make all CRUD operations work end-to-end for users, groups, roles, and permissions — with relational data matching how Jira Data Center works.

---

## Architecture Overview

| Service | Port | Schema | Role |
|---------|------|--------|------|
| jira-auth-service | 8081 | `jira_auth` | JWT login/register, BCrypt passwords |
| jira-user-service | 8082 | `jira_admin` + `jira_user` | Jira DC-compatible CWD users/groups/memberships |
| jira-admin-service | 8093 | `jira_admin` | Permission schemes, project roles, notification/security schemes |
| jira-gateway | 8080 | — | Routes requests to services |
| jira-frontend | 3000 | — | React SPA, nginx reverse proxy |

---

## Problems Identified

### 1. Gateway Routing Broken (Docker profile)
- **admin-api route** had `StripPrefix=1` which stripped `/api` from `/api/admin/**`, sending `/admin/**` to admin-service. But controllers expect `/api/admin/**`.
- **admin-service route** passed `/admin/**` through as-is, but controllers expect `/api/admin/**`.
- **No route for `/user-service/**`** — frontend calls `/user-service/rest/admin/1.0/...` for CWD user/group CRUD but the gateway had no matching route.

### 2. Frontend API Path Mismatches
- Group member operations (add/remove/list) called `/api/admin/users/groups/{groupId}/members/...` which routed to admin-service, but the actual CWD group data lives in user-service.
- No `updateUser` or `updateGroup` API functions existed in the frontend hooks.

### 3. Backend CRUD Gaps
- **No user UPDATE endpoint** — `AdminUserController` had GET, POST, DELETE but no PUT.
- **No group UPDATE endpoint** — same issue.
- **`deleteUser` bug** — passed `null` as `parentId` to `deleteByParentIdAndChildIdAndMembershipType()`, generating `WHERE parent_id = NULL` which never matches (DB CASCADE handles it, but Java code was wrong).
- **No project role PUT/DELETE** — `UserManagementController` had GET and POST for project-roles but no update or delete.

### 4. Nginx Proxy Issues
- All `proxy_pass` directives used hardcoded hostnames (resolved at startup), causing nginx to crash if any backend was unavailable.
- `rewrite ... break` stopped processing subsequent `set` directives, leaving upstream variables uninitialized.
- `/admin/` routes were proxied to gateway instead of served as SPA pages.
- No `/user-service/` location for CWD admin API calls.

### 5. Insufficient Seed Data
- Only 2 users (admin, ms86100) and 3 system groups.
- No custom groups, no realistic membership patterns.
- Auth-service users not synced with user-service CWD users.

---

## Completed Work

### Step 1: Fix Gateway Routing ✅
**File:** `jira-gateway/src/main/resources/application-docker.yml`
- Removed `StripPrefix=1` from `admin-api` route so `/api/admin/**` passes through to admin-service as-is.
- Added `RewritePath` filter to `admin-service` route: `/admin/**` → `/api/admin/**`.
- Added new `user-service-admin` route: `/user-service/**` with `StripPrefix=1` → sends `/rest/admin/1.0/...` to user-service.

### Step 2: Fix Nginx & Vite Proxy ✅
**File:** `jira-frontend/nginx.conf`
- Converted ALL `proxy_pass` directives to use variables (`set $upstream ...`) so nginx resolves hostnames at request time, not startup. This prevents nginx from crashing when a backend is down.
- Fixed `set` before `rewrite` ordering (nginx's `rewrite ... break` stops processing subsequent rewrite-phase directives including `set`).
- Removed `admin` from the regex location that proxied to gateway, so `/admin/*` SPA routes serve `index.html` instead of being proxied.
- Added `/user-service/` location block to proxy CWD admin API calls to gateway.

**File:** `jira-frontend/vite.config.ts`
- Added `'/user-service': apiProxy(GATEWAY)` proxy entry for dev mode.

### Step 3: Fix Frontend API Paths & Add Hooks ✅
**File:** `jira-frontend/src/features/admin/hooks/useAdminApi.ts`
- Changed `useAddUserToGroup`, `useRemoveUserFromGroup`, `useJiraGroupMembers` to use user-service CWD endpoints (`/user-service/rest/admin/1.0/groups/{groupId}/members/...`) instead of admin-service endpoints.
- Added `updateUser` and `updateGroup` functions to `jiraUserApi`.
- Added `getGroupMembers`, `addUserToGroup`, `removeUserFromGroup` functions to `jiraUserApi`.
- Added `useUpdateJiraUser` React Query mutation hook.

### Step 4: Add Backend CRUD Endpoints ✅
**User-Service changes:**
- **New file:** `jira-user-service/src/main/java/com/jira/user/dto/UpdateUserRequest.java` — DTO with fields: email, fullName, firstName, lastName, active.
- **File:** `AdminUserController.java` — Added `PUT /users/{userId}` and `PUT /groups/{groupId}` endpoints.
- **File:** `JiraUserManagementService.java` — Added `updateUser()` and `updateGroup()` methods. Fixed `deleteUser()` bug (replaced null-parentId call with `deleteAllByChildIdAndMembershipType`).
- **File:** `CwdMembershipRepository.java` — Added `deleteAllByChildIdAndMembershipType()` JPQL query method.

**Admin-Service changes:**
- **File:** `UserManagementController.java` — Added `PUT /project-roles/{roleId}` and `DELETE /project-roles/{roleId}` endpoints.
- **File:** `UserManagementService.java` — Added `updateProjectRole()` and `deleteProjectRole()` methods. Delete blocks system/default roles (Administrators, Developers, Users).

### Step 5: Create Comprehensive Seed Data ✅
**New file:** `postgres/seed-user-management.sql`
- 8 new users: john.smith, jane.doe, bob.wilson, alice.johnson, charlie.brown, diana.prince, eve.williams, frank.miller
- 4 custom groups: team-backend, team-frontend, qa-team, project-managers
- Realistic group memberships (all users in jira-software-users, team assignments match roles)
- Application access records for all users
- Login info with realistic login counts
- Auth-service sync: inserts into `jira_auth.users` with BCrypt password hash for "password123", assigns ROLE_USER

### Step 6: Docker Build Dockerfiles ✅ (created, not yet used)
**New files:**
- `jira-user-service/Dockerfile.build-and-package` — Multi-stage build with .m2-cache and Netskope cert
- `jira-admin-service/Dockerfile.build-and-package` — Same pattern

---

## Remaining Work

### Step 7: Rebuild Java Services (BLOCKED — Maven dependency issue)
The `.m2-cache` is missing some dependencies needed by user-service (e.g., `springdoc-openapi-starter-webmvc-ui:2.8.4` — cache has `2.5.0`). Corporate proxy blocks Maven Central with a Netskope SSL intercept.

**Options to resolve:**
1. **Align pom.xml versions to cache** — Change `<springdoc.version>` in `jira-user-service/pom.xml` from `2.8.4` to `2.5.0` (and any other mismatched deps), then build offline.
2. **Populate the .m2-cache** — Run `mvn dependency:go-offline` from a machine with network access and copy the result into `.m2-cache/`.
3. **Use the Netskope cert properly** — The `Dockerfile.build-and-package` files have the cert import logic; the issue was finding the correct Java cacerts path inside the Maven Docker image. The path `$JAVA_HOME/lib/security/cacerts` should work — needs re-testing.
4. **Build locally** — If Maven works locally (outside Docker), run `mvn package -DskipTests` in `jira-user-service/` and `jira-admin-service/`, then use the standard `Dockerfile` (which copies `target/*.jar`).

**After building JARs, rebuild Docker images:**
```bash
cd JDC-main
docker compose build user-service
docker compose build admin-service
docker compose build gateway
docker compose build frontend
```

### Step 8: Rebuild Gateway
The gateway `application-docker.yml` was modified but the gateway JAR doesn't need rebuilding — it reads YAML config at runtime. Just restart:
```bash
docker compose up -d gateway
```

### Step 9: Rebuild Frontend
The nginx.conf and vite.config.ts changes require a frontend image rebuild:
```bash
docker compose build frontend
docker compose up -d frontend
```

### Step 10: Run Seed Data
```bash
docker exec -i jira-postgres psql -U jiraadmin -d jira_platform < postgres/seed-user-management.sql
```

### Step 11: Restart All Services
```bash
docker compose up -d postgres
docker compose up -d auth-service
docker compose up -d user-service admin-service gateway
docker compose up -d frontend
```

### Step 12: End-to-End Testing
Test each CRUD operation via curl:

**Users:**
```bash
# List users
curl -s http://localhost:3000/user-service/rest/admin/1.0/users/search | jq '.content | length'

# Get single user
curl -s http://localhost:3000/user-service/rest/admin/1.0/users/a0000000-0000-0000-0000-000000000201 | jq '.displayName'

# Create user
curl -s -X POST http://localhost:3000/user-service/rest/admin/1.0/users \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@example.com","fullName":"Test User","userName":"test.user"}' | jq .

# Update user
curl -s -X PUT http://localhost:3000/user-service/rest/admin/1.0/users/a0000000-0000-0000-0000-000000000201 \
  -H 'Content-Type: application/json' \
  -d '{"fullName":"John A. Smith","active":true}' | jq .

# Delete user
curl -s -X DELETE http://localhost:3000/user-service/rest/admin/1.0/users/<id>
```

**Groups:**
```bash
# List groups
curl -s http://localhost:3000/user-service/rest/admin/1.0/groups | jq '.content | length'

# Create group
curl -s -X POST http://localhost:3000/user-service/rest/admin/1.0/groups \
  -H 'Content-Type: application/json' \
  -d '{"name":"new-team","description":"A new team"}' | jq .

# Add user to group
curl -s -X POST http://localhost:3000/user-service/rest/admin/1.0/groups/<groupId>/members/<userId>

# Remove user from group
curl -s -X DELETE http://localhost:3000/user-service/rest/admin/1.0/groups/<groupId>/members/<userId>

# List group members
curl -s http://localhost:3000/user-service/rest/admin/1.0/groups/<groupId>/members | jq '.[].displayName'
```

**Project Roles (via admin-service):**
```bash
# List roles
curl -s http://localhost:3000/admin/project-roles | jq .

# Create role
curl -s -X POST http://localhost:3000/admin/project-roles \
  -H 'Content-Type: application/json' \
  -d '{"name":"Testers","description":"QA testers role"}' | jq .

# Update role
curl -s -X PUT http://localhost:3000/api/admin/project-roles/<id> \
  -H 'Content-Type: application/json' \
  -d '{"name":"QA Testers","description":"Updated description"}' | jq .

# Delete role
curl -s -X DELETE http://localhost:3000/api/admin/project-roles/<id>
```

**Permission/Notification/Security Schemes (via admin-service):**
```bash
curl -s http://localhost:3000/admin/permission-schemes | jq .
curl -s http://localhost:3000/admin/notification-schemes | jq .
curl -s http://localhost:3000/admin/security-schemes | jq .
```

### Step 13: Browser UI Testing
Open `http://localhost:3000` and test:
- `/admin/users` — Should show 10 users with search/filter working
- `/admin/users/create` — Create a new user, verify it appears in the list
- `/admin/users/edit/<id>` — View user details, verify groups and login info display
- `/admin/groups` — Should show 7 groups (3 system + 4 custom) with user counts
- `/admin/groups/members/<id>` — Add/remove members from a group
- `/admin/roles` — CRUD project roles
- `/admin/permissions` — View/create permission, notification, security schemes

---

## Files Modified (Summary)

| File | Change |
|------|--------|
| `jira-gateway/src/main/resources/application-docker.yml` | Fixed routing for admin-service and added user-service-admin route |
| `jira-frontend/nginx.conf` | Variable-based proxy_pass, fixed set/rewrite ordering, added /user-service/ location, removed admin from proxy regex |
| `jira-frontend/vite.config.ts` | Added /user-service proxy |
| `jira-frontend/src/features/admin/hooks/useAdminApi.ts` | Fixed group member paths, added update hooks |
| `jira-user-service/src/main/java/.../dto/UpdateUserRequest.java` | **New** — Update user DTO |
| `jira-user-service/src/main/java/.../controller/AdminUserController.java` | Added PUT /users/{id} and PUT /groups/{id} |
| `jira-user-service/src/main/java/.../service/JiraUserManagementService.java` | Added updateUser, updateGroup, fixed deleteUser bug |
| `jira-user-service/src/main/java/.../repository/CwdMembershipRepository.java` | Added deleteAllByChildIdAndMembershipType |
| `jira-user-service/Dockerfile.build-and-package` | **New** — Multi-stage Docker build |
| `jira-admin-service/src/main/java/.../controller/UserManagementController.java` | Added PUT/DELETE /project-roles/{id} |
| `jira-admin-service/src/main/java/.../service/UserManagementService.java` | Added updateProjectRole, deleteProjectRole |
| `jira-admin-service/Dockerfile.build-and-package` | **New** — Multi-stage Docker build |
| `postgres/seed-user-management.sql` | **New** — Comprehensive seed data (users, groups, memberships, auth sync) |
