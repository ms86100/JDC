package com.jira.board.service;

import com.jira.board.entity.*;
import com.jira.board.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardConfigurationService {

    private final BoardAdministratorRepository adminRepository;
    private final BoardSwimlaneRepository swimlaneRepository;
    private final BoardCardColorRuleRepository cardColorRuleRepository;
    private final BoardCardFieldRepository cardFieldRepository;
    private final BoardIssueDetailFieldRepository issueDetailFieldRepository;
    private final BoardCFDSnapshotRepository cfdSnapshotRepository;
    private final FilterSubscriptionRepository filterSubscriptionRepository;

    // === Board Administrators ===

    @Transactional(readOnly = true)
    public List<BoardAdministrator> getAdministrators(UUID boardId) {
        return adminRepository.findByBoardId(boardId);
    }

    @Transactional
    public BoardAdministrator addAdministrator(UUID boardId, UUID holderId, String holderType) {
        if (adminRepository.existsByBoardIdAndHolderId(boardId, holderId)) {
            throw new IllegalArgumentException("User/group is already a board administrator");
        }
        return adminRepository.save(BoardAdministrator.builder()
                .boardId(boardId).holderId(holderId)
                .holderType(holderType != null ? holderType : "USER")
                .build());
    }

    @Transactional
    public void removeAdministrator(UUID boardId, UUID holderId) {
        adminRepository.deleteByBoardIdAndHolderId(boardId, holderId);
    }

    @Transactional
    public boolean isAdministrator(UUID boardId, UUID userId) {
        return adminRepository.existsByBoardIdAndHolderId(boardId, userId);
    }

    // === Swimlanes ===

    @Transactional(readOnly = true)
    public List<BoardSwimlane> getSwimlanes(UUID boardId) {
        return swimlaneRepository.findByBoardIdOrderByPositionAsc(boardId);
    }

    @Transactional
    public BoardSwimlane createSwimlane(UUID boardId, BoardSwimlane swimlane) {
        swimlane.setBoardId(boardId);
        return swimlaneRepository.save(swimlane);
    }

    @Transactional
    public BoardSwimlane updateSwimlane(UUID swimlaneId, BoardSwimlane update) {
        BoardSwimlane existing = swimlaneRepository.findById(swimlaneId)
                .orElseThrow(() -> new RuntimeException("Swimlane not found: " + swimlaneId));
        if (update.getName() != null) existing.setName(update.getName());
        if (update.getJqlQuery() != null) existing.setJqlQuery(update.getJqlQuery());
        if (update.getDescription() != null) existing.setDescription(update.getDescription());
        if (update.getPosition() != null) existing.setPosition(update.getPosition());
        return swimlaneRepository.save(existing);
    }

    @Transactional
    public void deleteSwimlane(UUID swimlaneId) {
        swimlaneRepository.deleteById(swimlaneId);
    }

    @Transactional
    public void deleteAllSwimlanes(UUID boardId) {
        swimlaneRepository.deleteAllByBoardId(boardId);
    }

    // === Card Color Rules ===

    @Transactional(readOnly = true)
    public List<BoardCardColorRule> getCardColorRules(UUID boardId) {
        return cardColorRuleRepository.findByBoardIdOrderByPositionAsc(boardId);
    }

    @Transactional
    public BoardCardColorRule createCardColorRule(UUID boardId, BoardCardColorRule rule) {
        rule.setBoardId(boardId);
        return cardColorRuleRepository.save(rule);
    }

    @Transactional
    public BoardCardColorRule updateCardColorRule(UUID ruleId, BoardCardColorRule update) {
        BoardCardColorRule existing = cardColorRuleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Card color rule not found: " + ruleId));
        if (update.getColorMethod() != null) existing.setColorMethod(update.getColorMethod());
        if (update.getMatchValue() != null) existing.setMatchValue(update.getMatchValue());
        if (update.getColor() != null) existing.setColor(update.getColor());
        if (update.getPosition() != null) existing.setPosition(update.getPosition());
        return cardColorRuleRepository.save(existing);
    }

    @Transactional
    public void deleteCardColorRule(UUID ruleId) {
        cardColorRuleRepository.deleteById(ruleId);
    }

    @Transactional
    public void deleteAllCardColorRules(UUID boardId) {
        cardColorRuleRepository.deleteAllByBoardId(boardId);
    }

    // === Card Fields (max 3 per board) ===

    @Transactional(readOnly = true)
    public List<BoardCardField> getCardFields(UUID boardId) {
        return cardFieldRepository.findByBoardIdOrderByPositionAsc(boardId);
    }

    @Transactional
    public BoardCardField addCardField(UUID boardId, BoardCardField field) {
        long count = cardFieldRepository.countByBoardId(boardId);
        if (count >= 3) {
            throw new IllegalArgumentException("Maximum 3 card fields per board");
        }
        field.setBoardId(boardId);
        if (field.getPosition() == null) {
            field.setPosition((int) count + 1);
        }
        return cardFieldRepository.save(field);
    }

    @Transactional
    public void removeCardField(UUID fieldId) {
        cardFieldRepository.deleteById(fieldId);
    }

    @Transactional
    public void replaceCardFields(UUID boardId, List<BoardCardField> fields) {
        if (fields.size() > 3) {
            throw new IllegalArgumentException("Maximum 3 card fields per board");
        }
        cardFieldRepository.deleteAllByBoardId(boardId);
        for (int i = 0; i < fields.size(); i++) {
            fields.get(i).setBoardId(boardId);
            fields.get(i).setPosition(i + 1);
            cardFieldRepository.save(fields.get(i));
        }
    }

    // === Issue Detail View Fields ===

    @Transactional(readOnly = true)
    public List<BoardIssueDetailField> getIssueDetailFields(UUID boardId) {
        return issueDetailFieldRepository.findByBoardIdOrderByPositionAsc(boardId);
    }

    @Transactional
    public BoardIssueDetailField addIssueDetailField(UUID boardId, BoardIssueDetailField field) {
        field.setBoardId(boardId);
        return issueDetailFieldRepository.save(field);
    }

    @Transactional
    public void removeIssueDetailField(UUID fieldId) {
        issueDetailFieldRepository.deleteById(fieldId);
    }

    @Transactional
    public void replaceIssueDetailFields(UUID boardId, List<BoardIssueDetailField> fields) {
        issueDetailFieldRepository.deleteAllByBoardId(boardId);
        for (int i = 0; i < fields.size(); i++) {
            fields.get(i).setBoardId(boardId);
            fields.get(i).setPosition(i);
            issueDetailFieldRepository.save(fields.get(i));
        }
    }

    // === CFD Snapshots ===

    @Transactional(readOnly = true)
    public List<BoardCFDSnapshot> getCFDData(UUID boardId, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return cfdSnapshotRepository.findByBoardIdAndDateRange(boardId, startDate, endDate);
    }

    @Transactional
    public void saveCFDSnapshot(BoardCFDSnapshot snapshot) {
        cfdSnapshotRepository.save(snapshot);
    }

    // === Filter Subscriptions ===

    @Transactional(readOnly = true)
    public List<FilterSubscription> getFilterSubscriptions(UUID filterId) {
        return filterSubscriptionRepository.findByFilterId(filterId);
    }

    @Transactional(readOnly = true)
    public List<FilterSubscription> getUserSubscriptions(UUID userId) {
        return filterSubscriptionRepository.findByUserId(userId);
    }

    @Transactional
    public FilterSubscription subscribe(UUID filterId, UUID userId, String frequency, String emailAddress) {
        FilterSubscription sub = FilterSubscription.builder()
                .filterId(filterId)
                .userId(userId)
                .frequency(frequency != null ? frequency : "DAILY")
                .emailAddress(emailAddress)
                .isEnabled(true)
                .nextRunAt(calculateNextRunAt(frequency))
                .build();
        return filterSubscriptionRepository.save(sub);
    }

    @Transactional
    public void unsubscribe(UUID subscriptionId) {
        filterSubscriptionRepository.deleteById(subscriptionId);
    }

    @Transactional
    public FilterSubscription toggleSubscription(UUID subscriptionId, boolean enabled) {
        FilterSubscription sub = filterSubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));
        sub.setIsEnabled(enabled);
        if (enabled) {
            sub.setNextRunAt(calculateNextRunAt(sub.getFrequency()));
        }
        return filterSubscriptionRepository.save(sub);
    }

    @Transactional(readOnly = true)
    public List<FilterSubscription> getDueSubscriptions() {
        return filterSubscriptionRepository.findDueSubscriptions(java.time.LocalDateTime.now());
    }

    private java.time.LocalDateTime calculateNextRunAt(String frequency) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return switch (frequency != null ? frequency.toUpperCase() : "DAILY") {
            case "HOURLY" -> now.plusHours(1);
            case "DAILY" -> now.plusDays(1).withHour(8).withMinute(0);
            case "WEEKLY" -> now.plusWeeks(1).withHour(8).withMinute(0);
            case "MONTHLY" -> now.plusMonths(1).withDayOfMonth(1).withHour(8).withMinute(0);
            default -> now.plusDays(1);
        };
    }
}
