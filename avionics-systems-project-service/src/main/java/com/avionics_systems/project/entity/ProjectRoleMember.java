package com.avionics_systems.project.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_role_members", schema = "jira_project")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectRoleMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_role_id", nullable = false)
    private ProjectRole projectRole;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "member_type", nullable = false, length = 20)
    private String memberType;

    @Column(name = "member_id")
    private UUID memberId;

    @Column(name = "group_name", length = 100)
    private String groupName;

    @Column(name = "added_by")
    private UUID addedBy;

    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;
}