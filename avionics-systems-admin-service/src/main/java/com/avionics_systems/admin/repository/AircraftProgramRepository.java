package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.AircraftProgramEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AircraftProgramRepository extends JpaRepository<AircraftProgramEntity, String> {

    List<AircraftProgramEntity> findByIsActiveTrueOrderByDisplayOrderAsc();

    List<AircraftProgramEntity> findByParentProgramIdAndIsActiveTrue(String parentProgramId);

    Optional<AircraftProgramEntity> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, String id);
}
