package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<GroupEntity, String> {
    Optional<GroupEntity> findByGroupName(String groupName);
}