package com.jira.workflow.engine.script;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.HostAccess;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class JdcTestApi {

    private final List<TestResult> results = new CopyOnWriteArrayList<>();
    private int passed = 0;
    private int failed = 0;

    @HostAccess.Export
    public void assertTrue(boolean condition, String message) {
        if (condition) {
            passed++;
            results.add(new TestResult("PASS", message, null));
        } else {
            failed++;
            results.add(new TestResult("FAIL", message, "Expected true but got false"));
        }
    }

    @HostAccess.Export
    public void assertEquals(Object expected, Object actual, String message) {
        boolean eq = Objects.equals(expected, actual) ||
                     (expected != null && expected.toString().equals(String.valueOf(actual)));
        if (eq) {
            passed++;
            results.add(new TestResult("PASS", message, null));
        } else {
            failed++;
            results.add(new TestResult("FAIL", message,
                    "Expected [" + expected + "] but got [" + actual + "]"));
        }
    }

    @HostAccess.Export
    public void assertEquals(Object expected, Object actual) {
        assertEquals(expected, actual, "assertEquals");
    }

    @HostAccess.Export
    public void assertNotNull(Object value, String message) {
        if (value != null) {
            passed++;
            results.add(new TestResult("PASS", message, null));
        } else {
            failed++;
            results.add(new TestResult("FAIL", message, "Expected non-null value"));
        }
    }

    @HostAccess.Export
    public void assertNull(Object value, String message) {
        if (value == null) {
            passed++;
            results.add(new TestResult("PASS", message, null));
        } else {
            failed++;
            results.add(new TestResult("FAIL", message, "Expected null but got [" + value + "]"));
        }
    }

    @HostAccess.Export
    public void assertContains(String haystack, String needle, String message) {
        if (haystack != null && haystack.contains(needle)) {
            passed++;
            results.add(new TestResult("PASS", message, null));
        } else {
            failed++;
            results.add(new TestResult("FAIL", message,
                    "Expected string to contain [" + needle + "]"));
        }
    }

    @HostAccess.Export
    public void fail(String message) {
        failed++;
        results.add(new TestResult("FAIL", message, "Explicit failure"));
    }

    @HostAccess.Export
    public int getPassed() {
        return passed;
    }

    @HostAccess.Export
    public int getFailed() {
        return failed;
    }

    @HostAccess.Export
    public int getTotal() {
        return passed + failed;
    }

    @HostAccess.Export
    public boolean allPassed() {
        return failed == 0 && passed > 0;
    }

    public List<Map<String, Object>> getResultsAsMap() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (TestResult r : results) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("status", r.status());
            m.put("message", r.message());
            if (r.detail() != null) m.put("detail", r.detail());
            list.add(m);
        }
        return list;
    }

    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", getTotal());
        summary.put("passed", passed);
        summary.put("failed", failed);
        summary.put("allPassed", allPassed());
        summary.put("results", getResultsAsMap());
        return summary;
    }

    public record TestResult(String status, String message, String detail) {}
}
