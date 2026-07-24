package com.jira.admin.repository;

import com.jira.admin.entity.SystemSupplierEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemSupplierRepository extends JpaRepository<SystemSupplierEntity, String> {

    List<SystemSupplierEntity> findByProgramIdAndSystemIdAndIsActiveTrueOrderByDisplayOrderAsc(String programId, String systemId);

    List<SystemSupplierEntity> findByProgramIdAndIsActiveTrueOrderByDisplayOrderAsc(String programId);
}
