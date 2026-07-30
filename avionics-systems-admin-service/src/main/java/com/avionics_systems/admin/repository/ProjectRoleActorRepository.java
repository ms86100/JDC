package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.ProjectRoleActorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRoleActorRepository extends JpaRepository<ProjectRoleActorEntity, String> {

    List<ProjectRoleActorEntity> findByProjectId(String projectId);

    List<ProjectRoleActorEntity> findByProjectIdAndProjectRoleId(String projectId, String projectRoleId);

    @Query("SELECT pra FROM ProjectRoleActorEntity pra WHERE pra.projectId = :projectId AND pra.holderId = :userId")
    List<ProjectRoleActorEntity> findByProjectIdAndUserId(@Param("projectId") String projectId, @Param("userId") String userId);

    @Query("SELECT pra FROM ProjectRoleActorEntity pra WHERE pra.projectId = :projectId AND pra.holderType = 'USER' AND pra.holderId = :userId")
    List<ProjectRoleActorEntity> findByProjectIdAndDirectUser(@Param("projectId") String projectId, @Param("userId") String userId);

    @Query("SELECT pra FROM ProjectRoleActorEntity pra WHERE pra.projectId = :projectId AND pra.holderType = 'GROUP' AND pra.holderId IN :groupIds")
    List<ProjectRoleActorEntity> findByProjectIdAndGroups(@Param("projectId") String projectId, @Param("groupIds") List<String> groupIds);

    Optional<ProjectRoleActorEntity> findByProjectIdAndHolderTypeAndHolderId(String projectId, String holderType, String holderId);
}