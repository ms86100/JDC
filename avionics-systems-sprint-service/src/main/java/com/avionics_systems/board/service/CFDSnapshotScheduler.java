package com.avionics_systems.board.service;

import com.avionics_systems.board.entity.AgileBoard;
import com.avionics_systems.board.entity.BoardCFDSnapshot;
import com.avionics_systems.board.entity.BoardColumn;
import com.avionics_systems.board.repository.AgileBoardRepository;
import com.avionics_systems.board.repository.BoardCFDSnapshotRepository;
import com.avionics_systems.board.repository.BoardColumnRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CFDSnapshotScheduler {

    private final AgileBoardRepository boardRepository;
    private final BoardColumnRepository columnRepository;
    private final BoardCFDSnapshotRepository snapshotRepository;

    @Value("${issue.service.url:http://avionics-systems-issue-service:8084}")
    private String issueServiceUrl;

    @Value("${cfd.snapshot.enabled:true}")
    private boolean snapshotEnabled;

    private final RestTemplate restTemplate;

    @Scheduled(cron = "${cfd.snapshot.cron:0 0 2 * * *}")
    @SchedulerLock(name = "CFDSnapshotScheduler_captureSnapshots", lockAtMostFor = "PT30M", lockAtLeastFor = "PT5M")
    @Transactional
    public void captureSnapshots() {
        if (!snapshotEnabled) return;
        log.info("Starting daily CFD snapshot capture");

        List<AgileBoard> boards = boardRepository.findAll();
        LocalDate today = LocalDate.now();
        int captured = 0;

        for (AgileBoard board : boards) {
            try {
                List<BoardColumn> columns = columnRepository.findByBoardIdOrderBySequenceAsc(board.getId());
                for (BoardColumn column : columns) {
                    int issueCount = countIssuesInColumn(board.getProjectId(), column);
                    snapshotRepository.save(BoardCFDSnapshot.builder()
                            .boardId(board.getId())
                            .snapshotDate(today)
                            .columnId(column.getId())
                            .columnName(column.getName())
                            .statusCategory(column.getStatusCategory())
                            .issueCount(issueCount)
                            .build());
                    captured++;
                }
            } catch (Exception e) {
                log.warn("Failed to capture CFD snapshot for board {}: {}", board.getId(), e.getMessage());
            }
        }

        log.info("CFD snapshot capture complete: {} data points across {} boards", captured, boards.size());
    }

    @SuppressWarnings("unchecked")
    private int countIssuesInColumn(UUID projectId, BoardColumn column) {
        try {
            String url = String.format("%s/api/issues?projectId=%s&statusCategory=%s&size=1",
                    issueServiceUrl, projectId, column.getStatusCategory());
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.get("totalElements") != null) {
                return ((Number) response.get("totalElements")).intValue();
            }
        } catch (Exception e) {
            log.debug("Could not count issues for column {}: {}", column.getName(), e.getMessage());
        }
        return 0;
    }
}
