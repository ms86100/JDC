package com.jira.admin.repository;

import com.jira.admin.entity.PriorityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PriorityRepository extends JpaRepository<PriorityEntity, String> {
    Optional<PriorityEntity> findByName(String name);
}