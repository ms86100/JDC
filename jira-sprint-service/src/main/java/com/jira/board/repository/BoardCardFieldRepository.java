package com.jira.board.repository;

import com.jira.board.entity.BoardCardField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface BoardCardFieldRepository extends JpaRepository<BoardCardField, UUID> {
    List<BoardCardField> findByBoardIdOrderByPositionAsc(UUID boardId);
    long countByBoardId(UUID boardId);
    void deleteAllByBoardId(UUID boardId);
}
