import { useCallback } from 'react';
import { useAuth } from '../../auth/context/AuthContext';

export type Permission = 'view' | 'edit' | 'admin' | 'none';

interface BoardPermissions {
  canView: boolean;
  canEdit: boolean;
  canAdmin: boolean;
  canMoveIssues: boolean;
  canCreateIssues: boolean;
  canDeleteIssues: boolean;
  canConfigureBoard: boolean;
  canManageSprints: boolean;
}

interface UseBoardPermissionsOptions {
  boardId?: string;
  projectId?: string;
  ownerId?: string;
}

export function useBoardPermissions({ boardId, projectId, ownerId }: UseBoardPermissionsOptions) {
  const { user, permissions: userPermissions } = useAuth();

  const checkPermission = useCallback(
    (permission: keyof BoardPermissions): boolean => {
      if (!user) return false;

      if (user.isAdmin) return true;

      const projectPerms = userPermissions?.projectPermissions?.[projectId || ''];
      if (projectPerms?.admin) return true;

      if (ownerId === user.userId) return true;

      const boardPerms = userPermissions?.boardPermissions?.[boardId || ''];

      switch (permission) {
        case 'canView':
          return boardPerms?.view ?? projectPerms?.view ?? true;
        case 'canEdit':
          return boardPerms?.edit ?? projectPerms?.edit ?? false;
        case 'canAdmin':
          return boardPerms?.admin ?? projectPerms?.admin ?? false;
        case 'canMoveIssues':
        case 'canCreateIssues':
          return boardPerms?.edit ?? projectPerms?.edit ?? false;
        case 'canDeleteIssues':
          return boardPerms?.admin ?? projectPerms?.admin ?? false;
        case 'canConfigureBoard':
          return boardPerms?.admin ?? projectPerms?.admin ?? false;
        case 'canManageSprints':
          return boardPerms?.admin ?? projectPerms?.admin ?? false;
        default:
          return false;
      }
    },
    [user, userPermissions, boardId, projectId, ownerId],
  );

  const permissions: BoardPermissions = {
    canView: checkPermission('canView'),
    canEdit: checkPermission('canEdit'),
    canAdmin: checkPermission('canAdmin'),
    canMoveIssues: checkPermission('canMoveIssues'),
    canCreateIssues: checkPermission('canCreateIssues'),
    canDeleteIssues: checkPermission('canDeleteIssues'),
    canConfigureBoard: checkPermission('canConfigureBoard'),
    canManageSprints: checkPermission('canManageSprints'),
  };

  return {
    permissions,
    isOwner: user?.userId === ownerId,
    canEdit: permissions.canEdit,
    canAdmin: permissions.canAdmin,
  };
}