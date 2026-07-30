package com.avionics_systems.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vvo_test_request_link")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VvoTestRequestLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "vvo_id", nullable = false)
    private UUID vvoId;

    @Column(name = "test_request_id", nullable = false)
    private UUID testRequestId;

    @Column(name = "link_type", length = 30)
    @Builder.Default
    private String linkType = "CONTAIN";

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
