package com.jira.migration.dc;

import com.jira.migration.parser.JiraDcXmlParser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JiraDcConflictResolutionApplierTest {

    @Test
    void applySkipEntities_removesSkippedKeys() {
        var entities = List.of(
                entity("Issue", "PROJ-1"),
                entity("Issue", "PROJ-2"));
        var options = Map.<String, Object>of(
                "conflictResolutions",
                List.of(Map.of("entityKey", "PROJ-1", "action", "SKIP_ENTITY")));
        var filtered = JiraDcConflictResolutionApplier.applySkipEntities(entities, options);
        assertEquals(1, filtered.size());
        assertEquals("PROJ-2", filtered.get(0).getEntityKey());
    }

    @Test
    void applyFieldOverrides_setsFieldValues() {
        var e = entity("Issue", "PROJ-1");
        e.getFields().put("summary", "old");
        var options = Map.<String, Object>of(
                "conflictResolutions",
                List.of(Map.of(
                        "entityKey", "PROJ-1",
                        "field", "summary",
                        "action", "OVERRIDE_VALUE",
                        "overrideValue", "new")));
        JiraDcConflictResolutionApplier.applyFieldOverrides(List.of(e), options);
        assertEquals("new", e.getFields().get("summary"));
    }

    private static JiraDcXmlParser.ParsedEntity entity(String type, String key) {
        JiraDcXmlParser.ParsedEntity e = new JiraDcXmlParser.ParsedEntity();
        e.setEntityType(type);
        e.setEntityKey(key);
        e.setFields(new java.util.HashMap<>());
        return e;
    }
}
