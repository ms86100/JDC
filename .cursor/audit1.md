# JIRA DATA CENTER PARITY AUDIT PROMPT — ISSUE NAVIGATOR / XRAY-STYLE IMPLEMENTATION AUDIT

You are acting as:
- A Jira Data Center Functional Architect (20+ years)
- Atlassian Plugin Ecosystem Expert
- Enterprise QA/Audit Lead
- Technical Product Architect
- Frontend + Backend Integration Auditor

Your responsibility is NOT to implement features.

Your responsibility is to perform a DEEP END-TO-END AUDIT of the current implementation and determine:

1. What is FULLY implemented
2. What is PARTIALLY implemented
3. What is MISSING
4. What is architecturally incorrect
5. What breaks Jira DC parity
6. What is visually inconsistent with Jira Data Center
7. What backend contracts are missing
8. What APIs are incomplete
9. What runtime behaviors are missing
10. What production risks exist

You MUST behave like a strict enterprise auditor.

DO NOT assume something works because UI exists.

You MUST verify:
- frontend
- backend
- routing
- API integration
- permissions
- workflows
- screen schemes
- transitions
- event propagation
- plugin extensibility
- URL persistence
- context retention
- optimistic updates
- websocket refresh
- pagination
- keyboard shortcuts
- audit logging
- CRUD completeness
- database integrity
- role-based behavior
- state synchronization

You MUST inspect:
- React pages
- Components
- Hooks
- Zustand/Redux/context stores
- Routes
- Services
- API clients
- Controllers
- DTOs
- Entities
- Repositories
- Event systems
- Database mappings
- Permission checks
- Workflow engines
- Transition validators
- Screen resolution systems
- Plugin extension points

====================================================
PRIMARY AUDIT GOAL
====================================================

Verify whether the application has achieved REAL Jira Data Center Issue Navigator parity and identify ALL remaining gaps.

You MUST compare against:
- Jira Data Center Issue Navigator
- Jira project issue navigator
- Xray-style issue interaction patterns
- Atlassian enterprise UX standards

====================================================
CRITICAL ARCHITECTURE TO VALIDATE
====================================================

You MUST validate whether BOTH modes exist:

1. GLOBAL ISSUE NAVIGATOR
Equivalent to:
- /issues/?filter=...
- global filters
- global search
- issue pills
- advanced search
- global JQL

2. PROJECT ISSUE NAVIGATOR
Equivalent to:
- /browse/KEY
- split view
- project sidebar
- embedded issue detail panel
- persistent issue context

====================================================
AUDIT PHASES
====================================================

# PHASE 1 — ROUTING & NAVIGATION AUDIT

Verify:
- Does project issues route exist?
- Does split-view route exist?
- Is routing Jira-like?
- Does selecting issue cause full reload?
- Does issue open inside panel?
- Is URL synchronized with issue selection?
- Does browser back/forward preserve state?
- Is filter preserved in URL?
- Is selected issue preserved?
- Is scroll position preserved?

Audit:
- React Router
- route nesting
- lazy loading
- context retention
- route params
- URL query synchronization

FAIL if:
- Selecting issue redirects to full page
- Context resets on navigation
- Filters disappear
- Sidebar collapses incorrectly
- URL not persistent

====================================================
# PHASE 2 — ISSUE LIST PANEL AUDIT
====================================================

Verify:
- JQL search
- sorting
- pagination
- virtualization
- issue selection
- keyboard navigation
- bulk selection
- filtering
- refresh behavior
- optimistic updates

Check:
- Does issue row update live?
- Does status refresh automatically?
- Are avatars rendered correctly?
- Are priorities/icons Jira-like?
- Is infinite scrolling implemented?
- Are loading states correct?

FAIL if:
- List refetch resets selection
- Scroll jumps
- Full page reload occurs
- Filters do not update list
- Sorting inconsistent

====================================================
# PHASE 3 — ISSUE DETAIL PANEL AUDIT
====================================================

Verify:
- Embedded detail panel exists
- Reuse vs duplicate architecture
- Issue tabs
- Comments
- Activity
- History
- Worklog
- Attachments
- Links
- Subtasks
- Watchers
- Voters
- Sprint data
- Epic links

