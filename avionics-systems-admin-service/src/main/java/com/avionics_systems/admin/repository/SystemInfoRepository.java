package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.SystemInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SystemInfoRepository extends JpaRepository<SystemInfoEntity, String> {
    Optional<SystemInfoEntity> findByKey(String key);
}