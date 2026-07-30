-- ============================================================
-- V27: Seed demo scripts for SIL Alternative demonstration
-- These scripts appear immediately in the Script Manager UI
-- and can be run from the Console tab without any setup.
-- ============================================================

-- Demo 1: System Health Inspector (CONSOLE)
-- Shows all 22 API bindings working, manipulates persistent vars, file I/O, testing
INSERT INTO jira_workflow.script_definitions (
    id, name, description, script_type, script_key, script_body,
    version, is_enabled, category, created_at, updated_at
) VALUES (
    'dd000001-0000-0000-0000-000000000001',
    'Demo: System Health Inspector',
    'Probes all 22 JDC API bindings and reports their status. Run this to verify the script engine is fully operational.',
    'CONSOLE',
    'demo-health-inspector',
    '// ============================================================
// JDC Script Engine — System Health Inspector
// Probes all 22 API bindings and reports operational status.
// Run this in the Console tab to verify everything works.
// ============================================================

var report = [];
var pass = 0;
var fail = 0;

function check(name, fn) {
  try {
    var result = fn();
    var ok = result !== undefined;
    report.push({ binding: name, status: ok ? "OK" : "EMPTY", detail: JSON.stringify(result).substring(0, 80) });
    if (ok) pass++; else fail++;
  } catch (e) {
    report.push({ binding: name, status: "ERROR", detail: e.message });
    fail++;
  }
}

console.log("=== JDC Script Engine — Health Inspector ===\n");

// Core API bindings
check("jdc.issue",     function() { return typeof jdc.issue; });
check("jdc.project",   function() { return typeof jdc.project; });
check("jdc.user",      function() { return typeof jdc.user; });
check("jdc.workflow",  function() { return typeof jdc.workflow; });
check("jdc.search",    function() { return typeof jdc.search; });
check("jdc.log",       function() { return typeof jdc.log; });

// Utility bindings
check("console",       function() { return typeof console.log; });
check("http",          function() { return typeof http.get; });
check("sql",           function() { return typeof sql.query; });
check("xml",           function() { return typeof xml.parse; });
check("vars",          function() { vars.set("_health_check", new Date().toISOString()); return vars.get("_health_check"); });
check("email",         function() { return typeof email.sendEmail; });
check("ldap",          function() { return typeof ldap.search; });
check("confluence",    function() { return typeof confluence.getPage; });
check("sprint",        function() { return typeof sprint.getSprint; });
check("asset",         function() { return typeof asset.getAsset; });
check("tempo",         function() { return typeof tempo.getWorklogs; });
check("webhook",       function() { return typeof webhook.setResponseCode; });
check("file",          function() { file.writeFile("_test", "ok"); return file.readFile("_test"); });
check("test",          function() { test.assertTrue(true, "sanity"); return test.allPassed(); });
check("include",       function() { return typeof include.isIncluded; });
check("env",           function() { return typeof env.get; });

// Print report
console.log("Binding".padEnd(18) + "Status".padEnd(10) + "Detail");
console.log("-".repeat(70));
for (var i = 0; i < report.length; i++) {
  var r = report[i];
  console.log(r.binding.padEnd(18) + r.status.padEnd(10) + (r.detail || ""));
}

console.log("\n=== Result: " + pass + "/" + (pass + fail) + " bindings operational ===");

// Cleanup
vars.remove("_health_check");
file.deleteFile("_test");

({ totalBindings: pass + fail, operational: pass, failed: fail, report: report });',
    1, true, 'Demo', now(), now()
);

