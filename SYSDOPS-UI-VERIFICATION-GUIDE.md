# SYSDOPS Aircraft Design System -- UI Verification Guide

> **Version:** 1.1 | **Date:** 2026-07-25
> **Services Required:** jira-auth-service (8081), jira-admin-service (8093), jira-project-service (8083), jira-issue-service (8084), jira-workflow-service (8085), jira-test-service (8095), jira-gateway (8080), jira-frontend (3000), PostgreSQL (5432)

---

## Seed Data Reference

The system comes pre-loaded with test data for immediate verification. No manual data entry required.

| Data Type | Count | Key IDs |
|-----------|-------|---------|
| **Project** | 1 | ID: `10000000-0000-0000-0000-000000000001`, Key: `NFMS` (nFMS System Development & Testing) |
| **Baselines (Fix Versions)** | 3 | Baseline 1: `20000000-...01`, Baseline 2: `20000000-...02`, Baseline 3: `20000000-...03` |
| **HLVVOs** | 2 | HLVVO-1 (DIR TO, AUTHORIZE), HLVVO-2 (HOLD, VVO_WRITING_IN_PROGRESS) |
| **VVOs** | 15 | 3 NEW, 3 TO_BE_VERIFIED, 3 VERIFIED, 3 RELEASED, 2 CANCELLED, 1 SUPERSEDED |
| **Test Requests** | 2 | LTR-1 (Lab Test Request), FTR-1 (Flight Test Request) |
| **TechEvents** | 8 | 2 OPEN, 1 UNDER_ORIGINATOR_ANALYSIS, 1 UNDER_RESOLVER_ANALYSIS, 1 CLASSIFIED, 1 RESOLVED_CORRECTED, 1 CLOSED, 1 CANCELLED |
| **Bench Defects** | 4 | 1 BLOCKING, 1 HIGH/P1, 1 LOW, 1 CLOSED |
| **Problem Reports** | 3 | 1 OPEN (CAT/HAZ), 1 UNDER_ANALYSIS (Functional), 1 CLOSED |
| **Test Cases** | 8 | DIR TO, HOLD, OFFSET tests in APPROVED status |
| **Test Executions** | 2 | 1 FAILED, 1 PASSED |
| **Issues (Change Cards)** | 8 | 5 Change Cards, 1 Design Item, 1 DCL, 1 Deliverable |
| **Components** | 6 | Lateral Guidance, Vertical Guidance, NavDB, MCDU, Performance, Datalink |
| **Master Data** | 300+ | 5 programs, 50 test means, 185 systems, 26 functions, 20 teams, 39 defect origins |

**Use this project ID for all page URLs:** `10000000-0000-0000-0000-000000000001`
**Use this fix version ID for baselines:** `20000000-0000-0000-0000-000000000001`

---

## Prerequisites

### 1. Start the Platform

```bash
# Option A: Docker Compose (recommended for production-like testing)
docker-compose up -d

# Option B: Local development
# Terminal 1: Start PostgreSQL
docker run -d -p 5432:5432 -e POSTGRES_DB=jira_platform -e POSTGRES_USER=jiraadmin -e POSTGRES_PASSWORD=jirapass postgres:16-alpine

# Terminal 2-7: Start each service
cd jira-auth-service && mvn spring-boot:run
cd jira-admin-service && mvn spring-boot:run
cd jira-project-service && mvn spring-boot:run
cd jira-issue-service && mvn spring-boot:run
cd jira-workflow-service && mvn spring-boot:run
cd jira-test-service && mvn spring-boot:run

# Terminal 8: Start frontend
cd jira-frontend && npm run dev
```

### 2. Dev Proxy Configuration (Local Dev Only)

Add these proxy rules to `jira-frontend/vite.config.ts` inside the `proxy` block:

```typescript
'/api/vvo': { target: 'http://localhost:8095', changeOrigin: true },
'/api/hlvvo': { target: 'http://localhost:8095', changeOrigin: true },
'/api/tech-events': { target: 'http://localhost:8095', changeOrigin: true },
'/api/bench-defects': { target: 'http://localhost:8095', changeOrigin: true },
'/api/problem-reports': { target: 'http://localhost:8095', changeOrigin: true },
'/api/vv-reports': { target: 'http://localhost:8095', changeOrigin: true },
'/api/campaigns': { target: 'http://localhost:8095', changeOrigin: true },
```

### 3. Login

Navigate to `http://localhost:3000/login` and log in with:
- Username: `ms86100` | Password: `admin123`
- Or: `john.smith` / `password123`

---

## Verification Steps

---

### STEP 1: Master Data Administration

**URL:** `http://localhost:3000/aircraft-design/master-data`

**What to verify:**

| # | Action | Expected Result | Pass/Fail |
|---|--------|----------------|-----------|
| 1.1 | Navigate to the URL | Page loads with 8 tabs: Programs, Test Means, Systems, ATA Chapters, Suppliers, Functions, Reporter Teams, Defect Origins | |
| 1.2 | Click "Programs" tab | Table shows 5 seeded programs: SA_CEONEO, SA_NAX, LR_CEONEO, A350, A380 | |
| 1.3 | Click "Add" button on Programs tab | Modal appears with fields: Code, Name, Description | |
| 1.4 | Create a new program: Code=`A220`, Name=`A220 Program` | New row appears in the table. Success toast shown | |
| 1.5 | Click "Test Means" tab, select SA_CEONEO from program dropdown | Table shows 10 test means: SIB, FIB, SIMULATOR, S23, CGIB, IVP, CSB, vFiB, HLSVE, FLIGHT_TEST | |
| 1.6 | Click "Systems" tab, select A350 from program dropdown | Table shows 37 aircraft systems (ACR, AFDX, ADIRS, ... SPP) | |
| 1.7 | Click "Functions" tab, select a system (FMS) | Table shows 27 functions (00-Non Functional through 26-Automated Operations) | |
| 1.8 | Click "Reporter Teams" tab | Table shows 20 teams: Actuators, AFS, Aircraft Operations, Autopilot, Display A350, etc. | |
| 1.9 | Click "Defect Origins" tab | Table shows 9 root categories. Click INSTRUMENTATION_AND_TOOLS to see 22 sub-items | |
| 1.10 | Edit an existing program (click edit icon), change description | Description updates successfully | |
| 1.11 | Deactivate a test mean (click delete icon) | Item disappears from active list (soft delete) | |

**API Endpoints Exercised:**
- `GET /api/admin/master-data/programs`
- `POST /api/admin/master-data/programs`
- `GET /api/admin/master-data/programs/{id}/test-means`
- `GET /api/admin/master-data/programs/{id}/systems`
- `GET /api/admin/master-data/systems/{id}/functions`
- `GET /api/admin/master-data/reporter-teams`
- `GET /api/admin/master-data/defect-origins`
- `GET /api/admin/master-data/defect-origins/{id}/sub-items`

---

### STEP 2: VVO Management

**URL:** `http://localhost:3000/aircraft-design/vvos?projectId=10000000-0000-0000-0000-000000000001`

> Note: You need a valid `projectId` UUID from the project-service. Use the Projects page to find one, or use the default project UUID from seed data.

**What to verify:**

