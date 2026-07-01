package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "screen")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Screen {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "screen_type", nullable = false, length = 50)
    private ScreenType screenType;

    @Column
    @Builder.Default
    private Integer position = 0;

    public enum ScreenType {
        CREATE, EDIT, VIEW, SEARCH
    }
}