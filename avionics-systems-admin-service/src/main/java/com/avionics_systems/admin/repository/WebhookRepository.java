package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.WebhookEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookRepository extends JpaRepository<WebhookEntity, String> {
    List<WebhookEntity> findByIsEnabledTrue();
}
