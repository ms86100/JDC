# Hardcoded Data Audit Report

**Date:** 2026-07-25  
**Auditor:** Senior Code Auditor (Automated Deep Scan)  
**Scope:** 24 Microservices — Full Java Source, SQL Migrations, Configuration Files  
**Total Findings:** 913 hardcoded business data instances  
**Severity Distribution:** 297 HIGH | 393 MEDIUM | 223 LOW

---

## Executive Summary

The codebase contains **913 instances of hardcoded business data** across 24 microservices. The most critical areas are:

1. **Status values** scattered across 14 services with duplicated substring-matching logic
2. **Default values** baked into entities, migrations, and service classes
3. **Business lists** (board columns, quick filters, templates) hardcoded in Java and SQL
4. **Role/Permission names** used as string literals for authorization checks
5. **Colors** hardcoded for statuses, priorities, and board columns
6. **Error messages** not externalized for i18n

---

## 1. Findings Inventory by Category

### 1.1 HARDCODED_DEFAULTS — 233 findings (38 HIGH)

| Service | File | Line | Hardcoded Value | Impact |
|---------|------|------|----------------|--------|
| jira-issue-service | IssueService.java | 65 | `DEFAULT_STATUS_ID = UUID("00000000-0000-0000-0001-000000000002")`, `DEFAULT_TYPE_ID`, `DEFAULT_PRIORITY_ID` — magic UUIDs for "To Do", "Bug", "Medium" | HIGH |
| jira-issue-service | CloneIssueService.java | 31 | `DEFAULT_STATUS_ID = UUID("00000000-0000-0000-0001-000000000002")` — duplicated magic UUID | HIGH |
| jira-issue-service | TestManagementService.java | 96 | `"MANUAL"` default test type, `"DRAFT"` default test status | MEDIUM |
| jira-issue-service | TestManagementService.java | 231-490 | `"DRAFT"`, `"OPEN"`, `"RUNNING"`, `"COVERED"` — default statuses for test entities | MEDIUM |
| jira-issue-service | ReportingService.java | 255 | Risk level thresholds: `>5 = HIGH`, `>2 = MEDIUM`, else `LOW` | MEDIUM |
| jira-issue-service | ReportingService.java | 317 | Quality thresholds: `>=95% = EXCELLENT`, `>=85% = GOOD`, `>=70% = NEEDS_IMPROVEMENT`, else `CRITICAL` | MEDIUM |
| jira-project-service | ProjectService.java | 58 | `"PROJECT_LEAD"` default assignee type | MEDIUM |
| jira-project-service | Project.java | 69 | `"PROJECT_LEAD"` entity @Builder.Default | MEDIUM |
| jira-project-service | ProjectTemplate.java | 61 | `"SOFTWARE"` default template category | MEDIUM |
| jira-user-service | JiraUserManagementService.java | 30 | `UUID("00000000-0000-0000-0000-000000000001")` — DEFAULT_DIRECTORY_ID | HIGH |
| jira-user-service | JiraUserManagementService.java | 253-254 | `g.getGroupName().contains("administrators")` / `contains("software")` for admin detection | HIGH |
| jira-user-service | JiraUserManagementService.java | 312 | Password length hardcoded to 12 chars | MEDIUM |
| jira-sprint-service | AgileBoard.java | 37 | `boardType = "SCRUM"` default | MEDIUM |
| jira-sprint-service | DashboardService.java | 19 | Default "System Dashboard" with 5 hardcoded gadgets | HIGH |
| jira-workflow-service | PostFunctionExecutionEngine.java | 37 | `MAX_RETRIES = 3`, `RETRY_DELAY_MS = 100` | LOW |
| jira-workflow-service | PostFunctionExecutionEngine.java | 273 | Default email subject `"Issue Notification"` | MEDIUM |
| jira-plan-service | SprintIssue.java | 62 | `"UNCOMPLETED"`, `"COMPLETED"`, `"DROPPED"` completion statuses | HIGH |
| jira-plan-service | BacklogService.java | 67 | `"BACKLOG"` default status | MEDIUM |
| jira-plan-service | ReleaseService.java | 53-106 | `"DRAFT"`, `"APPROVED"`, `"RELEASED"` release statuses | MEDIUM |
| jira-plan-service | GoalService.java | 51-159 | `"NOT_STARTED"`, `"COMPLETED"`, `"IN_PROGRESS"` goal statuses | MEDIUM |
| **+ 213 more across all services** | | | | |

### 1.2 STATUS_VALUES — 95 findings (49 HIGH)

**Critical Pattern:** Status matching logic is duplicated across 8+ services using `status.contains("done") || status.contains("closed")`. This is the single most dangerous hardcoding pattern.

| Service | File | Line | Hardcoded Value | Impact |
|---------|------|------|----------------|--------|
| jira-sprint-service | SprintReportService.java | 48 | `"Done"`, `"Completed"`, `"In Progress"` — status classification | HIGH |
| jira-sprint-service | SprintReportService.java | 86 | `"To Do"`, `"In Progress"`, `"Done"` — report bucket names | HIGH |
| jira-sprint-service | SprintReportService.java | 336-346 | `contains("Done")`, `contains("Completed")`, `equalsIgnoreCase("Closed")` — status-to-bucket matching | HIGH |
| jira-sprint-service | SprintPlanningService.java | 384 | `contains("done")`, `contains("completed")`, `contains("closed")`, `equals("resolved")` | HIGH |
| jira-sprint-service | IssueServiceClient.java | 382 | **DUPLICATE** of above completed-status logic | HIGH |
| jira-sprint-service | BoardService.java | 442 | **TRIPLICATE** of above completed-status logic | HIGH |
| jira-sprint-service | WipLimitService.java | 211 | `done/closed/resolved/completed`, `progress/review/doing`, `todo/backlog/open/new` — column matching | HIGH |
| jira-sprint-service | BoardService.java | 660 | **DUPLICATE** column-to-status matching | HIGH |
| jira-version-service | ReleaseHubService.java | 113-115 | `contains("Done")`, `contains("Closed")`, `contains("Resolved")`, `contains("Progress")` | HIGH |
| jira-issue-service | EpicService.java | 183 | `contains("done")`, `contains("closed")`, `contains("complete")` — completion detection | HIGH |
| jira-search-service | JQLSearchService.java | 279 | `List.of("To Do", "In Progress", "In Review", "Done", "Closed", "Open", "Reopened", "Blocked")` — fallback suggestions | HIGH |
| jira-plan-service | HierarchyRollupService.java | 55 | `"DONE"`, `"IN_PROGRESS"` — rollup calculation | HIGH |
| jira-plan-service | ProgramAggregationService.java | 83 | `"DONE"` — completion filter | MEDIUM |
| jira-plan-service | SprintService.java | 797 | `"COMPLETED"`, `"IN_PROGRESS"`, `"UNCOMPLETED"` — cumulative flow | HIGH |
| jira-issue-service | V1__init.sql | 67 | `INSERT INTO issue_statuses: 'To Do', 'In Progress', 'In Review', 'Done'` with fixed UUIDs | HIGH |
| jira-issue-service | V3__issue_types_and_schemes.sql | 42 | `INSERT: 'Backlog', 'Open', 'Resolved', 'Closed', 'Defined'` | HIGH |
| jira-issue-service | V8__workflow_status_catalog_ids.sql | 9 | Re-seeds 9 statuses with canonical UUIDs | HIGH |
| jira-project-service | V4__*.sql | 293 | 8 status definitions with keys, names, colors, icons, categories | HIGH |
| jira-workflow-service | V8__*.sql | 168 | `'TODO'/'To Do'/'#0052CC'`, `'IN_PROGRESS'/'In Progress'/'#FF991F'`, `'DONE'/'Done'/'#00875A'` | HIGH |
| jira-sprint-service | V3__*.sql | 68 | `i.status IN ('DONE', 'CLOSED', 'RESOLVED')` in SQL function | HIGH |
| jira-sprint-service | V4__*.sql | 85 | `i.status IN ('DONE', 'CLOSED', 'RESOLVED')` in velocity function | HIGH |
| **+ 74 more** | | | | |

