package com.avionics_systems.portal.repository;

import com.avionics_systems.portal.entity.CustomerRequest;
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
public interface CustomerRequestRepository extends JpaRepository<CustomerRequest, UUID> {

    Optional<CustomerRequest> findByRequestKey(String requestKey);

    Page<CustomerRequest> findByPortalId(UUID portalId, Pageable pageable);

    Page<CustomerRequest> findByCustomerEmail(String customerEmail, Pageable pageable);

    Page<CustomerRequest> findByAssignedAgentId(UUID agentId, Pageable pageable);

    List<CustomerRequest> findByStatus(String status);

    @Query("SELECT cr FROM CustomerRequest cr WHERE cr.portalId = :portalId AND cr.status = :status")
    Page<CustomerRequest> findByPortalIdAndStatus(
            @Param("portalId") UUID portalId,
            @Param("status") String status,
            Pageable pageable);

    @Query("SELECT cr FROM CustomerRequest cr WHERE cr.portalId = :portalId ORDER BY cr.createdAt DESC")
    Page<CustomerRequest> findByPortalIdOrderByDate(
            @Param("portalId") UUID portalId,
            Pageable pageable);
}