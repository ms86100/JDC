# Workflow Service 500 Errors - Root Cause Analysis

## Issue Summary

Frontend console errors on:
- `workflows/screens` - 500 
- `workflows/conditions/definitions` - 500
- `workflows/validators/definitions` - 500
- `workflows/post-functions/definitions` - 500

## Root Cause

The gateway routing configuration has a conflict:
1. `/admin/**` route (lines 145-150 in application-local.yml) rewrites `/admin/X` to `/api/admin/X` and routes to admin-service (port 8093)
2. `/api/admin/workflows/**` route (lines 223-226 in application-local.yml) routes to workflow-service (port 8085)
3. The frontend calls `/admin/workflows/...` which matches the FIRST route and goes to the admin-service
4. The admin-service has a `WorkflowAdminProxyService` but it only proxies basic CRUD operations for `/api/admin/workflows` (list, get, create, update, publish, create draft)
5. The admin-service does NOT have endpoints for:
   - `/conditions/definitions`
   - `/validators/definitions` 
   - `/post-functions/definitions`
   - `/screens`

## Solution Options

### Option 1: Change Route Order (Recommended)
Move the `/api/admin/workflows/**` route BEFORE the `/admin/**` route so it takes priority for matching `/api/admin/workflows/**` requests. This would NOT fix `/admin/**` calls which still need the proxy.

### Option 2: Add Specific Admin Routes
Add specific routes for admin workflow endpoints:
```
- id: admin-workflows-screens
  uri: http://localhost:8085
  predicates:
    - Path=/admin/workflows/screens/**
  filters:
    - RewritePath=/admin/workflows(?<path>/?.*), /api/admin/workflows$\{path}
```

### Option 3: Extend Admin Service Proxy
Add proxy methods in `jira-admin-service/src/main/java/com/jira/admin/service/WorkflowAdminProxyService.java` for the missing endpoints and modify the admin controller to handle them.

## Code Fixes Needed

### For Option 1 - Change Route Order in Gateway:
**File**: `/home/ubuntu/workspace/JDC/jira-gateway/src/main/resources/application-local.yml`

Move the `/api/admin/workflows/**` route (lines 223-226) to appear BEFORE the `/admin/**` route (lines 145-150).

### For Option 3 - Extend Admin Service Proxy:

**File**: `/home/ubuntu/workspace/JDC/jira-admin-service/src/main/java/com/jira/admin/service/WorkflowAdminProxyService.java`

Add these methods:
```java
public List<Map<String, Object>> getConditionDefinitions() {
    JsonNode body = restTemplate.getForObject(workflowServiceUrl + "/api/admin/workflows/conditions/definitions", JsonNode.class);
    return body != null && body.isArray() ? objectMapper.convertValue(body, new TypeReference<>() {}) : List.of();
}

public List<Map<String, Object>> getValidatorDefinitions() {
    JsonNode body = restTemplate.getForObject(workflowServiceUrl + "/api/admin/workflows/validators/definitions", JsonNode.class);
    return body != null && body.isArray() ? objectMapper.convertValue(body, new TypeReference<>() {}) : List.of();
}

public List<Map<String, Object>> getPostFunctionDefinitions() {
    JsonNode body = restTemplate.getForObject(workflowServiceUrl + "/api/admin/workflows/post-functions/definitions", JsonNode.class);
    return body != null && body.isArray() ? objectMapper.convertValue(body, new TypeReference<>() {}) : List.of();
}

public List<Map<String, Object>> listScreens(String screenType) {
    String url = screenType != null && !screenType.isEmpty() 
        ? workflowServiceUrl + "/api/admin/workflows/screens?screenType=" + screenType 
        : workflowServiceUrl + "/api/admin/workflows/screens";
    JsonNode body = restTemplate.getForObject(url, JsonNode.class);
    return body != null && body.isArray() ? objectMapper.convertValue(body, new TypeReference<>() {}) : List.of();
}
```

## Verification Steps

1. After fixing, these endpoints should return 200 with JSON data:
   - `/api/admin/workflows/conditions/definitions`
   - `/api/admin/workflows/validators/definitions`
   - `/api/admin/workflows/post-functions/definitions`
   - `/api/admin/workflows/screens`

2. Test via browser and verify frontend console errors are gone.

## Recommended Solution

Use Option 1 (change route order) AND Option 3 (extend proxy) for complete fix:
- Route order ensures `/api/admin/workflows/**` routes go directly to workflow-service
- Proxy extension ensures admin service can still handle legacy paths if needed

The `/admin/**` path rewrite should be left for other admin endpoints (non-workflow) that should go to the admin service.