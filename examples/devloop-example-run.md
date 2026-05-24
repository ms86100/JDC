# Autonomous DevLoop — Example Run
## Scenario: Implementing User Preferences API

---

## INITIAL STATE

**Feature file** (`feature.md`):
```
Endpoint: GET /api/users/{id}/preferences
Returns: { "theme": "dark", "notifications": true, "language": "en" }
```

**Current code state:**
- UserController.java has no `getPreferences()` method
- Service layer missing `getUserPreferences()` method
- Frontend expecting this endpoint for profile settings

---

## ITERATION 1

### Step 1: BUILD
```
$ python autonomous-devloop.py
╔══════════════════════════════════════════════════════════════╗
║       AUTONOMOUS DEVLOOP — JIRA PLATFORM                   ║
║       Build → Start → Monitor → Fix → Repeat                ║
╚══════════════════════════════════════════════════════════════╝

  BUILDING MISSING JARs
  Building 3 service(s)...
  Building jira-user-service...
  ✓ jira-user-service
  Building jira-gateway...
  ✓ jira-gateway
  Building jira-frontend...
  ✓ jira-frontend
  BUILD SUCCESS
```

### Step 2: START SERVICES
```
  ─────────────────────────────────────────────────────────────
  STEP 2: START SERVICES
  ─────────────────────────────────────────────────────────────
  Starting 14 backend services...

  WAVE 1: Backend Services
    [START] auth-service (PID: 15284)
    [START] user-service (PID: 15285)
    [START] project-service (PID: 15286)
    ...

  Waiting for backend health checks (60s)...
    user-service       ✓ (port 8082, 8.2s)
    auth-service       ✓ (port 8081, 6.1s)
    project-service    ✗ health check failed after 60s

  WAVE 2: Gateway
    [START] gateway (PID: 15299)
    gateway            ✓ (port 8080, 12.3s)

  WAVE 3: Frontend
    [START] frontend (PID: 15312)
    frontend           ✓ (port 3000, 8.0s)

  SERVICES STARTED: 13 healthy
```

### Step 3: MONITOR (30s observation)
```
  ─────────────────────────────────────────────────────────────
  STEP 3: MONITOR (30s observation window)
  ─────────────────────────────────────────────────────────────
  Scanning logs for errors...

  Log errors captured: 3
```

### Step 4: ERROR ANALYSIS
```
  ─────────────────────────────────────────────────────────────
  STEP 4: ERROR ANALYSIS
  ─────────────────────────────────────────────────────────────

  ==========================
  AUTONOMOUS DEVLOOP — FIX REQUEST
  ==========================
  Generated: 2026-05-23T14:30:00

  ## ERROR SUMMARY
  Total Errors: 3
    [1] spring_error
    [1] http_500
    [1] connection_refused

  ## ROOT CAUSES (by service)

  ### user-service
    Caused by: org.springframework.beans.factory.NoSuchBeanDefinitionException:
      No bean named 'userPreferencesService' available

  ### project-service
    Connection refused - check if database is running

  ## DETAILED ERRORS
  ------------------------------------------------------------------------
  ## Spring Framework Errors (1 errors)
  ------------------------------------------------------------------------

    Service: user-service
      → NoSuchBeanDefinitionException: No bean named 'userPreferencesService' available
      → at org.springframework.beans.factory.support.DefaultListableBeanFactory
      → .doGetBean(DefaultListableBeanFactory.java:604)
```

### Step 5: FIX REQUEST
```
  ══════════════════════════════════════════════════════════════
  STEP 5: REQUESTING AI FIX
  ══════════════════════════════════════════════════════════════

  Fix request written to: .devloop-fix-request.md

  IMPORTANT: Open a Claude Code session and run:
  $ claude --no-input < .devloop-fix-request.md

  Awaiting AI response...
```

**Generated fix request file** (`.devloop-fix-request.md`):
```markdown
# AUTONOMOUS DEVLOOP — FIX REQUEST
Generated: 2026-05-23T14:30:00

## ERROR SUMMARY
Total Errors: 3
  [1] Spring bean not found: userPreferencesService
  [1] HTTP 500 on GET /api/users/1/preferences
  [1] project-service database connection issue

## ROOT CAUSES (by service)

### user-service
  Caused by: NoSuchBeanDefinitionException: No bean named 'userPreferencesService'

### project-service
  Connection refused - check if database is running

## Instructions:
1. Read the error details above
2. Read the feature requirements: feature.md
3. Analyze the root causes
4. Make minimum necessary fixes
5. DO NOT refactor unrelated code
6. After fixing, output:
{
  "fixed": ["list of fixes"],
  "files_modified": ["list of files"]
}
```

---

## AI IMPLEMENTS FIXES

**AI reads `.devloop-fix-request.md`, then:**

1. Creates `UserPreferencesService.java`:
```java
@Service
public class UserPreferencesService {
    public UserPreferences getPreferences(Long userId) {
        // Implementation
    }
}
```

2. Adds to `UserController.java`:
```java
@GetMapping("/{id}/preferences")
public ResponseEntity<UserPreferences> getPreferences(@PathVariable Long id) {
    return ResponseEntity.ok(preferencesService.getPreferences(id));
}
```

3. Fixes `project-service` database config in `application.yml`

---

## ITERATION 2

### Step 1: BUILD
```
  Building 2 service(s)...
  Building jira-user-service...
  ✓ jira-user-service
  Building jira-project-service...
  ✓ jira-project-service

  BUILD SUCCESS
```

### Step 2: START
```
  Services started: 14 healthy
```

### Step 3: MONITOR
```
  Log errors captured: 0

  No errors detected — system appears healthy!
```

### Step 4: ACCEPTANCE CHECK
```
  ALL ACCEPTANCE CRITERIA MET!

  ✓ Endpoint compiles without errors
  ✓ Service starts without exceptions
  ✓ API returns 200 with correct JSON structure
  ✓ No white screen in frontend
  ✓ Backend logs show no errors
```

### Result
```
  ══════════════════════════════════════════════════════════════
  AUTONOMOUS DEVLOOP — FINAL REPORT
  ══════════════════════════════════════════════════════════════
  Total iterations: 2
  Feature: feature.md
  Status: SUCCESS — All criteria met
  ══════════════════════════════════════════════════════════════
```

---

## WHAT YOU DID:
1. Ran: `python autonomous-devloop.py --feature feature.md`
2. Waited while the system iterated automatically
3. The AI read `.devloop-fix-request.md` and implemented fixes
4. System verified success automatically

## WHAT YOU DIDN'T DO:
- Copy paste error logs manually
- Run `mvn compile` yourself
- Kill ports manually
- Restart services yourself
- Open browser and check manually