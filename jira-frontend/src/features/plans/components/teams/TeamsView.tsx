import { useState } from 'react';
import { useTeams, useCreateTeam, useDeleteTeam } from '../../hooks/useTeams';
import { useAddTeamMember } from '../../hooks/useTeams';
import { CreateTeamRequest, AddTeamMemberRequest } from '../../../../api/planApi';

interface TeamsViewProps {
  planId: string;
}

export default function TeamsView({ planId }: TeamsViewProps) {
  const [showCreateTeam, setShowCreateTeam] = useState(false);
  const [showAddMember, setShowAddMember] = useState<string | null>(null);
  const [teamName, setTeamName] = useState('');
  const [teamDescription, setTeamDescription] = useState('');
  const [memberUserId, setMemberUserId] = useState('');
  const [memberRole, setMemberRole] = useState('');

  const { data: teams, isLoading } = useTeams(planId);
  const createTeamMutation = useCreateTeam();
  const deleteTeamMutation = useDeleteTeam();
  const addMemberMutation = useAddTeamMember();

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
      }
    );
  };

  const handleAddMember = (teamId: string) => {
    const request: AddTeamMemberRequest = { userId: memberUserId, role: memberRole };
    addMemberMutation.mutate(
      { planId, teamId, data: request },
      {
        onSuccess: () => {
          setShowAddMember(null);
          setMemberUserId('');
          setMemberRole('');
        },
      }
    );
  };

  const handleDeleteTeam = (teamId: string) => {
    if (confirm('Delete this team?')) {
      deleteTeamMutation.mutate({ planId, teamId });
    }
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
                <div className="ab-form-group">
                  <label className="ab-label">User ID *</label>
                  <input
                    type="text"
                    className="ab-input"
                    value={memberUserId}
                    onChange={(e) => setMemberUserId(e.target.value)}
                    placeholder="Enter user ID"
                    required
                  />
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
                <button type="submit" className="ab-btn ab-btn-primary" disabled={addMemberMutation.isPending}>
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
