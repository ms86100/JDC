package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.NotificationSchemeEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationSchemeEventRepository extends JpaRepository<NotificationSchemeEventEntity, String> {

    List<NotificationSchemeEventEntity> findByNotificationSchemeId(String notificationSchemeId);

    List<NotificationSchemeEventEntity> findByEventId(String eventId);

    @Query("SELECT nse FROM NotificationSchemeEventEntity nse WHERE nse.notificationSchemeId = :schemeId AND nse.eventId = :eventId")
    List<NotificationSchemeEventEntity> findBySchemeAndEvent(@Param("schemeId") String schemeId, @Param("eventId") String eventId);
}