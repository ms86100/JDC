package com.jira.project.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "screen_scheme_issue_type_screens", schema = "jira_project")
@IdClass(ScreenSchemeIssueTypeScreen.IdClass.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenSchemeIssueTypeScreen {

    @Id
    @Column(name = "scheme_id")
    private UUID schemeId;

    @Id
    @Column(name = "issue_type_id")
    private UUID issueTypeId;

    @Id
    @Column(name = "screen_type", length = 20)
    private String screenType;

    @Column(name = "screen_id", nullable = false)
    private UUID screenId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdClass implements Serializable {
        private UUID schemeId;
        private UUID issueTypeId;
        private String screenType;
    }
}
