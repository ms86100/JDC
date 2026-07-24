package com.jira.test.service;

import com.jira.test.dto.*;
import com.jira.test.entity.BenchDefect;
import com.jira.test.entity.ProblemReport;
import com.jira.test.entity.RequirementLink;
import com.jira.test.entity.TechEvent;
import com.jira.test.entity.VvoDefinition;
import com.jira.test.repository.BenchDefectRepository;
import com.jira.test.repository.HlvvoDefinitionRepository;
import com.jira.test.repository.ProblemReportRepository;
import com.jira.test.repository.RequirementLinkRepository;
import com.jira.test.repository.TechEventRepository;
import com.jira.test.repository.TestExecutionRepository;
import com.jira.test.repository.VvoDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VvReportingService {

    private final VvoDefinitionRepository vvoRepo;
    private final HlvvoDefinitionRepository hlvvoRepo;
    private final TechEventRepository techEventRepo;
    private final BenchDefectRepository benchDefectRepo;
    private final ProblemReportRepository problemReportRepo;
    private final TestExecutionRepository testExecutionRepo;
    private final RequirementLinkRepository requirementLinkRepo;

    // === VVO Coverage Report ===
    // Shows VVOs grouped by component/cluster with test status and linked defects
    @Transactional(readOnly = true)
    public VvoCoverageReport generateCoverageReport(UUID projectId, UUID fixVersionId) {
        log.info("Generating VVO coverage report for project {} fixVersion {}", projectId, fixVersionId);

        List<VvoDefinition> vvos = vvoRepo.findByFixVersionId(fixVersionId).stream()
                .filter(v -> v.getProjectId().equals(projectId))
                .sorted(Comparator.comparing(v -> String.join(",", v.getComponentIds() != null
                        ? v.getComponentIds().stream().map(UUID::toString).toList()
                        : List.of())))
                .toList();

        List<VvoCoverageItem> items = new ArrayList<>();
        for (VvoDefinition vvo : vvos) {
            // Find linked tests via requirementLink
            List<RequirementLink> links = requirementLinkRepo.findByRequirementKey(vvo.getIssueKey());

            VvoCoverageItem item = VvoCoverageItem.builder()
                    .vvoId(vvo.getId())
                    .issueKey(vvo.getIssueKey())
                    .summary(vvo.getSummary())
                    .status(vvo.getStatus())
                    .vvoVersion(vvo.getVvoVersion())
                    .idDoors(vvo.getIdDoors())
                    .applicability(vvo.getApplicability())
                    .linkedTestCount(links.size())
                    .coverageStatus(links.isEmpty() ? "NOT_COVERED" : "COVERED")
                    .componentIds(vvo.getComponentIds())
                    .build();
            items.add(item);
        }

        int total = items.size();
        long covered = items.stream().filter(i -> "COVERED".equals(i.getCoverageStatus())).count();
        long notCovered = total - covered;
        double coveragePercent = total > 0 ? (covered * 100.0 / total) : 0;

        VvoCoverageReport report = VvoCoverageReport.builder()
                .projectId(projectId)
                .fixVersionId(fixVersionId)
                .totalVvos(total)
                .coveredVvos((int) covered)
                .notCoveredVvos((int) notCovered)
                .coveragePercentage(Math.round(coveragePercent * 100.0) / 100.0)
                .items(items)
                .generatedAt(LocalDateTime.now())
                .build();

        log.info("VVO coverage report generated: {} total, {} covered, {}% coverage",
                total, covered, report.getCoveragePercentage());
        return report;
    }

    // === VVO Coverage CSV Export ===
    @Transactional(readOnly = true)
    public String exportCoverageReportCsv(UUID projectId, UUID fixVersionId) {
        log.info("Exporting VVO coverage report as CSV for project {} fixVersion {}", projectId, fixVersionId);

        VvoCoverageReport report = generateCoverageReport(projectId, fixVersionId);
        StringBuilder csv = new StringBuilder();
        csv.append("VVO Key,Summary,Status,Version,ID Doors,Applicability,Linked Tests,Coverage\n");

        for (VvoCoverageItem item : report.getItems()) {
            csv.append(escapeCsv(item.getIssueKey())).append(",");
            csv.append(escapeCsv(item.getSummary())).append(",");
            csv.append(escapeCsv(item.getStatus())).append(",");
            csv.append(item.getVvoVersion() != null ? item.getVvoVersion() : "").append(",");
            csv.append(escapeCsv(item.getIdDoors())).append(",");
            csv.append(escapeCsv(String.join(";",
                    item.getApplicability() != null ? item.getApplicability() : List.of()))).append(",");
            csv.append(item.getLinkedTestCount()).append(",");
            csv.append(item.getCoverageStatus());
            csv.append("\n");
        }

        log.info("VVO coverage CSV export completed with {} rows", report.getItems().size());
        return csv.toString();
    }

    // === TechEvent Summary Report ===
    @Transactional(readOnly = true)
    public TechEventSummaryReport generateTechEventReport(UUID projectId) {
        log.info("Generating TechEvent summary report for project {}", projectId);

        List<TechEvent> events = techEventRepo.findByProjectIdOrderByCreatedAtDesc(projectId);

        Map<String, Long> byStatus = events.stream()
                .collect(Collectors.groupingBy(TechEvent::getStatus, Collectors.counting()));
        Map<String, Long> byType = events.stream()
                .filter(e -> e.getDefectType() != null)
                .collect(Collectors.groupingBy(TechEvent::getDefectType, Collectors.counting()));
        Map<String, Long> byOrigin = events.stream()
                .filter(e -> e.getDefectOrigin() != null)
                .collect(Collectors.groupingBy(TechEvent::getDefectOrigin, Collectors.counting()));
        Map<String, Long> byImpact = events.stream()
                .filter(e -> e.getDefectImpact() != null)
                .collect(Collectors.groupingBy(TechEvent::getDefectImpact, Collectors.counting()));

        long open = events.stream()
                .filter(e -> !List.of("CLOSED", "CANCELLED").contains(e.getStatus()))
                .count();
        long closed = events.stream()
                .filter(e -> "CLOSED".equals(e.getStatus()))
                .count();
        long cancelled = events.stream()
                .filter(e -> "CANCELLED".equals(e.getStatus()))
                .count();

        TechEventSummaryReport report = TechEventSummaryReport.builder()
                .projectId(projectId)
                .totalEvents(events.size())
                .openCount((int) open)
                .closedCount((int) closed)
                .cancelledCount((int) cancelled)
                .countByStatus(byStatus)
                .countByDefectType(byType)
                .countByDefectOrigin(byOrigin)
                .countByDefectImpact(byImpact)
                .generatedAt(LocalDateTime.now())
                .build();

        log.info("TechEvent summary: {} total, {} open, {} closed, {} cancelled",
                events.size(), open, closed, cancelled);
        return report;
    }

    // === Bench Defect Summary ===
    @Transactional(readOnly = true)
    public BenchDefectSummaryReport generateBenchDefectReport(UUID projectId) {
        log.info("Generating Bench Defect summary report for project {}", projectId);

        List<BenchDefect> defects = benchDefectRepo.findByProjectIdOrderByCreatedAtDesc(projectId);

        Map<String, Long> byStatus = defects.stream()
                .collect(Collectors.groupingBy(BenchDefect::getStatus, Collectors.counting()));
        Map<String, Long> bySeverity = defects.stream()
                .filter(d -> d.getSeverity() != null)
                .collect(Collectors.groupingBy(BenchDefect::getSeverity, Collectors.counting()));

        BenchDefectSummaryReport report = BenchDefectSummaryReport.builder()
                .projectId(projectId)
                .totalDefects(defects.size())
                .countByStatus(byStatus)
                .countBySeverity(bySeverity)
                .generatedAt(LocalDateTime.now())
                .build();

        log.info("Bench Defect summary: {} total defects", defects.size());
        return report;
    }

    // === Problem Report Summary ===
    @Transactional(readOnly = true)
    public ProblemReportSummaryReport generateProblemReportSummary(UUID projectId) {
        log.info("Generating Problem Report summary for project {}", projectId);

        List<ProblemReport> reports = problemReportRepo.findByProjectIdOrderByCreatedAtDesc(projectId);

        Map<String, Long> byStatus = reports.stream()
                .collect(Collectors.groupingBy(ProblemReport::getStatus, Collectors.counting()));
        Map<String, Long> byType = reports.stream()
                .filter(r -> r.getPrType() != null)
                .collect(Collectors.groupingBy(ProblemReport::getPrType, Collectors.counting()));
        Map<String, Long> byOrigin = reports.stream()
                .filter(r -> r.getPrOrigin() != null)
                .collect(Collectors.groupingBy(ProblemReport::getPrOrigin, Collectors.counting()));

        long openPRs = reports.stream()
                .filter(r -> !"CLOSED".equals(r.getStatus()) && !"REJECTED".equals(r.getStatus()))
                .count();

        ProblemReportSummaryReport report = ProblemReportSummaryReport.builder()
                .projectId(projectId)
                .totalReports(reports.size())
                .openCount((int) openPRs)
                .countByStatus(byStatus)
                .countByPrType(byType)
                .countByPrOrigin(byOrigin)
                .generatedAt(LocalDateTime.now())
                .build();

        log.info("Problem Report summary: {} total, {} open", reports.size(), openPRs);
        return report;
    }

    // === Project Dashboard ===
    // Aggregated metrics across all V&V artifacts
    @Transactional(readOnly = true)
    public ProjectDashboardResponse getProjectDashboard(UUID projectId) {
        log.info("Generating project V&V dashboard for project {}", projectId);

        long vvoTotal = vvoRepo.countByProjectId(projectId);
        long vvoNew = vvoRepo.countByProjectIdAndStatus(projectId, "NEW");
        long vvoVerified = vvoRepo.countByProjectIdAndStatus(projectId, "VERIFIED");
        long vvoReleased = vvoRepo.countByProjectIdAndStatus(projectId, "RELEASED");

        List<TechEvent> techEvents = techEventRepo.findByProjectIdOrderByCreatedAtDesc(projectId);
        long teOpen = techEvents.stream()
                .filter(e -> !List.of("CLOSED", "CANCELLED").contains(e.getStatus()))
                .count();

        List<BenchDefect> benchDefects = benchDefectRepo.findByProjectIdOrderByCreatedAtDesc(projectId);
        long bdOpen = benchDefects.stream()
                .filter(d -> !List.of("CLOSED", "CANCELLED").contains(d.getStatus()))
                .count();
        long bdBlocking = benchDefects.stream()
                .filter(d -> "BLOCKING".equals(d.getSeverity()))
                .count();

        List<ProblemReport> prs = problemReportRepo.findByProjectIdOrderByCreatedAtDesc(projectId);
        long prOpen = prs.stream()
                .filter(r -> !List.of("CLOSED", "REJECTED").contains(r.getStatus()))
                .count();

        ProjectDashboardResponse dashboard = ProjectDashboardResponse.builder()
                .projectId(projectId)
                .vvoMetrics(ProjectDashboardResponse.VvoMetrics.builder()
                        .total((int) vvoTotal)
                        .newCount((int) vvoNew)
                        .verifiedCount((int) vvoVerified)
                        .releasedCount((int) vvoReleased)
                        .build())
                .techEventMetrics(ProjectDashboardResponse.DefectMetrics.builder()
                        .total(techEvents.size())
                        .openCount((int) teOpen)
                        .build())
                .benchDefectMetrics(ProjectDashboardResponse.DefectMetrics.builder()
                        .total(benchDefects.size())
                        .openCount((int) bdOpen)
                        .blockingCount((int) bdBlocking)
                        .build())
                .problemReportMetrics(ProjectDashboardResponse.DefectMetrics.builder()
                        .total(prs.size())
                        .openCount((int) prOpen)
                        .build())
                .generatedAt(LocalDateTime.now())
                .build();

        log.info("Project dashboard generated: VVOs={}, TechEvents={}, BenchDefects={}, PRs={}",
                vvoTotal, techEvents.size(), benchDefects.size(), prs.size());
        return dashboard;
    }

    // === Export for Planning (bench slot reservation) ===
    // Estimated duration per {Component, Test Means, Priority}
    @Transactional(readOnly = true)
    public String exportForPlanning(UUID testPlanId) {
        log.info("Generating planning export for test plan {}", testPlanId);

        StringBuilder csv = new StringBuilder();
        csv.append("Component,Test Means,Priority,Estimated Duration (hours)\n");
        // Placeholder -- actual implementation would aggregate TestExecution
        // originalEstimate by component/testMeans/priority from the given test plan
        log.info("Export for planning generated for test plan {}", testPlanId);
        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
