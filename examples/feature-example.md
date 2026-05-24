You are a Senior Jira Data Center Product Architect, Atlassian Workflow Specialist, Enterprise UX Analyst, and Staff-Level Frontend/Backend Engineer.

Your task is to perform a COMPLETE reverse engineering and functional decomposition of the Jira Data Center Kanban Board system.

CRITICAL OBJECTIVE:
I want an enterprise-grade implementation blueprint of Jira Data Center Kanban Board behavior, UI, interactions, workflows, permissions, drag-drop mechanics, filters, board configuration, swimlanes, backlog logic, ranking, WIP enforcement, quick filters, card layouts, sprint compatibility, issue transitions, and all hidden enterprise behaviors.

DO NOT provide a high-level overview.
DO NOT skip edge cases.
DO NOT simplify interactions.
DO NOT miss even a single UI element, workflow, setting, state, popup, menu, keyboard behavior, permission dependency, drag-and-drop logic, or backend implication.

The goal is to replicate Jira Data Center Kanban behavior with production-level accuracy.

---------------------------------------------------
PHASE 1 — COMPLETE UI SURFACE MAPPING
---------------------------------------------------

Perform a FULL UI decomposition of the Kanban Board.

Identify EVERY visible and hidden UI element including:

1. TOP NAVIGATION
- Board selector
- Project selector
- Search
- Create issue
- Filter indicators
- Board actions
- Share actions
- Export options
- Configure board
- Board switchers
- Favorite/star behavior
- Board breadcrumbs
- User avatar interactions
- Notification indicators

2. BOARD HEADER
- Board title
- Sprint indicators
- Active filters
- Quick filters
- Kanban backlog toggle
- Epics panel toggle
- Versions panel
- Releases
- Velocity indicators
- Board statistics
- Card count
- Done count
- WIP indicators
- Search field
- Assignee filters
- Issue type filters
- Label filters
- Component filters

3. LEFT PANELS
Explain:
- Epic panel behavior
- Version panel behavior
- Release grouping
- Expand/collapse behavior
- Filtering interaction
- Color coding
- Selection state persistence
- Drag interaction impact

4. BOARD GRID
Explain ALL:
- Columns
- Swimlanes
- Card grouping
- WIP headers
- Column constraints
- Column collapse behavior
- Scroll behavior
- Sticky header behavior
- Dynamic resizing
- Virtualization
- Performance optimization

5. ISSUE CARDS
Break down every visible element:
- Key
- Summary
- Assignee
- Priority
- Story points
- Labels
- Epic color
- Status
- Flags
- Blocked indicators
- Avatars
- Due dates
- Parent/child indicators
- Attachment indicators
- Comment count
- Subtask progress
- Rank metadata
- SLA indicators
- Custom field rendering

Explain:
- Card sizing logic
- Compact vs detailed mode
- Dynamic rendering
- Responsive behavior
- Overflow handling
- Hover states
- Inline interactions

---------------------------------------------------
PHASE 2 — COMPLETE FUNCTIONAL BEHAVIOR
---------------------------------------------------

Explain ALL board functionality in depth.

1. DRAG & DROP ENGINE
Explain:
- How drag starts
- Hover states
- Drop zones
- Reordering logic
- Rank recalculation
- Cross-column movement
- Workflow validation
- Invalid transitions
- Transition screens
- Async updates
- Optimistic updates
- Rollback handling
- Multi-user collision handling
- Realtime refresh behavior
- Scroll during drag
- Drag ghost rendering
- Touch support
- Keyboard drag support
- Performance handling for large boards

Explain backend logic:
- Rank field mechanics
- LexoRank behavior
- Transition API flow
- Event publishing
- Board refresh triggers

2. COLUMN CONFIGURATION
Explain:
- Mapping statuses to columns
- Multiple statuses per column
- Done category behavior
- Hidden statuses
- Constraint validation
- Workflow dependency
- Auto-mapping
- Status category logic

3. WIP LIMITS
Explain:
- WIP enforcement
- Visual warnings
- Soft vs hard limits
- Header indicators
- User feedback
- Overflow behavior
- Board admin configuration

4. SWIMLANES
Explain ALL supported swimlane types:
- Stories
- Assignees
- Epics
- Queries
- Projects
- Custom JQL
- Priority grouping

Explain:
- Rendering hierarchy
- Nested issue handling
- Collapse persistence
- Sorting behavior

5. QUICK FILTERS
Explain:
- JQL integration
- AND/OR behavior
- Filter persistence
- Multi-filter combinations
- Performance impact
- URL state sync

6. BACKLOG MODE
Explain:
- Kanban backlog behavior
- Visibility rules
- Ranking inside backlog
- Transition from backlog to board
- Filtering behavior
- Backlog prioritization

7. ISSUE INTERACTIONS
Explain:
- Single click
- Double click
- Context menu
- Right click
- Inline edit
- Open drawer
- Open full issue
- Keyboard shortcuts
- Hover preview
- Card actions
- Bulk actions

