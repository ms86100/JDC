# Jira Data Center - Import/Export Architecture Analysis

## Executive Summary

This document provides a comprehensive analysis of Jira Data Center's import/export system, derived from deep reverse engineering of the installed codebase. The analysis covers the complete architecture, all components, and their interactions.

---

## 1. SYSTEM ARCHITECTURE OVERVIEW

### 1.1 Import/Export Components

| Component | Location | Purpose |
|----------|----------|---------|
| **Project Import Wizard** | `imports/project/` | Multi-step project import from backup ZIP |
| **XML Import Service** | `bc/imports/xml/` | Direct XML backup file import |
| **General Data Import** | `bc/dataimport/` | Full system backup/restore |
| **CSV Export** | `issue/views/csv/` | Issue search result CSV export |
| **Bulk Operations** | `views/bulkedit/` | Bulk issue operations |

### 1.2 Import Workflow (5-Step Wizard)

```
Step 1: ProjectImportSelectBackup.jsp
    ↓ Select backup ZIP file path
Step 2: ProjectImportSelectProject.jsp
    ↓ Choose specific project from backup
Step 3: ProjectImportSummary.jsp
    ↓ View validation results and field mappings
Step 4: ProjectImportProgress.jsp
    ↓ Track import progress (auto-refresh)
Step 5: ProjectImportResults.jsp
    ↓ Display final results with statistics
```

---

## 2. CORE IMPORT COMPONENTS

### 2.1 Parser Classes (imports/project/parser/)

Each entity type has a dedicated parser:

| Parser | Purpose |
|--------|---------|
| `ProjectParser` | Parse project data |
| `IssueParser` | Parse issue data |
| `CommentParser` | Parse comments |
| `WorklogParser` | Parse worklogs |
| `AttachmentParser` | Parse attachments |
| `CustomFieldParser` | Parse custom field definitions |
| `CustomFieldValueParser` | Parse custom field values |
| `LabelParser` | Parse labels |
| `IssueLinkParser` | Parse issue links |
| `IssueLinkTypeParser` | Parse link types |
| `VersionParser` | Parse project versions |
| `ComponentParser` | Parse project components |
| `UserAssociationParser` | Parse user associations |
| `GroupParser` | Parse groups |
| `ChangeGroupParser` | Parse change history |
| `EntityPropertyParser` | Parse entity properties |

### 2.2 Validator Classes (imports/project/validation/)

Validates mappings before persistence:

| Validator | Purpose |
|-----------|---------|
| `StatusMapperValidatorImpl` | Validates status mappings |
| `PriorityMapperValidator` | Validates priority mappings |
| `IssueTypeMapperValidatorImpl` | Validates issue type mappings |
| `CustomFieldMapperValidatorImpl` | Validates custom field mappings |
| `CustomFieldOptionMapperValidatorImpl` | Validates select option values |
| `UserMapperValidatorImpl` | Validates user mappings |
| `ProjectRoleActorMapperValidatorImpl` | Validates project role mappings |
| `SystemFieldsMaxTextLengthValidator` | Validates field length limits |

### 2.3 Persister Handlers (imports/project/handler/)

Handles database persistence for each entity:

| Handler | Purpose |
|---------|---------|
| `ProjectMapperHandler` | Persist project |
| `IssuePersisterHandler` | Persist issues |
| `IssueMapperHandler` | Map issue fields |
| `IssueRelatedEntitiesPartitionHandler` | Handle related entities |
| `CommentPersisterHandler` | Persist comments |
| `WorklogPersisterHandler` | Persist worklogs |
| `LabelsPersisterHandler` | Persist labels |
| `IssueLinkPersisterHandler` | Persist issue links |
| `CustomFieldValuePersisterHandler` | Persist custom field values |
| `CustomFieldMapperHandler` | Map custom fields |
| `VersionPersisterHandler` | Persist versions |
| `ComponentPersisterHandler` | Persist components |
| `AttachmentPersisterHandler` | Persist attachments |
| `AttachmentFileValidatorHandler` | Validate attachment files |
| `UserMapperHandler` | Map users |
| `UserAssociationPersisterHandler` | Persist user associations |
| `RegisterUserMapperHandler` | Register new users during import |
| `ChangeGroupPersisterHandler` | Persist change history |
| `EntityPropertiesPersisterHandler` | Persist entity properties |

---

## 3. DATABASE ENTITY MAPPING

### 3.1 Core Tables

