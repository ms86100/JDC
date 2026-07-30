import React, { useState } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import AdminLayout from '../components/AdminLayout';
import {
  useAviSysUsers,
  useAviSysGroups,
  useAviSysGroupByName,
  useCreateAviSysGroup,
  useDeleteAviSysGroup,
  useAviSysUser,
  useDeleteAviSysUser,
  useAddUserToGroup,
  useRemoveUserFromGroup,
  useAviSysGroupMembers,
  AviSysUser,
  AviSysGroup,
} from '../hooks/useAdminApi';
import './GroupMembersPage.css';

export default function GroupMembersPage() {
  const { groupId } = useParams();
  const navigate = useNavigate();
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const pageSize = 50;
  const [showAddModal, setShowAddModal] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // Get all groups to find current group
  const { data: groupsData } = useAviSysGroups({ size: 100 });
  const currentGroup = groupsData?.content?.find(g => g.id === groupId);

  // Get all users to add
  const { data: usersData, refetch: refetchUsers } = useAviSysUsers({ page, size: pageSize });
  const groups = useAviSysGroups({ size: 100 });
  const createGroup = useCreateAviSysGroup();
  const deleteGroup = useDeleteAviSysGroup();

  const showMessage = (msg: string, isError = false) => {
    if (isError) {
      setError(msg);
      setSuccess(null);
    } else {
      setSuccess(msg);
      setError(null);
    }
    setTimeout(() => {
      setError(null);
      setSuccess(null);
    }, 3000);
  };

  // Add/Remove user mutations
  const addUserToGroup = useAddUserToGroup();
  const removeUserFromGroup = useRemoveUserFromGroup();

  // Get current group members
  const { data: groupMembersData, refetch: refetchMembers } = useAviSysGroupMembers(groupId || '');

  const handleCreateGroup = async () => {
    if (!search.trim()) return;
    try {
      await createGroup.mutateAsync({ name: search.trim() });
      showMessage(`Group "${search}" created successfully`);
      setSearch('');
      refetchUsers();
    } catch (err: any) {
      showMessage(err?.message || 'Failed to create group', true);
    }
  };

  const handleDeleteGroup = async (id: string, name: string) => {
    if (!confirm(`Are you sure you want to delete the group "${name}"?`)) return;
    try {
      await deleteGroup.mutateAsync(id);
      showMessage(`Group "${name}" deleted successfully`);
      navigate('/admin/groups');
    } catch (err: any) {
      showMessage(err?.message || 'Failed to delete group', true);
    }
  };

  const handleAddMember = async (userId: string, displayName: string) => {
    if (!groupId) return;
    try {
      await addUserToGroup.mutateAsync({ groupId, userId });
      showMessage(`${displayName} added to group successfully`);
      refetchMembers();
    } catch (err: any) {
      showMessage(err?.message || 'Failed to add user to group', true);
    }
  };

  const handleRemoveMember = async (userId: string, displayName: string) => {
    if (!groupId) return;
    if (!confirm(`Remove ${displayName} from this group?`)) return;
    try {
      await removeUserFromGroup.mutateAsync({ groupId, userId });
      showMessage(`${displayName} removed from group successfully`);
      refetchMembers();
    } catch (err: any) {
      showMessage(err?.message || 'Failed to remove user from group', true);
    }
  };

  if (!groupId) {
    return (
      <AdminLayout>
        <div className="admin-page">
          <div className="admin-page-header">
            <h1 className="admin-page-title">Group Members</h1>
          </div>
          <div className="group-members-error">
            <p>No group specified. Please select a group from the{' '}
              <Link to="/admin/groups">Groups list</Link>.
            </p>
          </div>
        </div>
      </AdminLayout>
    );
  }

  if (!currentGroup) {
    return (
      <AdminLayout>
        <div className="admin-page">
          <div className="admin-page-header">
            <h1 className="admin-page-title">Group Members</h1>
          </div>
          <div className="group-members-error">
            <p>Group not found.</p>
            <Link to="/admin/groups">← Back to Groups</Link>
          </div>
        </div>
      </AdminLayout>
    );
  }

  return (
    <AdminLayout>
      <div className="admin-page">
        <div className="admin-page-header">
          <h1 className="admin-page-title">Group Members</h1>
          <p className="admin-page-description">
            Manage members for group: {currentGroup.name}
          </p>
        </div>

        {error && <div className="admin-alert admin-alert-error">{error}</div>}
        {success && <div className="admin-alert admin-alert-success">{success}</div>}

        <div className="admin-toolbar">
          <div className="admin-toolbar-left">
            <input
              type="text"
              placeholder="Filter members..."
              className="admin-search-input-toolbar"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <div className="admin-toolbar-right">
            <button className="admin-btn-primary" onClick={() => setShowAddModal(true)}>Add Members</button>
          </div>
        </div>

        <div className="admin-table-container">
          <table className="admin-table">
            <thead>
              <tr>
                <th>User</th>
                <th>Email</th>
                <th>Applications</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {groupMembersData && groupMembersData.length > 0 ? (
                groupMembersData.map(member => (
                  <tr key={member.id}>
                    <td>
                      <div className="user-cell">
                        <div className="user-avatar">{member.displayName.charAt(0).toUpperCase()}</div>
                        <div className="user-info">
                          <div className="user-name">{member.displayName}</div>
                          <div className="user-username">@{member.userName}</div>
                        </div>
                      </div>
                    </td>
                    <td>{member.emailAddress}</td>
                    <td>{member.applications?.join(', ') || '-'}</td>
                    <td>
                      <button
                        className="admin-btn-danger admin-btn-sm"
                        onClick={() => handleRemoveMember(member.id, member.displayName)}
                      >
                        Remove
                      </button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={4} className="empty-cell">
                    No members in this group. Click "Add Members" to add users.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Add Members Modal */}
        {showAddModal && (
          <div className="admin-modal-overlay" onClick={() => setShowAddModal(false)}>
            <div className="admin-modal admin-modal-wide" onClick={(e) => e.stopPropagation()}>
              <div className="admin-modal-header">
                <h2 className="admin-modal-title">Add Members to {currentGroup.name}</h2>
                <button className="admin-modal-close" onClick={() => setShowAddModal(false)}>×</button>
              </div>
              <div className="admin-modal-body">
                <p className="modal-info">Select users to add to this group:</p>
                <div className="admin-form-group">
                  <label className="admin-form-label">Search Users</label>
                  <input
                    type="text"
                    className="admin-form-input"
                    placeholder="Search by name or email..."
                  />
                </div>
                <div className="user-list">
                  {usersData?.content?.map(user => {
                    const isMember = groupMembersData?.some(m => m.id === user.id);
                    return (
                      <div key={user.id} className="user-list-item">
                        <div className="user-list-avatar">{user.displayName.charAt(0).toUpperCase()}</div>
                        <div className="user-list-info">
                          <div className="user-list-name">{user.displayName}</div>
                          <div className="user-list-email">{user.emailAddress}</div>
                        </div>
                        {isMember ? (
                          <span className="already-member-badge">Member</span>
                        ) : (
                          <button
                            className="admin-btn-secondary admin-btn-sm"
                            onClick={() => handleAddMember(user.id, user.displayName)}
                          >
                            Add
                          </button>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
              <div className="admin-modal-footer">
                <button className="admin-btn-secondary" onClick={() => setShowAddModal(false)}>Close</button>
              </div>
            </div>
          </div>
        )}
      </div>
    </AdminLayout>
  );
}