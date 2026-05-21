import { useQuery } from '@tanstack/react-query';
import { projectApi } from '../../api/projectApi';
import { planApi } from '../../api/planApi';
import { useDefaultBoard } from '../workspace/useDefaultBoard';
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

  const defaultBoard = useDefaultBoard(context?.type === 'project' ? context.id : undefined);

  if (!context) {
    return {
      label: null as string | null,
      template: undefined as string | undefined,
      defaultBoardPath: undefined as string | undefined,
      isLoading: false,
    };
  }

  if (context.type === 'project') {
    return {
      label: projectQuery.data?.name ?? null,
      template: projectQuery.data?.template,
      subtitle: projectQuery.data?.projectKey,
      defaultBoardPath: defaultBoard.boardHref,
      isLoading: projectQuery.isLoading || defaultBoard.isLoading,
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
