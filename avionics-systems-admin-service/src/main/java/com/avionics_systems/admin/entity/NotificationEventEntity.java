package com.avionics_systems.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_events")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "event_key", nullable = false, unique = true)
    private String eventKey;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;
}