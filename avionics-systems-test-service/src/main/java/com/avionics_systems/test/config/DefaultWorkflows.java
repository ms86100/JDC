package com.avionics_systems.test.config;

import com.avionics_systems.test.entity.WorkflowDefinition;
import com.avionics_systems.test.repository.WorkflowDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultWorkflows implements CommandLineRunner {

    private final WorkflowDefinitionRepository definitionRepository;

    @Override
    @Transactional
    public void run(String... args) {
        createDefaultIfNotExists("TEST_EXECUTION", "Test Execution Workflow",
                createTestExecutionWorkflow());
        createDefaultIfNotExists("TEST_APPROVAL", "Test Approval Workflow",
                createTestApprovalWorkflow());
        createDefaultIfNotExists("TEST_IMPORT", "Test Import Workflow",
                createTestImportWorkflow());
        createDefaultIfNotExists("TEST_PLAN", "Test Plan Workflow",
                createTestPlanWorkflow());
        createDefaultIfNotExists("TEST_SET", "Test Set Workflow",
                createTestSetWorkflow());
        log.info("Default workflows initialized successfully");
    }

    private void createDefaultIfNotExists(String workflowType, String name, String workflowJson) {
        definitionRepository.findByProjectIdAndWorkflowTypeAndIsDefaultTrue(null, workflowType)
                .orElseGet(() -> {
                    log.info("Creating default workflow for type: {}", workflowType);
                    WorkflowDefinition definition = WorkflowDefinition.builder()
                            .name(name)
                            .description("System default workflow for " + workflowType)
                            .workflowType(workflowType)
                            .workflowStepsJson(workflowJson)
                            .isDefault(true)
                            .isActive(true)
                            .build();
                    return definitionRepository.save(definition);
                });
    }

    private String createTestExecutionWorkflow() {
        return """
            {
              "initialState": "DRAFT",
              "states": ["DRAFT", "READY", "IN_PROGRESS", "PASSED", "FAILED", "BLOCKED", "CANCELLED"],
              "finalStates": ["PASSED", "FAILED", "CANCELLED"],
              "transitions": {
                "DRAFT": ["READY"],
                "READY": ["IN_PROGRESS", "CANCELLED"],
                "IN_PROGRESS": ["PASSED", "FAILED", "BLOCKED", "CANCELLED"],
                "BLOCKED": ["IN_PROGRESS", "CANCELLED"],
                "FAILED": ["IN_PROGRESS", "READY"],
                "PASSED": ["IN_PROGRESS"],
                "CANCELLED": ["READY"]
              }
            }
            """;
    }

    private String createTestApprovalWorkflow() {
        return """
            {
              "initialState": "PENDING_REVIEW",
              "states": ["PENDING_REVIEW", "IN_REVIEW", "APPROVED", "REJECTED", "CANCELLED"],
              "finalStates": ["APPROVED", "REJECTED", "CANCELLED"],
              "transitions": {
                "PENDING_REVIEW": ["IN_REVIEW", "CANCELLED"],
                "IN_REVIEW": ["APPROVED", "REJECTED", "CANCELLED"],
                "REJECTED": ["IN_REVIEW", "CANCELLED"],
                "APPROVED": ["IN_REVIEW"],
                "CANCELLED": []
              }
            }
            """;
    }

    private String createTestImportWorkflow() {
        return """
            {
              "initialState": "PENDING",
              "states": ["PENDING", "PROCESSING", "COMPLETED", "FAILED", "CANCELLED"],
              "finalStates": ["COMPLETED", "FAILED", "CANCELLED"],
              "transitions": {
                "PENDING": ["PROCESSING", "CANCELLED"],
                "PROCESSING": ["COMPLETED", "FAILED", "CANCELLED"],
                "FAILED": ["PROCESSING", "CANCELLED"],
                "COMPLETED": [],
                "CANCELLED": []
              }
            }
            """;
    }

    private String createTestPlanWorkflow() {
        return """
            {
              "initialState": "DRAFT",
              "states": ["DRAFT", "READY", "IN_PROGRESS", "COMPLETED", "CANCELLED"],
              "finalStates": ["COMPLETED", "CANCELLED"],
              "transitions": {
                "DRAFT": ["READY", "CANCELLED"],
                "READY": ["IN_PROGRESS", "CANCELLED"],
                "IN_PROGRESS": ["COMPLETED", "CANCELLED"],
                "COMPLETED": [],
                "CANCELLED": []
              }
            }
            """;
    }

    private String createTestSetWorkflow() {
        return """
            {
              "initialState": "DRAFT",
              "states": ["DRAFT", "READY", "IN_PROGRESS", "COMPLETED", "CANCELLED"],
              "finalStates": ["COMPLETED", "CANCELLED"],
              "transitions": {
                "DRAFT": ["READY", "CANCELLED"],
                "READY": ["IN_PROGRESS", "CANCELLED"],
                "IN_PROGRESS": ["COMPLETED", "CANCELLED"],
                "COMPLETED": ["IN_PROGRESS"],
                "CANCELLED": ["READY"]
              }
            }
            """;
    }
}
