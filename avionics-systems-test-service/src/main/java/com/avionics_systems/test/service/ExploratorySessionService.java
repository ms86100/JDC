package com.avionics_systems.test.service;

import com.avionics_systems.test.dto.ExploratorySessionRequest;
import com.avionics_systems.test.dto.ExploratorySessionResponse;
import com.avionics_systems.test.entity.ExploratorySession;
import com.avionics_systems.test.exception.InvalidOperationException;
import com.avionics_systems.test.exception.ResourceNotFoundException;
import com.avionics_systems.test.repository.ExploratorySessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExploratorySessionService {

    private final ExploratorySessionRepository exploratorySessionRepository;

    @Transactional
    public ExploratorySessionResponse createSession(ExploratorySessionRequest request) {
        log.info("Creating exploratory session for project: {}", request.getProjectId());

        ExploratorySession session = ExploratorySession.builder()
                .projectId(request.getProjectId())
                .charter(request.getCharter())
                .charterGoal(request.getCharterGoal())
                .sessionType(request.getSessionType() != null ? request.getSessionType() : "CHARTER_BASED")
                .timeBoxMinutes(request.getTimeBoxMinutes() != null ? request.getTimeBoxMinutes() : 60)
                .testerId(request.getTesterId())
                .environment(request.getEnvironment())
                .notes(request.getNotes())
                .bugs(request.getBugs() != null ? request.getBugs() : List.of())
                .ideas(request.getIdeas() != null ? request.getIdeas() : List.of())
                .questions(request.getQuestions() != null ? request.getQuestions() : List.of())
                .evidenceLinks(request.getEvidenceLinks() != null ? request.getEvidenceLinks() : List.of())
                .defectKeys(request.getDefectKeys() != null ? request.getDefectKeys() : List.of())
                .build();

        ExploratorySession saved = exploratorySessionRepository.save(session);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public ExploratorySessionResponse getSession(UUID id) {
        log.debug("Fetching exploratory session: {}", id);
        ExploratorySession session = exploratorySessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExploratorySession", "id", id));
        return mapToResponse(session);
    }

    @Transactional(readOnly = true)
    public List<ExploratorySessionResponse> getSessionsByProject(UUID projectId) {
        log.debug("Fetching exploratory sessions for project: {}", projectId);
        return exploratorySessionRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public ExploratorySessionResponse updateSession(UUID id, ExploratorySessionRequest request) {
        log.info("Updating exploratory session: {}", id);
        ExploratorySession session = exploratorySessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExploratorySession", "id", id));

        if (request.getCharter() != null) session.setCharter(request.getCharter());
        if (request.getCharterGoal() != null) session.setCharterGoal(request.getCharterGoal());
        if (request.getSessionType() != null) session.setSessionType(request.getSessionType());
        if (request.getTimeBoxMinutes() != null) session.setTimeBoxMinutes(request.getTimeBoxMinutes());
        if (request.getTesterId() != null) session.setTesterId(request.getTesterId());
        if (request.getEnvironment() != null) session.setEnvironment(request.getEnvironment());
        if (request.getNotes() != null) session.setNotes(request.getNotes());
        if (request.getBugs() != null) session.setBugs(request.getBugs());
        if (request.getIdeas() != null) session.setIdeas(request.getIdeas());
        if (request.getQuestions() != null) session.setQuestions(request.getQuestions());
        if (request.getEvidenceLinks() != null) session.setEvidenceLinks(request.getEvidenceLinks());
        if (request.getDefectKeys() != null) session.setDefectKeys(request.getDefectKeys());

        ExploratorySession saved = exploratorySessionRepository.save(session);
        return mapToResponse(saved);
    }

    @Transactional
    public ExploratorySessionResponse startSession(UUID id) {
        log.info("Starting exploratory session: {}", id);
        ExploratorySession session = exploratorySessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExploratorySession", "id", id));

        if (!"PLANNED".equals(session.getStatus())) {
            throw new InvalidOperationException("Session can only be started from PLANNED status, current: " + session.getStatus());
        }

        session.setStatus("IN_PROGRESS");
        session.setStartedAt(LocalDateTime.now());

        ExploratorySession saved = exploratorySessionRepository.save(session);
        return mapToResponse(saved);
    }

    @Transactional
    public ExploratorySessionResponse completeSession(UUID id) {
        log.info("Completing exploratory session: {}", id);
        ExploratorySession session = exploratorySessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExploratorySession", "id", id));

        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new InvalidOperationException("Session can only be completed from IN_PROGRESS status, current: " + session.getStatus());
        }

        session.setStatus("COMPLETED");
        session.setCompletedAt(LocalDateTime.now());

        if (session.getStartedAt() != null) {
            long minutes = ChronoUnit.MINUTES.between(session.getStartedAt(), session.getCompletedAt());
            session.setActualDurationMinutes((int) minutes);
        }

        ExploratorySession saved = exploratorySessionRepository.save(session);
        return mapToResponse(saved);
    }

    @Transactional
    public ExploratorySessionResponse abandonSession(UUID id) {
        log.info("Abandoning exploratory session: {}", id);
        ExploratorySession session = exploratorySessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExploratorySession", "id", id));

        if ("COMPLETED".equals(session.getStatus()) || "ABANDONED".equals(session.getStatus())) {
            throw new InvalidOperationException("Session cannot be abandoned from " + session.getStatus() + " status");
        }

        session.setStatus("ABANDONED");
        session.setCompletedAt(LocalDateTime.now());

        if (session.getStartedAt() != null) {
            long minutes = ChronoUnit.MINUTES.between(session.getStartedAt(), session.getCompletedAt());
            session.setActualDurationMinutes((int) minutes);
        }

        ExploratorySession saved = exploratorySessionRepository.save(session);
        return mapToResponse(saved);
    }

    @Transactional
    public ExploratorySessionResponse addNotes(UUID id, String notes) {
        log.info("Adding notes to exploratory session: {}", id);
        ExploratorySession session = exploratorySessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExploratorySession", "id", id));

        String existingNotes = session.getNotes() != null ? session.getNotes() : "";
        session.setNotes(existingNotes.isEmpty() ? notes : existingNotes + "\n" + notes);

        ExploratorySession saved = exploratorySessionRepository.save(session);
        return mapToResponse(saved);
    }

    @Transactional
    public ExploratorySessionResponse addBug(UUID id, String bug) {
        log.info("Adding bug to exploratory session: {}", id);
        ExploratorySession session = exploratorySessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExploratorySession", "id", id));

        List<String> bugs = new ArrayList<>(session.getBugs() != null ? session.getBugs() : List.of());
        bugs.add(bug);
        session.setBugs(bugs);

        ExploratorySession saved = exploratorySessionRepository.save(session);
        return mapToResponse(saved);
    }

    @Transactional
    public ExploratorySessionResponse addIdea(UUID id, String idea) {
        log.info("Adding idea to exploratory session: {}", id);
        ExploratorySession session = exploratorySessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExploratorySession", "id", id));

        List<String> ideas = new ArrayList<>(session.getIdeas() != null ? session.getIdeas() : List.of());
        ideas.add(idea);
        session.setIdeas(ideas);

        ExploratorySession saved = exploratorySessionRepository.save(session);
        return mapToResponse(saved);
    }

    @Transactional
    public ExploratorySessionResponse addQuestion(UUID id, String question) {
        log.info("Adding question to exploratory session: {}", id);
        ExploratorySession session = exploratorySessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExploratorySession", "id", id));

        List<String> questions = new ArrayList<>(session.getQuestions() != null ? session.getQuestions() : List.of());
        questions.add(question);
        session.setQuestions(questions);

        ExploratorySession saved = exploratorySessionRepository.save(session);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteSession(UUID id) {
        log.info("Deleting exploratory session: {}", id);
        ExploratorySession session = exploratorySessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExploratorySession", "id", id));
        exploratorySessionRepository.delete(session);
    }

    private ExploratorySessionResponse mapToResponse(ExploratorySession session) {
        return ExploratorySessionResponse.builder()
                .id(session.getId())
                .projectId(session.getProjectId())
                .charter(session.getCharter())
                .charterGoal(session.getCharterGoal())
                .sessionType(session.getSessionType())
                .timeBoxMinutes(session.getTimeBoxMinutes())
                .actualDurationMinutes(session.getActualDurationMinutes())
                .status(session.getStatus())
                .testerId(session.getTesterId())
                .environment(session.getEnvironment())
                .startedAt(session.getStartedAt())
                .completedAt(session.getCompletedAt())
                .notes(session.getNotes())
                .bugs(session.getBugs())
                .ideas(session.getIdeas())
                .questions(session.getQuestions())
                .evidenceLinks(session.getEvidenceLinks())
                .defectKeys(session.getDefectKeys())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }
}
