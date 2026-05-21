package com.jira.migration.workflow.parser;

import com.jira.migration.exception.ValidationException;
import com.jira.migration.workflow.model.WorkflowSchemeModel;
import org.springframework.stereotype.Component;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class JiraDcWorkflowSchemeXmlParser {

    public WorkflowSchemeModel parse(String xml) {
        if (xml == null || xml.isBlank()) {
            throw new ValidationException("Workflow scheme XML is empty", "SCHEME_XML_EMPTY", "file");
        }
        if (!xml.contains("workflow-scheme")) {
            throw new ValidationException("Not a workflow-scheme document", "SCHEME_XML_FORMAT", "file");
        }
        try {
            XMLInputFactory factory = XMLInputFactory.newFactory();
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(xml.trim()));

            String schemeName = null;
            String defaultWorkflow = null;
            Map<String, String> meta = new LinkedHashMap<>();
            List<WorkflowSchemeModel.WorkflowSchemeMapping> mappings = new ArrayList<>();
            List<WorkflowSchemeModel.WorkflowSchemeProjectAssociation> associations = new ArrayList<>();
            String pendingMetaName = null;
            boolean inDefaultWorkflow = false;

            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String local = reader.getLocalName();
                    switch (local) {
                        case "workflow-scheme" -> schemeName = attr(reader, "name");
                        case "meta" -> pendingMetaName = attr(reader, "name");
                        case "default-workflow" -> inDefaultWorkflow = true;
                        case "mapping" -> mappings.add(WorkflowSchemeModel.WorkflowSchemeMapping.builder()
                                .issueType(attr(reader, "issue-type"))
                                .workflow(attr(reader, "workflow"))
                                .build());
                        case "association" -> associations.add(WorkflowSchemeModel.WorkflowSchemeProjectAssociation.builder()
                                .projectKey(attr(reader, "project-key"))
                                .scheme(attr(reader, "scheme"))
                                .active("true".equalsIgnoreCase(attr(reader, "active")))
                                .build());
                        default -> { }
                    }
                } else if (event == XMLStreamConstants.CHARACTERS) {
                    String text = reader.getText().trim();
                    if (!text.isEmpty()) {
                        if (pendingMetaName != null) {
                            meta.put(pendingMetaName, text);
                            pendingMetaName = null;
                        } else if (inDefaultWorkflow) {
                            defaultWorkflow = text;
                        }
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    if ("default-workflow".equals(reader.getLocalName())) {
                        inDefaultWorkflow = false;
                    }
                }
            }
            reader.close();

            return WorkflowSchemeModel.builder()
                    .name(schemeName)
                    .meta(meta)
                    .defaultWorkflow(defaultWorkflow)
                    .mappings(mappings)
                    .projectAssociations(associations)
                    .build();
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to parse scheme XML: " + e.getMessage(), "SCHEME_XML_PARSE", "file");
        }
    }

    private static String attr(XMLStreamReader reader, String name) {
        String v = reader.getAttributeValue(null, name);
        return v != null ? v : reader.getAttributeValue(XMLConstants.NULL_NS_URI, name);
    }
}
