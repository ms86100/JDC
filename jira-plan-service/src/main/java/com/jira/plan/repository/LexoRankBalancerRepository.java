package com.jira.plan.repository;

import com.jira.plan.entity.LexoRankBalancer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LexoRankBalancerRepository extends JpaRepository<LexoRankBalancer, Long> {

    Optional<LexoRankBalancer> findByBucketIndex(Integer bucketIndex);
}