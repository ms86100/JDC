package com.avionics_systems.user.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "organization_members", schema = "jira_user")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(OrganizationMemberId.class)
public class OrganizationMember {

    @Id
    @Column(name = "org_id")
    private UUID orgId;

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String role = "MEMBER";

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private OffsetDateTime joinedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", insertable = false, updatable = false)
    private Organization organization;
}