# SYSDOPS Gap-Fill Implementation Plan

> **Date:** 2026-07-25
> **Source:** 27 SYSDOPS documents (7 plugin presentations, 12 core process docs, 8 V&V templates)
> **Scope:** Jira Data Center parity -- features that exist in Jira DC + plugins that SYSDOPS relies on
> **Constraint:** Must not create functionality outside the boundary of Jira Data Center. Every feature maps to a real Jira DC plugin or built-in capability.

---

## Document Index (All 27 Files Analyzed)

| # | Document | Key Features Discovered |
|---|----------|----------------------|
| 1-8 | Original SYSDOPS DO/LAB Template + Archive | VVO/HLVVO lifecycle, XRAY integration, Change Cards, DCL sync, campaign automation, DOORS, TechEvent M1668, Bench Defect, Problem Report, Big Picture, XPorter reports |
| 9 | Xray Plugin Presentation | 6 Xray issue types, requirement coverage computation, test repository folders, defect linking from failed tests |
| 10 | Big Picture Plugin Presentation | 10 modules: Roadmap, Backlog, Schedule, Goals, Resources, Board, Teams, Risks, Reports, Overview |
| 11 | Automation Plugin Presentation | Trigger-Condition-Action rules, date cascade, branch rules for linked/sub-issues, Jira DC 9.0+ built-in |
| 12 | Custom Charts Plugin Presentation | 10+ chart types, JQL data source, Simple Search gadgets, Reference ID cross-gadget linking |
| 13 | Assets & Inventory Plugin Presentation | Asset types with attributes, status/location tracking, bidirectional issue linking, QR codes |
| 14 | Performance Objective Plugin Presentation | Multi-dimensional charts, pivot tables, 10 dashboard gadgets |
| 15 | Google Drive Plugin Presentation | Drive attachments section in issues, no file copying |
| 16 | Introduction (User View) | JQL querying (WAS/CHANGED operators, functions), smart keywords, saved filters, boards |
| 17 | Search & Filter Functions | Full JQL syntax, saved searches, board creation from filters |
| 18 | User Access Request | ServiceNow integration, coarse/fine grain access model |
| 19 | GIT Presentation | Git repository creation, triggers, branches from Jira |
| 20 | QuickStart | User roles (Admin/Maintainer/Contributor/Reader), BigPicture Gantt access |
| 21 | PCS System Design Guide (96 slides) | System Standard (17-state M1659.2 workflow), Review Sub-Task (RAG with auto-clone on Red), 7 PCS issue types, BigPicture Gantt integration |
| 22 | Project Configuration Synopsis | Workshop model for project configuration |
| 23 | Onboarding Process | 8-16 configuration workshops, migration, 1-month support |
| 24 | Production Incidents | ServiceNow incident template |
| 25 | Validation Access | Validation platform access process |
| 26 | CERTIF V&V Template | MOD issue type with MAJOR/MINOR workflow, 500+ tickets, kanban per ATA |
| 27 | Lab System Testing V1 | Test workflow (Draft->Approved), VVO search, test repository |

---

## Current State Summary

### What's Well-Implemented (42% -- 27 features)
- Xray test management (all 6 issue types, coverage, traceability, folders, steps)
- VVO/HLVVO lifecycle with DOORS integration, baselining, transfer, Diff History
- Workflow engine (30+ post-functions, 15+ triggers, conditions, GraalJS scripting)
- TechEvent M1668 workflow, BenchDefect, ProblemReport
- Change Card/Design Item/DCL/Deliverable metadata
- Plans/Roadmap with teams, capacity, dependencies, critical path
- JQL parser with most operators and date functions

### What's Partially Implemented (31% -- 20 features)
- BigPicture (roadmap exists, but no Goals, no Risk Chart, no Calendar)
- Automation (embedded in workflow transitions, not standalone rules)
- Custom Charts (gadget container exists, no chart-type config)
- JQL functions (parsed but placeholder execution)
- User roles (generic, not SYSDOPS-specific presets)

### What's Missing (27% -- 17 features)
- Assets and Inventory plugin
- System Standard + Review Sub-Task + M1659.2 workflow
- MOD issue type with MAJOR/MINOR branching
- XPorter DOCX/PDF template engine
- Standalone Automation Rule engine
- ServiceNow integration
- Google Drive integration
- Goals module
- Smart JQL keywords
- QR code generation

---

## Gap-Fill Plan: Prioritized by Business Value

### Tier 1: MUST-HAVE (Blocks core SYSDOPS workflows)