### 1.3 ISSUE_TYPES — 45 findings (28 HIGH)

| Service | File | Line | Hardcoded Value | Impact |
|---------|------|------|----------------|--------|
| jira-issue-service | V1__init.sql | 56 | `INSERT: 'Bug', 'Task', 'Story', 'Epic', 'Sub-task'` with fixed UUIDs | HIGH |
| jira-issue-service | V3__*.sql | 15 | `INSERT: 'Improvement', 'New Feature', 'Change Request', 'Incident', 'Service Request', 'Problem'` | HIGH |
| jira-project-service | V2__*.sql | 145 | `INSERT issue_type_schemes: 'Scrum Issue Types', 'Kanban Issue Types', 'Bug Tracking', etc.` | HIGH |
| jira-sprint-service | SprintReportService.java | 88 | `"Story"`, `"Bug"`, `"Task"` — issue type bucketing | HIGH |
| jira-sprint-service | SprintPlanningService.java | 348 | `"Story"`, `"Task"`, `"Bug"` — effort estimation defaults | HIGH |
| jira-search-service | JQLSearchService.java | 293 | `List.of("Bug", "Story", "Task", "Epic", "Sub-task", "Improvement", "New Feature")` — fallback | HIGH |
| jira-plan-service | HierarchyService.java | various | `"Epic"`, `"Story"`, `"Task"`, `"Bug"`, `"Sub-task"` — hierarchy levels | HIGH |
| jira-migration-service | various | various | Issue type mapping for import/export | MEDIUM |
| **+ 37 more** | | | | |

### 1.4 PRIORITY_VALUES — 20 findings (9 HIGH)

| Service | File | Line | Hardcoded Value | Impact |
|---------|------|------|----------------|--------|
| jira-issue-service | V1__init.sql | 74 | `INSERT: 'Highest', 'High', 'Medium', 'Low', 'Lowest'` with fixed UUIDs and colors | HIGH |
| jira-issue-service | V3__*.sql | 52 | `INSERT: 'Blocker', 'Critical', 'Major', 'Minor', 'Trivial'` | HIGH |
| jira-sprint-service | SprintPlanningService.java | 360 | `"Highest"`, `"High"`, `"Medium"` for priority score calculation | HIGH |
| jira-search-service | JQLSearchService.java | 286 | `List.of("Highest", "High", "Medium", "Low", "Lowest")` — fallback | HIGH |
| jira-report-service | various | various | Priority-based grouping and reporting | MEDIUM |
| **+ 15 more** | | | | |

### 1.5 ROLE_NAMES — 34 findings (17 HIGH)

| Service | File | Line | Hardcoded Value | Impact |
|---------|------|------|----------------|--------|
| jira-project-service | ProjectService.java | various | `"Administrators"`, `"Developers"`, `"Viewers"` — default project roles created inline | HIGH |
| jira-project-service | ExportImportService.java | various | Same 3 default roles duplicated | HIGH |
| jira-project-service | ProjectTypeService.java | various | Same 3 default roles triplicated | HIGH |
| jira-auth-service | various | various | `"ROLE_ADMIN"`, `"ROLE_USER"`, `"ROLE_PROJECT_ADMIN"` | HIGH |
| jira-user-service | V2__*.sql | 177 | `'jira-administrators'`, `'jira-software-users'`, `'jira-system-administrators'` | HIGH |
| jira-notification-service | various | various | `"PROJECT_LEAD"`, `"CURRENT_ASSIGNEE"`, `"REPORTER"` — recipient types | MEDIUM |
| **+ 28 more** | | | | |

### 1.6 PERMISSION_NAMES — 33 findings (18 HIGH)

| Service | File | Line | Hardcoded Value | Impact |
|---------|------|------|----------------|--------|
| jira-project-service | ProjectService.java | various | Permission lists per role: `["BROWSE_PROJECTS","CREATE_ISSUES","EDIT_ISSUES",...]` | HIGH |
| jira-auth-service | various | various | Permission string checks in authorization filters | HIGH |
| jira-gateway | various | various | Route-level permission checks with hardcoded permission names | HIGH |
| jira-workflow-service | TransitionPermissionEvaluator.java | 75 | Auto-appends `"_ISSUES"` suffix to permission keys | MEDIUM |
| **+ 29 more** | | | | |

### 1.7 HARDCODED_COLORS — 22 findings (10 HIGH)

| Service | File | Line | Hardcoded Value | Impact |
|---------|------|------|----------------|--------|
| jira-project-service | V4__*.sql | 293 | Status colors: `#4C9AFF`, `#0052CC`, `#00875A`, `#FFAB00`, `#FF5630`, `#6C757D` | HIGH |
| jira-workflow-service | V8__*.sql | 168 | Different status colors: `#0052CC`, `#FF991F`, `#00875A` (inconsistent!) | HIGH |
| jira-sprint-service | BoardService.java | 566-574 | Board column colors: `#6b778c`, `#0052cc`, `#ff8b00`, `#36b37e` | HIGH |
| jira-workflow-service | WorkflowStatusCatalog.java | 109 | `"#6C757D"` — default null-status color | LOW |
| jira-issue-service | V1__init.sql | 74 | Priority colors: `#FF5630`, `#FF7452`, `#FFAB00`, `#0065FF`, `#2684FF` | HIGH |
| **+ 17 more** | | | | |