| # | Action | Expected Result | Pass/Fail |
|---|--------|----------------|-----------|
| 2.1 | Navigate to VVO list page | Page loads with status filter pills (ALL, NEW, TO_BE_VERIFIED, VERIFIED, RELEASED, CANCELLED, SUPERSEDED) and a table | |
| 2.2 | Click "Create VVO" button | Modal/form appears with fields: Summary, Description, Execution Responsible (checkboxes), VVO Usage (multi-select), VVO Scope (select), Applicability (multi-select), etc. | |
| 2.3 | Fill in: Summary=`Test VVO for DIR TO sequencing`, VVO Usage=`FORMAL_VERIFICATION`, VVO Scope=`FUNCTIONAL`, Applicability=`SA_CEONEO` | VVO created successfully. New row appears in table with status "NEW" and Issue Key "VVO-1" | |
| 2.4 | Click on the VVO issue key to open detail page | Detail page loads at `/aircraft-design/vvos/{id}` with all fields organized in sections: General, Classification, Test Means, Systems, Content, Planning, Requirements | |
| 2.5 | Edit the VVO: add Operational Conditions and Expected Results | Fields update and save button confirms success | |
| 2.6 | Click "Clone" action on a VVO | New VVO created with vvoVersion=2, cloneSourceId pointing to original, hlvvoId and fixVersionId cleared, status="NEW" | |
| 2.7 | Filter by status "NEW" | Only VVOs with status NEW are shown | |
| 2.8 | Search for "DIR TO" | Only VVOs containing "DIR TO" in summary are shown | |
| 2.9 | Click "Archive" on a VVO | VVO disappears from list (archived=true) | |

**API Endpoints Exercised:**
- `GET /api/vvo/project/10000000-0000-0000-0000-000000000001`
- `POST /api/vvo`
- `GET /api/vvo/{id}`
- `PUT /api/vvo/{id}`
- `POST /api/vvo/{id}/clone`
- `DELETE /api/vvo/{id}`

---

### STEP 3: HLVVO Management

**URL:** `http://localhost:3000/aircraft-design/hlvvos?projectId=10000000-0000-0000-0000-000000000001`

| # | Action | Expected Result | Pass/Fail |
|---|--------|----------------|-----------|
| 3.1 | Navigate to HLVVO list page | Page loads with HLVVO table | |
| 3.2 | Create HLVVO: Summary=`DIR TO Verification Package`, Target Date=next month | HLVVO created with status "NEW" | |
| 3.3 | Expand an HLVVO row | Shows list of child VVOs (linked via hlvvoId) | |
| 3.4 | Go to a VVO and set its hlvvoId to the HLVVO just created | VVO appears as child when HLVVO is expanded | |

**API Endpoints Exercised:**
- `GET /api/hlvvo/project/10000000-0000-0000-0000-000000000001`
- `POST /api/hlvvo`
- `GET /api/hlvvo/{id}/child-vvos`

---

### STEP 4: VVO Baselining

**URL:** `http://localhost:3000/aircraft-design/baselines?projectId=10000000-0000-0000-0000-000000000001`

| # | Action | Expected Result | Pass/Fail |
|---|--------|----------------|-----------|
| 4.1 | Navigate to baseline management page | Page loads with Project ID and Fix Version ID input fields | |
| 4.2 | Enter a project ID and fix version ID, click "Load Baseline" | Baseline summary shows counts (Released, Verified, Cancelled, Superseded) and VVO list | |
| 4.3 | Select multiple VVOs using checkboxes, click "Tag Baseline" | Success response with count of tagged VVOs | |
| 4.4 | Click "Publish Baseline" | All VERIFIED VVOs transition to RELEASED (done via WorkflowBridgeService, which calls the workflow engine) | |
| 4.5 | Click "Clone with Supersede" on a VVO | New version created, original automatically transitions to SUPERSEDED (via WorkflowBridgeService) | |
| 4.6 | View baseline summary after publish | Released count increases, Verified count decreases | |

**API Endpoints Exercised:**
- `GET /api/vvo/baseline/summary`
- `POST /api/vvo/baseline/tag`
- `POST /api/vvo/baseline/publish`
- `POST /api/vvo/baseline/clone-with-supersede/{id}`

**Workflow Engine Verification:**
- Open workflow-service logs. On "Publish Baseline", you should see logs showing the workflow engine was called (or fallback was used if workflow-service was down)
- Check that `VvoBaselineService.publishBaseline()` calls `workflowBridge.executeTransition("VVO", ..., "RELEASED", ...)` (not direct status string manipulation)

---

### STEP 5: DOORS Export/Import

**URL:** Same baseline management page, scroll to DOORS section

