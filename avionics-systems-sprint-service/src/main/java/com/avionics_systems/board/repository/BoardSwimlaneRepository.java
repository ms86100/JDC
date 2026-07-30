package com.avionics_systems.board.repository;

import com.avionics_systems.board.entity.BoardSwimlane;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface BoardSwimlaneRepository extends JpaRepository<BoardSwimlane, UUID> {
    List<BoardSwimlane> findByBoardIdOrderByPositionAsc(UUID boardId);
    void deleteAllByBoardId(UUID boardId);
}
