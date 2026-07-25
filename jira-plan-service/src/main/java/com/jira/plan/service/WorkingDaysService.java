package com.jira.plan.service;

import com.jira.plan.dto.request.CreateWorkingDaysRequest;
import com.jira.plan.dto.request.CreateNonWorkingDayRequest;
import com.jira.plan.dto.request.CreateTeamAvailabilityRequest;
import com.jira.plan.dto.response.WorkingDaysResponse;
import com.jira.plan.dto.response.NonWorkingDayResponse;
import com.jira.plan.dto.response.TeamAvailabilityResponse;
import com.jira.plan.dto.response.CapacityResponse;
import com.jira.plan.entity.*;
import com.jira.plan.exception.ResourceNotFoundException;
import com.jira.plan.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Working Days service for capacity planning and holiday management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkingDaysService {

    private final WorkingDaysRepository workingDaysRepository;
    private final NonWorkingDayRepository nonWorkingDayRepository;
    private final TeamAvailabilityRepository teamAvailabilityRepository;
    private final PlanTeamRepository planTeamRepository;
    private final PlanTeamMemberRepository planTeamMemberRepository;

    @Value("${app.working-days.default-name:Default}")
    private String defaultConfigName;

    @Value("${app.working-days.default-hours-per-day:8.00}")
    private String defaultHoursPerDay;

    @Value("${app.working-days.default-working-days:MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY}")
    private String defaultWorkingDaysStr;

    @Transactional(readOnly = true)
    public List<WorkingDaysResponse> getAllWorkingDaysConfigs() {
        return workingDaysRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WorkingDaysResponse getWorkingDaysConfig(UUID id) {
        WorkingDays config = workingDaysRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("WorkingDays", "id", id));
        return toResponse(config);
    }

    @Transactional(readOnly = true)
    public WorkingDaysResponse getDefaultWorkingDaysConfig() {
        return workingDaysRepository.findByIsDefaultTrue()
            .map(this::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Default working days config not found"));
    }

    @Transactional
    public WorkingDaysResponse createWorkingDaysConfig(CreateWorkingDaysRequest request) {
        // If setting as default, unset existing default
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            workingDaysRepository.findByIsDefaultTrue()
                .ifPresent(existing -> {
                    existing.setIsDefault(false);
                    workingDaysRepository.save(existing);
                });
        }

        WorkingDays config = WorkingDays.builder()
            .name(request.getName())
            .monday(request.getMonday() != null ? request.getMonday() : true)
            .tuesday(request.getTuesday() != null ? request.getTuesday() : true)
            .wednesday(request.getWednesday() != null ? request.getWednesday() : true)
            .thursday(request.getThursday() != null ? request.getThursday() : true)
            .friday(request.getFriday() != null ? request.getFriday() : true)
            .saturday(request.getSaturday() != null ? request.getSaturday() : false)
            .sunday(request.getSunday() != null ? request.getSunday() : false)
            .hoursPerDay(request.getHoursPerDay() != null ? request.getHoursPerDay() : new BigDecimal(defaultHoursPerDay))
            .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
            .build();

        config = workingDaysRepository.save(config);
        return toResponse(config);
    }

    @Transactional
    public WorkingDaysResponse updateWorkingDaysConfig(UUID id, CreateWorkingDaysRequest request) {
        WorkingDays config = workingDaysRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("WorkingDays", "id", id));

        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(config.getIsDefault())) {
            workingDaysRepository.findByIsDefaultTrue()
                .ifPresent(existing -> {
                    existing.setIsDefault(false);
                    workingDaysRepository.save(existing);
                });
        }

        if (request.getName() != null) config.setName(request.getName());
        if (request.getMonday() != null) config.setMonday(request.getMonday());
        if (request.getTuesday() != null) config.setTuesday(request.getTuesday());
        if (request.getWednesday() != null) config.setWednesday(request.getWednesday());
        if (request.getThursday() != null) config.setThursday(request.getThursday());
        if (request.getFriday() != null) config.setFriday(request.getFriday());
        if (request.getSaturday() != null) config.setSaturday(request.getSaturday());
        if (request.getSunday() != null) config.setSunday(request.getSunday());
        if (request.getHoursPerDay() != null) config.setHoursPerDay(request.getHoursPerDay());
        if (request.getIsDefault() != null) config.setIsDefault(request.getIsDefault());

        config = workingDaysRepository.save(config);
        return toResponse(config);
    }

    @Transactional
    public void deleteWorkingDaysConfig(UUID id) {
        WorkingDays config = workingDaysRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("WorkingDays", "id", id));
        workingDaysRepository.delete(config);
    }

    // Non-working days (holidays) management

    @Transactional(readOnly = true)
    public List<NonWorkingDayResponse> getHolidays(UUID configId) {
        return nonWorkingDayRepository.findByWorkingDaysId(configId).stream()
            .map(this::toNonWorkingDayResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public NonWorkingDayResponse addHoliday(UUID configId, CreateNonWorkingDayRequest request) {
        WorkingDays config = workingDaysRepository.findById(configId)
            .orElseThrow(() -> new ResourceNotFoundException("WorkingDays", "id", configId));

        if (nonWorkingDayRepository.existsByWorkingDaysIdAndDate(configId, request.getDate())) {
            throw new IllegalArgumentException("Holiday already exists for this date");
        }

        NonWorkingDay holiday = NonWorkingDay.builder()
            .workingDays(config)
            .date(request.getDate())
            .name(request.getName())
            .build();

        holiday = nonWorkingDayRepository.save(holiday);
        return toNonWorkingDayResponse(holiday);
    }

    @Transactional
    public void removeHoliday(UUID configId, UUID holidayId) {
        NonWorkingDay holiday = nonWorkingDayRepository.findById(holidayId)
            .orElseThrow(() -> new ResourceNotFoundException("NonWorkingDay", "id", holidayId));

        if (!holiday.getWorkingDays().getId().equals(configId)) {
            throw new IllegalArgumentException("Holiday does not belong to this config");
        }

        nonWorkingDayRepository.delete(holiday);
    }

    // Working days calculation

    /**
     * Calculate working days between two dates (excluding holidays).
     * Includes both start and end dates in the calculation.
     */
    public long calculateWorkingDays(LocalDate start, LocalDate end, WorkingDays config) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        long workingDays = 0;
        LocalDate current = start;

        while (!current.isAfter(end)) {
            if (config.isWorkingDay(current) && !isHoliday(current, config)) {
                workingDays++;
            }
            current = current.plusDays(1);
        }

        return workingDays;
    }

    /**
     * Calculate working days using config ID.
     * Uses default config if not found.
     */
    public long calculateWorkingDays(UUID configId, LocalDate start, LocalDate end) {
        WorkingDays config = workingDaysRepository.findById(configId)
                .orElseGet(() -> workingDaysRepository.findByIsDefaultTrue()
                        .orElseGet(this::createDefaultConfig));
        return calculateWorkingDays(start, end, config);
    }

    /**
     * Calculate working hours between two dates.
     */
    public BigDecimal calculateWorkingHours(LocalDate start, LocalDate end, WorkingDays config) {
        long workingDays = calculateWorkingDays(start, end, config);
        return config.getHoursPerDay().multiply(BigDecimal.valueOf(workingDays));
    }

    /**
     * Add working days to a date.
     */
    public LocalDate addWorkingDays(LocalDate start, long days, WorkingDays config) {
        if (days == 0) return start;
        LocalDate result = start;
        long added = 0;
        long target = Math.abs(days);
        int step = days > 0 ? 1 : -1;
        while (added < target) {
            result = result.plusDays(step);
            if (config.isWorkingDay(result) && !isHoliday(result, config)) {
                added++;
            }
        }
        return result;
    }

    /**
     * Get team capacity for a date range.
     */
    @Transactional(readOnly = true)
    public CapacityResponse getTeamCapacity(UUID teamId, LocalDate start, LocalDate end) {
        PlanTeam team = planTeamRepository.findById(teamId)
            .orElseThrow(() -> new ResourceNotFoundException("PlanTeam", "id", teamId));

        List<PlanTeamMember> members = planTeamMemberRepository.findByTeamId(teamId);
        WorkingDays config = workingDaysRepository.findByIsDefaultTrue()
            .orElse(createDefaultConfig());

        long workingDays = calculateWorkingDays(start, end, config);
        BigDecimal hoursPerDay = config.getHoursPerDay();
        BigDecimal totalCapacity = BigDecimal.ZERO;
        BigDecimal totalTimeOff = BigDecimal.ZERO;

        for (PlanTeamMember member : members) {
            BigDecimal memberCapacity = hoursPerDay.multiply(BigDecimal.valueOf(workingDays));

            // Subtract time off for this member
            List<TeamAvailability> availabilities = teamAvailabilityRepository
                .findByTeamIdAndUserIdAndDateRange(teamId, member.getUserId(), start, end);

            BigDecimal memberTimeOff = availabilities.stream()
                .map(a -> a.getHours() != null ? a.getHours() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalTimeOff = totalTimeOff.add(memberTimeOff);
            totalCapacity = totalCapacity.add(memberCapacity.subtract(memberTimeOff));
        }

        return CapacityResponse.builder()
            .teamId(teamId)
            .startDate(start)
            .endDate(end)
            .workingDays(workingDays)
            .totalCapacityHours(totalCapacity)
            .totalTimeOffHours(totalTimeOff)
            .netCapacityHours(totalCapacity)
            .memberCount(members.size())
            .build();
    }

    // Team availability management

    @Transactional
    public TeamAvailabilityResponse setTeamAvailability(UUID teamId, CreateTeamAvailabilityRequest request) {
        PlanTeam team = planTeamRepository.findById(teamId)
            .orElseThrow(() -> new ResourceNotFoundException("PlanTeam", "id", teamId));

        // Find existing or create new
        TeamAvailability availability = teamAvailabilityRepository
            .findByTeamIdAndUserIdAndDate(teamId, request.getUserId(), request.getDate())
            .orElse(new TeamAvailability());

        availability.setTeam(team);
        availability.setUserId(request.getUserId());
        availability.setDate(request.getDate());
        availability.setHours(request.getHours());
        availability.setReason(request.getReason());

        availability = teamAvailabilityRepository.save(availability);
        return toTeamAvailabilityResponse(availability);
    }

    @Transactional(readOnly = true)
    public List<TeamAvailabilityResponse> getTeamAvailability(UUID teamId, LocalDate start, LocalDate end) {
        return teamAvailabilityRepository.findByTeamIdAndDateRange(teamId, start, end).stream()
            .map(this::toTeamAvailabilityResponse)
            .collect(Collectors.toList());
    }

    private boolean isHoliday(LocalDate date, WorkingDays config) {
        return nonWorkingDayRepository.existsByWorkingDaysIdAndDate(config.getId(), date);
    }

    private WorkingDays createDefaultConfig() {
        List<String> workingDaysList = List.of(defaultWorkingDaysStr.split(","));
        WorkingDays config = workingDaysRepository.save(WorkingDays.builder()
            .name(defaultConfigName)
            .monday(workingDaysList.contains("MONDAY"))
            .tuesday(workingDaysList.contains("TUESDAY"))
            .wednesday(workingDaysList.contains("WEDNESDAY"))
            .thursday(workingDaysList.contains("THURSDAY"))
            .friday(workingDaysList.contains("FRIDAY"))
            .saturday(workingDaysList.contains("SATURDAY"))
            .sunday(workingDaysList.contains("SUNDAY"))
            .hoursPerDay(new BigDecimal(defaultHoursPerDay))
            .isDefault(true)
            .build());
        return config;
    }

    private WorkingDaysResponse toResponse(WorkingDays config) {
        List<NonWorkingDay> holidays = nonWorkingDayRepository.findByWorkingDaysId(config.getId());
        return WorkingDaysResponse.builder()
            .id(config.getId())
            .name(config.getName())
            .monday(config.getMonday())
            .tuesday(config.getTuesday())
            .wednesday(config.getWednesday())
            .thursday(config.getThursday())
            .friday(config.getFriday())
            .saturday(config.getSaturday())
            .sunday(config.getSunday())
            .hoursPerDay(config.getHoursPerDay())
            .isDefault(config.getIsDefault())
            .holidays(holidays.stream().map(this::toNonWorkingDayResponse).collect(Collectors.toList()))
            .workingDaysPerWeek(config.getWorkingDaysPerWeek())
            .build();
    }

    private NonWorkingDayResponse toNonWorkingDayResponse(NonWorkingDay day) {
        return NonWorkingDayResponse.builder()
            .id(day.getId())
            .date(day.getDate())
            .name(day.getName())
            .build();
    }

    private TeamAvailabilityResponse toTeamAvailabilityResponse(TeamAvailability availability) {
        return TeamAvailabilityResponse.builder()
            .id(availability.getId())
            .teamId(availability.getTeam().getId())
            .userId(availability.getUserId())
            .date(availability.getDate())
            .hours(availability.getHours())
            .reason(availability.getReason())
            .build();
    }
}