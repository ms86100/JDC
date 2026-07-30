package com.avionics_systems.plan.repository;

import com.avionics_systems.plan.entity.NonWorkingDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface NonWorkingDayRepository extends JpaRepository<NonWorkingDay, UUID> {

    List<NonWorkingDay> findByWorkingDaysId(UUID workingDaysId);

    boolean existsByWorkingDaysIdAndDate(UUID workingDaysId, LocalDate date);

    @Query("SELECT nwd FROM NonWorkingDay nwd WHERE nwd.workingDays.id = :configId AND nwd.date BETWEEN :start AND :end")
    List<NonWorkingDay> findByConfigIdAndDateRange(
        @Param("configId") UUID configId,
        @Param("start") LocalDate start,
        @Param("end") LocalDate end
    );

    void deleteByWorkingDaysIdAndDate(UUID workingDaysId, LocalDate date);
}