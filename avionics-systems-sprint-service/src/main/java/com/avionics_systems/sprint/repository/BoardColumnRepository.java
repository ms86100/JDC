package com.avionics_systems.sprint.repository;

import com.avionics_systems.sprint.entity.BoardColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BoardColumnRepository extends JpaRepository<BoardColumn, UUID> {
    List<BoardColumn> findByBoardIdOrderBySequenceAsc(UUID boardId);
    void deleteByBoardId(UUID boardId);
}