package com.avionics_systems.test.service;

import com.avionics_systems.test.dto.TestProjectSettingsRequest;
import com.avionics_systems.test.dto.TestProjectSettingsResponse;
import com.avionics_systems.test.entity.TestProjectSettings;
import com.avionics_systems.test.repository.TestProjectSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestSettingsService {

    private final TestProjectSettingsRepository testProjectSettingsRepository;

    @Transactional(readOnly = true)
    public TestProjectSettingsResponse getSettingsByProject(UUID projectId) {
        log.debug("Fetching test project settings for project: {}", projectId);
        return testProjectSettingsRepository.findByProjectId(projectId)
                .map(this::mapToResponse)
                .orElseGet(() -> mapToResponse(buildDefaultSettings(projectId)));
    }

    @Transactional
    public TestProjectSettingsResponse saveSettings(TestProjectSettingsRequest request) {
        log.info("Saving test project settings for project: {}", request.getProjectId());

        TestProjectSettings settings = testProjectSettingsRepository.findByProjectId(request.getProjectId())
                .orElseGet(() -> TestProjectSettings.builder()
                        .projectId(request.getProjectId())
                        .build());

        if (request.getSettings() != null) settings.setSettings(request.getSettings());
        if (request.getDefaultTestType() != null) settings.setDefaultTestType(request.getDefaultTestType());
        if (request.getDefaultPriority() != null) settings.setDefaultPriority(request.getDefaultPriority());
        if (request.getDefaultTestStatus() != null) settings.setDefaultTestStatus(request.getDefaultTestStatus());
        if (request.getAutoCreateExecution() != null) settings.setAutoCreateExecution(request.getAutoCreateExecution());
        if (request.getRequireApproval() != null) settings.setRequireApproval(request.getRequireApproval());
        if (request.getRetentionDays() != null) settings.setRetentionDays(request.getRetentionDays());
        if (request.getMaxStepsPerTest() != null) settings.setMaxStepsPerTest(request.getMaxStepsPerTest());

        TestProjectSettings saved = testProjectSettingsRepository.save(settings);
        return mapToResponse(saved);
    }

    private TestProjectSettings buildDefaultSettings(UUID projectId) {
        return TestProjectSettings.builder()
                .projectId(projectId)
                .settings("{}")
                .defaultTestType("MANUAL")
                .defaultPriority("MEDIUM")
                .defaultTestStatus("DRAFT")
                .autoCreateExecution(false)
                .requireApproval(false)
                .retentionDays(365)
                .maxStepsPerTest(100)
                .build();
    }

    private TestProjectSettingsResponse mapToResponse(TestProjectSettings settings) {
        return TestProjectSettingsResponse.builder()
                .id(settings.getId())
                .projectId(settings.getProjectId())
                .settings(settings.getSettings())
                .defaultTestType(settings.getDefaultTestType())
                .defaultPriority(settings.getDefaultPriority())
                .defaultTestStatus(settings.getDefaultTestStatus())
                .autoCreateExecution(settings.getAutoCreateExecution())
                .requireApproval(settings.getRequireApproval())
                .retentionDays(settings.getRetentionDays())
                .maxStepsPerTest(settings.getMaxStepsPerTest())
                .createdAt(settings.getCreatedAt())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }
}
