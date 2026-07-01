package com.jira.user.repository;

import com.jira.user.entity.CwdUser;
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
public interface CwdUserRepository extends JpaRepository<CwdUser, UUID> {

    Optional<CwdUser> findByLowerUserNameAndDirectoryId(String lowerUserName, UUID directoryId);

    Optional<CwdUser> findByEmailAddressAndDirectoryId(String email, UUID directoryId);

    Page<CwdUser> findByDirectoryIdAndActive(UUID directoryId, boolean active, Pageable pageable);

    @Query("SELECT u FROM CwdUser u WHERE u.directoryId = :dirId AND " +
           "(LOWER(u.lowerUserName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.emailAddress) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.displayName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<CwdUser> searchUsers(@Param("dirId") UUID directoryId,
                              @Param("search") String search,
                              Pageable pageable);

    @Query("SELECT u FROM CwdUser u WHERE u.directoryId = :dirId AND u.active = :active AND " +
           "(LOWER(u.lowerUserName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.emailAddress) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.displayName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<CwdUser> searchUsersByStatus(@Param("dirId") UUID directoryId,
                                       @Param("search") String search,
                                       @Param("active") boolean active,
                                       Pageable pageable);

    boolean existsByLowerUserNameAndDirectoryId(String lowerUserName, UUID directoryId);

    boolean existsByEmailAddressAndDirectoryId(String email, UUID directoryId);

    @Query("SELECT u FROM CwdUser u JOIN CwdMembership m ON m.childId = u.id " +
           "WHERE m.parentId = :groupId AND m.membershipType = 'GROUP_USER'")
    List<CwdUser> findUsersByGroupId(@Param("groupId") UUID groupId);

    @Query("SELECT COUNT(m) FROM CwdMembership m WHERE m.parentId = :groupId AND m.membershipType = 'GROUP_USER'")
    int countUsersInGroup(@Param("groupId") UUID groupId);
}