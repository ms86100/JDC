package com.jira.notification.service;

import com.jira.notification.dto.IncomingMailHandlerRequest;
import com.jira.notification.dto.IncomingMailHandlerResponse;
import com.jira.notification.entity.IncomingMailHandler;
import com.jira.notification.exception.ResourceNotFoundException;
import com.jira.notification.repository.IncomingMailHandlerRepository;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncomingMailService {

    private final IncomingMailHandlerRepository handlerRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String ISSUE_SERVICE_URL = "http://jira-issue-service:8084";
    private static final String COMMENT_SERVICE_URL = "http://jira-comment-service:8086";
    private static final Pattern ISSUE_KEY_PATTERN = Pattern.compile("\\[([A-Z][A-Z0-9]+-\\d+)]");

    @Transactional
    public IncomingMailHandlerResponse createHandler(IncomingMailHandlerRequest request) {
        log.info("Creating incoming mail handler: {}", request.getName());

        IncomingMailHandler handler = IncomingMailHandler.builder()
                .name(request.getName())
                .serverType(request.getServerType() != null ? request.getServerType() : "IMAP")
                .host(request.getHost())
                .port(request.getPort() != null ? request.getPort() : 993)
                .useSsl(request.getUseSsl() != null ? request.getUseSsl() : true)
                .username(request.getUsername())
                .encryptedPassword(request.getPassword())
                .folder(request.getFolder() != null ? request.getFolder() : "INBOX")
                .handlerType(request.getHandlerType() != null ? request.getHandlerType() : "CREATE_ISSUE")
                .projectId(request.getProjectId())
                .issueTypeId(request.getIssueTypeId())
                .defaultReporterId(request.getDefaultReporterId())
                .isEnabled(request.getIsEnabled() != null ? request.getIsEnabled() : true)
                .pollIntervalMinutes(request.getPollIntervalMinutes() != null ? request.getPollIntervalMinutes() : 5)
                .build();

        handler = handlerRepository.save(handler);
        log.info("Created incoming mail handler with id: {}", handler.getId());

        return mapToResponse(handler);
    }

    @Transactional(readOnly = true)
    public IncomingMailHandlerResponse getHandler(UUID handlerId) {
        log.debug("Fetching incoming mail handler: {}", handlerId);

        IncomingMailHandler handler = handlerRepository.findById(handlerId)
                .orElseThrow(() -> new ResourceNotFoundException("Incoming mail handler not found: " + handlerId));

        return mapToResponse(handler);
    }

    @Transactional(readOnly = true)
    public List<IncomingMailHandlerResponse> getAllHandlers() {
        log.debug("Fetching all incoming mail handlers");
        return handlerRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public IncomingMailHandlerResponse updateHandler(UUID handlerId, IncomingMailHandlerRequest request) {
        log.info("Updating incoming mail handler: {}", handlerId);

        IncomingMailHandler handler = handlerRepository.findById(handlerId)
                .orElseThrow(() -> new ResourceNotFoundException("Incoming mail handler not found: " + handlerId));

        handler.setName(request.getName());
        handler.setServerType(request.getServerType() != null ? request.getServerType() : handler.getServerType());
        handler.setHost(request.getHost());
        handler.setPort(request.getPort() != null ? request.getPort() : handler.getPort());
        handler.setUseSsl(request.getUseSsl() != null ? request.getUseSsl() : handler.getUseSsl());
        handler.setUsername(request.getUsername());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            handler.setEncryptedPassword(request.getPassword());
        }

        handler.setFolder(request.getFolder() != null ? request.getFolder() : handler.getFolder());
        handler.setHandlerType(request.getHandlerType() != null ? request.getHandlerType() : handler.getHandlerType());
        handler.setProjectId(request.getProjectId());
        handler.setIssueTypeId(request.getIssueTypeId());
        handler.setDefaultReporterId(request.getDefaultReporterId());

        if (request.getIsEnabled() != null) {
            handler.setIsEnabled(request.getIsEnabled());
        }
        if (request.getPollIntervalMinutes() != null) {
            handler.setPollIntervalMinutes(request.getPollIntervalMinutes());
        }

        handler = handlerRepository.save(handler);
        log.info("Updated incoming mail handler: {}", handlerId);

        return mapToResponse(handler);
    }

    @Transactional
    public void deleteHandler(UUID handlerId) {
        log.info("Deleting incoming mail handler: {}", handlerId);

        if (!handlerRepository.existsById(handlerId)) {
            throw new ResourceNotFoundException("Incoming mail handler not found: " + handlerId);
        }

        handlerRepository.deleteById(handlerId);
        log.info("Deleted incoming mail handler: {}", handlerId);
    }

    public boolean testConnection(UUID handlerId) {
        log.info("Testing connection for mail handler: {}", handlerId);

        IncomingMailHandler handler = handlerRepository.findById(handlerId)
                .orElseThrow(() -> new ResourceNotFoundException("Incoming mail handler not found: " + handlerId));

        Store store = null;
        try {
            store = connectToMailServer(handler);
            Folder folder = store.getFolder(handler.getFolder());
            folder.open(Folder.READ_ONLY);
            int messageCount = folder.getMessageCount();
            folder.close(false);
            log.info("Connection test successful for handler {}. Messages in folder: {}", handlerId, messageCount);
            return true;
        } catch (Exception e) {
            log.error("Connection test failed for handler {}: {}", handlerId, e.getMessage());
            throw new IllegalArgumentException("Connection test failed: " + e.getMessage());
        } finally {
            closeStore(store);
        }
    }

    @Transactional
    public void processMailbox(IncomingMailHandler handler) {
        log.info("Processing mailbox for handler: {} ({})", handler.getName(), handler.getId());

        Store store = null;
        try {
            store = connectToMailServer(handler);
            Folder folder = store.getFolder(handler.getFolder());
            folder.open(Folder.READ_WRITE);

            Set<String> processedIds = getProcessedMessageIds(handler);
            Message[] messages = folder.getMessages();
            int processed = 0;

            for (Message message : messages) {
                try {
                    if (!message.isSet(Flags.Flag.SEEN)) {
                        String messageId = getMessageId(message);

                        if (messageId != null && processedIds.contains(messageId)) {
                            continue;
                        }

                        ParsedMessage parsed = parseMessage(message);

                        switch (handler.getHandlerType()) {
                            case "CREATE_ISSUE":
                                handleCreateIssue(handler, parsed);
                                break;
                            case "CREATE_OR_COMMENT":
                                handleCreateOrComment(handler, parsed);
                                break;
                            default:
                                log.warn("Unknown handler type: {}", handler.getHandlerType());
                        }

                        message.setFlag(Flags.Flag.SEEN, true);

                        if (messageId != null) {
                            processedIds.add(messageId);
                        }
                        processed++;
                    }
                } catch (Exception e) {
                    log.error("Failed to process message in handler {}: {}", handler.getId(), e.getMessage());
                }
            }

            folder.close(false);

            handler.setLastPollAt(OffsetDateTime.now());
            handler.setProcessedMessageIds(String.join(",", processedIds));
            handlerRepository.save(handler);

            log.info("Processed {} messages for handler: {}", processed, handler.getName());

        } catch (Exception e) {
            log.error("Failed to process mailbox for handler {}: {}", handler.getId(), e.getMessage());
        } finally {
            closeStore(store);
        }
    }

    private Store connectToMailServer(IncomingMailHandler handler) throws MessagingException {
        Properties props = new Properties();
        String protocol = handler.getServerType().equalsIgnoreCase("POP3") ? "pop3" : "imap";

        if (handler.getUseSsl()) {
            protocol += "s";
            props.put("mail." + protocol + ".ssl.enable", "true");
        }

        props.put("mail." + protocol + ".host", handler.getHost());
        props.put("mail." + protocol + ".port", String.valueOf(handler.getPort()));
        props.put("mail." + protocol + ".connectiontimeout", "10000");
        props.put("mail." + protocol + ".timeout", "10000");

        Session session = Session.getInstance(props);
        Store store = session.getStore(protocol);
        store.connect(handler.getHost(), handler.getPort(), handler.getUsername(), handler.getEncryptedPassword());
        return store;
    }

    private ParsedMessage parseMessage(Message message) throws MessagingException, java.io.IOException {
        String subject = message.getSubject() != null ? message.getSubject() : "(no subject)";
        String from = "";
        if (message.getFrom() != null && message.getFrom().length > 0) {
            Address addr = message.getFrom()[0];
            if (addr instanceof InternetAddress) {
                from = ((InternetAddress) addr).getAddress();
            } else {
                from = addr.toString();
            }
        }

        String body = extractTextBody(message);

        return new ParsedMessage(subject, body, from, getMessageId(message));
    }

    private String extractTextBody(Part part) throws MessagingException, java.io.IOException {
        if (part.isMimeType("text/plain")) {
            return (String) part.getContent();
        }

        if (part.isMimeType("text/html")) {
            return (String) part.getContent();
        }

        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                String result = extractTextBody(multipart.getBodyPart(i));
                if (result != null) {
                    return result;
                }
            }
        }

        return "";
    }

    private String getMessageId(Message message) {
        try {
            String[] headers = message.getHeader("Message-ID");
            if (headers != null && headers.length > 0) {
                return headers[0];
            }
        } catch (MessagingException e) {
            log.warn("Could not extract Message-ID: {}", e.getMessage());
        }
        return null;
    }

    private Set<String> getProcessedMessageIds(IncomingMailHandler handler) {
        Set<String> ids = new HashSet<>();
        if (handler.getProcessedMessageIds() != null && !handler.getProcessedMessageIds().isBlank()) {
            ids.addAll(Arrays.asList(handler.getProcessedMessageIds().split(",")));
        }
        return ids;
    }

    private void handleCreateIssue(IncomingMailHandler handler, ParsedMessage parsed) {
        try {
            Map<String, Object> issueRequest = new HashMap<>();
            issueRequest.put("title", parsed.subject);
            issueRequest.put("description", parsed.body);

            if (handler.getProjectId() != null) {
                issueRequest.put("projectId", handler.getProjectId().toString());
            }
            if (handler.getIssueTypeId() != null) {
                issueRequest.put("issueTypeId", handler.getIssueTypeId().toString());
            }
            if (handler.getDefaultReporterId() != null) {
                issueRequest.put("reporterId", handler.getDefaultReporterId().toString());
            }

            restTemplate.postForObject(ISSUE_SERVICE_URL + "/api/issues", issueRequest, Map.class);
            log.info("Created issue from email: {}", parsed.subject);
        } catch (Exception e) {
            log.error("Failed to create issue from email: {}", e.getMessage());
        }
    }

    private void handleCreateOrComment(IncomingMailHandler handler, ParsedMessage parsed) {
        Matcher matcher = ISSUE_KEY_PATTERN.matcher(parsed.subject);

        if (matcher.find()) {
            String issueKey = matcher.group(1);
            try {
                Map<String, Object> commentRequest = new HashMap<>();
                commentRequest.put("issueKey", issueKey);
                commentRequest.put("body", parsed.body);

                if (handler.getDefaultReporterId() != null) {
                    commentRequest.put("authorId", handler.getDefaultReporterId().toString());
                }

                restTemplate.postForObject(COMMENT_SERVICE_URL + "/api/comments", commentRequest, Map.class);
                log.info("Added comment to issue {} from email", issueKey);
            } catch (Exception e) {
                log.error("Failed to add comment to issue {}: {}", issueKey, e.getMessage());
                handleCreateIssue(handler, parsed);
            }
        } else {
            handleCreateIssue(handler, parsed);
        }
    }

    private void closeStore(Store store) {
        if (store != null && store.isConnected()) {
            try {
                store.close();
            } catch (MessagingException e) {
                log.warn("Error closing mail store: {}", e.getMessage());
            }
        }
    }

    private IncomingMailHandlerResponse mapToResponse(IncomingMailHandler handler) {
        return IncomingMailHandlerResponse.builder()
                .id(handler.getId())
                .name(handler.getName())
                .serverType(handler.getServerType())
                .host(handler.getHost())
                .port(handler.getPort())
                .useSsl(handler.getUseSsl())
                .username(handler.getUsername())
                .folder(handler.getFolder())
                .handlerType(handler.getHandlerType())
                .projectId(handler.getProjectId())
                .issueTypeId(handler.getIssueTypeId())
                .defaultReporterId(handler.getDefaultReporterId())
                .isEnabled(handler.getIsEnabled())
                .pollIntervalMinutes(handler.getPollIntervalMinutes())
                .lastPollAt(handler.getLastPollAt())
                .createdAt(handler.getCreatedAt())
                .updatedAt(handler.getUpdatedAt())
                .build();
    }

    private record ParsedMessage(String subject, String body, String fromAddress, String messageId) {}
}
