package com.jira.migration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Migration import settings (Jira DC parity: attachment limits, FILE: import dir).
 */
@RestController
@RequestMapping("/api/migration/settings")
@RequiredArgsConstructor
@Tag(name = "Migration Settings", description = "Import attachment and CSV settings")
public class MigrationImportSettingsController {

    @Value("${migration.attachment.max-size-bytes:10485760}")
    private long maxAttachmentSizeBytes;

    @Value("${migration.import.attachments-dir:}")
    private String attachmentsImportDir;

    @GetMapping
    @Operation(summary = "Get migration import settings")
    public ResponseEntity<Map<String, Object>> getSettings() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("maxAttachmentSizeBytes", maxAttachmentSizeBytes);
        body.put("maxAttachmentSizeMb", maxAttachmentSizeBytes / (1024 * 1024));
        body.put("maxAttachmentSizeCapBytes", 2_147_483_647L);
        body.put("attachmentsImportDir", attachmentsImportDir != null ? attachmentsImportDir : "");
        body.put("storageNote", "Attachments are stored via attachment-service (port 8090), analogous to JIRA_HOME/data/attachments.");
        body.put("csvImportProfiles", Map.of(
                "LIGHTWEIGHT", "Issues CSV — no attachment column import",
                "EXTERNAL", "External System Import — URLs and FILE: references"
        ));
        body.put("legacyFieldsApiDeprecated", true);
        body.put("legacyFieldsApiReplacement", "/api/migration/mappings and /api/fields/custom");
        body.put("provisionApiNote", "Use wizard POST .../fields/provision-missing; generic POST /api/fields/provision requires ADMIN when method security is enabled.");
        return ResponseEntity.ok(body);
    }

    @GetMapping("/attachments")
    public ResponseEntity<Map<String, Object>> getAttachmentSettings() {
        return getSettings();
    }
}
