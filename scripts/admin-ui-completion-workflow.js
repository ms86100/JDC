export const meta = {
  name: 'complete-admin-master-data-ui',
  description: 'Wire up existing admin pages and create missing master data CRUD pages',
  phases: [
    { title: 'WireExisting', detail: 'Wire CRUD mutations/modals on 4 existing read-only pages' },
    { title: 'CreateNew', detail: 'Create new admin pages for Link Types, Board Types, System Config' },
    { title: 'Routes', detail: 'Register new pages in routing and navigation' },
  ],
}

const PROJECT_ROOT = 'c:/Users/SSHABNSA/Desktop/test/JDC-main'

const COMMON_PATTERNS = [
  '## Reusable Patterns (from existing admin pages)',
  '',
  'The frontend uses these consistent CSS classes and patterns. Follow them EXACTLY:',
  '',
  '### Table Pattern',
  '<div className="admin-table-container">',
  '  <table className="admin-table">',
  '    <thead><tr><th>Name</th><th>Actions</th></tr></thead>',
  '    <tbody>{items.map(item => <tr key={item.id}>...</tr>)}</tbody>',
  '  </table>',
  '</div>',
  '',
  '### Toolbar Pattern',
  '<div className="admin-toolbar">',
  '  <div className="admin-toolbar-left"><input className="admin-search-input" /></div>',
  '  <div className="admin-toolbar-right"><button className="admin-btn-primary">Add</button></div>',
  '</div>',
  '',
  '### Modal Pattern',
  '<div className="admin-modal-overlay" onClick={onClose}>',
  '  <div className="admin-modal" onClick={e => e.stopPropagation()}>',
  '    <div className="admin-modal-header"><h3>Title</h3><button onClick={onClose}>x</button></div>',
  '    <div className="admin-modal-body">...form fields...</div>',
  '    <div className="admin-modal-footer"><button className="admin-btn-secondary">Cancel</button><button className="admin-btn-primary">Save</button></div>',
  '  </div>',
  '</div>',
  '',
  '### Form Field Pattern',
  '<div className="admin-form-group">',
  '  <label className="admin-form-label">Field Name</label>',
  '  <input className="admin-form-input" value={val} onChange={e => setVal(e.target.value)} />',
  '</div>',
  '',
  '### Stat Cards Pattern',
  '<div className="admin-stats-grid">',
  '  <div className="admin-stat-card"><div className="admin-stat-value">{count}</div><div className="admin-stat-label">Label</div></div>',
  '</div>',
  '',
  '### Alert Pattern',
  '<div className="admin-alert admin-alert-success">Success message</div>',
  '<div className="admin-alert admin-alert-error">Error message</div>',
  '',
  '### Button Classes',
  'admin-btn-primary, admin-btn-secondary, admin-btn-danger, admin-btn-sm',
  '',
  '### Data Fetching: React Query via useAdminApi.ts hooks',
  'Look at existing hooks in src/features/admin/hooks/useAdminApi.ts for the pattern.',
  'Use @tanstack/react-query: useQuery for reads, useMutation for writes, useQueryClient for cache invalidation.',
  '',
  '### API calls: Use axiosClient',
  'import axiosClient from "../../../api/axiosClient" (or appropriate relative path)',
  'axiosClient.get/post/put/delete returns response.data',
  '',
  '### CSS files to import:',
  'The shared admin styles are in src/features/admin/styles/admin-shared.css',
  'Some pages also use src/features/admin/pages/AdminIssueConfig.css',
  '',
  '### Icons: lucide-react',
  'import { Plus, Edit2, Trash2, Search, X, ChevronDown, Settings, ... } from "lucide-react"',
].join('\n')

// =============================================
// PHASE 1: WIRE EXISTING PAGES
// =============================================
phase('WireExisting')
log('Wiring CRUD mutations and modals on 4 existing read-only pages...')

