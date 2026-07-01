You are a principal Jira Data Center architect and enterprise workflow platform engineer.

Your task is to design and implement a COMPLETE Jira Data Center-style Workflow + Issue Architecture with FULL enterprise-grade linkage between:

- Projects
- Workflow Schemes
- Workflows
- Issue Types
- Issues
- Statuses
- Transitions
- Screens
- Permissions
- Validators
- Conditions
- Post Functions
- Automation
- Notifications
- Audit Logs
- APIs
- Attachments
- Comments
- History
- Boards
- Sprints
- Reports
- Integrations

IMPORTANT:
This is NOT a simple status flow system.
This is NOT a Kanban-only implementation.
This is NOT a CRUD workflow builder.

This is an ENTERPRISE WORKFLOW EXECUTION ENGINE like Jira Data Center.

==================================================
CORE ENTERPRISE PRINCIPLE
==================================================

Issues DO NOT directly own workflows.

Correct Jira DC architecture is:

Project
 → Workflow Scheme
    → Issue Type Mapping
       → Workflow
          → Statuses
             → Transitions
                → Validators
                → Conditions
                → Post Functions

Issue inherits workflow behavior dynamically using:
- project_id
- issue_type_id
- workflow scheme mapping

The issue becomes:
“A runtime state machine instance executing a workflow.”

==================================================
YOU MUST IMPLEMENT FULL END-TO-END LINKAGE
==================================================

You MUST deeply implement and explain:

1. HOW workflows are linked
2. WHERE workflows are linked
3. WHEN workflows are resolved
4. WHY workflows are inherited
5. HOW issue transitions work
6. HOW runtime validation works
7. HOW permissions affect transitions
8. HOW screens affect transitions
9. HOW workflow schemes affect issue behavior
10. HOW migrations resolve workflows
11. HOW status mappings work
12. HOW automation hooks work
13. HOW audit trails work
14. HOW events propagate
15. HOW notifications trigger
16. HOW API behavior changes based on workflow

==================================================
MANDATORY TECH STACK
==================================================

BACKEND:
- Spring Boot
- Java
- PostgreSQL
- Flyway
- REST APIs
- JWT/Auth
- Event-driven architecture

FRONTEND:
- React
- TypeScript
- Jira Data Center-like UX
- Enterprise component architecture

INFRA:
- Docker
- CI/CD
- Audit logging
- Distributed-safe
- Transaction-safe
- Scalable

==================================================
PHASE 1 — CORE WORKFLOW ARCHITECTURE
==================================================

Implement these entities:

1. Workflow
2. Workflow Scheme
3. Workflow Scheme Mapping
4. Workflow Status
5. Workflow Transition
6. Workflow Validator
7. Workflow Condition
8. Workflow Post Function
9. Workflow Screen Mapping
10. Workflow Transition Screen
11. Workflow Events
12. Workflow Notification Rules
13. Workflow Automation Hooks

Explain EXACTLY:

- how issue type resolves workflow
- how project resolves workflow scheme
- how workflow resolves transitions
- how transition validates conditions
- how status updates issue state

==================================================
IMPORTANT:
DO NOT directly link issue.workflow_id

CORRECT DESIGN:

Issue
 ├── project_id
 ├── issue_type_id
 ├── current_status_id

Workflow is dynamically resolved:

Project
 → Workflow Scheme
    → Issue Type → Workflow Mapping

==================================================
PHASE 2 — ISSUE RUNTIME EXECUTION MODEL
==================================================

Implement issue lifecycle engine.

Issue acts as:
- workflow runtime instance
- transition executor
- audit generator
- event producer

For EVERY issue transition implement:

1. Load project
2. Load workflow scheme
3. Resolve issue type workflow
4. Validate current status
5. Validate allowed transition
6. Validate conditions
7. Validate permissions
8. Validate validators
9. Load transition screen
10. Validate screen fields
11. Execute transition
12. Execute post-functions
13. Generate notifications
14. Generate audit logs
15. Emit events
16. Update issue status
17. Reindex issue

==================================================
PHASE 3 — PROJECT LINKAGE
==================================================

Implement full project configuration linkage.

Project must own:

- Workflow Scheme
- Permission Scheme
- Notification Scheme
- Screen Scheme
- Field Configuration Scheme
- Issue Type Scheme

Explain:
- how changing workflow scheme affects issues
- how issue types change workflows
- how project migration impacts workflow resolution

==================================================
PHASE 4 — WORKFLOW SCHEME ENGINE
==================================================

Implement workflow scheme architecture exactly like Jira DC.

Workflow Scheme must support:

| Issue Type | Workflow |
|-------------|-----------|
| Bug         | Bug Workflow |
| Story       | Agile Workflow |
| Task        | Default Workflow |
| Incident    | ITSM Workflow |

Must support:
- default workflows
- issue-type-specific workflows
- workflow replacement
- draft workflow publishing
- scheme versioning

==================================================
PHASE 5 — UI/UX IMPLEMENTATION
==================================================

Build COMPLETE Jira-like workflow UI.

==================================================
A. WORKFLOW DESIGNER
==================================================

