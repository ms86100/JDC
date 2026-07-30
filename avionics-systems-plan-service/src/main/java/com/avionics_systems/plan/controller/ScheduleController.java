package com.avionics_systems.plan.controller;

import com.avionics_systems.plan.service.ScheduleEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleEngine scheduleEngine;

    @PostMapping("/forward")
    public ResponseEntity<ScheduleEngine.ScheduleResult> calculateForwardSchedule(
            @RequestParam UUID planId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        return ResponseEntity.ok(scheduleEngine.calculateForwardSchedule(planId, startDate));
    }

    @PostMapping("/backward")
    public ResponseEntity<ScheduleEngine.ScheduleResult> calculateBackwardSchedule(
            @RequestParam UUID planId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(scheduleEngine.calculateBackwardSchedule(planId, endDate));
    }

    @PostMapping("/propagate")
    public ResponseEntity<ScheduleEngine.ScheduleResult> propagateScheduleChanges(
            @RequestParam UUID planId,
            @RequestParam UUID changedItemId,
            @RequestParam(defaultValue = "0") int additionalDays) {
        return ResponseEntity.ok(scheduleEngine.propagateScheduleChanges(planId, changedItemId, additionalDays));
    }
}