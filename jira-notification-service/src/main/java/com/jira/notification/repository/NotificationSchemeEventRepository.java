package com.jira.notification.repository;

import com.jira.notification.entity.NotificationSchemeEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationSchemeEventRepository extends JpaRepository<NotificationSchemeEvent, UUID> {

    List<NotificationSchemeEvent> findBySchemeId(UUID schemeId);

    List<NotificationSchemeEvent> findBySchemeIdAndEnabled(UUID schemeId, Boolean enabled);

    @Query("SELECT nse FROM NotificationSchemeEvent nse WHERE nse.schemeId = :schemeId AND nse.eventType = :eventType AND nse.enabled = true")
    List<NotificationSchemeEvent> findActiveEventsForSchemeAndType(
            @Param("schemeId") UUID schemeId,
            @Param("eventType") String eventType);

    @Modifying
    @Query("DELETE FROM NotificationSchemeEvent nse WHERE nse.schemeId = :schemeId")
    void deleteAllBySchemeId(@Param("schemeId") UUID schemeId);

    @Query("SELECT COUNT(nse) FROM NotificationSchemeEvent nse WHERE nse.schemeId = :schemeId")
    long countBySchemeId(@Param("schemeId") UUID schemeId);
}