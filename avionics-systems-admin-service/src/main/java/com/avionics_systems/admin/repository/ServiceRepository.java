package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<ServiceEntity, String> {
    Optional<ServiceEntity> findByServiceId(String serviceId);
    Optional<ServiceEntity> findByServiceName(String serviceName);
}