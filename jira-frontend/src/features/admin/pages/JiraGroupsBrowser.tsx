import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useJiraGroups, useCreateJiraGroup, useDeleteJiraGroup } from '../hooks/useAdminApi';
import type { JiraGroup } from '../hooks/useAdminApi';
import './JiraGroupsBrowser.css';

export default function JiraGroupsBrowser() {
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const pageSize = 20;
  const [newGroupName, setNewGroupName] = useState('');

  const { data, isLoading, refetch } = useJiraGroups({
    search: search || undefined,
    page,
    size: pageSize,
  });

  const createGroup = useCreateJiraGroup();
  const deleteGroup = useDeleteJiraGroup();

  const handleFilter = () => {
    setPage(0);
    refetch();
  };

  const handleReset = () => {
    setSearch('');
    setPage(0);
  };

  const handleCreateGroup = async () => {
    if (!newGroupName.trim()) return;
    try {
      await createGroup.mutateAsync({ name: newGroupName.trim() });
      setNewGroupName('');
      refetch();
    } catch (error) {
      console.error('Failed to create group:', error);
    }
  };

  const handleDeleteGroup = async (groupId: string, groupName: string) => {
    if (!confirm(`Are you sure you want to delete the group "${groupName}"?`)) return;
    try {
      await deleteGroup.mutateAsync(groupId);
      refetch();
    } catch (error) {
      console.error('Failed to delete group:', error);
    }
  };

  return (
      <div className="groups-browser">
        <div className="groups-browser-header">
          <h1 className="groups-browser-title">Groups</h1>
        </div>

        <div className="groups-browser-toolbar">
          {/* Filter Section */}
          <div className="filter-section">
            <div className="filter-field">
              <label className="filter-label">Name contains</label>
              <input
                type="text"
                className="filter-input"
                placeholder="Filter groups"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleFilter()}
              />
            </div>
            <div className="filter-field">
              <label className="filter-label">Groups per page</label>
              <select className="filter-select" defaultValue={pageSize}>
                <option value="20">20</option>
                <option value="50">50</option>
                <option value="100">100</option>
              </select>
            </div>
            <div className="filter-actions">
              <button className="btn-primary" onClick={handleFilter}>Filter</button>
              <button className="btn-link" onClick={handleReset}>Reset filter</button>
            </div>
          </div>

          {/* Add Group Section */}
          <div className="add-group-section">
            <div className="filter-field">
              <label className="filter-label">Group name</label>
              <input
                type="text"
                className="filter-input"
                placeholder="Enter group name"
                value={newGroupName}
                onChange={(e) => setNewGroupName(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleCreateGroup()}
              />
            </div>
            <button
              className="btn-primary"
              onClick={handleCreateGroup}
              disabled={!newGroupName.trim() || createGroup.isPending}
            >
              {createGroup.isPending ? 'Adding...' : 'Add group'}
            </button>
          </div>
        </div>

        <div className="groups-browser-pagination-info">
          Displaying {data?.content?.length || 0} of {data?.totalElements || 0} groups
        </div>

        <table className="groups-table">
          <thead>
            <tr>
              <th>Group name</th>
              <th>Users</th>
              <th>Permission schemes</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <>
                {[...Array(8)].map((_, i) => (
                  <tr key={i}>
                    <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: '60%', borderRadius: 'var(--sa-radius-sm)' }} /></td>
                    <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: 40, borderRadius: 'var(--sa-radius-sm)' }} /></td>
                    <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: '40%', borderRadius: 'var(--sa-radius-sm)' }} /></td>
                    <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: 80, borderRadius: 'var(--sa-radius-sm)' }} /></td>
                  </tr>
                ))}
              </>
            ) : data?.content?.length === 0 ? (
              <tr>
                <td colSpan={4} className="empty-cell">No groups found</td>
              </tr>
            ) : (
              data?.content?.map((group) => (
                <GroupRow
                  key={group.id}
                  group={group}
                  onDelete={handleDeleteGroup}
                />
              ))
            )}
          </tbody>
        </table>
      </div>
  );
}

function GroupRow({
  group,
  onDelete,
}: {
  group: JiraGroup;
  onDelete: (id: string, name: string) => void;
}) {
  return (
    <tr className="group-row">
      <td>
        <div className="group-name-cell">
          <Link to={`/admin/groups/view?name=${encodeURIComponent(group.name)}`} className="group-name-link">
            {group.name}
          </Link>
          {group.isSystem && (
            <span className="group-badges">
              <span className="badge badge-admin">ADMIN</span>
              <span className="badge badge-jira">JIRA SOFTWARE</span>
            </span>
          )}
        </div>
      </td>
      <td>
        <Link to={`/admin/users?group=${encodeURIComponent(group.name)}`} className="user-count-link">
          {group.userCount}
        </Link>
      </td>
      <td>
        <span className="scheme-count">
          {group.permissionSchemes?.length || 0} schemes
        </span>
      </td>
      <td>
        <div className="action-links">
          <Link to={`/admin/groups/members/${group.id}`} className="action-link">
            Edit members
          </Link>
          {!group.isSystem && (
            <button
              className="action-link action-link-danger"
              onClick={() => onDelete(group.id, group.name)}
            >
              Delete
            </button>
          )}
        </div>
      </td>
    </tr>
  );
}