#### 1.1 System Standard + Review Sub-Task + M1659.2 Workflow
**Jira DC Plugin:** Native Jira issue types + workflow configuration + BigPicture
**Business Need:** PCS projects track system versions (EIF S1, EIF S2) through a 17-state process per Airbus Method M1659.2. Review Sub-Tasks represent milestones (PR, PDR, DDR, etc.) with RAG status.

**Implementation:**
- **Entities:** Add `SystemStandardMetadata` entity in issue-service (specFreezeDate, deliveryToLabDate, standardType LAB/FLIGHT, requestedLabClearanceDate, targetFlightDate)
- **Seed:** Register "System Standard" and "Review Sub-Task" issue types in admin-service V7 (already has the mechanism)
- **Workflow:** Add M1659.2 workflow in workflow-service (17 states: Backlog -> Internal KoM -> Common KoM -> PR -> FCR -> PDR -> DDR -> CDR -> LAR -> FAR -> FFR -> CR -> In Service Release -> In Service -> Certified -> Closed -> Cancelled)
- **Auto-creation:** Configure a workflow post-function that on System Standard creation, auto-creates 10 Review Sub-Task issues (Internal KoM, Common KoM, PR, FCR, PDR, DDR, CDR, LAR, FFR, CR) linked with "has to be done before"/"has to be done after"
- **RAG Review:** Add Review Sub-Task workflow (Backlog -> Planned -> Not Required | Passed Green | Passed Amber | Passed Red) with auto-clone on "Passed Red" creating a follow-up review
- **Files:** ~5 new files (1 entity, 1 migration, 1 workflow seed, 1 service method, 1 controller endpoint)

#### 1.2 Standalone Automation Rule Engine
**Jira DC Plugin:** Automation for Jira (built-in since DC 9.0)
**Business Need:** Users configure automatic actions (e.g., "when milestone end date changes, update all linked documentation end dates") without modifying workflow transitions.

**Implementation:**
- **Entity:** `AutomationRule` in workflow-service (name, description, projectId, trigger JSON, conditions JSON, actions JSON, isEnabled, executionCount, lastExecutedAt)
- **Trigger types:** ISSUE_CREATED, ISSUE_UPDATED, FIELD_CHANGED, STATUS_CHANGED, COMMENT_ADDED, SCHEDULED, MANUAL
- **Action types:** UPDATE_FIELD, CREATE_ISSUE, TRANSITION_STATUS, ADD_COMMENT, SEND_EMAIL, LINK_ISSUE, CLONE_ISSUE, ASSIGN_ISSUE
- **Branch rules:** FOR_EACH_LINKED_ISSUE, FOR_EACH_SUBTASK (iterate and apply actions)
- **Service:** `AutomationRuleService` evaluates rules on issue events
- **Controller:** `AutomationRuleController` for CRUD + manual trigger + execution log
- **Event listener:** Hook into existing workflow event outbox to trigger rules
- **Files:** ~8 new files (2 entities, 1 migration, 1 service, 1 controller, 2 DTOs, 1 event listener)

#### 1.3 XPorter-Style Document Template Engine
**Jira DC Plugin:** XPorter for Jira
**Business Need:** LTRA generation, VVO coverage reports, TechEvent lists -- all must export as DOCX/PDF with configurable templates and keyword substitution.

**Implementation:**
- **Entity:** `ExportTemplate` in report-service or test-service (name, description, templateType DOCX/XLSX/PDF/CSV, templateContent BYTEA, keywords JSON, createdBy)
- **Template engine:** Use Apache POI (XWPF for DOCX, XSSF for XLSX) -- already in issue-service's Maven dependency tree for import functionality
- **Keywords:** `${issueKey}`, `${summary}`, `${status}`, `${FOR_EACH:linked_tests}...${END_FOR_EACH}`, `${FILTER:status=RELEASED}`, `${COUNT:tests}`, `${COVERAGE_PERCENT}`
- **Pre-built templates:** Seed 5 templates matching SYSDOPS docs: "VVO export for Doors", "VVOs coverage from TestPlan", "Light VVOs coverage from TestPlan", "TechEvent List from TestPlan", "Test Runs Detailed from TestExecution"
- **Service:** `DocumentExportService` with `generateDocument(templateId, context)`
- **Controller:** `DocumentExportController` for template CRUD + document generation
- **Files:** ~7 new files (1 entity, 1 migration, 1 service, 1 controller, 2 DTOs, 1 template engine)

### Tier 2: SHOULD-HAVE (Improves productivity significantly)