### 1.8 LINK_TYPES — 11 findings (6 HIGH)

| Service | File | Line | Hardcoded Value | Impact |
|---------|------|------|----------------|--------|
| jira-issue-service | V5__*.sql | various | `INSERT: 'Blocks'/'is blocked by', 'Clones'/'is cloned by', 'Duplicates'/'is duplicated by', 'Relates'/'relates to'` | HIGH |
| jira-workflow-service | V23__*.sql | 225 | Post-function configs reference `'clone'`, `'is parent of'`, `'follow-up'` | HIGH |
| jira-issue-service | IssueLinkService.java | 359 | Fallback: `"Related"`, `"relates to"` | LOW |
| **+ 8 more** | | | | |

### 1.9 SPRINT_STATES — 12 findings (5 HIGH)

| Service | File | Line | Hardcoded Value | Impact |
|---------|------|------|----------------|--------|
| jira-sprint-service | various | various | `"PLANNING"`, `"ACTIVE"`, `"COMPLETED"` — sprint lifecycle states | HIGH |
| jira-plan-service | various | various | Sprint state checks with hardcoded strings | MEDIUM |
| **+ 10 more** | | | | |

### 1.10 WORKFLOW_STATES & TRANSITIONS — 38 findings (15 HIGH)

| Service | File | Line | Hardcoded Value | Impact |
|---------|------|------|----------------|--------|
| jira-workflow-service | V6__*.sql | 259 | 16 post-function definitions | HIGH |
| jira-workflow-service | V6__*.sql | 293 | 16 condition definitions | HIGH |
| jira-workflow-service | V6__*.sql | 327 | 10 validator definitions | HIGH |
| jira-workflow-service | WorkflowAdministrationService.java | 1044-1061 | Returns hardcoded lists of conditions, validators, post-functions instead of reading from DB tables that already exist | HIGH |
| jira-workflow-service | script-templates.json | 1 | 10 script templates with embedded business logic | MEDIUM |
| **+ 33 more** | | | | |

### 1.11 Other Categories Summary

| Category | Count | HIGH | Key Examples |
|----------|-------|------|-------------|
| HARDCODED_LABELS | 58 | 3 | English UI strings, fallback display names, template descriptions |
| HARDCODED_ENUM_AS_BUSINESS_DATA | 51 | 22 | Java enums used as business values (board types, field types, test types) |
| REPORT_TYPES | 12 | 5 | Chart templates, report categories seeded in SQL |
| FIELD_NAMES | 11 | 4 | Custom field type names, field configuration keys |
| PROJECT_TYPES | 10 | 4 | "COMPANY_MANAGED", "TEAM_MANAGED", template types |
| NOTIFICATION_TYPES | 10 | 4 | Event names, notification categories |
| BOARD_TYPES | 7 | 3 | "SCRUM", "KANBAN" with different column defaults |
| CUSTOM_FIELD_TYPES | 7 | 3 | "TEXT", "SELECT", "MULTI_SELECT", "DATE", etc. |
| NOTIFICATION_TEMPLATES | 5 | 2 | Email templates with hardcoded HTML/text |
| RESOLUTION_VALUES | 3 | 2 | "Fixed", "Won't Fix", "Duplicate", "Cannot Reproduce" |
| ERROR_MESSAGES | 68 | 0 | All user-facing error strings (i18n candidates) |
| HARDCODED_URLS_OR_PATHS | 23 | 8 | Inter-service URLs, API paths |

---

## 2. Master Data Configuration Design

### 2.1 Central Master Data Tables (in `jira_admin` database)

These tables belong in the **jira-admin-service** database because they are cross-cutting configuration used by multiple services.

#### Table: `master_statuses`
```sql
CREATE TABLE jira_admin.master_statuses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    status_key      VARCHAR(50) NOT NULL UNIQUE,    -- 'TODO', 'IN_PROGRESS', 'DONE'
    display_name    VARCHAR(100) NOT NULL,           -- 'To Do', 'In Progress', 'Done'
    description     VARCHAR(500),
    category        VARCHAR(30) NOT NULL,            -- 'TODO', 'IN_PROGRESS', 'DONE'
    color           VARCHAR(7) NOT NULL DEFAULT '#6C757D',
    icon            VARCHAR(50),
    sort_order      INTEGER NOT NULL DEFAULT 0,
    is_system       BOOLEAN NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_master_statuses_category ON jira_admin.master_statuses(category);
CREATE INDEX idx_master_statuses_active ON jira_admin.master_statuses(is_active);

-- Seed data (matching current hardcoded values)
INSERT INTO jira_admin.master_statuses (status_key, display_name, category, color, icon, sort_order, is_system) VALUES
('BACKLOG',     'Backlog',      'TODO',        '#6C757D', 'backlog',     0, TRUE),
('TODO',        'To Do',        'TODO',        '#4C9AFF', 'todo',        1, TRUE),
('OPEN',        'Open',         'TODO',        '#4C9AFF', 'open',        2, TRUE),
('DEFINED',     'Defined',      'TODO',        '#4C9AFF', 'defined',     3, FALSE),
('IN_PROGRESS', 'In Progress',  'IN_PROGRESS', '#0052CC', 'in-progress', 4, TRUE),
('IN_REVIEW',   'In Review',    'IN_PROGRESS', '#FF991F', 'review',      5, TRUE),
('BLOCKED',     'Blocked',      'IN_PROGRESS', '#FF5630', 'blocked',     6, FALSE),
('RESOLVED',    'Resolved',     'DONE',        '#00875A', 'resolved',    7, TRUE),
('DONE',        'Done',         'DONE',        '#00875A', 'done',        8, TRUE),
('CLOSED',      'Closed',       'DONE',        '#00875A', 'closed',      9, TRUE);
```

#### Table: `master_priorities`
```sql
CREATE TABLE jira_admin.master_priorities (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    priority_key    VARCHAR(50) NOT NULL UNIQUE,
    display_name    VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    color           VARCHAR(7) NOT NULL,
    icon_url        VARCHAR(255),
    sort_order      INTEGER NOT NULL DEFAULT 0,
    is_default      BOOLEAN NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO jira_admin.master_priorities (priority_key, display_name, color, sort_order, is_default) VALUES
('BLOCKER',  'Blocker',  '#FF0000', 0, FALSE),
('CRITICAL', 'Critical', '#FF5630', 1, FALSE),
('HIGHEST',  'Highest',  '#FF5630', 2, FALSE),
('HIGH',     'High',     '#FF7452', 3, FALSE),
('MAJOR',    'Major',    '#FF7452', 4, FALSE),
('MEDIUM',   'Medium',   '#FFAB00', 5, TRUE),
('LOW',      'Low',      '#0065FF', 6, FALSE),
('MINOR',    'Minor',    '#0065FF', 7, FALSE),
('LOWEST',   'Lowest',   '#2684FF', 8, FALSE),
('TRIVIAL',  'Trivial',  '#2684FF', 9, FALSE);
```

