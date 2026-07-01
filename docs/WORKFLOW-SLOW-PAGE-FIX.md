# Workflow Page Load Slow - Root Cause & Fix

## Issue Summary
Initial workflow page loads are slow due to multiple performance issues in the backend.

## Root Causes

### 1. N+1 Query Problem (Critical)
**Location**: `WorkflowService.mapTransitionDetail()`
```java
public TransitionDetailResponse mapTransitionDetail(WorkflowTransition transition) {
    List<WorkflowCondition> conditions = workflowConditionRepository.findByTransitionIdOrderBySequenceAsc(transition.getId());    // Query 1
    List<WorkflowValidator> validators = workflowConditionRepository.findByTransitionIdOrderBySequenceAsc(transition.getId());   // Query 2  
    List<WorkflowPostFunction> postFunctions = workflowConditionRepository.findByTransitionIdOrderBySequenceAsc(transition.getId()); // Query 3
```
**Problem**: For a workflow with N transitions, this makes 3N database calls:
- 1 query to fetch all transitions
- N transitions × 3 queries each (conditions, validators, post-functions)

**Example**: A workflow with 5 transitions = 1 + (5×3) = 16 queries instead of 4.

### 2. External API Calls Without Caching (Critical)
**Location**: `WorkflowStatusCatalog.loadCatalog()`
```java
public Map<String, StatusMeta> loadCatalog() {
    mergeIssueStatuses(catalog);  // HTTP call to issue-service
    mergeAdminStatuses(catalog);  // HTTP call to admin-service
    KNOWN_STATUS_NAMES.forEach(...)  // In-memory only
}
```
**Problem**: Called on EVERY request to `getWorkflowDetail` with no caching. 2 HTTP API calls per page load.

### 3. Duplicate Catalog Loading
**Location**: `WorkflowStatusService.getWorkflowStatuses()`
```java
Map<String, WorkflowStatusCatalog.StatusMeta> catalog = statusCatalog.loadCatalog();
```
**Problem**: Calls `loadCatalog()` separately from `WorkflowDetailService`, resulting in redundant API calls when both services are used together.

## Solutions

### 1. Fix N+1 Query - Batch Loading
**File**: `WorkflowService.java`

Option A: Create bulk repository methods:
```java
public List<WorkflowCondition> findByTransitionIdsInAndSequenceAsc(List<UUID> transitionIds);

public List<WorkflowValidator> findByTransitionIdsInAndSequenceAsc(List<UUID> transitionIds);

public List<WorkflowPostFunction> findByTransitionIdsInAndSequenceAsc(List<UUID> transitionIds);
```

Option B: Use @EntityGraph (preferred)
```java
@EntityGraph(attributePaths = {"conditions", "validators", "postFunctions"})
Optional<WorkflowTransition> findById(UUID id);

public List<TransitionDetailResponse> getTransitionsWithDetails(UUID workflowId) {
    List<WorkflowTransition> transitions = workflowTransitionRepository.findByWorkflowId(workflowId);
    
    // Load all related data in one batch query
    List<UUID> transitionIds = transitions.stream()
            .map(WorkflowTransition::getId)
            .collect(Collectors.toList());
    
    Map<UUID, List<WorkflowCondition>> conditionsMap = workflowConditionRepository
            .findByTransitionIdsInAndSequenceAsc(transitionIds).stream()
            .collect(Collectors.groupingBy(WorkflowCondition::getTransitionId));
    
    // Similar for validators and post-functions...
}
```

### 2. Add Caching to WorkflowStatusCatalog
**File**: `WorkflowStatusCatalog.java`

```java
@Cacheable(value = "status-catalog", key = "catalog")
public Map<String, StatusMeta> loadCatalog() {
    // Same logic but cached
}

@PreDestroy
public void cleanup() {
    // Clear cache on shutdown
}
```

Also add `@EnableCaching` to the main application class.

### 3. Share Catalog Between Services
**File**: `WorkflowDetailService.java`
```java
public WorkflowDetailResponse getWorkflowDetail(UUID workflowId) {
    // Load catalog once and pass to both services
    Map<String, WorkflowStatusCatalog.StatusMeta> catalog = statusCatalog.loadCatalog();
    
    WorkflowResponse workflow = workflowService.getWorkflow(workflowId, catalog);
    List<WorkflowStatusResponse> statuses = workflowStatusService.getWorkflowStatuses(workflowId, catalog);
    // ...
}
```

### 4. Alternative: Frontend Caching
Consider caching the definitions data in React Query since it's static:
- Add `staleTime: 30 * 60 * 1000` (30 minutes) to definition queries
- Use `refetchOnWindowFocus: false` if definitions rarely change

## Recommended Implementation Priority

1. **High Priority**: Fix N+1 query - this has the biggest impact on page load time
2. **High Priority**: Add caching to WorkflowStatusCatalog - eliminates 2 external API calls per request
3. **Medium Priority**: Share catalog between services - minor optimization

## Verification Steps

1. After N+1 fix: Check that `getTransitionsWithDetails` makes only 2-3 queries instead of 3N+1
2. After caching fix: Check `WorkflowStatusCatalog.loadCatalog()` only calls external APIs once per 30 minutes
3. Test page load time improvement should be significant (50-80% faster)