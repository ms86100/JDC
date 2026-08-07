package com.avionics_systems.test.service;

import com.avionics_systems.test.dto.*;
import com.avionics_systems.test.entity.ExecutionStatusConfig;
import com.avionics_systems.test.entity.TestStatusConfig;
import com.avionics_systems.test.entity.TestTypeConfig;
import com.avionics_systems.test.exception.DuplicateResourceException;
import com.avionics_systems.test.exception.InvalidOperationException;
import com.avionics_systems.test.exception.ResourceNotFoundException;
import com.avionics_systems.test.repository.ExecutionStatusConfigRepository;
import com.avionics_systems.test.repository.TestIssueRepository;
import com.avionics_systems.test.repository.TestStatusConfigRepository;
import com.avionics_systems.test.repository.TestTypeConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestAdminConfigService {

    private final TestStatusConfigRepository testStatusConfigRepository;
    private final ExecutionStatusConfigRepository executionStatusConfigRepository;
    private final TestTypeConfigRepository testTypeConfigRepository;
    private final TestIssueRepository testIssueRepository;

    // ===================== Test Status Config =====================

    @Transactional(readOnly = true)
    @Cacheable(value = "test-status-config", key = "'all'")
    public List<TestStatusConfigResponse> getAllStatuses() {
        log.debug("Fetching all active test status configs");
        return testStatusConfigRepository.findByIsActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(this::mapToStatusResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "test-status-config", key = "#projectId")
    public List<TestStatusConfigResponse> getStatusesByProject(UUID projectId) {
        log.debug("Fetching test status configs for project: {}", projectId);
        List<TestStatusConfig> projectStatuses = testStatusConfigRepository
                .findByProjectIdAndIsActiveTrueOrderBySortOrderAsc(projectId);
        if (projectStatuses.isEmpty()) {
            // Fall back to global statuses
            return testStatusConfigRepository.findByProjectIdIsNullAndIsActiveTrueOrderBySortOrderAsc()
                    .stream()
                    .map(this::mapToStatusResponse)
                    .toList();
        }
        return projectStatuses.stream().map(this::mapToStatusResponse).toList();
    }

    @Transactional
    @CacheEvict(value = "test-status-config", allEntries = true)
    public TestStatusConfigResponse createStatus(TestStatusConfigRequest request) {
        log.info("Creating test status config: {}", request.getName());
        if (testStatusConfigRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Test status config already exists with name: " + request.getName());
        }

        TestStatusConfig config = TestStatusConfig.builder()
                .projectId(request.getProjectId())
                .name(request.getName())
                .displayName(request.getDisplayName())
                .color(request.getColor() != null ? request.getColor() : "#6B7280")
                .icon(request.getIcon())
                .category(request.getCategory() != null ? request.getCategory() : "TODO")
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .isFinal(request.getIsFinal() != null ? request.getIsFinal() : false)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        TestStatusConfig saved = testStatusConfigRepository.save(config);
        return mapToStatusResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "test-status-config", allEntries = true)
    public TestStatusConfigResponse updateStatus(UUID id, TestStatusConfigRequest request) {
        log.info("Updating test status config: {}", id);
        TestStatusConfig config = testStatusConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TestStatusConfig", "id", id));

        if (request.getName() != null) config.setName(request.getName());
        if (request.getDisplayName() != null) config.setDisplayName(request.getDisplayName());
        if (request.getColor() != null) config.setColor(request.getColor());
        if (request.getIcon() != null) config.setIcon(request.getIcon());
        if (request.getCategory() != null) config.setCategory(request.getCategory());
        if (request.getIsDefault() != null) config.setIsDefault(request.getIsDefault());
        if (request.getIsFinal() != null) config.setIsFinal(request.getIsFinal());
        if (request.getSortOrder() != null) config.setSortOrder(request.getSortOrder());
        if (request.getIsActive() != null) config.setIsActive(request.getIsActive());

        TestStatusConfig saved = testStatusConfigRepository.save(config);
        return mapToStatusResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "test-status-config", allEntries = true)
    public void deleteStatus(UUID id) {
        log.info("Deleting test status config: {}", id);
        TestStatusConfig config = testStatusConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TestStatusConfig", "id", id));

        long usageCount = testIssueRepository.countByStatus(config.getName());

        if (usageCount > 0) {
            throw new InvalidOperationException(
                    "Cannot delete status '" + config.getName() + "' because it is used by " + usageCount + " test(s)");
        }

        testStatusConfigRepository.delete(config);
    }

    // ===================== Execution Status Config =====================

    @Transactional(readOnly = true)
    @Cacheable(value = "execution-status-config", key = "'all'")
    public List<ExecutionStatusConfigResponse> getAllExecutionStatuses() {
        log.debug("Fetching all active execution status configs");
        return executionStatusConfigRepository.findByIsActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(this::mapToExecutionStatusResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "execution-status-config", key = "#projectId")
    public List<ExecutionStatusConfigResponse> getExecutionStatusesByProject(UUID projectId) {
        log.debug("Fetching execution status configs for project: {}", projectId);
        List<ExecutionStatusConfig> projectStatuses = executionStatusConfigRepository
                .findByProjectIdAndIsActiveTrueOrderBySortOrderAsc(projectId);
        if (projectStatuses.isEmpty()) {
            return executionStatusConfigRepository.findByProjectIdIsNullAndIsActiveTrueOrderBySortOrderAsc()
                    .stream()
                    .map(this::mapToExecutionStatusResponse)
                    .toList();
        }
        return projectStatuses.stream().map(this::mapToExecutionStatusResponse).toList();
    }

    @Transactional
    @CacheEvict(value = "execution-status-config", allEntries = true)
    public ExecutionStatusConfigResponse createExecutionStatus(ExecutionStatusConfigRequest request) {
        log.info("Creating execution status config: {}", request.getName());
        if (executionStatusConfigRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Execution status config already exists with name: " + request.getName());
        }

        ExecutionStatusConfig config = ExecutionStatusConfig.builder()
                .projectId(request.getProjectId())
                .name(request.getName())
                .displayName(request.getDisplayName())
                .color(request.getColor() != null ? request.getColor() : "#6B7280")
                .icon(request.getIcon())
                .isPass(request.getIsPass() != null ? request.getIsPass() : false)
                .isFail(request.getIsFail() != null ? request.getIsFail() : false)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        ExecutionStatusConfig saved = executionStatusConfigRepository.save(config);
        return mapToExecutionStatusResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "execution-status-config", allEntries = true)
    public ExecutionStatusConfigResponse updateExecutionStatus(UUID id, ExecutionStatusConfigRequest request) {
        log.info("Updating execution status config: {}", id);
        ExecutionStatusConfig config = executionStatusConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExecutionStatusConfig", "id", id));

        if (request.getName() != null) config.setName(request.getName());
        if (request.getDisplayName() != null) config.setDisplayName(request.getDisplayName());
        if (request.getColor() != null) config.setColor(request.getColor());
        if (request.getIcon() != null) config.setIcon(request.getIcon());
        if (request.getIsPass() != null) config.setIsPass(request.getIsPass());
        if (request.getIsFail() != null) config.setIsFail(request.getIsFail());
        if (request.getSortOrder() != null) config.setSortOrder(request.getSortOrder());
        if (request.getIsActive() != null) config.setIsActive(request.getIsActive());

        ExecutionStatusConfig saved = executionStatusConfigRepository.save(config);
        return mapToExecutionStatusResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "execution-status-config", allEntries = true)
    public void deleteExecutionStatus(UUID id) {
        log.info("Deleting execution status config: {}", id);
        ExecutionStatusConfig config = executionStatusConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExecutionStatusConfig", "id", id));

        executionStatusConfigRepository.delete(config);
    }

    // ===================== Test Type Config =====================

    @Transactional(readOnly = true)
    @Cacheable(value = "test-type-config", key = "'all'")
    public List<TestTypeConfigResponse> getAllTestTypes() {
        log.debug("Fetching all active test type configs");
        return testTypeConfigRepository.findByIsActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(this::mapToTestTypeResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "test-type-config", key = "#projectId")
    public List<TestTypeConfigResponse> getTestTypesByProject(UUID projectId) {
        log.debug("Fetching test type configs for project: {}", projectId);
        List<TestTypeConfig> projectTypes = testTypeConfigRepository
                .findByProjectIdAndIsActiveTrueOrderBySortOrderAsc(projectId);
        if (projectTypes.isEmpty()) {
            return testTypeConfigRepository.findByProjectIdIsNullAndIsActiveTrueOrderBySortOrderAsc()
                    .stream()
                    .map(this::mapToTestTypeResponse)
                    .toList();
        }
        return projectTypes.stream().map(this::mapToTestTypeResponse).toList();
    }

    @Transactional
    @CacheEvict(value = "test-type-config", allEntries = true)
    public TestTypeConfigResponse createTestType(TestTypeConfigRequest request) {
        log.info("Creating test type config: {}", request.getName());
        if (testTypeConfigRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Test type config already exists with name: " + request.getName());
        }

        TestTypeConfig config = TestTypeConfig.builder()
                .projectId(request.getProjectId())
                .name(request.getName())
                .displayName(request.getDisplayName())
                .description(request.getDescription())
                .icon(request.getIcon())
                .color(request.getColor() != null ? request.getColor() : "#6B7280")
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        TestTypeConfig saved = testTypeConfigRepository.save(config);
        return mapToTestTypeResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "test-type-config", allEntries = true)
    public TestTypeConfigResponse updateTestType(UUID id, TestTypeConfigRequest request) {
        log.info("Updating test type config: {}", id);
        TestTypeConfig config = testTypeConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TestTypeConfig", "id", id));

        if (request.getName() != null) config.setName(request.getName());
        if (request.getDisplayName() != null) config.setDisplayName(request.getDisplayName());
        if (request.getDescription() != null) config.setDescription(request.getDescription());
        if (request.getIcon() != null) config.setIcon(request.getIcon());
        if (request.getColor() != null) config.setColor(request.getColor());
        if (request.getSortOrder() != null) config.setSortOrder(request.getSortOrder());
        if (request.getIsActive() != null) config.setIsActive(request.getIsActive());

        TestTypeConfig saved = testTypeConfigRepository.save(config);
        return mapToTestTypeResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "test-type-config", allEntries = true)
    public void deleteTestType(UUID id) {
        log.info("Deleting test type config: {}", id);
        TestTypeConfig config = testTypeConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TestTypeConfig", "id", id));

        long usageCount = testIssueRepository.countByTestType(config.getName());

        if (usageCount > 0) {
            throw new InvalidOperationException(
                    "Cannot delete test type '" + config.getName() + "' because it is used by " + usageCount + " test(s)");
        }

        testTypeConfigRepository.delete(config);
    }

    // ===================== Mappers =====================

    private TestStatusConfigResponse mapToStatusResponse(TestStatusConfig config) {
        return TestStatusConfigResponse.builder()
                .id(config.getId())
                .projectId(config.getProjectId())
                .name(config.getName())
                .displayName(config.getDisplayName())
                .color(config.getColor())
                .icon(config.getIcon())
                .category(config.getCategory())
                .isDefault(config.getIsDefault())
                .isFinal(config.getIsFinal())
                .sortOrder(config.getSortOrder())
                .isActive(config.getIsActive())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    private ExecutionStatusConfigResponse mapToExecutionStatusResponse(ExecutionStatusConfig config) {
        return ExecutionStatusConfigResponse.builder()
                .id(config.getId())
                .projectId(config.getProjectId())
                .name(config.getName())
                .displayName(config.getDisplayName())
                .color(config.getColor())
                .icon(config.getIcon())
                .isPass(config.getIsPass())
                .isFail(config.getIsFail())
                .sortOrder(config.getSortOrder())
                .isActive(config.getIsActive())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    private TestTypeConfigResponse mapToTestTypeResponse(TestTypeConfig config) {
        return TestTypeConfigResponse.builder()
                .id(config.getId())
                .projectId(config.getProjectId())
                .name(config.getName())
                .displayName(config.getDisplayName())
                .description(config.getDescription())
                .icon(config.getIcon())
                .color(config.getColor())
                .sortOrder(config.getSortOrder())
                .isActive(config.getIsActive())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
