package com.jira.plan.repository;

import com.jira.plan.entity.BoardColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BoardColumnRepository extends JpaRepository<BoardColumn, UUID> {

    List<BoardColumn> findByBoardConfigIdOrderBySequenceAsc(UUID boardId);

    void deleteByBoardConfigId(UUID boardId);

    int countByBoardConfigId(UUID boardId);
}