| Jira DC Table | jira-platform Table | Notes |
|--------------|-------------------|-------|
| `project` | `jira_project.projects` | |
| `projectversion` | `jira_issue.project_versions` | |
| `component` | `jira_issue.project_components` | |
| `jiraissue` | `jira_issue.issues` | |
| `issuelink` | `jira_issue.issue_links` | |
| `issuelinktype` | `jira_issue.issue_link_types` | |
| `issuestatus` | `jira_issue.issue_status` | |
| `issuetype` | `jira_issue.issue_type` | |
| `priority` | `jira_issue.issue_priority` | |
| `resolution` | `jira_issue.resolutions` | |
| `worklog` | `jira_issue.worklogs` | |
| `jiraaction` | `jira_issue.comments` | |
| `fileattachment` | `jira_attachment.attachments` | |
| `label` | `jira_issue.labels` | |
| `customfield` | `jira_issue.custom_fields` | |
| `customfieldvalue` | `jira_issue.custom_field_values` | |
| `customfieldoption` | `jira_issue.custom_field_options` | |
| `cwd_user` | `jira_user.users` | |
| `cwd_group` | `jira_user.groups` | |
| `cwd_membership` | `jira_user.group_members` | |
| `changegroup` | `jira_issue.change_groups` | |
| `changeitem` | `jira_issue.change_items` | |
| `audit_log` | `jira_audit.audit_logs` | |

### 3.2 Workflow Tables

| Jira DC Table | jira-platform Table | Notes |
|--------------|-------------------|-------|
| `workflowscheme` | `jira_project.workflow_schemes` | |
| `workflowschemeentity` | `jira_project.workflow_scheme_workflows` | |
| `permissionscheme` | `jira_project.permission_schemes` | |
| `schemepermissions` | `jira_project.permission_grants` | |
| `issuesecurityscheme` | `jira_project.security_schemes` | |
| `schemeissuesecuritylevels` | `jira_project.security_levels` | |
| `jiraworkflows` | `jira_workflow.workflows` | |
| `workflowstatuses` | `jira_workflow.workflow_statuses` | |
| `workflowtransitions` | `jira_workflow.workflow_transitions` | |

---

## 4. KEY GENERATION LOGIC

### 4.1 Project Key Generation

```
Pattern: Uppercased project name with special characters removed
Example: "My Test Project" → "MYTESTPROJECT"
Validation: Unique key check before creation
```

### 4.2 Issue Key Generation

```
Pattern: PROJECTKEY-NNNN
Example: JIRA-1234, PROJ-1

Generation Steps:
1. Get project key from selected project
2. Get next sequence number for that project key
3. Concatenate: projectKey + "-" + sequenceNumber
4. Store in Issue table

Sequence Management:
- Uses database sequence per project
- Locking to prevent duplicate keys
- Retry logic for concurrent creation
```

---

## 5. VALIDATION FLOW

### 5.1 Import Validation Pipeline

```
1. File Upload & Parse
   ↓
2. Schema Validation (XML/CSV structure)
   ↓
3. Reference Validation (external keys exist)
   ↓
4. Field Mapping Validation
   ├── StatusMapperValidator
   ├── PriorityMapperValidator
   ├── IssueTypeMapperValidator
   ├── UserMapperValidator
   ├── CustomFieldMapperValidator
   └── ProjectRoleActorMapperValidator
   ↓
5. Business Rule Validation
   ├── Field length validation
   ├── Required field validation
   └── Workflow transition validation
   ↓
6. Preview & Confirmation
   ↓
7. Transactional Import
```

### 5.2 Validation Error Categories

| Category | Example | Resolution |
|----------|---------|------------|
| Missing Reference | Status "Invalid" doesn't exist | Map to existing status |
| Duplicate Key | Project key "PROJ" already exists | Rename or skip |
| Required Field | Summary is empty | Provide default value |
| Invalid Format | Date "not-a-date" | Parse error message |
| Workflow Mismatch | Invalid status transition | Skip or map status |

---

## 6. ISSUE HIERARCHY SUPPORT

### 6.1 Epic → Story → Subtask

```sql
-- Epic (IssueType = Epic)
Issue {
  id: UUID,
  issue_key: 'PROJ-1',
  issue_type_id: epic_type_id,
  parent_issue_id: NULL
}

-- Story (linked to Epic via epic_id)
Issue {
  id: UUID,
  issue_key: 'PROJ-2',
  issue_type_id: story_type_id,
  parent_issue_id: NULL,
  epic_id: PROJ-1.id
}

-- Subtask (parent_issue_id set)
Issue {
  id: UUID,
  issue_key: 'PROJ-3',
  issue_type_id: subtask_type_id,
  parent_issue_id: PROJ-2.id
}
```

### 6.2 Import Resolution

During import, the system must:
1. Create Epics first (no dependencies)
2. Create Stories next (can reference Epics)
3. Create Subtasks last (can reference Stories)
4. Handle cross-references with pending resolution table

---

## 7. TRANSACTION MANAGEMENT

### 7.1 Import Transaction Flow

