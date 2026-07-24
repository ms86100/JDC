package com.jira.notification.controller;

import com.jira.notification.dto.EmailQueueResponse;
import com.jira.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/mail-queue")
@RequiredArgsConstructor
@Slf4j
public class EmailQueueController {

    private final EmailService emailService;

    @GetMapping
    public ResponseEntity<Page<EmailQueueResponse>> getQueueEntries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        log.info("GET /api/admin/mail-queue - Fetching queue entries, page={}, size={}, status={}", page, size, status);
        Page<EmailQueueResponse> response = emailService.getQueueEntries(page, size, status);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/flush")
    public ResponseEntity<Map<String, Object>> flushQueue() {
        log.info("POST /api/admin/mail-queue/flush - Force processing all queued emails");
        int processed = emailService.flushQueue();
        return ResponseEntity.ok(Map.of("processed", processed, "message", "Queue flush completed"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQueueEntry(@PathVariable UUID id) {
        log.info("DELETE /api/admin/mail-queue/{} - Deleting queue entry", id);
        emailService.deleteQueueEntry(id);
        return ResponseEntity.noContent().build();
    }
}