#### 2.1 Custom Charts Enhancement
**Jira DC Plugin:** Custom Charts for Jira
**Business Need:** Dashboard gadgets with 10+ chart types driven by JQL queries, linked via Reference IDs.

**Implementation:**
- **Enhance** existing `GadgetInstance` entity with `chartType` field (PIE, BAR, LINE, DONUT, TABLE, STACKED_BAR, AREA, SCATTER, GAUGE, HEATMAP)
- **Add** `chartConfig` JSONB field for type-specific settings (segment colors, axis labels, data grouping)
- **Add** `referenceId` field for cross-gadget linking (Simple Search -> Custom Chart)
- **Service:** `ChartDataService` that executes JQL via search-service and transforms results into chart-ready data series
- **Migration:** Add columns to existing gadget tables
- **Files:** ~4 files (1 migration, 1 service, 1 DTO, modify 1 entity)

#### 2.2 JQL Function Execution + Smart Keywords
**Jira DC Plugin:** Built-in Jira JQL
**Business Need:** `latestReleasedVersion()`, `membersOf()`, `linkedIssues()` functions and smart keywords (`my`, `r:`, `c:`, `v:`, `overdue`) are used extensively in SYSDOPS saved filters.

**Implementation:**
- **Fix** `JQLParser.handleIssueFunction()` to actually execute functions by querying version-service, user-service, issue-link tables
- **Add** smart keyword preprocessing: `my` -> `assignee = currentUser()`, `r:me` -> `reporter = currentUser()`, `c:security` -> `component = "security"`, `overdue` -> `duedate < now()`, `v:3.2` -> `fixVersion = "3.2"`
- **Add** `WAS`/`CHANGED` history queries: query `change_items` table for historical state
- **Files:** ~3 files (modify JQLParser, add JQLFunctionExecutor service, add SmartKeywordPreprocessor)

#### 2.3 BigPicture Goals Module
**Jira DC Plugin:** BigPicture
**Business Need:** Strategic goal tracking aligned to epics/initiatives with progress visualization.

**Implementation:**
- **Entity:** `PlanGoal` in plan-service (name, description, status, targetDate, progress, parentGoalId, linkedEpicIds, color, ownerUserId)
- **Service:** `GoalService` with progress calculation from linked epic completion
- **Controller:** `GoalController` for CRUD
- **Files:** ~5 files (1 entity, 1 migration, 1 service, 1 controller, 1 DTO)

#### 2.4 SYSDOPS User Role Presets
**Jira DC Plugin:** Built-in Jira project roles
**Business Need:** 4 standardized roles (Administrator, Maintainer, Contributor, Reader) with documented permission matrices.

**Implementation:**
- **Seed migration:** Insert 4 role definitions with exact permission sets from the QuickStart guide
- **Permission matrix:** Administrator (all), Maintainer (manage users/boards, edit), Contributor (edit), Reader (read only)
- **Files:** 1 migration file

#### 2.5 MOD Issue Type with MAJOR/MINOR Workflow
**Jira DC Plugin:** Native Jira issue types + workflow conditions
**Business Need:** Certification project tracks modifications with different workflow paths based on severity classification.

**Implementation:**
- **Entity:** `ModificationMetadata` in issue-service (modType MAJOR/MINOR, ataChapter, certificationImpact)
- **Workflow:** Add MOD workflow with conditional branching (MAJOR path: full review chain, MINOR path: simplified approval)
- **Files:** ~4 files (1 entity, 1 migration, 1 workflow seed, modify controller)

### Tier 3: NICE-TO-HAVE (Enhances user experience)

#### 3.1 Assets and Inventory Plugin
**Jira DC Plugin:** Assets and Inventory for Jira (Appfire)
**Business Need:** Test bench hardware tracking (SIB, FIB rigs), linking assets to test executions.

**Implementation:**
- **New service or entities in admin-service:** `Asset` (name, assetType, status, subStatus, location, attributes JSONB), `AssetType` (name, attributeSchema JSONB), `AssetIssueLink` (assetId, issueId)
- **QR code:** Use ZXing library for QR generation
- **Files:** ~10 files (3 entities, 1 migration, 1 service, 1 controller, 3 DTOs, 1 QR utility)

#### 3.2 Google Drive Integration
**Jira DC Plugin:** Google Drive & Docs for Jira (Bilith)
**Business Need:** Link Google Drive documents to issues without copying files to server.

**Implementation:**
- **Enhance** existing `ExternalPageLink` entity with `linkProvider` field (GOOGLE_DRIVE, CONFLUENCE, SHAREPOINT)
- **Add** Drive-specific display formatting in frontend (file type icon, last modified date)
- **Files:** ~2 files (1 migration, modify frontend component)

