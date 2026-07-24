package com.jira.workflow.engine.script;

import com.jira.workflow.entity.ScriptDefinition;
import com.jira.workflow.entity.ScriptSchedule;
import com.jira.workflow.repository.ScriptDefinitionRepository;
import com.jira.workflow.repository.ScriptScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "jira.scripting.scheduled-enabled", havingValue = "true")
public class ScheduledScriptExecutor {

    private final ScriptScheduleRepository scheduleRepository;
    private final ScriptDefinitionRepository scriptDefinitionRepository;
    private final ScriptExecutionService executionService;

    @Scheduled(fixedDelayString = "${jira.scripting.scheduled-poll-interval-ms:30000}")
    public void pollAndExecute() {
        List<ScriptSchedule> due = scheduleRepository
                .findByIsEnabledTrueAndNextRunAtBefore(LocalDateTime.now());

        for (ScriptSchedule schedule : due) {
            try {
                ScriptDefinition script = scriptDefinitionRepository.findById(schedule.getScriptId())
                        .orElse(null);
                if (script == null || !Boolean.TRUE.equals(script.getIsEnabled())) {
                    log.warn("Scheduled script {} not found or disabled, skipping", schedule.getScriptId());
                    continue;
                }

                Map<String, Object> ctx = Map.of(
                        "scheduleId", schedule.getId().toString(),
                        "executionMode", "SCHEDULED"
                );

                ScriptResult result = executionService.executeByKey(script.getScriptKey(), ctx, "SCHEDULED");

                schedule.setLastRunAt(LocalDateTime.now());
                schedule.setLastSuccess(result.success());
                schedule.setLastResult(result.success()
                        ? String.valueOf(result.value())
                        : result.errorMessage());
                schedule.setRunCount(schedule.getRunCount() + 1);
                schedule.setNextRunAt(calculateNextRun(schedule.getCronExpression()));
                scheduleRepository.save(schedule);

                log.info("Scheduled script '{}' executed: success={}, time={}ms",
                        script.getScriptKey(), result.success(), result.executionMs());

            } catch (Exception e) {
                log.error("Scheduled script {} failed unexpectedly", schedule.getId(), e);
                schedule.setLastSuccess(false);
                schedule.setLastResult(e.getMessage());
                schedule.setNextRunAt(calculateNextRun(schedule.getCronExpression()));
                scheduleRepository.save(schedule);
            }
        }
    }

    private LocalDateTime calculateNextRun(String cronExpression) {
        try {
            CronExpression cron = CronExpression.parse(cronExpression);
            return cron.next(LocalDateTime.now());
        } catch (Exception e) {
            log.warn("Invalid cron expression '{}', defaulting to 1 hour", cronExpression);
            return LocalDateTime.now().plusHours(1);
        }
    }
}
