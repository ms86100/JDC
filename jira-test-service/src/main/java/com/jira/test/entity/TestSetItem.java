package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "test_set_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestSetItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "test_set_id", nullable = false)
    private UUID testSetId;

    @Column(name = "test_id", nullable = false)
    private UUID testId;

    @CreationTimestamp
    @Column(name = "added_at")
    private LocalDateTime addedAt;
}