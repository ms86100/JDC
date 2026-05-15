package com.jira.sprint.repository;

import com.jira.sprint.entity.BoardColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BoardColumnRepository extends JpaRepository<BoardColumn, UUID> {
    List<BoardColumn> findByBoardIdOrderBySequenceAsc(UUID boardId);
    void deleteByBoardId(UUID boardId);
}