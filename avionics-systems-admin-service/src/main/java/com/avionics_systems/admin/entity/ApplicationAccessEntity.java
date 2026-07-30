package com.avionics_systems.admin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "application_access", schema = "jira_admin")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationAccessEntity {

    @EmbeddedId
    private ApplicationAccessId id;

    @Column(name = "granted_at")
    @Builder.Default
    private LocalDateTime grantedAt = LocalDateTime.now();

    @Column(name = "granted_by")
    private UUID grantedBy;
}
