package com.avionics_systems.report.service;

import com.avionics_systems.report.dto.*;
import com.avionics_systems.report.entity.*;
import com.avionics_systems.report.exception.ResourceNotFoundException;
import com.avionics_systems.report.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final TimeTrackingReportRepository timeTrackingReportRepository;
    private final SprintReportRepository sprintReportRepository;
    private final ProjectReportRepository projectReportRepository;
    private final SavedReportRepository savedReportRepository;

    private final RestTemplate restTemplate;
    private final MessageSource messageSource;

    @Value("${app.service.issue-url:http://localhost:8084}")
    private String issueServiceUrl;

    @Value("${app.service.sprint-url:http://localhost:8085}")
    private String sprintServiceUrl;

    @Value("${app.service.project-url:http://localhost:8083}")
    private String projectServiceUrl;

    @Value("${app.defaults.time-tracking-report-prefix:Time Tracking Report - }")
    private String timeTrackingReportPrefix;

    @Value("${app.defaults.sprint-report-name:Sprint Report}")
    private String defaultSprintReportName;

    @Value("${app.defaults.project-report-prefix:Project Report - }")
    private String projectReportPrefix;

    @Value("${app.defaults.time-tracking-report-type:USER}")
    private String defaultTimeTrackingReportType;

    @Value("${app.defaults.project-report-type:SUMMARY}")
    private String defaultProjectReportType;

    @Value("${app.defaults.time-format-zero:0h 0m}")
    private String timeFormatZero;

    @Value("${app.defaults.time-format-pattern:%dh %dm}")
    private String timeFormatPattern;

    @Transactional
    public TimeTrackingReportResponse generateTimeTrackingReport(TimeTrackingReportRequest request, UUID userId) {
        log.info("Generating time tracking report for user {} between {} and {}",
                userId, request.getStartDate(), request.getEndDate());

        TimeTrackingReport report = TimeTrackingReport.builder()
                .name(timeTrackingReportPrefix + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .projectId(request.getProjectId())
                .issueId(request.getIssueId())
                .userId(request.getUserId() != null ? request.getUserId() : userId)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .reportType(request.getReportType() != null ? request.getReportType() : defaultTimeTrackingReportType)
                .totalTimeSeconds(0L)
                .worklogDetails("[]")
                .breakdown("{}")
                .build();

        // In production, fetch actual worklog data from issue service
        report = timeTrackingReportRepository.save(report);

        return toTimeTrackingReportResponse(report);
    }

    @Transactional(readOnly = true)
    public List<TimeTrackingReportResponse> getTimeTrackingReportsByUser(UUID userId) {
        return timeTrackingReportRepository.findByUserId(userId).stream()
                .map(this::toTimeTrackingReportResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TimeTrackingReportResponse getTimeTrackingReport(UUID reportId) {
        TimeTrackingReport report = timeTrackingReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("TimeTrackingReport", "id", reportId));
        return toTimeTrackingReportResponse(report);
    }

    @Transactional
    public SprintReportResponse generateSprintReport(GenerateSprintReportRequest request) {
        log.info("Generating sprint report for sprint {}", request.getSprintId());

        // In production, fetch actual sprint data from sprint service
        SprintReport report = SprintReport.builder()
                .sprintId(request.getSprintId())
                .sprintName(request.getSprintName() != null ? request.getSprintName() : defaultSprintReportName)
                .projectId(request.getProjectId())
                .startDate(LocalDateTime.now().minusDays(14))
                .endDate(LocalDateTime.now())
                .totalIssues(0)
                .completedIssues(0)
                .incompleteIssues(0)
                .bugsCount(0)
                .completionRate(0.0)
                .totalStoryPoints(0.0)
                .completedStoryPoints(0.0)
                .totalTimeSeconds(0L)
                .issuesCompleted("[]")
                .issuesAddedDuringSprint("[]")
                .issuesNotCompleted("[]")
                .issuesLedged("[]")
                .burndownData("[]")
                .build();

        report = sprintReportRepository.save(report);

        return toSprintReportResponse(report);
    }

    @Transactional(readOnly = true)
    public SprintReportResponse getSprintReport(UUID reportId) {
        SprintReport report = sprintReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("SprintReport", "id", reportId));
        return toSprintReportResponse(report);
    }

    @Transactional(readOnly = true)
    public List<SprintReportResponse> getSprintReportsBySprint(UUID sprintId) {
        return sprintReportRepository.findBySprintId(sprintId).stream()
                .map(this::toSprintReportResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectReportResponse generateProjectReport(GenerateProjectReportRequest request) {
        log.info("Generating project report for project {}", request.getProjectId());

        ProjectReport report = ProjectReport.builder()
                .name(projectReportPrefix + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .projectId(request.getProjectId())
                .projectKey(request.getProjectKey())
                .reportDate(LocalDateTime.now())
                .totalIssues(0)
                .openIssues(0)
                .resolvedIssues(0)
                .totalStoryPoints(0.0)
                .completedStoryPoints(0.0)
                .velocity(0.0)
                .issuesByType("{}")
                .issuesByStatus("{}")
                .issuesByPriority("{}")
                .recentActivity("[]")
                .reportType(request.getReportType() != null ? request.getReportType() : defaultProjectReportType)
                .build();

        report = projectReportRepository.save(report);

        return toProjectReportResponse(report);
    }

    @Transactional(readOnly = true)
    public ProjectReportResponse getProjectReport(UUID reportId) {
        ProjectReport report = projectReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectReport", "id", reportId));
        return toProjectReportResponse(report);
    }

    @Transactional(readOnly = true)
    public List<ProjectReportResponse> getProjectReportsByProject(UUID projectId) {
        return projectReportRepository.findByProjectIdOrderByDate(projectId).stream()
                .map(this::toProjectReportResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SavedReportResponse saveReport(SaveReportRequest request, UUID userId) {
        log.info("Saving report '{}' for user {}", request.getName(), userId);

        SavedReport report = SavedReport.builder()
                .name(request.getName())
                .ownerId(userId)
                .projectId(request.getProjectId())
                .reportType(request.getReportType())
                .reportConfig(request.getReportConfig())
                .filters(request.getFilters())
                .schedule(request.getSchedule())
                .isShared(request.getIsShared() != null ? request.getIsShared() : false)
                .build();

        report = savedReportRepository.save(report);

        return toSavedReportResponse(report);
    }

    @Transactional(readOnly = true)
    public List<SavedReportResponse> getSavedReports(UUID userId) {
        return savedReportRepository.findByOwnerIdOrIsSharedTrue(userId).stream()
                .map(this::toSavedReportResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteSavedReport(UUID reportId, UUID userId) {
        log.info("Deleting saved report: {} by user: {}", reportId, userId);
        SavedReport report = savedReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("SavedReport", "id", reportId));
        if (userId != null && !report.getOwnerId().equals(userId)) {
            throw new IllegalArgumentException(messageSource.getMessage("error.report.delete.unauthorized", null, Locale.ENGLISH));
        }
        savedReportRepository.delete(report);
    }

    private TimeTrackingReportResponse toTimeTrackingReportResponse(TimeTrackingReport report) {
        return TimeTrackingReportResponse.builder()
                .id(report.getId())
                .name(report.getName())
                .projectId(report.getProjectId())
                .issueId(report.getIssueId())
                .userId(report.getUserId())
                .startDate(report.getStartDate())
                .endDate(report.getEndDate())
                .totalTimeSeconds(report.getTotalTimeSeconds())
                .formattedTotalTime(formatTime(report.getTotalTimeSeconds()))
                .worklogDetails(report.getWorklogDetails())
                .breakdown(report.getBreakdown())
                .reportType(report.getReportType())
                .createdAt(report.getCreatedAt())
                .build();
    }

    private SprintReportResponse toSprintReportResponse(SprintReport report) {
        return SprintReportResponse.builder()
                .id(report.getId())
                .sprintId(report.getSprintId())
                .sprintName(report.getSprintName())
                .projectId(report.getProjectId())
                .startDate(report.getStartDate())
                .endDate(report.getEndDate())
                .totalIssues(report.getTotalIssues())
                .completedIssues(report.getCompletedIssues())
                .incompleteIssues(report.getIncompleteIssues())
                .bugsCount(report.getBugsCount())
                .completionRate(report.getCompletionRate())
                .totalStoryPoints(report.getTotalStoryPoints())
                .completedStoryPoints(report.getCompletedStoryPoints())
                .totalTimeSeconds(report.getTotalTimeSeconds())
                .issuesCompleted(report.getIssuesCompleted())
                .issuesAddedDuringSprint(report.getIssuesAddedDuringSprint())
                .issuesNotCompleted(report.getIssuesNotCompleted())
                .issuesLedged(report.getIssuesLedged())
                .burndownData(report.getBurndownData())
                .createdAt(report.getCreatedAt())
                .build();
    }

    private ProjectReportResponse toProjectReportResponse(ProjectReport report) {
        return ProjectReportResponse.builder()
                .id(report.getId())
                .name(report.getName())
                .projectId(report.getProjectId())
                .projectKey(report.getProjectKey())
                .reportDate(report.getReportDate())
                .totalIssues(report.getTotalIssues())
                .openIssues(report.getOpenIssues())
                .resolvedIssues(report.getResolvedIssues())
                .totalStoryPoints(report.getTotalStoryPoints())
                .completedStoryPoints(report.getCompletedStoryPoints())
                .velocity(report.getVelocity())
                .issuesByType(report.getIssuesByType())
                .issuesByStatus(report.getIssuesByStatus())
                .issuesByPriority(report.getIssuesByPriority())
                .recentActivity(report.getRecentActivity())
                .reportType(report.getReportType())
                .createdAt(report.getCreatedAt())
                .build();
    }

    private SavedReportResponse toSavedReportResponse(SavedReport report) {
        return SavedReportResponse.builder()
                .id(report.getId())
                .name(report.getName())
                .ownerId(report.getOwnerId())
                .projectId(report.getProjectId())
                .reportType(report.getReportType())
                .reportConfig(report.getReportConfig())
                .filters(report.getFilters())
                .schedule(report.getSchedule())
                .lastRunAt(report.getLastRunAt())
                .isShared(report.getIsShared())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }

    private String formatTime(Long seconds) {
        if (seconds == null || seconds == 0) return timeFormatZero;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        return String.format(timeFormatPattern, hours, minutes);
    }
}