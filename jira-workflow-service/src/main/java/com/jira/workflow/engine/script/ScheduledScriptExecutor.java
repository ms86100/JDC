package com.jira.workflow.engine.script;

import com.jira.workflow.entity.ScriptDefinition;
import com.jira.workflow.entity.ScriptSchedule;
import com.jira.workflow.repository.ScriptDefinitionRepository;
import com.jira.workflow.repository.ScriptScheduleRepository;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@ConditionalOnProperty(name = "jira.scripting.scheduled-enabled", havingValue = "true")
public class ScheduledScriptExecutor {

    private final ScriptScheduleRepository scheduleRepository;
    private final ScriptDefinitionRepository scriptDefinitionRepository;
    private final ScriptExecutionService executionService;
    private final ExecutorService scriptPool;

    public ScheduledScriptExecutor(ScriptScheduleRepository scheduleRepository,
                                    ScriptDefinitionRepository scriptDefinitionRepository,
                                    ScriptExecutionService executionService) {
        this.scheduleRepository = scheduleRepository;
        this.scriptDefinitionRepository = scriptDefinitionRepository;
        this.executionService = executionService;
        this.scriptPool = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "scheduled-script");
            t.setDaemon(true);
            return t;
        });
    }

    @PreDestroy
    public void shutdown() {
        scriptPool.shutdown();
        try { scriptPool.awaitTermination(10, TimeUnit.SECONDS); }
        catch (InterruptedException ignored) { scriptPool.shutdownNow(); }
    }

    @Scheduled(fixedDelayString = "${jira.scripting.scheduled-poll-interval-ms:30000}")
    @SchedulerLock(name = "ScheduledScriptExecutor_pollAndExecute", lockAtMostFor = "PT24S", lockAtLeastFor = "PT12S")
    public void pollAndExecute() {
        List<ScriptSchedule> due = scheduleRepository
                .findByIsEnabledTrueAndNextRunAtBefore(LocalDateTime.now());

        for (ScriptSchedule schedule : due) {
            schedule.setNextRunAt(calculateNextRun(schedule.getCronExpression()));
            scheduleRepository.save(schedule);

            scriptPool.submit(() -> executeScheduledScript(schedule));
        }
    }

    private void executeScheduledScript(ScriptSchedule schedule) {
        try {
            ScriptDefinition script = scriptDefinitionRepository.findById(schedule.getScriptId())
                    .orElse(null);
            if (script == null || !Boolean.TRUE.equals(script.getIsEnabled())) {
                log.warn("Scheduled script {} not found or disabled", schedule.getScriptId());
                return;
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
            scheduleRepository.save(schedule);

            log.info("Scheduled script '{}' executed: success={}, time={}ms",
                    script.getScriptKey(), result.success(), result.executionMs());

        } catch (Exception e) {
            log.error("Scheduled script {} failed unexpectedly", schedule.getId(), e);
            schedule.setLastSuccess(false);
            schedule.setLastResult(e.getMessage());
            scheduleRepository.save(schedule);
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
