package com.jira.admin.repository;

import com.jira.admin.entity.SystemFunctionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemFunctionRepository extends JpaRepository<SystemFunctionEntity, String> {

    List<SystemFunctionEntity> findBySystemIdAndIsActiveTrueOrderByDisplayOrderAsc(String systemId);
}
