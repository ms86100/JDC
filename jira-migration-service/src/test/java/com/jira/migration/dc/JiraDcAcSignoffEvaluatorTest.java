package com.jira.migration.dc;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JiraDcAcSignoffEvaluatorTest {

    @Test
    void evaluate_returnsTenCriteria() {
        Map<String, Object> meta = Map.of(
                "slaProof", Map.of("slaMet", true, "slaTier", "SMALL", "durationMs", 1000, "maxAllowedMs", 120000),
                "historyReplayed", 5,
                "attachmentCount", 2);
        Map<String, Object> opts = Map.of("backupZip", true, "extractedBackupRoot", "/tmp/backup");
        Map<String, Object> signoff = JiraDcAcSignoffEvaluator.evaluate(meta, opts, "COMPLETED", 10, 0);
        @SuppressWarnings("unchecked")
        var criteria = (java.util.List<Map<String, Object>>) signoff.get("criteria");
        assertEquals(10, criteria.size());
        assertFalse((Boolean) signoff.get("formalSignoffComplete"));
    }
}
