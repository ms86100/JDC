# Jira Platform - Local Development Setup Guide

> **Quick start:** See [SETUP.md](SETUP.md) for clone + `npm ci` on a new machine.

## Prerequisites

1. **Java 21** installed
   ```bash
   java -version  # Should show 21.x
   ```

2. **PostgreSQL 16+** installed and running
   ```bash
   psql --version  # Should show 16+
   ```

3. **Node.js 18+** for frontend
   ```bash
   node -v  # Should show 18+
   ```

---

## Step 1: Create PostgreSQL Databases

Connect to PostgreSQL and create the required databases:

```bash
psql -U postgres
```

Then run:

```sql
-- Create databases for each microservice
CREATE DATABASE auth_db;
CREATE DATABASE user_db;
CREATE DATABASE project_db;
CREATE DATABASE issue_db;
CREATE DATABASE workflow_db;
CREATE DATABASE comment_db;
CREATE DATABASE notification_db;
CREATE DATABASE search_db;
CREATE DATABASE audit_db;
CREATE DATABASE attachment_db;
CREATE DATABASE sprint_db;

-- Grant permissions (adjust user as needed)
GRANT ALL PRIVILEGES ON DATABASE auth_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE user_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE project_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE issue_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE workflow_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE comment_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE search_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE audit_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE attachment_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE sprint_db TO postgres;

-- Enable UUID extension in each database
\c auth_db
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c user_db
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c project_db
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c issue_db
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c workflow_db
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c comment_db
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c notification_db
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c search_db
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c audit_db
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c attachment_db
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c sprint_db
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\q
```

---

## Step 2: Build Backend Services

Each service needs to be built with Maven:

```bash
cd jira-platform

# Build each service (in order)
cd jira-auth-service && mvn clean package -DskipTests && cd ..
cd jira-user-service && mvn clean package -DskipTests && cd ..
cd jira-project-service && mvn clean package -DskipTests && cd ..
cd jira-issue-service && mvn clean package -DskipTests && cd ..
cd jira-workflow-service && mvn clean package -DskipTests && cd ..
cd jira-comment-service && mvn clean package -DskipTests && cd ..
cd jira-notification-service && mvn clean package -DskipTests && cd ..
cd jira-search-service && mvn clean package -DskipTests && cd ..
cd jira-audit-service && mvn clean package -DskipTests && cd ..
cd jira-attachment-service && mvn clean package -DskipTests && cd ..
cd jira-sprint-service && mvn clean package -DskipTests && cd ..
cd jira-gateway && mvn clean package -DskipTests && cd ..
```

Or use the build script:
```bash
chmod +x build-local.sh
./build-local.sh
```

---

## Step 3: Start Backend Services

Run the startup script:
```bash
chmod +x start-local.sh
./start-local.sh
```

Or start services manually (in order):

```bash
# Terminal 1: Auth Service
cd jira-auth-service
java -jar target/*.jar --spring.profiles.active=local

# Terminal 2: User Service
cd jira-user-service
java -jar target/*.jar --spring.profiles.active=local

# Terminal 3: Project Service
cd jira-project-service
java -jar target/*.jar --spring.profiles.active=local

# Terminal 4: Issue Service
cd jira-issue-service
java -jar target/*.jar --spring.profiles.active=local

# Terminal 5: Workflow Service
cd jira-workflow-service
java -jar target/*.jar --spring.profiles.active=local

# Terminal 6: Comment Service
cd jira-comment-service
java -jar target/*.jar --spring.profiles.active=local

# Terminal 7: Notification Service
cd jira-notification-service
java -jar target/*.jar --spring.profiles.active=local

# Terminal 8: Search Service
cd jira-search-service
java -jar target/*.jar --spring.profiles.active=local

# Terminal 9: Audit Service
cd jira-audit-service
java -jar target/*.jar --spring.profiles.active=local

# Terminal 10: Attachment Service
cd jira-attachment-service
java -jar target/*.jar --spring.profiles.active=local

# Terminal 11: Sprint Service
cd jira-sprint-service
java -jar target/*.jar --spring.profiles.active=local

# Terminal 12: API Gateway (START LAST)
cd jira-gateway
java -jar target/*.jar --spring.profiles.active=local
```

---

## Step 4: Start Frontend

```bash
cd jira-frontend
npm install  # Only needed once
npm run dev
```

Frontend will be available at: http://localhost:3000

---

## Step 5: Verify Services

Check service health endpoints:

```bash
# API Gateway
curl http://localhost:8080/actuator/health

# Auth Service
curl http://localhost:8081/actuator/health

# Issue Service
curl http://localhost:8084/actuator/health
```

---

## Service Ports

| Service | Port | Purpose |
|---------|------|---------|
| Gateway | 8080 | API Gateway |
| Auth | 8081 | Authentication |
| User | 8082 | User management |
| Project | 8083 | Projects |
| Issue | 8084 | Issues |
| Workflow | 8085 | Workflows |
| Comment | 8086 | Comments |
| Notification | 8087 | Notifications |
| Search | 8088 | Search |
| Audit | 8089 | Audit logs |
| Attachment | 8090 | Attachments |
| Sprint | 8091 | Sprints |

---

## Troubleshooting

### PostgreSQL Connection Issues
If services can't connect to PostgreSQL:
1. Ensure PostgreSQL is running: `pg_isready`
2. Check credentials in `application-local.yml`
3. Verify database exists: `\l` in psql

### Port Already in Use
If a port is busy:
```bash
# Find process using port
netstat -ano | findstr :8080

# Kill it (Windows)
taskkill /PID <pid> /F
```

### Flyway Migration Errors
If migrations fail:
```bash
# Drop and recreate database
DROP DATABASE auth_db;
CREATE DATABASE auth_db;
```

Then restart the service.

---

## API Documentation

Swagger UI is available at:
- Gateway: http://localhost:8080/swagger-ui.html
- Auth: http://localhost:8081/swagger-ui.html
- Project: http://localhost:8083/swagger-ui.html

---

## Environment Variables

Override defaults using environment variables:

```bash
# Database
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=project_db
export DB_USERNAME=postgres
export DB_PASSWORD=postgres

# JWT
export JWT_SECRET=your-secret-key
export JWT_EXPIRATION_MS=86400000

# Then start service
java -jar target/*.jar
```

---

## What's Been Implemented

### Phase 1 Complete: Security/Permissions

✅ **Enhanced Permission System (V3 migration)**
- 30+ Jira DC-compatible permissions
- Permission grants to users, groups, and project roles
- Project roles (Admin, Developer, Committer, User, Viewer)
- Permission checking function
- Security levels and schemes

✅ **New Entities**
- Permission entity
- PermissionGrant entity
- ProjectRole entity
- SecurityLevel entity

✅ **Services**
- PermissionCheckService for authorization checks

### Coming Soon
- JQL Parser
- Enhanced Agile Boards
- Custom Field Types
- Automation Engine