```
BEGIN TRANSACTION
  TRY
    -- Phase 1: Create Projects & Schemes
    INSERT projects
    INSERT schemes
    INSERT scheme associations

    -- Phase 2: Create Reference Data
    INSERT users
    INSERT groups
    INSERT statuses
    INSERT priorities
    INSERT resolutions
    INSERT issue types
    INSERT custom fields

    -- Phase 3: Create Issues (ordered by dependency)
    INSERT issues (Epics first)
    INSERT issues (Stories second)
    INSERT issues (Subtasks last)

    -- Phase 4: Create Related Entities
    INSERT comments
    INSERT attachments
    INSERT worklogs
    INSERT labels
    INSERT custom field values
    INSERT issue links
    INSERT change history

    -- Phase 5: Final Validation
    RUN integrity checks
    UPDATE counters

    COMMIT
  CATCH
    ROLLBACK
    LOG errors
    REPORT to user
  END
```

### 7.2 Partial Import Handling

- **Soft Validation Errors**: Warnings shown, import continues
- **Hard Validation Errors**: Import halted, no changes made
- **Referential Integrity**: All foreign keys validated before insert
- **Duplicate Detection**: Check both source and target keys

---

## 8. CSV EXPORT ARCHITECTURE

### 8.1 CSV View Classes

| Class | Purpose |
|-------|---------|
| `SearchRequestCsvViewAllFields` | Export all fields |
| `SearchRequestCsvViewCurrentFields` | Export current view fields |
| `SearchRequestCsvWithBomViewAllFields` | UTF-8 BOM for Excel |
| `SearchRequestCsvWithBomViewCurrentFields` | UTF-8 BOM for current view |

### 8.2 Export Field Mapping

```
System Fields:
- Issue Key
- Summary
- Description
- Status
- Issue Type
- Priority
- Resolution
- Assignee
- Reporter
- Created
- Updated
- Due Date
- Labels
- Component
- Fix Version
- Affects Version
- Epic Link
- Parent
- Sub-Tasks
- Linked Issues
- Comment Count
- Attachment Count
- Custom Fields (dynamic)
```

---

## 9. ERROR HANDLING PATTERNS

### 9.1 Error Categories

| Error Type | Example | User Message |
|-----------|---------|--------------|
| `MISSING_REFERENCE` | User "john" not found | "Assignee 'john' could not be mapped" |
| `INVALID_VALUE` | Status "invalid" | "Status 'invalid' is not a valid status" |
| `DUPLICATE_KEY` | Project key exists | "Project key 'PROJ' already exists" |
| `REQUIRED_FIELD` | Empty summary | "Summary is required for all issues" |
| `FIELD_TOO_LONG` | Description > 255 chars | "Description exceeds maximum length" |
| `WORKFLOW_ERROR` | Invalid transition | "Cannot transition from 'Open' to 'Done'" |

### 9.2 Error Response Format

```json
{
  "row": 15,
  "column": "assignee",
  "value": "john",
  "errorType": "MISSING_REFERENCE",
  "errorMessage": "User 'john' does not exist in the system",
  "suggestion": "Map to existing user or create new user",
  "suggestedValues": ["jsmith", "jdoe", "admin"]
}
```

---

## 10. PERFORMANCE CONSIDERATIONS

### 10.1 Batch Processing

- **Batch Size**: 100-500 records per transaction
- **Parallel Processing**: Multiple entity types in parallel
- **Progress Tracking**: Real-time updates via WebSocket/polling
- **Memory Management**: Stream large files, don't load entirely into memory

### 10.2 Indexing Strategy

```sql
-- Pre-create indexes for import performance
CREATE INDEX idx_import_issues_project ON issues(project_id);
CREATE INDEX idx_import_issues_status ON issues(status_id);
CREATE INDEX idx_import_worklog_issue ON worklogs(issue_id);
CREATE INDEX idx_import_comments_issue ON comments(issue_id);

-- Disable non-essential indexes during bulk import
ALTER INDEX idx_issues_updated DISABLE;

-- Rebuild indexes after import
REINDEX TABLE issues;
```

---

## 11. RE-IMPORT BEHAVIOR

### 11.1 Update vs. Skip Strategy

| Entity | Default Behavior | Option |
|--------|-----------------|--------|
| Project | Skip (key exists) | Update |
| Issue | Skip (key exists) | Update |
| Comment | Skip (by author+date) | Create new |
| Worklog | Skip (by author+date) | Create new |
| Attachment | Skip (by filename) | Create new |
| Custom Field | Skip | Update values |

### 11.2 Conflict Resolution

```
Conflict Detection:
1. Compare source key to target key
2. If match found, apply conflict resolution strategy

Resolution Strategies:
- SKIP: Don't import, log warning
- UPDATE: Update existing record with source data
- RENAME: Import with new key (e.g., "PROJ-1" → "PROJ-1-imported")
- ERROR: Fail import, require user decision
```

