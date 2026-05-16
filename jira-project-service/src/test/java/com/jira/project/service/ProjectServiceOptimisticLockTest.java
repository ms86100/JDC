package com.jira.project.service;

import com.jira.project.dto.CreateProjectRequest;
import com.jira.project.dto.ProjectResponse;
import com.jira.project.dto.UpdateProjectRequest;
import com.jira.project.entity.Project;
import com.jira.project.exception.OptimisticLockException;
import com.jira.project.exception.ResourceNotFoundException;
import com.jira.project.repository.ProjectMemberRepository;
import com.jira.project.repository.ProjectRepository;
import com.jira.project.repository.ProjectRoleRepository;
import com.jira.project.repository.ProjectTemplateRepository;
import com.jira.project.repository.TemplateSchemeDefaultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceOptimisticLockTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectRoleRepository projectRoleRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectTemplateRepository projectTemplateRepository;

    @Mock
    private TemplateSchemeDefaultRepository templateSchemeDefaultRepository;

    @Mock
    private com.jira.project.service.ProjectSchemeService projectSchemeService;

    @InjectMocks
    private ProjectService projectService;

    private UUID projectId;
    private Project testProject;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        testProject = Project.builder()
                .id(projectId)
                .projectKey("TEST")
                .name("Test Project")
                .description("A test project")
                .projectType("COMPANY_MANAGED")
                .version(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Optimistic Locking Tests")
    class OptimisticLockingTests {

        @Test
        @DisplayName("Should update project when version matches")
        void updateProject_withMatchingVersion_shouldSucceed() {
            UpdateProjectRequest request = UpdateProjectRequest.builder()
                    .name("Updated Name")
                    .version(0L)
                    .build();

            when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
            when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

            ProjectResponse response = projectService.updateProject(projectId, request);

            assertThat(response.getName()).isEqualTo("Updated Name");
            assertThat(response.getVersion()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should throw OptimisticLockException when version mismatch")
        void updateProject_withMismatchedVersion_shouldThrowException() {
            UpdateProjectRequest request = UpdateProjectRequest.builder()
                    .name("Updated Name")
                    .version(5L) // Stale version, current is 0
                    .build();

            when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));

            assertThatThrownBy(() -> projectService.updateProject(projectId, request))
                    .isInstanceOf(OptimisticLockException.class)
                    .hasMessageContaining("was modified by another user")
                    .hasMessageContaining("Expected version: 0")
                    .hasMessageContaining("provided: 5");
        }

        @Test
        @DisplayName("Should succeed when version is null (skip check)")
        void updateProject_withNullVersion_shouldSucceed() {
            UpdateProjectRequest request = UpdateProjectRequest.builder()
                    .name("Updated Name")
                    .version(null) // No version check
                    .build();

            when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
            when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

            ProjectResponse response = projectService.updateProject(projectId, request);

            assertThat(response.getName()).isEqualTo("Updated Name");
        }

        @Test
        @DisplayName("Should handle concurrent update scenario - User A loads, User B updates, User A tries stale update")
        void concurrentUpdateScenario_staleUserAUpdate_shouldFail() {
            // User A loads project at version 0
            Project userALoadedProject = Project.builder()
                    .id(projectId)
                    .projectKey("TEST")
                    .name("Original Name")
                    .version(0L)
                    .build();

            // User B updates project (version 0 -> 1)
            Project userBUpdatedProject = Project.builder()
                    .id(projectId)
                    .projectKey("TEST")
                    .name("User B's Update")
                    .version(1L)
                    .build();

            // User A tries to update with stale version 0
            UpdateProjectRequest userARequest = UpdateProjectRequest.builder()
                    .name("User A's Update")
                    .version(0L) // Stale
                    .build();

            when(projectRepository.findById(projectId)).thenReturn(Optional.of(userBUpdatedProject));

            assertThatThrownBy(() -> projectService.updateProject(projectId, userARequest))
                    .isInstanceOf(OptimisticLockException.class)
                    .hasMessageContaining("Expected version: 1, provided: 0");
        }

        @Test
        @DisplayName("Should include version in response after update")
        void updateProject_shouldIncludeNewVersionInResponse() {
            UpdateProjectRequest request = UpdateProjectRequest.builder()
                    .name("Updated Name")
                    .version(0L)
                    .build();

            when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
            when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
                Project p = inv.getArgument(0);
                p.setVersion(p.getVersion() + 1);
                return p;
            });

            ProjectResponse response = projectService.updateProject(projectId, request);

            assertThat(response.getVersion()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when project not found")
        void updateProject_projectNotFound_shouldThrow() {
            UpdateProjectRequest request = UpdateProjectRequest.builder()
                    .name("Updated Name")
                    .version(0L)
                    .build();

            when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.updateProject(projectId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Project");
        }
    }

    @Nested
    @DisplayName("Response Version Tests")
    class ResponseVersionTests {

        @Test
        @DisplayName("Should return project with version from database")
        void getProject_shouldIncludeVersion() {
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));

            ProjectResponse response = projectService.getProject(projectId);

            assertThat(response.getVersion()).isEqualTo(0L);
        }
    }
}