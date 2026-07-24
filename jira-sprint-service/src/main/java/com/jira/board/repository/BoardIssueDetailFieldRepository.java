package com.jira.board.repository;

import com.jira.board.entity.BoardIssueDetailField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface BoardIssueDetailFieldRepository extends JpaRepository<BoardIssueDetailField, UUID> {
    List<BoardIssueDetailField> findByBoardIdOrderByPositionAsc(UUID boardId);
    void deleteAllByBoardId(UUID boardId);
}