#### Table: `master_issue_types`
```sql
CREATE TABLE jira_admin.master_issue_types (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type_key        VARCHAR(50) NOT NULL UNIQUE,
    display_name    VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    icon            VARCHAR(50) DEFAULT 'standard',
    color           VARCHAR(7),
    is_subtask      BOOLEAN NOT NULL DEFAULT FALSE,
    is_system       BOOLEAN NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order      INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO jira_admin.master_issue_types (type_key, display_name, icon, is_subtask, is_system, sort_order) VALUES
('BUG',             'Bug',              'bug',          FALSE, TRUE,  0),
('TASK',            'Task',             'task',         FALSE, TRUE,  1),
('STORY',           'Story',            'story',        FALSE, TRUE,  2),
('EPIC',            'Epic',             'epic',         FALSE, TRUE,  3),
('SUB_TASK',        'Sub-task',         'subtask',      TRUE,  TRUE,  4),
('IMPROVEMENT',     'Improvement',      'improvement',  FALSE, FALSE, 5),
('NEW_FEATURE',     'New Feature',      'feature',      FALSE, FALSE, 6),
('CHANGE_REQUEST',  'Change Request',   'change',       FALSE, FALSE, 7),
('INCIDENT',        'Incident',         'incident',     FALSE, FALSE, 8),
('SERVICE_REQUEST', 'Service Request',  'service',      FALSE, FALSE, 9),
('PROBLEM',         'Problem',          'problem',      FALSE, FALSE, 10);
```

#### Table: `master_resolutions`
```sql
CREATE TABLE jira_admin.master_resolutions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resolution_key  VARCHAR(50) NOT NULL UNIQUE,
    display_name    VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    sort_order      INTEGER NOT NULL DEFAULT 0,
    is_default      BOOLEAN NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO jira_admin.master_resolutions (resolution_key, display_name, sort_order, is_default) VALUES
('FIXED',            'Fixed',             0, TRUE),
('WONT_FIX',         'Won''t Fix',        1, FALSE),
('DUPLICATE',        'Duplicate',         2, FALSE),
('CANNOT_REPRODUCE', 'Cannot Reproduce',  3, FALSE),
('DONE',             'Done',              4, FALSE);
```

#### Table: `master_link_types`
```sql
CREATE TABLE jira_admin.master_link_types (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    link_key        VARCHAR(50) NOT NULL UNIQUE,
    outward_name    VARCHAR(100) NOT NULL,  -- "blocks"
    inward_name     VARCHAR(100) NOT NULL,  -- "is blocked by"
    description     VARCHAR(500),
    is_system       BOOLEAN NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order      INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO jira_admin.master_link_types (link_key, outward_name, inward_name, is_system, sort_order) VALUES
('BLOCKS',     'blocks',       'is blocked by',    TRUE, 0),
('CLONES',     'clones',       'is cloned by',     TRUE, 1),
('DUPLICATES', 'duplicates',   'is duplicated by',  TRUE, 2),
('RELATES',    'relates to',   'relates to',       TRUE, 3);
```

#### Table: `master_roles`
```sql
CREATE TABLE jira_admin.master_roles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_key        VARCHAR(50) NOT NULL UNIQUE,
    display_name    VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    is_system       BOOLEAN NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO jira_admin.master_roles (role_key, display_name, description, is_system) VALUES
('ADMINISTRATORS', 'Administrators', 'Project administrators with full control', TRUE),
('DEVELOPERS',     'Developers',     'Team members who develop features',        TRUE),
('VIEWERS',        'Viewers',        'Read-only access to the project',          TRUE);
```

#### Table: `master_permissions`
```sql
CREATE TABLE jira_admin.master_permissions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    permission_key  VARCHAR(80) NOT NULL UNIQUE,
    display_name    VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    category        VARCHAR(50) NOT NULL,  -- 'PROJECT', 'ISSUE', 'COMMENT', 'ATTACHMENT', etc.
    is_system       BOOLEAN NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE jira_admin.master_role_permissions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id         UUID NOT NULL REFERENCES jira_admin.master_roles(id),
    permission_id   UUID NOT NULL REFERENCES jira_admin.master_permissions(id),
    UNIQUE(role_id, permission_id)
);

INSERT INTO jira_admin.master_permissions (permission_key, display_name, category, is_system) VALUES
('BROWSE_PROJECTS',     'Browse Projects',      'PROJECT',    TRUE),
('CREATE_ISSUES',       'Create Issues',         'ISSUE',      TRUE),
('EDIT_ISSUES',         'Edit Issues',           'ISSUE',      TRUE),
('DELETE_ISSUES',       'Delete Issues',         'ISSUE',      TRUE),
('ASSIGN_ISSUES',       'Assign Issues',         'ISSUE',      TRUE),
('TRANSITION_ISSUES',   'Transition Issues',     'ISSUE',      TRUE),
('CLOSE_ISSUES',        'Close Issues',          'ISSUE',      TRUE),
('MANAGE_SPRINTS',      'Manage Sprints',        'PROJECT',    TRUE),
('ADMINISTER_PROJECTS', 'Administer Projects',   'PROJECT',    TRUE),
('ADD_COMMENTS',        'Add Comments',          'COMMENT',    TRUE),
('EDIT_ALL_COMMENTS',   'Edit All Comments',     'COMMENT',    TRUE),
('DELETE_ALL_COMMENTS', 'Delete All Comments',   'COMMENT',    TRUE),
('CREATE_ATTACHMENTS',  'Create Attachments',    'ATTACHMENT', TRUE),
('DELETE_ALL_ATTACHMENTS','Delete All Attachments','ATTACHMENT',TRUE),
('MANAGE_WATCHERS',     'Manage Watchers',       'ISSUE',      TRUE);
```

