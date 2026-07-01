package com.jira.migration;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class JiraMigrationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(JiraMigrationServiceApplication.class, args);
    }

    @Bean
    ApplicationRunner unhideCustomFields(JdbcTemplate jdbc) {
        return args -> {
            try {
                // Fix trigger that references non-existent updated_by column
                jdbc.execute(
                    "CREATE OR REPLACE FUNCTION jira_migration.increment_field_version() " +
                    "RETURNS TRIGGER AS $$ BEGIN " +
                    "INSERT INTO jira_migration.field_version_history (" +
                    "field_definition_id, version, change_type, field_key, display_name, description, " +
                    "field_type, renderer, screen_region, schema_definition, renderer_config, validation_rules, options, " +
                    "searchable, sortable, filterable, required, read_only, hidden, " +
                    "changed_at, changed_by, change_reason" +
                    ") VALUES (" +
                    "OLD.id, OLD.version, 'UPDATED', OLD.field_key, OLD.display_name, OLD.description, " +
                    "OLD.field_type::TEXT, OLD.renderer::TEXT, OLD.screen_region::TEXT, " +
                    "OLD.schema_definition, OLD.renderer_config, OLD.validation_rules, OLD.options, " +
                    "OLD.searchable, OLD.sortable, OLD.filterable, OLD.required, OLD.read_only, OLD.hidden, " +
                    "CURRENT_TIMESTAMP, OLD.created_by, 'Field definition updated'" +
                    "); RETURN NEW; END; $$ LANGUAGE plpgsql");
                int updated = jdbc.update(
                        "UPDATE jira_migration.field_definitions SET hidden = false WHERE custom = true AND hidden = true");
                if (updated > 0) {
                    System.out.println("[STARTUP] Un-hid " + updated + " custom field(s) so they appear on issue views");
                }
            } catch (Exception e) {
                System.out.println("[STARTUP] Could not un-hide custom fields (non-fatal): " + e.getMessage());
            }
        };
    }
}