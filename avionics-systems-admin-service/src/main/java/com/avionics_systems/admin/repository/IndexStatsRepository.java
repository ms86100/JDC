package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.IndexStatsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface IndexStatsRepository extends JpaRepository<IndexStatsEntity, String> {
    Optional<IndexStatsEntity> findByIndexName(String indexName);
}