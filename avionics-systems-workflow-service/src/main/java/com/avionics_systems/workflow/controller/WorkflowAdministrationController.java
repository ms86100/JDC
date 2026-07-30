package com.avionics_systems.workflow.controller;

import com.avionics_systems.workflow.service.WorkflowAdministrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/workflows")
@RequiredArgsConstructor
@Slf4j
public class WorkflowAdministrationController {

    private final WorkflowAdministrationService workflowAdminService;

    private UUID parseUUID(String id) {
        return UUID.fromString(id);
    }

    // ==================== Workflow CRUD ====================

    @PostMapping
    public ResponseEntity<?> createWorkflow(@RequestBody Map<String, Object> request) {
        try {
            var workflow = workflowAdminService.createWorkflow(request);
            return ResponseEntity.ok(workflow);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> listWorkflows(@RequestParam(required = false) String status,
                                           @RequestParam(required = false) String name) {
        var workflows = workflowAdminService.listWorkflows(status, name);
        return ResponseEntity.ok(workflows);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getWorkflow(@PathVariable String id) {
        try {
            var workflow = workflowAdminService.getWorkflow(parseUUID(id));
            return ResponseEntity.ok(workflow);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateWorkflow(@PathVariable String id,
                                            @RequestBody Map<String, Object> updates) {
        try {
            var workflow = workflowAdminService.updateWorkflow(parseUUID(id), updates);
            return ResponseEntity.ok(workflow);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteWorkflow(@PathVariable String id) {
        try {
            workflowAdminService.deleteWorkflow(parseUUID(id));
            return ResponseEntity.ok(Map.of("message", "Workflow deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/clone")
    public ResponseEntity<?> cloneWorkflow(@PathVariable String id,
                                            @RequestBody Map<String, Object> request) {
        try {
            String newName = (String) request.get("newName");
            var workflow = workflowAdminService.cloneWorkflow(parseUUID(id), newName);
            return ResponseEntity.ok(workflow);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<?> publishWorkflow(@PathVariable String id) {
        try {
            var workflow = workflowAdminService.publishWorkflow(parseUUID(id));
            return ResponseEntity.ok(workflow);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/draft")
    public ResponseEntity<?> createDraft(@PathVariable String id) {
        try {
            var workflow = workflowAdminService.createDraft(parseUUID(id));
            return ResponseEntity.ok(workflow);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/export")
    public ResponseEntity<?> exportWorkflow(@PathVariable String id) {
        try {
            var exported = workflowAdminService.exportWorkflow(parseUUID(id));
            return ResponseEntity.ok(exported);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/import")
    public ResponseEntity<?> importWorkflow(@RequestBody Map<String, Object> request) {
        try {
            var workflow = workflowAdminService.importWorkflow(request);
            return ResponseEntity.ok(workflow);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== Status Management ====================

    @PostMapping("/{id}/statuses")
    public ResponseEntity<?> addStatus(@PathVariable String id,
                                       @RequestBody Map<String, Object> statusData) {
        try {
            var status = workflowAdminService.addStatus(parseUUID(id), statusData);
            return ResponseEntity.ok(status);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/statuses/{statusId}")
    public ResponseEntity<?> updateStatus(@PathVariable String id,
                                          @PathVariable String statusId,
                                          @RequestBody Map<String, Object> updates) {
        try {
            var status = workflowAdminService.updateStatus(parseUUID(id), parseUUID(statusId), updates);
            return ResponseEntity.ok(status);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/statuses/{statusId}")
    public ResponseEntity<?> removeStatus(@PathVariable String id,
                                          @PathVariable String statusId) {
        try {
            workflowAdminService.removeStatus(parseUUID(id), parseUUID(statusId));
            return ResponseEntity.ok(Map.of("message", "Status removed successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/statuses/reorder")
    public ResponseEntity<?> reorderStatuses(@PathVariable String id,
                                             @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> statusIdsStr = (List<String>) request.get("statusIds");
            List<UUID> statusIds = statusIdsStr.stream().map(this::parseUUID).toList();
            workflowAdminService.reorderStatuses(parseUUID(id), statusIds);
            return ResponseEntity.ok(Map.of("message", "Statuses reordered successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== Transition Management ====================

    @PostMapping("/{id}/transitions")
    public ResponseEntity<?> addTransition(@PathVariable String id,
                                          @RequestBody Map<String, Object> transitionData) {
        try {
            var transition = workflowAdminService.addTransition(parseUUID(id), transitionData);
            return ResponseEntity.ok(transition);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/transitions/{transitionId}")
    public ResponseEntity<?> updateTransition(@PathVariable String id,
                                              @PathVariable String transitionId,
                                              @RequestBody Map<String, Object> updates) {
        try {
            var transition = workflowAdminService.updateTransition(parseUUID(id), parseUUID(transitionId), updates);
            return ResponseEntity.ok(transition);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/transitions/{transitionId}")
    public ResponseEntity<?> deleteTransition(@PathVariable String id,
                                               @PathVariable String transitionId) {
        try {
            workflowAdminService.deleteTransition(parseUUID(id), parseUUID(transitionId));
            return ResponseEntity.ok(Map.of("message", "Transition deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/transitions/reorder")
    public ResponseEntity<?> reorderTransitions(@PathVariable String id,
                                                 @RequestBody Map<String, Object> request) {
        try {
            UUID fromStatusId = parseUUID((String) request.get("fromStatusId"));
            @SuppressWarnings("unchecked")
            List<String> toStatusIdsStr = (List<String>) request.get("toStatusIds");
            List<UUID> toStatusIds = toStatusIdsStr.stream().map(this::parseUUID).toList();
            workflowAdminService.reorderTransitions(parseUUID(id), fromStatusId, toStatusIds);
            return ResponseEntity.ok(Map.of("message", "Transitions reordered successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== Condition Management ====================

    @PostMapping("/transitions/{transitionId}/conditions")
    public ResponseEntity<?> addCondition(@PathVariable String transitionId,
                                          @RequestBody Map<String, Object> conditionData) {
        try {
            var condition = workflowAdminService.addCondition(parseUUID(transitionId), conditionData);
            return ResponseEntity.ok(condition);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/conditions/{conditionId}")
    public ResponseEntity<?> removeCondition(@PathVariable String conditionId) {
        try {
            workflowAdminService.removeCondition(null, parseUUID(conditionId));
            return ResponseEntity.ok(Map.of("message", "Condition removed successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== Validator Management ====================

    @PostMapping("/transitions/{transitionId}/validators")
    public ResponseEntity<?> addValidator(@PathVariable String transitionId,
                                          @RequestBody Map<String, Object> validatorData) {
        try {
            var validator = workflowAdminService.addValidator(parseUUID(transitionId), validatorData);
            return ResponseEntity.ok(validator);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/validators/{validatorId}")
    public ResponseEntity<?> removeValidator(@PathVariable String validatorId) {
        try {
            workflowAdminService.removeValidator(null, parseUUID(validatorId));
            return ResponseEntity.ok(Map.of("message", "Validator removed successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== Post Function Management ====================

    @PostMapping("/transitions/{transitionId}/post-functions")
    public ResponseEntity<?> addPostFunction(@PathVariable String transitionId,
                                              @RequestBody Map<String, Object> functionData) {
        try {
            var function = workflowAdminService.addPostFunction(parseUUID(transitionId), functionData);
            return ResponseEntity.ok(function);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/post-functions/{functionId}")
    public ResponseEntity<?> removePostFunction(@PathVariable String functionId) {
        try {
            workflowAdminService.removePostFunction(null, parseUUID(functionId));
            return ResponseEntity.ok(Map.of("message", "Post function removed successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== Version Management ====================

    @GetMapping("/{id}/versions")
    public ResponseEntity<?> getVersions(@PathVariable String id) {
        try {
            var versions = workflowAdminService.getWorkflowVersions(parseUUID(id));
            return ResponseEntity.ok(versions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/versions/{versionNumber}")
    public ResponseEntity<?> getVersion(@PathVariable String id,
                                       @PathVariable int versionNumber) {
        try {
            var version = workflowAdminService.getWorkflowVersion(parseUUID(id), versionNumber);
            return ResponseEntity.ok(version);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/versions/{versionNumber}/revert")
    public ResponseEntity<?> revertToVersion(@PathVariable String id,
                                              @PathVariable int versionNumber) {
        try {
            var workflow = workflowAdminService.revertToVersion(parseUUID(id), versionNumber);
            return ResponseEntity.ok(workflow);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/compare")
    public ResponseEntity<?> compareVersions(@PathVariable String id,
                                             @RequestParam int v1,
                                             @RequestParam int v2) {
        try {
            var comparison = workflowAdminService.compareVersions(parseUUID(id), v1, v2);
            return ResponseEntity.ok(comparison);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== Workflow Scheme Management ====================

    @PostMapping("/schemes")
    public ResponseEntity<?> createScheme(@RequestBody Map<String, Object> data) {
        try {
            var scheme = workflowAdminService.createScheme(data);
            return ResponseEntity.ok(scheme);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/schemes")
    public ResponseEntity<?> listSchemes() {
        var schemes = workflowAdminService.listSchemes();
        return ResponseEntity.ok(schemes);
    }

    @GetMapping("/schemes/{id}")
    public ResponseEntity<?> getScheme(@PathVariable String id) {
        try {
            var scheme = workflowAdminService.getScheme(parseUUID(id));
            return ResponseEntity.ok(scheme);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/schemes/{id}")
    public ResponseEntity<?> updateScheme(@PathVariable String id,
                                          @RequestBody Map<String, Object> updates) {
        try {
            var scheme = workflowAdminService.updateScheme(parseUUID(id), updates);
            return ResponseEntity.ok(scheme);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/schemes/{id}")
    public ResponseEntity<?> deleteScheme(@PathVariable String id) {
        try {
            workflowAdminService.deleteScheme(parseUUID(id));
            return ResponseEntity.ok(Map.of("message", "Scheme deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/schemes/{id}/mappings")
    public ResponseEntity<?> assignWorkflowToScheme(@PathVariable String id,
                                                   @RequestBody Map<String, Object> mapping) {
        try {
            var result = workflowAdminService.assignWorkflowToScheme(parseUUID(id), mapping);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/schemes/{id}/mappings/{mappingId}")
    public ResponseEntity<?> removeWorkflowFromScheme(@PathVariable String id,
                                                     @PathVariable String mappingId) {
        try {
            workflowAdminService.removeWorkflowFromScheme(parseUUID(id), parseUUID(mappingId));
            return ResponseEntity.ok(Map.of("message", "Mapping removed successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/schemes/{id}/default")
    public ResponseEntity<?> setDefaultWorkflow(@PathVariable String id,
                                                @RequestBody Map<String, Object> request) {
        try {
            String workflowId = (String) request.get("workflowId");
            var scheme = workflowAdminService.setDefaultWorkflow(parseUUID(id), parseUUID(workflowId));
            return ResponseEntity.ok(scheme);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== Screen Management ====================

    @PostMapping("/screens")
    public ResponseEntity<?> createScreen(@RequestBody Map<String, Object> data) {
        try {
            var screen = workflowAdminService.createScreen(data);
            return ResponseEntity.ok(screen);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/screens")
    public ResponseEntity<?> listScreens(@RequestParam(required = false) String screenType) {
        var screens = workflowAdminService.listScreens(screenType);
        return ResponseEntity.ok(screens);
    }

    @GetMapping("/screens/{id}")
    public ResponseEntity<?> getScreen(@PathVariable String id) {
        try {
            var screen = workflowAdminService.getScreen(parseUUID(id));
            return ResponseEntity.ok(screen);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/screens/{id}")
    public ResponseEntity<?> updateScreen(@PathVariable String id,
                                          @RequestBody Map<String, Object> updates) {
        try {
            var screen = workflowAdminService.updateScreen(parseUUID(id), updates);
            return ResponseEntity.ok(screen);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/screens/{id}")
    public ResponseEntity<?> deleteScreen(@PathVariable String id) {
        try {
            workflowAdminService.deleteScreen(parseUUID(id));
            return ResponseEntity.ok(Map.of("message", "Screen deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/screens/{id}/tabs")
    public ResponseEntity<?> addScreenTab(@PathVariable String id,
                                          @RequestBody Map<String, Object> tabData) {
        try {
            var tab = workflowAdminService.addScreenTab(parseUUID(id), tabData);
            return ResponseEntity.ok(tab);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/screens/tabs/{tabId}")
    public ResponseEntity<?> updateScreenTab(@PathVariable String tabId,
                                             @RequestBody Map<String, Object> updates) {
        try {
            var tab = workflowAdminService.updateScreenTab(parseUUID(tabId), updates);
            return ResponseEntity.ok(tab);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/screens/tabs/{tabId}")
    public ResponseEntity<?> deleteScreenTab(@PathVariable String tabId) {
        try {
            workflowAdminService.deleteScreenTab(parseUUID(tabId));
            return ResponseEntity.ok(Map.of("message", "Tab deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/screens/tabs/{tabId}/fields")
    public ResponseEntity<?> configureScreenFields(@PathVariable String tabId,
                                                   @RequestBody List<Map<String, Object>> fields) {
        try {
            var configuredFields = workflowAdminService.configureScreenFields(parseUUID(tabId), fields);
            return ResponseEntity.ok(configuredFields);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/screens/fields/{fieldId}")
    public ResponseEntity<?> updateScreenField(@PathVariable String fieldId,
                                               @RequestBody Map<String, Object> updates) {
        try {
            var field = workflowAdminService.updateScreenField(parseUUID(fieldId), updates);
            return ResponseEntity.ok(field);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/screens/fields/{fieldId}")
    public ResponseEntity<?> deleteScreenField(@PathVariable String fieldId) {
        try {
            workflowAdminService.deleteScreenField(parseUUID(fieldId));
            return ResponseEntity.ok(Map.of("message", "Field removed successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== Transition Screen Configuration ====================

    @PostMapping("/transitions/{transitionId}/screen")
    public ResponseEntity<?> assignScreenToTransition(@PathVariable String transitionId,
                                                      @RequestBody Map<String, Object> screenData) {
        try {
            var result = workflowAdminService.assignScreenToTransition(parseUUID(transitionId), screenData);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/transitions/{transitionId}/screen")
    public ResponseEntity<?> removeScreenFromTransition(@PathVariable String transitionId) {
        try {
            workflowAdminService.removeScreenFromTransition(parseUUID(transitionId));
            return ResponseEntity.ok(Map.of("message", "Screen removed from transition"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== Migration & Usage Stats ====================

    @PostMapping("/{id}/migrate")
    public ResponseEntity<?> migrateIssues(@PathVariable String id,
                                           @RequestBody Map<String, Object> request) {
        try {
            String targetWorkflowId = (String) request.get("targetWorkflowId");
            @SuppressWarnings("unchecked")
            Map<String, Object> filters = (Map<String, Object>) request.get("filters");
            var result = workflowAdminService.migrateIssues(parseUUID(id), parseUUID(targetWorkflowId), filters);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/migration-preview")
    public ResponseEntity<?> previewMigration(@PathVariable String id) {
        try {
            var preview = workflowAdminService.previewMigration(parseUUID(id), null);
            return ResponseEntity.ok(preview);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/usage")
    public ResponseEntity<?> getWorkflowUsage(@PathVariable String id) {
        try {
            var usage = workflowAdminService.getWorkflowUsage(parseUUID(id));
            return ResponseEntity.ok(usage);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/transition-stats")
    public ResponseEntity<?> getTransitionStats(@PathVariable String id,
                                                @RequestParam(required = false) String startDate,
                                                @RequestParam(required = false) String endDate) {
        try {
            var stats = workflowAdminService.getTransitionStats(parseUUID(id), startDate, endDate);
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== Utility Endpoints ====================

    @PostMapping("/{id}/validate")
    public ResponseEntity<?> validateWorkflow(@PathVariable String id) {
        try {
            var validation = workflowAdminService.validateWorkflow(parseUUID(id));
            return ResponseEntity.ok(validation);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/available-transitions")
    public ResponseEntity<?> getAvailableTransitions(@PathVariable String id,
                                                     @RequestParam String statusId) {
        try {
            var transitions = workflowAdminService.getAvailableTransitions(parseUUID(id), parseUUID(statusId));
            return ResponseEntity.ok(transitions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/conditions/definitions")
    public ResponseEntity<?> getConditionDefinitions() {
        var definitions = workflowAdminService.getConditionDefinitions();
        return ResponseEntity.ok(definitions);
    }

    @GetMapping("/validators/definitions")
    public ResponseEntity<?> getValidatorDefinitions() {
        var definitions = workflowAdminService.getValidatorDefinitions();
        return ResponseEntity.ok(definitions);
    }

    @GetMapping("/post-functions/definitions")
    public ResponseEntity<?> getPostFunctionDefinitions() {
        var definitions = workflowAdminService.getPostFunctionDefinitions();
        return ResponseEntity.ok(definitions);
    }

    // ==================== Audit Log ====================

    @GetMapping("/{id}/audit-log")
    public ResponseEntity<?> getWorkflowAuditLog(@PathVariable String id,
                                                @RequestParam(required = false) String action,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "50") int size) {
        var auditLog = workflowAdminService.getWorkflowAuditLog(parseUUID(id), action, page, size);
        return ResponseEntity.ok(auditLog);
    }

    @GetMapping("/audit-log")
    public ResponseEntity<?> getAllAuditLog(@RequestParam(required = false) String action,
                                           @RequestParam(required = false) String userId,
                                           @RequestParam(required = false) String startDate,
                                           @RequestParam(required = false) String endDate,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "50") int size) {
        var auditLog = workflowAdminService.getAllAuditLog(action, userId, startDate, endDate, page, size);
        return ResponseEntity.ok(auditLog);
    }
}