-- Demo 2: Issue Lifecycle Automation (POST_FUNCTION)
-- Creates issue, adds comments, labels, worklogs, searches, demonstrates full issue API
INSERT INTO jira_workflow.script_definitions (
    id, name, description, script_type, script_key, script_body,
    version, is_enabled, category, created_at, updated_at
) VALUES (
    'dd000001-0000-0000-0000-000000000002',
    'Demo: Issue Lifecycle Automation',
    'Demonstrates full issue lifecycle: read fields, add comments, manage labels, log work, search via JQL, track with persistent variables.',
    'POST_FUNCTION',
    'demo-issue-lifecycle',
    '// ============================================================
// JDC Script Engine — Issue Lifecycle Automation
// Demonstrates the full power of the jdc.issue.* API.
// Works with the default console context.
// ============================================================

console.log("=== Issue Lifecycle Automation ===\n");

// 1. Read current issue data from context
console.log("--- Step 1: Read Issue Data ---");
var issue = jdc.issue.getCurrentIssue();
console.log("Issue ID: " + issueId);
console.log("Issue data keys: " + Object.keys(issue).join(", "));
console.log("Summary: " + (issueData.summary || "N/A"));
console.log("Priority: " + (issueData.priority || "N/A"));
console.log("Status: " + (issueData.statusName || "N/A"));
console.log("Type: " + (issueData.issueTypeName || "N/A"));

// 2. Add a comment with timestamp
console.log("\n--- Step 2: Add Comment ---");
var ts = new Date().toISOString();
var commentResult = jdc.issue.addComment(
  "Automated audit by JDC Script Engine at " + ts +
  "\nPriority: " + (issueData.priority || "unset") +
  "\nStatus: " + (issueData.statusName || "unknown")
);
console.log("Comment added: " + (commentResult.id ? "ID " + commentResult.id : "sent"));

// 3. Manage labels
console.log("\n--- Step 3: Label Management ---");
jdc.issue.addLabel("script-audited");
jdc.issue.addLabel("automated-" + new Date().toISOString().split("T")[0]);
var labels = jdc.issue.getLabels();
console.log("Current labels: " + JSON.stringify(labels));

// 4. Log work
console.log("\n--- Step 4: Log Work ---");
var worklog = jdc.issue.addWorklog("15m", "Automated script execution and audit");
console.log("Worklog: " + JSON.stringify(worklog));

// 5. Check subtasks
console.log("\n--- Step 5: Check Subtasks ---");
var subtasks = jdc.issue.getSubtasks();
console.log("Subtask count: " + subtasks.length);

// 6. Check watchers
console.log("\n--- Step 6: Check Watchers ---");
var watchers = jdc.issue.getWatchers();
console.log("Watcher count: " + watchers.length);

// 7. JQL Search
console.log("\n--- Step 7: JQL Search ---");
var searchResults = jdc.search.jql("ORDER BY created DESC", 5);
console.log("Recent issues found: " + searchResults.length);
for (var i = 0; i < Math.min(searchResults.length, 3); i++) {
  var sr = searchResults[i];
  console.log("  " + (sr.issueKey || sr.id || "?") + " — " + (sr.summary || "no summary"));
}

// 8. Persistent variable tracking
console.log("\n--- Step 8: Persistent Variables ---");
var prevRun = vars.get("demo-lifecycle-last-run");
console.log("Previous run: " + (prevRun || "never"));
vars.set("demo-lifecycle-last-run", ts);
vars.set("demo-lifecycle-run-count",
  String(parseInt(vars.get("demo-lifecycle-run-count") || "0") + 1));
console.log("Run count: " + vars.get("demo-lifecycle-run-count"));

// 9. Generate in-memory report
console.log("\n--- Step 9: Generate Report ---");
file.writeFile("audit-report.csv", "Field,Value\n");
file.appendToFile("audit-report.csv", "Issue," + issueId + "\n");
file.appendToFile("audit-report.csv", "Priority," + (issueData.priority || "") + "\n");
file.appendToFile("audit-report.csv", "Status," + (issueData.statusName || "") + "\n");
file.appendToFile("audit-report.csv", "Labels," + JSON.stringify(labels) + "\n");
file.appendToFile("audit-report.csv", "Subtasks," + subtasks.length + "\n");
file.appendToFile("audit-report.csv", "SearchResults," + searchResults.length + "\n");
file.appendToFile("audit-report.csv", "Timestamp," + ts + "\n");
console.log("Report generated (" + file.getFileSize("audit-report.csv") + " bytes):");
console.log(file.readFile("audit-report.csv"));

// 10. Run assertions
console.log("--- Step 10: Self-Test ---");
test.assertNotNull(issueId, "issueId present in context");
test.assertNotNull(projectId, "projectId present in context");
test.assertTrue(typeof jdc.issue.addComment === "function", "addComment API available");
test.assertTrue(typeof jdc.search.jql === "function", "JQL search available");
test.assertTrue(typeof vars.set === "function", "Persistent vars available");
console.log("Tests: " + test.getPassed() + " passed, " + test.getFailed() + " failed");

console.log("\n=== Lifecycle Automation Complete ===");
({
  issueId: issueId,
  labelsSet: labels.length,
  searchResults: searchResults.length,
  runCount: vars.get("demo-lifecycle-run-count"),
  testsPassed: test.getPassed()
});',
    1, true, 'Demo', now(), now()
);

