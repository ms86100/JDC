package com.avionics_systems.workflow.engine.script;

import com.avionics_systems.workflow.config.ScriptEngineProperties;
import com.avionics_systems.workflow.repository.ScriptExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScriptLogCleanupJob {

    private final ScriptExecutionLogRepository executionLogRepository;
    private final ScriptEngineProperties properties;

    @Scheduled(cron = "0 0 2 * * *")
    @SchedulerLock(name = "ScriptLogCleanupJob_cleanupOldLogs", lockAtMostFor = "PT30M", lockAtLeastFor = "PT5M")
    @Transactional
    public void cleanupOldLogs() {
        int retentionDays = properties.getLogRetentionDays();
        if (retentionDays <= 0) return;

        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        long count = executionLogRepository.countByCreatedAtBefore(cutoff);
        if (count > 0) {
            executionLogRepository.deleteByCreatedAtBefore(cutoff);
            log.info("Cleaned up {} script execution logs older than {} days", count, retentionDays);
        }
    }
}
