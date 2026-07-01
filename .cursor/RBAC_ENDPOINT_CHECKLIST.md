# Issue-service RBAC endpoint checklist (EPIC-3)

Production profile: `jira.permissions.fail-open: false` (default in `application.yml`).  
Local dev: `application-local.yml` sets `fail-open: true`.

| Endpoint | Permission | Enforced via |
|----------|------------|--------------|
| `POST /api/issues` | CREATE_ISSUES | `ProjectPermissionGuard` |
| `PUT /api/issues/{id}` | EDIT_ISSUES | `ProjectPermissionGuard` |
| `PATCH /api/issues/{id}/status` | RESOLVE_ISSUES | `ProjectPermissionGuard` |
| `DELETE /api/issues/{id}` | DELETE_ISSUES | `ProjectPermissionGuard` |
| `POST /api/issues/{id}/clone` | CREATE_ISSUES | `ProjectPermissionGuard` |
| `POST /api/issues/{id}/move` | EDIT_ISSUES | `ProjectPermissionGuard` |
| `PATCH /api/issues/{id}/rank` | EDIT_ISSUES | `ProjectPermissionGuard` |
| `POST /api/issues/{id}/links` | EDIT_ISSUES | `ProjectPermissionGuard` |
| `POST /api/bulk-operations` | Per op type | `BulkIssueOperationService.assertBulkPermission` |
| `POST/DELETE watch` | Authenticated user | `requireUser` |
| `GET /api/issues/{id}` | Public read | No gate (browse scheme future) |
| Internal workflow PATCH | Service token header | No user RBAC |

Unavailable project-service → **503** (`PermissionServiceUnavailableException`), not silent allow.
