package com.avionics_systems.portal.repository;

import com.avionics_systems.portal.entity.PortalComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PortalCommentRepository extends JpaRepository<PortalComment, UUID> {

    List<PortalComment> findByRequestIdOrderByCreatedAtAsc(UUID requestId);

    List<PortalComment> findByRequestIdAndIsPublicTrueOrderByCreatedAtAsc(UUID requestId);

    List<PortalComment> findByAuthorId(UUID authorId);
}