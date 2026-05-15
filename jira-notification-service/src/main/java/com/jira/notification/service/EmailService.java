package com.jira.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.web.client.RestTemplate;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import java.util.UUID;

/**
 * Email notification service - sends real emails via SMTP
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final RestTemplate restTemplate = new RestTemplate();
    private static final String USER_SERVICE_URL = "http://jira-user-service:8082";

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

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(email);
            helper.setSubject((String) variables.getOrDefault("subject", "Jira Notification"));
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent successfully to {} for template {}", email, templateName);

        } catch (MessagingException e) {
            log.error("Failed to send email to user {}: {}", userId, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending email to user {}: {}", userId, e.getMessage());
        }
    }

    private String getUserEmail(UUID userId) {
        try {
            String url = USER_SERVICE_URL + "/api/users/" + userId;
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
}