---

## 12. IMPLEMENTATION REQUIREMENTS

### 12.1 CSV Template Structure

```csv
# Projects Section
PROJECT_KEY,PROJECT_NAME,PROJECT_DESCRIPTION,PROJECT_LEAD

# Issues Section
ISSUE_KEY,SUMMARY,DESCRIPTION,ISSUE_TYPE,STATUS,PRIORITY,RESOLUTION,ASSIGNEE,REPORTER,DUE_DATE,PARENT_KEY,EPIC_KEY

# Versions Section
VERSION_NAME,VERSION_DESCRIPTION,RELEASE_DATE,RELEASED,ARCHIVED

# Components Section
COMPONENT_NAME,COMPONENT_DESCRIPTION,LEAD

# Labels Section
ISSUE_KEY,LABEL_NAME

# Comments Section
ISSUE_KEY,COMMENT_AUTHOR,COMMENT_BODY,CREATED_DATE

# Worklogs Section
ISSUE_KEY,WORKLOG_AUTHOR,TIME_SPENT_SECONDS,WORK_BODY,STARTED_DATE

# Issue Links Section
SOURCE_ISSUE_KEY,TARGET_ISSUE_KEY,LINK_TYPE

# Custom Fields Section
ISSUE_KEY,CUSTOM_FIELD_NAME,CUSTOM_FIELD_VALUE
```

### 12.2 API Endpoints

```
POST   /api/import/upload          - Upload CSV/XML file
POST   /api/import/validate        - Validate uploaded file
POST   /api/import/preview         - Preview import without committing
POST   /api/import/execute        - Execute import
GET    /api/import/status/{id}     - Get import status
GET    /api/import/errors/{id}     - Get import errors
POST   /api/import/cancel/{id}    - Cancel running import

POST   /api/export/project/{id}    - Export project to CSV
GET    /api/export/download/{id}  - Download exported file
GET    /api/export/templates      - Get available templates
```

### 12.3 Database Schema Requirements

```sql
-- Import tracking table
CREATE TABLE import_jobs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    project_id UUID,
    status VARCHAR(20) NOT NULL,
    file_name VARCHAR(255),
    total_records INT,
    processed_records INT,
    error_count INT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Import errors table
CREATE TABLE import_errors (
    id UUID PRIMARY KEY,
    job_id UUID REFERENCES import_jobs(id),
    row_number INT,
    entity_type VARCHAR(50),
    field_name VARCHAR(100),
    error_type VARCHAR(50),
    error_message TEXT,
    source_value TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Pending reference resolution
CREATE TABLE import_pending_refs (
    id UUID PRIMARY KEY,
    job_id UUID REFERENCES import_jobs(id),
    source_key VARCHAR(100),
    target_key VARCHAR(100),
    entity_type VARCHAR(50),
    field_name VARCHAR(100),
    resolved BOOLEAN DEFAULT FALSE,
    resolution VARCHAR(50),
    created_at TIMESTAMP DEFAULT NOW()
);
```

---

## 13. AUDIT REQUIREMENTS

### 13.1 Audit Events

| Event | Data Captured |
|-------|---------------|
| `IMPORT_STARTED` | Job ID, user, file, timestamp |
| `IMPORT_VALIDATED` | Job ID, validation results |
| `IMPORT_EXECUTED` | Job ID, record counts |
| `IMPORT_COMPLETED` | Job ID, duration, success/failure |
| `IMPORT_ROLLBACK` | Job ID, reason, rolled back records |
| `PROJECT_CREATED` | Project key, name |
| `ISSUE_CREATED` | Issue key, type |
| `ISSUE_UPDATED` | Issue key, fields changed |

### 13.2 Audit Log Schema

```sql
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    event_category VARCHAR(50),
    event_type VARCHAR(100),
    entity_type VARCHAR(50),
    entity_id UUID,
    entity_name VARCHAR(255),
    user_id UUID,
    user_name VARCHAR(100),
    remote_address VARCHAR(50),
    changes JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);
```

---

## 14. CONCLUSION

This analysis provides the complete blueprint for implementing a production-grade Jira-style migration/import/export system. The architecture must:

1. **Support CSV and XML formats** for maximum compatibility
2. **Multi-step wizard UI** for user guidance
3. **Comprehensive validation** before any data is written
4. **Transactional integrity** with full rollback on failure
5. **Progress tracking** for long-running imports
6. **Detailed error reporting** with actionable suggestions
7. **Epic/Story/Subtask hierarchy** support
8. **Custom field extensibility**
9. **Workflow validation**
10. **User/group mapping**

All components must be implemented to match the exact behavior of Jira Data Center's import/export system.
