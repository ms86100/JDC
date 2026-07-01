# Workflow Service - Root Cause Analysis & Fixes

## Date
2026-06-12

## Issue Summary
Frontend console showed multiple errors when accessing workflow-related pages:
- 500 errors on `GET /api/workflows/{id}/transitions-with-details`
- 404 errors on `POST /api/workflow-schemes/workflows/{id}/layout/auto`
- 404 errors on `GET /plans` (plan service was completely down)

## Root Causes

### 1. Workflow Service 500 Error on transitions-with-details
**Root Cause**: Database schema mismatch between Flyway V3 and V9 migrations.

- V3 created `jira_workflow.workflow_conditions` with columns: `condition_config`, `condition_order`, `is_required`
- V9 attempted to recreate the same table with new columns: `condition_data`, `field_name`, `operator`, `value`, `negate`, `sequence`
- V9 used `CREATE TABLE IF NOT EXISTS` which was a no-op since V3 already created the table
- The JPA entity `WorkflowCondition` expects V9 columns (`condition_data`, etc.)
- When `transitions-with-details` was called, it triggered a `findByTransitionIdOrderBySequenceAsc` query that referenced `condition_data` which didn't exist

**Error log evidence**:
```
ERROR: column wc1_0.condition_data does not exist
  Position: 17
```

**Fix**: Created new V14 migration `V14__fix_workflow_component_columns.sql` that:
- Adds missing columns to `workflow_conditions`, `workflow_validators`, `workflow_post_functions`
- Migrates data from old V3 columns (`condition_config`) to V9 columns (`condition_data`)
- Uses `IF NOT EXISTS` for safety on re-runs
- Seeds `condition_templates` and `validator_templates` for UI dropdowns

### 2. Plan Service 404 (Service Not Running)
**Root Cause**: Plan service crashed at startup on Flyway V18 migration due to column name typos:

- V18 referenced `action_type` but the table `sprint_audit_log` column is `event_type`
- V18 referenced `board_config_id` but the table `board_permissions` column is `board_id`
- Result: Flyway exception, service fails to start, `/api/plans` returns 404 (no service running)

**Error log evidence**:
```
ERROR: column "action_type" does not exist
```

**Additional V21 issue**: After V18 was fixed, V21 (`add_is_active_to_plan_releases`) failed with `column "is_active" already exists` because the column was already present from a previous run.

**Fix**:
- Corrected V18 column names to match actual schema (`event_type`, `board_id`)
- Made V21, V22, V23 idempotent by adding `IF NOT EXISTS` to all `ALTER TABLE ADD COLUMN` and `CREATE INDEX` statements

### 3. Workflow layout/auto 500 (FK Constraint Violation)
**Root Cause**: Service-level bug in `WorkflowLayoutService.autoLayout()` and `deleteLayout()`:

- Code deleted `workflow_layout_nodes` BEFORE `workflow_layout_edges`
- Foreign key constraint: `workflow_layout_edges.from_node_id` and `to_node_id` reference `workflow_layout_nodes.id`
- Result: constraint violation when deleting nodes that are still referenced by edges

**Error log evidence**:
```
ERROR: update or delete on table "workflow_layout_nodes" violates foreign key constraint "workflow_layout_edges_from_node_id_fkey"
  Detail: Key (id)=(...) is still referenced from table "workflow_layout_edges"
```

**Fix**: Reordered deletion in `WorkflowLayoutService.java`:
- Delete edges first, then nodes (in both `autoLayout` and `deleteLayout` methods)

## Files Modified

### Created
- `jira-workflow-service/src/main/resources/db/migration/V14__fix_workflow_component_columns.sql` (NEW)

### Modified
- `jira-plan-service/src/main/resources/db/migration/V18__add_extra_indexes.sql` (column name fixes)
- `jira-plan-service/src/main/resources/db/migration/V21__add_is_active_to_plan_releases.sql` (IF NOT EXISTS)
- `jira-plan-service/src/main/resources/db/migration/V22__add_is_active_to_plan_items.sql` (IF NOT EXISTS)
- `jira-plan-service/src/main/resources/db/migration/V23__add_optimistic_locking_version.sql` (IF NOT EXISTS)
- `jira-workflow-service/src/main/java/com/jira/workflow/service/WorkflowLayoutService.java` (delete order)

## Verification

All previously failing endpoints now return HTTP 200:

| Endpoint | Before | After |
|----------|--------|-------|
| `GET /api/workflows/{id}/transitions-with-details` | 500 | 200 |
| `POST /api/workflow-schemes/workflows/{id}/layout/auto` | 500 | 200 |
| `GET /api/plans` | 404 | 200 |
| `GET /api/plans/programs` | 404 | 200 |
| `GET /api/workflow-schemes/workflows/{id}/layout` | 200 | 200 |

## Migration Steps for Production

1. Apply V14 to `jira_workflow` schema
2. Apply V18-V24 to `jira_plan` schema (idempotent now)
3. Rebuild and restart:
   - `jira-workflow-service`
   - `jira-plan-service`
4. Verify endpoints with the e2e test script: `test-workflow-e2e.sh`