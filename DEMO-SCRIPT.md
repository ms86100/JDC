# SYSDOPS JIRA — Live Demo Presentation Script

## Opening (2 minutes)

> Good morning/afternoon everyone. Thank you for joining today's demonstration of **SYSDOPS JIRA** — our tailored Application Lifecycle Management platform for Aircraft System Developments within Engineering 1Y.
>
> As you know, our development process follows a strict quality chain — **Specification, Implementation, and Verification** — with traceability at the centre. Every change must be linked to who created it, who reviewed it, and when.
>
> Today I'll walk you through four key modules that directly support this quality process:
> 1. **Project Management** — how we structure our aircraft programmes
> 2. **CSV Migration** — how we onboard existing data with just a few clicks
> 3. **Issue Tracking** — how System Designers create, assign, and track tasks
> 4. **Workflow Engine** — how we enforce transparent task processing with auditable status transitions
>
> Let me start by showing you the platform.

---

## Module 1: Project Management (5 minutes)

### Step-by-step Demo

1. **Navigate to Projects page** (`/projects`)
   > "The Projects module is where we define our aircraft programme work areas. Each project maps to a system development scope — for example, an avionics subsystem or a flight control package."

2. **Create a new project** — click "Create Project"
   - **Name**: `Flight Management System v2`
   - **Key**: `FMS`
   - **Project Type**: Company-managed
   - **Template**: Scrum
   - **Lead**: Select your user

   > "Notice we're using a Scrum template here. SYSDOPS JIRA ships with six project templates — Scrum, Kanban, Bug Tracking, Task Management, Portfolio, and Basic — each pre-configured with the right workflow scheme, issue types, and screen layouts for its purpose."

3. **Show project settings** — click on the project, then "Admin" or Settings
   > "Every project has its own configuration: members with role-based access, workflow schemes, notification schemes, and permission schemes. Access is granted on the **need-to-know principle** — only assigned team members see the work area. Project administrators can further restrict visibility using issue security schemes."

4. **Show project Backlog** — navigate to Backlog tab
   > "This is the backlog view — the single source of truth for all tasks in this system development scope. From here, System Designers can create issues, prioritise them, and organise sprints."

5. **Show Releases/Versions** — navigate to Releases tab
   - Create a version: `v1.0-RC1`
   > "We track releases here — each representing a deliverable milestone. For aircraft certification, this gives us traceability from requirement to delivery."

6. **Show Components** — navigate to Components tab
   - Create a component: `Navigation Logic`
   > "Components let us organise issues by subsystem or functional area — Navigation Logic, Sensor Interface, Display Rendering, and so on."

### Talking points
- Role-based access control aligned with the **Group Works Agreement** principles
- Project-level permission schemes ensure data protection compliance
- The need-to-know principle is enforced through role assignments reviewed at regular intervals

---

## Module 2: CSV Migration (5 minutes)

### Step-by-step Demo

1. **Navigate to Migration Centre** (`/migration?import=csv`)
   > "One of the most powerful features for our team is the Migration Centre. When onboarding from legacy tools or consolidating data from different Jira instances across sites, System Designers need a seamless way to bring their work history into SYSDOPS."

2. **Select CSV Import** — click on "CSV Import" card
   > "We support CSV, Excel, and Jira Data Center XML import formats. Today I'll show the CSV path — it's the most common for cross-site data consolidation."

3. **Upload the CSV file** — drag and drop `Jira 2026-07-13T11_42_56+0200.csv`
   > "This is a real export from an existing Jira instance with 39 issues — tasks from our SLM System team. Notice how the platform instantly parses the file, detects 39 rows, identifies the entity type as ISSUE, and shows a preview."

4. **Select target project** — choose `Flight Management System v2`
   > "We map the imported data to our target project. The original issue keys — like SST1-39 — are preserved, maintaining full traceability back to the source system."

5. **Field Mapping step**
   > "Here's where the intelligence is. The system automatically maps CSV columns to target fields — Issue Type, Summary, Status, Custom Fields. The auto-mapping uses alias recognition, so whether your source calls it 'Issue Type', 'Type', or 'IssueType', it finds the right match."

6. **Validate** — click Next to validation
   > "Before any data touches the database, we run a full dry-run validation — checking required fields, data formats, value constraints. Zero errors means we're clear to import."

7. **Execute Import** — click Import
   > "With one click, all 39 issues are imported. The original issue keys are preserved, statuses are applied — To Do, In Review, Done — and custom fields are mapped. Full audit trail is recorded."

8. **Show the imported issues** — navigate to project backlog
   > "And here they are — all 39 issues, with their original SST1 keys, correct statuses, and full field data. This is what used to take days of manual data entry, done in under a minute."

### Talking points
- Preserves **traceability chain** from source system
- Supports EASA retention requirements — imported data retains original timestamps
- Cross-site and cross-programme data consolidation
- Validation-first approach prevents data quality issues

---

## Module 3: Issue Tracking (5 minutes)

### Step-by-step Demo

1. **Open an imported issue** — click on any SST1-XX issue
   > "This is the Issue Detail view — the primary workspace for System Designers. Every field that matters for aircraft system development is here."

