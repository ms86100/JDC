# Jira Test Management Platform - Plugin API Documentation

## Overview

The Plugin Extensibility infrastructure provides a framework for extending the Jira Test Management Platform with custom functionality through plugin hooks. Plugins can intercept and respond to key lifecycle events within the test management system.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      Plugin Controller                          │
│                  (REST API Endpoints)                            │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Plugin Registry                            │
│              (Plugin Lifecycle Management)                       │
└─────────────────────────────────────────────────────────────────┘
          │                    │                    │
          ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│  PluginHook     │  │  PluginManifest │  │  PluginSandbox  │
│  (Hook Types)   │  │  (Entity)       │  │  (Safe Exec)    │
└─────────────────┘  └─────────────────┘  └─────────────────┘
```

## Plugin Manifest Format

Plugins require a manifest file (`manifest.json`) packaged within the JAR:

```json
{
  "pluginId": "custom-coverage-reporter",
  "name": "Custom Coverage Reporter",
  "version": "1.0.0",
  "description": "Generates custom coverage reports with charts",
  "author": "ACME Corp",
  "vendor": "ACME Corp",
  "entryPoint": "com.acme.CoverageReporterPlugin",
  "permissions": ["READ_TESTS", "READ_COVERAGE", "WRITE_REPORTS"],
  "hooks": [
    "TEST_CREATED",
    "TEST_EXECUTION_COMPLETED",
    "COVERAGE_CALCULATED"
  ]
}
```

## Available Hooks

### 1. TEST_CREATED

Called after a new test is created in the system.

**Payload:**
```json
{
  "testId": "TEST-123",
  "testName": "Login smoke test",
  "testType": "AUTOMATED",
  "projectId": "PROJ",
  "createdBy": "user@company.com",
  "createdAt": "2026-05-21T10:30:00Z"
}
```

**Example Implementation:**
```java
import com.jira.test.plugin.hook.PluginHook;
import com.jira.test.plugin.hook.PluginHook.HookContext;
import com.jira.test.plugin.hook.PluginHook.HookResult;
import com.jira.test.plugin.hook.PluginHook.HookType;

public class TestTrackingPlugin implements PluginHook {

    @Override
    public HookType[] getHookTypes() {
        return new HookType[]{ HookType.TEST_CREATED };
    }

    @Override
    public HookResult onTestCreated(HookContext context) {
        String testId = (String) context.get("testId");
        String testName = (String) context.get("testName");
        String projectId = (String) context.get("projectId");

        System.out.println("New test created: " + testName + " (" + testId + ")");

        // Send notification, update external system, etc.
        return HookResult.success("Test tracking updated");
    }
}
```

### 2. TEST_EXECUTION_STARTED

Called before a test execution begins.

**Payload:**
```json
{
  "executionId": "EXEC-456",
  "testId": "TEST-123",
  "projectId": "PROJ",
  "environment": "staging",
  "triggeredBy": "scheduler"
}
```

**Example Implementation:**
```java
public class PreExecutionPlugin implements PluginHook {

    @Override
    public HookType[] getHookTypes() {
        return new HookType[]{ HookType.TEST_EXECUTION_STARTED };
    }

    @Override
    public HookResult onTestExecutionStarted(HookContext context) {
        String executionId = (String) context.get("executionId");
        String environment = (String) context.get("environment");

        // Validate environment setup
        if (!isEnvironmentReady(environment)) {
            return HookResult.failure("Environment " + environment + " is not ready");
        }

        // Setup test prerequisites
        setupPrerequisites(executionId);

        return HookResult.success();
    }
}
```

### 3. TEST_EXECUTION_COMPLETED

Called after a test execution finishes.

**Payload:**
```json
{
  "executionId": "EXEC-456",
  "testId": "TEST-123",
  "result": "PASSED",
  "duration": 12500,
  "projectId": "PROJ",
  "environment": "staging"
}
```

**Example Implementation:**
```java
public class ResultsNotifierPlugin implements PluginHook {

    @Override
    public HookType[] getHookTypes() {
        return new HookType[]{ HookType.TEST_EXECUTION_COMPLETED };
    }