const wireResults = await parallel([
  // --- Wire StatusesPage ---
  () => agent(`You are a senior React developer. Wire full CRUD functionality on the existing StatusesPage.

PROJECT ROOT: ${PROJECT_ROOT}
TARGET FILE: Find the StatusesPage.tsx (likely in src/features/admin/pages/ inside the frontend directory — check both avionics-systems-frontend/ and avionics-systems-frontend/)

## Current State
- The page already displays a read-only list of statuses fetched from the API
- "Add Status" and "Edit" buttons EXIST in the JSX but have NO onClick handlers
- Uses useStatuses() hook for read-only fetching
- No create/update/delete mutations exist

## What to Add

### 1. Create/Edit Modal
Add a modal component (inline or separate) for creating and editing statuses with fields:
- Name (text input, required)
- Key (text input, auto-generated from name, editable)
- Category (select: TODO, IN_PROGRESS, DONE)
- Color (color picker input or text input for hex)
- Icon (text input)
- Description (textarea)
- Sort Order (number input)

### 2. API Mutations
Add to useAdminApi.ts (or inline):
- useCreateStatus: POST /api/admin/master-data/statuses
- useUpdateStatus: PUT /api/admin/master-data/statuses/{id}
- useDeleteStatus: DELETE /api/admin/master-data/statuses/{id}

### 3. Wire the Buttons
- "Add Status" button → opens Create modal
- "Edit" button per row → opens Edit modal pre-filled with status data
- Add "Delete" button per row → confirmation dialog then DELETE call
- On success → invalidate statuses query, show success alert
- On error → show error alert

### 4. Color Preview
Show a small color swatch next to each status in the table (the color field from the status object).

### 5. Delete Confirmation
Show a simple confirmation: "Are you sure you want to delete status '{name}'? This cannot be undone."
Prevent deletion of system statuses (is_system = true).

${COMMON_PATTERNS}

Read the existing StatusesPage.tsx FIRST to understand its current structure, then modify it.
Also read IssueTypesPage.tsx or RolesPage.tsx as examples of fully working CRUD pages.
Follow the exact same patterns.`, {
    label: 'wire:statuses',
    phase: 'WireExisting',
    effort: 'high'
  }),

  // --- Wire PrioritiesPage ---
  () => agent(`You are a senior React developer. Wire full CRUD functionality on the existing PrioritiesPage.

PROJECT ROOT: ${PROJECT_ROOT}
TARGET FILE: Find PrioritiesPage.tsx in the frontend admin pages directory (check both avionics-systems-frontend/ and avionics-systems-frontend/)

## Current State
- The page already displays a read-only list of priorities fetched from the API
- "Add Priority", "Edit", "Delete" buttons EXIST but have NO onClick handlers
- Uses usePriorities() hook for read-only fetching

## What to Add

### 1. Create/Edit Modal
Fields:
- Name (text, required)
- Key (text, auto-generated from name)
- Description (textarea)
- Color (color input for hex — priorities use colors like #FF5630, #FFAB00, #0065FF)
- Icon URL (text input)
- Sort Order (number — determines priority ranking)
- Is Default (checkbox — only one priority can be default)

### 2. API Mutations
- useCreatePriority: POST /api/admin/master-data/priorities
- useUpdatePriority: PUT /api/admin/master-data/priorities/{id}
- useDeletePriority: DELETE /api/admin/master-data/priorities/{id}

### 3. Wire Buttons
- "Add Priority" → Create modal
- "Edit" per row → Edit modal
- "Delete" per row → confirmation then DELETE
- Color swatch display per row
- Drag-to-reorder or sort order number editing

### 4. Visual Priority Indicators
Show the color as a colored dot or bar next to each priority name in the table.

${COMMON_PATTERNS}

Read existing PrioritiesPage.tsx first, then read IssueTypesPage.tsx as a pattern reference.`, {
    label: 'wire:priorities',
    phase: 'WireExisting',
    effort: 'high'
  }),

  // --- Wire ResolutionsPage ---
  () => agent(`You are a senior React developer. Wire full CRUD functionality on the existing ResolutionsPage.

PROJECT ROOT: ${PROJECT_ROOT}
TARGET FILE: Find ResolutionsPage.tsx in the frontend admin pages directory (check both avionics-systems-frontend/ and avionics-systems-frontend/)

## Current State
- The page lists resolutions from the API (read-only)
- "Add resolution" button exists but is DISABLED
- Uses resolutionApi.getAll() from issueApi.ts
- No create/update/delete API methods exist in the API file

## What to Add

### 1. Add API Methods
In the appropriate API file (issueApi.ts or create a new one), add:
- resolutionApi.create(data): POST /api/admin/master-data/resolutions
- resolutionApi.update(id, data): PUT /api/admin/master-data/resolutions/{id}
- resolutionApi.delete(id): DELETE /api/admin/master-data/resolutions/{id}

### 2. Create/Edit Modal
Fields:
- Name (text, required) — e.g., "Fixed", "Won't Fix", "Duplicate"
- Key (text, auto-generated)
- Description (textarea)
- Sort Order (number)
- Is Default (checkbox)

### 3. Wire the Buttons
- Enable the "Add resolution" button, wire to Create modal
- Add Edit button per row → Edit modal
- Add Delete button per row → confirmation dialog
- Prevent deletion of default resolution

### 4. React Query Hooks
Create useResolutions, useCreateResolution, useUpdateResolution, useDeleteResolution hooks using the same pattern as other admin hooks.

${COMMON_PATTERNS}

Read existing ResolutionsPage.tsx first, then read RolesPage.tsx as a pattern reference for full CRUD.`, {
    label: 'wire:resolutions',
    phase: 'WireExisting',
    effort: 'high'
  }),

  // --- Wire ScreensPage ---
  () => agent(`You are a senior React developer. Wire full CRUD functionality on the existing ScreensPage.

PROJECT ROOT: ${PROJECT_ROOT}
TARGET FILE: Find ScreensPage.tsx in the frontend admin pages directory (check both avionics-systems-frontend/ and avionics-systems-frontend/)

## Current State
- Lists screens from API (read-only)
- "Add Screen", "Edit", "Configure Tabs" buttons exist but are NOT wired
- Screen Schemes tab has hardcoded HTML
- Uses useScreens() hook (read-only)

## What to Add

### 1. Create/Edit Modal for Screens
Fields:
- Name (text, required)
- Description (textarea)

### 2. API Mutations
- useCreateScreen: POST /api/admin/screens
- useUpdateScreen: PUT /api/admin/screens/{id}
- useDeleteScreen: DELETE /api/admin/screens/{id}

### 3. Wire the Buttons
- "Add Screen" → Create modal
- "Edit" per row → Edit modal
- "Delete" per row → confirmation
- "Configure Tabs" per row → navigate to screen tab configuration (or open a sub-view)

### 4. Screen Schemes Tab
Replace the hardcoded HTML with real data fetching:
- Fetch screen schemes from /api/admin/screen-schemes
- Show as table with name, description, screens count
- Add CRUD for screen schemes too if possible, or at minimum wire the list

${COMMON_PATTERNS}

Read existing ScreensPage.tsx first, then IssueTypesPage.tsx for CRUD pattern.`, {
    label: 'wire:screens',
    phase: 'WireExisting',
    effort: 'high'
  }),
])

