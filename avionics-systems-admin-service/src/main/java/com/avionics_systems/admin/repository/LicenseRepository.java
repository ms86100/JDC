package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.LicenseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LicenseRepository extends JpaRepository<LicenseEntity, String> {
}