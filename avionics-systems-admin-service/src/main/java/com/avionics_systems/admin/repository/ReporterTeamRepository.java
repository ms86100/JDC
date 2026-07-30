package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.ReporterTeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReporterTeamRepository extends JpaRepository<ReporterTeamEntity, String> {

    List<ReporterTeamEntity> findByIsActiveTrueOrderByDisplayOrderAsc();

    List<ReporterTeamEntity> findByProgramIdAndIsActiveTrueOrderByDisplayOrderAsc(String programId);

    List<ReporterTeamEntity> findByProgramIdIsNullAndIsActiveTrueOrderByDisplayOrderAsc();
}