log('Wired ' + wireResults.filter(Boolean).length + '/4 existing pages')

// =============================================
// PHASE 2: CREATE NEW PAGES
// =============================================
phase('CreateNew')
log('Creating new admin pages for missing master data entities...')

const newPageResults = await parallel([
  // --- LinkTypesPage ---
  () => agent(`You are a senior React developer. Create a new admin page for managing Issue Link Types.

PROJECT ROOT: ${PROJECT_ROOT}
FRONTEND DIR: Find the frontend directory (check both avionics-systems-frontend/ and avionics-systems-frontend/)
CREATE IN: src/features/admin/pages/LinkTypesPage.tsx

## Page Requirements

### Data Model (from backend API)
Each link type has:
- id (UUID)
- linkKey (string) — e.g., "BLOCKS", "CLONES", "DUPLICATES", "RELATES"
- outwardName (string) — e.g., "blocks", "clones", "duplicates", "relates to"
- inwardName (string) — e.g., "is blocked by", "is cloned by", "is duplicated by", "relates to"
- description (string, optional)
- isSystem (boolean) — system link types cannot be deleted
- isActive (boolean)
- sortOrder (integer)

### API Endpoints
- GET /api/admin/master-data/link-types — list all
- POST /api/admin/master-data/link-types — create
- PUT /api/admin/master-data/link-types/{id} — update
- DELETE /api/admin/master-data/link-types/{id} — delete (soft deactivate)

### Page Layout
1. **Stats bar**: Total link types, Active, System
2. **Toolbar**: Search input + "Add Link Type" button
3. **Table columns**: Outward Name, Inward Name, Key, System (badge), Active (badge), Actions (Edit, Delete)
4. **Create/Edit Modal** with fields:
   - Link Key (text, required, uppercase)
   - Outward Name (text, required) — "blocks"
   - Inward Name (text, required) — "is blocked by"
   - Description (textarea)
   - Sort Order (number)
5. **Delete confirmation** — prevent deletion of system link types

### React Query Hooks
Create in useAdminApi.ts or inline:
- useLinkTypes: GET query
- useCreateLinkType: POST mutation
- useUpdateLinkType: PUT mutation
- useDeleteLinkType: DELETE mutation

${COMMON_PATTERNS}

Read an existing CRUD page like IssueTypesPage.tsx or RolesPage.tsx as a template. Copy the exact same structure, CSS classes, and patterns. Create a complete, functional page.`, {
    label: 'create:link-types',
    phase: 'CreateNew',
    effort: 'high'
  }),

  // --- BoardTypesPage ---
  () => agent(`You are a senior React developer. Create a new admin page for managing Board Types and their default column templates.

PROJECT ROOT: ${PROJECT_ROOT}
FRONTEND DIR: Find the frontend directory (check both avionics-systems-frontend/ and avionics-systems-frontend/)
CREATE IN: src/features/admin/pages/BoardTypesPage.tsx

## Page Requirements

### Data Model
Board Type:
- id (UUID)
- typeKey (string) — "SCRUM", "KANBAN"
- displayName (string)
- description (string)
- isActive (boolean)

Board Column Template (child of Board Type):
- id (UUID)
- boardTypeId (UUID, FK)
- columnName (string) — "To Do", "In Progress", "Done"
- statusCategory (string) — "TODO", "IN_PROGRESS", "DONE"
- color (string, hex)
- wipLimit (integer, nullable)
- sortOrder (integer)

### API Endpoints
- GET /api/admin/master-data/board-types — list all board types with column templates
- POST /api/admin/master-data/board-types — create board type
- PUT /api/admin/master-data/board-types/{id} — update board type
- DELETE /api/admin/master-data/board-types/{id} — delete board type

### Page Layout
1. **Two-panel layout**: Board types list on left, selected board type's column templates on right
2. **Board Types list**: Card-style list showing type key, name, column count
3. **Column Templates panel** (when a board type is selected):
   - Table: Column Name, Status Category, Color (swatch), WIP Limit, Sort Order
   - Add Column button
   - Edit/Delete per column
   - Drag-to-reorder (optional, or sort order number)
4. **Create/Edit Board Type Modal**: Type Key, Display Name, Description
5. **Create/Edit Column Modal**: Column Name, Status Category (select: TODO/IN_PROGRESS/DONE), Color (color picker), WIP Limit (number), Sort Order (number)

${COMMON_PATTERNS}

Read an existing admin page for patterns. Make the column template section visually show the colors (colored bars or swatches next to each column name). This page should feel like configuring a Kanban/Scrum board layout.`, {
    label: 'create:board-types',
    phase: 'CreateNew',
    effort: 'high'
  }),

  // --- SystemConfigPage ---
  () => agent(`You are a senior React developer. Create a new admin page for managing System Configuration (key-value settings).

PROJECT ROOT: ${PROJECT_ROOT}
FRONTEND DIR: Find the frontend directory (check both avionics-systems-frontend/ and avionics-systems-frontend/)
CREATE IN: src/features/admin/pages/SystemConfigPage.tsx

## Page Requirements

### Data Model
System Configuration entry:
- id (UUID)
- configKey (string) — e.g., "issue.default.status_key", "board.default.type"
- configValue (string) — the value
- valueType (string) — "STRING", "INTEGER", "BOOLEAN", "JSON"
- category (string) — "ISSUE", "PROJECT", "BOARD", "SPRINT", "TEST", "QUALITY", "USER", "LDAP", "WORKFLOW", "RELEASE", "GOAL", "ATTACHMENT"
- description (string) — human-readable description
- isEditable (boolean)

### API Endpoints
- GET /api/admin/config — all configuration entries
- GET /api/admin/config/category/{category} — filter by category
- GET /api/admin/config/{key} — single entry
- PUT /api/admin/config/{key} — update value

### Page Layout
1. **Category tabs or sidebar filter**: Show all categories as tabs across the top or as a left sidebar filter
2. **Configuration table** per category:
   - Key (monospace font)
   - Value (editable inline or via modal)
   - Type badge (STRING/INTEGER/BOOLEAN)
   - Description (muted text below key)
   - Edit button (pencil icon)
3. **Edit Modal/Inline Edit**:
   - For STRING: text input
   - For INTEGER: number input
   - For BOOLEAN: toggle switch
   - For JSON: textarea with monospace font
   - Show description and current value
   - Save/Cancel buttons
4. **Search**: Filter across all categories by key or description
5. **No Delete** — configuration entries are system-managed, only values are editable
6. **Non-editable entries** (isEditable=false) should show a lock icon and greyed-out edit button

### Visual Design
- Group entries visually by category with section headers
- Use monospace font for keys and values
- Show a "Modified" badge for recently updated entries
- Color-code categories (optional)

${COMMON_PATTERNS}

Read the existing SystemSettingsPage.tsx to understand what's already there — you may want to enhance it rather than create a new page. If SystemSettingsPage is display-only, convert it to support editing. Otherwise create SystemConfigPage as a new page.

This is a critical page — it's how admins change all the default values that were previously hardcoded (default status, default priority, quality thresholds, password length, etc.).`, {
    label: 'create:system-config',
    phase: 'CreateNew',
    effort: 'high'
  }),

  // --- QuickFilterPresetsPage ---
  () => agent(`You are a senior React developer. Create a new admin page for managing Quick Filter Presets (system-level saved filters for agile boards).

PROJECT ROOT: ${PROJECT_ROOT}
FRONTEND DIR: Find the frontend directory (check both avionics-systems-frontend/ and avionics-systems-frontend/)
CREATE IN: src/features/admin/pages/QuickFilterPresetsPage.tsx

## Page Requirements

### Data Model
Quick Filter Preset:
- id (UUID)
- filterName (string) — e.g., "Assigned to Me", "High Priority", "Blocked"
- jqlQuery (string) — e.g., "assignee = currentUser()", "priority in (Highest, High)"
- icon (string) — emoji or icon name
- sortOrder (integer)
- isSystem (boolean)
- isActive (boolean)

### API Endpoints
- GET /api/admin/master-data/quick-filters — list all
- POST /api/admin/master-data/quick-filters — create
- PUT /api/admin/master-data/quick-filters/{id} — update
- DELETE /api/admin/master-data/quick-filters/{id} — delete

### Page Layout
1. **Stats**: Total filters, Active, System
2. **Toolbar**: Search + "Add Quick Filter" button
3. **Table/Card list**: Icon, Filter Name, JQL Query (monospace), System badge, Active badge, Actions
4. **Create/Edit Modal**:
   - Filter Name (text, required)
   - JQL Query (textarea with monospace font, required) — this is the key field
   - Icon (text input or emoji picker)
   - Sort Order (number)
   - Active (checkbox)
5. **JQL preview/help**: Show a small info box explaining JQL syntax or link to JQL documentation
6. **System filters** cannot be deleted, only deactivated

${COMMON_PATTERNS}

Read RolesPage.tsx or IssueTypesPage.tsx as pattern reference. Keep it simple and functional.`, {
    label: 'create:quick-filters',
    phase: 'CreateNew',
    effort: 'high'
  }),

  // --- NotificationEventsPage ---
  () => agent(`You are a senior React developer. Create a new admin page for managing Notification Events.

PROJECT ROOT: ${PROJECT_ROOT}
FRONTEND DIR: Find the frontend directory (check both avionics-systems-frontend/ and avionics-systems-frontend/)
CREATE IN: src/features/admin/pages/NotificationEventsPage.tsx

## Page Requirements

### Data Model
Notification Event:
- id (UUID)
- eventKey (string) — e.g., "ISSUE_CREATED", "COMMENT_ADDED", "STATUS_CHANGED"
- displayName (string) — e.g., "Issue Created", "Comment Added"
- description (string)
- category (string) — "Issue", "Comment", "Status", "Sprint", "Project"
- isSystem (boolean)
- isActive (boolean)

### API Endpoints
- GET /api/admin/master-data/notification-events — list all
- POST /api/admin/master-data/notification-events — create
- PUT /api/admin/master-data/notification-events/{id} — update
- DELETE /api/admin/master-data/notification-events/{id} — delete

### Page Layout
1. **Category filter tabs**: Issue | Comment | Status | Sprint | Project | All
2. **Table**: Event Key, Display Name, Category (badge), System (badge), Active (toggle), Actions
3. **Create/Edit Modal**: Event Key (uppercase), Display Name, Description, Category (select), Active (checkbox)
4. **System events** cannot be deleted

${COMMON_PATTERNS}

Read IssueTypesPage.tsx as pattern reference. Group events by category visually.`, {
    label: 'create:notification-events',
    phase: 'CreateNew',
    effort: 'high'
  }),
])

