import { useEffect, useRef, useState } from 'react';
import { useTeams, useCreateTeam, useDeleteTeam } from '../../hooks/useTeams';
import { useAddTeamMember } from '../../hooks/useTeams';
import { CreateTeamRequest, AddTeamMemberRequest } from '../../../../api/planApi';
import { adminApi, User } from '../../../../api/adminApi';
import { appNotify } from '../../../../lib/appNotify';

interface TeamsViewProps {
  planId: string;
}

interface SelectedUser {
  id: string;
  email: string;
  displayName: string;
}

export default function TeamsView({ planId }: TeamsViewProps) {
  const [showCreateTeam, setShowCreateTeam] = useState(false);
  const [showAddMember, setShowAddMember] = useState<string | null>(null);
  const [teamName, setTeamName] = useState('');
  const [teamDescription, setTeamDescription] = useState('');
  const [memberRole, setMemberRole] = useState('');
  const [memberSearch, setMemberSearch] = useState('');
  const [memberResults, setMemberResults] = useState<User[]>([]);
  const [searchingMembers, setSearchingMembers] = useState(false);
  const [selectedUser, setSelectedUser] = useState<SelectedUser | null>(null);
  const [showUserDropdown, setShowUserDropdown] = useState(false);
  const searchDebounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const searchContainerRef = useRef<HTMLDivElement | null>(null);

  const { data: teams, isLoading } = useTeams(planId);
  const createTeamMutation = useCreateTeam();
  const deleteTeamMutation = useDeleteTeam();
  const addMemberMutation = useAddTeamMember();

  useEffect(() => {
    if (!showAddMember) {
      setMemberSearch('');
      setMemberResults([]);
      setSelectedUser(null);
      setShowUserDropdown(false);
      if (searchDebounceRef.current) {
        clearTimeout(searchDebounceRef.current);
        searchDebounceRef.current = null;
      }
      return;
    }
    // Load initial list when modal opens
    void runUserSearch('');
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [showAddMember]);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (searchContainerRef.current && !searchContainerRef.current.contains(event.target as Node)) {
        setShowUserDropdown(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const runUserSearch = async (query: string) => {
    setSearchingMembers(true);
    try {
      const res = await adminApi.getUsers({ search: query, status: 'ACTIVE' });
      // Backend returns a Spring Page object; extract content array safely
      const data = res.data as unknown;
      let users: User[] = [];
      if (Array.isArray(data)) {
        users = data as User[];
      } else if (data && typeof data === 'object' && 'content' in data && Array.isArray((data as { content: unknown }).content)) {
        users = (data as { content: User[] }).content;
      }
      setMemberResults(users);
      setShowUserDropdown(true);
    } catch (error) {
      console.error('User search failed:', error);
      setMemberResults([]);
    } finally {
      setSearchingMembers(false);
    }
  };

  const handleMemberSearchChange = (value: string) => {
    setMemberSearch(value);
    setSelectedUser(null);
    if (searchDebounceRef.current) {
      clearTimeout(searchDebounceRef.current);
    }
    searchDebounceRef.current = setTimeout(() => {
      void runUserSearch(value.trim());
    }, 300);
  };

  const handleSelectUser = (user: User) => {
    setSelectedUser({ id: user.id, email: user.email, displayName: user.displayName });
    setMemberSearch(`${user.displayName} <${user.email}>`);
    setShowUserDropdown(false);
  };

  const handleCreateTeam = (e: React.FormEvent) => {
    e.preventDefault();
    const request: CreateTeamRequest = { name: teamName, description: teamDescription };
    createTeamMutation.mutate(
      { planId, data: request },
      {
        onSuccess: () => {
          setShowCreateTeam(false);
          setTeamName('');
          setTeamDescription('');
        },
        onError: (error: Error) => {
          appNotify.error(error.message || 'Failed to create team');
        },
      }
    );
  };

  const handleAddMember = (teamId: string) => {
    if (!selectedUser) {
      appNotify.error('Please select a user from the list');
      return;
    }
    const request: AddTeamMemberRequest = {
      userId: selectedUser.id,
      userName: selectedUser.displayName,
      role: memberRole,
    };
    addMemberMutation.mutate(
      { planId, teamId, data: request },
      {
        onSuccess: () => {
          setShowAddMember(null);
          setMemberRole('');
        },
        onError: (error: Error) => {
          appNotify.error(error.message || 'Failed to add member');
        },
      }
    );
  };

  const handleDeleteTeam = (teamId: string) => {
    deleteTeamMutation.mutate({ planId, teamId }, {
      onError: (error: Error) => {
        appNotify.error(error.message || 'Failed to delete team');
      },
    });
  };

  if (isLoading) {
    return (
      <div className="ab-loading">
        <div className="ab-spinner"></div>
      </div>
    );
  }

  return (
    <div className="ab-teams-view">
      <div className="ab-toolbar">
        <h3 className="ab-section-title">Teams ({teams?.length || 0})</h3>
        <button className="ab-btn ab-btn-primary" onClick={() => setShowCreateTeam(true)}>
          <span className="ab-icon-plus"></span>
          Create Team
        </button>
      </div>

      {teams && teams.length > 0 ? (
        <div className="ab-grid ab-grid-2">
          {teams.map((team) => (
            <div key={team.id} className="ab-card ab-team-card">
              <div className="ab-team-card-header">
                <div className="ab-team-icon">👥</div>
                <div className="ab-team-info">
                  <h4 className="ab-team-name">{team.name}</h4>
                  <p className="ab-team-description">{team.description || 'No description'}</p>
                </div>
                <button className="ab-btn-icon-sm" onClick={() => handleDeleteTeam(team.id)}>×</button>
              </div>
              <div className="ab-team-stats">
                <span>{team.memberCount} members</span>
                <span>{team.totalCapacity}h capacity</span>
              </div>
              <div className="ab-team-members">
                {team.members.map((member) => (
                  <div key={member.id} className="ab-team-member">
                    <div className="ab-member-avatar">{member.userName?.charAt(0) || '?'}</div>
                    <div className="ab-member-info">
                      <span className="ab-member-name">{member.userName || 'Unknown'}</span>
                      <span className="ab-member-role">{member.role || 'Member'}</span>
                    </div>
                    <span className="ab-member-capacity">{member.capacityHours}h</span>
                  </div>
                ))}
              </div>
              <button className="ab-btn ab-btn-sm ab-btn-secondary" onClick={() => setShowAddMember(team.id)}>
                Add Member
              </button>
            </div>
          ))}
        </div>
      ) : (
        <div className="ab-empty-state">
          <div className="ab-empty-state-icon">👥</div>
          <h3 className="ab-empty-state-title">No teams yet</h3>
          <p className="ab-empty-state-description">Create teams to assign work and track capacity</p>
        </div>
      )}

      {showCreateTeam && (
        <div className="ab-modal-overlay" onClick={() => setShowCreateTeam(false)}>
          <div className="ab-modal" onClick={(e) => e.stopPropagation()}>
            <div className="ab-modal-header">
              <h2 className="ab-modal-title">Create Team</h2>
              <button className="ab-btn-icon" onClick={() => setShowCreateTeam(false)}>
                <span className="ab-icon-close"></span>
              </button>
            </div>
            <form onSubmit={handleCreateTeam}>
              <div className="ab-modal-body">
                <div className="ab-form-group">
                  <label className="ab-label">Team Name *</label>
                  <input
                    type="text"
                    className="ab-input"
                    value={teamName}
                    onChange={(e) => setTeamName(e.target.value)}
                    placeholder="Enter team name"
                    required
                  />
                </div>
                <div className="ab-form-group">
                  <label className="ab-label">Description</label>
                  <textarea
                    className="ab-textarea"
                    value={teamDescription}
                    onChange={(e) => setTeamDescription(e.target.value)}
                    placeholder="Describe this team"
                    rows={3}
                  />
                </div>
              </div>
              <div className="ab-modal-footer">
                <button type="button" className="ab-btn ab-btn-secondary" onClick={() => setShowCreateTeam(false)}>
                  Cancel
                </button>
                <button type="submit" className="ab-btn ab-btn-primary" disabled={createTeamMutation.isPending}>
                  {createTeamMutation.isPending ? 'Creating...' : 'Create Team'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {showAddMember && (
        <div className="ab-modal-overlay" onClick={() => setShowAddMember(null)}>
          <div className="ab-modal" onClick={(e) => e.stopPropagation()}>
            <div className="ab-modal-header">
              <h2 className="ab-modal-title">Add Team Member</h2>
              <button className="ab-btn-icon" onClick={() => setShowAddMember(null)}>
                <span className="ab-icon-close"></span>
              </button>
            </div>
            <form onSubmit={(e) => { e.preventDefault(); handleAddMember(showAddMember); }}>
              <div className="ab-modal-body">
                <div className="ab-form-group" ref={searchContainerRef}>
                  <label className="ab-label">User *</label>
                  <input
                    type="text"
                    className="ab-input"
                    value={memberSearch}
                    onChange={(e) => handleMemberSearchChange(e.target.value)}
                    onFocus={() => setShowUserDropdown(true)}
                    placeholder="Search by name or email"
                    autoComplete="off"
                    required
                  />
                  {showUserDropdown && (
                    <div className="ab-user-dropdown">
                      {searchingMembers ? (
                        <div className="ab-user-dropdown-status">Searching...</div>
                      ) : memberResults.length > 0 ? (
                        memberResults.slice(0, 20).map((user) => (
                          <div
                            key={user.id}
                            className="ab-user-dropdown-item"
                            onClick={() => handleSelectUser(user)}
                          >
                            <div className="ab-user-dropdown-avatar">
                              {user.displayName?.charAt(0) || user.email?.charAt(0) || '?'}
                            </div>
                            <div className="ab-user-dropdown-info">
                              <span className="ab-user-dropdown-name">{user.displayName || user.username}</span>
                              <span className="ab-user-dropdown-email">{user.email}</span>
                            </div>
                          </div>
                        ))
                      ) : (
                        <div className="ab-user-dropdown-status">No users found</div>
                      )}
                    </div>
                  )}
                </div>
                <div className="ab-form-group">
                  <label className="ab-label">Role</label>
                  <input
                    type="text"
                    className="ab-input"
                    value={memberRole}
                    onChange={(e) => setMemberRole(e.target.value)}
                    placeholder="e.g., Developer, QA"
                  />
                </div>
              </div>
              <div className="ab-modal-footer">
                <button type="button" className="ab-btn ab-btn-secondary" onClick={() => setShowAddMember(null)}>
                  Cancel
                </button>
                <button type="submit" className="ab-btn ab-btn-primary" disabled={addMemberMutation.isPending || !selectedUser}>
                  {addMemberMutation.isPending ? 'Adding...' : 'Add Member'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
