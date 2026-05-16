# ENTERPRISE DOMAIN COMPLETENESS AUDIT REPORT
## Jira Platform - Database-to-Application Gap Analysis

**Audit Date:** 2026-05-16
**Audit Scope:** 14 Microservices, 50+ Database Tables
**Audit Methodology:** Database schema as single source of truth

---

# EXECUTIVE SUMMARY

| Service | DB Tables | Entities | Coverage | Priority Issues |
|---------|-----------|----------|----------|-----------------|
| jira-issue-service | 11 | 17 | 85% | Missing environment/version in DTOs |
| jira-project-service | 14 | 11 | 65% | Missing SecurityLevel, ProjectRoleMember, PermissionGrant repository |
| jira-workflow-service | 3 | 12+ | 95% | Complete |
| jira-sprint-service | 7 | 6+ | 75% | Duplicate AgileBoard, missing board_configs repo |
| jira-plan-service | 25+ | 22 | 90% | Schema tables missing (sprint_snapshots, velocity_history) |
| jira-auth-service | 10 | 2 | 20% | 8 missing entities (AdminUser, UserGroups, Settings, etc.) |
| jira-user-service | 4 | 9 | 95% | Minor gaps only |
| jira-comment-service | 1 | 1 | 65% | Missing internal field, pagination |
| jira-notification-service | 2 | 2 | 100% | Complete |
| jira-search-service | 1 | 1 | 85% | tsvector type mapping issue |
| jira-audit-service | 1 | 1 | 90% | JSONB mapping as String |
| jira-attachment-service | 1 | 1 | 80% | Missing thumbnail_path, mime_type_detected |
| jira-admin-service | N/A | 15+ | 40% | Multi-schema ownership confusion |
| jira-migration-service | N/A | 14+ | N/A | NO SCHEMA DEFINED |

---

# CRITICAL ISSUES (P0 - Must Fix)

## 1. SPRINT OWNERSHIP CONFLICT (Architecture)

**Issue:** Two services managing separate sprint tables - `jira_sprint.sprints` and `jira_plan.sprints`

**Impact:** Data fragmentation, inconsistent sprint lifecycle, user confusion

| Service | Table | Sprint Features |
|---------|-------|-----------------|
| jira-sprint-service | jira_sprint.sprints | Simple: name, goal, dates, status |
| jira-plan-service | jira_plan.sprints | Rich: velocity, committed/completed points, WIP limits, burndown |

**Recommendation:**
- **Option A:** Deprecate jira_sprint.sprints, migrate to jira_plan.sprints
- **Option B:** Define clear ownership - simple projects use jira-sprint-service, advanced use jira-plan-service
- **Option C:** Create Sprint Federation with interface abstraction

---

## 2. MISSING jira_migration SCHEMA (Schema Definition)

**Issue:** No `jira_migration` schema defined in consolidated SQL

**Tables Missing:**
- backup_entities
- field_mappings, user_mappings, project_mappings
- csv_templates
- migration_jobs
- entity_statuses, job_claims
- distributed_locks, leader_elections
- cluster_nodes, node_states
- dlq_entries

**Action Required:** Create V2__migration_schema.sql migration

---

## 3. jira-auth-service INCOMPLETE (20% Coverage)

**Missing Entities (8/10):**
- [ ] UserGroup (jira_auth.user_groups)
- [ ] UserGroupMember (jira_auth.user_group_members)
- [ ] AdminUser (jira_auth.admin_users)
- [ ] UserPreference (jira_auth.user_preferences)
- [ ] SystemSetting (jira_auth.system_settings)
- [ ] AppearanceSetting (jira_auth.appearance_settings)
- [ ] License (jira_auth.licenses)

**Missing Repositories:**
- UserGroupRepository
- UserGroupMemberRepository
- AdminUserRepository
- UserPreferenceRepository
- SystemSettingRepository
- AppearanceSettingRepository
- LicenseRepository

