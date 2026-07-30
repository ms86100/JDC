package com.avionics_systems.sprint.repository;

import com.avionics_systems.sprint.entity.BoardConfig;
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