package com.jira.plan.repository;

import com.jira.plan.entity.LexoRank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LexoRankRepository extends JpaRepository<LexoRank, UUID> {

    Optional<LexoRank> findByEntityTypeAndEntityId(String entityType, UUID entityId);

    List<LexoRank> findByEntityTypeAndBucketIdOrderByRankValueAsc(String entityType, Long bucketId);

    List<LexoRank> findByBucketIdOrderByRankValueAsc(Long bucketId);

    @Query("SELECT lr FROM LexoRank lr WHERE lr.entityType = :entityType AND lr.bucketId = :bucketId ORDER BY lr.rankValue ASC")
    List<LexoRank> findByEntityTypeAndBucketId(@Param("entityType") String entityType, @Param("bucketId") Long bucketId);

    @Query("SELECT lr FROM LexoRank lr WHERE lr.rankValue > :rank AND lr.bucketId = :bucketId ORDER BY lr.rankValue ASC LIMIT 1")
    Optional<LexoRank> findNextRank(@Param("rank") String rank, @Param("bucketId") Long bucketId);

    @Query("SELECT lr FROM LexoRank lr WHERE lr.rankValue < :rank AND lr.bucketId = :bucketId ORDER BY lr.rankValue DESC LIMIT 1")
    Optional<LexoRank> findPreviousRank(@Param("rank") String rank, @Param("bucketId") Long bucketId);

    boolean existsByEntityTypeAndEntityId(String entityType, UUID entityId);

    void deleteByEntityTypeAndEntityId(String entityType, UUID entityId);
}