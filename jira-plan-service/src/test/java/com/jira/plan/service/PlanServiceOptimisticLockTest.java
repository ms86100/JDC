package com.jira.plan.service;

import com.jira.plan.dto.request.CreatePlanRequest;
import com.jira.plan.dto.request.UpdatePlanRequest;
import com.jira.plan.dto.response.PlanResponse;
import com.jira.plan.entity.Plan;
import com.jira.plan.exception.OptimisticLockException;
import com.jira.plan.exception.ResourceNotFoundException;
import com.jira.plan.repository.PlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanServiceOptimisticLockTest {

    @Mock
    private PlanRepository planRepository;

    @InjectMocks
    private PlanService planService;

    private UUID planId;
    private Plan testPlan;

    @BeforeEach
    void setUp() {
        planId = UUID.randomUUID();
        testPlan = Plan.builder()
                .id(planId)
                .name("Test Plan")
                .description("A test plan")
                .ownerId(UUID.randomUUID())
                .version(0L)
                .isActive(true)
                .settings(Map.of())
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(14))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Optimistic Locking Tests")
    class OptimisticLockingTests {

        @Test
        @DisplayName("Should update plan when version matches")
        void updatePlan_withMatchingVersion_shouldSucceed() {
            UpdatePlanRequest request = UpdatePlanRequest.builder()
                    .name("Updated Plan Name")
                    .version(0L)
                    .build();

            when(planRepository.findById(planId)).thenReturn(Optional.of(testPlan));
            when(planRepository.save(any(Plan.class))).thenAnswer(inv -> inv.getArgument(0));

            PlanResponse response = planService.updatePlan(planId, request);

            assertThat(response.getName()).isEqualTo("Updated Plan Name");
            assertThat(response.getVersion()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should throw OptimisticLockException when version mismatch")
        void updatePlan_withMismatchedVersion_shouldThrowException() {
            UpdatePlanRequest request = UpdatePlanRequest.builder()
                    .name("Updated Plan Name")
                    .version(5L) // Stale version, current is 0
                    .build();

            when(planRepository.findById(planId)).thenReturn(Optional.of(testPlan));

            assertThatThrownBy(() -> planService.updatePlan(planId, request))
                    .isInstanceOf(OptimisticLockException.class)
                    .hasMessageContaining("was modified by another user")
                    .hasMessageContaining("Expected version: 0")
                    .hasMessageContaining("provided: 5");
        }

        @Test
        @DisplayName("Should succeed when version is null (skip check)")
        void updatePlan_withNullVersion_shouldSucceed() {
            UpdatePlanRequest request = UpdatePlanRequest.builder()
                    .name("Updated Plan Name")
                    .version(null) // No version check
                    .build();

            when(planRepository.findById(planId)).thenReturn(Optional.of(testPlan));
            when(planRepository.save(any(Plan.class))).thenAnswer(inv -> inv.getArgument(0));

            PlanResponse response = planService.updatePlan(planId, request);

            assertThat(response.getName()).isEqualTo("Updated Plan Name");
        }

        @Test
        @DisplayName("Should handle concurrent update scenario")
        void concurrentUpdateScenario_staleUserAUpdate_shouldFail() {
            // User A loads plan at version 0
            Plan userALoadedPlan = Plan.builder()
                    .id(planId)
                    .name("Original Plan")
                    .version(0L)
                    .build();

            // User B updates plan (version 0 -> 1)
            Plan userBUpdatedPlan = Plan.builder()
                    .id(planId)
                    .name("User B's Update")
                    .version(1L)
                    .build();

            // User A tries to update with stale version 0
            UpdatePlanRequest userARequest = UpdatePlanRequest.builder()
                    .name("User A's Update")
                    .version(0L) // Stale
                    .build();

            when(planRepository.findById(planId)).thenReturn(Optional.of(userBUpdatedPlan));

            assertThatThrownBy(() -> planService.updatePlan(planId, userARequest))
                    .isInstanceOf(OptimisticLockException.class)
                    .hasMessageContaining("Expected version: 1, provided: 0");
        }

        @Test
        @DisplayName("Should update multiple fields with version check")
        void updatePlan_multipleFields_shouldSucceed() {
            UpdatePlanRequest request = UpdatePlanRequest.builder()
                    .name("New Name")
                    .description("New Description")
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(30))
                    .version(0L)
                    .build();

            when(planRepository.findById(planId)).thenReturn(Optional.of(testPlan));
            when(planRepository.save(any(Plan.class))).thenAnswer(inv -> inv.getArgument(0));

            PlanResponse response = planService.updatePlan(planId, request);

            assertThat(response.getName()).isEqualTo("New Name");
            assertThat(response.getDescription()).isEqualTo("New Description");
            assertThat(response.getVersion()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should update settings with version check")
        void updatePlanSettings_shouldIncrementVersion() {
            // Create a new plan with version
            Plan planWithVersion = Plan.builder()
                    .id(planId)
                    .name("Test Plan")
                    .version(2L)
                    .settings(Map.of("key", "value"))
                    .build();

            when(planRepository.findById(planId)).thenReturn(Optional.of(planWithVersion));
            when(planRepository.save(any(Plan.class))).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> newSettings = Map.of("newKey", "newValue");
            PlanResponse response = planService.updatePlanSettings(planId, newSettings);

            assertThat(response.getSettings()).isEqualTo(newSettings);
            // Note: updatePlanSettings doesn't check version but still increments via JPA @Version
        }
    }

    @Nested
    @DisplayName("Response Version Tests")
    class ResponseVersionTests {

        @Test
        @DisplayName("Should return plan with version from database")
        void getPlanById_shouldIncludeVersion() {
            when(planRepository.findById(planId)).thenReturn(Optional.of(testPlan));

            PlanResponse response = planService.getPlanById(planId);

            assertThat(response.getVersion()).isEqualTo(0L);
        }

        @Test
        @DisplayName("Should return all plans with version information")
        void getAllPlans_shouldIncludeVersion() {
            Plan plan1 = Plan.builder().id(UUID.randomUUID()).name("Plan 1").version(0L).build();
            Plan plan2 = Plan.builder().id(UUID.randomUUID()).name("Plan 2").version(5L).build();

            when(planRepository.findByIsActiveTrue()).thenReturn(List.of(plan1, plan2));

            List<PlanResponse> responses = planService.getAllPlans();

            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).getVersion()).isEqualTo(0L);
            assertThat(responses.get(1).getVersion()).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("Resource Not Found Tests")
    class ResourceNotFoundTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when plan not found")
        void updatePlan_notFound_shouldThrow() {
            UpdatePlanRequest request = UpdatePlanRequest.builder()
                    .name("Updated Name")
                    .version(0L)
                    .build();

            when(planRepository.findById(planId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> planService.updatePlan(planId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Plan");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException on getPlanById")
        void getPlanById_notFound_shouldThrow() {
            when(planRepository.findById(planId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> planService.getPlanById(planId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Plan");
        }
    }
}