| # | Action | Expected Result | Pass/Fail |
|---|--------|----------------|-----------|
| 5.1 | Click "Export for DOORS" with project ID and fix version | CSV file downloads with columns: Issue key, Summary, Status, VVO Version, Applicability, Supplier Applicability, Operational Conditions, Expected Results | |
| 5.2 | Open the CSV, add "ID Doors" column values (e.g., `DOORS-001`, `DOORS-002`) | CSV ready for import | |
| 5.3 | Upload the modified CSV in the "Import DOORS IDs" section | Import runs with 6 validations. Success message shows updated count | |
| 5.4 | Try importing with a duplicate ID Doors value | Validation error: "Duplicate ID Doors: DOORS-001" | |
| 5.5 | Try importing with an empty ID Doors | Validation error: "ID Doors is empty for issue VVO-1" | |
| 5.6 | Verify VVOs now have idDoors field populated | Navigate to VVO detail -- idDoors field shows the imported value | |

**API Endpoints Exercised:**
- `POST /api/vvo/baseline/doors/export`
- `POST /api/vvo/baseline/doors/import`

---

### STEP 6: VVO Transfer (DO to LAB)

**URL:** Same baseline management page, scroll to Transfer section

| # | Action | Expected Result | Pass/Fail |
|---|--------|----------------|-----------|
| 6.1 | Enter source project ID (DO), target project ID (LAB), fix version ID | Fields populated | |
| 6.2 | Check "Preview Only" and click "Transfer" | Response shows CREATE/UPDATE preview without persisting changes | |
| 6.3 | Uncheck "Preview Only" and click "Transfer" | VVOs created/updated in target project. Created count shown. Status set to "UPDATE" for existing VVOs | |
| 6.4 | Navigate to VVO list for target (LAB) project | Transferred VVOs appear with their DOORS IDs | |

**API Endpoints Exercised:**
- `POST /api/vvo/baseline/transfer`

---

### STEP 7: TechEvent Defect Management (M1668)

**URL:** `http://localhost:3000/aircraft-design/tech-events?projectId=10000000-0000-0000-0000-000000000001`

| # | Action | Expected Result | Pass/Fail |
|---|--------|----------------|-----------|
| 7.1 | Navigate to TechEvent list page | Page loads with pipeline visualization: Open -> Analysis -> Resolver -> Classified -> Assessed -> Resolved -> Closed | |
| 7.2 | Click "Create Tech Event" | Form with fields: Summary, Description, Reporter Team (dropdown from master data), Team for Analysis, Detected on Program (cascading dropdown), Detected on Date, Priority | |
| 7.3 | Create TechEvent: Summary=`AFDX network timeout during DIR TO`, Reporter Team=`Flight Management`, Detected on Program=`SA_CEONEO` | TechEvent created with status "OPEN" | |
| 7.4 | On the detail page, verify cascading dropdowns: select Program, then Test Mean dropdown shows only test means for that program | Test Means, Systems, ATA Chapters filter based on selected program | |
| 7.5 | Click "Analyze" transition button | Status changes to UNDER_ORIGINATOR_ANALYSIS (via WorkflowBridgeService -> workflow engine) | |
| 7.6 | Click "Transfer to Resolver" | Status changes to UNDER_RESOLVER_ANALYSIS | |
| 7.7 | Click "Share with Supplier" (provide supplier project ID) | Copy of TechEvent created in supplier project, sync fields linked | |
| 7.8 | Click "Classify" (after setting defect_type, defect_origin, defect_impact -- these are required validators) | Status changes to CLASSIFIED. If required fields are empty, validation error shown | |
| 7.9 | Continue through: Assessment Ready -> Correction Verified OK -> Close | Status transitions through TO_BE_ASSESSED -> RESOLVED_CORRECTED -> CLOSED | |
| 7.10 | Try an invalid transition (e.g., OPEN -> CLOSED directly) | Error: "Invalid transition from OPEN to CLOSED" | |
| 7.11 | View "Available Transitions" | Shows only valid next states for current status | |
| 7.12 | Click "Create Bench Defect" from TechEvent | New BenchDefect created with fields copied from TechEvent, linked via sourceTechEventId | |
| 7.13 | Click "Create Problem Report" from TechEvent | New ProblemReport created with fields copied, linked via linkedTechEventId. TechEvent's linkedProblemReportId updated | |

