package com.jira.project.service;

import com.jira.project.dto.ProjectTemplateResponse;
import com.jira.project.dto.TemplateCategoryResponse;
import com.jira.project.dto.TemplateWithWorkflowResponse;
import com.jira.project.entity.*;
import com.jira.project.exception.ResourceNotFoundException;
import com.jira.project.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TemplateService.
 * Tests template CRUD operations, workflow associations, and scheme mappings.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Template Service Tests")
class TemplateServiceTest {

    @Mock
    private ProjectTemplateRepository projectTemplateRepository;

    @Mock
    private ProjectTypeRepository projectTypeRepository;

    @Mock
    private TemplateWorkflowStatusRepository templateWorkflowStatusRepository;

    @Mock
    private TemplateWorkflowTransitionRepository templateWorkflowTransitionRepository;

    @Mock
    private TemplateIssueTypeRepository templateIssueTypeRepository;

    @Mock
    private TemplateSchemeMappingRepository templateSchemeMappingRepository;

    @Mock
    private StatusDefinitionRepository statusDefinitionRepository;

    @Mock
    private TemplateCategoryRepository templateCategoryRepository;

    @Mock
    private TemplateCapabilityRepository templateCapabilityRepository;

    private TemplateService templateService;

    private final UUID templateId = UUID.randomUUID();
    private final UUID typeId = UUID.randomUUID();
    private final UUID workflowStatusId = UUID.randomUUID();
    private final UUID transitionId = UUID.randomUUID();
    private final UUID issueTypeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        templateService = new TemplateService(
                projectTemplateRepository,
                projectTypeRepository,
                templateCategoryRepository,
                templateCapabilityRepository,
                templateWorkflowStatusRepository,
                templateWorkflowTransitionRepository,
                templateIssueTypeRepository,
                templateSchemeMappingRepository,
                statusDefinitionRepository
        );
    }

    @Nested
    @DisplayName("Template Listing Tests")
    class TemplateListingTests {

        @Test
        @DisplayName("Should get templates grouped by category")
        void shouldGetTemplatesGroupedByCategory() {
            // Given
            ProjectType type = createProjectType();
            TemplateCategory pmCategory = TemplateCategory.builder()
                    .id(UUID.randomUUID())
                    .categoryKey("PROJECT_MANAGEMENT")
                    .name("Project Management")
                    .sortOrder(1)
                    .isActive(true)
                    .build();
            TemplateCategory swCategory = TemplateCategory.builder()
                    .id(UUID.randomUUID())
                    .categoryKey("SOFTWARE_DEVELOPMENT")
                    .name("Software Development")
                    .sortOrder(2)
                    .isActive(true)
                    .build();

            ProjectTemplate scrumTemplate = createTemplate("Scrum", "SOFTWARE", type);
            scrumTemplate.setTemplateCategory(pmCategory);
            ProjectTemplate bugTemplate = createTemplate("Bug Tracking", "SOFTWARE", type);
            bugTemplate.setTemplateCategory(swCategory);

            when(projectTemplateRepository.findAll()).thenReturn(List.of(scrumTemplate, bugTemplate));
            when(templateCategoryRepository.findByIsActiveTrueOrderBySortOrderAsc())
                    .thenReturn(List.of(pmCategory, swCategory));
            when(templateCapabilityRepository.findByTemplateIdInOrderBySortOrderAsc(any()))
                    .thenReturn(List.of());

            // When
            List<TemplateCategoryResponse> categories = templateService.getTemplatesByCategory();

            // Then
            assertThat(categories).hasSize(2);
            assertThat(categories).anyMatch(c -> "PROJECT_MANAGEMENT".equals(c.getCategoryKey()));
            assertThat(categories).anyMatch(c -> "SOFTWARE_DEVELOPMENT".equals(c.getCategoryKey()));
        }

        @Test
        @DisplayName("Should get templates for specific type")
        void shouldGetTemplatesForSpecificType() {
            // Given
            ProjectType type = createProjectType();
            ProjectTemplate template = createTemplate("Project management", "BUSINESS", type);

            when(projectTemplateRepository.findByTypeIdAndIsActiveTrueOrderBySortOrderAsc(typeId))
                    .thenReturn(List.of(template));

            // When
            List<ProjectTemplateResponse> templates = templateService.getTemplatesByType(typeId);

            // Then
            assertThat(templates).hasSize(1);
            assertThat(templates.get(0).getName()).isEqualTo("Project management");
        }

        @Test
        @DisplayName("Should get templates for specific category")
        void shouldGetTemplatesForSpecificCategory() {
            // Given
            ProjectType type = createProjectType();
            ProjectTemplate template = createTemplate("Project management", "BUSINESS", type);

            when(projectTemplateRepository.findAll()).thenReturn(List.of(template));

            // When
            List<ProjectTemplateResponse> templates = templateService.getTemplatesByCategory("BUSINESS");

            // Then
            assertThat(templates).hasSize(1);
            assertThat(templates.get(0).getName()).isEqualTo("Project management");
        }

        @Test
        @DisplayName("Should filter out inactive templates")
        void shouldFilterOutInactiveTemplates() {
            // Given
            ProjectType type = createProjectType();
            ProjectTemplate activeTemplate = createTemplate("Active", "BUSINESS", type);
            activeTemplate.setIsActive(true);
            ProjectTemplate inactiveTemplate = createTemplate("Inactive", "BUSINESS", type);
            inactiveTemplate.setIsActive(false);

            when(projectTemplateRepository.findAll()).thenReturn(List.of(activeTemplate, inactiveTemplate));

            // When
            List<ProjectTemplateResponse> templates = templateService.getTemplatesByCategory("BUSINESS");

            // Then
            assertThat(templates).hasSize(1);
            assertThat(templates.get(0).getName()).isEqualTo("Active");
        }
    }

    @Nested
    @DisplayName("Template Details Tests")
    class TemplateDetailsTests {

        @Test
        @DisplayName("Should get template with workflow details")
        void shouldGetTemplateWithWorkflowDetails() {
            // Given
            ProjectType type = createProjectType();
            ProjectTemplate template = createTemplate("Project management", "BUSINESS", type);

            List<TemplateIssueType> issueTypes = List.of(
                    createIssueType("Task", false),
                    createIssueType("Sub-task", true)
            );

            List<TemplateWorkflowStatus> statuses = List.of(
                    createWorkflowStatus("TODO", 0),
                    createWorkflowStatus("IN_PROGRESS", 1),
                    createWorkflowStatus("DONE", 2)
            );

            List<TemplateWorkflowTransition> transitions = List.of(
                    createTransition("TODO", "IN_PROGRESS"),
                    createTransition("IN_PROGRESS", "DONE")
            );

            List<TemplateSchemeMapping> schemeMappings = List.of(
                    createSchemeMapping("ISSUE_TYPE", "Task Issue Types")
            );

            when(projectTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));
            when(templateIssueTypeRepository.findByTemplateIdOrderBySequenceAsc(templateId)).thenReturn(issueTypes);
            when(templateWorkflowStatusRepository.findByTemplateIdOrderBySequenceAsc(templateId)).thenReturn(statuses);
            when(templateWorkflowTransitionRepository.findByTemplateIdOrderBySequenceAsc(templateId)).thenReturn(transitions);
            when(templateSchemeMappingRepository.findByTemplateId(templateId)).thenReturn(schemeMappings);

            // When
            TemplateWithWorkflowResponse response = templateService.getTemplateWithWorkflow(templateId);

            // Then
            assertThat(response.getId()).isEqualTo(templateId);
            assertThat(response.getName()).isEqualTo("Project management");
            assertThat(response.getIssueTypes()).hasSize(2);
            assertThat(response.getWorkflowStatuses()).hasSize(3);
            assertThat(response.getWorkflowTransitions()).hasSize(2);
        }

        @Test
        @DisplayName("Should throw exception for non-existent template")
        void shouldThrowExceptionForNonExistentTemplate() {
            // Given
            when(projectTemplateRepository.findById(templateId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> templateService.getTemplateWithWorkflow(templateId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Template");
        }

        @Test
        @DisplayName("Should get simple template response")
        void shouldGetSimpleTemplateResponse() {
            // Given
            ProjectType type = createProjectType();
            ProjectTemplate template = createTemplate("Project management", "BUSINESS", type);

            when(projectTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));

            // When
            ProjectTemplateResponse response = templateService.getTemplate(templateId);

            // Then
            assertThat(response.getId()).isEqualTo(templateId);
            assertThat(response.getName()).isEqualTo("Project management");
        }
    }

    @Nested
    @DisplayName("Workflow Status Tests")
    class WorkflowStatusTests {

        @Test
        @DisplayName("Should get available workflow statuses")
        void shouldGetAvailableWorkflowStatuses() {
            // Given
            List<StatusDefinition> definitions = List.of(
                    createStatusDefinition("TODO", "To Do", "#6B778C"),
                    createStatusDefinition("IN_PROGRESS", "In Progress", "#0052CC"),
                    createStatusDefinition("DONE", "Done", "#00875A")
            );

            when(statusDefinitionRepository.findAll()).thenReturn(definitions);

            // When
            List<TemplateWithWorkflowResponse.TemplateWorkflowStatusDto> statuses =
                    templateService.getAvailableStatuses();

            // Then
            assertThat(statuses).hasSize(3);
            assertThat(statuses).anyMatch(s -> "TODO".equals(s.getStatusKey()));
            assertThat(statuses).anyMatch(s -> "IN_PROGRESS".equals(s.getStatusKey()));
            assertThat(statuses).anyMatch(s -> "DONE".equals(s.getStatusKey()));
        }
    }

    @Nested
    @DisplayName("Template Workflow Visualization Tests")
    class WorkflowVisualizationTests {

        @Test
        @DisplayName("Should map workflow statuses in correct sequence")
        void shouldMapWorkflowStatusesInCorrectSequence() {
            // Given
            ProjectType type = createProjectType();
            ProjectTemplate template = createTemplate("Process management", "BUSINESS", type);

            List<TemplateWorkflowStatus> statuses = List.of(
                    createWorkflowStatusWithSequence("OPEN", 0),
                    createWorkflowStatusWithSequence("IN_PROGRESS", 1),
                    createWorkflowStatusWithSequence("UNDER_REVIEW", 2),
                    createWorkflowStatusWithSequence("DONE", 3)
            );

            when(projectTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));
            when(templateIssueTypeRepository.findByTemplateIdOrderBySequenceAsc(templateId)).thenReturn(List.of());
            when(templateWorkflowStatusRepository.findByTemplateIdOrderBySequenceAsc(templateId)).thenReturn(statuses);
            when(templateWorkflowTransitionRepository.findByTemplateIdOrderBySequenceAsc(templateId)).thenReturn(List.of());
            when(templateSchemeMappingRepository.findByTemplateId(templateId)).thenReturn(List.of());

            // When
            TemplateWithWorkflowResponse response = templateService.getTemplateWithWorkflow(templateId);

            // Then
            assertThat(response.getWorkflowStatuses()).hasSize(4);
            assertThat(response.getWorkflowStatuses().get(0).getStatusKey()).isEqualTo("OPEN");
            assertThat(response.getWorkflowStatuses().get(3).getStatusKey()).isEqualTo("DONE");
        }

        @Test
        @DisplayName("Should map workflow transitions correctly")
        void shouldMapWorkflowTransitionsCorrectly() {
            // Given
            ProjectType type = createProjectType();
            ProjectTemplate template = createTemplate("Process management", "BUSINESS", type);

            List<TemplateWorkflowTransition> transitions = List.of(
                    createTransitionWithDetails("OPEN", "IN_PROGRESS", "Start", true),
                    createTransitionWithDetails("IN_PROGRESS", "UNDER_REVIEW", "Submit for Review", true),
                    createTransitionWithDetails("UNDER_REVIEW", "DONE", "Approve", false)
            );

            when(projectTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));
            when(templateIssueTypeRepository.findByTemplateIdOrderBySequenceAsc(templateId)).thenReturn(List.of());
            when(templateWorkflowStatusRepository.findByTemplateIdOrderBySequenceAsc(templateId)).thenReturn(List.of());
            when(templateWorkflowTransitionRepository.findByTemplateIdOrderBySequenceAsc(templateId)).thenReturn(transitions);
            when(templateSchemeMappingRepository.findByTemplateId(templateId)).thenReturn(List.of());

            // When
            TemplateWithWorkflowResponse response = templateService.getTemplateWithWorkflow(templateId);

            // Then
            assertThat(response.getWorkflowTransitions()).hasSize(3);
            assertThat(response.getWorkflowTransitions().get(0).getFromStatusKey()).isEqualTo("OPEN");
            assertThat(response.getWorkflowTransitions().get(0).getToStatusKey()).isEqualTo("IN_PROGRESS");
            assertThat(response.getWorkflowTransitions().get(0).getTransitionName()).isEqualTo("Start");
        }

        @Test
        @DisplayName("Should include scheme info in template response")
        void shouldIncludeSchemeInfoInTemplateResponse() {
            // Given
            ProjectType type = createProjectType();
            ProjectTemplate template = createTemplate("Project management", "BUSINESS", type);

            UUID issueTypeSchemeId = UUID.randomUUID();
            List<TemplateSchemeMapping> mappings = List.of(
                    createSchemeMappingWithId("ISSUE_TYPE", "Task Issue Types", issueTypeSchemeId),
                    createSchemeMappingWithId("WORKFLOW", "Task Workflow", UUID.randomUUID())
            );

            when(projectTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));
            when(templateIssueTypeRepository.findByTemplateIdOrderBySequenceAsc(templateId)).thenReturn(List.of());
            when(templateWorkflowStatusRepository.findByTemplateIdOrderBySequenceAsc(templateId)).thenReturn(List.of());
            when(templateWorkflowTransitionRepository.findByTemplateIdOrderBySequenceAsc(templateId)).thenReturn(List.of());
            when(templateSchemeMappingRepository.findByTemplateId(templateId)).thenReturn(mappings);

            // When
            TemplateWithWorkflowResponse response = templateService.getTemplateWithWorkflow(templateId);

            // Then
            assertThat(response.getIssueTypeScheme()).isNotNull();
            assertThat(response.getIssueTypeScheme().getId()).isEqualTo(issueTypeSchemeId);
            assertThat(response.getIssueTypeScheme().getName()).isEqualTo("Task Issue Types");
            assertThat(response.getWorkflowScheme()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Template Issue Type Tests")
    class TemplateIssueTypeTests {

        @Test
        @DisplayName("Should return issue types with subtask flag")
        void shouldReturnIssueTypesWithSubtaskFlag() {
            // Given
            ProjectType type = createProjectType();
            ProjectTemplate template = createTemplate("Project management", "BUSINESS", type);

            List<TemplateIssueType> issueTypes = List.of(
                    createIssueTypeWithFlags("Task", false, false),
                    createIssueTypeWithFlags("Sub-task", true, true),
                    createIssueTypeWithFlags("Bug", false, false)
            );

            when(projectTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));
            when(templateIssueTypeRepository.findByTemplateIdOrderBySequenceAsc(templateId)).thenReturn(issueTypes);
            when(templateWorkflowStatusRepository.findByTemplateIdOrderBySequenceAsc(templateId)).thenReturn(List.of());
            when(templateWorkflowTransitionRepository.findByTemplateIdOrderBySequenceAsc(templateId)).thenReturn(List.of());
            when(templateSchemeMappingRepository.findByTemplateId(templateId)).thenReturn(List.of());

            // When
            TemplateWithWorkflowResponse response = templateService.getTemplateWithWorkflow(templateId);

            // Then
            assertThat(response.getIssueTypes()).hasSize(3);
            assertThat(response.getIssueTypes())
                    .anyMatch(it -> "Task".equals(it.getIssueTypeName()) && !it.getIsSubtask());
            assertThat(response.getIssueTypes())
                    .anyMatch(it -> "Sub-task".equals(it.getIssueTypeName()) && it.getIsSubtask());
        }

        @Test
        @DisplayName("Should identify default issue type")
        void shouldIdentifyDefaultIssueType() {
            // Given
            ProjectType type = createProjectType();
            ProjectTemplate template = createTemplate("Project management", "BUSINESS", type);

            List<TemplateIssueType> issueTypes = List.of(
                    createIssueTypeWithFlagsAndDefault("Task", false, false, true),
                    createIssueTypeWithFlagsAndDefault("Sub-task", true, true, false)
            );

            when(projectTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));
            when(templateIssueTypeRepository.findByTemplateIdOrderBySequenceAsc(templateId)).thenReturn(issueTypes);
            when(templateWorkflowStatusRepository.findByTemplateIdOrderBySequenceAsc(templateId)).thenReturn(List.of());
            when(templateWorkflowTransitionRepository.findByTemplateIdOrderBySequenceAsc(templateId)).thenReturn(List.of());
            when(templateSchemeMappingRepository.findByTemplateId(templateId)).thenReturn(List.of());

            // When
            TemplateWithWorkflowResponse response = templateService.getTemplateWithWorkflow(templateId);

            // Then
            assertThat(response.getIssueTypes())
                    .anyMatch(it -> "Task".equals(it.getIssueTypeName()) && it.getIsDefault());
        }
    }

    // ============ Helper Methods ============

    private ProjectType createProjectType() {
        return ProjectType.builder()
                .id(typeId)
                .name("Company-managed")
                .description("Traditional project type")
                .icon("folder")
                .build();
    }

    private ProjectTemplate createTemplate(String name, String category, ProjectType type) {
        return ProjectTemplate.builder()
                .id(templateId)
                .type(type)
                .name(name)
                .description("Description for " + name)
                .icon(name.toLowerCase().replace(" ", "-"))
                .color("#0052CC")
                .category(category)
                .templateType("MANAGEMENT")
                .isActive(true)
                .sortOrder(1)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private TemplateIssueType createIssueType(String name, boolean isSubtask) {
        return TemplateIssueType.builder()
                .id(issueTypeId)
                .templateId(templateId)
                .issueTypeName(name)
                .issueTypeIcon(name.toLowerCase())
                .isDefault(!isSubtask)
                .isSubtask(isSubtask)
                .sequence(isSubtask ? 1 : 0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private TemplateIssueType createIssueTypeWithFlags(String name, boolean isDefault, boolean isSubtask) {
        return TemplateIssueType.builder()
                .id(UUID.randomUUID())
                .templateId(templateId)
                .issueTypeName(name)
                .issueTypeIcon(name.toLowerCase())
                .isDefault(isDefault)
                .isSubtask(isSubtask)
                .sequence(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private TemplateIssueType createIssueTypeWithFlagsAndDefault(String name, boolean isDefault, boolean isSubtask, boolean defaultFlag) {
        return TemplateIssueType.builder()
                .id(UUID.randomUUID())
                .templateId(templateId)
                .issueTypeName(name)
                .issueTypeIcon(name.toLowerCase())
                .isDefault(defaultFlag)
                .isSubtask(isSubtask)
                .sequence(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private TemplateWorkflowStatus createWorkflowStatus(String statusKey, int sequence) {
        return TemplateWorkflowStatus.builder()
                .id(workflowStatusId)
                .templateId(templateId)
                .statusName(statusKey.replace("_", " "))
                .statusKey(statusKey)
                .statusColor("#0052CC")
                .statusCategory(getCategoryForStatus(statusKey))
                .sequence(sequence)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private TemplateWorkflowStatus createWorkflowStatusWithSequence(String statusKey, int sequence) {
        return TemplateWorkflowStatus.builder()
                .id(UUID.randomUUID())
                .templateId(templateId)
                .statusName(statusKey.replace("_", " "))
                .statusKey(statusKey)
                .statusColor("#0052CC")
                .statusCategory(getCategoryForStatus(statusKey))
                .sequence(sequence)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private TemplateWorkflowTransition createTransition(String fromKey, String toKey) {
        return TemplateWorkflowTransition.builder()
                .id(transitionId)
                .templateId(templateId)
                .fromStatusKey(fromKey)
                .toStatusKey(toKey)
                .transitionName(fromKey + " to " + toKey)
                .allowBackward(false)
                .requiresApproval(false)
                .sequence(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private TemplateWorkflowTransition createTransitionWithDetails(String fromKey, String toKey, String name, boolean allowBackward) {
        return TemplateWorkflowTransition.builder()
                .id(UUID.randomUUID())
                .templateId(templateId)
                .fromStatusKey(fromKey)
                .toStatusKey(toKey)
                .transitionName(name)
                .transitionIcon("arrow-right")
                .allowBackward(allowBackward)
                .requiresApproval(false)
                .sequence(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private TemplateSchemeMapping createSchemeMapping(String type, String name) {
        return TemplateSchemeMapping.builder()
                .id(UUID.randomUUID())
                .templateId(templateId)
                .schemeType(type)
                .schemeName(name)
                .isDefault(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private TemplateSchemeMapping createSchemeMappingWithId(String type, String name, UUID schemeId) {
        return TemplateSchemeMapping.builder()
                .id(UUID.randomUUID())
                .templateId(templateId)
                .schemeType(type)
                .schemeName(name)
                .schemeId(schemeId)
                .isDefault(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private StatusDefinition createStatusDefinition(String key, String name, String color) {
        return StatusDefinition.builder()
                .id(UUID.randomUUID())
                .statusKey(key)
                .statusName(name)
                .statusColor(color)
                .statusCategory(getCategoryForStatus(key))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private String getCategoryForStatus(String statusKey) {
        if ("TODO".equals(statusKey) || "OPEN".equals(statusKey)) {
            return "TODO";
        } else if ("DONE".equals(statusKey) || "APPROVED".equals(statusKey) ||
                   "REJECTED".equals(statusKey) || "CANCELLED".equals(statusKey)) {
            return "DONE";
        }
        return "IN_PROGRESS";
    }
}