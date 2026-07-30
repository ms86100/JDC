package com.avionics_systems.plan.repository;

import com.avionics_systems.plan.entity.BoardCardColor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BoardCardColorRepository extends JpaRepository<BoardCardColor, UUID> {

    List<BoardCardColor> findByBoardConfigIdOrderBySequenceAsc(UUID boardId);

    List<BoardCardColor> findByBoardConfigIdAndEnabledTrueOrderBySequenceAsc(UUID boardId);

    void deleteByBoardConfigId(UUID boardId);
}