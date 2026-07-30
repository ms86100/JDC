import { useQuery } from '@tanstack/react-query';
import { projectApi } from '../../api/projectApi';
import { planApi } from '../../api/planApi';
import { resolveProjectTemplate } from '../../lib/projectTemplate';
import type { RouteContext } from './contextNav';

export function useContextEntity(context: RouteContext | null) {
  const projectQuery = useQuery({
    queryKey: ['context-project', context?.id],
    queryFn: async () => {
      const res = await projectApi.getById(context!.id);
      return res.data;
    },
    enabled: context?.type === 'project',
    staleTime: 60000,
  });

  const programQuery = useQuery({
    queryKey: ['context-program', context?.id],
    queryFn: async () => {
      const res = await planApi.getProgramById(context!.id);
      return res.data;
    },
    enabled: context?.type === 'program',
    staleTime: 60000,
  });

  const planQuery = useQuery({
    queryKey: ['context-plan', context?.id],
    queryFn: async () => {
      const res = await planApi.getPlanById(context!.id);
      return res.data;
    },
    enabled: context?.type === 'plan',
    staleTime: 60000,
  });

  if (!context) {
    return {
      label: null as string | null,
      template: undefined as string | undefined,
      defaultBoardPath: undefined as string | undefined,
      isLoading: false,
    };
  }

  if (context.type === 'project') {
    const projectBoardPath = `/projects/${context.id}/board/active`;
    return {
      label: projectQuery.data?.name ?? null,
      template: resolveProjectTemplate(projectQuery.data),
      subtitle: projectQuery.data?.projectKey,
      defaultBoardPath: projectBoardPath,
      isLoading: projectQuery.isPending && !projectQuery.data,
    };
  }

  if (context.type === 'plan') {
    return {
      label: planQuery.data?.name ?? null,
      template: undefined,
      subtitle: planQuery.data ? `${planQuery.data.itemCount} items` : undefined,
      defaultBoardPath: undefined,
      isLoading: planQuery.isLoading,
    };
  }

  return {
    label: programQuery.data?.name ?? null,
    template: undefined,
    subtitle: programQuery.data ? `${programQuery.data.planCount} plans` : undefined,
    defaultBoardPath: undefined,
    isLoading: programQuery.isLoading,
  };
}
