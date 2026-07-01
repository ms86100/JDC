package com.jira.plan.repository;

import com.jira.plan.entity.BoardQuickFilterSharing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface BoardQuickFilterSharingRepository extends JpaRepository<BoardQuickFilterSharing, UUID> {
    List<BoardQuickFilterSharing> findByQuickFilterId(UUID quickFilterId);
    List<BoardQuickFilterSharing> findBySharedWithUserId(UUID userId);
    List<BoardQuickFilterSharing> findBySharedWithGroup(String groupName);
}