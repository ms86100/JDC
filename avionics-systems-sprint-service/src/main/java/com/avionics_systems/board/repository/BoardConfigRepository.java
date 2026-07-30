package com.avionics_systems.board.repository;

import com.avionics_systems.board.entity.BoardConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BoardConfigRepository extends JpaRepository<BoardConfig, UUID> {
    Optional<BoardConfig> findByBoardIdAndUserIdIsNull(UUID boardId);
    Optional<BoardConfig> findByBoardIdAndUserId(UUID boardId, UUID userId);
}