**Missing DTOs:** All admin-related DTOs

---

# HIGH PRIORITY ISSUES (P1)

## 4. jira-project-service - Missing Core Entities

**Missing Entities:**
- SecurityLevel (for security_levels table)
- SecurityLevelMember (for security_level_members table)
- ProjectRoleMember (for project_role_members table - distinct from ProjectMember)

**Missing Repository:**
- PermissionGrantRepository (entity exists but no data access)

**Missing Service Methods:**
- PermissionSchemeService CRUD
- IssueTypeSchemeService CRUD
- WorkflowSchemeService CRUD
- ScreenSchemeService CRUD
- SecurityLevelService CRUD

---

## 5. jira-attachment-service - Missing Columns

**Missing from Entity:**
- `thumbnail_path` - for image previews
- `mime_type_detected` - actual detected vs declared MIME type

---

## 6. jira-comment-service - Missing Fields

**Missing from Entity:**
- `internal` field - for internal comments (visible to developers only)

**Missing from DTO/Service:**
- getCommentById() method
- Pagination for getCommentsByIssueId()
- User-based comment queries
- Hard delete / restore functionality

---

# MEDIUM PRIORITY ISSUES (P2)

## 7. jira-issue-service - DTO Incompleteness

**Missing from IssueResponse DTO:**
- `environment` field
- `version` field (optimistic locking)

**Missing from LabelResponse:**
- `color` field
- `description` field

**Missing from WorklogResponse:**
- `time_spent_display` field
- `updated_at` field

**Missing API Endpoints:**
- Direct watcher CRUD endpoints
- Direct vote CRUD endpoints
- ChangeGroup/ChangeItem direct endpoints

---

## 8. jira-sprint-service - Duplicate/Duplicated Entities

**Duplicate AgileBoard:**
- `com.jira.sprint.entity.AgileBoard`
- `com.jira.board.entity.AgileBoard`

**Missing Repository:**
- board_configs repository for jira_sprint.board_configs table

---

## 9. jira-plan-service - Schema Tables Missing

**Tables referenced but not in schema:**
- jira_plan.sprint_snapshots (entity: SprintSnapshot)
- jira_plan.velocity_history (entity: VelocityHistory)

---

## 10. jira-admin-service - Multi-Schema Confusion

**Issue:** Tables scattered across multiple schemas without clear ownership

| Entity | Table | Schema |
|--------|-------|--------|
| UserEntity | admin_users | jira_auth |
| SystemSettingsEntity | system_settings | jira_auth |
| AppearanceEntity | appearance_settings | jira_auth |
| ProjectEntity | projects | jira_project |
| ProjectRoleEntity | project_roles | jira_project |
| NotificationSchemeEntity | (not in schema) | N/A |
| FieldConfigurationEntity | (not in schema) | N/A |

---

# LOW PRIORITY ISSUES (P3)

## 11. Type Mapping Issues

**jira-search-service:**
- `search_vector` tsvector mapped as String

**jira-audit-service:**
- `changes` JSONB mapped as String

---

## 12. jira-issue-service IssueLink Naming Mismatch

| DB Column | Entity Field |
|-----------|--------------|
| `target_issue_id` | `destination_issue_id` |

**Impact:** Potential JPA mapping issues

---

# ENTITY COMPLETENESS MATRIX

## jira-issue-service

| Entity | DB Columns | Entity Fields | Missing | DTO Coverage | API |
|--------|------------|---------------|---------|--------------|-----|
| Issue | 30 | 31 | 0 | 96% | 85% |
| Label | 8 | 8 | 0 | 75% | 75% |
| Worklog | 9 | 9 | 0 | 78% | 78% |
| IssueLink | 7 | 7 | 0* | 86% | 60% |
| Resolution | 7 | 7 | 0 | 100% | N/A |
| ProjectVersion | 12 | 13 | 0 | 100% | 80% |
| ProjectComponent | 10 | 10 | 0 | 100% | 80% |
| Watcher | 4 | 4 | 0 | N/A | 25% |
| Vote | 4 | 4 | 0 | N/A | 25% |
| ChangeGroup | 5 | 5 | 0 | 100% | 50% |
| ChangeItem | 9 | 9 | 0 | 100% | 50% |

