package com.jira.issue.service;

import com.jira.issue.dto.ChangeHistoryResponse;
import com.jira.issue.dto.ChangeItemResponse;
import com.jira.issue.entity.ChangeGroup;
import com.jira.issue.entity.ChangeItem;
import com.jira.issue.exception.ResourceNotFoundException;
import com.jira.issue.repository.ChangeGroupRepository;
import com.jira.issue.repository.ChangeItemRepository;
import com.jira.issue.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChangeHistoryService {

    private final ChangeGroupRepository changeGroupRepository;
    private final ChangeItemRepository changeItemRepository;
    private final IssueRepository issueRepository;

    @Transactional
    public ChangeHistoryResponse recordChange(UUID issueId, UUID authorId, String authorName, List<ChangeItemResponse> changes) {
        if (!issueRepository.existsById(issueId)) {
            throw new ResourceNotFoundException("Issue not found: " + issueId);
        }

        ChangeGroup changeGroup = ChangeGroup.builder()
                .issueId(issueId)
                .authorId(authorId)
                .authorName(authorName)
                .build();

        changeGroup = changeGroupRepository.save(changeGroup);

        for (ChangeItemResponse change : changes) {
            ChangeItem changeItem = ChangeItem.builder()
                    .changeGroupId(changeGroup.getId())
                    .fieldType(change.getFieldType())
                    .field(change.getField())
                    .oldValue(change.getOldValue())
                    .oldString(change.getOldString())
                    .newValue(change.getNewValue())
                    .newString(change.getNewString())
                    .build();
            changeItemRepository.save(changeItem);
        }

        log.info("Recorded change history {} for issue {}", changeGroup.getId(), issueId);
        return getChangeHistory(changeGroup.getId());
    }

    @Transactional(readOnly = true)
    public List<ChangeHistoryResponse> getChangeHistoryByIssue(UUID issueId) {
        return changeGroupRepository.findByIssueIdOrderByCreatedAtDesc(issueId)
                .stream()
                .map(changeGroup -> getChangeHistory(changeGroup.getId()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChangeHistoryResponse getChangeHistory(UUID changeGroupId) {
        ChangeGroup changeGroup = changeGroupRepository.findById(changeGroupId)
                .orElseThrow(() -> new ResourceNotFoundException("Change history not found: " + changeGroupId));

        List<ChangeItemResponse> changes = changeItemRepository.findByChangeGroupIdOrderByCreatedAtAsc(changeGroupId)
                .stream()
                .map(this::toChangeItemResponse)
                .collect(Collectors.toList());

        return ChangeHistoryResponse.builder()
                .id(changeGroup.getId())
                .issueId(changeGroup.getIssueId())
                .authorId(changeGroup.getAuthorId())
                .authorName(changeGroup.getAuthorName())
                .createdAt(changeGroup.getCreatedAt())
                .changes(changes)
                .build();
    }

    @Transactional
    public void deleteChangeHistoryByIssue(UUID issueId) {
        List<ChangeGroup> changeGroups = changeGroupRepository.findByIssueIdOrderByCreatedAtDesc(issueId);
        List<UUID> groupIds = changeGroups.stream().map(ChangeGroup::getId).collect(Collectors.toList());

        if (!groupIds.isEmpty()) {
            changeItemRepository.deleteByChangeGroupIdIn(groupIds);
        }
        changeGroupRepository.deleteByIssueId(issueId);
        log.info("Deleted change history for issue {}", issueId);
    }

    public ChangeItemResponse createChangeItem(String field, String oldValue, String oldString, String newValue, String newString) {
        return ChangeItemResponse.builder()
                .fieldType("jira")
                .field(field)
                .oldValue(oldValue)
                .oldString(oldString)
                .newValue(newValue)
                .newString(newString)
                .build();
    }

    private ChangeItemResponse toChangeItemResponse(ChangeItem item) {
        return ChangeItemResponse.builder()
                .id(item.getId())
                .changeGroupId(item.getChangeGroupId())
                .fieldType(item.getFieldType())
                .field(item.getField())
                .oldValue(item.getOldValue())
                .oldString(item.getOldString())
                .newValue(item.getNewValue())
                .newString(item.getNewString())
                .createdAt(item.getCreatedAt())
                .build();
    }
}