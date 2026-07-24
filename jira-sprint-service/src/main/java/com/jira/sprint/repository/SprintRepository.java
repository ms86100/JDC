package com.jira.sprint.repository;

import com.jira.sprint.entity.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, UUID> {
    List<Sprint> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
    List<Sprint> findByProjectIdAndStatusOrderByCreatedAtDesc(UUID projectId, Sprint.SprintStatus status);
    List<Sprint> findByBoardIdOrderBySequenceAsc(UUID boardId);
    List<Sprint> findByBoardIdInOrderByCreatedAtDesc(Collection<UUID> boardIds);
}
