package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.TestMeanDefectOriginEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestMeanDefectOriginRepository extends JpaRepository<TestMeanDefectOriginEntity, String> {

    List<TestMeanDefectOriginEntity> findByParentIdIsNullAndIsActiveTrueOrderByDisplayOrderAsc();

    List<TestMeanDefectOriginEntity> findByParentIdAndIsActiveTrueOrderByDisplayOrderAsc(String parentId);

    List<TestMeanDefectOriginEntity> findByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(String category);
}