#### 3.3 Risk Matrix Visualization
**Jira DC Plugin:** BigPicture Risks module
**Business Need:** Visual probability vs. impact grid for project risks.

**Implementation:**
- **Frontend component:** `RiskMatrixChart.tsx` that reads risk data from DeliverableMetadata (riskProbability, riskConsequence)
- **Files:** 1 frontend component

#### 3.4 Schedule Calendar View
**Jira DC Plugin:** BigPicture Schedule module
**Business Need:** Calendar-based view of milestones and test campaigns.

**Implementation:**
- **Frontend component:** `ScheduleCalendar.tsx` using existing plan-service schedule data
- **Files:** 1 frontend component

---

## Implementation Sequence

### Phase A: Tier 1 Critical Gaps (Estimated: 3 days)

| Step | Deliverable | Service | Files |
|------|------------|---------|-------|
| A.1 | System Standard + Review Sub-Task entities + M1659.2 workflow seed | issue-service, workflow-service | ~6 |
| A.2 | Automation Rule Engine (entities, service, controller, event listener) | workflow-service | ~8 |
| A.3 | Document Template Engine (XPorter) with 5 pre-built templates | test-service | ~7 |

### Phase B: Tier 2 Productivity Gains (Estimated: 2 days)

| Step | Deliverable | Service | Files |
|------|------------|---------|-------|
| B.1 | Custom Charts enhancement (chart types, config, reference ID) | dashboard-service | ~4 |
| B.2 | JQL functions + smart keywords + WAS/CHANGED history queries | search-service | ~3 |
| B.3 | BigPicture Goals module | plan-service | ~5 |
| B.4 | SYSDOPS user role presets (4 roles with permission matrices) | admin-service | ~1 |
| B.5 | MOD issue type with MAJOR/MINOR workflow | issue-service, workflow-service | ~4 |

### Phase C: Tier 3 Enhancements (Estimated: 1 day)

| Step | Deliverable | Service | Files |
|------|------------|---------|-------|
| C.1 | Assets and Inventory (entities, CRUD, QR codes) | admin-service | ~10 |
| C.2 | Google Drive link enhancement | issue-service, frontend | ~2 |
| C.3 | Risk Matrix visualization | frontend | ~1 |
| C.4 | Schedule Calendar view | frontend | ~1 |

---

## Regression Prevention Rules

1. **No new services.** All new entities go into existing services (issue-service, workflow-service, test-service, admin-service, plan-service, search-service, dashboard-service).
2. **No breaking schema changes.** All migrations use `IF NOT EXISTS` and `ON CONFLICT DO NOTHING`. New columns use `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`.
3. **No existing endpoint changes.** All new functionality uses new endpoints, never modifies existing endpoint contracts.
4. **No dependency additions** beyond what's already in pom.xml (except Apache POI for DOCX which may need explicit addition).
5. **Feature isolation.** Each gap-fill is a self-contained set of files that can be deployed independently.
6. **Compile verification.** After each step, `mvn compile` on affected services must pass.
7. **Jira DC boundary.** Every feature maps to a documented Jira DC plugin or built-in capability. No custom functionality that wouldn't exist in a real Jira DC deployment.

---

## Verification Checklist

After implementation, verify each feature matches its Jira DC plugin equivalent:

| Feature | Jira DC Plugin | Verification Method |
|---------|---------------|-------------------|
| System Standard + Reviews | Native issue types + BigPicture | Create System Standard -> verify 10 Review Sub-Tasks auto-created |
| Automation Rules | Automation for Jira (built-in DC 9.0+) | Create rule "on field change -> update linked issues" -> trigger -> verify cascade |
| Document Templates | XPorter for Jira | Upload DOCX template -> generate from Test Plan -> verify keywords substituted |
| Custom Charts | Custom Charts for Jira | Create pie chart gadget with JQL source -> verify data rendered |
| JQL Functions | Built-in Jira JQL | Execute `fixVersion = latestReleasedVersion()` -> verify correct version returned |
| Goals | BigPicture Goals | Create goal -> link epics -> verify progress aggregation |
| User Roles | Built-in Jira roles | Verify Contributor cannot manage users, Reader cannot edit issues |
| MOD Workflow | Native Jira workflows | Create MAJOR MOD -> verify full review chain; create MINOR -> verify simplified path |
| Assets | Assets & Inventory (Appfire) | Create asset type -> create asset -> link to issue -> generate QR code |
