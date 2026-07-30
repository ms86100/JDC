package com.avionics_systems.migration.parser;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.avionics_systems.migration.exception.MigrationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Parses {@code <LegacyDcBackup><Entity>...} exports into {@link LegacyDcXmlParser.ParsedEntity} records.
 */
public final class LegacyDcEntityBackupParser {

    private LegacyDcEntityBackupParser() {
    }

    public static List<LegacyDcXmlParser.ParsedEntity> parse(String xmlContent) {
        try {
            XmlMapper xmlMapper = new XmlMapper();
            xmlMapper.registerModule(new JavaTimeModule());
            xmlMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = xmlMapper.readValue(xmlContent, Map.class);
            List<LegacyDcXmlParser.ParsedEntity> entities = new ArrayList<>();
            for (Map<String, Object> entityMap : extractEntityNodes(parsed)) {
                if (entityMap.get("entityName") == null) {
                    continue;
                }
                entities.add(convertToParsedEntity(entityMap));
            }
            return entities;
        } catch (Exception e) {
            throw new MigrationException("Failed to parse Entity backup XML: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> extractEntityNodes(Map<String, Object> parsed) {
        Object entityNode = parsed.get("Entity");
        if (entityNode == null) {
            for (Object value : parsed.values()) {
                if (value instanceof Map<?, ?> nested) {
                    List<Map<String, Object>> fromNested = extractEntityNodes((Map<String, Object>) nested);
                    if (!fromNested.isEmpty()) {
                        return fromNested;
                    }
                }
            }
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        if (entityNode instanceof List<?> list) {
            for (Object e : list) {
                if (e instanceof Map<?, ?> m) {
                    out.add((Map<String, Object>) m);
                }
            }
        } else if (entityNode instanceof Map<?, ?> single) {
            out.add((Map<String, Object>) single);
        }
        return out;
    }

    private static LegacyDcXmlParser.ParsedEntity convertToParsedEntity(Map<String, Object> entityMap) {
        LegacyDcXmlParser.ParsedEntity entity = new LegacyDcXmlParser.ParsedEntity();
        entity.setEntityType((String) entityMap.get("entityName"));

        Map<String, String> fields = new HashMap<>();
        if (entityMap.containsKey("field")) {
            Object fieldObj = entityMap.get("field");
            if (fieldObj instanceof List<?> list) {
                for (Object f : list) {
                    if (f instanceof Map<?, ?> fieldMap) {
                        putField(fields, (Map<String, Object>) fieldMap);
                    }
                }
            } else if (fieldObj instanceof Map<?, ?> fieldMap) {
                putField(fields, (Map<String, Object>) fieldMap);
            }
        }

        entity.setFields(fields);
        entity.setEntityKey(LegacyDcParsedEntityKeys.generate(entity));
        return entity;
    }

    private static void putField(Map<String, String> fields, Map<String, Object> fieldMap) {
        String name = (String) fieldMap.get("name");
        Object value = fieldMap.get("value");
        if (name != null && value != null) {
            fields.put(name, value.toString());
        }
    }
}