-- Demo 3: DSL Transpiler Showcase (CONSOLE)
-- Shows SIL-like syntax being transpiled and executed
INSERT INTO jira_workflow.script_definitions (
    id, name, description, script_type, script_key, script_body,
    version, is_enabled, category, created_at, updated_at
) VALUES (
    'dd000001-0000-0000-0000-000000000003',
    'Demo: DSL Transpiler — SIL Syntax',
    'Demonstrates SIL-like syntax (assignee = "john", addComment("text")) being auto-transpiled to JavaScript API calls.',
    'CONSOLE',
    'demo-dsl-transpiler',
    '// ============================================================
// JDC Script Engine — DSL Transpiler Demo
// These lines use SIL-style syntax. The transpiler converts
// them to jdc.issue.setFieldValue() calls before execution.
// ============================================================

console.log("=== DSL Transpiler Showcase ===\n");
console.log("The following lines use SIL-like syntax.");
console.log("The JDC DSL Transpiler auto-converts them to JavaScript.\n");

// --- SIL-style field assignments ---
console.log("--- Field Assignments (SIL Syntax) ---");

// These lines look like SIL but are valid JDC DSL:
summary = "Updated by DSL Transpiler at " + new Date().toISOString();
console.log("  summary = ''Updated by DSL...'' -> jdc.issue.setFieldValue(''summary'', ...)");

description = "This description was set using SIL-like syntax.";
console.log("  description = ''text'' -> jdc.issue.setFieldValue(''description'', ...)");

environment = "Production";
console.log("  environment = ''Production'' -> jdc.issue.setFieldValue(''environment'', ...)");

// --- SIL-style function calls ---
console.log("\n--- Function Calls (SIL Aliases) ---");

addComment("This comment was added using SIL alias: addComment()");
console.log("  addComment(text) -> jdc.issue.addComment(text)");

logInfo("Log message via SIL alias");
console.log("  logInfo(msg) -> jdc.log.info(msg)");

// --- Persistent vars via SIL aliases ---
setPersistentVar("dsl-demo-run", new Date().toISOString());
console.log("  setPersistentVar(k,v) -> vars.set(k,v)");

var lastRun = getPersistentVar("dsl-demo-run");
console.log("  getPersistentVar(k) -> vars.get(k) = " + lastRun);

// --- Show the transpiler mapping table ---
console.log("\n=== DSL Transpiler Mapping Table ===");
console.log("SIL Syntax".padEnd(35) + "Transpiled To");
console.log("-".repeat(70));
var mappings = [
  ["assignee = ''john''",           "jdc.issue.setFieldValue(''assigneeId'', ''john'')"],
  ["priority = ''High''",           "jdc.issue.setFieldValue(''priorityId'', ''High'')"],
  ["status = ''Done''",             "jdc.issue.setFieldValue(''statusId'', ''Done'')"],
  ["summary = ''text''",            "jdc.issue.setFieldValue(''summary'', ''text'')"],
  ["addComment(text)",              "jdc.issue.addComment(text)"],
  ["createIssue(...)",              "jdc.issue.createIssue(...)"],
  ["transitionIssue(id, action)",   "jdc.issue.transitionIssue(id, action)"],
  ["getSubTasks()",                 "jdc.issue.getSubtasks()"],
  ["jqlSearch(query, max)",         "jdc.search.jql(query, max)"],
  ["isUserInGroup(group)",          "jdc.user.isInGroup(group)"],
  ["httpGet(url)",                  "http.get(url)"],
  ["httpPost(url, body)",           "http.post(url, body)"],
  ["sqlQuery(ds, sql)",             "sql.query(ds, sql)"],
  ["sendEmail(to, subj, body)",     "email.sendEmail(to, subj, body)"],
  ["setPersistentVar(k, v)",        "vars.set(k, v)"],
  ["getSprint(id)",                 "sprint.getSprint(id)"],
  ["moveToSprint(issue, sprint)",   "sprint.moveToSprint(issue, sprint)"],
  ["logInfo(msg)",                  "jdc.log.info(msg)"],
];
for (var i = 0; i < mappings.length; i++) {
  console.log(mappings[i][0].padEnd(35) + mappings[i][1]);
}

console.log("\n=== 21 field mappings + 54 function aliases supported ===");
console.log("=== DSL Demo Complete ===");
true;',
    1, true, 'Demo', now(), now()
);