Check:
- Inline edit behavior
- Autosave
- Optimistic updates
- rollback handling
- transition handling
- activity refresh
- websocket updates

FAIL if:
- Detail panel is separate page only
- Tabs incomplete
- No inline editing
- No optimistic update
- No refresh synchronization

====================================================
# PHASE 4 — FILTER SYSTEM AUDIT
====================================================

Verify:
- Saved filters
- Favorite filters
- Shared filters
- Filter switching
- Filter persistence
- JQL generation
- URL persistence
- Quick filters
- Search pills

Check:
- Does changing filter preserve issue?
- Does JQL regenerate correctly?
- Are filters project-aware?
- Is permission validation present?

FAIL if:
- Filters are UI-only
- JQL hardcoded
- Filter switching reloads entire page
- No persistence

====================================================
# PHASE 5 — SCREEN SCHEME AUDIT
====================================================

CRITICAL.

Verify runtime screen resolution:
- CREATE screen
- EDIT screen
- VIEW screen
- TRANSITION screen

Verify:
GET /api/projects/{id}/scheme
GET /api/admin/issues/screens/{screenId}/fields

Check:
- Field rendering by issue type
- Screen scheme mapping
- field configuration
- hidden fields
- required fields
- custom fields
- validators

FAIL if:
- Fields hardcoded
- Same fields everywhere
- Bug/Story screens identical
- Runtime resolution absent

====================================================
# PHASE 6 — WORKFLOW & TRANSITIONS AUDIT
====================================================

Verify:
GET /api/issues/{id}/transitions

Check:
- validators
- conditions
- post functions
- transition screens
- transition permissions
- status categories
- workflow schemes

Verify:
- unavailable transitions hidden
- validation errors surfaced
- workflow history stored
- transition audit logging exists

FAIL if:
- Transitions hardcoded
- No validator handling
- Permissions ignored
- Workflow not runtime-driven

====================================================
# PHASE 7 — PERMISSIONS AUDIT
====================================================

Verify:
- project permissions
- issue security
- role mapping
- group mapping
- scheme resolution
- transition permissions
- comment permissions
- attachment permissions

Check inheritance chain:
User → Groups → Roles → Scheme → Project

FAIL if:
- Frontend-only permissions
- No backend enforcement
- APIs accessible without checks

====================================================
# PHASE 8 — EVENT SYSTEM AUDIT
====================================================

Verify event bus exists.

Required events:
- IssueCreated
- IssueUpdated
- StatusChanged
- CommentAdded
- FilterChanged
- WorklogAdded
- AttachmentAdded

Check consumers:
- search index
- boards
- notifications
- audit log
- activity streams
- websocket updates

FAIL if:
- No event propagation
- Manual refresh required
- Components stale

====================================================
# PHASE 9 — PROJECT SIDEBAR AUDIT
====================================================

Verify:
- Jira DC-style sidebar
- module navigation
- contextual navigation
- plugin sections
- Reports
- Components
- Releases
- Xray-like modules

Check:
- active state
- persistence
- collapse behavior
- plugin injection

FAIL if:
- Static links only
- No contextual awareness
- No plugin architecture

====================================================
# PHASE 10 — MORE MENU / ACTIONS AUDIT
====================================================

Verify:
- move issue
- clone issue
- link issue
- watchers
- attachments
- exports
- shares
- transitions
- delete
- audit trail

Check:
- permissions
- modal flows
- validations
- cascading operations

FAIL if:
- Menu items nonfunctional
- APIs incomplete
- Missing validations

====================================================
# PHASE 11 — CREATE ISSUE FLOW AUDIT
====================================================

Verify:
- project selection
- issue type selection
- screen scheme rendering
- field validations
- defaults
- attachments
- subtasks
- linked issue creation

FAIL if:
- Static form
- No runtime fields
- No scheme integration

====================================================
# PHASE 12 — URL STATE & CONTEXT AUDIT
====================================================

Verify existence of:
IssueViewContext

Expected:
interface IssueViewContext {
  mode
  projectId
  filterId
  jql
  selectedIssueId
  sort
  listScrollOffset
  sidebarCollapsed
  activeActivityTab
}

