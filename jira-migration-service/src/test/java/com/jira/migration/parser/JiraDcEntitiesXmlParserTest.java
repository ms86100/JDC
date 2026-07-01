package com.jira.migration.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class JiraDcEntitiesXmlParserTest {

    @TempDir
    Path tempDir;

    @Test
    void parse_entitiesXml_issueCommentAttachmentAndChangeItem() throws Exception {
        String xml = """
                <entity-engine-xml>
                  <Issue id="10001" projectKey="DEMO" number="1" summary="Test" status="Open" priority="High"/>
                  <Action id="20001" issue="10001" author="admin" created="2026-01-01T10:00:00Z">Comment body</Action>
                  <FileAttachment id="30001" issue="10001" fileName="doc.txt"/>
                  <ChangeGroup id="40001" issue="10001" author="admin">
                    <ChangeItem id="1" field="status" oldvalue="Open" newvalue="Done"/>
                  </ChangeGroup>
                  <CustomFieldValue issue="10001" customfield="10000">42</CustomFieldValue>
                  <IssueLink source="10001" destination="10002" linkType="blocks"/>
                  <Label issue="10001" label="backend"/>
                </entity-engine-xml>
                """;
        Path file = tempDir.resolve("entities.xml");
        Files.writeString(file, xml);

        var entities = JiraDcEntitiesXmlParser.parse(file);
        Map<String, Long> byType = entities.stream()
                .collect(Collectors.groupingBy(JiraDcXmlParser.ParsedEntity::getEntityType, Collectors.counting()));

        assertTrue(byType.getOrDefault("Issue", 0L) >= 1);
        assertTrue(byType.getOrDefault("Comment", 0L) >= 1);
        assertTrue(byType.getOrDefault("Attachment", 0L) >= 1);
        assertTrue(byType.getOrDefault("History", 0L) >= 1);
        assertTrue(byType.getOrDefault("CustomField", 0L) >= 1);
        assertTrue(byType.getOrDefault("IssueLink", 0L) >= 1);
        assertTrue(byType.getOrDefault("Label", 0L) >= 1);

        var comment = entities.stream().filter(e -> "Comment".equals(e.getEntityType())).findFirst().orElseThrow();
        assertEquals("DEMO-1", comment.getFields().get("issueKey"));
    }
}
