package com.avionics_systems.migration.dc;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LegacyDcParitySummaryBuilderTest {

    @Test
    void build_passWhenHighCoverageNoFailures() {
        Map<String, Object> summary = LegacyDcParitySummaryBuilder.build(
                100, 100, 0, 5, 2, 1024, 3, 12,
                "ENTITIES_XML", 10, Map.of("Issue", 50), false, false);
        assertEquals("PASS", summary.get("parityStatus"));
        assertEquals(100.0, summary.get("coveragePercent"));
        assertEquals(100, summary.get("entitiesSucceeded"));
    }

    @Test
    void build_failWhenFailuresPresent() {
        Map<String, Object> summary = LegacyDcParitySummaryBuilder.build(
                10, 10, 3, 0, 0, 0, 0, 0,
                "RSS_092", null, Map.of(), false, true);
        assertEquals("FAIL", summary.get("parityStatus"));
        assertTrue((Boolean) summary.get("stubDownstream"));
    }
}