**API Endpoints Exercised:**
- `GET /api/tech-events/project/10000000-0000-0000-0000-000000000001`
- `POST /api/tech-events`
- `GET /api/tech-events/{id}`
- `POST /api/tech-events/{id}/transition?targetStatus=UNDER_ORIGINATOR_ANALYSIS`
- `GET /api/tech-events/{id}/available-transitions`
- `POST /api/tech-events/{id}/share-supplier?supplierProjectId={id}`
- `POST /api/tech-events/{id}/create-bench-defect`
- `POST /api/tech-events/{id}/create-problem-report`

**Workflow Engine Verification:**
- Check workflow-service logs for transition execution requests
- Validators fire on "Classify": defect_type, defect_origin, defect_impact are REQUIRED
- Validators fire on "Close": public_analysis is REQUIRED

---

### STEP 8: Change Card (6-Tab Layout)

**URL:** `http://localhost:3000/aircraft-design/change-cards?issueId=40000000-0000-0000-0000-000000000001`

> Note: You need a valid issue ID. Create an issue first via the Issues page, then navigate here with its ID.

| # | Action | Expected Result | Pass/Fail |
|---|--------|----------------|-----------|
| 8.1 | Navigate to Change Card page with an issueId | 7 tabs visible: Design, EIF, Planning, Review, Certification, Maturity Test, Safety | |
| 8.2 | **Design tab**: Set Change Type=ANOMALY, add Impact description, select Function | Fields save. Classification dropdown shows EASA types | |
| 8.3 | **EIF tab**: Set ICD CNTRL Impact=Yes, ICD BITE Impact=No, SCADE Impact=TBC | Dropdowns work with TBC/Yes/No options | |
| 8.4 | **Review tab**: Set Design Review Status=Green (RAG indicator) | Green indicator shown | |
| 8.5 | **Certification tab**: Set Classification=Type 1A, fill Current Behavior and Change Rationale | Fields save for certification documentation | |
| 8.6 | **Maturity Test tab**: Set Maturity Test=Yes, Priority=P1 High | Objective text field appears when Maturity Test=Yes | |
| 8.7 | **Safety tab**: Set Safety Review Required=Yes, fill Safety Design Analysis | Safety Review Status and Assignee fields become relevant | |
| 8.8 | Switch between tabs | Data persists across tab switches (no data loss) | |

**API Endpoints Exercised:**
- `GET /api/issues/40000000-0000-0000-0000-000000000001/change-card`
- `POST /api/issues/40000000-0000-0000-0000-000000000001/change-card`
- `PUT /api/issues/40000000-0000-0000-0000-000000000001/change-card`

---

### STEP 9: V&V Project Dashboard

**URL:** `http://localhost:3000/aircraft-design/dashboard?projectId=10000000-0000-0000-0000-000000000001`

| # | Action | Expected Result | Pass/Fail |
|---|--------|----------------|-----------|
| 9.1 | Navigate to dashboard with a project ID | Page loads with 6 KPI cards and charts | |
| 9.2 | Verify KPI cards show: Total VVOs, Verified, Released, Open TechEvents, Blocking BenchDefects, Open Problem Reports | Numbers reflect actual data from the project | |
| 9.3 | Verify coverage percentage gauge | Shows percentage of VVOs that have linked tests | |
| 9.4 | Verify status distribution bar chart | VVO status breakdown (NEW/VERIFIED/RELEASED/etc.) | |
| 9.5 | Verify TechEvent trend chart | Shows tech events by status category | |
| 9.6 | Verify BenchDefect severity donut chart | Shows BLOCKING/HIGH/LOW distribution | |
| 9.7 | Click "View All VVOs" quick link | Navigates to `/aircraft-design/vvos?projectId={id}` | |

**API Endpoints Exercised:**
- `GET /api/vv-reports/dashboard/10000000-0000-0000-0000-000000000001`

