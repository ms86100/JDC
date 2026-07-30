package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.MasterBoardTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MasterBoardTypeRepository extends JpaRepository<MasterBoardTypeEntity, UUID> {

    Optional<MasterBoardTypeEntity> findByTypeKey(String typeKey);

    List<MasterBoardTypeEntity> findByIsActiveTrueOrderByDisplayNameAsc();

    boolean existsByTypeKey(String typeKey);
}