log('Created ' + newPageResults.filter(Boolean).length + '/5 new pages')

// =============================================
// PHASE 3: REGISTER ROUTES AND NAVIGATION
// =============================================
phase('Routes')
log('Registering new pages in routing and navigation...')

await agent(`You are a senior React developer. Register all the newly created admin pages in the routing and navigation.

PROJECT ROOT: ${PROJECT_ROOT}
FRONTEND DIR: Find the frontend directory (check both avionics-systems-frontend/ and avionics-systems-frontend/)

## What was created/updated:
1. StatusesPage — UPDATED with full CRUD (already routed)
2. PrioritiesPage — UPDATED with full CRUD (already routed)
3. ResolutionsPage — UPDATED with full CRUD (already routed)
4. ScreensPage — UPDATED with full CRUD (already routed)
5. LinkTypesPage — NEW, needs route + nav entry
6. BoardTypesPage — NEW, needs route + nav entry
7. SystemConfigPage — NEW, needs route + nav entry
8. QuickFilterPresetsPage — NEW, needs route + nav entry
9. NotificationEventsPage — NEW, needs route + nav entry

## Files to Update

### 1. AdminRoutes.tsx
Location: src/features/admin/routes/AdminRoutes.tsx

Add lazy imports and routes for the 5 new pages:
- /admin/link-types → LinkTypesPage
- /admin/board-types → BoardTypesPage
- /admin/system-config → SystemConfigPage
- /admin/quick-filters → QuickFilterPresetsPage
- /admin/notification-events → NotificationEventsPage

Read the existing AdminRoutes.tsx to follow the exact pattern (React.lazy, Suspense, etc.).

### 2. Admin Navigation Sidebar
Location: src/components/layout/adminCategories.ts (or AdminNavSidebar.tsx)

Add navigation entries for the new pages under appropriate categories:
- Link Types → under "Issues" category (alongside Issue Types, etc.)
- Board Types → under "Projects" or "System" category
- System Configuration → under "System" category
- Quick Filter Presets → under "Projects" category (alongside boards)
- Notification Events → under "System" category (alongside notification schemes)

Read the existing adminCategories.ts to understand the category structure and add entries that fit naturally.

### 3. Verify existing routes still work
Make sure the 4 updated pages (Statuses, Priorities, Resolutions, Screens) still have their routes intact — they should since we only modified their content, not their routing.

## Important
- Use the exact same lazy loading pattern as existing routes
- Use appropriate icons from lucide-react for nav entries
- Ensure the URL paths are kebab-case and consistent with existing patterns
- Test that the imports resolve correctly (check file paths)`, {
  label: 'routes:register',
  phase: 'Routes',
  effort: 'high'
})

log('All routes and navigation registered')

return {
  wired: wireResults.filter(Boolean).length,
  created: newPageResults.filter(Boolean).length,
  total: 9
}