#### Table: `master_board_types` and `master_board_column_templates`
```sql
CREATE TABLE jira_admin.master_board_types (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type_key        VARCHAR(30) NOT NULL UNIQUE,
    display_name    VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE jira_admin.master_board_column_templates (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board_type_id   UUID NOT NULL REFERENCES jira_admin.master_board_types(id),
    column_name     VARCHAR(100) NOT NULL,
    status_category VARCHAR(30) NOT NULL,
    color           VARCHAR(7),
    wip_limit       INTEGER,
    sort_order      INTEGER NOT NULL DEFAULT 0,
    status_mappings JSONB DEFAULT '[]'
);

INSERT INTO jira_admin.master_board_types (type_key, display_name) VALUES
('SCRUM',  'Scrum Board'),
('KANBAN', 'Kanban Board');

-- Scrum default columns
INSERT INTO jira_admin.master_board_column_templates (board_type_id, column_name, status_category, color, wip_limit, sort_order)
SELECT id, 'Backlog',    'TODO',        '#6c757d', NULL, 0 FROM jira_admin.master_board_types WHERE type_key='SCRUM'
UNION ALL
SELECT id, 'To Do',      'TODO',        '#6c757d', NULL, 1 FROM jira_admin.master_board_types WHERE type_key='SCRUM'
UNION ALL
SELECT id, 'In Progress','IN_PROGRESS', '#0066ff', 5,    2 FROM jira_admin.master_board_types WHERE type_key='SCRUM'
UNION ALL
SELECT id, 'In Review',  'IN_PROGRESS', '#ff9200', 3,    3 FROM jira_admin.master_board_types WHERE type_key='SCRUM'
UNION ALL
SELECT id, 'Done',       'DONE',        '#00875a', NULL, 4 FROM jira_admin.master_board_types WHERE type_key='SCRUM';

-- Kanban default columns
INSERT INTO jira_admin.master_board_column_templates (board_type_id, column_name, status_category, color, wip_limit, sort_order)
SELECT id, 'Backlog',                 'TODO',        '#6b778c', NULL, 0 FROM jira_admin.master_board_types WHERE type_key='KANBAN'
UNION ALL
SELECT id, 'Selected for Development','TODO',        '#0052cc', NULL, 1 FROM jira_admin.master_board_types WHERE type_key='KANBAN'
UNION ALL
SELECT id, 'In Progress',            'IN_PROGRESS', '#ff8b00', 5,    2 FROM jira_admin.master_board_types WHERE type_key='KANBAN'
UNION ALL
SELECT id, 'Done',                   'DONE',        '#36b37e', NULL, 3 FROM jira_admin.master_board_types WHERE type_key='KANBAN';
```

#### Table: `master_notification_events`
```sql
CREATE TABLE jira_admin.master_notification_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_key       VARCHAR(80) NOT NULL UNIQUE,
    display_name    VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    category        VARCHAR(50) NOT NULL,  -- 'Issue', 'Comment', 'Status', 'Sprint', 'Project'
    is_system       BOOLEAN NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO jira_admin.master_notification_events (event_key, display_name, category, is_system) VALUES
('ISSUE_CREATED',     'Issue Created',     'Issue',   TRUE),
('ISSUE_UPDATED',     'Issue Updated',     'Issue',   TRUE),
('ISSUE_DELETED',     'Issue Deleted',     'Issue',   TRUE),
('ISSUE_ASSIGNED',    'Issue Assigned',    'Issue',   TRUE),
('COMMENT_ADDED',     'Comment Added',     'Comment', TRUE),
('COMMENT_UPDATED',   'Comment Updated',   'Comment', TRUE),
('STATUS_CHANGED',    'Status Changed',    'Status',  TRUE),
('SPRINT_STARTED',    'Sprint Started',    'Sprint',  TRUE),
('SPRINT_COMPLETED',  'Sprint Completed',  'Sprint',  TRUE);
```

#### Table: `system_configuration`
```sql
CREATE TABLE jira_admin.system_configuration (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    config_key      VARCHAR(200) NOT NULL UNIQUE,
    config_value    TEXT NOT NULL,
    value_type      VARCHAR(20) NOT NULL DEFAULT 'STRING',  -- STRING, INTEGER, BOOLEAN, JSON, UUID
    category        VARCHAR(50) NOT NULL,
    description     VARCHAR(500),
    is_editable     BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(100)
);

CREATE INDEX idx_system_config_category ON jira_admin.system_configuration(category);
CREATE INDEX idx_system_config_key ON jira_admin.system_configuration(config_key);

-- Seed: all current hardcoded defaults
INSERT INTO jira_admin.system_configuration (config_key, config_value, value_type, category, description) VALUES
-- Issue defaults
('issue.default.status_key',        'TODO',           'STRING',  'ISSUE',    'Default status for new issues'),
('issue.default.type_key',          'BUG',            'STRING',  'ISSUE',    'Default issue type'),
('issue.default.priority_key',      'MEDIUM',         'STRING',  'ISSUE',    'Default priority'),
('issue.default.link_type_fallback','RELATES',        'STRING',  'ISSUE',    'Fallback link type display name'),
-- Project defaults
('project.default.assignee_type',   'PROJECT_LEAD',   'STRING',  'PROJECT',  'Default assignee type'),
('project.default.template_category','SOFTWARE',      'STRING',  'PROJECT',  'Default template category'),
('project.default.security_level',  'RESTRICTED',     'STRING',  'PROJECT',  'Default security level type'),
-- Sprint/Board defaults
('board.default.type',              'SCRUM',          'STRING',  'BOARD',    'Default board type'),
('sprint.default.status',           'PLANNING',       'STRING',  'SPRINT',   'Default sprint status'),
-- Test management defaults
('test.default.type',               'MANUAL',         'STRING',  'TEST',     'Default test type'),
('test.default.status',             'DRAFT',          'STRING',  'TEST',     'Default test status'),
('test.default.execution_status',   'RUNNING',        'STRING',  'TEST',     'Default execution status'),
('test.default.coverage_status',    'COVERED',        'STRING',  'TEST',     'Default coverage status'),
-- Quality thresholds
('quality.risk.high_threshold',     '5',              'INTEGER', 'QUALITY',  'Defect density threshold for HIGH risk'),
('quality.risk.medium_threshold',   '2',              'INTEGER', 'QUALITY',  'Defect density threshold for MEDIUM risk'),
('quality.pass_rate.excellent',     '95',             'INTEGER', 'QUALITY',  'Pass rate threshold for EXCELLENT'),
('quality.pass_rate.good',          '85',             'INTEGER', 'QUALITY',  'Pass rate threshold for GOOD'),
('quality.pass_rate.needs_improvement','70',           'INTEGER', 'QUALITY',  'Pass rate threshold for NEEDS_IMPROVEMENT'),
-- User defaults
('user.default.timezone',           'UTC',            'STRING',  'USER',     'Default timezone for new users'),
('user.default.password_length',    '12',             'INTEGER', 'USER',     'Auto-generated password length'),
('ldap.default.user_search_filter', '(objectClass=person)', 'STRING', 'LDAP', 'Default LDAP user search filter'),
('ldap.default.group_search_filter','(objectClass=group)',  'STRING', 'LDAP', 'Default LDAP group search filter'),
('ldap.default.sync_interval_minutes','60',           'INTEGER', 'LDAP',     'Default LDAP sync interval'),
-- Workflow defaults
('workflow.post_function.max_retries','3',            'INTEGER', 'WORKFLOW', 'Max retries for post-functions'),
('workflow.post_function.retry_delay_ms','100',       'INTEGER', 'WORKFLOW', 'Retry delay in ms'),
('workflow.status_catalog.cache_ttl_ms','300000',     'INTEGER', 'WORKFLOW', 'Status catalog cache TTL'),
('workflow.default.email_subject',  'Issue Notification','STRING','WORKFLOW','Default email subject for post-functions'),
-- Release defaults
('release.default.status',          'DRAFT',          'STRING',  'RELEASE',  'Default release status'),
-- Goal defaults
('goal.default.status',             'NOT_STARTED',    'STRING',  'GOAL',     'Default goal status'),
-- Attachment settings
('attachment.allowed_types',        'image/png,image/jpeg,image/gif,image/webp,application/pdf,text/plain,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,text/csv,application/zip,application/x-7z-compressed,application/json,application/xml,text/xml,video/mp4,audio/mpeg', 'STRING', 'ATTACHMENT', 'Allowed MIME types for attachments'),
('attachment.max_size_bytes',       '52428800',       'INTEGER', 'ATTACHMENT','Max attachment size (50MB)');
```

