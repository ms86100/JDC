package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.NotificationEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationEventRepository extends JpaRepository<NotificationEventEntity, String> {

    Optional<NotificationEventEntity> findByEventKey(String eventKey);
}