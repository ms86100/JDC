package com.jira.admin.repository;

import com.jira.admin.entity.AircraftProgramEntity;
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