    @Override
    public HookResult onTestExecutionCompleted(HookContext context) {
        String executionId = (String) context.get("executionId");
        String result = (String) context.get("result");
        Long duration = (Long) context.get("duration");

        if ("FAILED".equals(result)) {
            notifyFailure(executionId, duration);
        } else if ("PASSED".equals(result)) {
            notifySuccess(executionId);
        }

        return HookResult.success();
    }
}
```

### 4. PRECONDITION_EVALUATED

Called after a test precondition is evaluated.

**Payload:**
```json
{
  "preconditionId": "PRE-789",
  "testId": "TEST-123",
  "result": "SKIPPED",
  "evaluatedAt": "2026-05-21T10:35:00Z",
  "projectId": "PROJ"
}
```

### 5. COVERAGE_CALCULATED

Called after test coverage is calculated for a project.

**Payload:**
```json
{
  "projectId": "PROJ",
  "coveragePercentage": 78.5,
  "coveredItems": 157,
  "totalItems": 200,
  "calculatedAt": "2026-05-21T10:40:00Z"
}
```

## REST API Endpoints

### Upload Plugin
```
POST /api/plugins/upload
Content-Type: multipart/form-data

Parameters:
- file: Plugin JAR file
- projectId: Target project ID

Response:
{
  "success": true,
  "message": "Plugin uploaded successfully",
  "pluginId": "custom-plugin_1716289200000",
  "status": "INSTALLED"
}
```

### List Plugins
```
GET /api/plugins
GET /api/plugins?projectId=PROJ
GET /api/plugins?enabled=true

Response:
{
  "plugins": [
    {
      "pluginId": "custom-plugin_123",
      "name": "Custom Plugin",
      "version": "1.0.0",
      "status": "ENABLED",
      "enabled": true
    }
  ],
  "total": 1
}
```

### Get Plugin Details
```
GET /api/plugins/{id}

Response:
{
  "pluginId": "custom-plugin_123",
  "name": "Custom Plugin",
  "version": "1.0.0",
  "description": "Plugin description",
  "author": "Author Name",
  "vendor": "Vendor Name",
  "status": "ENABLED",
  "enabled": true,
  "installedAt": "2026-05-21T10:00:00Z"
}
```

### Enable Plugin
```
PUT /api/plugins/{id}/enable

Response: Updated PluginInfo object
```

### Disable Plugin
```
PUT /api/plugins/{id}/disable

Response: Updated PluginInfo object
```

### Uninstall Plugin
```
DELETE /api/plugins/{id}

Response: 204 No Content
```

### Get Plugin Hooks
```
GET /api/plugins/{id}/hooks

Response:
{
  "pluginId": "custom-plugin_123",
  "hooks": ["test.created", "test.execution.completed"],
  "count": 2
}
```

### Test Plugin
```
POST /api/plugins/{id}/test
POST /api/plugins/{id}/test?hookType=TEST_CREATED

Response:
{
  "pluginId": "custom-plugin_123",
  "success": true,
  "message": "Hook executed successfully",
  "data": {}
}
```

## Security Considerations

### Sandbox Execution
All plugin hooks execute within a sandboxed environment providing:

1. **Timeout Enforcement**: Maximum 30 seconds execution time per hook
2. **Memory Limits**: Maximum 128MB memory allocation
3. **API Whitelist**: Only approved Java classes are accessible
4. **Audit Logging**: All plugin actions are logged

### Allowed APIs

Plugins can access the following safe APIs:

| Package | Classes |
|---------|---------|
| java.lang | String, Integer, Long, Boolean, Double, Float |
| java.util | List, Map, Set, HashMap, ArrayList, HashSet |
| java.time | LocalDateTime, Instant |
| Plugin API | PluginHook, HookContext, HookResult, HookType |

### Security Best Practices

1. **Input Validation**: Always validate hook payload data before use
2. **Error Handling**: Return appropriate HookResult on failures
3. **Resource Management**: Clean up resources in destroy() method
4. **Minimal Permissions**: Request only necessary permissions
5. **Timeout Awareness**: Keep hook execution fast to avoid timeout

### Audit Trail

The PluginSandbox maintains an audit log of all plugin actions:

```java
List<AuditEntry> entries = sandbox.getAuditLog(pluginId);
AuditStats stats = sandbox.getAuditStats();
```

## Example Plugin Implementation

### Complete Example: Slack Notification Plugin

```java
package com.example.slacknotify;