#### Table: `master_quick_filter_presets`
```sql
CREATE TABLE jira_admin.master_quick_filter_presets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    filter_name     VARCHAR(100) NOT NULL,
    jql_query       VARCHAR(500) NOT NULL,
    icon            VARCHAR(50),
    sort_order      INTEGER NOT NULL DEFAULT 0,
    is_system       BOOLEAN NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO jira_admin.master_quick_filter_presets (filter_name, jql_query, icon, sort_order, is_system) VALUES
('Assigned to Me',  'assignee = currentUser()',                   'user',     0, TRUE),
('Reported by Me',  'reporter = currentUser()',                   'edit',     1, TRUE),
('Recently Updated','updated >= -7d',                             'clock',    2, TRUE),
('Unassigned',      'assignee is EMPTY',                          'users',    3, TRUE),
('Has Due Date',    'dueDate is not EMPTY',                       'calendar', 4, TRUE),
('High Priority',   'priority in (Highest, High)',                'alert',    5, TRUE),
('Blocked',         'status = "Blocked"',                         'stop',     6, TRUE);
```

#### Table: `i18n_messages`
```sql
CREATE TABLE jira_admin.i18n_messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_key     VARCHAR(200) NOT NULL,
    locale          VARCHAR(10) NOT NULL DEFAULT 'en',
    message_value   TEXT NOT NULL,
    category        VARCHAR(50),
    UNIQUE(message_key, locale)
);

CREATE INDEX idx_i18n_locale ON jira_admin.i18n_messages(locale);

-- Seed English messages (all 68 currently hardcoded error messages)
INSERT INTO jira_admin.i18n_messages (message_key, locale, message_value, category) VALUES
('error.validation.invalid_params',    'en', 'Invalid request parameters',        'ERROR'),
('error.generic.unexpected',           'en', 'An unexpected error occurred',       'ERROR'),
('error.validation.failed',            'en', 'Validation Failed',                  'ERROR'),
('error.auth.invalid_credentials',     'en', 'Invalid credentials',                'ERROR'),
('error.auth.account_disabled',        'en', 'Account is disabled',                'ERROR'),
('error.auth.invalid_refresh_token',   'en', 'Invalid refresh token',              'ERROR'),
('error.auth.not_refresh_token',       'en', 'Token is not a refresh token',       'ERROR'),
('error.auth.user_not_found',          'en', 'User not found',                     'ERROR'),
('error.auth.insufficient_permissions','en', 'Insufficient permissions',           'ERROR'),
('sprint.error.cannot_add_completed',  'en', 'Cannot add issues to a completed sprint', 'ERROR'),
('sprint.error.already_in_sprint',     'en', 'Issue already in sprint',            'ERROR'),
('comment.error.parent_not_found',     'en', 'Parent comment not found',           'ERROR'),
('comment.error.parent_deleted',       'en', 'Parent comment has been deleted',    'ERROR'),
('comment.error.not_found',            'en', 'Comment not found',                  'ERROR'),
('comment.error.unauthorized_update',  'en', 'User is not authorized to update this comment', 'ERROR'),
('comment.error.unauthorized_delete',  'en', 'User is not authorized to delete this comment', 'ERROR'),
('version.error.modify_released',      'en', 'Cannot modify a released version',  'ERROR'),
('version.error.already_released',     'en', 'Version is already released',        'ERROR');
-- ... add all 68 error messages
```

---

## 3. Architectural Pattern

### 3.1 Master Data Service Layer (in jira-admin-service)

```
┌─────────────────────────────────────────────────────────┐
│                    jira-admin-service                     │
│                                                          │
│  ┌──────────────────┐   ┌─────────────────────────────┐ │
│  │ MasterDataController │   │ SystemConfigController    │ │
│  │  GET /api/admin/      │   │  GET /api/admin/config/   │ │
│  │    master-data/{type} │   │  PUT /api/admin/config/   │ │
│  │  POST/PUT/DELETE      │   │    {key}                  │ │
│  └──────────┬───────────┘   └───────────┬───────────────┘ │
│             │                           │                 │
│  ┌──────────▼───────────┐   ┌───────────▼───────────────┐ │
│  │ MasterDataService    │   │ SystemConfigService       │ │
│  │  - getStatuses()     │   │  - getConfig(key)         │ │
│  │  - getPriorities()   │   │  - setConfig(key, val)    │ │
│  │  - getIssueTypes()   │   │  - getByCategory(cat)     │ │
│  │  - getRoles()        │   │  - getDefaults()          │ │
│  │  - getPermissions()  │   └───────────────────────────┘ │
│  │  - getLinkTypes()    │                                 │
│  │  - getBoardTypes()   │   ┌───────────────────────────┐ │
│  │  - getResolutions()  │   │ I18nMessageService        │ │
│  │  - getQuickFilters() │   │  - getMessage(key, locale)│ │
│  │  - getNotifEvents()  │   │  - getMessages(locale)    │ │
│  └──────────────────────┘   └───────────────────────────┘ │
│                                                          │
│  ┌──────────────────────────────────────────────────────┐ │
│  │              jira_admin database                      │ │
│  │  master_statuses | master_priorities | master_roles   │ │
│  │  master_permissions | master_issue_types              │ │
│  │  master_link_types | master_board_types               │ │
│  │  master_resolutions | system_configuration            │ │
│  │  master_notification_events | i18n_messages           │ │
│  │  master_quick_filter_presets | ...                    │ │
│  └──────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

### 3.2 Consumer Pattern (all other services)

```java
// Each consuming service gets a MasterDataClient (Feign/RestTemplate)
@FeignClient(name = "jira-admin-service", url = "${admin-service.url}")
public interface MasterDataClient {
    