---------------------------------------------------
PHASE 3 — BOARD CONFIGURATION SYSTEM
---------------------------------------------------

Explain the ENTIRE board configuration module.

1. GENERAL SETTINGS
- Board name
- Admin permissions
- Sharing
- Saved filters
- Filter ownership
- Board type

2. COLUMNS CONFIG
- Status mapping
- Add/remove columns
- Rename columns
- Constraint rules
- Validation logic

3. SWIMLANE CONFIG
- Query rules
- Ordering
- Dynamic generation
- Nested hierarchy

4. QUICK FILTER CONFIG
- JQL setup
- Permissions
- Shared filters
- Persistence

5. CARD LAYOUT CONFIG
Explain:
- Card field rendering
- Compact mode
- Field prioritization
- Hidden field logic
- Custom field compatibility

6. ESTIMATION CONFIG
- Story points
- Time tracking
- Original estimates
- Remaining estimates
- Velocity implications

---------------------------------------------------
PHASE 4 — WORKFLOW + STATUS ENGINE
---------------------------------------------------

Explain deeply how Kanban integrates with Jira workflows.

Include:
- Workflow transition validation
- Transition screens
- Required fields
- Validators
- Conditions
- Post functions
- Resolution behavior
- Done state handling
- Auto-closing
- Reopen logic
- Permission checks
- Workflow schemes
- Cross-project workflows

Explain:
- What happens internally during drag
- How transitions are validated
- What happens when validation fails
- UI rollback handling
- Error recovery

---------------------------------------------------
PHASE 5 — PERMISSIONS & SECURITY
---------------------------------------------------

Explain:
- Board permissions
- Filter permissions
- Project permissions
- Issue security
- Hidden issue behavior
- Partial visibility
- Admin-only controls
- Edit restrictions
- Rank permissions
- Transition permissions

---------------------------------------------------
PHASE 6 — PERFORMANCE & ENTERPRISE SCALING
---------------------------------------------------

Explain:
- Virtual rendering
- Infinite scroll
- Lazy loading
- Websocket updates
- Polling strategy
- Cache invalidation
- Board refresh strategy
- Reconciliation logic
- Optimistic updates
- Race condition handling
- Large dataset performance
- Thousands of issues handling
- Browser memory optimization

---------------------------------------------------
PHASE 7 — RESPONSIVE & ACCESSIBILITY
---------------------------------------------------

Explain:
- Mobile board behavior
- Tablet adaptations
- Touch drag support
- Keyboard navigation
- Screen reader support
- ARIA behavior
- Focus management
- Accessibility shortcuts

---------------------------------------------------
PHASE 8 — EDGE CASES & REAL ENTERPRISE SCENARIOS
---------------------------------------------------

Explain ALL:
- Simultaneous edits
- Deleted statuses
- Workflow mismatch
- Invalid mappings
- Permission loss mid-session
- Board filter corruption
- Missing issues
- Hidden statuses
- Re-indexing impact
- Cross-project boards
- Archived projects
- Large attachments
- Huge subtasks
- Broken rank values
- Duplicate transitions
- Network disconnect during drag
- Refresh during active drag
- Browser tab sync

---------------------------------------------------
PHASE 9 — DATABASE & BACKEND ARCHITECTURE
---------------------------------------------------

Explain:
- Core entities
- Board schema
- Filter schema
- Rank storage
- Status mapping storage
- Card metadata storage
- Cache layers
- Event architecture
- Websocket/pubsub flow
- Transition processing
- Audit logging
- History tracking

---------------------------------------------------
PHASE 10 — IMPLEMENTATION BLUEPRINT
---------------------------------------------------

Provide:
- Recommended frontend architecture
- State management strategy
- Drag-drop architecture
- Backend service design
- API structure
- DB schema suggestions
- Realtime architecture
- Performance optimization plan
- Component decomposition
- Enterprise scalability guidance

---------------------------------------------------
OUTPUT REQUIREMENTS
---------------------------------------------------

For EVERY feature/functionality include:

1. Feature Name
2. UI Elements Involved
3. Functional Behavior
4. User Interaction Flow
5. Backend Logic
6. Validation Rules
7. Edge Cases
8. Permission Implications
9. Performance Considerations
10. Jira DC Enterprise Behavior
11. Recommended Implementation Strategy

---------------------------------------------------
IMPORTANT
---------------------------------------------------

You MUST behave like:
- Atlassian internal product architect
- Jira Data Center reverse engineer
- Enterprise Kanban specialist
- Staff frontend architect
- Workflow engine designer

DO NOT:
- Skip hidden behavior
- Skip admin configuration
- Skip edge cases
- Skip ranking logic
- Skip drag-drop internals
- Skip workflow integration
- Skip enterprise-scale concerns

The goal is:
A FULL Jira Data Center Kanban Board replication blueprint with production-grade functional parity.