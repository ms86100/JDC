package com.jira.admin.repository;

import com.jira.admin.entity.MasterBoardColumnTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MasterBoardColumnTemplateRepository extends JpaRepository<MasterBoardColumnTemplateEntity, UUID> {

    List<MasterBoardColumnTemplateEntity> findByBoardTypeIdOrderBySortOrderAsc(UUID boardTypeId);

    void deleteByBoardTypeId(UUID boardTypeId);
}
