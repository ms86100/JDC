package com.jira.migration.dc;

import com.jira.migration.parser.JiraDcEntitiesXmlParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SLA proof: parse-only always; live-tier benchmark when MIGRATION_SLA_LIVE=1.
 */
class JiraDcLiveImportSlaTest {

    @TempDir
    Path tempDir;

    @Test
    void slaProofBuilder_smallBatch_metWhenFast() {
        Map<String, Object> proof = JiraDcImportSlaProofBuilder.build(50, 30_000, 0, false, "LIVE_IMPORT_JOB");
        assertTrue((Boolean) proof.get("slaMet"));
        assertEquals("SMALL", proof.get("slaTier"));
    }

    @Test
    void slaProofBuilder_stubNeverCountsAsMet() {
        Map<String, Object> proof = JiraDcImportSlaProofBuilder.build(1000, 10_000, 0, true, "LIVE_IMPORT_JOB");
        assertFalse((Boolean) proof.get("slaMet"));
    }

    @Test
    void parseProof_1000Issues_underParseSla() throws Exception {
        Path file = tempDir.resolve("sla-1k.xml");
        StringBuilder xml = new StringBuilder("<entity-engine-xml>");
        for (int i = 1; i <= 1000; i++) {
            xml.append("<Issue id=\"").append(i)
                    .append("\" projectKey=\"SLA\" number=\"").append(i)
                    .append("\" summary=\"SLA ").append(i).append("\" status=\"Open\"/>");
        }
        xml.append("</entity-engine-xml>");
        Files.writeString(file, xml.toString());

        long start = System.currentTimeMillis();
        var entities = JiraDcEntitiesXmlParser.parse(file);
        long elapsed = System.currentTimeMillis() - start;

        Map<String, Object> proof = JiraDcImportSlaProofBuilder.buildParseProof(1000, elapsed);
        assertEquals(1000, entities.stream().filter(e -> "Issue".equals(e.getEntityType())).count());
        assertTrue((Boolean) proof.get("slaMet"), "Parse 1k in " + elapsed + "ms");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "MIGRATION_SLA_LIVE", matches = "1")
    void liveTier_benchmark_1000Issues_parseUnder1kSla() throws Exception {
        Path file = tempDir.resolve("sla-live-1k.xml");
        StringBuilder xml = new StringBuilder("<entity-engine-xml>");
        for (int i = 1; i <= 1000; i++) {
            xml.append("<Issue id=\"").append(i)
                    .append("\" projectKey=\"LIVE\" number=\"").append(i)
                    .append("\" summary=\"Live ").append(i).append("\" status=\"Open\"/>");
        }
        xml.append("</entity-engine-xml>");
        Files.writeString(file, xml.toString());

        long start = System.currentTimeMillis();
        JiraDcEntitiesXmlParser.parse(file);
        long elapsed = System.currentTimeMillis() - start;

        Map<String, Object> liveProof = JiraDcImportSlaProofBuilder.build(1000, elapsed, 0, false, "LIVE_IMPORT_BENCHMARK");
        assertTrue(elapsed < JiraDcImportSlaPolicy.LIVE_IMPORT_1K_MAX_MS,
                "1k parse benchmark took " + elapsed + "ms");
        assertEquals("1K", liveProof.get("slaTier"));
    }
}
