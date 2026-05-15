package com.jira.issue.repository;

import com.jira.issue.entity.ChangeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChangeItemRepository extends JpaRepository<ChangeItem, UUID> {
    List<ChangeItem> findByChangeGroupIdOrderByCreatedAtAsc(UUID changeGroupId);
    void deleteByChangeGroupId(UUID changeGroupId);
    void deleteByChangeGroupIdIn(List<UUID> changeGroupIds);
}