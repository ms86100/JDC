package com.jira.board.repository;

import com.jira.board.entity.BoardConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BoardConfigRepository extends JpaRepository<BoardConfig, UUID> {
    Optional<BoardConfig> findByBoardIdAndUserIdIsNull(UUID boardId);
    Optional<BoardConfig> findByBoardIdAndUserId(UUID boardId, UUID userId);
}
