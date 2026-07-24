package com.jira.admin.repository;

import com.jira.admin.entity.AircraftSystemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AircraftSystemRepository extends JpaRepository<AircraftSystemEntity, String> {

    List<AircraftSystemEntity> findByProgramIdAndIsActiveTrueOrderByDisplayOrderAsc(String programId);

    boolean existsByProgramIdAndCode(String programId, String code);
}
