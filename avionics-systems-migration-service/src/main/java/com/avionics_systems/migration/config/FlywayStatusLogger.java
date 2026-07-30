package com.avionics_systems.migration.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;

@Component
@ConditionalOnBean(Flyway.class)
@Slf4j
public class FlywayStatusLogger {

    private final Flyway flyway;

    public FlywayStatusLogger(ObjectProvider<Flyway> flywayProvider) {
        this.flyway = flywayProvider.getIfAvailable();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logSchemaVersion() {
        if (flyway == null) {
            return;
        }
        MigrationInfoService info = flyway.info();
        MigrationInfo current = info.current();
        int pending = info.pending().length;
        log.info(
                "Flyway schema ready: current={}, pending={}, applied={}",
                current != null ? current.getVersion() + " " + current.getDescription() : "none",
                pending,
                info.applied().length
        );
        if (pending > 0) {
            log.warn("Flyway has {} pending migration(s) — run mvn flyway:migrate", pending);
        }
    }
}
