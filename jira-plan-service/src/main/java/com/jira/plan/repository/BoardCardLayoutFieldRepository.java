package com.jira.plan.repository;

import com.jira.plan.entity.BoardCardLayoutField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BoardCardLayoutFieldRepository extends JpaRepository<BoardCardLayoutField, UUID> {

    List<BoardCardLayoutField> findByBoardConfigIdOrderBySequenceAsc(UUID boardId);

    List<BoardCardLayoutField> findByBoardConfigIdAndIsVisibleTrueOrderBySequenceAsc(UUID boardId);

    void deleteByBoardConfigId(UUID boardId);
}