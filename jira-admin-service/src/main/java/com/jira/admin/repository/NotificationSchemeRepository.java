package com.jira.admin.repository;

import com.jira.admin.entity.NotificationSchemeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface NotificationSchemeRepository extends JpaRepository<NotificationSchemeEntity, String> {
    Optional<NotificationSchemeEntity> findByName(String name);
}