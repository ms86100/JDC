import { useQuery } from '@tanstack/react-query';
import { planApi, PlanResponse, SprintBurndownResponse, SprintResponse as PlanSprintResponse } from '../../api/planApi';
import type { BurndownDataPoint, SprintVelocity } from '../../api/sprintApi';

export interface ProgramActiveSprint {
  sprint: PlanSprintResponse;
  planId: string;
  planName: string;
  boardId: string;
}

export interface ProgramDeliveryMetrics {
  activeSprints: ProgramActiveSprint[];
  primaryBurndown: SprintBurndownResponse | null;
  burndownChartData: BurndownDataPoint[];
  burndownTotalPoints: number;
  velocitySeries: SprintVelocity[];
  averageVelocity: number;
  totalClosedSprints: number;
}

async function loadDeliveryForPlans(plans: PlanResponse[]): Promise<ProgramDeliveryMetrics> {
  const activeSprints: ProgramActiveSprint[] = [];
  const velocitySeries: SprintVelocity[] = [];

  for (const plan of plans) {
    let boards: { id: string; name?: string }[] = [];
    try {
      const boardsRes = await planApi.getBoards(plan.id);
      boards = boardsRes.data ?? [];
    } catch {
      continue;
    }

    for (const board of boards) {
      let sprints: PlanSprintResponse[] = [];
      try {
        const sprintsRes = await planApi.getSprints(board.id);
        sprints = sprintsRes.data ?? [];
      } catch {
        continue;
      }

      for (const sprint of sprints) {
        if (sprint.state === 'ACTIVE') {
          activeSprints.push({
            sprint,
            planId: plan.id,
            planName: plan.name,
            boardId: board.id,
          });
        }
        if (sprint.state === 'CLOSED' && (sprint.committedPoints > 0 || sprint.completedPoints > 0)) {
          velocitySeries.push({
            sprintId: sprint.id,
            sprintName: `${plan.name}: ${sprint.name}`,
            startDate: sprint.startDate ?? '',
            endDate: sprint.endDate ?? sprint.completeDate ?? '',
            committedPoints: sprint.committedPoints,
            completedPoints: sprint.completedPoints,
            reliability:
              sprint.committedPoints > 0
                ? Math.round((sprint.completedPoints / sprint.committedPoints) * 100)
                : 0,
            isCompleted: true,
          });
        }
      }
    }
  }

  let primaryBurndown: SprintBurndownResponse | null = null;
  const primary = activeSprints[0];
  if (primary) {
    try {
      const res = await planApi.getSprintBurndown(primary.sprint.id);
      primaryBurndown = res.data;
    } catch {
      primaryBurndown = null;
    }
  }

  const burndownChartData: BurndownDataPoint[] =
    primaryBurndown?.burndownPoints?.map((p) => ({
      date: p.date,
      remainingPoints: p.remainingPoints ?? p.remainingIssues,
      idealPoints: p.idealRemaining,
      totalIssues: primaryBurndown!.totalIssues,
      completedIssues: p.completedIssues,
      addedIssues: 0,
      removedIssues: 0,
    })) ?? [];

  const burndownTotalPoints =
    primaryBurndown?.totalPoints ?? primaryBurndown?.totalIssues ?? primary?.sprint.committedPoints ?? 0;

  const completedVelocities = velocitySeries.filter((v) => v.isCompleted);
  const averageVelocity =
    completedVelocities.length > 0
      ? completedVelocities.reduce((s, v) => s + v.completedPoints, 0) / completedVelocities.length
      : activeSprints.reduce((s, a) => s + (a.sprint.velocity ?? 0), 0) / Math.max(activeSprints.length, 1);

  return {
    activeSprints,
    primaryBurndown,
    burndownChartData,
    burndownTotalPoints,
    velocitySeries: completedVelocities.slice(-8),
    averageVelocity,
    totalClosedSprints: completedVelocities.length,
  };
}

/** Delivery metrics for plans linked to one or more programs */
export function useProgramDeliveryMetrics(plans: PlanResponse[], enabled = true) {
  const planKey = plans.map((p) => p.id).sort().join(',');

  return useQuery({
    queryKey: ['ws-program-delivery', planKey],
    queryFn: () => loadDeliveryForPlans(plans),
    enabled: enabled && plans.length > 0,
    staleTime: 60000,
  });
}

/** Portfolio-wide delivery across all plans (programs list page) */
export function usePortfolioDeliveryMetrics(plans: PlanResponse[]) {
  return useProgramDeliveryMetrics(plans, plans.length > 0);
}