---

### STEP 10: Campaign Creation from CSV

**URL:** `http://localhost:3000/aircraft-design/campaigns?projectId=10000000-0000-0000-0000-000000000001`

| # | Action | Expected Result | Pass/Fail |
|---|--------|----------------|-----------|
| 10.1 | Navigate to campaign page | Page loads with CSV upload area | |
| 10.2 | Prepare a CSV file with columns: `ID Doors,VVO Summary,Applicability,Version,Fix Version,Priority` | File ready | |
| 10.3 | Upload the CSV file | Page parses CSV and shows preview table with VVO rows | |
| 10.4 | Click "Create Campaign" (provide Test Plan ID) | Campaign created. Response shows: VVOs processed, TestExecutions created, errors (if any) | |
| 10.5 | Re-upload same CSV and create again | Idempotent: no duplicate TestExecutions created. Log shows "already exists" for existing pairs | |

**API Endpoints Exercised:**
- `POST /api/campaigns/create-from-csv/{testPlanId}`

---

### STEP 11: Reporting

**Verify via API directly (Swagger or curl):**

| # | Endpoint | Expected Result | Pass/Fail |
|---|----------|----------------|-----------|
| 11.1 | `GET /api/vv-reports/coverage?projectId={id}&fixVersionId={id}` | JSON with totalVvos, coveredVvos, notCoveredVvos, coveragePercentage, items[] array | |
| 11.2 | `GET /api/vv-reports/coverage/export?projectId={id}&fixVersionId={id}` | CSV file download with VVO coverage data | |
| 11.3 | `GET /api/vv-reports/tech-events?projectId={id}` | JSON with totalEvents, openCount, closedCount, countByStatus map, countByDefectType map | |
| 11.4 | `GET /api/vv-reports/bench-defects?projectId={id}` | JSON with totalDefects, countByStatus, countBySeverity | |
| 11.5 | `GET /api/vv-reports/problem-reports?projectId={id}` | JSON with totalReports, openCount, countByPrType, countByPrOrigin | |
| 11.6 | `GET /api/vv-reports/planning-export/{testPlanId}` | CSV with columns: Component, Test Means, Priority, Test Count, Estimated Duration | |

---

### STEP 12: Workflow Engine Integration Verification

This verifies the critical integration between entity status management and the workflow engine.

| # | Action | Expected Result | Pass/Fail |
|---|--------|----------------|-----------|
| 12.1 | Start workflow-service and test-service | Both services start without errors | |
| 12.2 | Check workflow-service DB: `SELECT * FROM jira_workflow.workflow_schemes WHERE name = 'Aircraft Design System Scheme'` | One row returned with 10 mappings | |
| 12.3 | Check workflow scheme mappings: `SELECT wsm.*, w.name as workflow_name FROM jira_workflow.workflow_scheme_mappings wsm JOIN jira_workflow.workflows w ON wsm.workflow_id = w.id WHERE wsm.scheme_id = 'a1000000-0000-0000-0000-000000000001'` | 10 rows: VVO->VVO Workflow, HLVVO->HLVVO Workflow, etc. | |
| 12.4 | Create a TechEvent and transition it via the API: `POST /api/tech-events/{id}/transition?targetStatus=UNDER_ORIGINATOR_ANALYSIS` | Status changes. Check test-service logs for: "Workflow transition executed via engine" (if workflow-service is up) or "falling back to local validation" (if down) | |
| 12.5 | Publish a VVO baseline: `POST /api/vvo/baseline/publish?projectId=...&fixVersionId=...` | VVOs transition to RELEASED. Check logs for WorkflowBridgeService calls | |
| 12.6 | Clone-with-supersede a VVO: `POST /api/vvo/baseline/clone-with-supersede/{id}` | Original VVO transitions to SUPERSEDED via WorkflowBridgeService. New clone has version+1 | |
| 12.7 | Check WorkflowInternalController: `GET /api/issues/{vvoId}` on test-service port (8095) | Returns VVO data in workflow-compatible format with statusId, issueTypeKey, assigneeId | |
| 12.8 | Check workflow-service can reach test-service: verify `jira.services.test-url` in workflow-service application.yml | Value: `http://jira-test-service:8095` (Docker) or `http://localhost:8095` (local) | |