    @GetMapping("/api/admin/master-data/statuses")
    List<MasterStatusDTO> getStatuses();
    
    @GetMapping("/api/admin/master-data/statuses/categories")
    List<String> getStatusCategories();
    
    @GetMapping("/api/admin/config/{key}")
    ConfigValueDTO getConfig(@PathVariable String key);
    
    @GetMapping("/api/admin/i18n/{locale}")
    Map<String, String> getMessages(@PathVariable String locale);
}
```

### 3.3 Caching Strategy

```java
@Service
public class MasterDataCacheService {
    
    private final MasterDataClient masterDataClient;
    private final CacheManager cacheManager;
    
    // Cache with 5-minute TTL, auto-refresh
    @Cacheable(value = "masterStatuses", unless = "#result.isEmpty()")
    public List<MasterStatusDTO> getStatuses() {
        return masterDataClient.getStatuses();
    }
    
    // Status category check — replaces ALL hardcoded contains("done") logic
    public boolean isCompletedStatus(String statusName) {
        return getStatuses().stream()
            .filter(s -> s.getDisplayName().equalsIgnoreCase(statusName) 
                      || s.getStatusKey().equalsIgnoreCase(statusName))
            .anyMatch(s -> "DONE".equals(s.getCategory()));
    }
    
    public boolean isInProgressStatus(String statusName) {
        return getStatuses().stream()
            .filter(s -> s.getDisplayName().equalsIgnoreCase(statusName)
                      || s.getStatusKey().equalsIgnoreCase(statusName))
            .anyMatch(s -> "IN_PROGRESS".equals(s.getCategory()));
    }
    
    // Event-driven invalidation via Spring Cloud Bus or direct API
    @CacheEvict(value = "masterStatuses", allEntries = true)
    public void evictStatusCache() {}
}
```

### 3.4 Cache Invalidation via Events

```
Admin updates status → jira-admin-service publishes event
  → RabbitMQ/Kafka topic: master-data-changed
    → All consumer services receive event
      → Evict local cache
        → Next request fetches fresh data
```

### 3.5 Startup Preloading

```java
@Component
public class MasterDataPreloader implements ApplicationRunner {
    
    private final MasterDataCacheService cacheService;
    
    @Override
    public void run(ApplicationArguments args) {
        // Preload all master data into cache at startup
        cacheService.getStatuses();
        cacheService.getPriorities();
        cacheService.getIssueTypes();
        // Fail-open: if admin-service is down, use last cached values
    }
}
```

---

## 4. REST API Endpoints (jira-admin-service)

```
GET    /api/admin/master-data/statuses                → List all statuses
POST   /api/admin/master-data/statuses                → Create status
PUT    /api/admin/master-data/statuses/{id}            → Update status
DELETE /api/admin/master-data/statuses/{id}            → Deactivate status

GET    /api/admin/master-data/priorities               → List all priorities
POST   /api/admin/master-data/priorities               → Create priority
PUT    /api/admin/master-data/priorities/{id}           → Update priority

GET    /api/admin/master-data/issue-types              → List all issue types
POST   /api/admin/master-data/issue-types              → Create issue type
PUT    /api/admin/master-data/issue-types/{id}         → Update issue type

GET    /api/admin/master-data/resolutions              → List all resolutions
GET    /api/admin/master-data/link-types               → List all link types
GET    /api/admin/master-data/roles                    → List all roles
GET    /api/admin/master-data/permissions              → List all permissions
GET    /api/admin/master-data/board-types              → List board types with column templates
GET    /api/admin/master-data/notification-events      → List notification events
GET    /api/admin/master-data/quick-filters            → List quick filter presets

GET    /api/admin/config                               → All system configuration
GET    /api/admin/config/{key}                         → Get specific config
PUT    /api/admin/config/{key}                         → Update config value
GET    /api/admin/config/category/{category}           → Get configs by category

