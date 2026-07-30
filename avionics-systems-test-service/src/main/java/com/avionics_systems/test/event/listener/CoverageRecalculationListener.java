package com.avionics_systems.test.event.listener;

import com.avionics_systems.test.event.CoverageRecalculatedEvent;
import com.avionics_systems.test.event.RequirementUpdatedEvent;
import com.avionics_systems.test.event.TestRunUpdatedEvent;
import com.avionics_systems.test.service.CoverageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CoverageRecalculationListener {

    private final CoverageService coverageService;

    @Async
    @EventListener
    public void onTestRunUpdated(TestRunUpdatedEvent event) {
        log.info("CoverageRecalculationListener: Received TestRunUpdatedEvent for execution: {}",
                event.getExecutionId());
        try {
            coverageService.recalculateCoverage(event.getProjectId(), event.getTestId());
            log.info("Coverage recalculated successfully for project: {}", event.getProjectId());
        } catch (Exception e) {
            log.error("Failed to recalculate coverage for project: {} - {}",
                    event.getProjectId(), e.getMessage(), e);
        }
    }

    @Async
    @EventListener
    public void onRequirementUpdated(RequirementUpdatedEvent event) {
        log.info("CoverageRecalculationListener: Received RequirementUpdatedEvent for requirement: {}",
                event.getRequirementKey());
        try {
            if (event.getRequirementId() != null) {
                coverageService.recalculateRequirementCoverage(event.getProjectId(), event.getRequirementId());
                log.info("Requirement coverage recalculated for: {}", event.getRequirementKey());
            }
        } catch (Exception e) {
            log.error("Failed to recalculate requirement coverage: {} - {}",
                    event.getRequirementKey(), e.getMessage(), e);
        }
    }

    @Async
    @EventListener
    public void onCoverageRecalculated(CoverageRecalculatedEvent event) {
        log.info("CoverageRecalculationListener: Received CoverageRecalculatedEvent - coverage: {}%",
                event.getCoveragePercentage());
    }
}