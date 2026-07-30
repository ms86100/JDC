package com.avionics_systems.test.controller;

import com.avionics_systems.test.dto.*;
import com.avionics_systems.test.service.TimelineReplayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/timeline")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Timeline & Replay", description = "Timeline event recording and playback management")
public class TimelineController {

    private final TimelineReplayService timelineService;

    // ==================== SESSION ENDPOINTS ====================

    @PostMapping("/sessions")
    @Operation(summary = "Start new replay session", description = "Creates a new playback session for timeline replay")
    public ResponseEntity<PlaybackSessionResponse> startSession(
            @RequestParam UUID executionId,
            @RequestParam(required = false) UUID userId,
            @RequestBody(required = false) CreateSessionRequest request) {

        log.info("Starting replay session for execution: {}", executionId);
        Map<String, Object> result = timelineService.startReplay(
                executionId, userId, request != null ? request.getName() : null);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PlaybackSessionResponse.builder()
                        .sessionId((UUID) result.get("sessionId"))
                        .executionId(executionId)
                        .name((String) result.get("name"))
                        .playbackPositionMs((Integer) result.get("playbackPositionMs"))
                        .isPlaying((Boolean) result.get("isPlaying"))
                        .playbackSpeed((Double) result.get("playbackSpeed"))
                        .status("PAUSED")
                        .eventCount((Integer) result.get("eventCount"))
                        .build());
    }

    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "Get session details", description = "Retrieves details of a specific replay session")
    public ResponseEntity<PlaybackSessionResponse> getSession(
            @PathVariable UUID sessionId) {

        log.info("Getting session details for: {}", sessionId);
        Map<String, Object> result = timelineService.getSession(sessionId);

        return ResponseEntity.ok(PlaybackSessionResponse.builder()
                .sessionId(sessionId)
                .executionId((UUID) result.get("executionId"))
                .playbackPositionMs((Integer) result.get("playbackPositionMs"))
                .isPlaying((Boolean) result.get("isPlaying"))
                .playbackSpeed((Double) result.get("playbackSpeed"))
                .createdBy((UUID) result.get("createdBy"))
                .sessionStart((LocalDateTime) result.get("sessionStart"))
                .sessionEnd((LocalDateTime) result.get("sessionEnd"))
                .status((String) result.get("status"))
                .eventCount((Integer) result.get("eventCount"))
                .build());
    }

    @GetMapping("/sessions/execution/{executionId}")
    @Operation(summary = "Get all sessions for execution", description = "Lists all replay sessions for a specific execution")
    public ResponseEntity<List<Map<String, Object>>> getSessionsByExecution(
            @PathVariable UUID executionId) {

        log.info("Getting sessions for execution: {}", executionId);
        return ResponseEntity.ok(timelineService.getAllSessions(executionId));
    }

    @PostMapping("/sessions/{sessionId}/play")
    @Operation(summary = "Resume playback", description = "Resumes a paused playback session")
    public ResponseEntity<Map<String, Object>> playSession(
            @PathVariable UUID sessionId) {

        log.info("Resuming playback for session: {}", sessionId);
        timelineService.resumeReplay(sessionId);
        return ResponseEntity.ok(Map.of("sessionId", sessionId, "isPlaying", true));
    }

    @PostMapping("/sessions/{sessionId}/pause")
    @Operation(summary = "Pause playback", description = "Pauses an active playback session")
    public ResponseEntity<Map<String, Object>> pauseSession(
            @PathVariable UUID sessionId) {

        log.info("Pausing playback for session: {}", sessionId);
        timelineService.pauseReplay(sessionId);
        return ResponseEntity.ok(Map.of("sessionId", sessionId, "isPlaying", false));
    }

    @PostMapping("/sessions/{sessionId}/stop")
    @Operation(summary = "Stop playback", description = "Stops a playback session and marks it as completed")
    public ResponseEntity<Map<String, Object>> stopSession(
            @PathVariable UUID sessionId) {

        log.info("Stopping playback for session: {}", sessionId);
        timelineService.stopReplay(sessionId);
        return ResponseEntity.ok(Map.of("sessionId", sessionId, "status", "COMPLETED"));
    }

    @PostMapping("/sessions/{sessionId}/seek")
    @Operation(summary = "Seek to position", description = "Seeks to a specific position in the timeline")
    public ResponseEntity<Map<String, Object>> seekTo(
            @PathVariable UUID sessionId,
            @RequestBody SeekRequest request) {

        log.info("Seeking session {} to position {} ms", sessionId, request.getPositionMs());
        return ResponseEntity.ok(timelineService.seekTo(sessionId, request.getPositionMs()));
    }

    @PutMapping("/sessions/{sessionId}/speed")
    @Operation(summary = "Set playback speed", description = "Changes the playback speed (0.25x, 0.5x, 1x, 2x, 4x, 8x)")
    public ResponseEntity<Map<String, Object>> setSpeed(
            @PathVariable UUID sessionId,
            @RequestBody SetSpeedRequest request) {

        log.info("Setting speed {} for session: {}", request.getSpeed(), sessionId);
        return ResponseEntity.ok(timelineService.setSpeed(sessionId, request.getSpeed()));
    }

    @GetMapping("/sessions/{sessionId}/speed")
    @Operation(summary = "Get playback speed", description = "Gets the current playback speed and supported speeds")
    public ResponseEntity<Map<String, Object>> getSpeed(
            @PathVariable UUID sessionId) {

        return ResponseEntity.ok(timelineService.getPlaybackSpeed(sessionId));
    }

    @PutMapping("/sessions/{sessionId}/position")
    @Operation(summary = "Update playback position", description = "Updates the current playback position in milliseconds")
    public ResponseEntity<Map<String, Object>> updatePosition(
            @PathVariable UUID sessionId,
            @RequestParam Integer positionMs) {

        log.info("Updating position for session: {} to {} ms", sessionId, positionMs);
        timelineService.updatePlaybackPosition(sessionId, positionMs);
        return ResponseEntity.ok(Map.of("sessionId", sessionId, "positionMs", positionMs));
    }

    @PostMapping("/sessions/{sessionId}/restore")
    @Operation(summary = "Restore session", description = "Restores a stopped session for continued playback")
    public ResponseEntity<Map<String, Object>> restoreSession(
            @PathVariable UUID sessionId) {

        log.info("Restoring session: {}", sessionId);
        return ResponseEntity.ok(timelineService.restoreSession(sessionId));
    }

    @GetMapping("/sessions/{sessionId}/state")
    @Operation(summary = "Get session state", description = "Gets the current state of a session for persistence")
    public ResponseEntity<Map<String, Object>> getSessionState(
            @PathVariable UUID sessionId) {

        return ResponseEntity.ok(timelineService.saveSessionState(sessionId));
    }

    // ==================== EVENT ENDPOINTS ====================

    @GetMapping("/events/{sessionId}")
    @Operation(summary = "Get timeline events for playback",
               description = "Retrieves events for a specific execution with optional filtering")
    public ResponseEntity<List<Map<String, Object>>> getEvents(
            @PathVariable UUID sessionId,
            @Parameter(description = "Execution ID for the timeline")
            @RequestParam UUID executionId,
            @Parameter(description = "Filter by event type")
            @RequestParam(required = false) String eventType,
            @Parameter(description = "Filter by user ID")
            @RequestParam(required = false) UUID userId,
            @Parameter(description = "Filter events after this date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Filter events before this date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Filter by step index")
            @RequestParam(required = false) Integer stepIndex) {

        log.info("Getting filtered events for execution: {}", executionId);

        if (eventType != null || userId != null || startDate != null || endDate != null || stepIndex != null) {
            return ResponseEntity.ok(timelineService.getFilteredTimeline(
                    executionId, eventType, userId, startDate, endDate, stepIndex));
        }

        return ResponseEntity.ok(timelineService.getTimeline(executionId));
    }

    // ==================== SNAPSHOT ENDPOINTS ====================

    @PostMapping("/snapshots")
    @Operation(summary = "Create snapshot", description = "Creates a snapshot at the current playback position")
    public ResponseEntity<SnapshotResponse> createSnapshot(
            @RequestParam UUID sessionId,
            @RequestParam(required = false) UUID userId,
            @RequestBody(required = false) CreateSnapshotRequest request) {

        log.info("Creating snapshot for session: {}", sessionId);
        Map<String, Object> result = timelineService.createSnapshot(
                sessionId, userId,
                request != null ? request.getName() : null,
                request != null ? request.getDescription() : null);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SnapshotResponse.builder()
                        .snapshotId((UUID) result.get("snapshotId"))
                        .sessionId(sessionId)
                        .name((String) result.get("name"))
                        .description((String) result.get("description"))
                        .eventIndex((Integer) result.get("eventIndex"))
                        .positionMs((Integer) result.get("positionMs"))
                        .createdAt((LocalDateTime) result.get("createdAt"))
                        .build());
    }

    @GetMapping("/snapshots/{snapshotId}")
    @Operation(summary = "Get snapshot", description = "Retrieves a specific snapshot with its event data")
    public ResponseEntity<SnapshotResponse> getSnapshot(
            @PathVariable UUID snapshotId) {

        log.info("Getting snapshot: {}", snapshotId);
        Map<String, Object> result = timelineService.getSnapshot(snapshotId);

        return ResponseEntity.ok(SnapshotResponse.builder()
                .snapshotId(snapshotId)
                .sessionId((UUID) result.get("sessionId"))
                .executionId((UUID) result.get("executionId"))
                .name((String) result.get("name"))
                .description((String) result.get("description"))
                .eventIndex((Integer) result.get("eventIndex"))
                .positionMs((Integer) result.get("positionMs"))
                .playbackSpeed((Double) result.get("playbackSpeed"))
                .createdAt((LocalDateTime) result.get("createdAt"))
                .createdBy((UUID) result.get("createdBy"))
                .build());
    }

    @GetMapping("/snapshots/session/{sessionId}")
    @Operation(summary = "Get snapshots for session", description = "Lists all snapshots for a specific session")
    public ResponseEntity<List<Map<String, Object>>> getSnapshotsForSession(
            @PathVariable UUID sessionId) {

        log.info("Getting snapshots for session: {}", sessionId);
        return ResponseEntity.ok(timelineService.getSnapshotsForSession(sessionId));
    }

    @GetMapping("/snapshots/execution/{executionId}")
    @Operation(summary = "Get snapshots for execution", description = "Lists all snapshots for a specific execution")
    public ResponseEntity<List<Map<String, Object>>> getSnapshotsForExecution(
            @PathVariable UUID executionId) {

        log.info("Getting snapshots for execution: {}", executionId);
        return ResponseEntity.ok(timelineService.getSnapshotsForExecution(executionId));
    }

    @DeleteMapping("/snapshots/{snapshotId}")
    @Operation(summary = "Delete snapshot", description = "Deletes a specific snapshot")
    public ResponseEntity<Void> deleteSnapshot(
            @PathVariable UUID snapshotId) {

        log.info("Deleting snapshot: {}", snapshotId);
        timelineService.deleteSnapshot(snapshotId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/snapshots/{snapshotId}/restore")
    @Operation(summary = "Restore from snapshot", description = "Restores session position from a snapshot")
    public ResponseEntity<Map<String, Object>> restoreFromSnapshot(
            @PathVariable UUID snapshotId) {

        log.info("Restoring from snapshot: {}", snapshotId);
        return ResponseEntity.ok(timelineService.restoreFromSnapshot(snapshotId));
    }

    // ==================== PLAYBACK TIMELINE DATA ====================

    @GetMapping("/playback/{sessionId}/data")
    @Operation(summary = "Get playback timeline data", description = "Retrieves complete timeline data for visualization")
    public ResponseEntity<PlaybackTimelineResponse> getPlaybackData(
            @PathVariable UUID sessionId) {

        log.info("Getting playback data for session: {}", sessionId);
        Map<String, Object> data = timelineService.getPlaybackTimelineData(sessionId);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events = (List<Map<String, Object>>) data.get("events");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> markers = (List<Map<String, Object>>) data.get("markers");
        @SuppressWarnings("unchecked")
        Map<String, Integer> eventTypes = (Map<String, Integer>) data.get("eventTypes");
        @SuppressWarnings("unchecked")
        List<Integer> stepBreaks = (List<Integer>) data.get("stepBreaks");

        return ResponseEntity.ok(PlaybackTimelineResponse.builder()
                .sessionId(sessionId)
                .executionId((UUID) data.get("executionId"))
                .currentPositionMs((Integer) data.get("currentPositionMs"))
                .isPlaying((Boolean) data.get("isPlaying"))
                .playbackSpeed((Double) data.get("playbackSpeed"))
                .totalEvents((Integer) data.get("totalEvents"))
                .totalDurationMs((Long) data.get("totalDurationMs"))
                .events(events != null ? events.stream().map(this::mapToEventResponse).toList() : List.of())
                .markers(markers)
                .eventTypes(eventTypes)
                .stepBreaks(stepBreaks)
                .build());
    }

    @GetMapping("/summary/{executionId}")
    @Operation(summary = "Get timeline summary", description = "Gets a summary of the timeline for an execution")
    public ResponseEntity<TimelineSummaryResponse> getTimelineSummary(
            @PathVariable UUID executionId) {

        log.info("Getting timeline summary for execution: {}", executionId);
        Map<String, Object> data = timelineService.getTimelineSummary(executionId);

        return ResponseEntity.ok(TimelineSummaryResponse.builder()
                .executionId(executionId)
                .totalEvents((Integer) data.get("totalEvents"))
                .totalDurationMs((Long) data.get("totalDurationMs"))
                .eventTypes((Map<String, Integer>) data.get("eventTypes"))
                .startTime((LocalDateTime) data.get("startTime"))
                .endTime((LocalDateTime) data.get("endTime"))
                .durationSeconds((Long) data.get("durationSeconds"))
                .build());
    }

    // ==================== HELPER METHODS ====================

    @SuppressWarnings("unchecked")
    private TimelineEventResponse mapToEventResponse(Map<String, Object> map) {
        return TimelineEventResponse.builder()
                .id((UUID) map.get("id"))
                .eventType((String) map.get("eventType"))
                .eventTimestamp((LocalDateTime) map.get("eventTimestamp"))
                .stepIndex((Integer) map.get("stepIndex"))
                .eventData((String) map.get("eventData"))
                .screenshotPath((String) map.get("screenshotPath"))
                .logEntries((java.util.List<String>) map.get("logEntries"))
                .sequenceOrder((Integer) map.get("sequenceOrder"))
                .metadata((Map<String, Object>) map.get("metadata"))
                .build();
    }
}