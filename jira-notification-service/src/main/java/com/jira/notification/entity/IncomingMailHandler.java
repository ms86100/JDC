package com.jira.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "incoming_mail_handlers", schema = "jira_notification")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomingMailHandler {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "server_type", nullable = false, length = 10)
    @Builder.Default
    private String serverType = "IMAP";

    @Column(nullable = false, length = 500)
    private String host;

    @Column(nullable = false)
    @Builder.Default
    private Integer port = 993;

    @Column(name = "use_ssl")
    @Builder.Default
    private Boolean useSsl = true;

    @Column(nullable = false, length = 200)
    private String username;

    @Column(name = "encrypted_password", nullable = false, columnDefinition = "TEXT")
    private String encryptedPassword;

    @Column(length = 100)
    @Builder.Default
    private String folder = "INBOX";

    @Column(name = "handler_type", nullable = false, length = 30)
    @Builder.Default
    private String handlerType = "CREATE_ISSUE";

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "issue_type_id")
    private UUID issueTypeId;

    @Column(name = "default_reporter_id")
    private UUID defaultReporterId;

    @Column(name = "is_enabled")
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "poll_interval_minutes")
    @Builder.Default
    private Integer pollIntervalMinutes = 5;

    @Column(name = "last_poll_at")
    private OffsetDateTime lastPollAt;

    @Column(name = "processed_message_ids", columnDefinition = "TEXT")
    private String processedMessageIds;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
