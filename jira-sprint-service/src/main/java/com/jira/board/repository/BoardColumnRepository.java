package com.jira.board.repository;

import com.jira.board.entity.BoardColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BoardColumnRepository extends JpaRepository<BoardColumn, UUID> {
    List<BoardColumn> findByBoardIdOrderBySequenceAsc(UUID boardId);
    List<BoardColumn> findByBoardId(UUID boardId);
}