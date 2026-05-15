package com.jira.admin.repository;

import com.jira.admin.entity.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<GroupEntity, String> {
    Optional<GroupEntity> findByName(String name);
}