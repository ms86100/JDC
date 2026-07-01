package com.jira.admin.repository;

import com.jira.admin.entity.ScreenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ScreenRepository extends JpaRepository<ScreenEntity, String> {
    Optional<ScreenEntity> findByName(String name);
}