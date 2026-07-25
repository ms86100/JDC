package com.jira.workflow.engine.script;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class ScriptTracer {

    private final boolean enabled;
    private final List<TraceEntry> entries = new CopyOnWriteArrayList<>();
    private final long startTime;

    public ScriptTracer(boolean enabled) {
        this.enabled = enabled;
        this.startTime = System.currentTimeMillis();
    }

    public void trace(String apiCall, String target, long durationMs, boolean success, String detail) {
        if (!enabled) return;
        entries.add(new TraceEntry(
                System.currentTimeMillis() - startTime,
                apiCall, target, durationMs, success, detail
        ));
    }

    public void traceApiCall(String api, String method, long startMs) {
        if (!enabled) return;
        long duration = System.currentTimeMillis() - startMs;
        entries.add(new TraceEntry(
                System.currentTimeMillis() - startTime,
                api + "." + method, null, duration, true, null
        ));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<TraceEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public int getApiCallCount() {
        return entries.size();
    }

    public long getTotalApiTime() {
        return entries.stream().mapToLong(TraceEntry::durationMs).sum();
    }

    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalApiCalls", entries.size());
        summary.put("totalApiTimeMs", getTotalApiTime());
        summary.put("totalElapsedMs", System.currentTimeMillis() - startTime);

        Map<String, Long> byApi = new LinkedHashMap<>();
        for (TraceEntry e : entries) {
            String api = e.apiCall().contains(".") ? e.apiCall().substring(0, e.apiCall().indexOf('.')) : e.apiCall();
            byApi.merge(api, 1L, Long::sum);
        }
        summary.put("callsByApi", byApi);

        long failures = entries.stream().filter(e -> !e.success()).count();
        summary.put("failedCalls", failures);

        return summary;
    }

    public List<Map<String, Object>> getEntriesAsMap() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (TraceEntry e : entries) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("offsetMs", e.offsetMs());
            m.put("apiCall", e.apiCall());
            if (e.target() != null) m.put("target", e.target());
            m.put("durationMs", e.durationMs());
            m.put("success", e.success());
            if (e.detail() != null) m.put("detail", e.detail());
            result.add(m);
        }
        return result;
    }

    public record TraceEntry(
            long offsetMs,
            String apiCall,
            String target,
            long durationMs,
            boolean success,
            String detail
    ) {}
}