*IssueLink has naming mismatch: `target_issue_id` (DB) vs `destination_issue_id` (Entity)

---

## jira-project-service

| Entity | DB Table | Missing | Status |
|--------|----------|---------|--------|
| Project | projects | 0 | Complete |
| ProjectRole | project_roles | is_default | Incomplete |
| ProjectMember | project_members | 0 | Complete |
| PermissionScheme | permission_schemes | 0 | Complete (no CRUD) |
| PermissionGrant | permission_grants | 0 | No Repository |
| IssueTypeScheme | issue_type_schemes | 0 | Complete (no CRUD) |
| WorkflowScheme | workflow_schemes | 0 | Complete (no CRUD) |
| ScreenScheme | screen_schemes | 0 | Complete (no CRUD) |
| SecurityLevel | security_levels | ENTIRE ENTITY | MISSING |
| SecurityLevelMember | security_level_members | ENTIRE ENTITY | MISSING |
| ProjectRoleMember | project_role_members | ENTIRE ENTITY | MISSING |

---

# DATABASE vs APPLICATION COMPARISON

## Issues Table (jira_issue.issues) - 30 columns

| Category | Column | Entity | DTO | API |
|----------|--------|--------|-----|-----|
| Core | id | ✓ | ✓ | ✓ |
| Core | project_id | ✓ | ✓ | ✓ |
| Core | issue_key | ✓ | ✓ | ✓ |
| Core | title | ✓ | ✓ | ✓ |
| Core | description | ✓ | ✓ | ✓ |
| Status | status | ✓ | ✓ | ✓ |
| Priority | priority | ✓ | ✓ | ✓ |
| Type | issue_type | ✓ | ✓ | ✓ |
| People | reporter_id | ✓ | ✓ | ✓ |
| People | assignee_id | ✓ | ✓ | ✓ |
| Hierarchy | parent_issue_id | ✓ | ✓ | ✓ |
| Epic | epic_id | ✓ | ✓ | ✓ |
| Epic | epic_name | ✓ | ✓ | ✓ |
| Epic | epic_color | ✓ | ✓ | ✓ |
| Security | security_level_id | ✓ | ✓ | ✓ |
| Versions | affects_versions[] | ✓ | ✓ | ✓ |
| Versions | fix_versions[] | ✓ | ✓ | ✓ |
| Agile | story_points | ✓ | ✓ | ✓ |
| Agile | rank | ✓ | ✓ | ✓ |
| Time | original_estimate | ✓ | ✓ | ✓ |
| Time | remaining_estimate | ✓ | ✓ | ✓ |
| Time | time_spent | ✓ | ✓ | ✓ |
| Resolution | resolution_id | ✓ | ✓ | ✓ |
| Resolution | resolution_date | ✓ | ✓ | ✓ |
| Dates | due_date | ✓ | ✓ | ✓ |
| Social | vote_count | ✓ | ✓ | ✓ |
| Social | watcher_count | ✓ | ✓ | ✓ |
| Extra | environment | ✓ | **MISSING** | **MISSING** |
| Extra | creator_id | ✓ | ✓ | ✓ |
| Extra | last_viewed_at | ✓ | ✓ | ✓ |
| Extra | version | ✓ | **MISSING** | **MISSING** |

**Coverage: 28/30 (93%)**

---

# RELATIONSHIP VALIDATION REPORT

## Foreign Key Relationships

