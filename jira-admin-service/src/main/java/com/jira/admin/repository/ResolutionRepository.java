package com.jira.admin.repository;

import com.jira.admin.entity.ResolutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ResolutionRepository extends JpaRepository<ResolutionEntity, String> {
    Optional<ResolutionEntity> findByName(String name);
}