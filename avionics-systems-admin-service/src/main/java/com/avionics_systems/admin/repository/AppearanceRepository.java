package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.AppearanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppearanceRepository extends JpaRepository<AppearanceEntity, String> {
}