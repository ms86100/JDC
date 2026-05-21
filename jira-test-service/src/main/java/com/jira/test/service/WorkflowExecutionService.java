package com.jira.test.service;

import com.jira.test.dto.CreateWorkflowDefinitionRequest;
import com.jira.test.entity.WorkflowDefinition;
import com.jira.test.entity.WorkflowInstance;
import com.jira.test.repository.WorkflowDefinitionRepository;
import com.jira.test.repository.WorkflowInstanceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowExecutionService {

    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowInstanceRepository instanceRepository;
    private final ObjectMapper objectMapper;

    // ========== Definition Management ==========

    @Transactional
    public WorkflowDefinition createDefinition(CreateWorkflowDefinitionRequest request) {
        WorkflowDefinition definition = WorkflowDefinition.builder()
                .name(request.getName())
                .description(request.getDescription())
                .projectId(request.getProjectId())
                .workflowType(request.getWorkflowType())
                .workflowStepsJson(request.getWorkflowStepsJson())
                .transitionRulesJson(request.getTransitionRulesJson())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .isActive(true)
                .build();

        WorkflowDefinition saved = definitionRepository.save(definition);

        // If this is set as default, unset other defaults
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            unsetOtherDefaults(saved.getId(), saved.getProjectId());
        }

        return saved;
    }

    public List<WorkflowDefinition> getDefinitionsByProject(UUID projectId) {
        return definitionRepository.findByProjectId(projectId);
    }

    public List<WorkflowDefinition> getDefinitionsByType(UUID projectId, String workflowType) {
        return definitionRepository.findByProjectIdAndWorkflowType(projectId, workflowType);
    }

    public WorkflowDefinition getDefinitionById(UUID id) {
        return definitionRepository.findById(id)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow definition not found: " + id));
    }

    public WorkflowDefinition getDefaultDefinition(UUID projectId, String workflowType) {
        return definitionRepository.findByProjectIdAndWorkflowTypeAndIsDefaultTrue(projectId, workflowType)
                .orElseThrow(() -> new WorkflowNotFoundException("No default workflow found for type: " + workflowType));
    }

    @Transactional
    public WorkflowDefinition updateDefinition(UUID id, CreateWorkflowDefinitionRequest request) {
        WorkflowDefinition definition = getDefinitionById(id);

        if (request.getName() != null) definition.setName(request.getName());
        if (request.getDescription() != null) definition.setDescription(request.getDescription());
        if (request.getWorkflowStepsJson() != null) definition.setWorkflowStepsJson(request.getWorkflowStepsJson());
        if (request.getTransitionRulesJson() != null) definition.setTransitionRulesJson(request.getTransitionRulesJson());
        if (request.getIsDefault() != null) {
            definition.setIsDefault(request.getIsDefault());
            if (Boolean.TRUE.equals(request.getIsDefault())) {
                unsetOtherDefaults(id, definition.getProjectId());
            }
        }

        return definitionRepository.save(definition);
    }

    @Transactional
    public void deleteDefinition(UUID id) {
        WorkflowDefinition definition = getDefinitionById(id);

        // Check if there are active instances using this definition
        long activeInstances = instanceRepository.countByDefinitionIdAndIsCompletedFalse(id);
        if (activeInstances > 0) {
            throw new WorkflowException("Cannot delete workflow with " + activeInstances + " active instances");
        }

        definitionRepository.delete(definition);
    }

    @Transactional
    public WorkflowDefinition activateDefinition(UUID id) {
        WorkflowDefinition definition = getDefinitionById(id);
        definition.setIsActive(true);
        return definitionRepository.save(definition);
    }

    @Transactional
    public WorkflowDefinition deactivateDefinition(UUID id) {
        WorkflowDefinition definition = getDefinitionById(id);
        definition.setIsActive(false);
        return definitionRepository.save(definition);
    }

    // ========== Instance Management ==========

    @Transactional
    public WorkflowInstance startWorkflow(UUID definitionId, String entityType, UUID entityId, UUID userId) {
        WorkflowDefinition definition = getDefinitionById(definitionId);

        if (!definition.getIsActive()) {
            throw new WorkflowException("Workflow definition is not active");
        }

        // Parse initial state from workflow steps
        String initialState = parseInitialState(definition.getWorkflowStepsJson());

        // Create state history entry
        StateTransition initialTransition = StateTransition.builder()
                .fromState(null)
                .toState(initialState)
                .transitionedBy(userId)
                .transitionedAt(LocalDateTime.now())
                .comment("Workflow started")
                .build();

        WorkflowInstance instance = WorkflowInstance.builder()
                .definitionId(definitionId)
                .entityType(entityType)
                .entityId(entityId)
                .currentState(initialState)
                .stateHistoryJson(toJson(Collections.singletonList(initialTransition)))
                .initiatedBy(userId)
                .isCompleted(false)
                .build();

        return instanceRepository.save(instance);
    }

    @Transactional
    public WorkflowInstance transition(UUID instanceId, String targetState, UUID userId, String comment) {
        WorkflowInstance instance = getInstanceById(instanceId);

        if (instance.getIsCompleted()) {
            throw new WorkflowException("Workflow instance is already completed");
        }

        WorkflowDefinition definition = getDefinitionById(instance.getDefinitionId());

        // Validate transition is allowed
        validateTransition(definition, instance.getCurrentState(), targetState);

        // Create transition record
        StateTransition transition = StateTransition.builder()
                .fromState(instance.getCurrentState())
                .toState(targetState)
                .transitionedBy(userId)
                .transitionedAt(LocalDateTime.now())
                .comment(comment)
                .build();

        // Update instance
        instance.setCurrentState(targetState);

        // Add to history
        List<StateTransition> history = fromJson(instance.getStateHistoryJson());
        history.add(transition);
        instance.setStateHistoryJson(toJson(history));

        // Check if this is a final state
        if (isFinalState(definition, targetState)) {
            instance.setIsCompleted(true);
            instance.setCompletedAt(LocalDateTime.now());
        }

        return instanceRepository.save(instance);
    }

    public WorkflowInstance getInstanceById(UUID id) {
        return instanceRepository.findById(id)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow instance not found: " + id));
    }

    public List<WorkflowInstance> getActiveInstances() {
        return instanceRepository.findByIsCompletedFalse();
    }

    public List<WorkflowInstance> getInstancesByEntity(String entityType, UUID entityId) {
        return instanceRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    public List<WorkflowInstance> getInstancesByDefinition(UUID definitionId) {
        return instanceRepository.findByDefinitionId(definitionId);
    }

    public List<WorkflowInstance> getInstancesByUser(UUID userId) {
        return instanceRepository.findByInitiatedBy(userId);
    }

    @Transactional
    public WorkflowInstance cancelWorkflow(UUID instanceId, UUID userId, String reason) {
        WorkflowInstance instance = getInstanceById(instanceId);

        if (instance.getIsCompleted()) {
            throw new WorkflowException("Cannot cancel completed workflow");
        }

        StateTransition cancelTransition = StateTransition.builder()
                .fromState(instance.getCurrentState())
                .toState("CANCELLED")
                .transitionedBy(userId)
                .transitionedAt(LocalDateTime.now())
                .comment(reason != null ? reason : "Workflow cancelled")
                .build();

        List<StateTransition> history = fromJson(instance.getStateHistoryJson());
        history.add(cancelTransition);

        instance.setCurrentState("CANCELLED");
        instance.setStateHistoryJson(toJson(history));
        instance.setIsCompleted(true);
        instance.setCompletedAt(LocalDateTime.now());

        return instanceRepository.save(instance);
    }

    @Transactional
    public WorkflowInstance reassignWorkflow(UUID instanceId, UUID newAssignee, UUID userId) {
        WorkflowInstance instance = getInstanceById(instanceId);

        if (instance.getIsCompleted()) {
            throw new WorkflowException("Cannot reassign completed workflow");
        }

        instance.setAssignedTo(newAssignee);

        StateTransition reassignTransition = StateTransition.builder()
                .fromState(instance.getCurrentState())
                .toState(instance.getCurrentState())
                .transitionedBy(userId)
                .transitionedAt(LocalDateTime.now())
                .comment("Reassigned to " + newAssignee)
                .build();

        List<StateTransition> history = fromJson(instance.getStateHistoryJson());
        history.add(reassignTransition);
        instance.setStateHistoryJson(toJson(history));

        return instanceRepository.save(instance);
    }

    // ========== Workflow Engine Logic ==========

    public List<String> getAvailableTransitions(UUID instanceId) {
        WorkflowInstance instance = getInstanceById(instanceId);
        WorkflowDefinition definition = getDefinitionById(instance.getDefinitionId());

        return getAvailableStatesFromDefinition(definition, instance.getCurrentState());
    }

    public List<String> getAllStates(UUID definitionId) {
        WorkflowDefinition definition = getDefinitionById(definitionId);
        return parseStates(definition.getWorkflowStepsJson());
    }

    public Map<String, Object> getWorkflowProgress(UUID instanceId) {
        WorkflowInstance instance = getInstanceById(instanceId);
        WorkflowDefinition definition = getDefinitionById(instance.getDefinitionId());

        List<String> allStates = parseStates(definition.getWorkflowStepsJson());
        List<StateTransition> history = fromJson(instance.getStateHistoryJson());

        Map<String, Object> progress = new HashMap<>();
        progress.put("totalStates", allStates.size());
        progress.put("visitedStates", history.size());
        progress.put("percentComplete", calculatePercentComplete(allStates, instance.getCurrentState()));
        progress.put("currentState", instance.getCurrentState());
        progress.put("isCompleted", instance.getIsCompleted());
        progress.put("elapsedTime", instance.getCreatedAt() != null ?
                java.time.Duration.between(instance.getCreatedAt(), LocalDateTime.now()).toMinutes() : 0);

        return progress;
    }

    public List<StateTransition> getStateHistory(UUID instanceId) {
        WorkflowInstance instance = getInstanceById(instanceId);
        return fromJson(instance.getStateHistoryJson());
    }

    // ========== Validation ==========

    public ValidationResult validateWorkflowDefinition(WorkflowDefinition definition) {
        ValidationResult result = new ValidationResult();

        try {
            List<String> states = parseStates(definition.getWorkflowStepsJson());

            if (states.isEmpty()) {
                result.addError("No states defined in workflow");
            }

            // Check for circular transitions
            if (hasCircularTransitions(definition)) {
                result.addWarning("Workflow may have circular transitions");
            }

            // Check for unreachable states
            List<String> reachableStates = findReachableStates(definition);
            for (String state : states) {
                if (!reachableStates.contains(state)) {
                    result.addWarning("State '" + state + "' may be unreachable");
                }
            }

            // Check for missing final states
            boolean hasFinalState = states.stream().anyMatch(s -> isFinalState(definition, s));
            if (!hasFinalState) {
                result.addWarning("No terminal states defined");
            }

            // Check initial state exists
            String initialState = parseInitialState(definition.getWorkflowStepsJson());
            if (!states.contains(initialState)) {
                result.addError("Initial state '" + initialState + "' not found in states list");
            }

        } catch (Exception e) {
            result.addError("Invalid workflow JSON: " + e.getMessage());
        }

        return result;
    }

    // ========== Helper Methods ==========

    private String parseInitialState(String workflowStepsJson) {
        try {
            JsonNode root = objectMapper.readTree(workflowStepsJson);
            if (root.has("initialState")) {
                return root.get("initialState").asText();
            }
            // Default to first state if no initial state specified
            if (root.has("states") && root.get("states").isArray() && root.get("states").size() > 0) {
                JsonNode firstState = root.get("states").get(0);
                if (firstState.isTextual()) {
                    return firstState.asText();
                } else if (firstState.has("name")) {
                    return firstState.get("name").asText();
                }
            }
            return "INITIATED";
        } catch (Exception e) {
            log.error("Failed to parse initial state", e);
            return "INITIATED";
        }
    }

    private List<String> parseStates(String workflowStepsJson) {
        try {
            JsonNode root = objectMapper.readTree(workflowStepsJson);
            List<String> states = new ArrayList<>();

            if (root.has("states") && root.get("states").isArray()) {
                for (JsonNode stateNode : root.get("states")) {
                    if (stateNode.isTextual()) {
                        states.add(stateNode.asText());
                    } else if (stateNode.has("name")) {
                        states.add(stateNode.get("name").asText());
                    }
                }
            }

            return states;
        } catch (Exception e) {
            log.error("Failed to parse states", e);
            return Collections.emptyList();
        }
    }

    private List<String> getAvailableStatesFromDefinition(WorkflowDefinition definition, String currentState) {
        try {
            JsonNode root = objectMapper.readTree(definition.getWorkflowStepsJson());
            List<String> availableStates = new ArrayList<>();

            if (root.has("transitions")) {
                JsonNode transitions = root.get("transitions");
                if (transitions.has(currentState)) {
                    JsonNode allowedTransitions = transitions.get(currentState);
                    if (allowedTransitions.isArray()) {
                        for (JsonNode node : allowedTransitions) {
                            availableStates.add(node.asText());
                        }
                    }
                }
            }

            // If no explicit transitions, allow all non-final states
            if (availableStates.isEmpty()) {
                List<String> allStates = parseStates(definition.getWorkflowStepsJson());
                for (String state : allStates) {
                    if (!state.equals(currentState) && !isFinalState(definition, state)) {
                        availableStates.add(state);
                    }
                }
            }

            return availableStates;
        } catch (Exception e) {
            log.error("Failed to get available states", e);
            return Collections.emptyList();
        }
    }

    private void validateTransition(WorkflowDefinition definition, String fromState, String toState) {
        List<String> availableStates = getAvailableStatesFromDefinition(definition, fromState);

        if (!availableStates.contains(toState)) {
            throw new InvalidTransitionException(
                    String.format("Invalid transition from '%s' to '%s'. Available transitions: %s",
                            fromState, toState, availableStates));
        }
    }

    private boolean isFinalState(WorkflowDefinition definition, String state) {
        try {
            JsonNode root = objectMapper.readTree(definition.getWorkflowStepsJson());

            if (root.has("finalStates") && root.get("finalStates").isArray()) {
                for (JsonNode node : root.get("finalStates")) {
                    if (node.asText().equals(state)) {
                        return true;
                    }
                }
            }

            // Default final states
            return state.equals("COMPLETED") || state.equals("APPROVED") ||
                    state.equals("REJECTED") || state.equals("CANCELLED");
        } catch (Exception e) {
            return state.equals("COMPLETED") || state.equals("APPROVED") ||
                    state.equals("REJECTED") || state.equals("CANCELLED");
        }
    }

    private boolean hasCircularTransitions(WorkflowDefinition definition) {
        // Simple cycle detection using DFS
        try {
            JsonNode root = objectMapper.readTree(definition.getWorkflowStepsJson());
            Map<String, List<String>> graph = new HashMap<>();

            if (root.has("states") && root.get("states").isArray()) {
                for (JsonNode stateNode : root.get("states")) {
                    String stateName;
                    if (stateNode.isTextual()) {
                        stateName = stateNode.asText();
                    } else if (stateNode.has("name")) {
                        stateName = stateNode.get("name").asText();
                    } else {
                        continue;
                    }

                    List<String> transitions = new ArrayList<>();

                    if (root.has("transitions") && root.get("transitions").has(stateName)) {
                        JsonNode trans = root.get("transitions").get(stateName);
                        if (trans.isArray()) {
                            for (JsonNode t : trans) {
                                transitions.add(t.asText());
                            }
                        }
                    }
                    graph.put(stateName, transitions);
                }
            }

            Set<String> visited = new HashSet<>();
            Set<String> recursionStack = new HashSet<>();

            for (String node : graph.keySet()) {
                if (!visited.contains(node)) {
                    if (hasCycleDFS(node, graph, visited, recursionStack)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to check circular transitions", e);
            return false;
        }
    }

    private boolean hasCycleDFS(String node, Map<String, List<String>> graph, Set<String> visited, Set<String> stack) {
        visited.add(node);
        stack.add(node);

        for (String neighbor : graph.getOrDefault(node, Collections.emptyList())) {
            if (!visited.contains(neighbor)) {
                if (hasCycleDFS(neighbor, graph, visited, stack)) {
                    return true;
                }
            } else if (stack.contains(neighbor)) {
                return true;
            }
        }

        stack.remove(node);
        return false;
    }

    private List<String> findReachableStates(WorkflowDefinition definition) {
        Set<String> reachable = new HashSet<>();
        String initial = parseInitialState(definition.getWorkflowStepsJson());
        reachable.add(initial);

        try {
            JsonNode root = objectMapper.readTree(definition.getWorkflowStepsJson());

            Queue<String> queue = new LinkedList<>();
            queue.add(initial);

            while (!queue.isEmpty()) {
                String current = queue.poll();

                if (root.has("transitions") && root.get("transitions").has(current)) {
                    JsonNode trans = root.get("transitions").get(current);
                    if (trans.isArray()) {
                        for (JsonNode t : trans) {
                            String next = t.asText();
                            if (!reachable.contains(next)) {
                                reachable.add(next);
                                queue.add(next);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to find reachable states", e);
        }

        return new ArrayList<>(reachable);
    }

    private int calculatePercentComplete(List<String> allStates, String currentState) {
        if (allStates.isEmpty()) return 0;
        int index = allStates.indexOf(currentState);
        if (index < 0) index = allStates.size() - 1;
        return (int) (((double) (index + 1) / allStates.size()) * 100);
    }

    private String toJson(List<StateTransition> transitions) {
        try {
            return objectMapper.writeValueAsString(transitions);
        } catch (Exception e) {
            log.error("Failed to serialize state transitions", e);
            return "[]";
        }
    }

    private List<StateTransition> fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, StateTransition.class));
        } catch (Exception e) {
            log.error("Failed to deserialize state transitions", e);
            return new ArrayList<>();
        }
    }

    private void unsetOtherDefaults(UUID currentId, UUID projectId) {
        if (projectId == null) return;
        List<WorkflowDefinition> definitions = definitionRepository.findByProjectId(projectId);
        for (WorkflowDefinition def : definitions) {
            if (!def.getId().equals(currentId) && Boolean.TRUE.equals(def.getIsDefault())) {
                def.setIsDefault(false);
                definitionRepository.save(def);
            }
        }
    }

    // ========== Inner Classes ==========

    @lombok.Data
    @lombok.Builder
    public static class StateTransition {
        private String fromState;
        private String toState;
        private UUID transitionedBy;
        private LocalDateTime transitionedAt;
        private String comment;
    }

    @lombok.Data
    public static class ValidationResult {
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();

        public void addError(String error) { errors.add(error); }
        public void addWarning(String warning) { warnings.add(warning); }
        public boolean isValid() { return errors.isEmpty(); }
        public boolean hasWarnings() { return !warnings.isEmpty(); }
    }

    // ========== Custom Exceptions ==========

    public static class WorkflowNotFoundException extends RuntimeException {
        public WorkflowNotFoundException(String message) { super(message); }
    }

    public static class WorkflowException extends RuntimeException {
        public WorkflowException(String message) { super(message); }
    }

    public static class InvalidTransitionException extends RuntimeException {
        public InvalidTransitionException(String message) { super(message); }
    }
}
