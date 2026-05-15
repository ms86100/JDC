package com.jira.admin.repository;

import com.jira.admin.entity.ScreenSchemeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ScreenSchemeRepository extends JpaRepository<ScreenSchemeEntity, String> {
    Optional<ScreenSchemeEntity> findByName(String name);
}