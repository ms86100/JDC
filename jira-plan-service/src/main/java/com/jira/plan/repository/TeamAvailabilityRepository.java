package com.jira.plan.repository;

import com.jira.plan.entity.TeamAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamAvailabilityRepository extends JpaRepository<TeamAvailability, UUID> {

    List<TeamAvailability> findByTeamId(UUID teamId);

    Optional<TeamAvailability> findByTeamIdAndUserIdAndDate(UUID teamId, UUID userId, LocalDate date);

    @Query("SELECT ta FROM TeamAvailability ta WHERE ta.team.id = :teamId AND ta.date BETWEEN :start AND :end")
    List<TeamAvailability> findByTeamIdAndDateRange(
        @Param("teamId") UUID teamId,
        @Param("start") LocalDate start,
        @Param("end") LocalDate end
    );

    @Query("SELECT ta FROM TeamAvailability ta WHERE ta.team.id = :teamId AND ta.userId = :userId AND ta.date BETWEEN :start AND :end")
    List<TeamAvailability> findByTeamIdAndUserIdAndDateRange(
        @Param("teamId") UUID teamId,
        @Param("userId") UUID userId,
        @Param("start") LocalDate start,
        @Param("end") LocalDate end
    );

    @Query("SELECT SUM(ta.hours) FROM TeamAvailability ta WHERE ta.team.id = :teamId AND ta.date BETWEEN :start AND :end")
    Optional<Double> sumHoursByTeamIdAndDateRange(
        @Param("teamId") UUID teamId,
        @Param("start") LocalDate start,
        @Param("end") LocalDate end
    );
}