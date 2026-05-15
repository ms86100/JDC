package com.jira.plan.repository;

import com.jira.plan.entity.BoardDetailField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BoardDetailFieldRepository extends JpaRepository<BoardDetailField, UUID> {

    List<BoardDetailField> findByBoardConfigIdOrderBySequenceAsc(UUID boardId);

    List<BoardDetailField> findByBoardConfigIdAndIsVisibleTrueOrderBySequenceAsc(UUID boardId);

    void deleteByBoardConfigId(UUID boardId);
}