package com.jira.notification.service;

import com.jira.notification.dto.CreateEmailTemplateRequest;
import com.jira.notification.dto.EmailTemplateResponse;
import com.jira.notification.entity.EmailTemplate;
import com.jira.notification.exception.ResourceNotFoundException;
import com.jira.notification.repository.EmailTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateService {

    private final EmailTemplateRepository templateRepository;

    @Transactional
    public EmailTemplateResponse createTemplate(CreateEmailTemplateRequest request) {
        log.info("Creating email template: {}", request.getTemplateKey());

        if (templateRepository.existsByTemplateKey(request.getTemplateKey())) {
            throw new IllegalArgumentException("Email template with key '" + request.getTemplateKey() + "' already exists");
        }

        if (request.getIsDefault() != null && request.getIsDefault() &&
                templateRepository.existsDefaultForEventType(request.getEventType())) {
            throw new IllegalArgumentException("Default template already exists for event type: " + request.getEventType());
        }

        EmailTemplate template = EmailTemplate.builder()
                .templateKey(request.getTemplateKey())
                .name(request.getName())
                .description(request.getDescription())
                .subjectTemplate(request.getSubjectTemplate())
                .bodyTemplate(request.getBodyTemplate())
                .eventType(request.getEventType())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .templateType(request.getTemplateType() != null ? request.getTemplateType() : "THYMELEAF")
                .createdBy(request.getCreatedBy())
                .build();

        template = templateRepository.save(template);
        log.info("Created email template with id: {}", template.getId());

        return mapToResponse(template);
    }

    @Transactional(readOnly = true)
    public EmailTemplateResponse getTemplate(UUID templateId) {
        log.debug("Fetching email template: {}", templateId);

        EmailTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Email template not found: " + templateId));

        return mapToResponse(template);
    }

    @Transactional(readOnly = true)
    public EmailTemplateResponse getTemplateByKey(String templateKey) {
        log.debug("Fetching email template by key: {}", templateKey);

        EmailTemplate template = templateRepository.findByTemplateKey(templateKey)
                .orElseThrow(() -> new ResourceNotFoundException("Email template not found: " + templateKey));

        return mapToResponse(template);
    }

    @Transactional(readOnly = true)
    public Page<EmailTemplateResponse> getAllTemplates(int page, int size) {
        log.debug("Fetching all email templates");
        Pageable pageable = PageRequest.of(page, size);
        return templateRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<EmailTemplateResponse> getTemplatesByEventType(String eventType) {
        log.debug("Fetching email templates for event type: {}", eventType);
        return templateRepository.findByEventType(eventType).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EmailTemplateResponse getDefaultTemplateForEventType(String eventType) {
        log.debug("Fetching default template for event type: {}", eventType);

        EmailTemplate template = templateRepository.findDefaultByEventType(eventType)
                .orElseThrow(() -> new ResourceNotFoundException("No default template found for event type: " + eventType));

        return mapToResponse(template);
    }

    @Transactional(readOnly = true)
    public List<EmailTemplateResponse> getActiveTemplates() {
        log.debug("Fetching all active email templates");
        return templateRepository.findAllActive().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public EmailTemplateResponse updateTemplate(UUID templateId, CreateEmailTemplateRequest request) {
        log.info("Updating email template: {}", templateId);

        EmailTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Email template not found: " + templateId));

        if (!template.getTemplateKey().equals(request.getTemplateKey()) &&
                templateRepository.existsByTemplateKey(request.getTemplateKey())) {
            throw new IllegalArgumentException("Email template with key '" + request.getTemplateKey() + "' already exists");
        }

        if (request.getIsDefault() != null && request.getIsDefault() &&
                !template.getIsDefault() && templateRepository.existsDefaultForEventType(request.getEventType())) {
            throw new IllegalArgumentException("Default template already exists for event type: " + request.getEventType());
        }

        template.setTemplateKey(request.getTemplateKey());
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setSubjectTemplate(request.getSubjectTemplate());
        template.setBodyTemplate(request.getBodyTemplate());
        template.setEventType(request.getEventType());
        if (request.getIsDefault() != null) {
            template.setIsDefault(request.getIsDefault());
        }
        if (request.getEnabled() != null) {
            template.setEnabled(request.getEnabled());
        }
        if (request.getTemplateType() != null) {
            template.setTemplateType(request.getTemplateType());
        }

        template = templateRepository.save(template);
        log.info("Updated email template: {}", templateId);

        return mapToResponse(template);
    }

    @Transactional
    public void deleteTemplate(UUID templateId) {
        log.info("Deleting email template: {}", templateId);

        if (!templateRepository.existsById(templateId)) {
            throw new ResourceNotFoundException("Email template not found: " + templateId);
        }

        templateRepository.deleteById(templateId);
        log.info("Deleted email template: {}", templateId);
    }

    @Transactional
    public EmailTemplateResponse toggleTemplate(UUID templateId, boolean enabled) {
        log.info("Toggling email template {} to enabled={}", templateId, enabled);

        EmailTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Email template not found: " + templateId));

        template.setEnabled(enabled);
        template = templateRepository.save(template);

        return mapToResponse(template);
    }

    @Transactional
    public void initializeDefaultTemplates() {
        log.info("Initializing default email templates");

        List<EmailTemplate> defaultTemplates = Arrays.asList(
                EmailTemplate.builder()
                        .templateKey("issue-assigned")
                        .name("Issue Assigned Email")
                        .description("Email template for issue assignment notifications")
                        .subjectTemplate("You have been assigned to issue: [[${issueKey}]]")
                        .bodyTemplate("<html><body><h1>Issue Assigned</h1><p>You have been assigned to issue [[${issueKey}]] - [[${title}]] in project [[${projectKey}]].</p><p>Reporter: [[${reporter}]]</p></body></html>")
                        .eventType("ISSUE_ASSIGNED")
                        .isDefault(true)
                        .build(),
                EmailTemplate.builder()
                        .templateKey("issue-commented")
                        .name("Issue Commented Email")
                        .description("Email template for issue comment notifications")
                        .subjectTemplate("New comment on issue: [[${issueKey}]]")
                        .bodyTemplate("<html><body><h1>New Comment</h1><p>[[${commenter}]] commented on issue [[${issueKey}]].</p><blockquote>[[${commentPreview}]]</blockquote></body></html>")
                        .eventType("ISSUE_COMMENTED")
                        .isDefault(true)
                        .build(),
                EmailTemplate.builder()
                        .templateKey("sprint-started")
                        .name("Sprint Started Email")
                        .description("Email template for sprint start notifications")
                        .subjectTemplate("Sprint Started: [[${sprintName}]]")
                        .bodyTemplate("<html><body><h1>Sprint Started</h1><p>Sprint [[${sprintName}]] has begun!</p><p>Duration: [[${startDate}]] to [[${endDate}]]</p><p>Goal: [[${goal}]]</p></body></html>")
                        .eventType("SPRINT_STARTED")
                        .isDefault(true)
                        .build(),
                EmailTemplate.builder()
                        .templateKey("sprint-completed")
                        .name("Sprint Completed Email")
                        .description("Email template for sprint completion notifications")
                        .subjectTemplate("Sprint Completed: [[${sprintName}]]")
                        .bodyTemplate("<html><body><h1>Sprint Completed</h1><p>Sprint [[${sprintName}]] has been completed!</p><p>Completed Issues: [[${completedIssues}]]</p><p>Total Points: [[${totalPoints}]]</p><p>Velocity: [[${velocity}]]</p></body></html>")
                        .eventType("SPRINT_COMPLETED")
                        .isDefault(true)
                        .build()
        );

        for (EmailTemplate template : defaultTemplates) {
            if (!templateRepository.existsByTemplateKey(template.getTemplateKey())) {
                templateRepository.save(template);
                log.info("Initialized default template: {}", template.getTemplateKey());
            }
        }
    }

    private EmailTemplateResponse mapToResponse(EmailTemplate template) {
        return EmailTemplateResponse.builder()
                .id(template.getId())
                .templateKey(template.getTemplateKey())
                .name(template.getName())
                .description(template.getDescription())
                .subjectTemplate(template.getSubjectTemplate())
                .bodyTemplate(template.getBodyTemplate())
                .eventType(template.getEventType())
                .isDefault(template.getIsDefault())
                .enabled(template.getEnabled())
                .templateType(template.getTemplateType())
                .createdBy(template.getCreatedBy())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}