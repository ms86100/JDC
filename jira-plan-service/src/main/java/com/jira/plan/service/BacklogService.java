package com.jira.plan.service;

import com.jira.plan.dto.request.CreatePlanItemRequest;
import com.jira.plan.dto.request.ReorderRequest;
import com.jira.plan.dto.response.BacklogResponse;
import com.jira.plan.dto.response.PlanItemResponse;
import com.jira.plan.entity.Plan;
import com.jira.plan.entity.PlanItem;
import com.jira.plan.exception.ResourceNotFoundException;
import com.jira.plan.repository.PlanItemRepository;
import com.jira.plan.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BacklogService {

    private final PlanItemRepository planItemRepository;
    private final PlanRepository planRepository;

    @Transactional(readOnly = true)
    public BacklogResponse getBacklog(UUID planId) {
        Plan plan = findPlanById(planId);
        List<PlanItem> items = planItemRepository.findByPlanIdOrderBySortOrder(planId);

        List<PlanItemResponse> itemResponses = items.stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());

        int epicCount = (int) items.stream().filter(i -> "EPIC".equals(i.getIssueType())).count();
        int storyCount = (int) items.stream().filter(i -> "STORY".equals(i.getIssueType())).count();
        int subtaskCount = (int) items.stream().filter(i -> "SUBTASK".equals(i.getIssueType())).count();

        return BacklogResponse.builder()
                .planId(planId)
                .planName(plan.getName())
                .totalItems(items.size())
                .epicCount(epicCount)
                .storyCount(storyCount)
                .subtaskCount(subtaskCount)
                .items(itemResponses)
                .build();
    }

    @Transactional
    public PlanItemResponse addItemToBacklog(UUID planId, CreatePlanItemRequest request) {
        Plan plan = findPlanById(planId);

        String sortOrder = request.getSortOrder();
        if (sortOrder == null) {
            sortOrder = generateSortOrder(planId, request.getParentId());
        }

        PlanItem item = PlanItem.builder()
                .planId(planId)
                .issueId(request.getIssueId())
                .issueType(request.getIssueType())
                .parentId(request.getParentId())
                .sortOrder(sortOrder)
                .targetDate(request.getTargetDate())
                .status(request.getStatus() != null ? request.getStatus() : "BACKLOG")
                .build();

        item = planItemRepository.save(item);
        return toItemResponse(item);
    }

    @Transactional
    public PlanItemResponse updateItem(UUID planId, UUID itemId, CreatePlanItemRequest request) {
        PlanItem item = findItemById(itemId);
        // IDOR check: verify item belongs to specified plan
        if (!item.getPlanId().equals(planId)) {
            throw new ResourceNotFoundException("PlanItem", "id", itemId);
        }

        if (request.getParentId() != null) {
            item.setParentId(request.getParentId());
        }
        if (request.getTargetDate() != null) {
            item.setTargetDate(request.getTargetDate());
        }
        if (request.getStatus() != null) {
            item.setStatus(request.getStatus());
        }

        item = planItemRepository.save(item);
        return toItemResponse(item);
    }

    @Transactional
    public void removeItemFromBacklog(UUID planId, UUID itemId) {
        PlanItem item = findItemById(itemId);
        // IDOR check: verify item belongs to specified plan
        if (!item.getPlanId().equals(planId)) {
            throw new ResourceNotFoundException("PlanItem", "id", itemId);
        }
        planItemRepository.delete(item);
    }

    @Transactional
    public void reorderItems(UUID planId, ReorderRequest request) {
        if (request.getItemId() != null && request.getNewSortOrder() != null) {
            PlanItem item = findItemById(request.getItemId());
            // IDOR check: verify item belongs to specified plan
            if (!item.getPlanId().equals(planId)) {
                throw new ResourceNotFoundException("PlanItem", "id", request.getItemId());
            }
            item.setSortOrder(request.getNewSortOrder());
            if (request.getNewParentId() != null) {
                item.setParentId(request.getNewParentId());
            }
            planItemRepository.save(item);
        }
    }

    private Plan findPlanById(UUID id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", id));
    }

    private PlanItem findItemById(UUID id) {
        return planItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PlanItem", "id", id));
    }

    private String generateSortOrder(UUID planId, UUID parentId) {
        List<PlanItem> siblings = parentId != null
                ? planItemRepository.findByPlanIdAndParentId(planId, parentId)
                : planItemRepository.findByPlanIdAndParentIdIsNull(planId);

        if (siblings.isEmpty()) {
            return "a0";
        }

        String lastSortOrder = siblings.stream()
                .map(PlanItem::getSortOrder)
                .max(String::compareTo)
                .orElse("a0");

        return incrementSortOrder(lastSortOrder);
    }

    private String incrementSortOrder(String sortOrder) {
        if (sortOrder.isEmpty()) {
            return "a0";
        }
        char first = sortOrder.charAt(0);
        if (first < 'z') {
            return (char) (first + 1) + "0";
        }
        return sortOrder + "0";
    }

    private PlanItemResponse toItemResponse(PlanItem item) {
        return PlanItemResponse.builder()
                .id(item.getId())
                .planId(item.getPlanId())
                .issueId(item.getIssueId())
                .issueType(item.getIssueType())
                .parentId(item.getParentId())
                .sortOrder(item.getSortOrder())
                .targetDate(item.getTargetDate())
                .status(item.getStatus())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
