package com.avionics_systems.migration.parser;



import com.avionics_systems.migration.exception.MigrationException;

import org.xml.sax.Attributes;

import org.xml.sax.helpers.DefaultHandler;



import javax.xml.parsers.SAXParserFactory;

import java.io.InputStream;

import java.nio.charset.StandardCharsets;

import java.nio.file.Files;

import java.nio.file.Path;

import java.util.*;



/**

 * SAX parser for native Legacy DC {@code entity-engine-xml} exports.

 */

public final class LegacyDcEntitiesXmlParser {



    private static final int MAX_ENTITY_EXPANSION_DEPTH = 20;



    private LegacyDcEntitiesXmlParser() {

    }



    public static List<LegacyDcXmlParser.ParsedEntity> parse(Path xmlPath) {

        try (InputStream in = Files.newInputStream(xmlPath)) {

            return parse(in);

        } catch (Exception e) {

            throw new MigrationException("Failed to parse entities.xml: " + e.getMessage(), e);

        }

    }



    public static List<LegacyDcXmlParser.ParsedEntity> parse(String xmlContent) {

        return parse(new java.io.ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));

    }



    public static List<LegacyDcXmlParser.ParsedEntity> parse(InputStream inputStream) {

        try {

            SAXParserFactory factory = SAXParserFactory.newInstance();

            factory.setNamespaceAware(false);

            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);

            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            try {

                factory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);

            } catch (Exception ignored) {

                // JDK may not expose secure-processing on all parsers

            }



            EntitiesHandler handler = new EntitiesHandler();

            factory.newSAXParser().parse(inputStream, handler);

            return handler.build();

        } catch (Exception e) {

            throw new MigrationException("Failed to parse entities.xml stream: " + e.getMessage(), e);

        }

    }



    private static final class EntitiesHandler extends DefaultHandler {

        private final List<LegacyDcXmlParser.ParsedEntity> entities = new ArrayList<>();

        private final Set<String> projects = new HashSet<>();

        private final Map<String, String> issueIdToKey = new HashMap<>();



        private String currentElement;

        private final Map<String, String> currentAttrs = new HashMap<>();

        private final StringBuilder text = new StringBuilder();



        private Map<String, String> activeChangeGroup;

        private int depth;



        @Override

        public void startElement(String uri, String localName, String qName, Attributes attributes) {

            depth++;

            if (depth > MAX_ENTITY_EXPANSION_DEPTH) {

                throw new MigrationException("XML nesting exceeds max depth " + MAX_ENTITY_EXPANSION_DEPTH);

            }

            currentElement = localName != null && !localName.isBlank() ? localName : qName;

            currentAttrs.clear();

            text.setLength(0);

            for (int i = 0; i < attributes.getLength(); i++) {

                currentAttrs.put(attributes.getLocalName(i), attributes.getValue(i));

            }

            if ("ChangeGroup".equals(currentElement)) {

                activeChangeGroup = new HashMap<>(currentAttrs);

                activeChangeGroup.put("issue", first(currentAttrs, "issue", "issueId"));

            }

        }



        @Override

        public void characters(char[] ch, int start, int length) {

            text.append(ch, start, length);

        }



        @Override

        public void endElement(String uri, String localName, String qName) {

            String name = localName != null && !localName.isBlank() ? localName : qName;

            String body = text.toString().trim();



            switch (name) {

                case "Issue" -> emitIssue(currentAttrs, body);

                case "Action" -> emitComment(currentAttrs, body);

                case "FileAttachment" -> emitAttachment(currentAttrs);

                case "ChangeItem" -> emitChangeItem(currentAttrs);

                case "CustomFieldValue" -> emitCustomFieldValue(currentAttrs, body);

                case "IssueLink" -> emitIssueLink(currentAttrs);

                case "Label" -> emitLabel(currentAttrs);

                case "Version" -> emitVersion(currentAttrs);

                case "Component" -> emitComponent(currentAttrs);

                case "User" -> emitUser(currentAttrs);

                case "Worklog" -> emitWorklog(currentAttrs, body);

                case "Watcher" -> emitWatcher(currentAttrs);

                case "Vote" -> emitVote(currentAttrs);

                case "IssueType", "Status", "Priority", "Resolution" -> emitReferenceEntity(name, currentAttrs);

                default -> {
                    if (isPluginElement(name)) {
                        emitPluginEntity(name, currentAttrs, body);
                    }
                }

            }

            if ("ChangeGroup".equals(name)) {

                activeChangeGroup = null;

            }

            text.setLength(0);

            depth--;

        }



        private void emitIssue(Map<String, String> attrs, String body) {

            String projectKey = first(attrs, "projectKey", "project");

            String number = first(attrs, "number", "issueNumber");

            String id = attrs.get("id");

            if (projectKey == null) {

                return;

            }

            String issueKey = number != null ? projectKey + "-" + number : projectKey;

            if (id != null) {

                issueIdToKey.put(id, issueKey);

            }

            if (projects.add(projectKey)) {

                LegacyDcXmlParser.ParsedEntity project = new LegacyDcXmlParser.ParsedEntity();

                project.setEntityType("Project");

                Map<String, String> pf = new HashMap<>();

                pf.put("key", projectKey);

                pf.put("name", projectKey);

                project.setFields(pf);

                project.setEntityKey(projectKey);

                entities.add(project);

            }

            LegacyDcXmlParser.ParsedEntity issue = new LegacyDcXmlParser.ParsedEntity();

            String issueTypeName = first(attrs, "type", "issueType");
            boolean subTask = issueTypeName != null && issueTypeName.toLowerCase().contains("sub")
                    || first(attrs, "parent", "parentId", "parentIssue") != null;
            issue.setEntityType(subTask ? "SubTask" : "Issue");

            Map<String, String> fields = new HashMap<>(attrs);

            fields.put("project", projectKey);

            fields.put("issueKey", issueKey);

            if (id != null) {

                fields.put("id", id);

            }

            putIfPresent(fields, "summary", first(attrs, "summary"));

            putIfPresent(fields, "issueType", first(attrs, "type", "issueType"));

            putIfPresent(fields, "status", attrs.get("status"));

            putIfPresent(fields, "priority", attrs.get("priority"));

            putIfPresent(fields, "assignee", first(attrs, "assignee", "assigneeKey"));

            putIfPresent(fields, "reporter", first(attrs, "reporter", "creator"));

            putIfPresent(fields, "created", first(attrs, "created", "createdDate"));

            putIfPresent(fields, "updated", first(attrs, "updated", "updatedDate"));

            if (!body.isBlank()) {

                fields.putIfAbsent("description", body);

            }

            issue.setFields(fields);

            issue.setEntityKey(issueKey);

            entities.add(issue);

        }



        private void emitComment(Map<String, String> attrs, String body) {

            String issueRef = first(attrs, "issue", "issueId");

            if (issueRef == null) {

                return;

            }

            String issueKey = issueIdToKey.getOrDefault(issueRef, issueRef);

            LegacyDcXmlParser.ParsedEntity comment = new LegacyDcXmlParser.ParsedEntity();

            comment.setEntityType("Comment");

            Map<String, String> fields = new HashMap<>(attrs);

            fields.put("issueId", issueRef);

            fields.put("issue", issueKey);

            fields.put("issueKey", issueKey);

            fields.put("body", body);

            putIfPresent(fields, "created", first(attrs, "created", "createdDate"));

            putIfPresent(fields, "author", first(attrs, "author", "authorKey"));

            comment.setFields(fields);

            comment.setEntityKey(issueKey + ":comment:" + attrs.getOrDefault("id", UUID.randomUUID().toString()));

            entities.add(comment);

        }



        private void emitAttachment(Map<String, String> attrs) {

            String issueRef = first(attrs, "issue", "issueId");

            String issueKey = issueRef != null ? issueIdToKey.getOrDefault(issueRef, issueRef) : null;

            LegacyDcXmlParser.ParsedEntity att = new LegacyDcXmlParser.ParsedEntity();

            att.setEntityType("Attachment");

            Map<String, String> fields = new HashMap<>(attrs);

            if (issueRef != null) {

                fields.put("issueId", issueRef);

            }

            if (issueKey != null) {

                fields.put("issue", issueKey);

                fields.put("issueKey", issueKey);

            }

            fields.put("sourceAttachmentId", attrs.get("id"));

            fields.put("filename", first(attrs, "fileName", "filename", "name"));

            att.setFields(fields);

            att.setEntityKey("att-" + attrs.getOrDefault("id", UUID.randomUUID().toString()));

            entities.add(att);

        }



        private void emitChangeItem(Map<String, String> attrs) {

            LegacyDcXmlParser.ParsedEntity history = new LegacyDcXmlParser.ParsedEntity();

            history.setEntityType("History");

            Map<String, String> fields = new HashMap<>();

            if (activeChangeGroup != null) {

                fields.putAll(activeChangeGroup);

            }

            fields.putAll(attrs);

            fields.put("field", first(attrs, "field", "fieldname", "fieldName"));

            fields.put("old", first(attrs, "oldvalue", "oldValue", "oldstring", "oldString"));

            fields.put("new", first(attrs, "newvalue", "newValue", "newstring", "newString"));

            String issueRef = first(fields, "issue", "issueId");

            if (issueRef != null) {

                fields.put("issueKey", issueIdToKey.getOrDefault(issueRef, issueRef));

            }

            fields.put("sourceHistoryId", activeChangeGroup != null

                    ? activeChangeGroup.get("id") : attrs.get("id"));

            history.setFields(fields);

            history.setEntityKey("history-" + fields.getOrDefault("sourceHistoryId", "")

                    + "-" + fields.getOrDefault("field", "f") + "-" + attrs.getOrDefault("id", "0"));

            entities.add(history);

        }



        private void emitCustomFieldValue(Map<String, String> attrs, String body) {

            String issueRef = first(attrs, "issue", "issueId");

            String customField = first(attrs, "customfield", "customFieldId", "customfieldId");

            if (issueRef == null || customField == null) {

                return;

            }

            LegacyDcXmlParser.ParsedEntity cf = new LegacyDcXmlParser.ParsedEntity();

            cf.setEntityType("CustomField");

            Map<String, String> fields = new HashMap<>(attrs);

            fields.put("issueId", issueRef);

            fields.put("issueKey", issueIdToKey.getOrDefault(issueRef, issueRef));

            fields.put("customFieldId", customField.startsWith("customfield_")

                    ? customField : "customfield_" + customField);

            fields.put("value", body.isBlank() ? first(attrs, "stringvalue", "textvalue") : body);

            cf.setFields(fields);

            cf.setEntityKey(fields.get("issueKey") + ":cf:" + customField);

            entities.add(cf);

        }



        private void emitIssueLink(Map<String, String> attrs) {

            LegacyDcXmlParser.ParsedEntity link = new LegacyDcXmlParser.ParsedEntity();

            link.setEntityType("IssueLink");

            Map<String, String> fields = new HashMap<>(attrs);

            String source = first(attrs, "source", "sourceId", "outward");

            String dest = first(attrs, "destination", "destinationId", "inward");

            if (source != null) {

                fields.put("sourceIssueKey", issueIdToKey.getOrDefault(source, source));

            }

            if (dest != null) {

                fields.put("targetIssueKey", issueIdToKey.getOrDefault(dest, dest));

            }

            fields.put("linkType", first(attrs, "linkType", "linktype", "type"));

            link.setFields(fields);

            link.setEntityKey((fields.getOrDefault("sourceIssueKey", "?"))

                    + "->" + fields.getOrDefault("targetIssueKey", "?"));

            entities.add(link);

        }



        private void emitLabel(Map<String, String> attrs) {

            String issueRef = first(attrs, "issue", "issueId");

            String label = first(attrs, "label", "name");

            if (issueRef == null || label == null) {

                return;

            }

            LegacyDcXmlParser.ParsedEntity entity = new LegacyDcXmlParser.ParsedEntity();

            entity.setEntityType("Label");

            Map<String, String> fields = new HashMap<>();

            fields.put("issueId", issueRef);

            fields.put("issueKey", issueIdToKey.getOrDefault(issueRef, issueRef));

            fields.put("label", label);

            entity.setFields(fields);

            entity.setEntityKey(fields.get("issueKey") + ":label:" + label);

            entities.add(entity);

        }



        private void emitVersion(Map<String, String> attrs) {

            emitReferenceEntity("Version", attrs);

        }



        private void emitComponent(Map<String, String> attrs) {

            emitReferenceEntity("Component", attrs);

        }



        private void emitUser(Map<String, String> attrs) {

            LegacyDcXmlParser.ParsedEntity user = new LegacyDcXmlParser.ParsedEntity();

            user.setEntityType("User");

            Map<String, String> fields = new HashMap<>(attrs);

            fields.put("userKey", first(attrs, "userKey", "key", "name"));

            fields.put("lowerUserName", first(attrs, "lowerUserName", "userName", "username"));

            user.setFields(fields);

            user.setEntityKey(fields.getOrDefault("userKey", fields.getOrDefault("lowerUserName", "user")));

            entities.add(user);

        }



        private void emitWorklog(Map<String, String> attrs, String body) {
            String issueRef = first(attrs, "issue", "issueId");
            if (issueRef == null) {
                return;
            }
            String issueKey = issueIdToKey.getOrDefault(issueRef, issueRef);
            LegacyDcXmlParser.ParsedEntity worklog = new LegacyDcXmlParser.ParsedEntity();
            worklog.setEntityType("Worklog");
            Map<String, String> fields = new HashMap<>(attrs);
            fields.put("issueId", issueRef);
            fields.put("issueKey", issueKey);
            fields.put("issue", issueKey);
            fields.put("comment", body);
            putIfPresent(fields, "timeSpent", first(attrs, "timeSpent", "timespent"));
            putIfPresent(fields, "started", first(attrs, "started", "startDate", "created"));
            putIfPresent(fields, "author", first(attrs, "author", "authorKey"));
            worklog.setFields(fields);
            worklog.setEntityKey(issueKey + ":worklog:" + attrs.getOrDefault("id", UUID.randomUUID().toString()));
            entities.add(worklog);
        }

        private void emitWatcher(Map<String, String> attrs) {
            String issueRef = first(attrs, "issue", "issueId");
            if (issueRef == null) {
                return;
            }
            LegacyDcXmlParser.ParsedEntity entity = new LegacyDcXmlParser.ParsedEntity();
            entity.setEntityType("Watcher");
            Map<String, String> fields = new HashMap<>(attrs);
            fields.put("issueId", issueRef);
            fields.put("issueKey", issueIdToKey.getOrDefault(issueRef, issueRef));
            entity.setFields(fields);
            entity.setEntityKey(fields.get("issueKey") + ":watcher:" + attrs.getOrDefault("id", "0"));
            entities.add(entity);
        }

        private void emitVote(Map<String, String> attrs) {
            String issueRef = first(attrs, "issue", "issueId");
            if (issueRef == null) {
                return;
            }
            LegacyDcXmlParser.ParsedEntity entity = new LegacyDcXmlParser.ParsedEntity();
            entity.setEntityType("Vote");
            Map<String, String> fields = new HashMap<>(attrs);
            fields.put("issueId", issueRef);
            fields.put("issueKey", issueIdToKey.getOrDefault(issueRef, issueRef));
            entity.setFields(fields);
            entity.setEntityKey(fields.get("issueKey") + ":vote:" + attrs.getOrDefault("id", "0"));
            entities.add(entity);
        }

        private void emitReferenceEntity(String type, Map<String, String> attrs) {

            LegacyDcXmlParser.ParsedEntity ref = new LegacyDcXmlParser.ParsedEntity();

            ref.setEntityType(type);

            ref.setFields(new HashMap<>(attrs));

            ref.setEntityKey(type + "-" + attrs.getOrDefault("id", attrs.getOrDefault("name", UUID.randomUUID().toString())));

            entities.add(ref);

        }

        private static boolean isPluginElement(String elementName) {
            if (elementName == null) {
                return false;
            }
            return elementName.startsWith("GreenHopper")
                    || elementName.startsWith("AO_")
                    || elementName.startsWith("AO")
                    || "Sprint".equals(elementName)
                    || "RapidView".equals(elementName)
                    || elementName.contains("pyxis")
                    || elementName.contains("tempo");
        }

        private void emitPluginEntity(String pluginType, Map<String, String> attrs, String body) {
            LegacyDcXmlParser.ParsedEntity plugin = new LegacyDcXmlParser.ParsedEntity();
            plugin.setEntityType("PluginEntity");
            Map<String, String> fields = new HashMap<>(attrs);
            fields.put("pluginType", pluginType);
            if (!body.isBlank()) {
                fields.put("value", body);
            }
            String issueRef = first(attrs, "issue", "issueId");
            if (issueRef != null) {
                fields.put("issueId", issueRef);
                fields.put("issueKey", issueIdToKey.getOrDefault(issueRef, issueRef));
            }
            fields.put("field", first(attrs, "field", "customfield", "customFieldId"));
            plugin.setFields(fields);
            plugin.setEntityKey("plugin-" + pluginType + "-" + attrs.getOrDefault("id", UUID.randomUUID().toString()));
            entities.add(plugin);
        }



        List<LegacyDcXmlParser.ParsedEntity> build() {

            return entities;

        }



        private static String first(Map<String, String> m, String... keys) {

            for (String k : keys) {

                if (m.containsKey(k) && m.get(k) != null && !m.get(k).isBlank()) {

                    return m.get(k);

                }

            }

            return null;

        }



        private static void putIfPresent(Map<String, String> target, String key, String value) {

            if (value != null && !value.isBlank()) {

                target.put(key, value);

            }

        }

    }

}

