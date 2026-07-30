package com.avionics_systems.migration.dc;

import com.avionics_systems.migration.parser.LegacyDcXmlParser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyDcConflictResolutionApplierTest {

    @Test
    void applySkipEntities_removesSkippedKeys() {
        var entities = List.of(
                entity("Issue", "PROJ-1"),
                entity("Issue", "PROJ-2"));
        var options = Map.<String, Object>of(
                "conflictResolutions",
                List.of(Map.of("entityKey", "PROJ-1", "action", "SKIP_ENTITY")));
        var filtered = LegacyDcConflictResolutionApplier.applySkipEntities(entities, options);
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
        LegacyDcConflictResolutionApplier.applyFieldOverrides(List.of(e), options);
        assertEquals("new", e.getFields().get("summary"));
    }

    private static LegacyDcXmlParser.ParsedEntity entity(String type, String key) {
        LegacyDcXmlParser.ParsedEntity e = new LegacyDcXmlParser.ParsedEntity();
        e.setEntityType(type);
        e.setEntityKey(key);
        e.setFields(new java.util.HashMap<>());
        return e;
    }
}
