package com.avionics_systems.workflow.engine.script;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Script debugger supporting breakpoints, step-through, and variable inspection.
 * Works by injecting debug hooks into the script execution context.
 *
 * Usage from scripts:
 *   debugger.breakpoint("label");     // pause execution until resumed
 *   debugger.inspect("varName", val); // capture variable snapshot
 *   debugger.step();                  // pause at next statement
 *
 * Usage from controller/API:
 *   POST /api/workflow/scripts/debug/resume/{sessionId}   // resume paused script
 *   GET  /api/workflow/scripts/debug/state/{sessionId}     // get current state + snapshots
 */
@Slf4j
public class ScriptDebugger {

    @Getter
    private final String sessionId;
    private final boolean enabled;
    private final Set<String> breakpoints = ConcurrentHashMap.newKeySet();
    private final List<DebugSnapshot> snapshots = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Object> watchedVariables = new ConcurrentHashMap<>();
    private volatile boolean stepping = false;
    private volatile boolean paused = false;
    private volatile String pausedAt = null;
    private CountDownLatch resumeLatch = null;

    private static final Map<String, ScriptDebugger> activeSessions = new ConcurrentHashMap<>();

    public ScriptDebugger(String sessionId, boolean enabled) {
        this.sessionId = sessionId;
        this.enabled = enabled;
        if (enabled) {
            activeSessions.put(sessionId, this);
        }
    }

    public void breakpoint(String label) {
        if (!enabled) return;
        snapshots.add(new DebugSnapshot(
                System.currentTimeMillis(), "BREAKPOINT", label,
                new LinkedHashMap<>(watchedVariables)));
        doPause(label);
    }

    public void inspect(String name, Object value) {
        if (!enabled) return;
        watchedVariables.put(name, value != null ? value.toString() : "null");
        snapshots.add(new DebugSnapshot(
                System.currentTimeMillis(), "INSPECT", name + " = " + value,
                Map.of(name, value != null ? value.toString() : "null")));
    }

    public void step() {
        if (!enabled) return;
        stepping = true;
        doPause("step");
    }

    private void doPause(String location) {
        paused = true;
        pausedAt = location;
        resumeLatch = new CountDownLatch(1);
        log.info("Script debugger paused at '{}' (session {})", location, sessionId);
        try {
            if (!resumeLatch.await(60, TimeUnit.SECONDS)) {
                log.warn("Debug session {} timed out waiting for resume at '{}'", sessionId, location);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            paused = false;
            pausedAt = null;
        }
    }

    public void resume() {
        if (resumeLatch != null) {
            resumeLatch.countDown();
        }
    }

    public void cleanup() {
        activeSessions.remove(sessionId);
        if (resumeLatch != null) {
            resumeLatch.countDown();
        }
    }

    public boolean isPaused() {
        return paused;
    }

    public Map<String, Object> getState() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("sessionId", sessionId);
        state.put("enabled", enabled);
        state.put("paused", paused);
        state.put("pausedAt", pausedAt);
        state.put("stepping", stepping);
        state.put("breakpoints", new ArrayList<>(breakpoints));
        state.put("watchedVariables", new LinkedHashMap<>(watchedVariables));
        state.put("snapshotCount", snapshots.size());

        List<Map<String, Object>> snapshotList = new ArrayList<>();
        for (DebugSnapshot s : snapshots) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("timestamp", s.timestamp());
            m.put("type", s.type());
            m.put("label", s.label());
            m.put("variables", s.variables());
            snapshotList.add(m);
        }
        state.put("snapshots", snapshotList);
        return state;
    }

    public static ScriptDebugger getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }

    public static Map<String, Object> listActiveSessions() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", activeSessions.size());
        List<Map<String, Object>> sessions = new ArrayList<>();
        activeSessions.forEach((id, dbg) -> {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("sessionId", id);
            s.put("paused", dbg.isPaused());
            s.put("pausedAt", dbg.pausedAt);
            sessions.add(s);
        });
        result.put("sessions", sessions);
        return result;
    }

    public record DebugSnapshot(long timestamp, String type, String label, Map<String, Object> variables) {}
}
