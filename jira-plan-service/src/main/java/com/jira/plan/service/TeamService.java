package com.jira.plan.service;

import com.jira.plan.dto.request.AddTeamMemberRequest;
import com.jira.plan.dto.request.CreateTeamRequest;
import com.jira.plan.dto.response.TeamMemberResponse;
import com.jira.plan.dto.response.TeamResponse;
import com.jira.plan.entity.Plan;
import com.jira.plan.entity.PlanTeam;
import com.jira.plan.entity.PlanTeamMember;
import com.jira.plan.exception.ResourceNotFoundException;
import com.jira.plan.repository.PlanRepository;
import com.jira.plan.repository.PlanTeamMemberRepository;
import com.jira.plan.repository.PlanTeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final PlanTeamRepository teamRepository;
    private final PlanTeamMemberRepository memberRepository;
    private final PlanRepository planRepository;

    @Transactional(readOnly = true)
    public List<TeamResponse> getTeamsByPlanId(UUID planId) {
        return teamRepository.findByPlanIdAndIsActiveTrue(planId).stream()
                .map(this::toTeamResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeamById(UUID planId, UUID teamId) {
        PlanTeam team = findTeamById(teamId);
        return toTeamResponse(team);
    }

    @Transactional
    public TeamResponse createTeam(UUID planId, CreateTeamRequest request) {
        Plan plan = findPlanById(planId);

        PlanTeam team = PlanTeam.builder()
                .planId(planId)
                .name(request.getName())
                .description(request.getDescription())
                .build();

        team = teamRepository.save(team);
        return toTeamResponse(team);
    }

    @Transactional
    public TeamResponse updateTeam(UUID planId, UUID teamId, CreateTeamRequest request) {
        PlanTeam team = findTeamById(teamId);

        if (request.getName() != null) {
            team.setName(request.getName());
        }
        if (request.getDescription() != null) {
            team.setDescription(request.getDescription());
        }

        team = teamRepository.save(team);
        return toTeamResponse(team);
    }

    @Transactional
    public void deleteTeam(UUID planId, UUID teamId) {
        PlanTeam team = findTeamById(teamId);
        team.setIsActive(false);
        teamRepository.save(team);
    }

    @Transactional
    public TeamMemberResponse addTeamMember(UUID teamId, AddTeamMemberRequest request) {
        PlanTeam team = findTeamById(teamId);

        PlanTeamMember member = PlanTeamMember.builder()
                .teamId(teamId)
                .userId(request.getUserId())
                .userName(request.getUserName())
                .capacityHours(request.getCapacityHours() != null ? request.getCapacityHours() : new BigDecimal("40.00"))
                .role(request.getRole())
                .build();

        member = memberRepository.save(member);
        return toMemberResponse(member);
    }

    @Transactional
    public void removeTeamMember(UUID teamId, UUID memberId) {
        PlanTeamMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("TeamMember", "id", memberId));
        memberRepository.delete(member);
    }

    private Plan findPlanById(UUID id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", id));
    }

    private PlanTeam findTeamById(UUID id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", id));
    }

    private TeamResponse toTeamResponse(PlanTeam team) {
        List<PlanTeamMember> members = memberRepository.findByTeamId(team.getId());
        BigDecimal totalCapacity = members.stream()
                .map(PlanTeamMember::getCapacityHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return TeamResponse.builder()
                .id(team.getId())
                .planId(team.getPlanId())
                .name(team.getName())
                .description(team.getDescription())
                .isActive(team.getIsActive())
                .memberCount(members.size())
                .totalCapacity(totalCapacity)
                .members(members.stream().map(this::toMemberResponse).collect(Collectors.toList()))
                .createdAt(team.getCreatedAt())
                .updatedAt(team.getUpdatedAt())
                .build();
    }

    private TeamMemberResponse toMemberResponse(PlanTeamMember member) {
        return TeamMemberResponse.builder()
                .id(member.getId())
                .teamId(member.getTeamId())
                .userId(member.getUserId())
                .userName(member.getUserName())
                .capacityHours(member.getCapacityHours())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
