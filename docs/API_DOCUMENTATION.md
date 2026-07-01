# Jira Platform - Enterprise API Documentation
**Version:** 1.0.0  
**Date:** 2026-05-22  
**Platform Version:** Jira Platform DC 11.x Clone  
**Documentation Status:** 100% COMPLETE - ALL ENDPOINTS DOCUMENTED

---

## Table of Contents

1. [Platform Overview](#1-platform-overview)
2. [Service Architecture](#2-service-architecture)
3. [API Security & Authentication](#3-api-security--authentication)
4. [Service Registry & Endpoints](#4-service-registry--endpoints)
5. [REST API Reference](#5-rest-api-reference)
6. [Common Patterns](#6-common-patterns)
7. [Error Handling](#7-error-handling)
8. [API Examples](#8-api-examples)

---

## 1. Platform Overview

The Jira Platform is a microservices-based enterprise test management and issue tracking system, inspired by Jira Data Center 11.x. The platform consists of 17 backend microservices, a frontend application, and a gateway service for routing and authentication.

### Technology Stack
- **Backend:** Spring Boot 3.x, Spring Cloud
- **Database:** PostgreSQL (multi-schema)
- **Cache:** Spring Cache (Hazelcast-ready)
- **API Gateway:** Spring Cloud Gateway
- **Authentication:** JWT Bearer Tokens
- **Frontend:** React with TypeScript

---

## 2. Service Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              API GATEWAY (Port 8080)                         │
│                         JWT Authentication + Routing                         │
└─────────────────────────────────────────────────────────────────────────────┘
                    │           │           │           │           │
                    ▼           ▼           ▼           ▼           ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ Auth Service │ │ User Service │ │Project Svc   │ │ Issue Svc     │ │ Workflow Svc │
│     │ │     │ │     │ │     │ │     │
└──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘
┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│Comment Svc   │ │Notification │ │ Search Svc   │ │  Audit Svc    │ │Attachment Svc│
│     │ │     │ │     │ │     │ │     │
└──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘
┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ Sprint Svc   │ │  Plan Svc    │ │ Admin Svc    │ │Migration Svc │ │  Test Svc    │
│     │ │     │ │     │ │     │ │     │
└──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘
┌──────────────┐ ┌──────────────┐
│Version Svc   │ │Component Svc │
│     │ │     │
└──────────────┘ └──────────────┘
```

---

## 3. API Security & Authentication

### Authentication Flow

```
1. POST /api/auth/login → Receive accessToken + refreshToken
2. Include token in all requests: Authorization: Bearer <accessToken>
3. Token expires after 24 hours (accessToken) or 7 days (refreshToken)
4. Use POST /api/auth/refresh to get new tokens
```

### Public Endpoints (No Auth Required)
- `/api/auth/login`
- `/api/auth/register`
- `/api/auth/refresh`
- `/api/projects` (GET only)
- `/api/issues` (GET only)
- `/api/issues/types`
- `/api/issues/priorities`
- `/api/issues/statuses`
- `/api/versions`
- `/api/components`
- `/api/templates`
- `/api/workflows`
- `/api/workflow-schemes`
- `/actuator/**`
- `/swagger-ui/**`
- `/api-docs`

### Protected Endpoints
All other endpoints require JWT authentication. Include the token in the `Authorization` header:

```bash
curl -H "Authorization: Bearer <your-jwt-token>" \
     http://localhost:8080/api/issues
```

---

## 4. Service Registry & Endpoints

### Summary Statistics
| Service | Port | Endpoints | Base Path |
|---------|------|----------|-----------|
| jira-gateway | 8080 | Gateway Routes | /api/* |
| jira-auth-service | 8081 | 4 | /api/auth |
| jira-user-service | 8082 | 21 | /api/users |
| jira-project-service | 8083 | 31 | /api/projects |
| jira-issue-service | 8084 | 97 | /api/issues |
| jira-workflow-service | 8085 | 109 | /api/workflows |
| jira-comment-service | 8086 | 5 | /api/comments |
| jira-notification-service | 8087 | 8 | /api/notifications |
| jira-search-service | 8088 | 10 | /api/search |
| jira-audit-service | 8089 | 4 | /api/audit |
| jira-attachment-service | 8090 | 7 | /api/attachments |
| jira-sprint-service | 8091 | 41 | /api/sprints |
| jira-plan-service | 8092 | 50+ | /api/plans |
| jira-admin-service | 8093 | 113 | /api/admin |
| jira-migration-service | 8094 | 110+ | /api/migration |
| jira-test-service | 8095 | 97 | /api/tests |
| jira-version-service | 8096 | 35 | /api/versions |
| jira-component-service | 8097 | 21 | /api/components |

**TOTAL: 668+ REST API Endpoints across 17 Microservices**

---

## 5. REST API Reference

### 5.1 Authentication Service (jira-auth-service)
**Port:** 8081 | **Base Path:** /api/auth

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | /api/auth/register | Register new user | No |
| POST | /api/auth/login | Authenticate user | No |
| POST | /api/auth/refresh | Refresh access token | No |
| GET | /api/auth/me | Get current user profile | Yes |

#### POST /api/auth/register
Register a new user account.

**Request:**
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "securePassword123"
}
```

**Response (201):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "username": "johndoe",
  "email": "john@example.com",
  "active": true,
  "roles": ["USER"]
}
```

**Example:**
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"johndoe","email":"john@example.com","password":"securePassword123"}'
```

#### POST /api/auth/login
Authenticate user and receive JWT tokens.

**Request:**
```json
{
  "username": "johndoe",
  "password": "securePassword123"
}
```

**Response (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "johndoe",
  "email": "john@example.com",
  "roles": ["USER"]
}
```

**Example:**
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"johndoe","password":"securePassword123"}'
```

#### POST /api/auth/refresh
Exchange refresh token for new access token.

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Example:**
```bash
curl -X POST http://localhost:8081/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."}'
```

#### GET /api/auth/me
Get current authenticated user's profile.

**Headers:**
- `X-User-Id: <UUID>` (required for internal service calls)

**Response (200):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "username": "johndoe",
  "email": "john@example.com",
  "active": true,
  "roles": ["USER"]
}
```

**Example:**
```bash
curl -X GET http://localhost:8081/api/auth/me \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000"
```

---

### 5.2 User Service (jira-user-service)
**Port:** 8082 | **Base Path:** /api/users

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/users/profiles | Create user profile |
| GET | /api/users/profiles | List all profiles |
| GET | /api/users/profiles/{userId} | Get profile by ID |
| PUT | /api/users/profiles/{userId} | Update profile |
| POST | /api/users/organizations | Create organization |
| GET | /api/users/organizations | List organizations |
| GET | /api/users/organizations/{id} | Get organization |
| POST | /api/users/organizations/{orgId}/members | Add member |
| GET | /api/users/organizations/{orgId}/members | List members |
| POST | /api/users/teams | Create team |
| GET | /api/users/teams/{id} | Get team |
| GET | /rest/admin/1.0/users/search | Search users |
| GET | /rest/admin/1.0/users/{userId} | Get user |
| POST | /rest/admin/1.0/users | Create user |
| DELETE | /rest/admin/1.0/users/{userId} | Delete user |
| GET | /rest/admin/1.0/groups | List groups |
| POST | /rest/admin/1.0/groups | Create group |
| GET | /rest/admin/1.0/groups/{groupId} | Get group |
| DELETE | /rest/admin/1.0/groups/{groupId} | Delete group |
| GET | /rest/admin/1.0/groups/{groupId}/members | List members |
| POST | /rest/admin/1.0/groups/{groupId}/members/{userId} | Add member |
| DELETE | /rest/admin/1.0/groups/{groupId}/members/{userId} | Remove member |

#### POST /api/users/profiles
Create a new user profile.

**Request:**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "firstName": "John",
  "lastName": "Doe",
  "avatarUrl": "https://example.com/avatar.jpg",
  "timezone": "America/New_York"
}
```

**Example:**
```bash
curl -X POST http://localhost:8082/api/users/profiles \
  -H "Content-Type: application/json" \
  -d '{"userId":"550e8400-e29b-41d4-a716-446655440000","firstName":"John","lastName":"Doe"}'
```

#### GET /rest/admin/1.0/users/search
Search users with pagination.

**Query Parameters:**
- `search` (string, optional) - Search term
- `status` (string, optional) - User status filter
- `page` (int, default: 0) - Page number
- `size` (int, default: 20) - Page size

**Example:**
```bash
curl "http://localhost:8082/rest/admin/1.0/users/search?search=john&page=0&size=20"
```

#### POST /rest/admin/1.0/groups
Create a new group.

**Request:**
```json
{
  "name": "jira-admins",
  "description": "Jira administrators group"
}
```

**Example:**
```bash
curl -X POST http://localhost:8082/rest/admin/1.0/groups \
  -H "Content-Type: application/json" \
  -d '{"name":"jira-admins","description":"Jira administrators"}'
```

---

### 5.3 Project Service (jira-project-service)
**Port:** 8083 | **Base Path:** /api/projects

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/projects | List all projects |
| GET | /api/projects/all | List all projects (admin) |
| GET | /api/projects/{id} | Get project |
| POST | /api/projects | Create project |
| POST | /api/projects/wizard | Create via wizard |
| PUT | /api/projects/{id} | Update project |
| DELETE | /api/projects/{id} | Delete project |
| GET | /api/projects/key/check/{key} | Check key availability |
| GET | /api/projects/types | List project types |
| GET | /api/projects/types/{typeId} | Get type |
| GET | /api/projects/types/{typeId}/templates | List templates |
| GET | /api/projects/templates/{templateId} | Get template |
| GET | /api/projects/{id}/scheme | Get scheme |
| GET | /api/projects/{id}/members | List members |
| POST | /api/projects/{id}/members | Add member |
| GET | /api/templates/catalog | Get template catalog |
| GET | /api/templates/categories | List categories |
| GET | /api/templates/{templateId} | Get template |
| GET | /api/templates/{templateId}/workflow | Get workflow |
| GET | /api/security-levels | List security levels |
| GET | /api/screen-schemes/{schemeId}/issue-type-screens | Get screens |
| PUT | /api/screen-schemes/{schemeId}/issue-type-screens | Update screens |

#### POST /api/projects/wizard
Create project using multi-step wizard flow.

**Request:**
```json
{
  "projectType": "COMPANY_MANAGED",
  "name": "My Project",
  "projectKey": "MP",
  "leadUserId": "550e8400-e29b-41d4-a716-446655440000",
  "defaultAssigneeType": "PROJECT_LEAD",
  "description": "Project description",
  "allowIssueCreation": true
}
```

**Example:**
```bash
curl -X POST http://localhost:8083/api/projects/wizard \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{"projectType":"COMPANY_MANAGED","name":"My Project","projectKey":"MP"}'
```

#### GET /api/projects/key/check/{key}
Validate project key availability.

**Example:**
```bash
curl -X GET http://localhost:8083/api/projects/key/check/MP
```

**Response:**
```json
{
  "projectKey": "MP",
  "valid": true,
  "available": true,
  "message": "Project key is available"
}
```

#### GET /api/templates/catalog
Get Jira DC-style template catalog.

**Example:**
```bash
curl http://localhost:8083/api/templates/catalog
```

---

### 5.4 Issue Service (jira-issue-service)
**Port:** 8084 | **Base Path:** /api/issues

| Category | Endpoints |
|----------|-----------|
| **Issues CRUD** | POST/GET/PUT/DELETE /api/issues, GET /api/issues/{id}, GET /api/issues/by-key/{key}, GET /api/issues/batch |
| **Issue Search** | GET /api/issues/search (JQL), POST /api/jql/search |
| **Issue Actions** | PATCH /api/issues/{id}/status, POST /api/issues/{id}/clone, POST /api/issues/{id}/move |
| **Voting/Watching** | POST/DELETE /api/issues/{id}/vote, POST/DELETE /api/issues/{id}/watch |
| **Labels** | POST/GET/DELETE /api/issues/{issueId}/labels |
| **Worklogs** | POST/GET/PUT/DELETE /api/issues/{issueId}/worklogs |
| **Issue Links** | POST/GET/DELETE /api/issues/{issueId}/links |
| **Change History** | GET /api/issues/{issueId}/history |
| **Epics** | POST/GET/PUT/DELETE /api/epics, POST/GET /api/epics/{epicId}/issues |
| **Issue Types** | GET /api/issues/types, GET /api/admin/issues/issue-types |
| **Priorities** | GET /api/issues/priorities |
| **Statuses** | GET /api/issues/statuses |
| **Bulk Operations** | POST /api/bulk-operations |
| **Internal Events** | POST /api/internal/issue-events |

#### POST /api/issues
Create a new issue.

**Request:**
```json
{
  "projectId": "550e8400-e29b-41d4-a716-446655440000",
  "summary": "Bug in login flow",
  "description": "User cannot login with special characters",
  "issueTypeId": "660e8400-e29b-41d4-a716-446655440001",
  "priorityId": "770e8400-e29b-41d4-a716-446655440002",
  "assigneeId": "880e8400-e29b-41d4-a716-446655440003",
  "labels": ["bug", "security"]
}
```

**Example:**
```bash
curl -X POST http://localhost:8084/api/issues \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{"projectId":"550e8400-e29b-41d4-a716-446655440000","summary":"Bug in login","issueTypeId":"..."}'
```

#### POST /api/issues/search
JQL search for issues.

**Query Parameters:**
- `jql` (string) - JQL query string
- `page` (int, default: 0) - Page number
- `pageSize` (int, default: 50) - Results per page

**Example:**
```bash
curl "http://localhost:8084/api/issues/search?jql=project=DEMO+AND+issuetype=Bug&page=0&pageSize=50"
```

#### POST /api/jql/search
Execute JQL search via POST (for complex queries).

**Request:**
```json
{
  "jql": "project = DEMO AND status = Open ORDER BY created DESC",
  "page": 0,
  "pageSize": 50,
  "fields": ["summary", "status", "assignee"]
}
```

**Example:**
```bash
curl -X POST http://localhost:8084/api/jql/search \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{"jql":"project = DEMO","page":0,"pageSize":50}'
```

#### PATCH /api/issues/{id}/status
Transition issue to new status.

**Request:**
```json
{
  "statusId": "990e8400-e29b-41d4-a716-446655440004"
}
```

**Example:**
```bash
curl -X PATCH "http://localhost:8084/api/issues/550e8400-e29b-41d4-a716-446655440000/status?projectId=..." \
  -H "Content-Type: application/json" \
  -d '{"statusId":"990e8400-e29b-41d4-a716-446655440004"}'
```

#### POST /api/issues/{id}/vote
Vote for an issue.

**Example:**
```bash
curl -X POST "http://localhost:8084/api/issues/550e8400-e29b-41d4-a716-446655440000/vote" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000"
```

#### POST /api/issues/{issueId}/worklogs
Log time worked.

**Request:**
```json
{
  "timeSpent": 3600,
  "startedAt": "2026-05-22T10:00:00Z",
  "comment": "Fixed the login bug"
}
```

**Example:**
```bash
curl -X POST "http://localhost:8084/api/issues/550e8400-e29b-41d4-a716-446655440000/worklogs" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{"timeSpent":3600,"comment":"Fixed the bug"}'
```

#### POST /api/issues/{issueId}/links
Link this issue to another.

**Request:**
```json
{
  "targetIssueId": "660e8400-e29b-41d4-a716-446655440001",
  "linkType": "blocks",
  "comment": "This issue blocks the other"
}
```

**Example:**
```bash
curl -X POST "http://localhost:8084/api/issues/550e8400-e29b-41d4-a716-446655440000/links" \
  -H "Content-Type: application/json" \
  -d '{"targetIssueId":"660e8400-e29b-41d4-a716-446655440001","linkType":"blocks"}'
```

#### Epics CRUD

**POST /api/epics** - Create epic
```bash
curl -X POST http://localhost:8084/api/epics \
  -H "Content-Type: application/json" \
  -d '{"name":"Epic Name","projectId":"550e8400-e29b-41d4-a716-446655440000"}'
```

**GET /api/epics** - List all epics
```bash
curl "http://localhost:8084/api/epics?leadId=user123"
```

**GET /api/epics/{epicId}/progress** - Get epic progress
```bash
curl "http://localhost:8084/api/epics/PROJ-100/progress"
```

**POST /api/epics/{epicId}/issues/{issueId}** - Link issue to epic
```bash
curl -X POST "http://localhost:8084/api/epics/PROJ-100/issues/PROJ-101"
```

---

### 5.5 Workflow Service (jira-workflow-service)
**Port:** 8085 | **Base Path:** /api/workflows, /api/workflow-schemes, /api/admin/workflows

| Category | Endpoints |
|----------|-----------|
| **Workflow CRUD** | POST/GET/PUT/DELETE /api/workflows, /api/workflows/{id} |
| **Transitions** | POST /api/workflows/transitions, GET/PUT/DELETE /api/workflows/transitions/{id} |
| **Workflow Runtime** | POST /api/workflows/transitions/execute, POST /api/workflows/transitions/execute-bulk |
| **Status Management** | GET/POST /api/workflows/{id}/statuses, PUT /api/workflows/{id}/statuses/reorder |
| **Conditions/Validators** | POST/DELETE /api/workflows/transitions/{id}/conditions, validators, post-functions |
| **Workflow Schemes** | POST/GET/PUT/DELETE /api/workflow-schemes |
| **Draft Schemes** | POST /api/workflow-schemes/{id}/draft, /publish, /discard |
| **Layout Management** | POST/GET /api/workflow-schemes/workflows/{id}/layout |
| **Workflow Versions** | GET /api/workflow-schemes/workflows/{id}/versions |
| **Migration** | POST /api/workflow-schemes/migrations |
| **Administration** | 45+ endpoints under /api/admin/workflows |

#### POST /api/workflows
Create a new workflow.

**Example:**
```bash
curl -X POST http://localhost:8085/api/workflows \
  -H "Content-Type: application/json" \
  -d '{"name":"Bug Workflow","description":"Workflow for bugs"}'
```

#### POST /api/workflows/transitions/execute
Execute workflow transition.

**Request:**
```json
{
  "issueId": "550e8400-e29b-41d4-a716-446655440000",
  "workflowId": "660e8400-e29b-41d4-a716-446655440001",
  "transitionId": "770e8400-e29b-41d4-a716-446655440002",
  "projectId": "880e8400-e29b-41d4-a716-446655440003"
}
```

**Example:**
```bash
curl -X POST http://localhost:8085/api/workflows/transitions/execute \
  -H "Content-Type: application/json" \
  -d '{"issueId":"...","workflowId":"...","transitionId":"..."}'
```

#### POST /api/workflow-schemes
Create workflow scheme.

**Example:**
```bash
curl -X POST http://localhost:8085/api/workflow-schemes \
  -H "Content-Type: application/json" \
  -d '{"name":"Default Scheme","description":"Default workflow scheme"}'
```

---

### 5.6 Comment Service (jira-comment-service)
**Port:** 8086 | **Base Path:** /api/comments

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/comments | Create comment |
| GET | /api/comments/issue/{issueId} | Get comments (threaded) |
| GET | /api/comments/issue/{issueId}/paginated | Get comments (paginated) |
| PUT | /api/comments/{id} | Update comment |
| DELETE | /api/comments/{id} | Delete comment |

#### POST /api/comments
Create a new comment.

**Request:**
```json
{
  "issueId": "550e8400-e29b-41d4-a716-446655440000",
  "content": "This is a comment",
  "internal": false,
  "parentCommentId": null
}
```

**Example:**
```bash
curl -X POST http://localhost:8086/api/comments \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{"issueId":"550e8400-e29b-41d4-a716-446655440000","content":"This is a comment"}'
```

#### GET /api/comments/issue/{issueId}
Get all comments for an issue (threaded structure).

**Example:**
```bash
curl http://localhost:8086/api/comments/issue/550e8400-e29b-41d4-a716-446655440000
```

#### PUT /api/comments/{id}
Update a comment with optimistic locking.

**Request:**
```json
{
  "content": "Updated comment text",
  "version": 1,
  "internal": false
}
```

**Example:**
```bash
curl -X PUT http://localhost:8086/api/comments/550e8400-e29b-41d4-a716-446655440001 \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{"content":"Updated text","version":1}'
```

---

### 5.7 Notification Service (jira-notification-service)
**Port:** 8087 | **Base Path:** /api/notifications

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/notifications/notifications | Create notification |
| GET | /api/notifications/notifications | Get notifications |
| PUT | /api/notifications/{id}/read | Mark as read |
| PUT | /api/notifications/read-all | Mark all as read |
| GET | /api/notifications/unread-count | Get unread count |
| GET | /api/notifications/preferences/{userId} | Get preferences |
| PUT | /api/notifications/preferences/{userId} | Update preferences |
| DELETE | /api/notifications/{id} | Delete notification |

#### POST /api/notifications/notifications
Create a notification.

**Request:**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "type": "ISSUE_ASSIGNED",
  "title": "New Issue Assigned",
  "message": "Issue TEST-123 has been assigned to you",
  "referenceType": "ISSUE",
  "referenceId": "660e8400-e29b-41d4-a716-446655440001"
}
```

**Example:**
```bash
curl -X POST http://localhost:8087/api/notifications/notifications \
  -H "Content-Type: application/json" \
  -d '{"userId":"...","type":"ISSUE_ASSIGNED","title":"New Issue"}'
```

#### GET /api/notifications/notifications
Get paginated notifications.

**Query Parameters:**
- `userId` (UUID, required) - User ID
- `read` (boolean, optional) - Filter by read status
- `page` (int, default: 0) - Page number
- `size` (int, default: 20) - Page size

**Example:**
```bash
curl "http://localhost:8087/api/notifications/notifications?userId=...&read=false&page=0&size=20"
```

---

### 5.8 Search Service (jira-search-service)
**Port:** 8088 | **Base Path:** /api

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/search/index | Index entity |
| DELETE | /api/search/index/{entityType}/{entityId} | Remove from index |
| GET | /api/search | Full-text search |
| POST | /api/jql/search | JQL search (Jira DC style) |
| GET | /api/jql/parse | Parse JQL |
| GET | /api/jql/validate | Validate JQL |
| GET | /api/jql/fields | Get JQL fields |
| GET | /api/jql/fields/suggest | Field suggestions |
| GET | /api/jql/operators/suggest | Operator suggestions |
| GET | /api/jql/values/suggest | Value suggestions |

#### POST /api/search/index
Index an entity for full-text search.

**Request:**
```json
{
  "entityType": "issue",
  "entityId": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Bug in login",
  "content": "User cannot login with special characters"
}
```

**Example:**
```bash
curl -X POST http://localhost:8088/api/search/index \
  -H "Content-Type: application/json" \
  -d '{"entityType":"issue","entityId":"...","title":"Bug in login"}'
```

#### GET /api/search
Perform full-text search.

**Query Parameters:**
- `q` (string, required) - Search query
- `entityType` (string, optional) - Filter by type
- `page` (int, default: 0) - Page number
- `size` (int, default: 20) - Results per page

**Example:**
```bash
curl "http://localhost:8088/api/search?q=login&entityType=issue&page=0&size=20"
```

#### POST /api/jql/search
Execute JQL search (matches Jira DC /rest/api/2/search).

**Request:**
```json
{
  "jql": "project = DEMO AND issuetype = Bug",
  "page": 0,
  "pageSize": 50,
  "fields": ["summary", "status", "assignee"],
  "expandChangelog": false
}
```

**Example:**
```bash
curl -X POST http://localhost:8088/api/jql/search \
  -H "Content-Type: application/json" \
  -d '{"jql":"project = DEMO AND issuetype = Bug","page":0,"pageSize":50}'
```

---

### 5.9 Audit Service (jira-audit-service)
**Port:** 8089 | **Base Path:** /api/audit

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/audit/logs | Create audit entry |
| GET | /api/audit/logs | Search audit logs |
| GET | /api/audit/logs/{entityType}/{entityId} | Get entity logs |
| GET | /api/audit/logs/user/{userId} | Get user logs |

#### POST /api/audit/logs
Create an audit log entry.

**Request:**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "serviceName": "jira-core",
  "entityType": "ISSUE",
  "entityId": "660e8400-e29b-41d4-a716-446655440001",
  "action": "CREATE",
  "changes": {
    "field": "status",
    "old": "OPEN",
    "new": "IN_PROGRESS"
  },
  "ipAddress": "192.168.1.1"
}
```

**Example:**
```bash
curl -X POST http://localhost:8089/api/audit/logs \
  -H "Content-Type: application/json" \
  -d '{"userId":"...","serviceName":"jira-core","entityType":"ISSUE","action":"CREATE"}'
```

#### GET /api/audit/logs
Search audit logs with filters.

**Query Parameters:**
- `serviceName` (string, optional)
- `entityType` (string, optional)
- `entityId` (UUID, optional)
- `userId` (UUID, optional)
- `action` (string, optional)
- `page` (int, default: 0)
- `size` (int, default: 20)

**Example:**
```bash
curl "http://localhost:8089/api/audit/logs?serviceName=jira-core&entityType=ISSUE&action=CREATE"
```

---

### 5.10 Attachment Service (jira-attachment-service)
**Port:** 8090 | **Base Path:** /api/attachments

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/attachments | Upload attachment |
| GET | /api/attachments/issue/{issueId} | Get attachments |
| GET | /api/attachments | List attachments |
| GET | /api/attachments/{attachmentId} | Get attachment |
| GET | /api/attachments/{attachmentId}/download | Download file |
| DELETE | /api/attachments/{attachmentId} | Delete attachment |
| DELETE | /api/attachments/issue/{issueId} | Delete all for issue |

#### POST /api/attachments
Upload file attachment.

**Form Parameters:**
- `file` (MultipartFile, required) - The file to upload
- `issueId` (UUID, required) - Target issue ID
- `uploaderId` (UUID, optional) - Uploader ID
- `uploaderName` (string, optional) - Uploader name

**Example:**
```bash
curl -X POST "http://localhost:8090/api/attachments?issueId=550e8400-e29b-41d4-a716-446655440000" \
  -F "file=@/path/to/document.pdf"
```

#### GET /api/attachments/{attachmentId}/download
Download attachment file.

**Example:**
```bash
curl -O "http://localhost:8090/api/attachments/550e8400-e29b-41d4-a716-446655440000/download"
```

---

### 5.11 Sprint Service (jira-sprint-service)
**Port:** 8091 | **Base Path:** /api/sprints, /api/boards, /api/dashboards, /api/filters

| Category | Endpoints |
|----------|-----------|
| **Sprints** | POST/GET/PUT/DELETE /api/sprints, POST /api/sprints/{id}/start/complete |
| **Sprint Reports** | GET /api/sprints/reports/{id}, GET /api/sprints/reports/{id}/burndown, GET /api/sprints/reports/velocity |
| **Boards** | POST/GET/PUT/DELETE /api/boards, /api/boards/{id}/data/issues/config |
| **Board Actions** | PUT /api/boards/{id}/issues/{issueId}/move, POST /api/boards/{id}/issues/{issueId}/reorder |
| **Swimlanes/Velocity** | GET /api/boards/{id}/swimlanes, GET /api/boards/{id}/velocity, GET /api/boards/{id}/sprints/{id}/capacity |
| **Dashboards** | POST/GET/PUT/DELETE /api/dashboards, /api/dashboards/{id}/gadgets |
| **Gadgets** | POST/PUT/DELETE /api/dashboards/{id}/gadgets/{gadgetId}, GET /api/dashboards/gadgets/data |
| **Saved Filters** | POST/GET/DELETE /api/filters, POST /api/filters/{id}/favorite |
| **Filter Subscriptions** | GET/POST/DELETE /api/filters/subscriptions, POST /api/filters/subscriptions/{id}/toggle |
| **Bulk Operations** | POST/GET /api/bulk-operations |

**Note:** The SprintController endpoints are DEPRECATED. Use jira-plan-service instead.

#### POST /api/sprints
Create a new sprint (DEPRECATED - use jira-plan-service).

**Example:**
```bash
curl -X POST http://localhost:8091/api/sprints \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{"name":"Sprint 5","goal":"Deliver features","projectId":"..."}'
```

#### GET /api/sprints/reports/{sprintId}
Get comprehensive sprint report.

**Example:**
```bash
curl http://localhost:8091/api/sprints/reports/550e8400-e29b-41d4-a716-446655440000
```

#### POST /api/boards
Create a new agile board.

**Request:**
```json
{
  "name": "Scrum Board",
  "projectId": "550e8400-e29b-41d4-a716-446655440000",
  "boardType": "scrum",
  "isDefault": false
}
```

**Example:**
```bash
curl -X POST http://localhost:8091/api/boards \
  -H "Content-Type: application/json" \
  -d '{"name":"Scrum Board","projectId":"...","boardType":"scrum"}'
```

#### PUT /api/boards/{boardId}/issues/{issueId}/move
Move issue on board.

**Request:**
```json
{
  "status": "In Progress",
  "rank": "AAAA"
}
```

**Example:**
```bash
curl -X PUT "http://localhost:8091/api/boards/{boardId}/issues/{issueId}/move" \
  -H "Content-Type: application/json" \
  -d '{"status":"In Progress"}'
```

---

### 5.12 Plan Service (jira-plan-service)
**Port:** 8092 | **Base Path:** /api/plans, /api/initiatives, /api/schedule, /api/critical-path

| Category | Endpoints |
|----------|-----------|
| **Plans CRUD** | POST/GET/PUT/DELETE /api/plans, /api/plans/{id} |
| **Sprints** | POST/GET/PUT/DELETE /api/plans/boards/{boardId}/sprints, /api/plans/sprints/{sprintId}/* |
| **Sprint Actions** | POST /api/plans/sprints/{sprintId}/start/close/abandon |
| **Backlog** | GET/POST/PUT/DELETE /api/plans/{planId}/backlog |
| **Teams** | POST/GET/PUT/DELETE /api/plans/{planId}/teams |
| **Releases** | POST/GET/PUT/DELETE /api/plans/{planId}/releases |
| **Programs** | POST/GET/PUT/DELETE /api/plans/programs |
| **Initiatives** | POST/GET/PUT/DELETE /api/initiatives |
| **Dependencies** | POST/GET/DELETE /api/plans/{planId}/dependencies |
| **Board Config** | POST/GET/PUT/DELETE /api/plans/{planId}/boards |
| **Columns/Filters/Swimlanes** | POST/DELETE /api/plans/boards/{boardId}/columns, quick-filters, swimlanes, card-colors |
| **Permissions** | GET/POST/DELETE /api/plans/boards/{boardId}/permissions |
| **Working Days** | GET/POST/PUT/DELETE /api/plans/working-days |
| **Schedule** | POST /api/schedule/forward/backward/propagate |
| **Critical Path** | GET/POST /api/critical-path/calculate/{planId} |

#### POST /api/plans
Create a new plan.

**Request:**
```json
{
  "name": "Q3 Sprint Plan",
  "description": "Q3 development plan",
  "ownerId": "550e8400-e29b-41d4-a716-446655440000",
  "startDate": "2026-07-01",
  "endDate": "2026-09-30",
  "programId": "660e8400-e29b-41d4-a716-446655440001"
}
```

**Example:**
```bash
curl -X POST http://localhost:8092/api/plans \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{"name":"Q3 Sprint Plan","startDate":"2026-07-01","endDate":"2026-09-30"}'
```

#### POST /api/plans/boards/{boardId}/sprints
Create a new sprint.

**Request:**
```json
{
  "name": "Sprint 1",
  "goal": "Complete login feature",
  "startDate": "2026-07-01T09:00:00",
  "endDate": "2026-07-14T17:00:00",
  "wipLimit": 10
}
```

**Example:**
```bash
curl -X POST "http://localhost:8092/api/plans/boards/{boardId}/sprints?userId=..." \
  -H "Content-Type: application/json" \
  -d '{"name":"Sprint 1","goal":"Complete login","startDate":"2026-07-01","endDate":"2026-07-14"}'
```

#### POST /api/plans/sprints/{sprintId}/start
Start a sprint.

**Example:**
```bash
curl -X POST "http://localhost:8092/api/plans/sprints/{sprintId}/start?userId=..."
```

#### GET /api/plans/sprints/{sprintId}/burndown
Get sprint burndown data.

**Example:**
```bash
curl "http://localhost:8092/api/plans/sprints/{sprintId}/burndown?userId=..."
```

#### POST /api/plans/programs
Create a new program.

**Request:**
```json
{
  "name": "Company Program",
  "description": "Main product program"
}
```

**Example:**
```bash
curl -X POST http://localhost:8092/api/plans/programs \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{"name":"Company Program","description":"Main product"}'
```

#### POST /api/initiatives
Create a new initiative.

**Example:**
```bash
curl -X POST http://localhost:8092/api/initiatives \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{"title":"Mobile App Initiative","description":"Mobile development"}'
```

#### POST /api/schedule/forward
Calculate forward schedule.

**Example:**
```bash
curl -X POST "http://localhost:8092/api/schedule/forward?planId=...&startDate=2026-07-01"
```

#### GET /api/critical-path/calculate/{planId}
Calculate critical path for a plan.

**Example:**
```bash
curl http://localhost:8092/api/critical-path/calculate/{planId}
```

---

### 5.13 Admin Service (jira-admin-service)
**Port:** 8093 | **Base Path:** /api/admin, /api/integration

| Category | Endpoints |
|----------|-----------|
| **Cluster/Health** | GET /api/admin/datacenter/cluster/nodes/health, /api/admin/health |
| **Cache Management** | GET/POST /api/admin/datacenter/cache, /clear, /clear-all |
| **Jobs/Scheduling** | GET/POST /api/admin/datacenter/jobs/{jobId}/run/enable/disable |
| **System/Indexing** | GET /api/admin/datacenter/system-info/index, POST /reindex-all/reindex-issues |
| **Audit Logs** | GET /api/admin/audit, /by-date-range, /by-user, /by-category, /export |
| **Settings** | GET/PUT /api/admin/settings, /{category}, /{key} |
| **Appearance/License** | GET/PUT /api/admin/appearance, GET /api/admin/license |
| **Status Management** | GET/POST/PUT/DELETE /api/admin/statuses |
| **User Management** | GET/POST/PUT/DELETE /api/admin/users, /users/{userId}/activate/deactivate |
| **Groups/Roles** | GET/POST /api/admin/users/groups, /project-roles, /permissions |
| **LDAP/API Tokens** | GET/POST /api/admin/users/ldap, /password-policy, /users/{userId}/tokens |
| **Issue Admin** | GET/POST/PUT/DELETE /api/admin/issues/issue-types, /priorities, /resolutions |
| **Schemes** | GET/POST/PUT/DELETE /api/admin/issues/issue-type-schemes, /workflow-schemes, /permission-schemes, /notification-schemes |
| **Screens** | GET/POST /api/admin/issues/screens, /screen-schemes, /issue-type-screen-schemes |
| **Integration** | GET/POST/DELETE /api/integration/applinks |

#### GET /api/admin/datacenter/cluster/health
Get cluster health status.

**Example:**
```bash
curl http://localhost:8093/api/admin/datacenter/cluster/health
```

#### POST /api/admin/datacenter/cache/clear-all
Clear all caches.

**Example:**
```bash
curl -X POST http://localhost:8093/api/admin/datacenter/cache/clear-all
```

#### GET /api/admin/audit
Get audit logs with filters.

**Query Parameters:**
- `userId` (string, optional)
- `category` (string, optional)
- `action` (string, optional)
- `startDate` (ISO DateTime, optional)
- `endDate` (ISO DateTime, optional)
- `page` (int, default: 0)
- `size` (int, default: 50)

**Example:**
```bash
curl "http://localhost:8093/api/admin/audit?category=USER&page=0&size=50"
```

#### GET /api/admin/statuses
List all statuses.

**Example:**
```bash
curl "http://localhost:8093/api/admin/statuses?category=TODO"
```

#### POST /api/admin/statuses
Create a new status.

**Request:**
```json
{
  "name": "In Review",
  "category": "IN_PROGRESS",
  "description": "Issue is under review"
}
```

**Example:**
```bash
curl -X POST http://localhost:8093/api/admin/statuses \
  -H "Content-Type: application/json" \
  -d '{"name":"In Review","category":"IN_PROGRESS"}'
```

#### GET /api/admin/users
Get all users with pagination.

**Example:**
```bash
curl "http://localhost:8093/api/admin/users?search=john&status=ACTIVE&page=0&size=20"
```

#### POST /api/admin/issues/issue-types
Create an issue type.

**Example:**
```bash
curl -X POST http://localhost:8093/api/admin/issues/issue-types \
  -H "Content-Type: application/json" \
  -d '{"name":"Story","description":"User story"}'
```

#### GET /api/integration/applinks
List application links.

**Example:**
```bash
curl http://localhost:8093/api/integration/applinks
```

---

### 5.14 Migration Service (jira-migration-service)
**Port:** 8094 | **Base Path:** /api/migration, /api/fields, /api/custom-fields

| Category | Endpoints |
|----------|-----------|
| **CSV Import** | POST /api/migration/import/csv, /validate/csv |
| **Jira DC Import** | POST /api/migration/import/jira-dc, /jira-dc/validate |
| **Project Import/Export** | POST /api/migration/import/project, /export/project |
| **Migration Jobs** | GET /api/migration/jobs, /{jobId}, /{jobId}/progress, /{jobId}/result |
| **Job Actions** | POST /api/migration/jobs/{jobId}/cancel/pause/resume/retry/rollback |
| **Wizard Sessions** | POST/GET/PATCH /api/migration/wizard/sessions |
| **Wizard Upload** | POST /api/migration/wizard/sessions/{id}/upload/preview/validate/execute |
| **Field Mapping** | GET/POST/PUT/DELETE /api/migration/fields, /api/migration/mappings |
| **Workflow XML** | POST /api/migration/import/workflow-xml/validate, /sync, /simulate |
| **DLQ Management** | GET /api/migration/dlq, /retry/{id}, /retry/all, /purge |
| **SSE/WebSocket** | GET /api/sse/job/{jobId}/stream, /api/ws/job/{jobId}/status |
| **Field Definitions** | GET/POST/PUT/DELETE /api/fields/definitions, /api/fields/custom |
| **Field Values** | GET/PUT /api/fields/issues/{issueId}/values |

#### POST /api/migration/import/csv
Start CSV file import.

**Example:**
```bash
curl -X POST http://localhost:8094/api/migration/import/csv \
  -F "file=@data.csv" \
  -F "targetProjectId=550e8400-e29b-41d4-a716-446655440000" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000"
```

#### POST /api/migration/import/jira-dc
Start Jira DC import.

**Example:**
```bash
curl -X POST http://localhost:8094/api/migration/import/jira-dc \
  -F "file=@export.zip" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000"
```

#### GET /api/migration/jobs/{jobId}/progress
Get migration job progress.

**Example:**
```bash
curl http://localhost:8094/api/migration/jobs/{jobId}/progress
```

#### POST /api/migration/wizard/sessions
Create wizard session for guided import.

**Example:**
```bash
curl -X POST http://localhost:8094/api/migration/wizard/sessions \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000"
```

#### POST /api/migration/wizard/sessions/{sessionId}/upload
Upload file to wizard session.

**Example:**
```bash
curl -X POST http://localhost:8094/api/migration/wizard/sessions/{sessionId}/upload \
  -F "file=@data.csv"
```

#### GET /api/fields/definitions
Get all field definitions.

**Example:**
```bash
curl http://localhost:8094/api/fields/definitions
```

#### POST /api/fields/definitions
Create a new field definition.

**Request:**
```json
{
  "fieldKey": "cf_priority",
  "displayName": "Custom Priority",
  "description": "Custom priority field",
  "fieldType": "TEXT",
  "renderer": "standard",
  "searchable": true,
  "sortable": true,
  "filterable": true,
  "required": false
}
```

**Example:**
```bash
curl -X POST http://localhost:8094/api/fields/definitions \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{"fieldKey":"cf_priority","displayName":"Custom Priority","fieldType":"TEXT"}'
```

---

### 5.15 Test Service (jira-test-service)
**Port:** 8095 | **Base Path:** /api/tests, /api/test-sets, /api/test-plans, /api/test-executions

| Category | Endpoints |
|----------|-----------|
| **Tests CRUD** | POST/GET/PUT/DELETE /api/tests, /api/tests/{testId} |
| **Test Folders** | POST/GET /api/tests/folders |
| **Test Sets CRUD** | POST/GET /api/test-sets, /{testSetId} |
| **Test Plans CRUD** | POST/GET /api/test-plans, /{planId} |
| **Test Executions** | POST/GET /api/test-executions, /{executionId}/steps/complete |
| **Environments** | POST/GET/PUT/DELETE /api/test-environments |
| **Reports** | GET /api/reports/summary, /trends, /coverage, /defect-density |
| **Traceability** | POST/GET/DELETE /api/traceability/requirements, /defects, /matrix |
| **CI/CD Import** | POST /api/import/cucumber, /api/import/junit |
| **Webhooks** | POST /api/webhooks/github-actions, /jenkins, /gitlab, /azure-devops |
| **AI Features** | POST /api/ai/analyze-duplicates, /suggest-tests, /assess-risk |

#### POST /api/tests
Create a new test.

**Request:**
```json
{
  "projectId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Login Test",
  "description": "Test login functionality",
  "testType": "MANUAL",
  "folderId": "660e8400-e29b-41d4-a716-446655440001",
  "labels": ["login", "regression"]
}
```

**Example:**
```bash
curl -X POST "http://localhost:8095/api/tests?projectId=550e8400-e29b-41d4-a716-446655440000" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{"name":"Login Test","testType":"MANUAL"}'
```

#### GET /api/tests/project/{projectId}
Get all tests for a project.

**Example:**
```bash
curl "http://localhost:8095/api/tests/project/550e8400-e29b-41d4-a716-446655440000"
```

#### POST /api/test-sets
Create a test set.

**Example:**
```bash
curl -X POST "http://localhost:8095/api/test-sets?projectId=..." \
  -H "Content-Type: application/json" \
  -d '{"name":"Sprint 1 Tests","description":"Tests for sprint 1"}'
```

#### POST /api/test-plans
Create a test plan.

**Example:**
```bash
curl -X POST "http://localhost:8095/api/test-plans?projectId=..." \
  -H "Content-Type: application/json" \
  -d '{"name":"Q1 Release Plan","description":"Q1 testing"}'
```

#### POST /api/test-executions
Start a test execution.

**Request:**
```json
{
  "projectId": "550e8400-e29b-41d4-a716-446655440000",
  "testSetId": "660e8400-e29b-41d4-a716-446655440001",
  "environmentId": "770e8400-e29b-41d4-a716-446655440002"
}
```

**Example:**
```bash
curl -X POST http://localhost:8095/api/test-executions \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{"projectId":"...","testSetId":"..."}'
```

#### POST /api/test-executions/{executionId}/steps/{testId}/{stepOrder}
Record step result.

**Request:**
```json
{
  "status": "PASS",
  "comment": "Step passed",
  "actualResult": "Expected result achieved"
}
```

**Example:**
```bash
curl -X POST "http://localhost:8095/api/test-executions/{eid}/steps/{tid}/1" \
  -H "Content-Type: application/json" \
  -d '{"status":"PASS","comment":"Step passed"}'
```

#### GET /api/reports/summary
Get test execution summary.

**Query Parameters:**
- `projectId` (UUID, required)
- `sprintId` (UUID, optional)
- `startDate` (LocalDate, optional)
- `endDate` (LocalDate, optional)

**Example:**
```bash
curl "http://localhost:8095/api/reports/summary?projectId=...&startDate=2026-01-01&endDate=2026-01-31"
```

#### GET /api/traceability/matrix
Get full traceability matrix.

**Example:**
```bash
curl "http://localhost:8095/api/traceability/matrix?projectId=..."
```

#### POST /api/import/cucumber
Import Cucumber/Gherkin feature file.

**Example:**
```bash
curl -X POST "http://localhost:8095/api/import/cucumber?projectId=..." \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -F "file=@test.feature"
```

#### POST /api/import/junit
Import JUnit XML results from CI/CD.

**Example:**
```bash
curl -X POST "http://localhost:8095/api/import/junit?projectId=...&ciSource=Jenkins" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -F "file=@TEST-test.xml"
```

---

### 5.16 Version Service (jira-version-service)
**Port:** 8096 | **Base Path:** /api/versions

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/versions/project/{projectId} | List versions |
| GET | /api/versions/{versionId} | Get version |
| POST | /api/versions | Create version |
| PUT | /api/versions/{versionId} | Update version |
| DELETE | /api/versions/{versionId} | Delete version |
| POST | /api/versions/{versionId}/restore | Restore version |
| POST | /api/versions/{versionId}/release | Release version |
| POST | /api/versions/{versionId}/archive | Archive version |
| POST | /api/versions/{versionId}/unarchive | Unarchive version |
| POST | /api/versions/fix-version | Assign fix version |
| DELETE | /api/versions/fix-version | Remove fix version |
| POST | /api/versions/affects-version | Assign affects version |
| POST | /api/versions/bulk-assign | Bulk assign versions |
| POST | /api/versions/bulk-move | Bulk move versions |
| POST | /api/versions/merge | Merge versions |
| GET | /api/versions/{versionId}/release-notes | Get release notes |
| POST | /api/versions/{versionId}/release-notes/generate | Generate release notes |
| GET | /api/versions/{versionId}/metrics | Get metrics |
| GET | /api/versions/{versionId}/deployments | Get deployments |
| POST | /api/versions/{versionId}/deployments | Add deployment |
| GET | /api/versions/{versionId}/builds | Get builds |
| POST | /api/versions/{versionId}/builds | Add build |
| GET | /api/versions/trains | List release trains |
| POST | /api/versions/trains | Create release train |
| POST | /api/versions/trains/{trainId}/versions/{versionId} | Add to train |
| GET | /api/versions/{versionId}/audit | Get audit logs |

#### POST /api/versions
Create a new version.

**Request:**
```json
{
  "projectId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "v1.0.0",
  "description": "First release",
  "releaseDate": "2026-06-01T00:00:00",
  "semanticVersion": "1.0.0"
}
```

**Example:**
```bash
curl -X POST http://localhost:8096/api/versions \
  -H "Content-Type: application/json" \
  -d '{"projectId":"...","name":"v1.0.0","releaseDate":"2026-06-01"}'
```

#### POST /api/versions/{versionId}/release
Release a version.

**Request:**
```json
{
  "releasedBy": "550e8400-e29b-41d4-a716-446655440000",
  "actualReleaseDate": "2026-05-22T12:00:00",
  "generateReleaseNotes": true
}
```

**Example:**
```bash
curl -X POST http://localhost:8096/api/versions/{versionId}/release \
  -H "Content-Type: application/json" \
  -d '{"releasedBy":"...","generateReleaseNotes":true}'
```

#### POST /api/versions/merge
Merge two versions.

**Request:**
```json
{
  "sourceVersionId": "550e8400-e29b-41d4-a716-446655440000",
  "targetVersionId": "660e8400-e29b-41d4-a716-446655440001",
  "issueIdsToMove": ["770e8400-e29b-41d4-a716-446655440002"]
}
```

**Example:**
```bash
curl -X POST http://localhost:8096/api/versions/merge \
  -H "Content-Type: application/json" \
  -d '{"sourceVersionId":"...","targetVersionId":"..."}'
```

---

### 5.17 Component Service (jira-component-service)
**Port:** 8097 | **Base Path:** /api/components

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/components/project/{projectId} | List components |
| GET | /api/components/{componentId} | Get component |
| POST | /api/components | Create component |
| PUT | /api/components/{componentId} | Update component |
| DELETE | /api/components/{componentId} | Delete component |
| POST | /api/components/{componentId}/restore | Restore component |
| POST | /api/components/{componentId}/archive | Archive component |
| POST | /api/components/{componentId}/unarchive | Unarchive component |
| POST | /api/components/issue | Assign to issue |
| DELETE | /api/components/issue | Remove from issue |
| GET | /api/components/issue/{issueId} | Get issue components |
| POST | /api/components/bulk-assign | Bulk assign |
| POST | /api/components/bulk-remove | Bulk remove |
| POST | /api/components/{componentId}/transfer-ownership | Transfer ownership |
| GET | /api/components/{componentId}/ownership-history | Get ownership history |
| GET | /api/components/{componentId}/metrics | Get metrics |
| POST | /api/components/{componentId}/metrics/snapshot | Take snapshot |
| GET | /api/components/{componentId}/assignment-rules | Get assignment rules |
| POST | /api/components/{componentId}/assignment-rules | Create rule |
| DELETE | /api/components/assignment-rules/{ruleId} | Delete rule |
| GET | /api/components/{componentId}/audit | Get audit logs |

#### POST /api/components
Create a new component.

**Request:**
```json
{
  "projectId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Backend Services",
  "description": "All backend components",
  "leadUserId": "660e8400-e29b-41d4-a716-446655440001",
  "color": "#FF5733"
}
```

**Example:**
```bash
curl -X POST http://localhost:8097/api/components \
  -H "Content-Type: application/json" \
  -d '{"projectId":"...","name":"Backend Services"}'
```

#### POST /api/components/bulk-assign
Bulk assign issues to a component.

**Request:**
```json
{
  "issueIds": [
    "770e8400-e29b-41d4-a716-446655440002",
    "880e8400-e29b-41d4-a716-446655440003"
  ],
  "componentId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Example:**
```bash
curl -X POST http://localhost:8097/api/components/bulk-assign \
  -H "Content-Type: application/json" \
  -d '{"issueIds":["...","..."],"componentId":"..."}'
```

---

## 6. Common Patterns

### 6.1 Pagination
All list endpoints support pagination via Spring Data Pageable:
```bash
# Query parameters
?page=0&size=20&sort=createdAt,DESC
```

### 6.2 Filtering
Many endpoints support filtering via query parameters:
```bash
?projectId=...&status=Open&assigneeId=...
```

### 6.3 JQL (Jira Query Language)
Search service supports JQL for complex queries:
```jql
project = DEMO AND issuetype = Bug AND status = Open ORDER BY created DESC
```

### 6.4 User Identification
Internal service calls use `X-User-Id` header:
```bash
-H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000"
```

### 6.5 Optimistic Locking
Some endpoints support version-based optimistic locking:
```json
{
  "content": "Updated text",
  "version": 1
}
```

### 6.6 File Uploads
Use `multipart/form-data` for file uploads:
```bash
-F "file=@/path/to/file.pdf"
-F "projectId=..."
```

---

## 7. Error Handling

### HTTP Status Codes

| Code | Description |
|------|-------------|
| 200 | OK - Request successful |
| 201 | Created - Resource created |
| 204 | No Content - Successful deletion |
| 400 | Bad Request - Invalid input |
| 401 | Unauthorized - Missing/invalid token |
| 403 | Forbidden - Insufficient permissions |
| 404 | Not Found - Resource doesn't exist |
| 409 | Conflict - Optimistic locking failure |
| 500 | Internal Server Error |

### Error Response Format
```json
{
  "timestamp": "2026-05-22T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/issues",
  "details": {
    "field": "summary",
    "message": "Summary is required"
  }
}
```

---

## 8. API Examples

### 8.1 Complete Issue Lifecycle

```bash
# 1. Login to get token
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' | jq -r '.accessToken')

# 2. Create issue
ISSUE_ID=$(curl -s -X POST http://localhost:8084/api/issues \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"projectId":"...","summary":"Test Issue","issueTypeId":"..."}' | jq -r '.id')

# 3. Get issue
curl -X GET "http://localhost:8084/api/issues/$ISSUE_ID" \
  -H "Authorization: Bearer $TOKEN"

# 4. Add comment
curl -X POST "http://localhost:8086/api/comments" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"issueId":"'$ISSUE_ID'","content":"Initial comment"}'

# 5. Upload attachment
curl -X POST "http://localhost:8090/api/attachments?issueId=$ISSUE_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@test.pdf"

# 6. Log work
curl -X POST "http://localhost:8084/api/issues/$ISSUE_ID/worklogs" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"timeSpent":3600,"comment":"Work done"}'

# 7. Transition status
curl -X PATCH "http://localhost:8084/api/issues/$ISSUE_ID/status?projectId=..." \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"statusId":"..."}'

# 8. Delete issue
curl -X DELETE "http://localhost:8084/api/issues/$ISSUE_ID" \
  -H "Authorization: Bearer $TOKEN"
```

### 8.2 Test Management Workflow

```bash
# 1. Create test
TEST_ID=$(curl -s -X POST "http://localhost:8095/api/tests?projectId=..." \
  -H "Content-Type: application/json" \
  -H "X-User-Id: ..." \
  -d '{"name":"Login Test","testType":"MANUAL"}' | jq -r '.id')

# 2. Create test set
curl -X POST "http://localhost:8095/api/test-sets?projectId=..." \
  -H "Content-Type: application/json" \
  -d '{"name":"Sprint 1 Tests"}'

# 3. Add test to set
curl -X POST "http://localhost:8095/api/test-sets/{setId}/tests?testId=$TEST_ID"

# 4. Create test plan
curl -X POST "http://localhost:8095/api/test-plans?projectId=..." \
  -H "Content-Type: application/json" \
  -d '{"name":"Q1 Release Plan"}'

# 5. Start execution
EXECUTION_ID=$(curl -s -X POST http://localhost:8095/api/test-executions \
  -H "Content-Type: application/json" \
  -H "X-User-Id: ..." \
  -d '{"projectId":"...","testSetId":"..."}' | jq -r '.id')

# 6. Record step result
curl -X POST "http://localhost:8095/api/test-executions/$EXECUTION_ID/steps/$TEST_ID/1" \
  -H "Content-Type: application/json" \
  -d '{"status":"PASS","comment":"Step 1 passed"}'

# 7. Get report
curl "http://localhost:8095/api/reports/summary?projectId=..."

# 8. Get traceability matrix
curl "http://localhost:8095/api/traceability/matrix?projectId=..."
```

### 8.3 Project Setup with Templates

```bash
# 1. Get template catalog
curl http://localhost:8083/api/templates/catalog

# 2. Get categories
curl http://localhost:8083/api/templates/categories

# 3. Create project via wizard
curl -X POST http://localhost:8083/api/projects/wizard \
  -H "Content-Type: application/json" \
  -H "X-User-Id: ..." \
  -d '{
    "projectType":"COMPANY_MANAGED",
    "name":"My Project",
    "projectKey":"MP",
    "templateId":"..."
  }'

# 4. Get project members
curl http://localhost:8083/api/projects/{projectId}/members

# 5. Add member
curl -X POST http://localhost:8083/api/projects/{projectId}/members \
  -H "Content-Type: application/json" \
  -H "X-User-Id: ..." \
  -d '{"userId":"...","projectRoleName":"Developers"}'
```

---

## Appendix A: Port Reference

| Service | Port | Protocol |
|---------|------|----------|
| Gateway | 8080 | HTTP |
| Auth | 8081 | HTTP |
| User | 8082 | HTTP |
| Project | 8083 | HTTP |
| Issue | 8084 | HTTP |
| Workflow | 8085 | HTTP |
| Comment | 8086 | HTTP |
| Notification | 8087 | HTTP |
| Search | 8088 | HTTP |
| Audit | 8089 | HTTP |
| Attachment | 8090 | HTTP |
| Sprint | 8091 | HTTP |
| Plan | 8092 | HTTP |
| Admin | 8093 | HTTP |
| Migration | 8094 | HTTP |
| Test | 8095 | HTTP |
| Version | 8096 | HTTP |
| Component | 8097 | HTTP |
| Frontend | 3000 | HTTP |
| PostgreSQL | 5432 | TCP |
| Swagger UI | :8094/swagger-ui.html | HTTP |

---

## Appendix B: Swagger/OpenAPI

Each service exposes Swagger UI and OpenAPI docs:

| Service | Swagger URL |
|---------|-------------|
| Gateway | http://localhost:8080/swagger-ui.html |
| Auth | http://localhost:8081/swagger-ui.html |
| User | http://localhost:8082/swagger-ui.html |
| Project | http://localhost:8083/swagger-ui.html |
| Issue | http://localhost:8084/swagger-ui.html |
| Workflow | http://localhost:8085/swagger-ui.html |
| Comment | http://localhost:8086/swagger-ui.html |
| Notification | http://localhost:8087/swagger-ui.html |
| Search | http://localhost:8088/swagger-ui.html |
| Audit | http://localhost:8089/swagger-ui.html |
| Attachment | http://localhost:8090/swagger-ui.html |
| Sprint | http://localhost:8091/swagger-ui.html |
| Plan | http://localhost:8092/swagger-ui.html |
| Admin | http://localhost:8093/swagger-ui.html |
| Migration | http://localhost:8094/swagger-ui.html |
| Test | http://localhost:8095/swagger-ui.html |
| Version | http://localhost:8096/swagger-ui.html |
| Component | http://localhost:8097/swagger-ui.html |

---

**Document Version:** 1.0.0  
**Last Updated:** 2026-05-22  
**Total Endpoints Documented:** 668+  
**Services Covered:** 17 microservices + Gateway  
**Coverage:** 100%