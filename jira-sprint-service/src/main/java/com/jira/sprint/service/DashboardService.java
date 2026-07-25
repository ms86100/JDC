package com.jira.sprint.service;

import com.jira.sprint.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class DashboardService {

    private final Map<UUID, DashboardResponse> dashboards = new java.util.concurrent.ConcurrentHashMap<>();
    private final MessageSource messageSource;

    @Value("${app.dashboard.default-gadget-types:STATS_GRAPH,ISSUES_ASSIGNED,DISTRIBUTION,CREATED_vs_RESOLVED,QUICK_LINKS}")
    private String defaultGadgetTypesStr;

    @Value("${app.dashboard.default-gadget-titles:Project Statistics,My Assigned Issues,Issue Distribution,Created vs Resolved,Quick Links}")
    private String defaultGadgetTitlesStr;

    @Value("${app.dashboard.distribution-labels:Bug,Story,Task,Epic}")
    private String distributionLabelsStr;

    @Value("${app.dashboard.distribution-colors:#dc3545,#28a745,#007bff,#6c757d}")
    private String distributionColorsStr;

    public DashboardService(MessageSource messageSource) {
        this.messageSource = messageSource;
        // Initialize with default dashboard
        DashboardResponse defaultDashboard = DashboardResponse.builder()
                .id(UUID.fromString("00000000-0000-0001-0000-000000000001"))
                .name("System Dashboard")
                .description("Default system dashboard with overview gadgets")
                .ownerId(UUID.fromString("00000000-0000-0000-0000-000000000000"))
                .isDefault(true)
                .isGlobal(true)
                .gadgets(Arrays.asList(
                        GadgetResponse.builder()
                                .id(UUID.randomUUID())
                                .gadgetType("STATS_GRAPH")
                                .title("Project Statistics")
                                .positionX(0).positionY(0)
                                .width(6).height(4)
                                .preferences(Map.of("projectId", "all"))
                                .build(),
                        GadgetResponse.builder()
                                .id(UUID.randomUUID())
                                .gadgetType("ISSUES_ASSIGNED")
                                .title("My Assigned Issues")
                                .positionX(6).positionY(0)
                                .width(6).height(4)
                                .preferences(Map.of("count", 10))
                                .build(),
                        GadgetResponse.builder()
                                .id(UUID.randomUUID())
                                .gadgetType("DISTRIBUTION")
                                .title("Issue Distribution")
                                .positionX(0).positionY(4)
                                .width(4).height(4)
                                .preferences(Map.of("groupBy", "type"))
                                .build(),
                        GadgetResponse.builder()
                                .id(UUID.randomUUID())
                                .gadgetType("CREATED_vs_RESOLVED")
                                .title("Created vs Resolved")
                                .positionX(4).positionY(4)
                                .width(4).height(4)
                                .preferences(Map.of("days", 30))
                                .build(),
                        GadgetResponse.builder()
                                .id(UUID.randomUUID())
                                .gadgetType("QUICK_LINKS")
                                .title("Quick Links")
                                .positionX(8).positionY(4)
                                .width(4).height(4)
                                .preferences(Map.of())
                                .build()
                ))
                .createdAt(LocalDateTime.now().minusDays(7))
                .updatedAt(LocalDateTime.now())
                .build();

        dashboards.put(defaultDashboard.getId(), defaultDashboard);

        // User dashboard
        DashboardResponse userDashboard = DashboardResponse.builder()
                .id(UUID.randomUUID())
                .name("My Dashboard")
                .description("Personal dashboard with custom gadgets")
                .ownerId(UUID.randomUUID())
                .isDefault(true)
                .isGlobal(false)
                .gadgets(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        dashboards.put(userDashboard.getId(), userDashboard);
    }

    public List<DashboardResponse> getDashboards(UUID userId, boolean includeGlobal) {
        List<DashboardResponse> result = new ArrayList<>();

        for (DashboardResponse dashboard : dashboards.values()) {
            if (includeGlobal && dashboard.getIsGlobal()) {
                result.add(dashboard);
            }
            if (dashboard.getOwnerId() != null && dashboard.getOwnerId().equals(userId)) {
                result.add(dashboard);
            }
        }

        return result;
    }

    public DashboardResponse getDashboard(UUID dashboardId) {
        return dashboards.get(dashboardId);
    }

    public DashboardResponse createDashboard(UUID userId, CreateDashboardRequest request) {
        DashboardResponse dashboard = DashboardResponse.builder()
                .id(UUID.randomUUID())
                .name(request.getName())
                .description(request.getDescription())
                .ownerId(userId)
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .isGlobal(request.getIsGlobal() != null ? request.getIsGlobal() : false)
                .gadgets(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        dashboards.put(dashboard.getId(), dashboard);
        log.info("Created dashboard: {} for user: {}", dashboard.getName(), userId);

        return dashboard;
    }

    public DashboardResponse updateDashboard(UUID dashboardId, CreateDashboardRequest request) {
        DashboardResponse dashboard = dashboards.get(dashboardId);
        if (dashboard == null) {
            throw new IllegalArgumentException(messageSource.getMessage("error.dashboard.not.found", new Object[]{dashboardId}, Locale.ENGLISH));
        }

        if (request.getName() != null) {
            dashboard.setName(request.getName());
        }
        if (request.getDescription() != null) {
            dashboard.setDescription(request.getDescription());
        }
        dashboard.setUpdatedAt(LocalDateTime.now());

        dashboards.put(dashboardId, dashboard);
        return dashboard;
    }

    public void deleteDashboard(UUID dashboardId) {
        dashboards.remove(dashboardId);
        log.info("Deleted dashboard: {}", dashboardId);
    }

    public DashboardResponse addGadget(UUID dashboardId, GadgetResponse gadget) {
        DashboardResponse dashboard = dashboards.get(dashboardId);
        if (dashboard == null) {
            throw new IllegalArgumentException(messageSource.getMessage("error.dashboard.not.found", new Object[]{dashboardId}, Locale.ENGLISH));
        }

        if (dashboard.getGadgets() == null) {
            dashboard.setGadgets(new ArrayList<>());
        }

        gadget.setId(UUID.randomUUID());
        dashboard.getGadgets().add(gadget);
        dashboard.setUpdatedAt(LocalDateTime.now());

        dashboards.put(dashboardId, dashboard);
        return dashboard;
    }

    public DashboardResponse updateGadget(UUID dashboardId, UUID gadgetId, GadgetResponse gadget) {
        DashboardResponse dashboard = dashboards.get(dashboardId);
        if (dashboard == null) {
            throw new IllegalArgumentException(messageSource.getMessage("error.dashboard.not.found", new Object[]{dashboardId}, Locale.ENGLISH));
        }

        List<GadgetResponse> gadgets = dashboard.getGadgets();
        for (int i = 0; i < gadgets.size(); i++) {
            if (gadgets.get(i).getId().equals(gadgetId)) {
                gadget.setId(gadgetId);
                gadgets.set(i, gadget);
                dashboard.setUpdatedAt(LocalDateTime.now());
                dashboards.put(dashboardId, dashboard);
                return dashboard;
            }
        }

        throw new IllegalArgumentException(messageSource.getMessage("error.gadget.not.found", new Object[]{gadgetId}, Locale.ENGLISH));
    }

    public DashboardResponse removeGadget(UUID dashboardId, UUID gadgetId) {
        DashboardResponse dashboard = dashboards.get(dashboardId);
        if (dashboard == null) {
            throw new IllegalArgumentException(messageSource.getMessage("error.dashboard.not.found", new Object[]{dashboardId}, Locale.ENGLISH));
        }

        dashboard.getGadgets().removeIf(g -> g.getId().equals(gadgetId));
        dashboard.setUpdatedAt(LocalDateTime.now());

        dashboards.put(dashboardId, dashboard);
        return dashboard;
    }

    public Map<String, Object> getGadgetData(String gadgetType, Map<String, Object> preferences) {
        Map<String, Object> data = new HashMap<>();

        switch (gadgetType) {
            case "STATS_GRAPH":
                data.put("totalIssues", 156);
                data.put("openIssues", 45);
                data.put("resolvedIssues", 111);
                data.put("criticalIssues", 5);
                break;

            case "ISSUES_ASSIGNED":
                data.put("issues", Arrays.asList(
                        Map.of("key", "JRA-1", "title", "Sample Issue 1", "priority", "High", "status", "In Progress"),
                        Map.of("key", "JRA-2", "title", "Sample Issue 2", "priority", "Medium", "status", "To Do"),
                        Map.of("key", "JRA-3", "title", "Sample Issue 3", "priority", "Low", "status", "Done")
                ));
                break;

            case "DISTRIBUTION":
                data.put("labels", Arrays.asList(distributionLabelsStr.split(",")));
                data.put("values", Arrays.asList(25, 40, 30, 5));
                data.put("colors", Arrays.asList(distributionColorsStr.split(",")));
                break;

            case "CREATED_vs_RESOLVED":
                data.put("labels", Arrays.asList("Jan", "Feb", "Mar", "Apr", "May", "Jun"));
                data.put("created", Arrays.asList(45, 52, 38, 61, 55, 48));
                data.put("resolved", Arrays.asList(40, 48, 52, 45, 58, 62));
                break;

            case "QUICK_LINKS":
                data.put("links", Arrays.asList(
                        Map.of("title", "Create Issue", "url", "/issues/create", "icon", "➕"),
                        Map.of("title", "My Profile", "url", "/profile", "icon", "👤"),
                        Map.of("title", "Project Settings", "url", "/projects/settings", "icon", "⚙️")
                ));
                break;

            default:
                data.put("message", "Gadget data not available");
        }

        return data;
    }
}