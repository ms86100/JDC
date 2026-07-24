package com.jira.notification.service;

import com.jira.notification.dto.EmailQueueResponse;
import com.jira.notification.entity.EmailQueue;
import com.jira.notification.exception.ResourceNotFoundException;
import com.jira.notification.repository.EmailQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.web.client.RestTemplate;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final EmailQueueRepository emailQueueRepository;
    private final RestTemplate restTemplate;

    private final Object queueLock = new Object();

    @Value("${user.service.url:http://jira-user-service:8082}")
    private String userServiceUrl;

    @Value("${notification.email.from:noreply@jira.local}")
    private String fromAddress;

    @Async
    public void sendIssueAssignedEmail(UUID userId, Map<String, Object> issueData) {
        sendTemplatedEmail(userId, "issue-assigned", Map.of(
            "subject", "Issue assigned: " + issueData.getOrDefault("issueKey", "Unknown"),
            "issueKey", issueData.getOrDefault("issueKey", "Unknown").toString(),
            "title", issueData.getOrDefault("title", "No title").toString(),
            "reporter", issueData.getOrDefault("reporter", "Unknown").toString(),
            "projectKey", issueData.getOrDefault("projectKey", "Unknown").toString()
        ));
    }

    @Async
    public void sendIssueCommentedEmail(UUID userId, Map<String, Object> commentData) {
        sendTemplatedEmail(userId, "issue-commented", Map.of(
            "subject", "New comment on: " + commentData.getOrDefault("issueKey", "Unknown"),
            "issueKey", commentData.getOrDefault("issueKey", "Unknown").toString(),
            "commenter", commentData.getOrDefault("commenter", "Unknown").toString(),
            "commentPreview", truncate(commentData.getOrDefault("comment", "").toString(), 200),
            "projectKey", commentData.getOrDefault("projectKey", "Unknown").toString()
        ));
    }

    @Async
    public void sendSprintStartedEmail(UUID userId, Map<String, Object> sprintData) {
        sendTemplatedEmail(userId, "sprint-started", Map.of(
            "subject", "Sprint started: " + sprintData.getOrDefault("sprintName", "Unknown"),
            "sprintName", sprintData.getOrDefault("sprintName", "Unknown").toString(),
            "startDate", sprintData.getOrDefault("startDate", "Unknown").toString(),
            "endDate", sprintData.getOrDefault("endDate", "Unknown").toString(),
            "goal", sprintData.getOrDefault("goal", "").toString()
        ));
    }

    @Async
    public void sendSprintCompletedEmail(UUID userId, Map<String, Object> sprintData) {
        sendTemplatedEmail(userId, "sprint-completed", Map.of(
            "subject", "Sprint completed: " + sprintData.getOrDefault("sprintName", "Unknown"),
            "sprintName", sprintData.getOrDefault("sprintName", "Unknown").toString(),
            "completedIssues", sprintData.getOrDefault("completedIssues", 0).toString(),
            "totalPoints", sprintData.getOrDefault("totalPoints", 0).toString(),
            "velocity", sprintData.getOrDefault("velocity", 0).toString()
        ));
    }

    @Async
    public void sendBulkNotificationEmail(UUID userId, String subject, String message) {
        sendTemplatedEmail(userId, "bulk-notification", Map.of(
            "subject", subject,
            "message", message
        ));
    }

    @Async
    public void sendGenericNotificationEmail(UUID userId, String title, String message,
                                              Map<String, Object> context) {
        Map<String, Object> vars = new java.util.HashMap<>(context);
        vars.put("subject", title);
        vars.put("title", title);
        vars.put("message", message);
        sendTemplatedEmail(userId, "generic-notification", vars);
    }

    @Transactional
    public void queueEmail(String recipientEmail, String subject, String bodyHtml) {
        EmailQueue entry = EmailQueue.builder()
                .recipientEmail(recipientEmail)
                .subject(subject)
                .bodyHtml(bodyHtml)
                .status("QUEUED")
                .retryCount(0)
                .maxRetries(3)
                .nextRetryAt(OffsetDateTime.now())
                .build();

        emailQueueRepository.save(entry);
        log.debug("Queued email to {} with subject: {}", recipientEmail, subject);
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void processQueue() {
        synchronized (queueLock) {
            OffsetDateTime now = OffsetDateTime.now();

            List<EmailQueue> queued = emailQueueRepository.findReadyToSend(now);
            List<EmailQueue> retryable = emailQueueRepository.findRetryable(now);

            queued.addAll(retryable);

            if (queued.isEmpty()) {
                return;
            }

            log.info("Processing email queue: {} entries", queued.size());

            for (EmailQueue entry : queued) {
                sendQueuedEmail(entry);
            }
        }
    }

    @Transactional
    public int flushQueue() {
        synchronized (queueLock) {
            OffsetDateTime future = OffsetDateTime.now().plusYears(1);
            List<EmailQueue> queued = emailQueueRepository.findReadyToSend(future);
            List<EmailQueue> retryable = emailQueueRepository.findRetryable(future);
            queued.addAll(retryable);

            log.info("Flushing email queue: {} entries", queued.size());

            int processed = 0;
            for (EmailQueue entry : queued) {
                sendQueuedEmail(entry);
                processed++;
            }
            return processed;
        }
    }

    @Transactional(readOnly = true)
    public Page<EmailQueueResponse> getQueueEntries(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size);

        Page<EmailQueue> entries;
        if (status != null && !status.isBlank()) {
            entries = emailQueueRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            entries = emailQueueRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return entries.map(this::mapToQueueResponse);
    }

    @Transactional
    public void deleteQueueEntry(UUID entryId) {
        log.info("Deleting email queue entry: {}", entryId);

        if (!emailQueueRepository.existsById(entryId)) {
            throw new ResourceNotFoundException("Email queue entry not found: " + entryId);
        }

        emailQueueRepository.deleteById(entryId);
        log.info("Deleted email queue entry: {}", entryId);
    }

    private void sendTemplatedEmail(UUID userId, String templateName, Map<String, Object> variables) {
        try {
            String email = getUserEmail(userId);
            if (email == null || email.isBlank()) {
                log.warn("No email found for user {}, skipping email notification", userId);
                return;
            }

            Context context = new Context();
            variables.forEach(context::setVariable);
            String htmlContent = templateEngine.process("email/" + templateName, context);
            String subject = (String) variables.getOrDefault("subject", "Jira Notification");

            queueEmail(email, subject, htmlContent);

        } catch (Exception e) {
            log.error("Failed to queue email for user {}: {}", userId, e.getMessage());
        }
    }

    private void sendQueuedEmail(EmailQueue entry) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(entry.getRecipientEmail());
            helper.setSubject(entry.getSubject());
            helper.setText(entry.getBodyHtml(), true);

            mailSender.send(message);

            entry.setStatus("SENT");
            entry.setSentAt(OffsetDateTime.now());
            emailQueueRepository.save(entry);

            log.info("Email sent successfully to {}", entry.getRecipientEmail());

        } catch (MessagingException e) {
            handleSendFailure(entry, e.getMessage());
        } catch (Exception e) {
            handleSendFailure(entry, e.getMessage());
        }
    }

    private void handleSendFailure(EmailQueue entry, String errorMessage) {
        entry.setRetryCount(entry.getRetryCount() + 1);
        entry.setErrorMessage(errorMessage);

        if (entry.getRetryCount() >= entry.getMaxRetries()) {
            entry.setStatus("FAILED");
            log.error("Email to {} permanently failed after {} retries: {}",
                    entry.getRecipientEmail(), entry.getRetryCount(), errorMessage);
        } else {
            long backoffSeconds = (long) Math.pow(2, entry.getRetryCount()) * 30;
            entry.setNextRetryAt(OffsetDateTime.now().plusSeconds(backoffSeconds));
            log.warn("Email to {} failed (attempt {}/{}), next retry in {}s: {}",
                    entry.getRecipientEmail(), entry.getRetryCount(), entry.getMaxRetries(),
                    backoffSeconds, errorMessage);
        }

        emailQueueRepository.save(entry);
    }

    private String getUserEmail(UUID userId) {
        try {
            String url = userServiceUrl + "/api/users/" + userId;
            var response = restTemplate.getForObject(url, java.util.Map.class);
            if (response != null) {
                return (String) response.get("email");
            }
        } catch (Exception e) {
            log.warn("Failed to get user email for {}: {}", userId, e.getMessage());
        }
        return null;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    private EmailQueueResponse mapToQueueResponse(EmailQueue entry) {
        return EmailQueueResponse.builder()
                .id(entry.getId())
                .recipientEmail(entry.getRecipientEmail())
                .subject(entry.getSubject())
                .status(entry.getStatus())
                .errorMessage(entry.getErrorMessage())
                .retryCount(entry.getRetryCount())
                .maxRetries(entry.getMaxRetries())
                .createdAt(entry.getCreatedAt())
                .sentAt(entry.getSentAt())
                .nextRetryAt(entry.getNextRetryAt())
                .build();
    }
}