| Relationship | DB Enforcement | App Implementation | Status |
|--------------|---------------|-------------------|--------|
| Issue → Project | FK project_id | Service-level | OK |
| Issue → Status | FK status | Eager load | OK |
| Issue → Priority | FK priority | Eager load | OK |
| Issue → IssueType | FK issue_type | Eager load | OK |
| Issue → Parent Issue | FK parent_issue_id | Hierarchy supported | OK |
| Comment → Issue | FK issue_id | Cascade delete | OK |
| Worklog → Issue | FK issue_id | Cascade delete | OK |
| Attachment → Issue | FK issue_id | Cascade delete | OK |
| Label → Issue | FK issue_id | Cascade delete | OK |
| Project → Lead User | FK lead_user_id | Service-level | OK |
| Sprint → Project | FK project_id | Service-level | **CONFLICT** |
| Board → Project | FK project_id | Service-level | OK |
| BoardColumn → Board | FK board_id | Cascade delete | OK |
| ProjectMember → Project | FK project_id | Cascade delete | OK |

---

# IMPLEMENTATION CORRECTION PLAN

## Phase 1: Critical Fixes (Week 1)

### 1.1 Add internal field to Comment entity
```
// Comment.java
@Column(name = "internal")
@Builder.Default
private Boolean internal = false;
```

### 1.2 Add environment/version to IssueResponse DTO
```java
// IssueResponse.java
private String environment;
private Long version;
```

### 1.3 Add missing Attachment fields
```java
// Attachment.java
@Column(name = "thumbnail_path")
private String thumbnailPath;

@Column(name = "mime_type_detected")
private String mimeTypeDetected;
```

---

## Phase 2: High Priority (Week 2)

### 2.1 Create jira-auth-service missing entities
- UserGroup, UserGroupMember, AdminUser
- UserPreference, SystemSetting, AppearanceSetting, License

### 2.2 Create jira-project-service missing entities
- SecurityLevel, SecurityLevelMember, ProjectRoleMember
- PermissionGrantRepository

### 2.3 Fix IssueLink naming mismatch
- Rename `destinationIssueId` to `targetIssueId` in IssueLink entity

---

## Phase 3: Architecture (Week 3-4)

### 3.1 Resolve Sprint Ownership
- Decision: Deprecate jira-sprint-service or establish clear boundaries
- Migrate data if consolidating
- Update API gateway routing

### 3.2 Add jira_migration schema
- Create V2__migration_schema.sql
- Define all migration-specific tables

---

## Phase 4: Polish (Week 5)

### 4.1 Add pagination to comment service
### 4.2 Add direct watcher/vote endpoints
### 4.3 Fix type mappings (tsvector, JSONB)
### 4.4 Consolidate AgileBoard entities

---

# FINAL SCORECARD

| Service | Completeness | Critical Gaps | Action Required |
|---------|-------------|---------------|-----------------|
| jira-issue-service | 85% | 2 DTO fields | Quick fix |
| jira-project-service | 65% | 3 entities + repo | Medium effort |
| jira-workflow-service | 95% | None | None |
| jira-sprint-service | 75% | Duplicate entity, repo | Medium effort |
| jira-plan-service | 90% | Schema tables | Quick fix |
| jira-auth-service | 20% | 8 entities | High effort |
| jira-user-service | 95% | 1 minor | None |
| jira-comment-service | 65% | internal field, pagination | Quick fix |
| jira-notification-service | 100% | None | None |
| jira-search-service | 85% | Type mapping | Quick fix |
| jira-audit-service | 90% | Type mapping | Quick fix |
| jira-attachment-service | 80% | 2 columns | Quick fix |
| jira-admin-service | 40% | Schema clarity | High effort |
| jira-migration-service | N/A | No schema | Schema needed |

**Overall Platform Completeness: ~70%**

---

# RECOMMENDATIONS

1. **Immediate:** Fix P0 issues (sprint conflict, auth missing entities, migration schema)
2. **This Sprint:** Add missing DTO fields and entity columns
3. **Next Sprint:** Create missing entities and repositories
4. **Next Quarter:** Architecture refactor for sprint ownership

---

*Report generated by Enterprise Domain Completeness Audit*
*Database is the single source of truth*