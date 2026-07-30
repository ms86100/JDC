package com.avionics_systems.project.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "screen_scheme_screens", schema = "jira_project")
@IdClass(ScreenSchemeScreen.IdClass.class)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenSchemeScreen {

    @Id
    @Column(name = "scheme_id")
    private UUID schemeId;

    @Id
    @Column(name = "screen_type", length = 20)
    private String screenType;

    @Id
    @Column(name = "screen_id")
    private UUID screenId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdClass implements Serializable {
        private UUID schemeId;
        private String screenType;
        private UUID screenId;
    }
}