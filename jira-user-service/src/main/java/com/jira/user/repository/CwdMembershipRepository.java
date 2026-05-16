package com.jira.user.repository;

import com.jira.user.entity.CwdMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CwdMembershipRepository extends JpaRepository<CwdMembership, UUID> {

    List<CwdMembership> findByParentIdAndMembershipType(UUID parentId, String membershipType);

    List<CwdMembership> findByChildIdAndMembershipType(UUID childId, String membershipType);

    Optional<CwdMembership> findByParentIdAndChildIdAndMembershipType(UUID parentId, UUID childId, String membershipType);

    boolean existsByParentIdAndChildIdAndMembershipType(UUID parentId, UUID childId, String membershipType);

    void deleteByParentIdAndChildIdAndMembershipType(UUID parentId, UUID childId, String membershipType);

    @Query("SELECT COUNT(m) FROM CwdMembership m WHERE m.parentId = :parentId AND m.membershipType = :type")
    int countByParentIdAndType(@Param("parentId") UUID parentId, @Param("type") String type);
}