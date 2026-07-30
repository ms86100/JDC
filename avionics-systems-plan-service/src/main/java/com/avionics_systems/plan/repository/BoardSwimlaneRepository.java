package com.avionics_systems.plan.repository;

import com.avionics_systems.plan.entity.BoardSwimlane;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BoardSwimlaneRepository extends JpaRepository<BoardSwimlane, UUID> {

    List<BoardSwimlane> findByBoardConfigIdOrderBySequenceAsc(UUID boardId);

    List<BoardSwimlane> findByBoardConfigIdAndEnabledTrueOrderBySequenceAsc(UUID boardId);

    void deleteByBoardConfigId(UUID boardId);
}