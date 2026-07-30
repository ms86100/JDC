package com.avionics_systems.dashboard.service;

import com.avionics_systems.dashboard.dto.*;
import com.avionics_systems.dashboard.entity.*;
import com.avionics_systems.dashboard.exception.ResourceNotFoundException;
import com.avionics_systems.dashboard.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final DashboardShareRepository dashboardShareRepository;
    private final GadgetInstanceRepository gadgetInstanceRepository;
    private final GadgetRepository gadgetRepository;

    @Value("${app.defaults.dashboard-layout:DEFAULT}")
    private String defaultDashboardLayout;

    @Value("${app.defaults.share-permission-type:VIEW}")
    private String defaultSharePermissionType;

    @Transactional
    public DashboardResponse createDashboard(CreateDashboardRequest request, UUID ownerId) {
        log.info("Creating dashboard '{}' for user {}", request.getName(), ownerId);

        Dashboard dashboard = Dashboard.builder()
                .name(request.getName())
                .description(request.getDescription())
                .ownerId(ownerId)
                .projectId(request.getProjectId())
                .isShared(request.getIsShared() != null ? request.getIsShared() : false)
                .layout(request.getLayout() != null ? request.getLayout() : defaultDashboardLayout)
                .config(request.getConfig())
                .build();

        dashboard = dashboardRepository.save(dashboard);
        log.info("Created dashboard with ID: {}", dashboard.getId());

        return toDashboardResponse(dashboard);
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(UUID dashboardId) {
        Dashboard dashboard = dashboardRepository.findById(dashboardId)
                .orElseThrow(() -> new ResourceNotFoundException("Dashboard", "id", dashboardId));
        return toDashboardResponse(dashboard);
    }

    @Transactional(readOnly = true)
    public Page<DashboardResponse> getDashboardsByOwner(UUID ownerId, Pageable pageable) {
        return dashboardRepository.findByOwnerId(ownerId, pageable)
                .map(this::toDashboardResponse);
    }

    @Transactional(readOnly = true)
    public List<DashboardResponse> getAccessibleDashboards(UUID userId) {
        return dashboardRepository.findAccessibleDashboards(userId).stream()
                .map(this::toDashboardResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DashboardResponse> getFavoriteDashboards(UUID userId) {
        return dashboardRepository.findByIsFavoriteTrueAndOwnerId(userId).stream()
                .map(this::toDashboardResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DashboardResponse updateDashboard(UUID dashboardId, UpdateDashboardRequest request, UUID userId) {
        log.info("Updating dashboard {} by user {}", dashboardId, userId);

        Dashboard dashboard = dashboardRepository.findById(dashboardId)
                .orElseThrow(() -> new ResourceNotFoundException("Dashboard", "id", dashboardId));

        if (request.getName() != null) {
            dashboard.setName(request.getName());
        }
        if (request.getDescription() != null) {
            dashboard.setDescription(request.getDescription());
        }
        if (request.getIsShared() != null) {
            dashboard.setIsShared(request.getIsShared());
        }
        if (request.getLayout() != null) {
            dashboard.setLayout(request.getLayout());
        }
        if (request.getPermissionLevel() != null) {
            dashboard.setPermissionLevel(request.getPermissionLevel());
        }
        if (request.getConfig() != null) {
            dashboard.setConfig(request.getConfig());
        }
        if (request.getOrdering() != null) {
            dashboard.setOrdering(request.getOrdering());
        }

        dashboard.setUpdatedBy(userId);
        dashboard = dashboardRepository.save(dashboard);

        return toDashboardResponse(dashboard);
    }

    @Transactional
    public void deleteDashboard(UUID dashboardId) {
        log.info("Deleting dashboard: {}", dashboardId);

        dashboardShareRepository.deleteByDashboardId(dashboardId);
        gadgetInstanceRepository.deleteByDashboardId(dashboardId);
        dashboardRepository.deleteById(dashboardId);
    }

    @Transactional
    public DashboardResponse shareDashboard(UUID dashboardId, ShareDashboardRequest request) {
        log.info("Sharing dashboard {} with type {}", dashboardId, request.getShareType());

        Dashboard dashboard = dashboardRepository.findById(dashboardId)
                .orElseThrow(() -> new ResourceNotFoundException("Dashboard", "id", dashboardId));

        DashboardShare share = DashboardShare.builder()
                .dashboardId(dashboardId)
                .shareType(request.getShareType())
                .shareId(request.getShareId())
                .shareName(request.getShareName())
                .permissionType(request.getPermissionType() != null ? request.getPermissionType() : defaultSharePermissionType)
                .build();

        dashboardShareRepository.save(share);

        dashboard.setIsShared(true);
        dashboard = dashboardRepository.save(dashboard);

        return toDashboardResponse(dashboard);
    }

    @Transactional
    public DashboardResponse toggleFavorite(UUID dashboardId, UUID userId) {
        Dashboard dashboard = dashboardRepository.findById(dashboardId)
                .orElseThrow(() -> new ResourceNotFoundException("Dashboard", "id", dashboardId));

        dashboard.setIsFavorite(!dashboard.getIsFavorite());
        dashboard.setUpdatedBy(userId);
        dashboard = dashboardRepository.save(dashboard);

        return toDashboardResponse(dashboard);
    }

    @Transactional(readOnly = true)
    public List<GadgetResponse> getAvailableGadgets(String category) {
        List<Gadget> gadgets;
        if (category != null && !category.isEmpty()) {
            gadgets = gadgetRepository.findEnabledByCategory(category);
        } else {
            gadgets = gadgetRepository.findAllEnabledGadgets();
        }

        return gadgets.stream()
                .map(this::toGadgetResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GadgetInstanceResponse> getDashboardGadgets(UUID dashboardId) {
        return gadgetInstanceRepository.findByDashboardIdOrderByPositionRowAscPositionColumnAsc(dashboardId)
                .stream()
                .map(this::toGadgetInstanceResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public GadgetInstanceResponse addGadgetToDashboard(UUID dashboardId, CreateGadgetInstanceRequest request) {
        log.info("Adding gadget {} to dashboard {}", request.getGadgetId(), dashboardId);

        dashboardRepository.findById(dashboardId)
                .orElseThrow(() -> new ResourceNotFoundException("Dashboard", "id", dashboardId));

        Gadget gadget = gadgetRepository.findById(request.getGadgetId())
                .orElseThrow(() -> new ResourceNotFoundException("Gadget", "id", request.getGadgetId()));

        GadgetInstance instance = GadgetInstance.builder()
                .dashboardId(dashboardId)
                .gadgetId(request.getGadgetId())
                .title(request.getTitle() != null ? request.getTitle() : gadget.getTitle())
                .positionRow(request.getPositionRow())
                .positionColumn(request.getPositionColumn())
                .width(request.getWidth())
                .height(request.getHeight())
                .config(request.getConfig())
                .filters(request.getFilters())
                .color(request.getColor())
                .build();

        instance = gadgetInstanceRepository.save(instance);
        return toGadgetInstanceResponse(instance);
    }

    @Transactional
    public void removeGadgetFromDashboard(UUID dashboardId, UUID gadgetInstanceId) {
        log.info("Removing gadget instance {} from dashboard {}", gadgetInstanceId, dashboardId);
        gadgetInstanceRepository.deleteById(gadgetInstanceId);
    }

    @Transactional
    public GadgetInstanceResponse updateGadgetInstance(UUID gadgetInstanceId, String config, String filters) {
        GadgetInstance instance = gadgetInstanceRepository.findById(gadgetInstanceId)
                .orElseThrow(() -> new ResourceNotFoundException("GadgetInstance", "id", gadgetInstanceId));

        if (config != null) {
            instance.setConfig(config);
        }
        if (filters != null) {
            instance.setFilters(filters);
        }

        instance = gadgetInstanceRepository.save(instance);
        return toGadgetInstanceResponse(instance);
    }

    @Transactional
    public GadgetInstanceResponse toggleGadgetMinimized(UUID gadgetInstanceId) {
        GadgetInstance instance = gadgetInstanceRepository.findById(gadgetInstanceId)
                .orElseThrow(() -> new ResourceNotFoundException("GadgetInstance", "id", gadgetInstanceId));

        instance.setIsMinimized(!instance.getIsMinimized());
        instance = gadgetInstanceRepository.save(instance);

        return toGadgetInstanceResponse(instance);
    }

    private DashboardResponse toDashboardResponse(Dashboard dashboard) {
        List<DashboardShare> shares = dashboardShareRepository.findByDashboardId(dashboard.getId());
        List<GadgetInstance> gadgets = gadgetInstanceRepository.findByDashboardIdOrderByPositionRowAscPositionColumnAsc(dashboard.getId());

        return DashboardResponse.builder()
                .id(dashboard.getId())
                .name(dashboard.getName())
                .description(dashboard.getDescription())
                .ownerId(dashboard.getOwnerId())
                .projectId(dashboard.getProjectId())
                .isShared(dashboard.getIsShared())
                .isSystem(dashboard.getIsSystem())
                .isFavorite(dashboard.getIsFavorite())
                .layout(dashboard.getLayout())
                .permissionLevel(dashboard.getPermissionLevel())
                .sharePermissionType(dashboard.getSharePermissionType())
                .popularity(dashboard.getPopularity())
                .ordering(dashboard.getOrdering())
                .config(dashboard.getConfig())
                .shares(shares.stream().map(this::toDashboardShareResponse).collect(Collectors.toList()))
                .gadgets(gadgets.stream().map(this::toGadgetInstanceResponse).collect(Collectors.toList()))
                .createdAt(dashboard.getCreatedAt())
                .updatedAt(dashboard.getUpdatedAt())
                .updatedBy(dashboard.getUpdatedBy())
                .build();
    }

    private DashboardShareResponse toDashboardShareResponse(DashboardShare share) {
        return DashboardShareResponse.builder()
                .id(share.getId())
                .dashboardId(share.getDashboardId())
                .shareType(share.getShareType())
                .shareId(share.getShareId())
                .shareName(share.getShareName())
                .permissionType(share.getPermissionType())
                .createdAt(share.getCreatedAt())
                .build();
    }

    private GadgetResponse toGadgetResponse(Gadget gadget) {
        return GadgetResponse.builder()
                .id(gadget.getId())
                .title(gadget.getTitle())
                .description(gadget.getDescription())
                .moduleKey(gadget.getModuleKey())
                .category(gadget.getCategory())
                .thumbnailUrl(gadget.getThumbnailUrl())
                .configSchema(gadget.getConfigSchema())
                .configDefaults(gadget.getConfigDefaults())
                .isEnabled(gadget.getIsEnabled())
                .isSystem(gadget.getIsSystem())
                .isSensitive(gadget.getIsSensitive())
                .permissionType(gadget.getPermissionType())
                .apiVersion(gadget.getApiVersion())
                .build();
    }

    private GadgetInstanceResponse toGadgetInstanceResponse(GadgetInstance instance) {
        Gadget gadget = gadgetRepository.findById(instance.getGadgetId()).orElse(null);

        return GadgetInstanceResponse.builder()
                .id(instance.getId())
                .dashboardId(instance.getDashboardId())
                .gadgetId(instance.getGadgetId())
                .gadgetModuleKey(gadget != null ? gadget.getModuleKey() : null)
                .gadgetCategory(gadget != null ? gadget.getCategory() : null)
                .title(instance.getTitle())
                .positionRow(instance.getPositionRow())
                .positionColumn(instance.getPositionColumn())
                .width(instance.getWidth())
                .height(instance.getHeight())
                .config(instance.getConfig())
                .filters(instance.getFilters())
                .color(instance.getColor())
                .isMinimized(instance.getIsMinimized())
                .isCollapsed(instance.getIsCollapsed())
                .createdAt(instance.getCreatedAt())
                .updatedAt(instance.getUpdatedAt())
                .build();
    }
}
