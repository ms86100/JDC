# Workflow transition pipeline (17 steps)

Canonical entry: `POST /api/workflows/transitions/execute` → `WorkflowExecutionEngine.execute()`

| Step | Phase 2 requirement | Implementation |
|------|---------------------|----------------|
| 1 | Load project | `WorkflowContextResolver` → `integrationClient.fetchProject` / issue `projectId` |
| 2 | Load workflow scheme | `WorkflowSchemeService` via `resolveWorkflow(projectId, issueTypeId)` |
| 3 | Resolve issue type workflow | `WorkflowContextResolver.resolve` |
| 4 | Validate current status | `validateCurrentStatus()` |
| 5 | Validate allowed transition | Transition row `fromStatusId` match |
| 6 | Validate conditions | `ConditionEvaluator.evaluateAll` |
| 7 | Validate permissions | `checkPermissions` + `checkTransitionPermission` |
| 8 | Validate validators | `ValidatorExecutor.validate` |
| 9 | Load transition screen | `TransitionScreenService.getScreenFields` (on available transitions) |
| 10 | Validate screen fields | `TransitionScreenService.validateScreenInputFields` |
| 11 | Execute transition | `PostFunctionPipeline.execute` |
| 12 | Execute post-functions | `PostFunctionExecutor` essential + configured chain |
| 13 | Generate notifications | `SEND_EMAIL` / notification outbox (post-function) |
| 14 | Generate audit logs | `WorkflowTransitionHistory` + `issue_transition_history` |
| 15 | Emit events | `WorkflowEventPublisher.publishIssueTransitioned` |
| 16 | Update issue status | `PostFunctionExecutor.applyStatus` → issue internal PATCH |
| 17 | Reindex issue | `PostFunctionExecutor.reindexIssue` |

Issue-side history: `GET /api/issues/{id}/transitions/history`
