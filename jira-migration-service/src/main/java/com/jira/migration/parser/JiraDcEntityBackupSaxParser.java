package com.jira.migration.parser;

import com.jira.migration.exception.MigrationException;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Streaming SAX parser for {@code <JiraDcBackup><Entity>} exports.
 */
public final class JiraDcEntityBackupSaxParser {

    private JiraDcEntityBackupSaxParser() {
    }

    public static List<JiraDcXmlParser.ParsedEntity> parse(Path xmlPath) {
        try (InputStream in = Files.newInputStream(xmlPath)) {
            return parse(in);
        } catch (Exception e) {
            throw new MigrationException("Failed to stream Entity backup: " + e.getMessage(), e);
        }
    }

    public static List<JiraDcXmlParser.ParsedEntity> parse(InputStream in) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            EntityBackupHandler handler = new EntityBackupHandler();
            factory.newSAXParser().parse(in, handler);
            return handler.build();
        } catch (Exception e) {
            throw new MigrationException("Failed to parse Entity backup stream: " + e.getMessage(), e);
        }
    }

    private static final class EntityBackupHandler extends DefaultHandler {
        private final List<JiraDcXmlParser.ParsedEntity> entities = new ArrayList<>();

        private boolean inEntity;
        private String entityName;
        private String currentFieldName;
        private final Map<String, String> fieldValues = new HashMap<>();
        private final StringBuilder text = new StringBuilder();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            String name = el(localName, qName);
            text.setLength(0);
            if ("Entity".equalsIgnoreCase(name)) {
                inEntity = true;
                entityName = null;
                fieldValues.clear();
            } else if (inEntity && "name".equalsIgnoreCase(name)) {
                currentFieldName = null;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            text.append(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            String name = el(localName, qName);
            String body = text.toString().trim();

            if (inEntity) {
                if ("entityName".equalsIgnoreCase(name)) {
                    entityName = body;
                } else if ("name".equalsIgnoreCase(name)) {
                    currentFieldName = body;
                } else if ("value".equalsIgnoreCase(name) && currentFieldName != null) {
                    fieldValues.put(currentFieldName, body);
                    currentFieldName = null;
                } else if ("Entity".equalsIgnoreCase(name)) {
                    if (entityName != null) {
                        entities.add(toParsed(entityName, fieldValues));
                    }
                    inEntity = false;
                }
            }
            text.setLength(0);
        }

        List<JiraDcXmlParser.ParsedEntity> build() {
            return entities;
        }

        private static JiraDcXmlParser.ParsedEntity toParsed(String entityName, Map<String, String> fields) {
            JiraDcXmlParser.ParsedEntity entity = new JiraDcXmlParser.ParsedEntity();
            entity.setEntityType(entityName);
            entity.setFields(new HashMap<>(fields));
            entity.setEntityKey(JiraDcParsedEntityKeys.generate(entity));
            return entity;
        }

        private static String el(String localName, String qName) {
            if (localName != null && !localName.isBlank()) {
                return localName;
            }
            return qName != null ? qName : "";
        }
    }
}
