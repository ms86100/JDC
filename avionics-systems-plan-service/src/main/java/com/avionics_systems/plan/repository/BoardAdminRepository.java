package com.avionics_systems.plan.repository;

import com.avionics_systems.plan.entity.BoardAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BoardAdminRepository extends JpaRepository<BoardAdmin, UUID> {
    List<BoardAdmin> findByBoardId(UUID boardId);
    List<BoardAdmin> findByUserId(UUID userId);
    Optional<BoardAdmin> findByBoardIdAndUserId(UUID boardId, UUID userId);
    boolean existsByBoardIdAndUserId(UUID boardId, UUID userId);
}