package com.jira.user.scheduler;

import com.jira.user.entity.Directory;
import com.jira.user.repository.DirectoryRepository;
import com.jira.user.service.LdapSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DirectorySyncScheduler {

    private final DirectoryRepository directoryRepository;
    private final LdapSyncService ldapSyncService;

    @Value("${app.ldap.sync-status.syncing:SYNCING}")
    private String syncStatusSyncing;

    @Value("${app.defaults.sync-interval-minutes:60}")
    private int defaultSyncIntervalMinutes;

    @Scheduled(fixedDelayString = "${ldap.sync.check-interval-ms:60000}")
    @SchedulerLock(name = "DirectorySyncScheduler_checkAndSyncDirectories", lockAtMostFor = "PT48S", lockAtLeastFor = "PT24S")
    public void checkAndSyncDirectories() {
        List<Directory> directories = directoryRepository.findByIsActiveTrueOrderByOrderIndexAsc();

        for (Directory directory : directories) {
            if (!isLdapDirectory(directory)) {
                continue;
            }

            if (directory.getServerUrl() == null || directory.getServerUrl().isBlank()) {
                continue;
            }

            if (syncStatusSyncing.equals(directory.getSyncStatus())) {
                continue;
            }

            int intervalMinutes = directory.getSyncIntervalMinutes() != null
                    ? directory.getSyncIntervalMinutes() : defaultSyncIntervalMinutes;

            if (directory.getLastSyncAt() == null ||
                    directory.getLastSyncAt().plusMinutes(intervalMinutes).isBefore(LocalDateTime.now())) {
                try {
                    log.info("Scheduled LDAP sync triggered for directory: {} ({})",
                            directory.getDirectoryName(), directory.getId());
                    ldapSyncService.syncDirectory(directory.getId());
                } catch (Exception e) {
                    log.error("Scheduled sync failed for directory {}: {}",
                            directory.getId(), e.getMessage());
                }
            }
        }
    }

    private boolean isLdapDirectory(Directory directory) {
        String type = directory.getDirectoryType();
        return "LDAP".equalsIgnoreCase(type) || "AD".equalsIgnoreCase(type);
    }
}
