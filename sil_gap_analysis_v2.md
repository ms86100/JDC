# JDC Script Engine vs SIL — Deep Gap Analysis & Implementation Plan

**Version:** 2.0 — Honest Assessment
**Date:** 2026-07-25
**Classification:** Internal Engineering — Investment Decision Document
**Audience:** Product Owner, Principal Engineers, Solution Architects

---

## Executive Verdict

**Would a customer invest in this SIL alternative today? NO.**

The current JDC scripting engine is a **JavaScript execution sandbox with API wrappers**. It is NOT an enterprise automation language. Here is the honest comparison:

| Dimension | Real SIL | JDC Script Engine |
|---|---|---|
| Language | Purpose-built DSL with custom grammar, parser, AST, type system | Reuses GraalVM JavaScript — no custom language |
| Built-in Functions | 700+ domain-specific routines | ~108 exported methods (~56% of SIL's applicable functions) |
| Integration Depth | Hooks into every Jira extension point (OSGi, Spring Scanner, plugin modules) | API wrapper calls over REST — no deep platform hooks |
| Type System | Custom types (Issue, User, Project, JComment, etc.) with typed properties | Raw JavaScript objects — no type safety |
| Editor/IDE | SIL Manager with folder tree, syntax highlighting, Check, Search, Settings, Show usage | Flat list + Monaco editor in a modal — no SIL-specific features |
| Transactional Safety | Volatile clone system — changes applied on success, discarded on error | Direct API calls — no rollback on script failure |
| Event System | Deep integration with Jira event bus | **NOT WIRED** — listener service exists but nothing calls it |
| Field Behaviors (Live Fields) | Browser-side real-time field manipulation (10 functions) | Server-side only, applied to 2 of 15+ form fields |
| Debugging | Tracing, line-by-line execution, variable inspection | None |
| Script Organization | Folder tree with categories, dependency tracking | Flat list, no folders, no dependency graph |
| Cluster Safety | Distributed locks via Hazelcast, node synchronization | TOCTOU race condition in scheduled execution |
| Security Model | runAs(), permission grants, execution quotas, admin-only access | No authentication on any endpoint, no role checks |

**Current realistic SIL parity: ~35-40%** (not the 81.6% previously claimed)

The 81.6% counted "feature exists" but not "feature works end-to-end with enterprise quality."

---

## Part 1: End-to-End Scenario Audit Results

Ten real enterprise scenarios were traced through actual code paths.

| # | Enterprise Scenario | Verdict | Details |
|---|---|---|---|
| 1 | Workflow Condition Script | **WORKS** | Full trace: frontend → gateway → condition evaluator → plugin registry → script engine → result |
| 2 | Workflow Validator Script | **WORKS** | Script context includes issueTypeId, screenInput (form data), all transition context |
| 3 | Post-Function Script | **WORKS** | clone, addComment, sendEmail, setFieldValue all delegate to real service calls |
| 4 | Event Listener Script | **NOT WIRED** | `ScriptListenerService.fireEvent()` exists but is NEVER CALLED from anywhere in the codebase |
| 5 | Scheduled Script | **WORKS** (off by default) | Requires `jira.scripting.scheduled-enabled=true`. TOCTOU race in multi-instance |
| 6 | Field Behavior Script | **PARTIAL** | Works for issueType changes. Does NOT re-evaluate when other form fields change (missing `issueData` in useEffect deps). Only 2 of 15+ fields respect behaviors |
| 7 | Calculated Custom Field | **WORKS** | End-to-end: script evaluates → result displayed with lightning bolt indicator |
| 8 | Script Include/Composition | **WORKS** (textual only) | Regex-based prepending works. Runtime `JdcIncludeApi` is dead code — loads code into StringBuilder nobody reads |
| 9 | REST/Webhook Trigger | **WORKS** | External systems can POST to `/execute-by-key/{scriptKey}` with arbitrary context |
| 10 | Console/REPL Testing | **WORKS** | Monaco editor → execute → console output captured and displayed |

### Critical Finding: Event Listeners Are Completely Dead

The `ScriptListenerService` has full implementation — project filtering, issue type filtering, async execution — but **nothing in the entire codebase calls `scriptListenerService.fireEvent()`**. Not the `WorkflowEventOutboxProcessor`, not the `AutomationEventListener`, not issue-service callbacks. This means:

- "Auto-assign when issue created" — IMPOSSIBLE
- "Send Slack notification when issue transitions to Done" — IMPOSSIBLE
- "Sync fields when comment added" — IMPOSSIBLE
- "Audit log when attachment deleted" — IMPOSSIBLE

This is the #1 gap. Event-driven automation is the heart of SIL.

---

## Part 2: Critical Bugs & Dead Code

### CRITICAL — Must Fix Before Any Release

| # | Issue | Impact | Location |
|---|---|---|---|
| C1 | **Event listeners never fire** | Entire event-driven automation is non-functional | `ScriptListenerService.fireEvent()` never called |
| C2 | **No authentication on ScriptController** | Any user can create/execute/delete scripts. Any user can run arbitrary JavaScript on the server | `ScriptController.java` — no `@PreAuthorize` |
| C3 | **SQL API is completely dead** | `ScriptDataSourceConfig` returns empty map. `sql.query()` and `sql.update()` always return empty/fail | `ScriptDataSourceConfig.java` returns `Collections.emptyMap()` |
| C4 | **`memoryLimitMb` property never applied** | No memory limit on script execution. A script can consume unlimited heap | `GraalScriptEngine.java` — property declared but never wired to GraalVM context |
| C5 | **JdcIncludeApi runtime include is dead code** | `include.include("key")` returns true but loaded code never executes | `JdcIncludeApi.java` — StringBuilder never evaluated |

### HIGH — Breaks Enterprise Use Cases

| # | Issue | Impact | Location |
|---|---|---|---|
| H1 | **Scheduled script TOCTOU race** | Two nodes execute same scheduled script simultaneously | `ScheduledScriptExecutor.java` — no row-level DB lock |
| H2 | **Silent error swallowing in JdcApi** | Every method catches all exceptions, returns empty. Script authors cannot distinguish "no data" from "service error" | `JdcApi.java` — all 70+ methods |
| H3 | **useFieldBehaviors missing issueData dependency** | Field behaviors don't re-evaluate when form data changes (except issueType) | `useFieldBehaviors.ts` line 61 |
| H4 | **Only 2 form fields respect field behaviors** | `description` and `environment` only. Priority, assignee, sprint, labels, etc. all ignore behaviors | `CreateIssueModal.tsx` |
| H5 | **IssueDetailPage imports useFieldBehaviors but never applies it** | Behaviors are evaluated on VIEW screen but directives are not used in any rendered JSX | `IssueDetailPage.tsx` — destructured but unused |
| H6 | **No email rate limiting** | A script loop can send unlimited emails | `JdcEmailApi.java` |
| H7 | **No script-level authorization** | `deleteIssue`, `moveIssue`, `transitionIssue` available regardless of calling user's permissions | `JdcApi.java` |
| H8 | **URL encoding missing in API calls** | `JdcLdapApi.search()`, `JdcConfluenceApi.search()` pass query params without encoding | Multiple files |

---

## Part 3: Function Library — SIL vs JDC

### Summary (against 272 applicable SIL functions, excluding 15 N/A third-party plugin functions)

| Status | Count | % |
|---|---|---|
| **IMPLEMENTED** (full equivalent) | 153 | 56.3% |
| **PARTIAL** (some coverage) | 19 | 7.0% |
| **MISSING** (no equivalent) | 100 | 36.8% |

### Category Breakdown

| # | Category | SIL Functions | JDC Implemented | JDC Partial | JDC Missing | Coverage |
|---|---|---|---|---|---|---|
| 1 | Issue Functions | 25 | 18 | 3 | 4 | 78% |
| 2 | Field Functions | 15 | 4 | 1 | 10 | 30% |
| 3 | User/Group Functions | 20 | 7 | 3 | 10 | 43% |
| 4 | Project Functions | 18 | 10 | 0 | 8 | 56% |
| 5 | Workflow Functions | 12 | 7 | 1 | 4 | 63% |
| 6 | Comment Functions | 6 | 2 | 1 | 3 | 42% |
| 7 | Attachment Functions | 8 | 1 | 1 | 6 | 19% |
| 8 | Worklog Functions | 6 | 2 | 1 | 3 | 42% |
| 9 | String Functions | 20 | 20 | 0 | 0 | 100% |
| 10 | Date/Time Functions | 12 | 11 | 1 | 0 | 96% |
| 11 | Math Functions | 12 | 12 | 0 | 0 | 100% |
| 12 | Array/Collection Functions | 15 | 15 | 0 | 0 | 100% |
| 13 | HTTP/REST Functions | 8 | 8 | 0 | 0 | 100% |
| 14 | Database/SQL Functions | 5 | 2 | 1 | 2 | 50% |
| 15 | Email Functions | 6 | 2 | 0 | 4 | 33% |
| 16 | File Functions | 8 | 0 | 0 | 8 | 0% |
| 17 | JSON Functions | 5 | 4 | 1 | 0 | 90% |
| 18 | XML Functions | 6 | 4 | 0 | 2 | 67% |
| 19 | LDAP Functions | 6 | 4 | 0 | 2 | 67% |
| 20 | Logging Functions | 5 | 5 | 0 | 0 | 100% |
| 21 | Regex Functions | 5 | 5 | 0 | 0 | 100% |
| 22 | Utility Functions | 6 | 3 | 2 | 1 | 67% |
| 23 | Confluence Functions | 8 | 4 | 0 | 4 | 50% |
| 24 | Sprint/Agile Functions | 10 | 0 | 2 | 8 | 10% |
| 25 | Webhook Functions | 4 | 0 | 1 | 3 | 13% |
| 26 | Persistent Variables | 4 | 3 | 0 | 1 | 75% |
| 27 | Security Functions | 5 | 2 | 0 | 3 | 40% |
| 28 | Live Fields / Behaviors | 10 | 0 | 0 | 10 | 0% |
| **TOTAL** | **272** | **153** | **19** | **100** | **63%** |

### Where JavaScript Saves Us (Free Coverage)

Because we use GraalVM JavaScript instead of a custom DSL, we get 69 functions for free:

- String (20), Math (12), Array/Collection (15), Date/Time (11), Regex (5), JSON (4), Logging via console (5)

Without JavaScript's built-in capabilities, our custom function count drops to **84 out of 203** = **41%**.

### Biggest Function Gaps (High Impact)

| Gap Area | Missing Count | Impact |
|---|---|---|
| **Live Fields / Field Behaviors** | 10 functions | Cannot dynamically control form UI from scripts |
| **Field Metadata** | 10 functions | Cannot query field config, available values, required status |
| **User/Group Management** | 10 functions | Cannot create/delete users, manage group membership |
| **Sprint/Agile** | 8 functions | Cannot manipulate sprints, boards, backlogs |
| **File I/O** | 8 functions | Deliberate security choice — acceptable gap |
| **Project Management** | 8 functions | Cannot archive/delete versions, delete components, manage roles |
| **Attachment CRUD** | 6 functions | Can only count attachments, not upload/download/delete |

---

## Part 4: Architecture Gaps — What SIL Has That We Fundamentally Lack

These are not missing functions — they are missing architectural capabilities.

### 4.1 No Custom Language (Biggest Philosophical Gap)

**SIL approach:** Purpose-built DSL with custom grammar, parser, AST, semantic analyzer, and interpreter. The language vocabulary maps directly to the business domain. `assignee = "john"` directly sets the field.

**JDC approach:** Reuse GraalVM JavaScript. Scripts write `jdc.issue.setFieldValue("assigneeId", userId)`.

**Impact:**
- SIL scripts are 3-5x shorter for common operations
- SIL provides compile-time validation of field names and types
- SIL's "Issue Context" makes field names first-class variables
- JavaScript requires learning a separate API (`jdc.issue.*`) rather than using natural syntax

**Assessment:** This is a deliberate architectural choice. JavaScript gives us a mature language with full IDE support (Monaco, VSCode, IntelliSense). But it means we can never match SIL's ergonomics for simple operations. **This is acceptable** if we provide excellent API documentation and autocomplete.

### 4.2 No Transactional Clone System

**SIL approach:** When a script modifies an issue, SIL creates a volatile clone in memory. All changes are applied to the clone. On success, the clone is persisted. On error, the clone is discarded. The original issue is untouched.

**JDC approach:** Every `jdc.issue.setFieldValue()` and `jdc.issue.addComment()` makes an immediate REST call. If the script fails halfway through, some changes are already committed.

**Impact:**
- No rollback on partial failure
- A script that calls `addComment()` then `transitionIssue()` will leave a dangling comment if the transition fails
- Enterprise customers expect atomicity

**Assessment:** This requires significant architectural work — either batch all mutations and apply at the end, or implement a compensating transaction pattern.

### 4.3 No Layered Execution Context

**SIL approach:** Three-layer context resolution:
1. Current block variables (highest priority)
2. Script global variables
3. Issue Context — field names are automatically mapped as variables (`summary`, `assignee`, `priority`)

A script can write `summary = "New Title"` without any API calls.

**JDC approach:** Flat bindings map. Field values require explicit API calls: `jdc.issue.getFieldValue("summary")`.

**Impact:** Every field operation requires 2-3x more code than SIL.

### 4.4 No SIL Manager UI

From the SIL Manager screenshot, SIL provides:

```
+----------------------------------------------+-------------------------------------------+
| SIL Manager                                  |                          Quick Links v    |
+----------------------------------------------+-------------------------------------------+
| View v | Selection v | Refresh |             | Check | Save | Search | Replace |       |
|                                               | Fullscreen | Settings | Show usage      |
+----------------------------+------------------+-------------------------------------------+
| Select a folder to search  |  1  |                                                     |
|                            |  2  | function writeLog(string message) {                 |
| v silprograms              |  3  |                                                     |
|   > Calls                  |  4  |     printInFile("./listener_log.txt", message);     |
|   > customfield_10009      |  5  | }                                                   |
|   > customfield_11700      |  6  |                                                     |
|   > examples               |  7  | struct projVers {                                   |
|   > HubSpot                |  8  |     string pkey;                                    |
|   > Includes               |  9  |     string [] versions;                             |
|   > Javascript             | 10  | }                                                   |
|   > JQL                    | 11  |                                                     |
|   > Listeners              | 12  | //string key = argv[0];                             |
|   > LiveFields             | 13  |                                                     |
|   > Migration              | 14  | if(project == "PM") { //only run listener for PM    |
|   > PCF                    | 15  |                                                     |
|   > RunnerGadget           | 16  |     string [] projectBKeys = {"AR","DW","TSIO"};    |
|     > Param_Scripts        | 17  |     string [] linkedIssues= linkedIssues(key);      |
|     copyFields.sil         | 18  |     string [] currentIssueVersions = %key%.fix...   |
|     createCustomFields.sil | 19  |     projVers [] projectVersions;                    |
|     emailList.sil          | 20  |                                                     |
|     jql_Params.sil         | 21  |     //create list of destination projects and ver.. |
|     removeCloneFrom...     | 22  |     for(string pkey in projectBKeys) {              |
+----------------------------+-----+-----------------------------------------------------+
```

**JDC provides:**
- Flat table list (no folders, no tree)
- Monaco editor in a 350px modal (not full-page)
- No Check/Validate button in UI (endpoint exists, never called)
- No Search across scripts
- No Settings panel
- No "Show usage" / dependency tracking
- No folder organization
- No fullscreen editor

### 4.5 No Debugging Support

**SIL provides:** Tracing, variable state logging, execution step visibility.

**JDC provides:** Nothing. Script errors return a GraalVM stack trace. No breakpoints, no variable inspection, no step-through.

### 4.6 No Webhook Response Control

**SIL provides:** Scripts that expose REST endpoints can control the HTTP response via `appendToWebhookResponse()` and `setWebhookResponseCode()`.

**JDC provides:** Single `execute-by-key` endpoint returns a fixed JSON structure. Scripts cannot control response format, status code, or headers.

---

## Part 5: Infrastructure Gaps

| # | Finding | Severity |
|---|---|---|
| I1 | **No auth on script endpoints** — SecurityConfig permits all `/api/**`. Port 8085 exposed in all docker-compose files | CRITICAL |
| I2 | **No scripting env vars in any docker-compose** — Cannot override timeout, memory limit, enable/disable per environment | MEDIUM |
| I3 | **Notification service hardcodes workflow-service URL** — No `WORKFLOW_SERVICE_URL` env var | LOW |
| I4 | **Scheduled execution off by default** — Requires manual config change. No documented way to enable per environment | LOW |

---

## Part 6: Honest Score Card

### By Capability Area (Weighted by Enterprise Value)

| Capability Area | Weight | SIL Score | JDC Score | Gap |
|---|---|---|---|---|
| **Workflow Automation** (conditions, validators, post-functions) | 25% | 100 | 85 | -15 |
| **Event-Driven Automation** (listeners, event bus) | 20% | 100 | 0 | -100 |
| **Script Management** (editor, organization, versioning) | 15% | 100 | 30 | -70 |
| **Function Library** (API coverage) | 15% | 100 | 56 | -44 |
| **Security & Enterprise** (auth, audit, cluster safety) | 10% | 100 | 15 | -85 |
| **UI Integration** (field behaviors, calculated fields, web panels) | 10% | 100 | 25 | -75 |
| **Tooling** (debugging, profiling, autocomplete) | 5% | 100 | 5 | -95 |

### Weighted Overall Score

```
Workflow:     25% × 85  = 21.25
Events:       20% × 0   =  0.00
Management:   15% × 30  =  4.50
Functions:    15% × 56  =  8.40
Security:     10% × 15  =  1.50
UI:           10% × 25  =  2.50
Tooling:       5% × 5   =  0.25
                         ------
TOTAL:                    38.40 / 100
```

**Weighted SIL parity: 38.4%**

---

## Part 7: Implementation Plan — Road to 90%+ Parity

### Phase 0: Critical Bug Fixes (MUST DO BEFORE ANYTHING ELSE)

**Goal: Fix broken/dead code that makes current features non-functional**

| # | Task | Subtasks | Priority | Effort |
|---|---|---|---|---|
| 0.1 | **Wire event listener system** | 1. Add `scriptListenerService.fireEvent()` calls to `WorkflowEventOutboxProcessor.dispatch()` for ISSUE_CREATED, ISSUE_UPDATED, ISSUE_TRANSITIONED, ISSUE_DELETED events | CRITICAL | 2d |
| | | 2. Add `scriptListenerService.fireEvent()` to comment-service callbacks for COMMENT_ADDED, COMMENT_UPDATED, COMMENT_DELETED | | |
| | | 3. Add `scriptListenerService.fireEvent()` to worklog and attachment service callbacks | | |
| | | 4. Add listener event types enum covering all supported events | | |
| | | 5. Test end-to-end: create listener → create issue → verify script fires | | |
| 0.2 | **Add authentication to ScriptController** | 1. Add `@PreAuthorize("hasRole('ADMIN')")` to all CRUD and execute endpoints | CRITICAL | 1d |
| | | 2. Add `@PreAuthorize("hasAnyRole('ADMIN','USER')")` to read-only endpoints | | |
| | | 3. Add request user extraction for audit logging | | |
| | | 4. Add rate limiting to console execute and execute-by-key endpoints | | |
| | | 5. Test auth enforcement with non-admin user | | |
| 0.3 | **Fix SQL API dead code** | 1. Create `ScriptDataSourceConfig` that reads named datasources from YAML config | CRITICAL | 1d |
| | | 2. Add `jira.scripting.datasources` config section with name/url/username/password per datasource | | |
| | | 3. Add datasource health check to `ScriptEngineHealthIndicator` | | |
| | | 4. Document SQL API usage in script reference | | |
| | | 5. Test with PostgreSQL datasource | | |
| 0.4 | **Wire memory limit** | 1. Apply `memoryLimitMb` to GraalVM Context via `ResourceLimits` heap limit | CRITICAL | 0.5d |
| | | 2. Test that exceeding memory limit throws a catchable error | | |
| 0.5 | **Clean up dead include mechanism** | 1. Remove `JdcIncludeApi` class entirely | HIGH | 0.5d |
| | | 2. Replace `include` binding with a simple no-op function that logs a deprecation warning | | |
| | | 3. Document that `include("key")` works via textual pre-resolution, not runtime include | | |
| | | 4. Add include dependency tracking (which scripts include which libraries) | | |
| | | 5. Show include dependencies in script detail view | | |

**Phase 0 Total: 5 days | Moves score from 38% to ~50%**

---

### Phase 1: Event System & Field Behaviors (Enterprise Core)

**Goal: Make event-driven automation and dynamic UI fully functional**

| # | Task | Subtasks | Priority | Effort |
|---|---|---|---|---|
| 1.1 | **Complete event listener wiring** | 1. Define canonical event type enum: ISSUE_CREATED, ISSUE_UPDATED, ISSUE_DELETED, ISSUE_TRANSITIONED, COMMENT_ADDED, COMMENT_UPDATED, COMMENT_DELETED, WORKLOG_ADDED, ATTACHMENT_ADDED, ATTACHMENT_DELETED, SPRINT_STARTED, SPRINT_COMPLETED, VERSION_RELEASED | HIGH | 3d |
| | | 2. Add event publishing from issue-service webhook callbacks | | |
| | | 3. Add event publishing from notification-service automation listener | | |
| | | 4. Add event context enrichment (full issue data, user data, change details) | | |
| | | 5. Add listener management UI tab in WorkflowScriptsPage (create/edit/delete/toggle listeners with event type picker, project filter, issue type filter) | | |
| 1.2 | **Fix field behaviors end-to-end** | 1. Add `JSON.stringify(issueData)` to `useFieldBehaviors` useEffect dependency array | HIGH | 3d |
| | | 2. Add debouncing (300ms) to prevent excessive re-evaluation on rapid typing | | |
| | | 3. Apply field behaviors to ALL form fields in CreateIssueModal (priority, assignee, sprint, epic, story points, labels, versions, components, due date, estimates) | | |
| | | 4. Apply field behaviors in IssueDetailPage edit mode (wire the already-imported but unused hook) | | |
| | | 5. Implement `getFieldDefault()` and `getFieldOptions()` application in form initialization | | |
| 1.3 | **Field behavior admin UI** | 1. Create field behavior management section in WorkflowScriptsPage | HIGH | 2d |
| | | 2. CRUD interface for field behavior bindings (script + screen context + project + issue type) | | |
| | | 3. Preview mode: show which fields are affected by each behavior script | | |
| | | 4. Test mode: evaluate behavior with mock context and show directives | | |
| | | 5. Document field behavior script API (return format, available directives) | | |
| 1.4 | **Scheduled script improvements** | 1. Add database-level pessimistic lock (`SELECT ... FOR UPDATE SKIP LOCKED`) for scheduled script polling | MEDIUM | 2d |
| | | 2. Add execution timeout tracking and dead-script detection | | |
| | | 3. Add schedule management UI improvements (cron expression builder, next 5 run times preview) | | |
| | | 4. Add missed-execution detection and catch-up | | |
| | | 5. Enable scheduled execution by default in docker-compose with safe defaults | | |

**Phase 1 Total: 10 days | Moves score from ~50% to ~65%**

---

### Phase 2: Function Library Expansion (API Coverage)

**Goal: Close the biggest function gaps — Sprint/Agile, Attachments, Comments, User/Group**

| # | Task | Subtasks | Priority | Effort |
|---|---|---|---|---|
| 2.1 | **Sprint/Agile API** | 1. Add `JdcSprintApi` class with: `getSprint(sprintId)`, `getActiveSprint(boardId)`, `getAllSprints(boardId)`, `moveToSprint(issueId, sprintId)`, `moveToBacklog(issueId)`, `getBoard(boardId)`, `getSprintIssues(sprintId)`, `createSprint(boardId, name, startDate, endDate)`, `closeSprint(sprintId)`, `getEpic(epicId)` | HIGH | 3d |
| | | 2. Add sprint-service integration to `WorkflowIntegrationClient` | | |
| | | 3. Wire into `JdcScriptBindings` as `sprint` binding | | |
| | | 4. Add sprint-url to all gateway profiles and docker-compose files | | |
| | | 5. Test: script that moves all "Done" issues to next sprint | | |
| 2.2 | **Attachment API** | 1. Add `JdcAttachmentApi` class with: `getAttachments(issueId)`, `addAttachment(issueId, filename, content)`, `deleteAttachment(attachmentId)`, `getAttachmentContent(attachmentId)`, `getAttachmentUrl(attachmentId)`, `copyAttachments(sourceIssueId, targetIssueId)` | HIGH | 2d |
| | | 2. Add attachment-service integration to `WorkflowIntegrationClient` | | |
| | | 3. Wire into `JdcApi.IssueApi` or as separate `attachment` binding | | |
| | | 4. Handle binary content safely within GraalVM sandbox (base64 encoding) | | |
| | | 5. Test: script that copies attachments between issues on clone | | |
| 2.3 | **Comment CRUD completion** | 1. Add `deleteComment(commentId)` to `JdcApi.IssueApi` | MEDIUM | 1d |
| | | 2. Add `updateComment(commentId, newText)` to `JdcApi.IssueApi` | | |
| | | 3. Add `getLastComment()` convenience method | | |
| | | 4. Add comment visibility/restriction support | | |
| | | 5. Add comment-service DELETE and PUT integration to `WorkflowIntegrationClient` | | |
| 2.4 | **User/Group management** | 1. Add `addUserToGroup(userId, groupName)`, `removeUserFromGroup(userId, groupName)` to `JdcApi.UserApi` | MEDIUM | 2d |
| | | 2. Add `isAdmin(userId)` check | | |
| | | 3. Add `getUserByEmail(email)` | | |
| | | 4. Add user-service REST endpoints for group management if not existing | | |
| | | 5. Add `getAllUsers(query, limit)` with pagination | | |
| 2.5 | **Field metadata API** | 1. Add `hasField(issueId, fieldName)` to `JdcApi.IssueApi` | MEDIUM | 2d |
| | | 2. Add `getAvailableFieldValues(fieldName, projectId, issueTypeId)` for select/option fields | | |
| | | 3. Add `isFieldRequired(fieldName, screenId)` | | |
| | | 4. Add `getFieldType(fieldName)` returning field data type | | |
| | | 5. Add migration-service integration for field configuration queries | | |
| 2.6 | **Project management completion** | 1. Add `archiveVersion(versionId)`, `deleteVersion(versionId)` | LOW | 1d |
| | | 2. Add `deleteComponent(componentId)` | | |
| | | 3. Add `getProjectRoles(projectId)` | | |
| | | 4. Add `getAllProjects(query)` | | |
| | | 5. Add version/component service DELETE integrations | | |
| 2.7 | **Workflow function completion** | 1. Add `getAvailableActions(issueId)` returning available transitions | LOW | 1d |
| | | 2. Add `getWorkflowName(issueId)` | | |
| | | 3. Wire to workflow-service internal API | | |
| 2.8 | **Issue function completion** | 1. Add `unlinkIssue(sourcceId, targetId, linkType)` | LOW | 1d |
| | | 2. Add `setRank(issueId, rank)` or `rankBefore(issueId, beforeIssueId)` | | |
| | | 3. Add `getSecurityLevel(issueId)`, `setSecurityLevel(issueId, levelId)` | | |
| | | 4. Wire to issue-service REST endpoints | | |
| 2.9 | **Worklog completion** | 1. Add `deleteWorklog(worklogId)` | LOW | 0.5d |
| | | 2. Add `updateWorklog(worklogId, timeSpent, comment)` | | |
| | | 3. Add `getRemainingEstimate(issueId)`, `getOriginalEstimate(issueId)` | | |
| 2.10 | **Webhook response control** | 1. Add `JdcWebhookApi` with `setResponseCode(code)`, `setResponseBody(body)`, `setResponseHeader(name, value)` | LOW | 1d |
| | | 2. Thread response control through ScriptResult back to controller | | |
| | | 3. Apply response overrides in `ScriptController.executeByKey()` | | |

**Phase 2 Total: 14.5 days | Moves score from ~65% to ~80%**

---

### Phase 3: Script Manager UI (Enterprise-Grade Editor)

**Goal: Transform the flat list + modal into a proper SIL Manager equivalent**

| # | Task | Subtasks | Priority | Effort |
|---|---|---|---|---|
| 3.1 | **Full-page script editor** | 1. Replace modal editor with full-page editor route (`/workflows/admin/scripts/:scriptId`) | HIGH | 3d |
| | | 2. Monaco editor fills available height (not fixed 350px) | | |
| | | 3. Add toolbar: Save, Validate (Check), Run in Console, Export, Fullscreen toggle | | |
| | | 4. Add keyboard shortcuts: Ctrl+S (save), Ctrl+Enter (run), Ctrl+Shift+V (validate) | | |
| | | 5. Add unsaved changes warning on navigation | | |
| 3.2 | **Folder/category organization** | 1. Add `category` or `folder` field to ScriptDefinition entity | HIGH | 3d |
| | | 2. Add V20 migration for category column | | |
| | | 3. Create tree sidebar component with folders (Conditions, Validators, Post-Functions, Listeners, LiveFields, Libraries, Scheduled, Console Scripts) | | |
| | | 4. Allow custom folder creation | | |
| | | 5. Drag-and-drop scripts between folders | | |
| 3.3 | **Script search and filtering** | 1. Add search bar on script list page with name/key/body search | MEDIUM | 1d |
| | | 2. Add type filter dropdown (CONDITION, VALIDATOR, POST_FUNCTION, etc.) | | |
| | | 3. Add enabled/disabled filter | | |
| | | 4. Add sort by name/type/updated/created | | |
| | | 5. Add pagination for large script collections | | |
| 3.4 | **Version diff viewer** | 1. Add side-by-side diff component (Monaco diff editor) | MEDIUM | 2d |
| | | 2. Show old vs new script body for each version | | |
| | | 3. Highlight changes (additions in green, deletions in red) | | |
| | | 4. One-click revert from diff view | | |
| | | 5. Version comparison selector (compare any two versions) | | |
| 3.5 | **Dependency/usage tracking** | 1. Add "Show usage" button that finds all workflows referencing this script | MEDIUM | 2d |
| | | 2. Query workflow conditions, validators, post-functions for script key matches | | |
| | | 3. Show include dependencies (which scripts include this library) | | |
| | | 4. Show reverse dependencies (which libraries this script includes) | | |
| | | 5. Warn on delete if script is in use | | |
| 3.6 | **Import UI** | 1. Add "Import Script" button on script list page | LOW | 1d |
| | | 2. File picker for JSON upload | | |
| | | 3. Preview imported script before saving | | |
| | | 4. Conflict resolution (merge, overwrite, skip) | | |
| | | 5. Bulk import support (multiple scripts in one JSON file) | | |
| 3.7 | **JDC API autocomplete** | 1. Create Monaco completion provider for `jdc.*` API namespace | MEDIUM | 2d |
| | | 2. Register all method signatures with parameter hints | | |
| | | 3. Add JSDoc-style documentation for each method | | |
| | | 4. Context-aware completions (show `.issue.*` after typing `jdc.`) | | |
| | | 5. Add snippets for common patterns (create issue, search+iterate, send email) | | |

**Phase 3 Total: 14 days | Moves score from ~80% to ~88%**

---

### Phase 4: Security & Enterprise Hardening

**Goal: Make the script engine production-ready for enterprise deployment**

| # | Task | Subtasks | Priority | Effort |
|---|---|---|---|---|
| 4.1 | **Script permission model** | 1. Add `script_permissions` table with allowed operations per script (CAN_CREATE_ISSUE, CAN_DELETE_ISSUE, CAN_SEND_EMAIL, CAN_HTTP, CAN_SQL) | HIGH | 3d |
| | | 2. Add permission declaration UI (checkboxes on script editor) | | |
| | | 3. Enforce permissions at API binding level (check before executing) | | |
| | | 4. Add `runAs(userId, callback)` for impersonation with audit trail | | |
| | | 5. Add admin-only flag for dangerous operations (delete, move, transition) | | |
| 4.2 | **Execution audit trail** | 1. Enhance `script_execution_log` with: `executedBy`, `targetIssueId`, `apiCallCount`, `apiCallDetails` | HIGH | 2d |
| | | 2. Add structured telemetry: timing per API call, total mutations | | |
| | | 3. Add audit dashboard UI with filters (by script, by user, by date, by status) | | |
| | | 4. Add export audit logs to CSV | | |
| | | 5. Add alerting on script failure rate exceeding threshold | | |
| 4.3 | **Execution quotas & rate limiting** | 1. Add per-script execution rate limit (max N executions per minute) | MEDIUM | 2d |
| | | 2. Add per-user execution rate limit | | |
| | | 3. Add global system rate limit | | |
| | | 4. Add email sending rate limit (max N emails per script per hour) | | |
| | | 5. Add HTTP call rate limit per domain | | |
| 4.4 | **Error surfacing** | 1. Add `JdcError` class returned from API calls instead of silent swallowing | MEDIUM | 2d |
| | | 2. Make API methods return `{ success, data, error }` objects instead of bare data | | |
| | | 3. Add `jdc.getLastError()` for debugging | | |
| | | 4. Add line number mapping for include-prepended scripts | | |
| | | 5. Add structured error output in console | | |
| 4.5 | **Docker environment configuration** | 1. Add all `JIRA_SCRIPTING_*` env vars to docker-compose.yml | LOW | 0.5d |
| | | 2. Add `WORKFLOW_SERVICE_URL` to notification-service in docker-compose | | |
| | | 3. Document all configuration knobs in README | | |
| 4.6 | **Transactional execution model** | 1. Add mutation buffer — collect all `setFieldValue`, `addComment`, `addLabel` calls without executing immediately | HIGH | 5d |
| | | 2. Add commit phase — apply all buffered mutations at script completion | | |
| | | 3. Add rollback on script error — discard buffer | | |
| | | 4. Mark immediate operations (createIssue, deleteIssue) that bypass buffer | | |
| | | 5. Add `jdc.flush()` for explicit mid-script commit | | |

**Phase 4 Total: 14.5 days | Moves score from ~88% to ~93%**

---

### Phase 5: Advanced Features (Differentiation)

**Goal: Go beyond SIL parity — provide features SIL doesn't have**

| # | Task | Subtasks | Priority | Effort |
|---|---|---|---|---|
| 5.1 | **Script debugging** | 1. Add execution trace mode — log every API call with timing | MEDIUM | 5d |
| | | 2. Add variable snapshot at each step | | |
| | | 3. Add execution timeline visualization in UI | | |
| | | 4. Add "dry run" mode that shows what would be changed without applying | | |
| | | 5. Add "step through" execution with pause/resume via WebSocket | | |
| 5.2 | **Script template library** | 1. Create bundled template scripts for common patterns | LOW | 2d |
| | | 2. Templates: auto-assign on create, enforce subtask resolution, cascade field updates, SLA tracker, approval workflow, bulk transition, email digest | | |
| | | 3. "New from template" button in script creation | | |
| | | 4. Template categories matching SIL folders (Conditions, Validators, Post-Functions, Listeners, Scheduled) | | |
| | | 5. Community template sharing (import/export) | | |
| 5.3 | **Execution profiler** | 1. Add per-API-call timing instrumentation in JdcApi | LOW | 2d |
| | | 2. Add memory usage tracking per script execution | | |
| | | 3. Add profiler dashboard showing slowest scripts, most API calls, highest memory | | |
| | | 4. Add optimization suggestions (e.g., "this script makes 50 HTTP calls — consider batching") | | |
| | | 5. Add historical performance trending | | |
| 5.4 | **Email enhancements** | 1. Add CC/BCC support to `email.sendEmail()` | LOW | 1d |
| | | 2. Add HTML template support with variable interpolation | | |
| | | 3. Add attachment support for emails | | |
| | | 4. Add email queue for batch sending | | |
| | | 5. Add email delivery status tracking | | |
| 5.5 | **Script testing framework** | 1. Add `jdc.test.assert(condition, message)` built-in | MEDIUM | 3d |
| | | 2. Add `jdc.test.assertEquals(expected, actual)` | | |
| | | 3. Add `jdc.test.mock(apiName, returnValue)` for mocking API calls | | |
| | | 4. Add test runner UI — run all test scripts and show pass/fail report | | |
| | | 5. Add CI integration — API endpoint to run all test scripts and return results | | |

**Phase 5 Total: 13 days | Moves score from ~93% to ~97%+**

---

## Part 8: Timeline & Resource Summary

```
+----------------------------------------------------------+
|          Phase 0: Critical Bug Fixes                      |
|          ████████████ 5 days                              |
|          Score: 38% → 50%                                 |
+----------------------------------------------------------+
|          Phase 1: Events & Field Behaviors                |
|          ████████████████████ 10 days                     |
|          Score: 50% → 65%                                 |
+----------------------------------------------------------+
|          Phase 2: Function Library                        |
|          █████████████████████████████ 14.5 days          |
|          Score: 65% → 80%                                 |
+----------------------------------------------------------+
|          Phase 3: Script Manager UI                       |
|          ████████████████████████████ 14 days             |
|          Score: 80% → 88%                                 |
+----------------------------------------------------------+
|          Phase 4: Security & Enterprise                   |
|          █████████████████████████████ 14.5 days          |
|          Score: 88% → 93%                                 |
+----------------------------------------------------------+
|          Phase 5: Advanced/Differentiation                |
|          ██████████████████████████ 13 days               |
|          Score: 93% → 97%                                 |
+----------------------------------------------------------+

Total: 71 engineering days (~3.5 months with 1 engineer, ~6 weeks with 2)
```

### What Gets Us to "Investable" (MVP for Enterprise Customer)

Phases 0 + 1 + 2 = **29.5 days** = **80% parity** = Minimum viable product

At 80%, a customer can:
- Write workflow conditions, validators, and post-functions ✓
- React to events (issue created, updated, transitioned, commented) ✓
- Schedule batch scripts ✓
- Manipulate sprints, attachments, comments, users, groups ✓
- Control field visibility, required status, read-only on forms ✓
- Test scripts in console ✓
- Import/export scripts ✓
- Use persistent variables across scripts ✓
- Call external REST APIs, query SQL, send emails ✓

At 80%, a customer CANNOT:
- Debug scripts with breakpoints/step-through
- See script dependencies and usage
- Organize scripts in folders
- Get API autocomplete in the editor
- Rely on transactional safety (partial failure = partial commit)
- Have admin-only script restrictions

### What the Remaining 3% Is (Why We Stop at 97%)

The final 3% represents SIL capabilities that are either:
- **Third-party integrations** (Tempo, Insight/Assets) — requires separate products
- **File system access** — deliberate security exclusion
- **Exact SIL syntax parity** — we use JavaScript, not SIL's DSL, by design
- **Dashboard gadgets/web panels** — requires frontend plugin architecture

These are architectural choices, not gaps.

---

## Part 9: Risk Matrix

| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| GraalVM version incompatibility on upgrade | Medium | High | Pin GraalVM version, integration test suite |
| Script execution consuming too much memory | High | High | Phase 0 fixes memory limit wiring |
| Malicious script execution (no auth) | Critical | Critical | Phase 0 adds authentication |
| Event listener performance under load | Medium | Medium | Phase 1 adds async + rate limiting |
| Breaking existing scripts on API changes | Low | High | Version the API, deprecation warnings |
| Monaco editor performance with large scripts | Low | Medium | Virtual scrolling, lazy loading |
| Database connection pool exhaustion from SQL API | Medium | High | Separate datasource pool, connection limits |

---

## Conclusion

The JDC script engine has a **solid foundation** — GraalVM sandbox, 108 API methods, working workflow integration, console, scheduling, and persistence. But it is at **38% weighted enterprise parity** with real SIL, not the 81.6% previously reported.

The path to "investable" (80%) requires 29.5 engineering days focused on:
1. Fixing critical dead code (events, auth, SQL, memory)
2. Completing the event listener system
3. Expanding the function library (sprint, attachments, field metadata)
4. Making field behaviors work across all form fields

The path to "competitive" (93%+) adds 28.5 more days for:
4. Enterprise-grade script manager UI
5. Security hardening and transactional safety
6. Advanced tooling (debugging, profiling, testing framework)

**Total investment to enterprise-grade: 71 engineering days.**
