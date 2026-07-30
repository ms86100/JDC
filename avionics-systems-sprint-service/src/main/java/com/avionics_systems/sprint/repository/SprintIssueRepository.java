package com.avionics_systems.sprint.repository;

import com.avionics_systems.sprint.entity.SprintIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SprintIssueRepository extends JpaRepository<SprintIssue, UUID> {
    List<SprintIssue> findBySprintIdOrderByOrderIndex(UUID sprintId);
    List<SprintIssue> findBySprintId(UUID sprintId);
    Optional<SprintIssue> findBySprintIdAndIssueId(UUID sprintId, UUID issueId);
    void deleteBySprintIdAndIssueId(UUID sprintId, UUID issueId);
    int countBySprintId(UUID sprintId);

    List<SprintIssue> findBySprintIdAndRemovedAtIsNull(UUID sprintId);
    List<SprintIssue> findBySprintIdAndRemovedAtIsNotNull(UUID sprintId);
    List<SprintIssue> findBySprintIdAndAddedAtAfter(UUID sprintId, java.time.LocalDateTime after);
    List<SprintIssue> findByIssueIdAndRemovedAtIsNull(UUID issueId);
}
