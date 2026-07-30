package com.avionics_systems.notification.repository;

import com.avionics_systems.notification.entity.NotificationEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationEventRepository extends JpaRepository<NotificationEventEntity, UUID> {

    Optional<NotificationEventEntity> findByEventType(String eventType);

    List<NotificationEventEntity> findByCategory(String category);

    Page<NotificationEventEntity> findByEnabled(Boolean enabled, Pageable pageable);

    @Query("SELECT ne FROM NotificationEventEntity ne WHERE ne.isSystemEvent = true")
    List<NotificationEventEntity> findSystemEvents();

    @Query("SELECT ne FROM NotificationEventEntity ne WHERE ne.enabled = true ORDER BY ne.category, ne.name")
    List<NotificationEventEntity> findAllActiveEvents();

    boolean existsByEventType(String eventType);

    @Modifying
    @Query("UPDATE NotificationEventEntity ne SET ne.enabled = :enabled WHERE ne.eventType = :eventType")
    int updateEnabledByEventType(@Param("eventType") String eventType, @Param("enabled") Boolean enabled);
}