GET    /api/admin/i18n/{locale}                        → All messages for locale
GET    /api/admin/i18n/{locale}/{key}                  → Specific message
PUT    /api/admin/i18n/{locale}/{key}                  → Update message
```

---

## 5. Step-by-Step Migration Plan

### Phase 1: Foundation (Week 1-2) — Zero Regression Risk

**Step 1.1:** Create all master data tables in `jira_admin` database
- Add Flyway migration `V_next__master_data_tables.sql` in jira-admin-service
- Contains all CREATE TABLE + seed INSERT statements from Section 2

**Step 1.2:** Build MasterDataService + CRUD controllers in jira-admin-service
- `MasterDataController.java` — CRUD for each master data type
- `SystemConfigController.java` — CRUD for system_configuration
- `I18nMessageController.java` — CRUD for i18n messages

**Step 1.3:** Build shared client library `jira-cluster-commons`
- `MasterDataClient.java` — Feign client interface
- `MasterDataCacheService.java` — cached wrapper with category helpers
- `MasterDataPreloader.java` — startup preloader
- `SystemConfigClient.java` — config key lookup client

### Phase 2: Status Centralization (Week 3) — Highest Impact

**Target:** Eliminate all 95 hardcoded status references and the duplicated `contains("done")` pattern across 8 services.

**Step 2.1:** Replace status substring matching in jira-sprint-service (6 duplicated methods):
- `SprintReportService.java:48,86,336,346,530` → `masterDataCacheService.isCompletedStatus()`
- `SprintPlanningService.java:384` → `masterDataCacheService.isCompletedStatus()`
- `IssueServiceClient.java:382` → `masterDataCacheService.isCompletedStatus()`
- `BoardService.java:442,660` → `masterDataCacheService.isCompletedStatus()`
- `WipLimitService.java:211` → `masterDataCacheService.getStatusCategory()`
- `SavedFilterService.java:33` → build JQL from master data

**Step 2.2:** Replace status matching in jira-version-service:
- `ReleaseHubService.java:113-115` → `masterDataCacheService.getStatusCategory()`

**Step 2.3:** Replace status matching in jira-issue-service:
- `EpicService.java:183` → `masterDataCacheService.isCompletedStatus()`

**Step 2.4:** Replace status matching in jira-plan-service:
- `HierarchyRollupService.java:55` → use category from master data
- `ProgramAggregationService.java:83` → use category from master data
- `SprintService.java:797` → use column-to-status mapping

**Step 2.5:** Replace hardcoded status lists in jira-search-service:
- `JQLSearchService.java:279` → fetch from master data with cache fallback

### Phase 3: Defaults & Magic UUIDs (Week 4)

**Step 3.1:** Replace magic UUIDs in jira-issue-service:
- `IssueService.java:65` — look up by key from system_configuration
- `CloneIssueService.java:31` — same

**Step 3.2:** Replace all `@Builder.Default` hardcoded strings:
- All entity classes with hardcoded defaults → read from `SystemConfigClient`

**Step 3.3:** Replace default assignee type across services:
- `ProjectService.java`, `Project.java`, `ProjectTemplate.java` → read from config

### Phase 4: Roles, Permissions, Board Columns (Week 5)

**Step 4.1:** Replace hardcoded role creation:
- `ProjectService.java` → fetch default roles from master data
- `ExportImportService.java` → same
- `ProjectTypeService.java` → same (eliminate triplication)

**Step 4.2:** Replace hardcoded board column templates:
- `AgileBoardService.java:71-85` → fetch from `master_board_column_templates`
- `BoardService.java:566-574` → same (eliminate duplication)
- `BoardConfigService.java:458-464` → same

**Step 4.3:** Replace hardcoded quick filters:
- `BoardService.java:39` → fetch from `master_quick_filter_presets`
- `SavedFilterService.java:28` → same

### Phase 5: Issue Types, Priorities, Link Types (Week 6)

**Step 5.1:** Replace hardcoded issue type references:
- `SprintReportService.java:88` → fetch from master data
- `SprintPlanningService.java:348` → fetch from master data
- `JQLSearchService.java:293` → fetch with cache

**Step 5.2:** Replace hardcoded priority references:
- `SprintPlanningService.java:360` → fetch from master data
- `JQLSearchService.java:286` → fetch with cache

**Step 5.3:** Replace hardcoded link type fallbacks:
- `IssueLinkService.java:359` → fetch default from config

### Phase 6: Workflow Definitions (Week 7)

**Step 6.1:** Replace hardcoded definition lists (already have DB tables!):
- `WorkflowAdministrationService.java:1044` → load conditions from DB table
- `WorkflowAdministrationService.java:1052` → load validators from DB table
- `WorkflowAdministrationService.java:1061` → load post-functions from DB table

**Step 6.2:** Externalize script-templates.json:
- Move to `script_templates` database table
- Replace hardcoded issue type/status references with configurable parameters

### Phase 7: i18n & Error Messages (Week 8)

**Step 7.1:** Create `messages.properties` files:
- Create per-service `src/main/resources/messages.properties`
- Replace all 68 hardcoded error strings with `MessageSource` lookups
- Alternatively use the `i18n_messages` database table for admin-editable messages

**Step 7.2:** Replace hardcoded labels:
- All user-facing display strings → `MessageSource` or database lookup

### Phase 8: SQL Migration Cleanup (Week 9)

**Step 8.1:** For each service with hardcoded INSERT seeds:
- Create migration that moves seed data to reference master_data tables
- Add FK constraints where appropriate
- Replace CHECK constraints with FK to lookup tables (e.g., `V29__plan_goals.sql` status CHECK)

---

## 6. Regression Prevention Strategy

### 6.1 Backward Compatibility

Every change follows this pattern:
1. **Add** the new master data table/endpoint
2. **Add** the client + cache in the consuming service
3. **Replace** the hardcoded value with the dynamic lookup, using the SAME default value as fallback
4. **Test** that the behavior is identical with the seed data
5. **Remove** the hardcoded fallback only after verification

### 6.2 Fallback Chain

```java
// Pattern: master data → config property → hardcoded fallback (same as current)
public String getDefaultStatus() {
    try {
        return masterDataClient.getConfig("issue.default.status_key").getValue();
    } catch (Exception e) {
        return configProperty("issue.default.status-key", "TODO"); // same as before
    }
}
```

### 6.3 Testing Strategy

- **Unit tests:** Mock `MasterDataClient` to return seed data → verify identical behavior
- **Integration tests:** Start admin-service with seed data → verify cross-service calls work
- **Regression test:** Compare API responses before/after for every endpoint
- **Contract tests:** Verify master data API contract with Spring Cloud Contract

### 6.4 Feature Flag

```yaml
# application.yml per service
feature:
  use-master-data: true   # false = use hardcoded fallbacks (rollback switch)
```

### 6.5 Monitoring

- Alert if `MasterDataClient` calls fail > 5% of requests
- Alert if cache hit ratio drops below 90%
- Dashboard showing master data sync status per service

---

## 7. Files Changed Summary

| Service | Files to Modify | New Files | Migrations |
|---------|----------------|-----------|------------|
| jira-admin-service | 3 | 8 (controllers, services, entities, repos) | 1 (master data tables) |
| jira-cluster-commons | 0 | 4 (clients, cache service, preloader, DTOs) | 0 |
| jira-issue-service | 8 | 0 | 0 |
| jira-sprint-service | 7 | 0 | 0 |
| jira-project-service | 5 | 0 | 0 |
| jira-workflow-service | 4 | 0 | 0 |
| jira-version-service | 2 | 0 | 0 |
| jira-plan-service | 6 | 0 | 0 |
| jira-search-service | 3 | 0 | 0 |
| jira-notification-service | 3 | 0 | 0 |
| jira-auth-service | 4 | 1 (messages.properties) | 0 |
| jira-comment-service | 3 | 1 (messages.properties) | 0 |
| jira-report-service | 3 | 1 (messages.properties) | 0 |
| jira-component-service | 2 | 1 (messages.properties) | 0 |
| jira-version-service | 3 | 1 (messages.properties) | 0 |
| jira-attachment-service | 2 | 0 | 0 |
| jira-dashboard-service | 2 | 0 | 0 |
| jira-document-service | 2 | 0 | 0 |
| **TOTAL** | **~62 files** | **~18 new files** | **1 migration** |

---

## Appendix: Hardcoded Colors Inventory (Inconsistencies Found)

| Status/Entity | Service A Color | Service B Color | Resolution |
|--------------|-----------------|-----------------|------------|
| To Do / TODO | `#4C9AFF` (project) | `#0052CC` (workflow) | Use master_statuses.color |
| In Progress  | `#0052CC` (project) | `#FF991F` (workflow) | Use master_statuses.color |
| Done         | `#00875A` (both) | `#36b37e` (sprint board) | Use master_statuses.color |
| Board "In Progress" column | `#0066ff` (agile) | `#ff8b00` (kanban) | Use master_board_column_templates.color |
