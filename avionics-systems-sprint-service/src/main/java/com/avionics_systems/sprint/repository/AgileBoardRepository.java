package com.avionics_systems.sprint.repository;

import com.avionics_systems.sprint.entity.AgileBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgileBoardRepository extends JpaRepository<AgileBoard, UUID> {
    List<AgileBoard> findByProjectId(UUID projectId);
    Optional<AgileBoard> findByProjectIdAndIsDefaultTrue(UUID projectId);
    Optional<AgileBoard> findByProjectIdAndBoardType(UUID projectId, String boardType);
}