import com.jira.test.plugin.hook.PluginHook;
import com.jira.test.plugin.hook.PluginHook.HookContext;
import com.jira.test.plugin.hook.PluginHook.HookResult;
import com.jira.test.plugin.hook.PluginHook.HookType;
import java.util.HashMap;
import java.util.Map;

public class SlackNotifierPlugin implements PluginHook {

    private String webhookUrl;
    private String channel;

    @Override
    public HookType[] getHookTypes() {
        return new HookType[]{
            HookType.TEST_EXECUTION_COMPLETED,
            HookType.COVERAGE_CALCULATED
        };
    }

    @Override
    public void initialize(Map<String, String> config) {
        this.webhookUrl = config.get("webhookUrl");
        this.channel = config.get("channel");
    }

    @Override
    public HookResult onTestExecutionCompleted(HookContext context) {
        String testId = (String) context.get("testId");
        String result = (String) context.get("result");
        Long duration = (Long) context.get("duration");

        String message = String.format(
            "Test %s %s in %dms",
            testId, result, duration
        );

        sendSlackMessage(message);
        return HookResult.success("Notification sent");
    }

    @Override
    public HookResult onCoverageCalculated(HookContext context) {
        Double coverage = (Double) context.get("coveragePercentage");
        String projectId = (String) context.get("projectId");

        String message = String.format(
            "Coverage for %s: %.1f%%",
            projectId, coverage
        );

        sendSlackMessage(message);
        return HookResult.success("Coverage notification sent");
    }

    private void sendSlackMessage(String message) {
        // Implement Slack webhook integration
    }

    @Override
    public void destroy() {
        this.webhookUrl = null;
        this.channel = null;
    }
}
```

### manifest.json
```json
{
  "pluginId": "slack-notifier",
  "name": "Slack Notifier",
  "version": "1.0.0",
  "description": "Sends test results to Slack",
  "author": "Example Team",
  "vendor": "Example Corp",
  "entryPoint": "com.example.slacknotify.SlackNotifierPlugin",
  "permissions": ["READ_TESTS", "READ_COVERAGE"],
  "hooks": [
    "TEST_EXECUTION_COMPLETED",
    "COVERAGE_CALCULATED"
  ]
}
```

## Plugin Lifecycle

```
    ┌──────────┐
    │  PENDING │
    └────┬─────┘
         │ install
         ▼
    ┌──────────┐
    │ INSTALLED│◄──────┐
    └────┬─────┘       │
         │ enable      │ error
         ▼             │
    ┌──────────┐       │
    │ ENABLED  │───────┘
    └────┬─────┘
         │ disable
         ▼
    ┌──────────┐
    │ DISABLED │
    └────┬─────┘
         │ uninstall
         ▼
    ┌──────────┐
    │ REMOVED  │
    └──────────┘
```

## Error Handling

Plugins should always return appropriate HookResult values:

```java
// Success with message
return HookResult.success("Action completed");

// Success with data
Map<String, Object> data = new HashMap<>();
data.put("processed", count);
return HookResult.success(data);

// Failure with reason
return HookResult.failure("Precondition not met");
```

## Configuration

Plugins receive configuration through the initialize method:

```java
@Override
public void initialize(Map<String, String> config) {
    this.apiKey = config.get("apiKey");
    this.endpoint = config.get("endpoint");
    this.debug = Boolean.parseBoolean(config.getOrDefault("debug", "false"));
}
```

Configuration values can be set in the plugin manifest or through the admin interface.

## Summary

The Plugin Extensibility infrastructure enables:

- **Lifecycle Hooks**: React to test creation, execution, and coverage events
- **Safe Execution**: All plugins run in a sandboxed environment
- **REST Management**: Full CRUD operations for plugins
- **Audit Trail**: Complete logging of plugin activities
- **Typed Payloads**: Type-safe context objects with validation helpers
