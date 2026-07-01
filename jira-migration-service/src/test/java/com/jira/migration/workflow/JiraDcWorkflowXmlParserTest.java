package com.jira.migration.workflow;

import com.jira.migration.workflow.graph.WorkflowGraphBuilder;
import com.jira.migration.workflow.model.WorkflowDescriptorModel;
import com.jira.migration.workflow.parser.JiraDcWorkflowSchemeXmlParser;
import com.jira.migration.workflow.parser.JiraDcWorkflowXmlParser;
import com.jira.migration.workflow.validation.WorkflowXmlValidationService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JiraDcWorkflowXmlParserTest {

    private final JiraDcWorkflowXmlParser parser = new JiraDcWorkflowXmlParser();
    private final JiraDcWorkflowSchemeXmlParser schemeParser = new JiraDcWorkflowSchemeXmlParser();
    private final WorkflowXmlValidationService validationService =
            new WorkflowXmlValidationService(new WorkflowGraphBuilder(), new com.jira.migration.workflow.registry.OsWorkflowDescriptorRegistry(new com.fasterxml.jackson.databind.ObjectMapper()));

    @Test
    void parsesEnterpriseFixture() throws Exception {
        String xml = Files.readString(Path.of("src/test/resources/samples/workflow/jira-dc-enterprise-change-workflow.xml"));
        WorkflowDescriptorModel model = parser.parse(xml);

        assertEquals("Enterprise Change Management Workflow", model.getName());
        assertFalse(model.getSteps().isEmpty());
        assertFalse(model.getInitialActions().isEmpty());
        assertFalse(model.getCommonActions().isEmpty());

        var cabStep = model.getSteps().stream().filter(s -> "CAB Review".equals(s.getName())).findFirst();
        assertTrue(cabStep.isPresent());
        var approve = cabStep.get().getActions().stream().filter(a -> "Approve".equals(a.getName())).findFirst();
        assertTrue(approve.isPresent());
        assertFalse(approve.get().getValidators().isEmpty());
        assertFalse(approve.get().getConditions().isEmpty());
        assertTrue(approve.get().getResults().size() >= 2);

        var report = validationService.validate(model);
        assertTrue(report.isValid(), () -> String.join(", ", report.getErrors()));
        assertTrue(report.getStepCount() >= 7);
        assertTrue(report.getTransitionCount() > 5);
    }

    @Test
    void parsesSchemeFixture() throws Exception {
        String xml = Files.readString(Path.of("src/test/resources/samples/workflow/jira-dc-enterprise-workflow-scheme.xml"));
        var scheme = schemeParser.parse(xml);
        assertEquals("Enterprise Change Scheme", scheme.getName());
        assertEquals("Enterprise Change Management Workflow", scheme.getDefaultWorkflow());
        assertFalse(scheme.getMappings().isEmpty());
    }
}
