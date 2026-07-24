package com.jira.board.repository;

import com.jira.board.entity.BoardAdministrator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface BoardAdministratorRepository extends JpaRepository<BoardAdministrator, UUID> {
    List<BoardAdministrator> findByBoardId(UUID boardId);
    boolean existsByBoardIdAndHolderId(UUID boardId, UUID holderId);
    void deleteByBoardIdAndHolderId(UUID boardId, UUID holderId);
}
