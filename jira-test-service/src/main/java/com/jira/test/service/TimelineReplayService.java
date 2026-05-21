package com.jira.test.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.test.entity.ExecutionReplaySession;
import com.jira.test.entity.ExecutionTimelineEvent;
import com.jira.test.entity.TimelineSnapshot;
import com.jira.test.exception.InvalidOperationException;
import com.jira.test.exception.ResourceNotFoundException;
import com.jira.test.repository.ExecutionReplaySessionRepository;
import com.jira.test.repository.ExecutionTimelineEventRepository;
import com.jira.test.repository.TimelineSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimelineReplayService {

    private final ExecutionTimelineEventRepository eventRepository;
    private final ExecutionReplaySessionRepository sessionRepository;
    private final TimelineSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;

    private static final List<BigDecimal> SUPPORTED_SPEEDS = List.of(
            new BigDecimal("0.25"), new BigDecimal("0.5"),
            new BigDecimal("1.0"), new BigDecimal("2.0"),
            new BigDecimal("4.0"), new BigDecimal("8.0")
    );

    // ==================== EVENT RECORDING ====================

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

    @Transactional
    public void recordEvent(UUID executionId, String eventType, Integer stepIndex, String eventData,
                           String screenshotPath, List<String> logEntries, String entityType,
                           UUID entityId, String userId, Map<String, Object> beforeState,
                           Map<String, Object> afterState) {
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

        if (entityType != null) {
            event.setMetadata(serializeMap(Map.of(
                    "entityType", entityType,
                    "entityId", entityId != null ? entityId.toString() : null,
                    "userId", userId != null ? userId.toString() : null
            )));
        }

        eventRepository.save(event);
    }

    // ==================== TIMELINE RETRIEVAL ====================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTimeline(UUID executionId) {
        return eventRepository.findByExecutionIdOrderBySequenceOrderAsc(executionId).stream()
                .map(this::eventToMap)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFilteredTimeline(UUID executionId, String eventType,
                                                          UUID userId, LocalDateTime startDate,
                                                          LocalDateTime endDate, Integer stepIndex) {
        List<ExecutionTimelineEvent> events = eventRepository.findByExecutionIdOrderBySequenceOrderAsc(executionId);

        return events.stream()
                .filter(e -> eventType == null || eventType.equals(e.getEventType()))
                .filter(e -> stepIndex == null || Objects.equals(stepIndex, e.getStepIndex()))
                .filter(e -> {
                    if (startDate == null && endDate == null) return true;
                    if (startDate != null && e.getEventTimestamp().isBefore(startDate)) return false;
                    if (endDate != null && e.getEventTimestamp().isAfter(endDate)) return false;
                    return true;
                })
                .map(this::eventToMap)
                .collect(Collectors.toList());
    }

    // ==================== PLAYBACK SESSION MANAGEMENT ====================

    @Transactional
    public Map<String, Object> startReplay(UUID executionId, UUID userId, String name) {
        ExecutionReplaySession session = ExecutionReplaySession.builder()
                .executionId(executionId)
                .sessionStart(LocalDateTime.now())
                .createdBy(userId)
                .playbackSpeed(BigDecimal.ONE)
                .build();

        session = sessionRepository.save(session);

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", session.getId());
        result.put("name", name != null ? name : "Replay Session");
        result.put("playbackPositionMs", 0);
        result.put("isPlaying", false);
        result.put("playbackSpeed", 1.0);
        result.put("eventCount", eventRepository.findByExecutionIdOrderBySequenceOrderAsc(executionId).size());
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSession(UUID sessionId) {
        ExecutionReplaySession session = findSessionById(sessionId);

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", session.getId());
        result.put("executionId", session.getExecutionId());
        result.put("playbackPositionMs", session.getPlaybackPositionMs());
        result.put("isPlaying", session.getIsPlaying());
        result.put("playbackSpeed", session.getPlaybackSpeed().doubleValue());
        result.put("createdBy", session.getCreatedBy());
        result.put("sessionStart", session.getSessionStart());
        result.put("sessionEnd", session.getSessionEnd());
        result.put("status", session.getIsPlaying() ? "PLAYING" : (session.getSessionEnd() != null ? "COMPLETED" : "PAUSED"));
        result.put("eventCount", eventRepository.findByExecutionIdOrderBySequenceOrderAsc(session.getExecutionId()).size());

        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllSessions(UUID executionId) {
        List<ExecutionReplaySession> sessions = sessionRepository.findByExecutionId(executionId)
                .map(List::of)
                .orElse(List.of());

        return sessions.stream()
                .map(s -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("sessionId", s.getId());
                    map.put("playbackPositionMs", s.getPlaybackPositionMs());
                    map.put("isPlaying", s.getIsPlaying());
                    map.put("playbackSpeed", s.getPlaybackSpeed().doubleValue());
                    map.put("sessionStart", s.getSessionStart());
                    map.put("sessionEnd", s.getSessionEnd());
                    return map;
                })
                .collect(Collectors.toList());
    }

    // ==================== PLAYBACK CONTROLS ====================

    @Transactional
    public void updatePlaybackPosition(UUID sessionId, Integer positionMs) {
        ExecutionReplaySession session = findSessionById(sessionId);
        session.setPlaybackPositionMs(positionMs);
        sessionRepository.save(session);
    }

    @Transactional
    public void pauseReplay(UUID sessionId) {
        ExecutionReplaySession session = findSessionById(sessionId);
        session.setIsPlaying(false);
        sessionRepository.save(session);
    }

    @Transactional
    public void resumeReplay(UUID sessionId) {
        ExecutionReplaySession session = findSessionById(sessionId);
        session.setIsPlaying(true);
        sessionRepository.save(session);
    }

    @Transactional
    public void stopReplay(UUID sessionId) {
        ExecutionReplaySession session = findSessionById(sessionId);
        session.setIsPlaying(false);
        session.setSessionEnd(LocalDateTime.now());
        sessionRepository.save(session);
    }

    @Transactional
    public Map<String, Object> seekTo(UUID sessionId, Integer positionMs) {
        ExecutionReplaySession session = findSessionById(sessionId);

        List<ExecutionTimelineEvent> events = eventRepository.findByExecutionIdOrderBySequenceOrderAsc(
                session.getExecutionId());

        Integer eventIndex = findEventIndexAtPosition(events, positionMs);
        session.setPlaybackPositionMs(positionMs);
        sessionRepository.save(session);

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("positionMs", positionMs);
        result.put("eventIndex", eventIndex);
        result.put("totalEvents", events.size());

        return result;
    }

    @Transactional
    public Map<String, Object> setSpeed(UUID sessionId, BigDecimal speed) {
        if (!SUPPORTED_SPEEDS.contains(speed)) {
            throw new InvalidOperationException("Unsupported playback speed: " + speed +
                    ". Supported speeds: 0.25x, 0.5x, 1x, 2x, 4x, 8x");
        }

        ExecutionReplaySession session = findSessionById(sessionId);
        session.setPlaybackSpeed(speed);
        sessionRepository.save(session);

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("playbackSpeed", speed.doubleValue());
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPlaybackSpeed(UUID sessionId) {
        ExecutionReplaySession session = findSessionById(sessionId);
        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("playbackSpeed", session.getPlaybackSpeed().doubleValue());
        result.put("supportedSpeeds", List.of(0.25, 0.5, 1.0, 2.0, 4.0, 8.0));
        return result;
    }

    // ==================== SNAPSHOT MANAGEMENT ====================

    @Transactional
    public Map<String, Object> createSnapshot(UUID sessionId, UUID userId, String name, String description) {
        ExecutionReplaySession session = findSessionById(sessionId);

        List<ExecutionTimelineEvent> events = eventRepository.findByExecutionIdOrderBySequenceOrderAsc(
                session.getExecutionId());

        int currentIndex = findEventIndexAtPosition(events, session.getPlaybackPositionMs());
        List<Map<String, Object>> eventSnapshot = events.subList(0, Math.min(currentIndex + 1, events.size()))
                .stream()
                .map(this::eventToMap)
                .collect(Collectors.toList());

        TimelineSnapshot snapshot = TimelineSnapshot.builder()
                .sessionId(sessionId)
                .executionId(session.getExecutionId())
                .name(name != null ? name : "Snapshot at " + LocalDateTime.now())
                .description(description)
                .eventIndex(currentIndex)
                .positionMs(session.getPlaybackPositionMs())
                .playbackSpeed(session.getPlaybackSpeed())
                .snapshotData(serializeList(eventSnapshot))
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();

        snapshot = snapshotRepository.save(snapshot);

        Map<String, Object> result = new HashMap<>();
        result.put("snapshotId", snapshot.getId());
        result.put("name", snapshot.getName());
        result.put("description", snapshot.getDescription());
        result.put("eventIndex", snapshot.getEventIndex());
        result.put("positionMs", snapshot.getPositionMs());
        result.put("createdAt", snapshot.getCreatedAt());
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSnapshot(UUID snapshotId) {
        TimelineSnapshot snapshot = snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new ResourceNotFoundException("TimelineSnapshot", "id", snapshotId));

        Map<String, Object> result = new HashMap<>();
        result.put("snapshotId", snapshot.getId());
        result.put("sessionId", snapshot.getSessionId());
        result.put("executionId", snapshot.getExecutionId());
        result.put("name", snapshot.getName());
        result.put("description", snapshot.getDescription());
        result.put("eventIndex", snapshot.getEventIndex());
        result.put("positionMs", snapshot.getPositionMs());
        result.put("playbackSpeed", snapshot.getPlaybackSpeed().doubleValue());
        result.put("createdAt", snapshot.getCreatedAt());
        result.put("createdBy", snapshot.getCreatedBy());
        result.put("events", parseList(snapshot.getSnapshotData()));

        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSnapshotsForSession(UUID sessionId) {
        List<TimelineSnapshot> snapshots = snapshotRepository.findBySessionIdOrderByCreatedAtDesc(sessionId);

        return snapshots.stream()
                .map(s -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("snapshotId", s.getId());
                    map.put("name", s.getName());
                    map.put("description", s.getDescription());
                    map.put("eventIndex", s.getEventIndex());
                    map.put("positionMs", s.getPositionMs());
                    map.put("createdAt", s.getCreatedAt());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSnapshotsForExecution(UUID executionId) {
        List<TimelineSnapshot> snapshots = snapshotRepository.findByExecutionIdOrderByCreatedAtDesc(executionId);

        return snapshots.stream()
                .map(s -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("snapshotId", s.getId());
                    map.put("sessionId", s.getSessionId());
                    map.put("name", s.getName());
                    map.put("eventIndex", s.getEventIndex());
                    map.put("positionMs", s.getPositionMs());
                    map.put("createdAt", s.getCreatedAt());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteSnapshot(UUID snapshotId) {
        TimelineSnapshot snapshot = snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new ResourceNotFoundException("TimelineSnapshot", "id", snapshotId));
        snapshotRepository.delete(snapshot);
    }

    @Transactional
    public Map<String, Object> restoreFromSnapshot(UUID snapshotId) {
        TimelineSnapshot snapshot = snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new ResourceNotFoundException("TimelineSnapshot", "id", snapshotId));

        ExecutionReplaySession session = findSessionById(snapshot.getSessionId());
        session.setPlaybackPositionMs(snapshot.getPositionMs());
        session.setPlaybackSpeed(snapshot.getPlaybackSpeed());
        session.setIsPlaying(false);
        sessionRepository.save(session);

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", session.getId());
        result.put("positionMs", snapshot.getPositionMs());
        result.put("eventIndex", snapshot.getEventIndex());
        return result;
    }

    // ==================== TIMELINE VISUALIZATION ====================

    @Transactional(readOnly = true)
    public Map<String, Object> getPlaybackTimelineData(UUID sessionId) {
        ExecutionReplaySession session = findSessionById(sessionId);
        List<ExecutionTimelineEvent> events = eventRepository.findByExecutionIdOrderBySequenceOrderAsc(
                session.getExecutionId());

        List<Map<String, Object>> eventList = events.stream()
                .map(this::eventToMap)
                .collect(Collectors.toList());

        Map<String, Object> timelineData = new HashMap<>();
        timelineData.put("sessionId", session.getId());
        timelineData.put("executionId", session.getExecutionId());
        timelineData.put("currentPositionMs", session.getPlaybackPositionMs());
        timelineData.put("isPlaying", session.getIsPlaying());
        timelineData.put("playbackSpeed", session.getPlaybackSpeed().doubleValue());
        timelineData.put("totalEvents", events.size());
        timelineData.put("totalDurationMs", calculateTotalDuration(events));
        timelineData.put("events", eventList);

        // Generate timeline markers for visualization
        timelineData.put("markers", generateTimelineMarkers(events));
        timelineData.put("eventTypes", getEventTypeSummary(events));
        timelineData.put("stepBreaks", getStepBreaks(events));

        return timelineData;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTimelineSummary(UUID executionId) {
        List<ExecutionTimelineEvent> events = eventRepository.findByExecutionIdOrderBySequenceOrderAsc(executionId);

        Map<String, Object> summary = new HashMap<>();
        summary.put("executionId", executionId);
        summary.put("totalEvents", events.size());
        summary.put("totalDurationMs", calculateTotalDuration(events));
        summary.put("eventTypes", getEventTypeSummary(events));

        if (!events.isEmpty()) {
            ExecutionTimelineEvent first = events.get(0);
            ExecutionTimelineEvent last = events.get(events.size() - 1);
            summary.put("startTime", first.getEventTimestamp());
            summary.put("endTime", last.getEventTimestamp());
            summary.put("durationSeconds", Duration.between(first.getEventTimestamp(), last.getEventTimestamp()).toSeconds());
        }

        return summary;
    }

    // ==================== SESSION PERSISTENCE ====================

    @Transactional
    public Map<String, Object> saveSessionState(UUID sessionId) {
        ExecutionReplaySession session = findSessionById(sessionId);
        Map<String, Object> state = new HashMap<>();
        state.put("sessionId", session.getId());
        state.put("executionId", session.getExecutionId());
        state.put("playbackPositionMs", session.getPlaybackPositionMs());
        state.put("playbackSpeed", session.getPlaybackSpeed().doubleValue());
        state.put("isPlaying", session.getIsPlaying());
        state.put("sessionStart", session.getSessionStart());
        state.put("sessionEnd", session.getSessionEnd());
        return state;
    }

    @Transactional
    public Map<String, Object> restoreSession(UUID sessionId) {
        ExecutionReplaySession session = findSessionById(sessionId);
        session.setSessionEnd(null);
        session.setIsPlaying(false);
        sessionRepository.save(session);

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", session.getId());
        result.put("restored", true);
        result.put("positionMs", session.getPlaybackPositionMs());
        return result;
    }

    // ==================== HELPER METHODS ====================

    private ExecutionReplaySession findSessionById(UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("ExecutionReplaySession", "id", sessionId));
    }

    private Map<String, Object> eventToMap(ExecutionTimelineEvent e) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", e.getId());
        map.put("eventType", e.getEventType());
        map.put("eventTimestamp", e.getEventTimestamp());
        map.put("stepIndex", e.getStepIndex());
        map.put("screenshotPath", e.getScreenshotPath());
        map.put("logEntries", parseList(e.getLogEntries()));
        map.put("eventData", e.getEventData());
        map.put("sequenceOrder", e.getSequenceOrder());
        map.put("metadata", e.getMetadata() != null ? parseMap(e.getMetadata()) : null);
        return map;
    }

    private int findEventIndexAtPosition(List<ExecutionTimelineEvent> events, Integer positionMs) {
        if (events.isEmpty() || positionMs == null || positionMs <= 0) {
            return 0;
        }

        // Calculate estimated position based on average event duration
        int avgEventDuration = 1000; // 1 second per event as default
        int index = positionMs / avgEventDuration;
        return Math.min(Math.max(0, index), events.size() - 1);
    }

    private long calculateTotalDuration(List<ExecutionTimelineEvent> events) {
        if (events == null || events.size() < 2) {
            return 0;
        }
        ExecutionTimelineEvent first = events.get(0);
        ExecutionTimelineEvent last = events.get(events.size() - 1);
        return Duration.between(first.getEventTimestamp(), last.getEventTimestamp()).toMillis();
    }

    private List<Map<String, Object>> generateTimelineMarkers(List<ExecutionTimelineEvent> events) {
        List<Map<String, Object>> markers = new ArrayList<>();
        long baseTime = events.isEmpty() ? System.currentTimeMillis() :
                events.get(0).getEventTimestamp().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();

        for (int i = 0; i < events.size(); i++) {
            ExecutionTimelineEvent e = events.get(i);
            long markerTime = e.getEventTimestamp().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();

            Map<String, Object> marker = new HashMap<>();
            marker.put("index", i);
            marker.put("time", markerTime - baseTime);
            marker.put("type", e.getEventType());
            marker.put("stepIndex", e.getStepIndex());
            markers.add(marker);
        }

        return markers;
    }

    private Map<String, Integer> getEventTypeSummary(List<ExecutionTimelineEvent> events) {
        Map<String, Integer> summary = new HashMap<>();
        for (ExecutionTimelineEvent e : events) {
            summary.merge(e.getEventType(), 1, Integer::sum);
        }
        return summary;
    }

    private List<Integer> getStepBreaks(List<ExecutionTimelineEvent> events) {
        Set<Integer> steps = new LinkedHashSet<>();
        for (ExecutionTimelineEvent e : events) {
            if (e.getStepIndex() != null) {
                steps.add(e.getStepIndex());
            }
        }
        return new ArrayList<>(steps);
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMap(String json) {
        if (json == null) return new HashMap<>();
        try { return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {}); }
        catch (JsonProcessingException e) { return new HashMap<>(); }
    }

    private String serializeMap(Map<String, Object> map) {
        try { return objectMapper.writeValueAsString(map); }
        catch (JsonProcessingException e) { return "{}"; }
    }
}