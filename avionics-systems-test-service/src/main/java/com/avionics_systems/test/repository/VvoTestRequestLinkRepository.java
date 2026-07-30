package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.VvoTestRequestLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VvoTestRequestLinkRepository extends JpaRepository<VvoTestRequestLink, UUID> {

    List<VvoTestRequestLink> findByTestRequestId(UUID testRequestId);

    List<VvoTestRequestLink> findByVvoId(UUID vvoId);

    boolean existsByVvoIdAndTestRequestId(UUID vvoId, UUID testRequestId);

    void deleteByVvoIdAndTestRequestId(UUID vvoId, UUID testRequestId);
}