Implement:
- drag/drop status graph
- transition connectors
- validator configuration
- condition configuration
- post-function configuration
- transition screen mapping
- workflow publish
- workflow draft mode

==================================================
B. WORKFLOW SCHEME SCREEN
==================================================

Implement:
- issue type → workflow mapping
- workflow assignment
- scheme association
- project assignment
- validation warnings

==================================================
C. ISSUE VIEW SCREEN
==================================================

Issue screen MUST dynamically change based on workflow.

Examples:
- visible transitions
- hidden buttons
- editable fields
- required fields
- transition screens

Transition buttons MUST be dynamically resolved from:
workflow + permissions + conditions

==================================================
PHASE 6 — TRANSITION ENGINE
==================================================

Implement enterprise-grade transition engine.

Each transition supports:

1. Conditions
   - role conditions
   - group conditions
   - assignee conditions
   - expression conditions

2. Validators
   - required fields
   - regex validation
   - attachment required
   - comment required
   - custom script validator

3. Post Functions
   - update status
   - assign user
   - create comment
   - fire webhook
   - trigger automation
   - update SLA
   - create linked issue

==================================================
PHASE 7 — DATABASE ARCHITECTURE
==================================================

Generate COMPLETE PostgreSQL schema.

Required tables:

- workflows
- workflow_versions
- workflow_statuses
- workflow_transitions
- workflow_transition_rules
- workflow_conditions
- workflow_validators
- workflow_post_functions
- workflow_schemes
- workflow_scheme_mappings
- workflow_transition_screens
- workflow_events
- workflow_audit_logs
- issue_status_history
- issue_transition_history

Issue table MUST NOT directly own workflow_id.

Explain ALL:
- relationships
- constraints
- indexes
- audit columns
- versioning
- Flyway migrations

==================================================
PHASE 8 — BACKEND IMPLEMENTATION
==================================================

Generate:

- Controllers
- Services
- Repositories
- DTOs
- Mappers
- Validators
- Event publishers
- Workflow execution engine
- Transition orchestrator
- Permission evaluators
- Workflow resolvers

For EVERY API provide:

- endpoint
- method
- request
- response
- validation
- RBAC
- audit behavior
- transaction boundaries

==================================================
PHASE 9 — MIGRATION LINKAGE
==================================================

Explain EXACTLY how workflow links during migration.

When migrating issue:
DO NOT directly insert issue.

Correct migration flow:

1. Resolve target project
2. Resolve workflow scheme
3. Resolve issue type mapping
4. Resolve target workflow
5. Resolve target statuses
6. Map source status → target status
7. Validate transitions
8. Validate mandatory fields
9. Create issue
10. Attach workflow runtime state
11. Replay history
12. Reindex

Explain:
- workflow translation layer
- status mapping engine
- workflow mismatch handling
- missing status recovery
- transition reconstruction

==================================================
PHASE 10 — SECURITY & PERMISSIONS
==================================================

Implement:
- RBAC
- project permissions
- transition permissions
- workflow edit permissions
- workflow publish permissions
- issue transition permissions

Transitions must dynamically disappear if:
- conditions fail
- permissions fail
- validators fail

==================================================
PHASE 11 — EVENT & AUTOMATION SYSTEM
==================================================

Implement event-driven workflow execution.

Events:
- ISSUE_CREATED
- ISSUE_UPDATED
- ISSUE_TRANSITIONED
- STATUS_CHANGED
- COMMENT_ADDED
- ATTACHMENT_ADDED

Explain:
- event propagation
- async processing
- webhook delivery
- retry queues
- DLQ handling
- automation hooks

==================================================
PHASE 12 — ENTERPRISE EDGE CASES
==================================================

You MUST handle:

- workflow deletion
- workflow replacement
- workflow versioning
- draft publishing
- concurrent transitions
- stale transitions
- issue locking
- failed post-functions
- partial rollback
- invalid workflow references
- orphan statuses
- missing permissions
- migration conflicts

==================================================
PHASE 13 — IMPLEMENTATION TASK BREAKDOWN
==================================================

Break implementation into:

EPICS
 → STORIES
   → SUBTASKS

For EACH task provide:
- objective
- technical implementation
- backend tasks
- frontend tasks
- DB tasks
- API tasks
- security tasks
- QA tasks
- acceptance criteria

==================================================
PHASE 14 — ENTERPRISE AUDIT
==================================================

Critically audit architecture.

Identify:
- missing workflow links
- isolated modules
- broken inheritance
- workflow deadlocks
- security gaps
- race conditions
- transition inconsistencies
- invalid state risks
- audit gaps
- scalability bottlenecks

Provide:
- remediation
- production-hardening
- HA recommendations
- observability recommendations

==================================================
FINAL REQUIREMENT
==================================================

The final implementation MUST behave like Jira Data Center:
- workflows are inherited dynamically
- issue behavior changes dynamically
- transitions are permission-aware
- validations execute correctly
- post-functions execute correctly
- workflows affect every issue operation
- no isolated functionality exists
- no fake workflow behavior exists
- no hardcoded transitions exist
- no hardcoded statuses exist

The platform must feel indistinguishable from enterprise Jira Data Center workflow architecture.