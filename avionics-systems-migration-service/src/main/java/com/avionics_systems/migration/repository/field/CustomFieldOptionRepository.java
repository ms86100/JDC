package com.avionics_systems.migration.repository.field;

import com.avionics_systems.migration.entity.field.CustomFieldOption;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomFieldOptionRepository extends JpaRepository<CustomFieldOption, UUID> {

    List<CustomFieldOption> findByCustomFieldIdOrderBySequence(UUID customFieldId);

    List<CustomFieldOption> findByCustomFieldIdAndDisabled(UUID customFieldId, Boolean disabled);

    Optional<CustomFieldOption> findByCustomFieldIdAndValue(UUID customFieldId, String value);

    List<CustomFieldOption> findByParentOptionId(UUID parentOptionId);

    @Query("SELECT cfo FROM CustomFieldOption cfo WHERE cfo.customFieldId = :customFieldId AND cfo.disabled = false ORDER BY cfo.sequence")
    List<CustomFieldOption> findActiveByCustomFieldId(UUID customFieldId);

    @Query("SELECT cfo FROM CustomFieldOption cfo WHERE cfo.customFieldId = :customFieldId AND cfo.parentOptionId IS NULL AND cfo.disabled = false ORDER BY cfo.sequence")
    List<CustomFieldOption> findRootOptionsByCustomFieldId(UUID customFieldId);

    @Query("SELECT COUNT(cfo) FROM CustomFieldOption cfo WHERE cfo.customFieldId = :customFieldId")
    long countByCustomFieldId(UUID customFieldId);

    boolean existsByCustomFieldIdAndValue(UUID customFieldId, String value);
}