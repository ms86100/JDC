package com.jira.admin.repository;

import com.jira.admin.entity.MasterRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MasterRoleRepository extends JpaRepository<MasterRoleEntity, UUID> {

    Optional<MasterRoleEntity> findByRoleKey(String roleKey);

    List<MasterRoleEntity> findByIsActiveTrueOrderByDisplayNameAsc();

    boolean existsByRoleKey(String roleKey);
}