---

### STEP 13: Advanced Features Verification

| # | Action | Expected Result | Pass/Fail |
|---|--------|----------------|-----------|
| 13.1 | **Diff History**: Call `POST /api/vvo/{cloneId}/diff?comparedWithId={originalId}&mode=difference` (if endpoint exposed) or verify DiffHistoryService exists by checking service logs | HTML report with color-coded field differences between original and clone | |
| 13.2 | **VVO-Test Consistency**: Call service method or verify via logs | Consistency check returns mismatches in Component/Applicability/Supplier between VVOs and linked tests | |
| 13.3 | **Frozen TestRequest**: Create a TestRequest, transition to DONE, then try to link a VVO | Error: "Cannot modify links on a frozen Test Request" | |
| 13.4 | **Workflow Notifications**: Transition a TechEvent and check test-service logs | Logs show: "Notifying assignee {userId} of status transition" | |

---

## Quick Smoke Test (5 Minutes)

If you need to verify the system works end-to-end in 5 minutes:

1. Start all services + frontend
2. Login at `http://localhost:3000/login`
3. Navigate to `http://localhost:3000/aircraft-design/master-data` -- verify 5 programs load
4. Navigate to `http://localhost:3000/aircraft-design/vvos?projectId={any-uuid}` -- create a VVO
5. Navigate to `http://localhost:3000/aircraft-design/tech-events?projectId={any-uuid}` -- create a TechEvent and transition it
6. Navigate to `http://localhost:3000/aircraft-design/dashboard?projectId={any-uuid}` -- verify KPI cards show data
7. Navigate to `http://localhost:3000/aircraft-design/change-cards` -- verify 7 tabs render

---

## Swagger API Documentation

All backend services expose Swagger UI:

| Service | Swagger URL |
|---------|------------|
| Admin (Master Data) | `http://localhost:8093/swagger-ui.html` |
| Test (VVO, TechEvent, Reports) | `http://localhost:8095/swagger-ui.html` |
| Issue (Change Card, Deliverable) | `http://localhost:8084/swagger-ui.html` |
| Workflow (State Machines) | `http://localhost:8085/swagger-ui.html` |

Use Swagger to test individual endpoints when the frontend cannot reach a specific API.

---

## Appendix: Complete File Inventory (Phase 1-4)

### jira-admin-service (37 new files)
- 8 entities, 8 repositories, 16 DTOs, 1 service, 1 controller, 2 migrations, 1 bootstrap

### jira-test-service (75+ new/modified files)
- 7 entities (VVO, HLVVO, TestRequest, VvoTestRequestLink, TechEvent, BenchDefect, ProblemReport)
- 7 repositories + 2 updated repos (TestExecutionRepository, BenchDefect/ProblemReport with countByProjectId)
- 22 DTOs
- 10 services (VvoService, VvoBaselineService, DoorsIntegrationService, VvoTransferService, DefectManagementService, TechEventWorkflowService, CampaignAutomationService, VvReportingService, WorkflowBridgeService, DiffHistoryService, VvoTestConsistencyService, WorkflowNotificationService, MasterDataClientService)
- 10 controllers (VvoController, HlvvoController, VvTestRequestController, VvoBaselineController, TechEventController, TechEventWorkflowController, BenchDefectController, ProblemReportController, CampaignController, VvReportingController, WorkflowInternalController)
- 2 migrations (V10, V11), 1 config (AdminServiceClientConfig)

### jira-issue-service (12 new files)
- 4 entities, 4 repositories, 1 service, 1 controller, 2 migrations

### jira-workflow-service (3 new/modified files)
- 2 migrations (V18 workflows, V19 scheme mappings), 1 modified WorkflowIntegrationClient

### jira-frontend (17 new files)
- 5 API clients, 10 pages, 1 CSS file, App.tsx routes updated
