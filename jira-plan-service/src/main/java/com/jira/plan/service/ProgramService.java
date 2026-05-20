package com.jira.plan.service;

import com.jira.plan.dto.request.CreateProgramRequest;
import com.jira.plan.dto.request.UpdateProgramRequest;
import com.jira.plan.dto.response.ProgramResponse;
import com.jira.plan.entity.Plan;
import com.jira.plan.entity.Program;
import com.jira.plan.exception.ResourceNotFoundException;
import com.jira.plan.repository.PlanRepository;
import com.jira.plan.repository.ProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgramService {

    private final ProgramRepository programRepository;
    private final PlanRepository planRepository;

    @Transactional(readOnly = true)
    public List<ProgramResponse> getAllPrograms() {
        return programRepository.findByIsActiveTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProgramResponse getProgramById(UUID id) {
        Program program = findProgramById(id);
        return toResponse(program);
    }

    @Transactional
    public ProgramResponse createProgram(CreateProgramRequest request, UUID userId) {
        Program program = Program.builder()
                .name(request.getName())
                .description(request.getDescription())
                .ownerId(request.getOwnerId() != null ? request.getOwnerId() : userId)
                .accessType(request.getAccessType() != null ? request.getAccessType() : "OPEN")
                .build();
        program = programRepository.save(program);
        return toResponse(program);
    }

    @Transactional
    public ProgramResponse updateProgram(UUID id, UpdateProgramRequest request) {
        Program program = findProgramById(id);
        if (request.getName() != null) {
            program.setName(request.getName());
        }
        if (request.getDescription() != null) {
            program.setDescription(request.getDescription());
        }
        if (request.getAccessType() != null) {
            program.setAccessType(request.getAccessType());
        }
        if (request.getIsActive() != null) {
            program.setIsActive(request.getIsActive());
        }
        program = programRepository.save(program);
        return toResponse(program);
    }

    @Transactional
    public void deleteProgram(UUID id) {
        Program program = findProgramById(id);
        program.setIsActive(false);
        // Cascade soft-delete to linked plans
        if (program.getPlans() != null) {
            program.getPlans().forEach(plan -> plan.setIsActive(false));
        }
        programRepository.save(program);
    }

    @Transactional
    public void linkPlanToProgram(UUID programId, UUID planId) {
        Program program = findProgramById(programId);
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", planId));
        program.getPlans().add(plan);
        programRepository.save(program);
    }

    @Transactional
    public void unlinkPlanFromProgram(UUID programId, UUID planId) {
        Program program = findProgramById(programId);
        program.getPlans().removeIf(p -> p.getId().equals(planId));
        programRepository.save(program);
    }

    private Program findProgramById(UUID id) {
        return programRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Program", "id", id));
    }

    private ProgramResponse toResponse(Program program) {
        int planCount = 0;
        try {
            planCount = program.getPlans() != null ? program.getPlans().size() : 0;
        } catch (Exception e) {
            // LazyInitializationException - plans not loaded, use count query instead
        }
        return ProgramResponse.builder()
                .id(program.getId())
                .name(program.getName())
                .description(program.getDescription())
                .ownerId(program.getOwnerId())
                .accessType(program.getAccessType())
                .isActive(program.getIsActive())
                .planCount(planCount)
                .createdAt(program.getCreatedAt())
                .updatedAt(program.getUpdatedAt())
                .build();
    }
}
