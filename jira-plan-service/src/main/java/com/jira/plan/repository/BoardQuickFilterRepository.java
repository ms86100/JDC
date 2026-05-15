package com.jira.plan.repository;

import com.jira.plan.entity.BoardQuickFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BoardQuickFilterRepository extends JpaRepository<BoardQuickFilter, UUID> {

    List<BoardQuickFilter> findByBoardConfigIdOrderBySequenceAsc(UUID boardId);

    List<BoardQuickFilter> findByBoardConfigIdAndIsEnabledTrueOrderBySequenceAsc(UUID boardId);

    void deleteByBoardConfigId(UUID boardId);
}