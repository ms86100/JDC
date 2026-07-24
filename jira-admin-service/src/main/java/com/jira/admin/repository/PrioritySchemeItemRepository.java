package com.jira.admin.repository;

import com.jira.admin.entity.PrioritySchemeItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrioritySchemeItemRepository extends JpaRepository<PrioritySchemeItemEntity, String> {
    List<PrioritySchemeItemEntity> findBySchemeIdOrderByPositionAsc(String schemeId);
    void deleteBySchemeId(String schemeId);
}
