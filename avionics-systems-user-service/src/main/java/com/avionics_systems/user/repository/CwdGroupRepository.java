package com.avionics_systems.user.repository;

import com.avionics_systems.user.entity.CwdGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CwdGroupRepository extends JpaRepository<CwdGroup, UUID> {

    Optional<CwdGroup> findByLowerGroupNameAndDirectoryId(String lowerGroupName, UUID directoryId);

    Page<CwdGroup> findByDirectoryIdAndActive(UUID directoryId, boolean active, Pageable pageable);

    @Query("SELECT g FROM CwdGroup g WHERE g.directoryId = :dirId AND " +
           "LOWER(g.lowerGroupName) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<CwdGroup> searchGroups(@Param("dirId") UUID directoryId,
                                @Param("search") String search,
                                Pageable pageable);

    boolean existsByLowerGroupNameAndDirectoryId(String lowerGroupName, UUID directoryId);

    @Query("SELECT g FROM CwdGroup g JOIN CwdMembership m ON m.parentId = g.id " +
           "WHERE m.childId = :userId AND m.membershipType = 'GROUP_USER'")
    List<CwdGroup> findGroupsByUserId(@Param("userId") UUID userId);

    List<CwdGroup> findByDirectoryIdAndIsSystem(UUID directoryId, boolean isSystem);
}