Verify:
- URL sync
- session persistence
- restoration after refresh

FAIL if:
- State lost on refresh
- Issue context disappears

====================================================
# PHASE 13 — DATABASE & BACKEND AUDIT
====================================================

Verify:
- entities
- relationships
- indexing
- transaction handling
- optimistic locking
- soft deletes
- audit tables
- event outbox
- workflow persistence

Check:
- N+1 query issues
- missing indexes
- transaction safety
- pagination performance

====================================================
# PHASE 14 — API CONTRACT AUDIT
====================================================

Verify ALL CRUD endpoints:
- return proper status codes
- validation responses
- pagination metadata
- authorization checks
- optimistic locking
- DTO correctness

Check:
- missing endpoints
- placeholder APIs
- mocked responses
- hardcoded data

FAIL if:
- endpoint exists but not wired
- frontend bypasses API
- inconsistent schemas

====================================================
# PHASE 15 — PLUGIN ARCHITECTURE AUDIT
====================================================

Verify plugin extension support:
- issue panels
- activity tabs
- navigation injections
- custom fields
- custom actions
- workflow extensions

Verify parity with:
- Xray
- BigPicture
- Git integrations

FAIL if:
- architecture closed
- no extension contracts

====================================================
# PHASE 16 — UX & VISUAL PARITY AUDIT
====================================================

Compare against Jira Data Center.

Audit:
- spacing
- typography
- panel behavior
- transitions
- hover states
- split-view widths
- breadcrumbs
- avatars
- issue pills
- badges
- filters
- tables
- menus
- keyboard shortcuts

FAIL if:
- generic admin template feel
- not Jira-like
- inconsistent spacing
- incorrect layout hierarchy

====================================================
# PHASE 17 — PRODUCTION READINESS AUDIT
====================================================

Verify:
- websocket scalability
- caching
- debounce
- throttling
- retry logic
- loading states
- error boundaries
- skeleton loaders
- optimistic rollback
- race condition handling

FAIL if:
- fragile state management
- duplicated API calls
- memory leaks
- stale state issues

====================================================
OUTPUT FORMAT (MANDATORY)
====================================================

Provide results ONLY in this structure:

# EXECUTIVE SUMMARY

- % Jira DC parity achieved
- % backend completeness
- % frontend completeness
- % UX parity
- % production readiness

# FULLY IMPLEMENTED

Feature:
Evidence:
Files:
APIs:
Notes:

# PARTIALLY IMPLEMENTED

Feature:
What exists:
What missing:
Risk:
Files:
Required fixes:

# MISSING

Feature:
Why critical:
Affected modules:
Recommended implementation:

# ARCHITECTURAL RISKS

Severity:
Impact:
Recommendation:

# UX PARITY FAILURES

Reference Jira behavior:
Current behavior:
Gap:
Fix recommendation:

# BACKEND CONTRACT GAPS

Endpoint:
Missing behavior:
Expected contract:
Risk:

# PRODUCTION RISKS

Issue:
Likelihood:
Impact:
Fix:

# PRIORITIZED IMPLEMENTATION PLAN

Sprint 1
Sprint 2
Sprint 3
Sprint 4
Sprint 5

====================================================
STRICT RULES
====================================================

- DO NOT assume functionality works because UI exists
- VERIFY actual integration
- VERIFY actual API wiring
- VERIFY actual persistence
- VERIFY actual permissions
- VERIFY actual runtime behavior
- VERIFY actual workflow resolution
- VERIFY actual screen scheme resolution
- VERIFY actual event propagation
- VERIFY actual URL synchronization
- VERIFY actual optimistic update handling
- VERIFY actual rollback behavior

You MUST identify:
- dead code
- placeholder implementations
- mocked APIs
- duplicated architecture
- missing backend enforcement
- broken parity behavior
- hidden production risks
- technical debt

Be brutally strict.

If something is only 70% implemented, mark it PARTIAL.

If backend exists but UI not wired, mark PARTIAL.

If UI exists but runtime integration missing, mark PARTIAL.

If architecture violates Jira DC patterns, explicitly call it out.

Reference implementation target:
Jira Data Center Issue Navigator + Xray-style enterprise interaction patterns.