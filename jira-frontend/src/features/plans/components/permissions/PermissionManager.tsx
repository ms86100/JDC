import React, { useState } from 'react';
import { useBoardPermissions, useGrantBoardPermission, useRevokeBoardPermission } from '../../hooks/usePermissions';
import { appNotify } from '../../../../lib/appNotify';

interface PermissionManagerProps {
  boardId: string;
  onClose: () => void;
}

export default function PermissionManager({ boardId, onClose }: PermissionManagerProps) {
  const { data: permissions, isLoading } = useBoardPermissions(boardId);
  const grantPermission = useGrantBoardPermission();
  const revokePermission = useRevokeBoardPermission();

  const [formData, setFormData] = useState({
    permissionType: 'VIEW' as 'VIEW' | 'EDIT' | 'ADMIN' | 'MANAGE_SPRINTS' | 'EDIT_SPRINTS',
    principalType: 'USER' as 'USER' | 'GROUP',
    principalId: '',
  });

  const PERMISSION_TYPES = [
    { value: 'VIEW', label: 'View', description: 'Can view the board' },
    { value: 'EDIT', label: 'Edit', description: 'Can edit board configuration' },
    { value: 'ADMIN', label: 'Admin', description: 'Full administrative access' },
    { value: 'MANAGE_SPRINTS', label: 'Manage Sprints', description: 'Can create and manage sprints' },
    { value: 'EDIT_SPRINTS', label: 'Edit Sprints', description: 'Can edit sprint name and goal' },
  ];

  if (isLoading) {
    return <div className="ab-config-loading">Loading permissions...</div>;
  }

  const handleGrant = () => {
    if (!formData.principalId.trim()) {
      appNotify.warning('Please enter a user or group ID');
      return;
    }
    grantPermission.mutate({
      boardId,
      data: {
        permissionType: formData.permissionType,
        principalType: formData.principalType,
        principalId: formData.principalId,
      },
    });
    setFormData({ ...formData, principalId: '' });
  };

  const handleRevoke = (permissionId: string) => {
    if (confirm('Are you sure you want to revoke this permission?')) {
      revokePermission.mutate(permissionId);
    }
  };

  const getPermissionInfo = (type: string) => {
    return PERMISSION_TYPES.find(p => p.value === type);
  };

  return (
    <div className="ab-permission-manager">
      <div className="ab-config-header">
        <h2>Board Permissions</h2>
        <button className="ab-btn-close" onClick={onClose}>&times;</button>
      </div>

      <div className="ab-config-content">
        {/* Add Permission */}
        <div className="ab-permission-form">
          <h3>Grant Permission</h3>
          <div className="ab-form-group">
            <label>Permission Type</label>
            <select
              className="ab-select"
              value={formData.permissionType}
              onChange={(e) => setFormData({ ...formData, permissionType: e.target.value as typeof formData.permissionType })}
            >
              {PERMISSION_TYPES.map(perm => (
                <option key={perm.value} value={perm.value}>
                  {perm.label} - {perm.description}
                </option>
              ))}
            </select>
          </div>
          <div className="ab-form-group">
            <label>Grant To</label>
            <select
              className="ab-select"
              value={formData.principalType}
              onChange={(e) => setFormData({ ...formData, principalType: e.target.value as 'USER' | 'GROUP' })}
            >
              <option value="USER">User</option>
              <option value="GROUP">Group</option>
            </select>
          </div>
          <div className="ab-form-group">
            <label>ID</label>
            <input
              type="text"
              className="ab-input"
              placeholder={`Enter ${formData.principalType.toLowerCase()} ID`}
              value={formData.principalId}
              onChange={(e) => setFormData({ ...formData, principalId: e.target.value })}
            />
          </div>
          <button className="ab-btn ab-btn-primary" onClick={handleGrant}>
            Grant Permission
          </button>
        </div>

        {/* Current Permissions */}
        <div className="ab-permissions-list">
          <h3>Current Permissions</h3>
          {permissions?.length === 0 ? (
            <p className="ab-empty-state">No permissions configured</p>
          ) : (
            <table className="ab-permissions-table">
              <thead>
                <tr>
                  <th>Permission</th>
                  <th>Type</th>
                  <th>ID</th>
                  <th>Granted</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {permissions?.map(perm => (
                  <tr key={perm.id}>
                    <td>
                      <span className="ab-permission-badge">{perm.permissionType}</span>
                    </td>
                    <td>{perm.principalType}</td>
                    <td className="ab-principal-id">{perm.principalId}</td>
                    <td>{new Date(perm.grantedAt).toLocaleDateString()}</td>
                    <td>
                      <button
                        className="ab-btn ab-btn-sm ab-btn-danger"
                        onClick={() => handleRevoke(perm.id)}
                      >
                        Revoke
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* Permission Descriptions */}
        <div className="ab-permission-info">
          <h4>Permission Types</h4>
          <dl>
            {PERMISSION_TYPES.map(perm => (
              <React.Fragment key={perm.value}>
                <dt>{perm.label}</dt>
                <dd>{perm.description}</dd>
              </React.Fragment>
            ))}
          </dl>
        </div>
      </div>
    </div>
  );
}