2. **Walk through the layout**:
   - **Header**: Issue key (SST1-4), Type badge (Task), Status badge (To Do)
   - **Description**: Rich text description area
   - **Right sidebar**: People (Assignee, Reporter), Details (Priority, Resolution, Components, Labels, Security Level), Custom Fields

   > "Notice the structured layout — it follows Atlassian conventions that our engineers are already familiar with, but it's customised for our quality process. The Security Level field, for example, supports the requirement from our Works Agreement that tasks can be restricted to only the creator, owner, and manager."

3. **Add a comment**
   > "Comments support our collaboration model — especially across disciplines and locations. Every comment is timestamped and attributed, supporting our compliance documentation requirements."

4. **Show Activity tab**
   > "The Activity tab provides a complete audit trail — every status change, field update, and comment is recorded with who made the change and when. This is critical for our EASA traceability requirements."

5. **Show Workflow Transitions** — click the Status button (e.g., "To Do")
   > "Status transitions are governed by the project's workflow — I can move this task to 'In Progress' by clicking 'Start Progress'. The workflow ensures transparent task processing with defined states like Open, In Progress, In Review, Done, and Closed."

6. **Execute a transition** — click "Start Progress"
   > "Done. The status updates immediately, and the transition is recorded in the audit history. If this workflow had validators — for example, requiring an assignee before starting progress — the system would enforce that automatically."

7. **Create a new issue** — click "+ Create Issue"
   - **Type**: Task
   - **Summary**: `Implement FMS waypoint calculation module`
   - **Description**: `Implement the core waypoint calculation logic per specification FMS-SPEC-2026-001`
   - **Priority**: High

   > "Creating issues is straightforward — type, summary, description, priority. The issue gets an auto-generated key following the project prefix."

8. **Show Labels, Links, Work Log tabs**
   > "We also support labels for categorisation, issue links for dependency tracking between specifications, and work logging for time tracking — all supporting our planning and needs identification requirements."

### Talking points
- Full **History function** records which user made which change — as required by our system description
- Compliance-ready documentation for audits, certifications, and customer evidence
- File attachments supported for specification documents
- Dashboards and filters available for project oversight

---

## Module 4: Workflow Management (3 minutes)

### Step-by-step Demo

1. **Navigate to Workflow Admin** (`/workflows/admin`)
   > "Workflows are the backbone of our quality process. They define the allowed state transitions for each issue type — ensuring that tasks follow the prescribed path from creation to completion."

2. **Show the Scrum Workflow**
   > "Our default Scrum workflow has five statuses: Backlog, To Do, In Progress, In Review, and Done. Each transition has a defined source and target — for example, you can only move from 'In Progress' to 'In Review', not directly to 'Done'. This enforces our review gate."

3. **Show available workflows**
   > "We ship with five pre-configured workflows: Scrum, Kanban, Bug, Task, and Portfolio. Each is tailored for a specific work pattern. The Portfolio workflow, for instance, supports our programme-level planning with statuses like Backlog, Defined, In Progress, and Done."

4. **Explain workflow schemes**
   > "Workflow schemes map workflows to issue types per project. So a project can use the Bug Workflow for defect tracking and the Scrum Workflow for feature development — all within the same project, with different state machines for different issue types."

### Talking points
- Transparent task processing with auditable state transitions
- Workflow validators and conditions enforce quality gates
- Configurable per project and issue type
- Supports the statuses defined in our system description: open, in progress, reviewed, done, closed

---

## Closing (2 minutes)

> "Let me summarise what we've seen today:
>
> **SYSDOPS JIRA** is purpose-built for our Aircraft System Development quality process. It provides:
>
> - **Structured project management** with role-based access and the need-to-know principle
> - **Seamless data migration** from legacy systems with full traceability preservation
> - **Comprehensive issue tracking** with audit trails for EASA compliance
> - **Configurable workflows** that enforce our quality gates and transparent task processing
>
> The platform stores only user ID, name, and login timestamps — no additional personal data beyond what's required for traceability. All processing serves exclusively the distribution and resolution of work tasks, traceability of processing status, and project management.
>
> Performance or behaviour monitoring is explicitly excluded from this system.
>
> We're fully aligned with the Group Works Agreement principles, and the system is designed to remove manual, non-value-added tasks from our System Designers — letting them focus on what matters: building safe, reliable aircraft systems.
>
> Thank you. I'm happy to take questions."

---

## Quick Reference — URLs for Demo

| Module | URL |
|--------|-----|
| Projects | `/projects` |
| Create Project | Click "Create Project" on Projects page |
| Migration Centre | `/migration?import=csv` |
| Project Backlog | `/projects/{id}/backlog` |
| Issue Detail | Click any issue in the backlog |
| Workflow Admin | `/workflows/admin` |
| User Management | `/admin/users` |
| System Health | Migration page → Downstream Services panel |

## Pre-Demo Checklist

- [ ] All services running (`docker compose ps` — 18+ healthy)
- [ ] Database clean (no stale test data)
- [ ] CSV file ready: `Jira 2026-07-13T11_42_56+0200.csv`
- [ ] Logged in as admin user
- [ ] Browser cache cleared (`Ctrl+Shift+R`)
- [ ] No console errors on page load
