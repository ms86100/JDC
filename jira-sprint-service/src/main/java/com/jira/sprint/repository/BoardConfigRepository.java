package com.jira.sprint.repository;

import com.jira.sprint.entity.BoardConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BoardConfigRepository extends JpaRepository<BoardConfig, UUID> {
    List<BoardConfig> findByBoardId(UUID boardId);
    Optional<BoardConfig> findByBoardIdAndUserId(UUID boardId, UUID userId);
    void deleteByBoardId(UUID boardId);
}