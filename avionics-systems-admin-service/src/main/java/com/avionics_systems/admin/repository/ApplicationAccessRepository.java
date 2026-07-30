package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.ApplicationAccessEntity;
import com.avionics_systems.admin.entity.ApplicationAccessId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApplicationAccessRepository extends JpaRepository<ApplicationAccessEntity, ApplicationAccessId> {
    List<ApplicationAccessEntity> findByIdUserId(UUID userId);
    void deleteByIdUserId(UUID userId);
}
