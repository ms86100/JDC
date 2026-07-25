package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "i18n_messages", schema = "jira_admin",
       uniqueConstraints = @UniqueConstraint(columnNames = {"message_key", "locale"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class I18nMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "message_key", nullable = false, length = 200)
    private String messageKey;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String locale = "en";

    @Column(name = "message_value", nullable = false, columnDefinition = "TEXT")
    private String messageValue;

    @Column(length = 50)
    private String category;
}
