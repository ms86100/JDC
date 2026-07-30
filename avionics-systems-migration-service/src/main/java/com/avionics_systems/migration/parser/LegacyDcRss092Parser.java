package com.avionics_systems.migration.parser;

import com.avionics_systems.migration.exception.MigrationException;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SAX parser for Legacy DC RSS 0.92 issue exports ({@code rss/channel/item}).
 */
public final class LegacyDcRss092Parser {

    private LegacyDcRss092Parser() {
    }

    public static List<LegacyDcXmlParser.ParsedEntity> parse(String xmlContent) {
        return parseStream(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
    }

    public static List<LegacyDcXmlParser.ParsedEntity> parsePath(Path xmlPath) {
        try (InputStream in = Files.newInputStream(xmlPath)) {
            return parseStream(in);
        } catch (Exception e) {
            throw new MigrationException("Failed to parse RSS from path: " + e.getMessage(), e);
        }
    }

    private static List<LegacyDcXmlParser.ParsedEntity> parseStream(InputStream inputStream) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            try {
                factory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
            } catch (Exception ignored) {
                // optional
            }

            RssHandler handler = new RssHandler();
            factory.newSAXParser().parse(inputStream, handler);
            return handler.buildEntities();
        } catch (Exception e) {
            throw new MigrationException("Failed to parse RSS 0.92 Legacy DC XML: " + e.getMessage(), e);
        }
    }

    private static final class ItemDraft {
        String issueKey;
        String issueId;
        String summary;
        String description;
        String issueType;
        String status;
        String priority;
        String assignee;
        String reporter;
        String created;
        String updated;
        String parentKey;
        final List<CustomFieldDraft> customFields = new ArrayList<>();
        final List<AttachmentDraft> attachments = new ArrayList<>();
        final List<CommentDraft> comments = new ArrayList<>();
        final List<String> labels = new ArrayList<>();
        final List<IssueLinkDraft> issueLinks = new ArrayList<>();
        final List<WorklogDraft> worklogs = new ArrayList<>();
        final List<HistoryDraft> changelog = new ArrayList<>();
    }

    private static final class IssueLinkDraft {
        String linkType;
        String outwardKey;
        String inwardKey;
    }

    private static final class WorklogDraft {
        String id;
        String author;
        String timeSpent;
        String started;
        String comment;
    }

    private static final class HistoryDraft {
        String field;
        String author;
        String created;
        String oldValue;
        String newValue;
    }

    private static final class CustomFieldDraft {
        String id;
        String name;
        final List<String> values = new ArrayList<>();
    }

    private static final class AttachmentDraft {
        String id;
        String name;
    }

    private static final class CommentDraft {
        String id;
        String body;
        String author;
        String created;
    }

    private static final class RssHandler extends DefaultHandler {
        private final List<ItemDraft> items = new ArrayList<>();
        private String channelTitle;

        private boolean inChannel;
        private boolean inItem;
        private ItemDraft current;

        private boolean inAttachments;
        private boolean inCustomFields;
        private boolean inComments;
        private boolean inCustomFieldValues;
        private boolean inLabels;
        private boolean inIssueLinks;
        private boolean inWorklogs;
        private boolean inChangelog;

        private IssueLinkDraft currentIssueLink;
        private WorklogDraft currentWorklog;
        private HistoryDraft currentHistory;

        private CustomFieldDraft currentCustomField;
        private AttachmentDraft currentAttachment;
        private CommentDraft currentComment;

        private final StringBuilder textBuffer = new StringBuilder();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            String name = elementName(localName, qName);
            textBuffer.setLength(0);

            switch (name) {
                case "channel" -> inChannel = true;
                case "item" -> {
                    inItem = true;
                    current = new ItemDraft();
                }
                case "attachments" -> inAttachments = true;
                case "customfields" -> inCustomFields = true;
                case "comments" -> inComments = true;
                case "labels" -> inLabels = true;
                case "issuelinks" -> inIssueLinks = true;
                case "worklogs" -> inWorklogs = true;
                case "changelog" -> inChangelog = true;
                case "issuelink" -> {
                    if (inIssueLinks && inItem) {
                        currentIssueLink = new IssueLinkDraft();
                        currentIssueLink.linkType = firstAttr(attributes, "type", "linktype");
                        currentIssueLink.outwardKey = firstAttr(attributes, "outwardKey", "outward", "source");
                        currentIssueLink.inwardKey = firstAttr(attributes, "inwardKey", "inward", "destination");
                    }
                }
                case "worklog" -> {
                    if (inWorklogs && inItem) {
                        currentWorklog = new WorklogDraft();
                        currentWorklog.id = attributes.getValue("id");
                        currentWorklog.author = firstAttr(attributes, "author", "authorKey");
                        currentWorklog.timeSpent = firstAttr(attributes, "timeSpent", "timespent");
                        currentWorklog.started = firstAttr(attributes, "started", "startDate");
                    }
                }
                case "change" -> {
                    if (inChangelog && inItem) {
                        currentHistory = new HistoryDraft();
                        currentHistory.field = firstAttr(attributes, "field", "fieldname");
                        currentHistory.author = firstAttr(attributes, "author", "authorKey");
                        currentHistory.created = firstAttr(attributes, "created", "createdDate");
                        currentHistory.oldValue = firstAttr(attributes, "old", "oldvalue", "oldString");
                        currentHistory.newValue = firstAttr(attributes, "new", "newvalue", "newString");
                    }
                }
                case "key" -> {
                    if (inItem && current != null) {
                        current.issueId = attributes.getValue("id");
                    }
                }
                case "customfield" -> {
                    if (inCustomFields && inItem) {
                        currentCustomField = new CustomFieldDraft();
                        currentCustomField.id = attributes.getValue("id");
                    }
                }
                case "attachment" -> {
                    if (inAttachments && inItem) {
                        currentAttachment = new AttachmentDraft();
                        currentAttachment.id = attributes.getValue("id");
                        currentAttachment.name = attributes.getValue("name");
                    }
                }
                case "comment" -> {
                    if (inComments && inItem) {
                        currentComment = new CommentDraft();
                        currentComment.id = attributes.getValue("id");
                    }
                }
                case "customfieldvalues" -> inCustomFieldValues = true;
                default -> { }
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            textBuffer.append(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            String name = elementName(localName, qName);
            String text = textBuffer.toString().trim();

            if (inItem && current != null) {
                switch (name) {
                    case "key" -> current.issueKey = text;
                    case "summary" -> current.summary = text;
                    case "description" -> current.description = text;
                    case "type", "issuetype" -> current.issueType = text;
                    case "status" -> current.status = text;
                    case "priority" -> current.priority = text;
                    case "assignee" -> current.assignee = text;
                    case "reporter", "creator" -> current.reporter = text;
                    case "created" -> {
                        if (currentComment != null) {
                            currentComment.created = text;
                        } else {
                            current.created = text;
                        }
                    }
                    case "updated" -> current.updated = text;
                    case "parent" -> current.parentKey = text;
                    case "author" -> {
                        if (currentComment != null) {
                            currentComment.author = text;
                        } else if (currentWorklog != null && currentWorklog.author == null) {
                            currentWorklog.author = text;
                        } else if (currentHistory != null && currentHistory.author == null) {
                            currentHistory.author = text;
                        }
                    }
                    case "label" -> {
                        if (inLabels && inItem && !text.isBlank()) {
                            current.labels.add(text);
                        }
                    }
                    case "issuelink" -> {
                        if (currentIssueLink != null) {
                            if (currentIssueLink.linkType == null) {
                                currentIssueLink.linkType = text;
                            }
                            current.issueLinks.add(currentIssueLink);
                            currentIssueLink = null;
                        }
                    }
                    case "worklog" -> {
                        if (currentWorklog != null) {
                            if (currentWorklog.comment == null || currentWorklog.comment.isBlank()) {
                                currentWorklog.comment = text;
                            }
                            current.worklogs.add(currentWorklog);
                            currentWorklog = null;
                        }
                    }
                    case "change" -> {
                        if (currentHistory != null) {
                            current.changelog.add(currentHistory);
                            currentHistory = null;
                        }
                    }
                    case "customfieldname" -> {
                        if (currentCustomField != null) {
                            currentCustomField.name = text;
                        }
                    }
                    case "customfieldvalue" -> {
                        if (currentCustomField != null && inCustomFieldValues) {
                            currentCustomField.values.add(text);
                        }
                    }
                    case "customfield" -> {
                        if (currentCustomField != null) {
                            current.customFields.add(currentCustomField);
                            currentCustomField = null;
                        }
                    }
                    case "attachment" -> {
                        if (currentAttachment != null) {
                            current.attachments.add(currentAttachment);
                            currentAttachment = null;
                        }
                    }
                    case "comment" -> {
                        if (currentComment != null) {
                            if (currentComment.body == null || currentComment.body.isBlank()) {
                                currentComment.body = text;
                            }
                            current.comments.add(currentComment);
                            currentComment = null;
                        }
                    }
                    case "attachments" -> inAttachments = false;
                    case "customfields" -> inCustomFields = false;
                    case "comments" -> inComments = false;
                    case "customfieldvalues" -> inCustomFieldValues = false;
                    case "labels" -> inLabels = false;
                    case "issuelinks" -> inIssueLinks = false;
                    case "worklogs" -> inWorklogs = false;
                    case "changelog" -> inChangelog = false;
                    case "item" -> {
                        items.add(current);
                        inItem = false;
                        current = null;
                    }
                    default -> { }
                }
            } else if (inChannel && "title".equals(name)) {
                channelTitle = text;
            }

            textBuffer.setLength(0);
        }

        List<LegacyDcXmlParser.ParsedEntity> buildEntities() {
            List<LegacyDcXmlParser.ParsedEntity> entities = new ArrayList<>();
            Set<String> projectsEmitted = new HashSet<>();

            for (ItemDraft item : items) {
                if (item.issueKey == null || item.issueKey.isBlank()) {
                    continue;
                }
                String projectKey = LegacyDcParsedEntityKeys.projectKeyFromIssueKey(item.issueKey);
                if (projectKey != null && projectsEmitted.add(projectKey)) {
                    entities.add(buildProject(projectKey, channelTitle));
                }
                entities.add(buildIssue(item, channelTitle));
                for (CommentDraft c : item.comments) {
                    entities.add(buildComment(item.issueKey, c));
                }
                for (AttachmentDraft a : item.attachments) {
                    entities.add(buildAttachment(item.issueKey, a));
                }
                for (String label : item.labels) {
                    entities.add(buildLabel(item.issueKey, label));
                }
                for (IssueLinkDraft link : item.issueLinks) {
                    entities.add(buildIssueLink(item.issueKey, link));
                }
                for (WorklogDraft w : item.worklogs) {
                    entities.add(buildWorklog(item.issueKey, w));
                }
                for (HistoryDraft h : item.changelog) {
                    entities.add(buildHistory(item.issueKey, h));
                }
            }
            return entities;
        }

        private static LegacyDcXmlParser.ParsedEntity buildLabel(String issueKey, String label) {
            Map<String, String> fields = new HashMap<>();
            fields.put("issueKey", issueKey);
            fields.put("label", label);
            LegacyDcXmlParser.ParsedEntity entity = new LegacyDcXmlParser.ParsedEntity();
            entity.setEntityType("Label");
            entity.setFields(fields);
            entity.setEntityKey(issueKey + ":label:" + label);
            return entity;
        }

        private static LegacyDcXmlParser.ParsedEntity buildIssueLink(String sourceKey, IssueLinkDraft link) {
            Map<String, String> fields = new HashMap<>();
            fields.put("linkType", link.linkType != null ? link.linkType : "relates");
            fields.put("sourceIssueKey", sourceKey);
            if (link.outwardKey != null) {
                fields.put("targetIssueKey", link.outwardKey);
            } else if (link.inwardKey != null && !link.inwardKey.equals(sourceKey)) {
                fields.put("targetIssueKey", link.inwardKey);
            }
            LegacyDcXmlParser.ParsedEntity entity = new LegacyDcXmlParser.ParsedEntity();
            entity.setEntityType("IssueLink");
            entity.setFields(fields);
            entity.setEntityKey(sourceKey + "->" + fields.get("targetIssueKey"));
            return entity;
        }

        private static LegacyDcXmlParser.ParsedEntity buildWorklog(String issueKey, WorklogDraft w) {
            Map<String, String> fields = new HashMap<>();
            fields.put("issue", issueKey);
            fields.put("issueKey", issueKey);
            putIfPresent(fields, "id", w.id);
            putIfPresent(fields, "author", w.author);
            putIfPresent(fields, "timeSpent", w.timeSpent);
            putIfPresent(fields, "started", w.started);
            putIfPresent(fields, "comment", w.comment);
            fields.put("sourceFormat", "RSS_092");
            LegacyDcXmlParser.ParsedEntity entity = new LegacyDcXmlParser.ParsedEntity();
            entity.setEntityType("Worklog");
            entity.setFields(fields);
            entity.setEntityKey(issueKey + ":worklog:" + (w.id != null ? w.id : w.timeSpent));
            return entity;
        }

        private static LegacyDcXmlParser.ParsedEntity buildHistory(String issueKey, HistoryDraft h) {
            Map<String, String> fields = new HashMap<>();
            fields.put("issue", issueKey);
            fields.put("issueKey", issueKey);
            putIfPresent(fields, "field", h.field);
            putIfPresent(fields, "author", h.author);
            putIfPresent(fields, "created", h.created);
            putIfPresent(fields, "old", h.oldValue);
            putIfPresent(fields, "new", h.newValue);
            LegacyDcXmlParser.ParsedEntity entity = new LegacyDcXmlParser.ParsedEntity();
            entity.setEntityType("History");
            entity.setFields(fields);
            entity.setEntityKey(issueKey + ":history:" + (h.field != null ? h.field : "f"));
            return entity;
        }

        private static String firstAttr(Attributes attributes, String... names) {
            for (String n : names) {
                String v = attributes.getValue(n);
                if (v != null && !v.isBlank()) {
                    return v;
                }
            }
            return null;
        }

        private static LegacyDcXmlParser.ParsedEntity buildProject(String projectKey, String channelTitle) {
            Map<String, String> fields = new HashMap<>();
            fields.put("key", projectKey);
            fields.put("name", channelTitle != null && !channelTitle.isBlank()
                    ? channelTitle + " (" + projectKey + ")"
                    : projectKey);
            fields.put("projectType", "COMPANY_MANAGED");
            fields.put("sourceFormat", "RSS_092");

            LegacyDcXmlParser.ParsedEntity entity = new LegacyDcXmlParser.ParsedEntity();
            entity.setEntityType("Project");
            entity.setFields(fields);
            entity.setEntityKey(projectKey);
            return entity;
        }

        private static LegacyDcXmlParser.ParsedEntity buildIssue(ItemDraft item, String channelTitle) {
            Map<String, String> fields = new HashMap<>();
            fields.put("issueKey", item.issueKey);
            fields.put("id", item.issueId != null ? item.issueId : issueNumberFromKey(item.issueKey));
            fields.put("project", LegacyDcParsedEntityKeys.projectKeyFromIssueKey(item.issueKey));
            putIfPresent(fields, "summary", item.summary);
            putIfPresent(fields, "description", item.description);
            putIfPresent(fields, "issueType", item.issueType);
            putIfPresent(fields, "status", item.status);
            putIfPresent(fields, "priority", item.priority);
            putIfPresent(fields, "assignee", item.assignee);
            putIfPresent(fields, "reporter", item.reporter);
            putIfPresent(fields, "created", item.created);
            putIfPresent(fields, "updated", item.updated);
            putIfPresent(fields, "parent", item.parentKey);
            putIfPresent(fields, "channelTitle", channelTitle);
            fields.put("sourceFormat", "RSS_092");

            for (CustomFieldDraft cf : item.customFields) {
                if (cf.id != null) {
                    String value = cf.values.isEmpty() ? "" : String.join(",", cf.values);
                    fields.put(cf.id, value);
                    if (cf.name != null) {
                        fields.put(cf.id + "_name", cf.name);
                    }
                }
            }
            if (!item.labels.isEmpty()) {
                fields.put("labels", String.join(",", item.labels));
            }

            LegacyDcXmlParser.ParsedEntity entity = new LegacyDcXmlParser.ParsedEntity();
            entity.setEntityType("Issue");
            entity.setFields(fields);
            entity.setEntityKey(item.issueKey);
            return entity;
        }

        private static LegacyDcXmlParser.ParsedEntity buildComment(String issueKey, CommentDraft c) {
            Map<String, String> fields = new HashMap<>();
            fields.put("issue", issueKey);
            fields.put("issueKey", issueKey);
            if (c.id != null) {
                fields.put("id", c.id);
                fields.put("sourceCommentId", c.id);
            }
            putIfPresent(fields, "body", c.body);
            putIfPresent(fields, "author", c.author);
            putIfPresent(fields, "created", c.created);
            fields.put("sourceFormat", "RSS_092");

            LegacyDcXmlParser.ParsedEntity entity = new LegacyDcXmlParser.ParsedEntity();
            entity.setEntityType("Comment");
            entity.setFields(fields);
            entity.setEntityKey(LegacyDcParsedEntityKeys.generate(entity));
            return entity;
        }

        private static LegacyDcXmlParser.ParsedEntity buildAttachment(String issueKey, AttachmentDraft a) {
            Map<String, String> fields = new HashMap<>();
            fields.put("issue", issueKey);
            fields.put("issueKey", issueKey);
            if (a.id != null) {
                fields.put("sourceAttachmentId", a.id);
            }
            putIfPresent(fields, "filename", a.name);
            putIfPresent(fields, "name", a.name);
            fields.put("sourceFormat", "RSS_092");

            LegacyDcXmlParser.ParsedEntity entity = new LegacyDcXmlParser.ParsedEntity();
            entity.setEntityType("Attachment");
            entity.setFields(fields);
            entity.setEntityKey(LegacyDcParsedEntityKeys.generate(entity));
            return entity;
        }

        private static String issueNumberFromKey(String issueKey) {
            int dash = issueKey.indexOf('-');
            return dash >= 0 && dash < issueKey.length() - 1
                    ? issueKey.substring(dash + 1)
                    : issueKey;
        }

        private static void putIfPresent(Map<String, String> fields, String key, String value) {
            if (value != null && !value.isBlank()) {
                fields.put(key, value);
            }
        }

        private static String elementName(String localName, String qName) {
            if (localName != null && !localName.isBlank()) {
                return localName;
            }
            if (qName != null && qName.contains(":")) {
                return qName.substring(qName.indexOf(':') + 1);
            }
            return qName != null ? qName : "";
        }
    }
}
