package com.jira.board.repository;

import com.jira.board.entity.AgileBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AgileBoardRepository extends JpaRepository<AgileBoard, UUID> {
    List<AgileBoard> findByProjectId(UUID projectId);
    List<AgileBoard> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}