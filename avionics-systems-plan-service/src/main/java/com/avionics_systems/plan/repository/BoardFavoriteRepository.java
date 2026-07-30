package com.avionics_systems.plan.repository;

import com.avionics_systems.plan.entity.BoardFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BoardFavoriteRepository extends JpaRepository<BoardFavorite, UUID> {
    List<BoardFavorite> findByUserIdOrderBySequenceAsc(UUID userId);
    Optional<BoardFavorite> findByBoardIdAndUserId(UUID boardId, UUID userId);
    boolean existsByBoardIdAndUserId(UUID boardId, UUID userId);
}