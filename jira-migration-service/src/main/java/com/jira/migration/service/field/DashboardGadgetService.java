package com.jira.migration.service.field;

import com.jira.migration.dto.DashboardGadgetConfigResponse;
import com.jira.migration.dto.SaveDashboardGadgetRequest;
import com.jira.migration.entity.field.CustomFieldDefinition;
import com.jira.migration.entity.field.DashboardGadgetFieldConfigEntity;
import com.jira.migration.entity.field.FieldDefinition;
import com.jira.migration.entity.field.FieldScreenMapping.FieldScreenType;
import com.jira.migration.entity.field.IssueFieldValue;
import com.jira.migration.repository.field.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardGadgetService {

    public static final String GADGET_CUSTOM_FIELD_STATS = "custom-field-statistics";
    public static final String GADGET_CUSTOM_FIELD_CHART = "custom-field-chart";
    public static final String GADGET_CUSTOM_FIELD_FILTER = "custom-field-filter";

    private final DashboardGadgetFieldConfigRepository gadgetConfigRepository;
    private final CustomFieldDefinitionRepository customFieldDefinitionRepository;
    private final FieldDefinitionRepository fieldDefinitionRepository;
    private final IssueFieldValueRepository issueFieldValueRepository;
    private final FieldVisibilityEngine fieldVisibilityEngine;

    @Transactional(readOnly = true)
    public DashboardGadgetConfigResponse getGadgetConfig(String dashboardKey, String gadgetKey, UUID projectId) {
        String dash = dashboardKey != null ? dashboardKey : "system";
        List<DashboardGadgetFieldConfigEntity> configured =
                gadgetConfigRepository.findByDashboardKeyAndGadgetKeyAndEnabledTrueOrderByDisplayOrderAsc(
                        dash, gadgetKey);

        List<DashboardGadgetConfigResponse.GadgetEligibleFieldDto> eligible =
                listEligibleForGadget(gadgetKey, projectId);

        List<DashboardGadgetConfigResponse.GadgetFieldDto> fieldDtos = configured.stream()
                .map(c -> DashboardGadgetConfigResponse.GadgetFieldDto.builder()
                        .fieldKey(c.getFieldKey())
                        .displayName(resolveDisplayName(c.getFieldKey()))
                        .chartType(c.getChartType())
                        .displayOrder(c.getDisplayOrder() != null ? c.getDisplayOrder() : 0)
                        .enabled(Boolean.TRUE.equals(c.getEnabled()))
                        .build())
                .toList();

        Map<String, Object> stats = buildStatistics(configured, projectId);

        return DashboardGadgetConfigResponse.builder()
                .dashboardKey(dash)
                .gadgetKey(gadgetKey)
                .configuredFields(fieldDtos)
                .eligibleFields(eligible)
                .statistics(stats)
                .build();
    }

    @Transactional
    public DashboardGadgetConfigResponse saveGadgetConfig(SaveDashboardGadgetRequest request, UUID projectId) {
        String dash = request.getDashboardKey() != null ? request.getDashboardKey() : "system";
        gadgetConfigRepository.deleteByDashboardKeyAndGadgetKey(dash, request.getGadgetKey());

        if (request.getFields() != null) {
            int order = 0;
            for (SaveDashboardGadgetRequest.GadgetFieldSelection sel : request.getFields()) {
                if (sel.getFieldKey() == null || sel.getFieldKey().isBlank()) {
                    continue;
                }
                if (!isGadgetEligible(sel.getFieldKey(), request.getGadgetKey(), projectId)) {
                    continue;
                }
                gadgetConfigRepository.save(DashboardGadgetFieldConfigEntity.builder()
                        .dashboardKey(dash)
                        .gadgetKey(request.getGadgetKey())
                        .fieldKey(sel.getFieldKey())
                        .chartType(sel.getChartType())
                        .displayOrder(sel.getDisplayOrder() != null ? sel.getDisplayOrder() : order++)
                        .enabled(sel.getEnabled() == null || sel.getEnabled())
                        .build());
            }
        }
        return getGadgetConfig(dash, request.getGadgetKey(), projectId);
    }

    @Transactional(readOnly = true)
    public List<String> listSupportedGadgets() {
        return List.of(GADGET_CUSTOM_FIELD_STATS, GADGET_CUSTOM_FIELD_CHART, GADGET_CUSTOM_FIELD_FILTER);
    }

    private List<DashboardGadgetConfigResponse.GadgetEligibleFieldDto> listEligibleForGadget(
            String gadgetKey, UUID projectId) {
        boolean chart = GADGET_CUSTOM_FIELD_CHART.equals(gadgetKey);
        boolean filter = GADGET_CUSTOM_FIELD_FILTER.equals(gadgetKey);

        List<DashboardGadgetConfigResponse.GadgetEligibleFieldDto> list = new ArrayList<>();
        for (CustomFieldDefinition cf : customFieldDefinitionRepository.findAllEnabled()) {
            if (!Boolean.TRUE.equals(cf.getSearchable()) && (chart || filter)) {
                continue;
            }
            if (!fieldVisibilityEngine.isFieldVisible(
                    cf.getFieldKey(), projectId, null, FieldScreenType.VIEW, null)) {
                continue;
            }
            list.add(DashboardGadgetConfigResponse.GadgetEligibleFieldDto.builder()
                    .fieldKey(cf.getFieldKey())
                    .displayName(resolveDisplayName(cf.getFieldKey()))
                    .fieldType(fieldDefinitionRepository.findByFieldKey(cf.getFieldKey())
                            .map(fd -> fd.getFieldType().name())
                            .orElse(cf.getType()))
                    .supportsChart(chart || GADGET_CUSTOM_FIELD_STATS.equals(gadgetKey))
                    .supportsFilter(filter || GADGET_CUSTOM_FIELD_STATS.equals(gadgetKey))
                    .build());
        }
        list.sort(Comparator.comparing(DashboardGadgetConfigResponse.GadgetEligibleFieldDto::getDisplayName,
                String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    private Map<String, Object> buildStatistics(
            List<DashboardGadgetFieldConfigEntity> configured, UUID projectId) {
        Map<String, Object> stats = new LinkedHashMap<>();
        for (DashboardGadgetFieldConfigEntity cfg : configured) {
            FieldDefinition def = fieldDefinitionRepository.findByFieldKey(cfg.getFieldKey()).orElse(null);
            if (def == null) {
                continue;
            }
            List<IssueFieldValue> values = issueFieldValueRepository.findByFieldDefinitionId(def.getId());
            Map<String, Long> distribution = values.stream()
                    .filter(v -> v.getStringValue() != null || v.getFormattedValue() != null)
                    .collect(Collectors.groupingBy(
                            v -> v.getFormattedValue() != null ? v.getFormattedValue() : v.getStringValue(),
                            Collectors.counting()));
            stats.put(cfg.getFieldKey(), Map.of(
                    "displayName", resolveDisplayName(cfg.getFieldKey()),
                    "totalValues", values.size(),
                    "distribution", distribution));
        }
        return stats;
    }

    private String resolveDisplayName(String fieldKey) {
        return fieldDefinitionRepository.findByFieldKey(fieldKey)
                .map(FieldDefinition::getDisplayName)
                .orElse(fieldKey);
    }

    private boolean isGadgetEligible(String fieldKey, String gadgetKey, UUID projectId) {
        return listEligibleForGadget(gadgetKey, projectId).stream()
                .anyMatch(e -> e.getFieldKey().equals(fieldKey));
    }
}
