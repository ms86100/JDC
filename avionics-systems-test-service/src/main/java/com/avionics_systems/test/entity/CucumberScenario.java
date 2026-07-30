package com.avionics_systems.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cucumber_scenario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CucumberScenario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "feature_key", nullable = false)
    private String featureKey;

    @Column(name = "feature_file", nullable = false, length = 500)
    private String featureFile;

    @Column(name = "feature_name", nullable = false, length = 500)
    private String featureName;

    @Column(name = "scenario_name", nullable = false, length = 500)
    private String scenarioName;

    @Column(name = "scenario_key", nullable = false, length = 500)
    private String scenarioKey;

    @Column(name = "scenario_type", length = 50)
    @Builder.Default
    private String scenarioType = "Scenario";

    @Column(columnDefinition = "TEXT")
    private String background;

    @Column(columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> tags = List.of();

    @Column(name = "line_number")
    private Integer lineNumber;

    @Column(name = "test_id")
    private UUID testId;

    @CreationTimestamp
    @Column(name = "imported_at")
    private LocalDateTime importedAt;
}