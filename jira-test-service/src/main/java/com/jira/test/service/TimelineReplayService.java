package com.jira.test.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.test.entity.*;
import com.jira.test.exception.ResourceNotFoundException;
import com.jira.test.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimelineReplayService {

    private final ExecutionTimelineEventRepository eventRepository;
    private final ExecutionReplaySessionRepository sessionRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void recordEvent(UUID executionId, String eventType, Integer stepIndex, String eventData,
                           String screenshotPath, List<String> logEntries) {
        int sequenceOrder = eventRepository.findByExecutionIdOrderBySequenceOrderAsc(executionId).size() + 1;

        ExecutionTimelineEvent event = ExecutionTimelineEvent.builder()
                .executionId(executionId)
                .eventType(eventType)
                .eventTimestamp(LocalDateTime.now())
                .stepIndex(stepIndex)
                .eventData(eventData)
                .screenshotPath(screenshotPath)
                .logEntries(serializeList(logEntries))
                .sequenceOrder(sequenceOrder)
                .build();

        eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTimeline(UUID executionId) {
        return eventRepository.findByExecutionIdOrderBySequenceOrderAsc(executionId).stream()
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", e.getId());
                    map.put("eventType", e.getEventType());
                    map.put("eventTimestamp", e.getEventTimestamp());
                    map.put("stepIndex", e.getStepIndex());
                    map.put("screenshotPath", e.getScreenshotPath());
                    map.put("logEntries", parseList(e.getLogEntries()));
                    map.put("eventData", e.getEventData());
                    return map;
                }).toList();
    }

    @Transactional
    public Map<String, Object> startReplay(UUID executionId, UUID userId) {
        ExecutionReplaySession session = ExecutionReplaySession.builder()
                .executionId(executionId)
                .sessionStart(LocalDateTime.now())
                .createdBy(userId)
                .playbackSpeed(BigDecimal.ONE)
                .build();

        session = sessionRepository.save(session);

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", session.getId());
        result.put("playbackPositionMs", 0);
        result.put("isPlaying", false);
        return result;
    }

    @Transactional
    public void updatePlaybackPosition(UUID sessionId, Integer positionMs) {
        ExecutionReplaySession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        session.setPlaybackPositionMs(positionMs);
        sessionRepository.save(session);
    }

    @Transactional
    public void pauseReplay(UUID sessionId) {
        ExecutionReplaySession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        session.setIsPlaying(false);
        sessionRepository.save(session);
    }

    @Transactional
    public void resumeReplay(UUID sessionId) {
        ExecutionReplaySession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        session.setIsPlaying(true);
        sessionRepository.save(session);
    }

    private String serializeList(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (JsonProcessingException e) { return "[]"; }
    }

    private List<String> parseList(String json) {
        if (json == null) return new ArrayList<>();
        try { return objectMapper.readValue(json, new TypeReference<List<String>>() {}); }
        catch (JsonProcessingException e) { return new ArrayList<>(); }
    }
}