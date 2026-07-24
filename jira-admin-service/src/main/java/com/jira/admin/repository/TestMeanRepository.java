package com.jira.admin.repository;

import com.jira.admin.entity.TestMeanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestMeanRepository extends JpaRepository<TestMeanEntity, String> {

    List<TestMeanEntity> findByProgramIdAndIsActiveTrueOrderByDisplayOrderAsc(String programId);

    boolean existsByProgramIdAndCode(String programId, String code);
}
