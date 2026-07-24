package com.jira.admin.repository;

import com.jira.admin.entity.WebhookDeliveryLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookDeliveryLogRepository extends JpaRepository<WebhookDeliveryLogEntity, String> {
    List<WebhookDeliveryLogEntity> findByWebhookIdOrderByDeliveredAtDesc(String webhookId);
}
