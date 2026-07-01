package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "screen_scheme_screen")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScreenSchemeScreen {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "screen_scheme_id", nullable = false)
    private UUID screenSchemeId;

    @Column(name = "screen_id", nullable = false)
    private UUID screenId;

    @Enumerated(EnumType.STRING)
    @Column(name = "screen_type", nullable = false, length = 50)
    private Screen.ScreenType screenType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_scheme_id", insertable = false, updatable = false)
    private ScreenScheme screenScheme;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id", insertable = false, updatable = false)
    private Screen screen;
}