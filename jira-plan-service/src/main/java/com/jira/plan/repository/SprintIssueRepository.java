package com.jira.plan.repository;

import com.jira.plan.entity.SprintIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SprintIssueRepository extends JpaRepository<SprintIssue, UUID> {

    List<SprintIssue> findBySprintId(UUID sprintId);

    List<SprintIssue> findBySprintIdAndCompletionStatus(UUID sprintId, String completionStatus);

    @Query("SELECT si FROM SprintIssue si WHERE si.sprint.id = :sprintId AND si.completionStatus != 'DROPPED'")
    List<SprintIssue> findActiveBySprintId(@Param("sprintId") UUID sprintId);

    Optional<SprintIssue> findBySprintIdAndPlanItemId(UUID sprintId, UUID planItemId);

    Optional<SprintIssue> findBySprintIdAndIssueId(UUID sprintId, UUID issueId);

    boolean existsBySprintIdAndPlanItemId(UUID sprintId, UUID planItemId);

    @Query("SELECT COUNT(si) FROM SprintIssue si WHERE si.sprint.id = :sprintId AND si.completionStatus = 'COMPLETED'")
    int countCompletedBySprintId(@Param("sprintId") UUID sprintId);

    @Query("SELECT SUM(si.planItem.storyPoints) FROM SprintIssue si WHERE si.sprint.id = :sprintId AND si.completionStatus = 'COMPLETED'")
    Integer sumCompletedPoints(@Param("sprintId") UUID sprintId);

    @Query("SELECT SUM(si.planItem.storyPoints) FROM SprintIssue si WHERE si.sprint.id = :sprintId AND si.completionStatus != 'DROPPED'")
    Integer sumTotalPoints(@Param("sprintId") UUID sprintId);
}