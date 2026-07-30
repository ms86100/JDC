package com.avionics_systems.notification.repository;

import com.avionics_systems.notification.entity.IncomingMailHandler;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IncomingMailHandlerRepository extends JpaRepository<IncomingMailHandler, UUID> {

    List<IncomingMailHandler> findByIsEnabled(Boolean isEnabled);

    @Query("SELECT h FROM IncomingMailHandler h WHERE h.isEnabled = true")
    List<IncomingMailHandler> findAllEnabled();

    List<IncomingMailHandler> findByProjectId(UUID projectId);
}
