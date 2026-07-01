package com.jira.project.controller;

import com.jira.project.dto.*;
import com.jira.project.service.TemplateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TemplateController.
 * Tests template API endpoints and response handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Template Controller Tests")
class TemplateControllerTest {

    @Mock
    private TemplateService templateService;

    @InjectMocks
    private TemplateController templateController;

    private final UUID templateId = UUID.randomUUID();
    private final UUID typeId = UUID.randomUUID();
    private final UUID schemeId = UUID.randomUUID();

    @Nested
    @DisplayName("Get Templates By Category")
    class GetTemplatesByCategoryTests {

        @Test
        @DisplayName("Should return templates grouped by category")
        void shouldReturnTemplatesGroupedByCategory() {
            // Given
            List<TemplateCategoryResponse> categories = List.of(
                    createCategoryResponse("BUSINESS", 3),
                    createCategoryResponse("SOFTWARE", 2)
            );
            when(templateService.getTemplatesByCategory()).thenReturn(categories);

            // When
            ResponseEntity<List<TemplateCategoryResponse>> response = templateController.getTemplatesByCategory();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
            verify(templateService).getTemplatesByCategory();
        }

        @Test
        @DisplayName("Should return empty list when no templates")
        void shouldReturnEmptyListWhenNoTemplates() {
            // Given
            when(templateService.getTemplatesByCategory()).thenReturn(List.of());

            // When
            ResponseEntity<List<TemplateCategoryResponse>> response = templateController.getTemplatesByCategory();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Templates By Category Name")
    class GetTemplatesByCategoryNameTests {

        @Test
        @DisplayName("Should return templates for specific category")
        void shouldReturnTemplatesForSpecificCategory() {
            // Given
            List<ProjectTemplateResponse> templates = List.of(
                    createTemplateResponse("Project management"),
                    createTemplateResponse("Task management")
            );
            when(templateService.getTemplatesByCategory("BUSINESS")).thenReturn(templates);

            // When
            ResponseEntity<List<ProjectTemplateResponse>> response =
                    templateController.getTemplatesByCategoryName("BUSINESS");

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
            verify(templateService).getTemplatesByCategory("BUSINESS");
        }
    }

    @Nested
    @DisplayName("Get Templates By Type")
    class GetTemplatesByTypeTests {

        @Test
        @DisplayName("Should return templates for specific type")
        void shouldReturnTemplatesForSpecificType() {
            // Given
            List<ProjectTemplateResponse> templates = List.of(
                    createTemplateResponse("Scrum")
            );
            when(templateService.getTemplatesByType(typeId)).thenReturn(templates);

            // When
            ResponseEntity<List<ProjectTemplateResponse>> response =
                    templateController.getTemplatesByType(typeId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            verify(templateService).getTemplatesByType(typeId);
        }
    }

    @Nested
    @DisplayName("Get Template With Workflow")
    class GetTemplateWithWorkflowTests {

        @Test
        @DisplayName("Should return template with workflow details")
        void shouldReturnTemplateWithWorkflowDetails() {
            // Given
            TemplateWithWorkflowResponse template = createTemplateWithWorkflowResponse();
            when(templateService.getTemplateWithWorkflow(templateId)).thenReturn(template);

            // When
            ResponseEntity<TemplateWithWorkflowResponse> response =
                    templateController.getTemplateWithWorkflow(templateId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getId()).isEqualTo(templateId);
            assertThat(response.getBody().getWorkflowStatuses()).isNotEmpty();
            assertThat(response.getBody().getWorkflowTransitions()).isNotEmpty();
            assertThat(response.getBody().getIssueTypes()).isNotEmpty();
        }

        @Test
        @DisplayName("Should include issue types in workflow response")
        void shouldIncludeIssueTypesInWorkflowResponse() {
            // Given
            TemplateWithWorkflowResponse template = createTemplateWithWorkflowResponse();
            when(templateService.getTemplateWithWorkflow(templateId)).thenReturn(template);

            // When
            ResponseEntity<TemplateWithWorkflowResponse> response =
                    templateController.getTemplateWithWorkflow(templateId);

            // Then
            assertThat(response.getBody().getIssueTypes()).hasSize(2);
            assertThat(response.getBody().getIssueTypes())
                    .anyMatch(it -> "Task".equals(it.getIssueTypeName()));
            assertThat(response.getBody().getIssueTypes())
                    .anyMatch(it -> "Sub-task".equals(it.getIssueTypeName()));
        }

        @Test
        @DisplayName("Should include workflow statuses and transitions")
        void shouldIncludeWorkflowStatusesAndTransitions() {
            // Given
            TemplateWithWorkflowResponse template = createProcessManagementTemplate();
            when(templateService.getTemplateWithWorkflow(templateId)).thenReturn(template);

            // When
            ResponseEntity<TemplateWithWorkflowResponse> response =
                    templateController.getTemplateWithWorkflow(templateId);

            // Then
            assertThat(response.getBody().getWorkflowStatuses()).hasSize(7); // Process management has 7 statuses
            assertThat(response.getBody().getWorkflowTransitions()).hasSize(8); // Process management has 8 transitions
        }
    }

    @Nested
    @DisplayName("Get Simple Template")
    class GetSimpleTemplateTests {

        @Test
        @DisplayName("Should return simple template response")
        void shouldReturnSimpleTemplateResponse() {
            // Given
            ProjectTemplateResponse template = createTemplateResponse("Project management");
            when(templateService.getTemplate(templateId)).thenReturn(template);

            // When
            ResponseEntity<ProjectTemplateResponse> response = templateController.getTemplate(templateId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getName()).isEqualTo("Project management");
        }
    }

    @Nested
    @DisplayName("Get All Templates")
    class GetAllTemplatesTests {

        @Test
        @DisplayName("Should return all templates")
        void shouldReturnAllTemplates() {
            // Given
            List<ProjectTemplateResponse> templates = List.of(
                    createTemplateResponse("Project management"),
                    createTemplateResponse("Task management"),
                    createTemplateResponse("Scrum")
            );
            when(templateService.getAllTemplates()).thenReturn(templates);

            // When
            ResponseEntity<List<ProjectTemplateResponse>> response = templateController.getAllTemplates();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Get Available Workflow Statuses")
    class GetAvailableStatusesTests {

        @Test
        @DisplayName("Should return available workflow statuses")
        void shouldReturnAvailableWorkflowStatuses() {
            // Given
            List<TemplateWithWorkflowResponse.TemplateWorkflowStatusDto> statuses = List.of(
                    createStatusDto("TODO", "To Do", "#6B778C"),
                    createStatusDto("IN_PROGRESS", "In Progress", "#0052CC"),
                    createStatusDto("DONE", "Done", "#00875A")
            );
            when(templateService.getAvailableStatuses()).thenReturn(statuses);

            // When
            ResponseEntity<List<TemplateWithWorkflowResponse.TemplateWorkflowStatusDto>> response =
                    templateController.getAvailableStatuses();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(3);
        }
    }

    // ============ Helper Methods ============

    private TemplateCategoryResponse createCategoryResponse(String categoryName, int templateCount) {
        List<ProjectTemplateResponse> templates = java.util.stream.IntStream.range(0, templateCount)
                .mapToObj(i -> createTemplateResponse("Template " + i))
                .toList();

        return TemplateCategoryResponse.builder()
                .categoryName(categoryName)
                .categoryIcon(categoryName.equals("BUSINESS") ? "briefcase" : "code")
                .templates(templates)
                .build();
    }

    private ProjectTemplateResponse createTemplateResponse(String name) {
        return ProjectTemplateResponse.builder()
                .id(templateId)
                .typeId(typeId)
                .typeName("Company-managed")
                .name(name)
                .description("Description for " + name)
                .icon(name.toLowerCase().replace(" ", "-"))
                .color("#0052CC")
                .isActive(true)
                .sortOrder(1)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private TemplateWithWorkflowResponse createTemplateWithWorkflowResponse() {
        return TemplateWithWorkflowResponse.builder()
                .id(templateId)
                .typeId(typeId)
                .typeName("Company-managed")
                .name("Project management")
                .description("Plan, track and report on all of your work")
                .icon("project-management")
                .color("#0052CC")
                .category("BUSINESS")
                .templateType("MANAGEMENT")
                .isActive(true)
                .instructions("Create your tasks, organize and track their progress")
                .issueTypes(List.of(
                        createIssueTypeDto("Task", false, false),
                        createIssueTypeDto("Sub-task", true, false)
                ))
                .workflowStatuses(List.of(
                        createStatusDto("TODO", "To Do", "#6B778C"),
                        createStatusDto("IN_PROGRESS", "In Progress", "#0052CC"),
                        createStatusDto("DONE", "Done", "#00875A")
                ))
                .workflowTransitions(List.of(
                        createTransitionDto("TODO", "IN_PROGRESS", "Start"),
                        createTransitionDto("IN_PROGRESS", "DONE", "Complete")
                ))
                .issueTypeScheme(TemplateWithWorkflowResponse.SchemeInfoDto.builder()
                        .id(schemeId)
                        .name("Task Issue Types")
                        .build())
                .workflowScheme(TemplateWithWorkflowResponse.SchemeInfoDto.builder()
                        .id(schemeId)
                        .name("Task Workflow")
                        .build())
                .build();
    }

    private TemplateWithWorkflowResponse createProcessManagementTemplate() {
        return TemplateWithWorkflowResponse.builder()
                .id(templateId)
                .typeId(typeId)
                .name("Process management")
                .category("BUSINESS")
                .issueTypes(List.of(
                        createIssueTypeDto("Task", false, false),
                        createIssueTypeDto("Sub-task", true, false)
                ))
                .workflowStatuses(List.of(
                        createStatusDto("OPEN", "Open", "#6B778C"),
                        createStatusDto("IN_PROGRESS", "In Progress", "#0052CC"),
                        createStatusDto("UNDER_REVIEW", "Under Review", "#00B8D9"),
                        createStatusDto("APPROVED", "Approved", "#36B37E"),
                        createStatusDto("DONE", "Done", "#00875A"),
                        createStatusDto("REJECTED", "Rejected", "#FF5630"),
                        createStatusDto("CANCELLED", "Cancelled", "#6B778C")
                ))
                .workflowTransitions(List.of(
                        createTransitionDto("OPEN", "IN_PROGRESS", "Start"),
                        createTransitionDto("IN_PROGRESS", "UNDER_REVIEW", "Submit for Review"),
                        createTransitionDto("UNDER_REVIEW", "APPROVED", "Approve"),
                        createTransitionDto("UNDER_REVIEW", "REJECTED", "Reject"),
                        createTransitionDto("APPROVED", "DONE", "Complete"),
                        createTransitionDto("REJECTED", "IN_PROGRESS", "Revise"),
                        createTransitionDto("IN_PROGRESS", "CANCELLED", "Cancel"),
                        createTransitionDto("CANCELLED", "OPEN", "Reactivate")
                ))
                .build();
    }

    private TemplateWithWorkflowResponse.TemplateIssueTypeDto createIssueTypeDto(
            String name, boolean isSubtask, boolean isDefault) {
        return TemplateWithWorkflowResponse.TemplateIssueTypeDto.builder()
                .id(UUID.randomUUID())
                .issueTypeName(name)
                .issueTypeIcon(name.toLowerCase())
                .isSubtask(isSubtask)
                .isDefault(isDefault)
                .sequence(isSubtask ? 1 : 0)
                .build();
    }

    private TemplateWithWorkflowResponse.TemplateWorkflowStatusDto createStatusDto(
            String key, String name, String color) {
        return TemplateWithWorkflowResponse.TemplateWorkflowStatusDto.builder()
                .id(UUID.randomUUID())
                .statusKey(key)
                .statusName(name)
                .statusColor(color)
                .statusCategory(getCategoryForStatus(key))
                .sequence(0)
                .build();
    }

    private TemplateWithWorkflowResponse.TemplateWorkflowTransitionDto createTransitionDto(
            String fromKey, String toKey, String name) {
        return TemplateWithWorkflowResponse.TemplateWorkflowTransitionDto.builder()
                .id(UUID.randomUUID())
                .fromStatusKey(fromKey)
                .toStatusKey(toKey)
                .transitionName(name)
                .allowBackward(false)
                .requiresApproval(false)
                .sequence(0)
                .build();
    }

    private String getCategoryForStatus(String key) {
        if ("TODO".equals(key) || "OPEN".equals(key)) {
            return "TODO";
        } else if ("DONE".equals(key) || "APPROVED".equals(key) ||
                   "REJECTED".equals(key) || "CANCELLED".equals(key)) {
            return "DONE";
        }
        return "IN_PROGRESS";
    }
}