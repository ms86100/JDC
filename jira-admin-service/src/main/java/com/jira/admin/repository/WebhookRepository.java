package com.jira.admin.repository;

import com.jira.admin.entity.WebhookEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookRepository extends JpaRepository<WebhookEntity, String> {
    List<WebhookEntity> findByIsEnabledTrue();
}
