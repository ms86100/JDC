package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.CascadingOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CascadingOptionRepository extends JpaRepository<CascadingOption, UUID> {

    List<CascadingOption> findByFieldIdOrderByDisplayOrderAsc(UUID fieldId);

    List<CascadingOption> findByFieldIdAndParentValueOrderByDisplayOrderAsc(UUID fieldId, String parentValue);

    @Query("SELECT DISTINCT c.parentValue FROM CascadingOption c WHERE c.fieldId = :fieldId ORDER BY c.displayOrder")
    List<String> findDistinctParentValuesByFieldId(@Param("fieldId") UUID fieldId);

    void deleteByFieldId(UUID fieldId);

    boolean existsByFieldIdAndParentValueAndChildValue(UUID fieldId, String parentValue, String childValue);
}