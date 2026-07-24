# JDC Script Engine — SIL/ScriptRunner Alternative

## Table of Contents

1. [Overview](#1-overview)
2. [IP & Legal Analysis](#2-ip--legal-analysis)
3. [Architecture](#3-architecture)
4. [JDC Script Language Reference](#4-jdc-script-language-reference)
5. [Script Types & Contracts](#5-script-types--contracts)
6. [API Reference — `jdc.*` Namespace](#6-api-reference--jdc-namespace)
7. [Sandbox Security Model](#7-sandbox-security-model)
8. [Administration Guide](#8-administration-guide)
9. [REST API Reference](#9-rest-api-reference)
10. [Frontend UI Guide](#10-frontend-ui-guide)
11. [Code Examples Cookbook](#11-code-examples-cookbook)
12. [Implementation Roadmap](#12-implementation-roadmap)
13. [Codebase Reference](#13-codebase-reference)

---

## 1. Overview

### What is JDC Script?

JDC Script is a JavaScript-based scripting engine embedded in the JDC (Jira Data Center clone) platform. It allows administrators to write custom logic that executes during workflow transitions — as **conditions** (gate transitions), **validators** (enforce rules), and **post-functions** (automate actions).

### How does it compare to SIL and ScriptRunner?

| Feature | SIL (Power Scripts) | ScriptRunner | JDC Script |
|---|---|---|---|
| **Language** | SIL (proprietary DSL) | Groovy (JVM) | JavaScript (ES2022) |
| **Vendor** | cPrime / Appfire | Adaptavist | Built-in (JDC) |
| **Sandbox** | Limited | None (full JVM access) | Full sandbox (GraalVM) |
| **Workflow Hooks** | Conditions, validators, post-functions, listeners | Conditions, validators, post-functions, listeners, behaviours | Conditions, validators, post-functions |
| **Editor** | Custom IDE plugin | Groovy console | Monaco (VS Code engine) |
| **API Style** | `issue.get("field")` | `issue.getAsString("field")` | `jdc.getIssueField("field")` |
| **IP Status** | Proprietary (Appfire) | Proprietary (Adaptavist) | Open (JDC-owned) |

### Key Advantages of JDC Script

1. **JavaScript** — universally known language, no proprietary DSL to learn
2. **Sandboxed** — scripts cannot access the filesystem, network, spawn processes, or load Java classes
3. **Resource-limited** — configurable CPU timeout, memory cap, and statement limits prevent runaway scripts
4. **Versioned** — every script edit creates an immutable version, enabling rollback and audit
5. **Audited** — every execution is logged with context, timing, and result
6. **Console** — test scripts safely before deploying to production workflows

---

## 2. IP & Legal Analysis

### What we DO NOT implement

| Proprietary Technology | Owner | Our Approach |
|---|---|---|
| SIL language syntax and grammar | cPrime / Appfire | We use **standard JavaScript** (ES2022) |
| SIL built-in functions (`currentUser()`, `getIssue()`, etc.) | cPrime / Appfire | We designed our own API namespace: `jdc.*` |
| ScriptRunner's Groovy DSL and API bindings | Adaptavist | We use GraalJS, not Groovy, with our own API |
| ScriptRunner's script fragments, behaviours, listeners | Adaptavist | We implement only workflow hooks (conditions, validators, post-functions) |

### What we DO implement

- A standard JavaScript runtime (GraalJS, by Oracle, Apache 2.0 licensed)
- Our own API surface (`jdc.getIssue()`, `jdc.setIssueField()`, etc.) — entirely different from SIL or ScriptRunner
- Standard workflow engine extension points (conditions, validators, post-functions) — these are generic software patterns, not proprietary to any vendor
- A Monaco-based code editor (Microsoft, MIT licensed)

### Conclusion

**No IP violation.** We use:
- An open-source runtime (GraalJS, Apache 2.0)
- A standard programming language (JavaScript)
- Our own API design (the `jdc.*` namespace)
- Generic software patterns (plugin registry, sandbox execution)

---

## 3. Architecture

### System Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                         JDC Platform                              │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │                   jira-workflow-service                      │  │
│  │                                                              │  │
│  │  ┌────────────────────────────────────────────────────────┐  │  │
│  │  │              Workflow Execution Engine                  │  │  │
│  │  │                                                        │  │  │
│  │  │   ConditionEvaluator ──┐                               │  │  │
│  │  │   ValidatorExecutor  ──┤── WorkflowPluginRegistry      │  │  │
│  │  │   PostFunctionExecutor ┘        │                      │  │  │
│  │  │                                 │                      │  │  │
│  │  │                     ┌───────────┴────────────┐         │  │  │
│  │  │                     │  ScriptPluginRegistrar  │         │  │  │
│  │  │                     └───────────┬────────────┘         │  │  │
│  │  └─────────────────────────────────│──────────────────────┘  │  │
│  │                                    │                          │  │
│  │  ┌─────────────────────────────────┴──────────────────────┐  │  │
│  │  │              Script Execution Engine                    │  │  │
│  │  │                                                        │  │  │
│  │  │  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐  │  │  │
│  │  │  │ GraalScript  │  │  JdcScript   │  │  Script     │  │  │  │
│  │  │  │ Engine       │  │  Bindings    │  │  Execution  │  │  │  │
│  │  │  │ (sandbox)    │  │  (jdc.* API) │  │  Service    │  │  │  │
│  │  │  └──────────────┘  └──────────────┘  └─────────────┘  │  │  │
│  │  │        │                   │                            │  │  │
│  │  │  ┌─────┴───────────────────┴──────────────────────┐    │  │  │
│  │  │  │         WorkflowIntegrationClient               │    │  │  │
│  │  │  │  (REST calls to issue, user, project services)  │    │  │  │
│  │  │  └─────────────────────────────────────────────────┘    │  │  │
│  │  └────────────────────────────────────────────────────────┘  │  │
│  │                                                              │  │
│  │  ┌────────────────────────────────────────────────────────┐  │  │
│  │  │              Script Management                         │  │  │
│  │  │  ScriptController → ScriptDefinitionService            │  │  │
│  │  │  → ScriptDefinitionRepository (PostgreSQL)             │  │  │
│  │  └────────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                     jira-frontend                            │  │
│  │  WorkflowScriptsPage → ScriptConsole → Monaco Editor         │  │
│  └──────────────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────────┘
```

### Execution Flow

```
Workflow Transition Request
       │
       ▼
WorkflowExecutionEngine.execute()
       │
       ├─1─► ConditionEvaluator.evaluateAll()
       │         │
       │         ├── Built-in conditions (field value, user group, etc.)
       │         └── SCRIPT condition
       │              │
       │              ▼
       │         WorkflowPluginRegistry.evaluateCondition(scriptKey)
       │              │
       │              ▼
       │         ScriptExecutionService.evaluateCondition(scriptKey, ctx)
       │              │
       │              ├── Load ScriptDefinition from DB
       │              ├── Build jdc.* bindings from WorkflowContext
       │              ├── Execute in GraalJS sandbox
       │              ├── Log execution result
       │              └── Return boolean (true = allow, false = block)
       │
       ├─2─► ValidatorExecutor.validate()
       │         │
       │         ├── Built-in validators (field required, regex, etc.)
       │         └── SCRIPT validator
       │              │
       │              ▼
       │         Same flow → returns Optional<String> error message
       │
       └─3─► PostFunctionPipeline.execute()
                 │
                 ├── Essential chain (status, comment, history, reindex)
                 ├── Built-in post-functions (assign, set field, etc.)
                 └── SCRIPT_POST_FUNCTION
                      │
                      ▼
                 Same flow → executes for side effects
```

### Database Schema

```sql
jira_workflow.script_definitions
├── id (UUID PK)
├── name (VARCHAR 255)
├── description (TEXT)
├── script_type (VARCHAR 20) -- CONDITION | VALIDATOR | POST_FUNCTION
├── script_key (VARCHAR 255 UNIQUE) -- lookup key
├── script_body (TEXT) -- JavaScript source code
├── version (INTEGER)
├── is_enabled (BOOLEAN)
├── created_by (UUID)
├── updated_by (UUID)
├── created_at (TIMESTAMP)
└── updated_at (TIMESTAMP)

jira_workflow.script_versions
├── id (UUID PK)
├── script_id (UUID FK → script_definitions)
├── version (INTEGER)
├── script_body (TEXT) -- snapshot of script at this version
├── change_summary (TEXT)
├── created_by (UUID)
└── created_at (TIMESTAMP)

jira_workflow.script_execution_log
├── id (UUID PK)
├── script_id (UUID FK → script_definitions)
├── script_key (VARCHAR 255)
├── script_type (VARCHAR 20)
├── execution_mode (VARCHAR 20) -- WORKFLOW | CONSOLE
├── issue_id (UUID)
├── project_id (UUID)
├── user_id (UUID)
├── transition_id (UUID)
├── success (BOOLEAN)
├── result_value (TEXT)
├── error_message (TEXT)
├── execution_ms (BIGINT)
├── context_summary (TEXT)
└── created_at (TIMESTAMP)
```

---

## 4. JDC Script Language Reference

### Language

JDC Script uses **JavaScript (ECMAScript 2022)** in strict mode. All standard JavaScript features are available:

- Variables: `var`, `let`, `const`
- Functions: declarations, arrow functions, closures
- Control flow: `if/else`, `switch`, `for`, `while`, `do/while`
- Data types: strings, numbers, booleans, arrays, objects, null, undefined
- Operators: arithmetic, comparison, logical, ternary, nullish coalescing (`??`), optional chaining (`?.`)
- Destructuring, spread/rest, template literals
- Array methods: `map`, `filter`, `reduce`, `find`, `some`, `every`, `forEach`, `includes`
- String methods: `startsWith`, `endsWith`, `includes`, `trim`, `split`, `replace`
- Object methods: `Object.keys`, `Object.values`, `Object.entries`, `Object.assign`
- JSON: `JSON.parse`, `JSON.stringify`
- Regular expressions
- Error handling: `try/catch/finally`, `throw`

### What is NOT available

Due to sandbox restrictions:
- No `import` / `require` — no module loading
- No `fetch` / `XMLHttpRequest` — no network access
- No `fs` / file system access
- No `setTimeout` / `setInterval` — no timers
- No `eval` or `Function()` constructor
- No `Java.type()` — no Java class access
- No `Proxy` or `Reflect` — no meta-programming escapes
- No `SharedArrayBuffer` — no shared memory
- No `Worker` — no threading

### Return Value Convention

The **last expression** in the script is the return value. Alternatively, you can assign to a `result` variable:

```javascript
// Option 1: Last expression (preferred)
var issue = jdc.getIssue();
issue.priority === "Critical";  // returns true/false

// Option 2: Explicit result
var result;
if (jdc.getIssueField("priority") === "Critical") {
    result = true;
} else {
    result = false;
}
result;  // last expression is still the return
```

### Built-in Globals

| Global | Type | Description |
|---|---|---|
| `jdc` | Object | The JDC API namespace — all platform interactions |
| `console` | Object | Logging: `console.log()`, `console.warn()`, `console.error()` |
| `issueId` | String | Current issue UUID |
| `projectId` | String | Current project UUID |
| `userId` | String | Current user UUID |
| `issueData` | Object | Pre-fetched issue data (fields, status, etc.) |
| `userData` | Object | Pre-fetched user data (groups, permissions, display name) |
| `screenInput` | Object | Transition screen form data submitted by the user |

---

## 5. Script Types & Contracts

### CONDITION Scripts

**Purpose:** Gate workflow transitions. Determines whether a transition is allowed.

**Contract:**
- Must return a **boolean** value
- `true` = transition is allowed
- `false` = transition is blocked
- On error: defaults to `false` (fail-closed — blocks the transition)

**When executed:** During step 8 of the workflow pipeline (after permission checks, before validators)

```javascript
// Example: Only allow transition if all subtasks are Done
var issue = jdc.getIssue();
var subtasks = issue.subtasks || [];
var allDone = subtasks.length === 0 || subtasks.every(function(st) {
    return st.statusName === "Done";
});
allDone;
```

### VALIDATOR Scripts

**Purpose:** Enforce business rules before a transition. Returns an error message or null.

**Contract:**
- Return `null`, `undefined`, or empty string = **validation passes**
- Return a non-empty string = **validation fails** with that string as the error message
- On error: returns the error message (blocks the transition with an error)

**When executed:** During step 9 of the workflow pipeline (after conditions pass, before post-functions)

```javascript
// Example: Require description > 20 chars for Critical/Blocker issues
var priority = jdc.getIssueField("priority");
var description = jdc.getIssueField("description") || "";
if ((priority === "Critical" || priority === "Blocker") && description.length < 20) {
    "Critical/Blocker issues require a description of at least 20 characters";
} else {
    null;
}
```

### POST_FUNCTION Scripts

**Purpose:** Execute side effects after a transition is approved. No return value expected.

**Contract:**
- Return value is ignored
- Runs after essential post-functions (status update, comment, change history)
- On error: error is logged, transition is NOT rolled back (log-and-continue)
- Can modify issue fields, add comments, create links, etc. via `jdc.*` API

**When executed:** During step 10 of the workflow pipeline (after validators pass, in the post-function pipeline)

```javascript
// Example: Auto-assign Critical issues to project lead
var priority = jdc.getIssueField("priority");
if (priority === "Critical") {
    var project = jdc.getProject();
    jdc.setIssueField("assigneeId", project.leadUserId);
    jdc.addComment("Auto-assigned to project lead due to Critical priority");
    console.log("Auto-assigned issue to project lead: " + project.leadUserId);
}
```

---

## 6. API Reference — `jdc.*` Namespace

### Namespaced vs Flat API

JDC Script supports both a **namespaced API** (recommended) and a **flat API** (backward compatible):

```javascript
// Namespaced (recommended)
var issue = jdc.issue.getCurrentIssue();
var comments = jdc.issue.getComments();
var project = jdc.project.getProjectByKey("PROJ");
var results = jdc.search.jql("status = Open", 50);

// Flat (backward compatible)
var issue = jdc.getIssue();
var field = jdc.getIssueField("priority");
```

### Sub-Object Reference

| Namespace | Purpose | Key Methods |
|---|---|---|
| `jdc.issue` | Issue operations | `getCurrentIssue()`, `getFieldValue()`, `setFieldValue()`, `getIssue(key)`, `addComment()`, `getComments()`, `getHistory()`, `getWatchers()`, `addWatcher()`, `link()`, `getLinkedIssues()`, `getAttachmentCount()` |
| `jdc.project` | Project data | `getCurrentProject()`, `getProject(id)`, `getProjectByKey(key)`, `getVersions(id)`, `getComponents(id)`, `getIssueTypes()`, `getMembers(id)` |
| `jdc.user` | User data | `getCurrentUser()`, `getUser(id)`, `isInGroup(name)`, `hasPermission(perm)`, `getUserGroups()` |
| `jdc.workflow` | Workflow metadata | `getCurrentTransition()`, `getAllStatuses()` |
| `jdc.search` | JQL search | `jql(query, maxResults)`, `findIssues(projectKey, statusName)` |
| `jdc.log` | Structured logging | `info()`, `warn()`, `error()`, `debug()` |

---

### Flat API (Backward Compatible)

#### `jdc.getIssue()`
Fetches the full issue data from the Issue Service.

**Returns:** `Object` — Issue data map including all fields

**Example:**
```javascript
var issue = jdc.getIssue();
console.log(issue.summary);       // "Fix login bug"
console.log(issue.issueKey);      // "PROJ-123"
console.log(issue.statusName);    // "In Progress"
console.log(issue.priorityName);  // "High"
console.log(issue.assigneeId);    // UUID string or null
console.log(issue.reporterId);    // UUID string
console.log(issue.issueTypeName); // "Bug"
```

#### `jdc.getIssueField(fieldName)`
Reads a specific field from the pre-fetched issue data (no HTTP call).

**Parameters:**
- `fieldName` (String) — The field name to read

**Returns:** The field value (String, Number, Boolean, Object, or null)

**Example:**
```javascript
var priority = jdc.getIssueField("priority");
var storyPoints = jdc.getIssueField("storyPoints");
var labels = jdc.getIssueField("labels");  // array
```

#### `jdc.setIssueField(fieldName, value)`
Updates a field on the current issue via the Issue Service.

**Parameters:**
- `fieldName` (String) — The field name to update
- `value` (any) — The new value

**Returns:** void

**Example:**
```javascript
jdc.setIssueField("assigneeId", "550e8400-e29b-41d4-a716-446655440000");
jdc.setIssueField("priorityId", "high-priority-uuid");
jdc.setIssueField("labels", ["urgent", "production"]);
```

---

### User API

#### `jdc.getCurrentUser()`
Fetches the user performing the workflow transition.

**Returns:** `Object` — User data map

**Example:**
```javascript
var user = jdc.getCurrentUser();
console.log(user.displayName);  // "John Doe"
console.log(user.username);     // "jdoe"
console.log(user.email);        // "jdoe@example.com"
console.log(user.groups);       // ["jira-users", "developers"]
```

#### `jdc.getUser(userId)`
Fetches any user by UUID.

**Parameters:**
- `userId` (String) — The user UUID

**Returns:** `Object` — User data map

**Example:**
```javascript
var assignee = jdc.getUser(jdc.getIssueField("assigneeId"));
console.log(assignee.displayName);
```

---

### Project API

#### `jdc.getProject()`
Fetches the project the issue belongs to.

**Returns:** `Object` — Project data map

**Example:**
```javascript
var project = jdc.getProject();
console.log(project.name);        // "My Project"
console.log(project.projectKey);  // "PROJ"
console.log(project.leadUserId);  // UUID of project lead
console.log(project.projectType); // "software"
```

---

### Comment API

#### `jdc.addComment(content)`
Adds a comment to the current issue.

**Parameters:**
- `content` (String) — The comment body text

**Returns:** void

**Example:**
```javascript
jdc.addComment("This issue was automatically escalated to Critical priority.");
jdc.addComment("Transition performed by script: auto-assign-on-close");
```

---

### Permission API

#### `jdc.hasPermission(permission)`
Checks if the current user has a specific permission in the current project.

**Parameters:**
- `permission` (String) — The permission key (e.g., "EDIT_ISSUES", "CLOSE_ISSUES", "ASSIGN_ISSUES")

**Returns:** `boolean`

**Example:**
```javascript
if (!jdc.hasPermission("CLOSE_ISSUES")) {
    "You do not have permission to close issues in this project";
}
```

---

### Linked Issues API

#### `jdc.getLinkedIssues()`
Fetches all issues linked to the current issue.

**Returns:** `Array<Object>` — List of linked issue data

**Example:**
```javascript
var links = jdc.getLinkedIssues();
var hasBlocker = links.some(function(link) {
    return link.linkType === "blocks" && link.statusName !== "Done";
});
if (hasBlocker) {
    "Cannot close: blocked by unresolved issues";
}
```

---

### Screen Input API

#### `jdc.getScreenInput()`
Returns the data submitted via the transition screen form.

**Returns:** `Object` — Key-value map of screen form fields

**Example:**
```javascript
var input = jdc.getScreenInput();
var resolution = input.resolutionId;
var comment = input.comment;
if (resolution === "wontfix" && (!comment || comment.length < 10)) {
    "Please provide a reason when resolving as Won't Fix (at least 10 characters)";
}
```

---

### Attachment API

#### `jdc.getAttachmentCount()`
Returns the number of attachments on the current issue.

**Returns:** `number`

**Example:**
```javascript
if (jdc.getAttachmentCount() < 1) {
    "At least one attachment is required before closing a bug";
}
```

---

### Transition Metadata API

#### `jdc.getTransitionName()`
Returns the name of the transition being executed.

**Returns:** `String`

#### `jdc.getFromStatusId()`
Returns the UUID of the current (source) status.

**Returns:** `String`

#### `jdc.getToStatusId()`
Returns the UUID of the target status.

**Returns:** `String`

**Example:**
```javascript
console.log("Transition: " + jdc.getTransitionName());
console.log("From: " + jdc.getFromStatusId() + " To: " + jdc.getToStatusId());
```

---

## 7. Sandbox Security Model

### GraalVM Sandbox Configuration

Every script executes in an isolated GraalVM `Context` with the following restrictions:

| Security Control | Setting | Effect |
|---|---|---|
| Host class access | `allowHostClassLookup(className -> false)` | Cannot access any Java class via `Java.type()` |
| Host access | `HostAccess.EXPLICIT` | Only `@HostAccess.Export` annotated methods callable |
| File system | `IOAccess.NONE` | No filesystem read/write |
| Network | No IO access | No outbound network calls from scripts |
| Thread creation | `allowCreateThread(false)` | Cannot spawn threads |
| Process creation | `allowCreateProcess(false)` | Cannot execute system commands |
| Native access | `allowNativeAccess(false)` | No FFI/JNI |
| Environment | `EnvironmentAccess.NONE` | Cannot read environment variables |

### Resource Limits

| Limit | Default | Configurable | Description |
|---|---|---|---|
| CPU timeout | 5,000 ms | `jira.scripting.timeout-ms` | Script execution time limit |
| Console timeout | 10,000 ms | `jira.scripting.console-timeout-ms` | Longer timeout for console testing |
| Statement limit | 500,000 | `jira.scripting.max-statements` | Max JS statements (prevents infinite loops) |
| Memory limit | 64 MB | `jira.scripting.memory-limit-mb` | Max heap per execution context |

### What happens when limits are exceeded

- **Timeout:** The GraalVM context is forcibly closed via `context.close(true)`, which throws a `PolyglotException` with `isCancelled() == true`. The script returns an error result.
- **Statement limit:** Throws a `PolyglotException` with `isResourceExhausted() == true`. Prevents infinite `while(true)` loops.
- **Memory limit:** Throws a `PolyglotException` with `isResourceExhausted() == true`.

### Script-to-Platform boundary

Scripts interact with the platform ONLY through the `jdc.*` API. Each API call is a method on a Java object annotated with `@HostAccess.Export`. The Java methods internally call `WorkflowIntegrationClient` which makes REST calls to other microservices with the `X-Workflow-Internal: true` header. Scripts cannot bypass this boundary.

```
Script (JS) ──► @HostAccess.Export method (Java) ──► REST call (HTTP) ──► Target Service
     │                                                                         │
     └── Sandboxed (no direct access)                     Internal header ──────┘
```

---

## 8. Administration Guide

### Creating a Script

1. Navigate to **Workflow Administration > Scripts** (`/workflows/admin/scripts`)
2. Click **Create Script**
3. Fill in:
   - **Name:** Human-readable name (e.g., "Auto-assign Critical Issues")
   - **Script Key:** Unique lowercase identifier (e.g., `auto-assign-critical`) — this is how the script is referenced in workflow configuration
   - **Type:** CONDITION, VALIDATOR, or POST_FUNCTION
   - **Description:** What the script does
   - **Script Body:** JavaScript code in the Monaco editor
4. Click **Save**

### Testing a Script

1. Open the script editor or click **Test in Console**
2. The console provides:
   - **Script Editor** (left) — the JavaScript code
   - **Mock Context** (right top) — JSON with test data:
     ```json
     {
       "issueId": "00000000-0000-0000-0000-000000000001",
       "projectId": "00000000-0000-0000-0000-000000000002",
       "userId": "00000000-0000-0000-0000-000000000003",
       "issueData": {
         "summary": "Test Issue",
         "priority": "High",
         "statusName": "Open",
         "assigneeId": null
       },
       "userData": {
         "displayName": "Test User",
         "groups": ["jira-users", "developers"]
       },
       "screenInput": {}
     }
     ```
   - **Result** (right bottom) — shows success/error, return value, execution time
3. Click **Run** to execute
4. Review the result and adjust

### Attaching a Script to a Workflow Transition

1. Open the **Workflow Designer** and select a transition
2. Open the **Transition Configuration Panel**
3. For conditions: Add a `SCRIPT` type condition, select your script from the dropdown
4. For validators: Add a `SCRIPT` type validator, select your script
5. For post-functions: Add a `SCRIPT_POST_FUNCTION` type post-function, select your script
6. The script's `scriptKey` is stored in the transition configuration

### Version History & Rollback

- Every time a script is saved, a new version is created automatically
- View version history at **Scripts > [Script] > Versions**
- Each version stores the complete script body and a change summary
- Click **Revert** on any version to restore it (creates a new version with the old body)

### Enabling / Disabling Scripts

- Toggle the **Enabled** switch on the scripts list
- Disabled scripts will not execute during workflow transitions
- Attempting to use a disabled script returns an error result

### Monitoring Execution

- View execution logs at **Scripts > [Script] > Execution Log** or **Scripts > Execution Log** (global)
- Each log entry shows: script, issue, user, success/error, result, execution time
- Logs are retained for 30 days by default (configurable via `jira.scripting.log-retention-days`)

---

## 9. REST API Reference

### Base URL: `/api/workflow/scripts`

#### CRUD Operations

| Method | Endpoint | Description |
|---|---|---|
| `GET /` | List all scripts | Optional `?type=CONDITION\|VALIDATOR\|POST_FUNCTION` filter |
| `GET /{id}` | Get script by ID | Returns full script with body |
| `POST /` | Create a new script | Body: `CreateScriptRequest` |
| `PUT /{id}` | Update a script | Body: `UpdateScriptRequest` |
| `DELETE /{id}` | Delete a script | Cascading delete of versions and logs |
| `PATCH /{id}/toggle` | Toggle enabled state | `?enabled=true\|false` |

#### Version Management

| Method | Endpoint | Description |
|---|---|---|
| `GET /{id}/versions` | Get version history | Ordered newest first |
| `POST /{id}/revert/{version}` | Revert to version | Creates new version with old body |

#### Execution

| Method | Endpoint | Description |
|---|---|---|
| `GET /{id}/executions` | Script execution log | Paginated, `?page=0&size=20` |
| `GET /executions` | Global execution log | Paginated, all scripts |
| `POST /console` | Console test execution | Body: `ScriptConsoleRequest` |

#### Workflow Integration

| Method | Endpoint | Description |
|---|---|---|
| `GET /available` | List enabled scripts | `?type=CONDITION` — for workflow config dropdowns |

### Request / Response DTOs

#### CreateScriptRequest
```json
{
  "name": "Auto-assign Critical Issues",
  "description": "Assigns Critical issues to the project lead on transition to In Progress",
  "scriptType": "POST_FUNCTION",
  "scriptKey": "auto-assign-critical",
  "scriptBody": "var priority = jdc.getIssueField('priority');\nif (priority === 'Critical') {\n  var project = jdc.getProject();\n  jdc.setIssueField('assigneeId', project.leadUserId);\n}"
}
```

#### UpdateScriptRequest
```json
{
  "name": "Auto-assign Critical Issues v2",
  "scriptBody": "// updated script body...",
  "changeSummary": "Added comment on auto-assign"
}
```

#### ScriptResponse
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Auto-assign Critical Issues",
  "description": "Assigns Critical issues to the project lead",
  "scriptType": "POST_FUNCTION",
  "scriptKey": "auto-assign-critical",
  "scriptBody": "var priority = jdc.getIssueField('priority');\n...",
  "version": 3,
  "isEnabled": true,
  "createdBy": "user-uuid",
  "updatedBy": "user-uuid",
  "createdAt": "2026-07-24T10:00:00",
  "updatedAt": "2026-07-24T14:30:00"
}
```

#### ScriptConsoleRequest
```json
{
  "scriptBody": "jdc.getIssueField('priority') === 'Critical';",
  "scriptType": "CONDITION",
  "context": {
    "issueId": "00000000-0000-0000-0000-000000000001",
    "projectId": "00000000-0000-0000-0000-000000000002",
    "userId": "00000000-0000-0000-0000-000000000003",
    "issueData": { "priority": "Critical", "summary": "Test" },
    "userData": { "displayName": "Admin", "groups": ["jira-admins"] },
    "screenInput": {}
  }
}
```

#### ScriptConsoleResponse
```json
{
  "success": true,
  "result": true,
  "errorMessage": null,
  "executionMs": 12,
  "logs": "[script] Checking priority..."
}
```

---

## 10. Frontend UI Guide

### Script Management Page (`/workflows/admin/scripts`)

The script management page is accessed via **Workflow Administration > Scripts**. It provides:

**Scripts Tab:**
- Table listing all scripts with columns: Name, Key, Type, Version, Enabled (toggle), Actions (Edit, Delete)
- Create button opens a modal with:
  - Name field
  - Script Key field (auto-generated from name, editable)
  - Type dropdown (CONDITION, VALIDATOR, POST_FUNCTION)
  - Description textarea
  - Monaco editor for JavaScript code (VS Code-like experience with syntax highlighting, auto-completion, bracket matching)

**Execution Log Tab:**
- Paginated table showing all script executions across the system
- Columns: Script, Type, Mode (Workflow/Console), Issue, Success, Time, Date
- Click a row to see full details (result value, error message, context summary)

### Script Console

The console is a split-panel testing environment:
- **Left panel:** Monaco editor with the script body
- **Right top panel:** Monaco editor (JSON mode) for the mock execution context
- **Right bottom panel:** Results display showing success/error, return value, execution time, and captured console.log output
- **Controls:** Script type selector (CONDITION/VALIDATOR/POST_FUNCTION) and Run button

---

## 11. Code Examples Cookbook

### Condition: Only allow transition for Admins

```javascript
var user = jdc.getCurrentUser();
var groups = user.groups || [];
groups.indexOf("jira-administrators") >= 0;
```

### Condition: Block transition if unresolved blockers exist

```javascript
var links = jdc.getLinkedIssues();
var unresolvedBlockers = links.filter(function(link) {
    return link.linkType === "is blocked by" && link.statusName !== "Done" && link.statusName !== "Closed";
});
unresolvedBlockers.length === 0;
```

### Condition: Require minimum story points before closing

```javascript
var points = jdc.getIssueField("storyPoints");
points !== null && points !== undefined && points > 0;
```

### Validator: Require description for Bugs

```javascript
var issueType = jdc.getIssueField("issueTypeName");
var description = jdc.getIssueField("description") || "";
if (issueType === "Bug" && description.trim().length < 20) {
    "Bug reports must have a description of at least 20 characters. Current: " + description.length;
} else {
    null;
}
```

### Validator: Require resolution comment when resolving as Won't Fix

```javascript
var input = jdc.getScreenInput();
var resolution = input.resolutionName || input.resolutionId || "";
var comment = input.comment || "";
if (resolution.toLowerCase().indexOf("won't fix") >= 0 || resolution.toLowerCase() === "wontfix") {
    if (comment.length < 10) {
        "When resolving as Won't Fix, please provide a reason (minimum 10 characters)";
    } else {
        null;
    }
} else {
    null;
}
```

### Validator: Ensure all required custom fields are filled

```javascript
var requiredFields = ["component", "affectsVersion", "fixVersion"];
var missing = [];
for (var i = 0; i < requiredFields.length; i++) {
    var val = jdc.getIssueField(requiredFields[i]);
    if (val === null || val === undefined || val === "") {
        missing.push(requiredFields[i]);
    }
}
if (missing.length > 0) {
    "The following fields are required: " + missing.join(", ");
} else {
    null;
}
```

### Post-function: Auto-assign to project lead for Critical issues

```javascript
var priority = jdc.getIssueField("priority");
if (priority === "Critical" || priority === "Blocker") {
    var project = jdc.getProject();
    if (project.leadUserId) {
        jdc.setIssueField("assigneeId", project.leadUserId);
        jdc.addComment("[Automated] Issue auto-assigned to project lead due to " + priority + " priority.");
        console.log("Auto-assigned issue to project lead: " + project.leadUserId);
    }
}
```

### Post-function: Add label based on component

```javascript
var issue = jdc.getIssue();
var component = issue.componentName || "";
if (component.toLowerCase().indexOf("security") >= 0) {
    var labels = issue.labels || [];
    if (labels.indexOf("security-review") < 0) {
        labels.push("security-review");
        jdc.setIssueField("labels", labels);
        jdc.addComment("[Automated] Added 'security-review' label for security component.");
    }
}
```

### Post-function: Escalation notification

```javascript
var priority = jdc.getIssueField("priority");
var transition = jdc.getTransitionName();
if (priority === "Blocker" && transition === "Reopen") {
    jdc.addComment("[ESCALATION] Blocker issue reopened. Notifying project lead and team.");
    console.warn("Blocker reopened: " + issueId);
}
```

### Advanced: Chained validation with multiple rules

```javascript
var errors = [];
var priority = jdc.getIssueField("priority");
var description = jdc.getIssueField("description") || "";
var assignee = jdc.getIssueField("assigneeId");
var attachments = jdc.getAttachmentCount();

if (priority === "Critical" || priority === "Blocker") {
    if (description.length < 50) {
        errors.push("High-priority issues require a detailed description (50+ characters)");
    }
    if (!assignee) {
        errors.push("High-priority issues must have an assignee");
    }
    if (attachments < 1) {
        errors.push("High-priority issues require at least one attachment (screenshot, log, etc.)");
    }
}

errors.length > 0 ? errors.join("; ") : null;
```

---

## 12. Implementation Roadmap

### Phase 1: Documentation & Foundation

| Task | Subtask | Description | Files |
|---|---|---|---|
| 1.1 | Documentation | Create this `sil_alternative.md` file | `sil_alternative.md` |
| 1.2 | Maven deps | Add GraalVM Polyglot dependencies | `jira-workflow-service/pom.xml` |
| 1.3 | Database | Flyway V15 migration — script tables | `V15__script_engine_tables.sql` |
| 1.4.1 | Entity | ScriptDefinition entity | `entity/ScriptDefinition.java` |
| 1.4.2 | Entity | ScriptVersion entity | `entity/ScriptVersion.java` |
| 1.4.3 | Entity | ScriptExecutionLog entity | `entity/ScriptExecutionLog.java` |
| 1.5.1 | Repository | ScriptDefinitionRepository | `repository/ScriptDefinitionRepository.java` |
| 1.5.2 | Repository | ScriptVersionRepository | `repository/ScriptVersionRepository.java` |
| 1.5.3 | Repository | ScriptExecutionLogRepository | `repository/ScriptExecutionLogRepository.java` |
| 1.6.1 | Config | ScriptEngineProperties class | `config/ScriptEngineProperties.java` |
| 1.6.2 | Config | application.yml scripting section | `application.yml` |

### Phase 2: Script Engine Core

| Task | Subtask | Description | Files |
|---|---|---|---|
| 2.1.1 | Engine | GraalScriptEngine — sandbox, timeout, execution | `engine/script/GraalScriptEngine.java` |
| 2.1.2 | Engine | ScriptResult record | `engine/script/ScriptResult.java` |
| 2.2.1 | Bindings | JdcScriptBindings — builds bindings map | `engine/script/JdcScriptBindings.java` |
| 2.2.2 | Bindings | JdcApi — the `jdc.*` namespace methods | `engine/script/JdcApi.java` |
| 2.2.3 | Bindings | JdcConsole — console.log/warn/error | `engine/script/JdcConsole.java` |
| 2.3 | Service | ScriptExecutionService — orchestrates execution + logging | `engine/script/ScriptExecutionService.java` |

### Phase 3: Wire Into Workflow Engine

| Task | Subtask | Description | Files |
|---|---|---|---|
| 3.1 | Fix | WorkflowPluginRegistry — add executePostFunction() and validateWithProvider() | `engine/plugin/WorkflowPluginRegistry.java` |
| 3.2 | New | ScriptPluginRegistrar — registers scripts at startup | `engine/script/ScriptPluginRegistrar.java` |
| 3.3.1 | Fix | ValidatorExecutor — add SCRIPT to SUPPORTED_TYPES | `engine/ValidatorExecutor.java` |
| 3.3.2 | Fix | ValidatorExecutor — inject WorkflowPluginRegistry | `engine/ValidatorExecutor.java` |
| 3.3.3 | Fix | ValidatorExecutor — add SCRIPT case in evaluate() | `engine/ValidatorExecutor.java` |
| 3.4 | Fix | PostFunctionExecutor — use executePostFunction() instead of evaluateCondition() | `engine/PostFunctionExecutor.java` |
| 3.5 | Fix | ConditionEvaluator — enrich script context with full WorkflowContext data | `engine/ConditionEvaluator.java` |

### Phase 4: REST API & Service Layer

| Task | Subtask | Description | Files |
|---|---|---|---|
| 4.1.1 | DTO | CreateScriptRequest | `dto/CreateScriptRequest.java` |
| 4.1.2 | DTO | UpdateScriptRequest | `dto/UpdateScriptRequest.java` |
| 4.1.3 | DTO | ScriptResponse | `dto/ScriptResponse.java` |
| 4.1.4 | DTO | ScriptConsoleRequest | `dto/ScriptConsoleRequest.java` |
| 4.1.5 | DTO | ScriptConsoleResponse | `dto/ScriptConsoleResponse.java` |
| 4.1.6 | DTO | ScriptVersionResponse | `dto/ScriptVersionResponse.java` |
| 4.1.7 | DTO | ScriptExecutionLogResponse | `dto/ScriptExecutionLogResponse.java` |
| 4.2 | Service | ScriptDefinitionService — CRUD + versioning | `service/ScriptDefinitionService.java` |
| 4.3 | Controller | ScriptController — REST API at /api/workflow/scripts | `controller/ScriptController.java` |

### Phase 5: Frontend

| Task | Subtask | Description | Files |
|---|---|---|---|
| 5.1 | API | scriptApi.ts — TypeScript API module | `jira-frontend/src/api/scriptApi.ts` |
| 5.2 | Page | WorkflowScriptsPage — Monaco-based script management | `jira-frontend/src/features/workflows/pages/WorkflowScriptsPage.tsx` |
| 5.3 | Component | ScriptConsole — test execution panel | `jira-frontend/src/features/workflows/components/ScriptConsole.tsx` |
| 5.4.1 | Route | App.tsx — add scripts route | `jira-frontend/src/App.tsx` |
| 5.4.2 | Nav | WorkflowAdminShell — add Scripts tab | `WorkflowAdminShell.tsx` |
| 5.4.3 | Hub | WorkflowAdminHubPage — add Scripts section | `WorkflowAdminHubPage.tsx` |
| 5.4.4 | Dep | package.json — add @monaco-editor/react | `jira-frontend/package.json` |

---

## 13. Codebase Reference

### Key Existing Files (Modified)

| File | Purpose |
|---|---|
| `jira-workflow-service/pom.xml` | Add GraalJS dependencies |
| `jira-workflow-service/src/main/resources/application.yml` | Add scripting config |
| `engine/plugin/WorkflowPluginRegistry.java` | Add executePostFunction(), validateWithProvider() |
| `engine/ValidatorExecutor.java` | Add SCRIPT support |
| `engine/PostFunctionExecutor.java` | Fix executeScript() to use proper post-function path |
| `engine/ConditionEvaluator.java` | Enrich script context |
| `jira-frontend/src/App.tsx` | Add scripts route |
| `jira-frontend/src/features/workflows/components/WorkflowAdminShell.tsx` | Add Scripts tab |

### New Files Created

| File | Purpose |
|---|---|
| `sil_alternative.md` | This documentation |
| `db/migration/V15__script_engine_tables.sql` | Database schema |
| `entity/ScriptDefinition.java` | Script storage entity |
| `entity/ScriptVersion.java` | Version history entity |
| `entity/ScriptExecutionLog.java` | Execution audit entity |
| `repository/ScriptDefinitionRepository.java` | Script CRUD repository |
| `repository/ScriptVersionRepository.java` | Version history repository |
| `repository/ScriptExecutionLogRepository.java` | Execution log repository |
| `config/ScriptEngineProperties.java` | Configuration properties |
| `engine/script/GraalScriptEngine.java` | Sandboxed JS execution engine |
| `engine/script/ScriptResult.java` | Execution result record |
| `engine/script/JdcScriptBindings.java` | Bindings builder |
| `engine/script/JdcApi.java` | The jdc.* API proxy |
| `engine/script/JdcConsole.java` | console.log support |
| `engine/script/ScriptExecutionService.java` | Execution orchestrator |
| `engine/script/ScriptPluginRegistrar.java` | Dynamic registration |
| `dto/CreateScriptRequest.java` | Create request DTO |
| `dto/UpdateScriptRequest.java` | Update request DTO |
| `dto/ScriptResponse.java` | Response DTO |
| `dto/ScriptConsoleRequest.java` | Console request DTO |
| `dto/ScriptConsoleResponse.java` | Console response DTO |
| `dto/ScriptVersionResponse.java` | Version response DTO |
| `dto/ScriptExecutionLogResponse.java` | Execution log response DTO |
| `service/ScriptDefinitionService.java` | CRUD + versioning service |
| `controller/ScriptController.java` | REST controller |
| `jira-frontend/src/api/scriptApi.ts` | Frontend API module |
| `jira-frontend/src/features/workflows/pages/WorkflowScriptsPage.tsx` | Management page |
| `jira-frontend/src/features/workflows/components/ScriptConsole.tsx` | Console component |

### Technology Stack

| Technology | Version | Purpose |
|---|---|---|
| GraalVM Polyglot API | 24.1.1 | JavaScript execution engine |
| GraalJS | 24.1.1 | JavaScript language implementation |
| Java | 21 | Runtime platform |
| Spring Boot | 3.4.5 | Application framework |
| PostgreSQL | 16 | Database |
| Flyway | (managed) | Database migrations |
| Monaco Editor | 4.6.0 | Frontend code editor |
| React | (existing) | Frontend framework |
| TypeScript | (existing) | Frontend language |
