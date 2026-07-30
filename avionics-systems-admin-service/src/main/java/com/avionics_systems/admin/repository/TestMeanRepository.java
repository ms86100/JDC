package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.TestMeanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestMeanRepository extends JpaRepository<TestMeanEntity, String> {

    List<TestMeanEntity> findByProgramIdAndIsActiveTrueOrderByDisplayOrderAsc(String programId);

    boolean existsByProgramIdAndCode(String programId, String code);
}
