package com.jira.board.repository;

import com.jira.board.entity.BoardSprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BoardSprintRepository extends JpaRepository<BoardSprint, UUID> {
    List<BoardSprint> findByBoardIdOrderBySequenceAsc(UUID boardId);
    Optional<BoardSprint> findByBoardIdAndSprintId(UUID boardId, UUID sprintId);
}