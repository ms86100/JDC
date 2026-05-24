package com.jira.portal.repository;

import com.jira.portal.entity.RequestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RequestTypeRepository extends JpaRepository<RequestType, UUID> {

    List<RequestType> findByPortalIdOrderByDisplayOrderAsc(UUID portalId);

    List<RequestType> findByPortalIdAndIsEnabledTrueOrderByDisplayOrderAsc(UUID portalId);

    RequestType findByPortalIdAndIsDefaultTrue(UUID portalId);
}