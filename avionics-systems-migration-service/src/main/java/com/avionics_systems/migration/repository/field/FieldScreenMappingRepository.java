package com.avionics_systems.migration.repository.field;

import com.avionics_systems.migration.entity.field.FieldScreenMapping;
import com.avionics_systems.migration.entity.field.FieldScreenMapping.FieldScreenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FieldScreenMappingRepository extends JpaRepository<FieldScreenMapping, UUID> {

    List<FieldScreenMapping> findByProjectIdAndScreenType(UUID projectId, FieldScreenType screenType);

    List<FieldScreenMapping> findByProjectIdIsNullAndScreenType(FieldScreenType screenType);

    boolean existsByProjectIdAndScreenType(UUID projectId, FieldScreenType screenType);

    boolean existsByProjectIdIsNullAndScreenType(FieldScreenType screenType);

    Optional<FieldScreenMapping> findByProjectIdAndScreenTypeAndFieldKey(
            UUID projectId, FieldScreenType screenType, String fieldKey);

    Optional<FieldScreenMapping> findByProjectIdIsNullAndScreenTypeAndFieldKey(
            FieldScreenType screenType, String fieldKey);
}
