# Custom Field Visibility Engine (Phases 1–13)

## Architecture

Central resolver: `FieldVisibilityEngine` (`jira-migration-service`)

```
CustomFieldDefinition + FieldDefinition
    → CustomFieldContext (project / issue type)
    → FieldScreenMapping (CREATE | EDIT | VIEW | TRANSITION)
    → FieldConfigurationOverride (hidden / required per project)
    → IssueFieldValue (stored values)
    → IssueVisibleFieldsResponse (API)
```

**Rule:** Creating a custom field does **not** show it everywhere. Fields are `hidden=true` by default until mapped to screens (import provision or admin `ensure-fields`).

## APIs

| Endpoint | Purpose |
|----------|---------|
| `GET /api/fields/issues/{uuidOrKey}/visible?screen=VIEW` | Resolved visible fields + values |
| `GET /api/fields/issues/{id}/values` | Raw values (supports issue key) |
| `GET /api/fields/contexts` | List contexts |
| `GET /api/fields/search/custom` | Search by custom field value |
| `GET /api/fields/search/autocomplete` | JQL-style field key autocomplete |
| `GET/PUT /api/fields/boards/{boardId}/card-layout` | Board card field picker (Phase 7) |
| `POST /api/fields/boards/issues/visible-batch` | Batch card field values |
| `GET/PUT /api/fields/dashboard/gadgets/{gadgetKey}` | Dashboard gadget field config (Phase 9) |
| `POST /api/fields/schemes/projects/{id}/ensure-fields` | Map fields to VIEW/EDIT screens |

## UI

- **Right sidebar:** Custom fields section (always visible on issue view)
- **Details tab:** Full custom fields block + badge count on tab
- Uses issue **UUID** from loaded issue (fixes PXX-6 key-only routes)

## Database

Flyway `V19__field_visibility_engine.sql` — `field_screen_mappings`, `field_configuration_overrides`, backfill global VIEW/EDIT mappings for existing custom fields.

## Phase status

| Phase | Status |
|-------|--------|
| 1 Definitions | Implemented (registry, types, plugin registry) |
| 2 Contexts | Implemented + API list |
| 3 Screens | Implemented (`field_screen_mappings`) |
| 4 Field config | Implemented (`field_configuration_overrides`) |
| 5 Issue UI | Implemented (sidebar + Details) |
| 6 Search | Partial (`FieldSearchService`, autocomplete) |
| 7 Boards | Done — Card fields tab + batch values on cards |
| 8 Workflow | Workflow screens separate; visibility API usable on transition |
| 9 Dashboards | Done — Statistics + chart gadgets with field picker |
| 10 API/Automation | REST complete for visibility/values |
| 11 Migration | Import uses provision + screen mapping |
| 12 Scale | Indexes in V2/V5; lazy resolve per issue |
| 13 DB/Events | Schema + migration; event bus TBD |

## Restart required

After deploy: restart **migration-service** (8094) and refresh frontend. Run Flyway V19 on migration DB.