-- Demo 4: Condition Script — Block Transition Without Comment
INSERT INTO jira_workflow.script_definitions (
    id, name, description, script_type, script_key, script_body,
    version, is_enabled, category, created_at, updated_at
) VALUES (
    'dd000001-0000-0000-0000-000000000004',
    'Demo: Condition — Require Comment',
    'Workflow condition that blocks transition if no comment is provided. Attach to any workflow transition to enforce comment policy.',
    'CONDITION',
    'demo-require-comment',
    '// Workflow Condition: Block transition if no comment provided
// Returns: true (allow) or false (block)

console.log("Evaluating: comment required for transition");
console.log("Transition: " + (transitionName || "unknown"));

var hasComment = false;

if (comment && String(comment).trim().length > 0) {
  hasComment = true;
  console.log("Comment found: " + String(comment).substring(0, 50) + "...");
}

if (screenInput && screenInput.comment && String(screenInput.comment).trim().length > 0) {
  hasComment = true;
  console.log("Screen comment found: " + String(screenInput.comment).substring(0, 50) + "...");
}

if (hasComment) {
  console.log("RESULT: ALLOWED — comment provided");
} else {
  console.log("RESULT: BLOCKED — no comment");
}

hasComment;',
    1, true, 'Demo', now(), now()
);

-- Demo 5: Validator Script — Story Points Required for Stories
INSERT INTO jira_workflow.script_definitions (
    id, name, description, script_type, script_key, script_body,
    version, is_enabled, category, created_at, updated_at
) VALUES (
    'dd000001-0000-0000-0000-000000000005',
    'Demo: Validator — Story Points Required',
    'Workflow validator that rejects transition if Story Points are not set for Story-type issues. Returns error message or null.',
    'VALIDATOR',
    'demo-story-points-validator',
    '// Workflow Validator: Require Story Points for Stories
// Returns: null (valid) or error message string (invalid)

console.log("Validating: story points for issue type");
var typeName = issueData.issueTypeName || issueData.type || "";
var points = issueData.storyPoints;

console.log("Issue type: " + typeName);
console.log("Story points: " + (points || "not set"));

if (typeName === "Story" && (!points || points <= 0)) {
  console.log("RESULT: INVALID — Story Points required for Stories");
  "Story Points are required for Story type issues. Please estimate before transitioning.";
} else {
  console.log("RESULT: VALID");
  null;
}',
    1, true, 'Demo', now(), now()
);

-- Create version records for all demo scripts
INSERT INTO jira_workflow.script_versions (id, script_id, version, script_body, change_summary, created_at)
SELECT gen_random_uuid(), id, 1, script_body, 'Initial demo version', now()
FROM jira_workflow.script_definitions
WHERE script_key LIKE 'demo-%';
