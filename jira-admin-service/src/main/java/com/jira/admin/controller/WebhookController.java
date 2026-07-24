package com.jira.admin.controller;

import com.jira.admin.entity.WebhookDeliveryLogEntity;
import com.jira.admin.entity.WebhookEntity;
import com.jira.admin.service.WebhookDispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "System Webhook Management API")
@CrossOrigin(origins = "*")
public class WebhookController {

    private final WebhookDispatchService webhookDispatchService;

    @GetMapping
    @Operation(summary = "List all webhooks")
    public ResponseEntity<List<WebhookEntity>> getAllWebhooks() {
        return ResponseEntity.ok(webhookDispatchService.getAllWebhooks());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get webhook by ID")
    public ResponseEntity<WebhookEntity> getWebhookById(@PathVariable String id) {
        return webhookDispatchService.getWebhookById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a new webhook")
    public ResponseEntity<WebhookEntity> createWebhook(@RequestBody WebhookEntity webhook) {
        WebhookEntity created = webhookDispatchService.createWebhook(webhook);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing webhook")
    public ResponseEntity<WebhookEntity> updateWebhook(@PathVariable String id, @RequestBody WebhookEntity webhook) {
        return webhookDispatchService.updateWebhook(id, webhook)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a webhook")
    public ResponseEntity<Void> deleteWebhook(@PathVariable String id) {
        if (webhookDispatchService.deleteWebhook(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/deliveries")
    @Operation(summary = "Get delivery logs for a webhook")
    public ResponseEntity<List<WebhookDeliveryLogEntity>> getDeliveryLogs(@PathVariable String id) {
        if (webhookDispatchService.getWebhookById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(webhookDispatchService.getDeliveryLogs(id));
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "Test fire a webhook")
    public ResponseEntity<WebhookDeliveryLogEntity> testWebhook(@PathVariable String id) {
        try {
            WebhookDeliveryLogEntity result = webhookDispatchService.testWebhook(id);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
