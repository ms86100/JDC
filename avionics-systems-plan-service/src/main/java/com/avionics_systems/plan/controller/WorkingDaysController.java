package com.avionics_systems.plan.controller;

import com.avionics_systems.plan.dto.request.CreateNonWorkingDayRequest;
import com.avionics_systems.plan.dto.request.CreateTeamAvailabilityRequest;
import com.avionics_systems.plan.dto.request.CreateWorkingDaysRequest;
import com.avionics_systems.plan.dto.response.CapacityResponse;
import com.avionics_systems.plan.dto.response.NonWorkingDayResponse;
import com.avionics_systems.plan.dto.response.TeamAvailabilityResponse;
import com.avionics_systems.plan.dto.response.WorkingDaysResponse;
import com.avionics_systems.plan.service.WorkingDaysService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for Working Days and Holidays management.
 */
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class WorkingDaysController {

    private final WorkingDaysService workingDaysService;

    // Working Days Config

    @GetMapping("/working-days")
    public ResponseEntity<List<WorkingDaysResponse>> getAllWorkingDaysConfigs() {
        return ResponseEntity.ok(workingDaysService.getAllWorkingDaysConfigs());
    }

    @GetMapping("/working-days/default")
    public ResponseEntity<WorkingDaysResponse> getDefaultConfig() {
        return ResponseEntity.ok(workingDaysService.getDefaultWorkingDaysConfig());
    }

    @GetMapping("/working-days/{id}")
    public ResponseEntity<WorkingDaysResponse> getWorkingDaysConfig(@PathVariable UUID id) {
        return ResponseEntity.ok(workingDaysService.getWorkingDaysConfig(id));
    }

    @PostMapping("/working-days")
    public ResponseEntity<WorkingDaysResponse> createWorkingDaysConfig(
            @RequestBody CreateWorkingDaysRequest request) {
        return ResponseEntity.ok(workingDaysService.createWorkingDaysConfig(request));
    }

    @PutMapping("/working-days/{id}")
    public ResponseEntity<WorkingDaysResponse> updateWorkingDaysConfig(
            @PathVariable UUID id,
            @RequestBody CreateWorkingDaysRequest request) {
        return ResponseEntity.ok(workingDaysService.updateWorkingDaysConfig(id, request));
    }

    @DeleteMapping("/working-days/{id}")
    public ResponseEntity<Void> deleteWorkingDaysConfig(@PathVariable UUID id) {
        workingDaysService.deleteWorkingDaysConfig(id);
        return ResponseEntity.noContent().build();
    }

    // Holidays

    @GetMapping("/working-days/{configId}/holidays")
    public ResponseEntity<List<NonWorkingDayResponse>> getHolidays(@PathVariable UUID configId) {
        return ResponseEntity.ok(workingDaysService.getHolidays(configId));
    }

    @PostMapping("/working-days/{configId}/holidays")
    public ResponseEntity<NonWorkingDayResponse> addHoliday(
            @PathVariable UUID configId,
            @RequestBody CreateNonWorkingDayRequest request) {
        return ResponseEntity.ok(workingDaysService.addHoliday(configId, request));
    }

    @DeleteMapping("/working-days/{configId}/holidays/{holidayId}")
    public ResponseEntity<Void> removeHoliday(
            @PathVariable UUID configId,
            @PathVariable UUID holidayId) {
        workingDaysService.removeHoliday(configId, holidayId);
        return ResponseEntity.noContent().build();
    }

    // Team Availability

    @GetMapping("/teams/{teamId}/availability")
    public ResponseEntity<List<TeamAvailabilityResponse>> getTeamAvailability(
            @PathVariable UUID teamId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(workingDaysService.getTeamAvailability(teamId, start, end));
    }

    @PostMapping("/teams/{teamId}/availability")
    public ResponseEntity<TeamAvailabilityResponse> setTeamAvailability(
            @PathVariable UUID teamId,
            @RequestBody CreateTeamAvailabilityRequest request) {
        return ResponseEntity.ok(workingDaysService.setTeamAvailability(teamId, request));
    }

    // Capacity calculations

    @GetMapping("/teams/{teamId}/capacity")
    public ResponseEntity<CapacityResponse> getTeamCapacity(
            @PathVariable UUID teamId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(workingDaysService.getTeamCapacity(teamId, start, end));
    }

    @PostMapping("/calculate-working-days")
    public ResponseEntity<Long> calculateWorkingDays(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam UUID configId) {
        long workingDays = workingDaysService.calculateWorkingDays(configId, start, end);
        return ResponseEntity.ok(workingDays);
    }
}