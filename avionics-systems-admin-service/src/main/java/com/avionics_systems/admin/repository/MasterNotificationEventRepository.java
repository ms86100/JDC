package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.MasterNotificationEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MasterNotificationEventRepository extends JpaRepository<MasterNotificationEventEntity, UUID> {

    Optional<MasterNotificationEventEntity> findByEventKey(String eventKey);

    List<MasterNotificationEventEntity> findByIsActiveTrueOrderByCategoryAscDisplayNameAsc();

    List<MasterNotificationEventEntity> findByCategoryOrderByDisplayNameAsc(String category);

    boolean existsByEventKey(String eventKey);
}
