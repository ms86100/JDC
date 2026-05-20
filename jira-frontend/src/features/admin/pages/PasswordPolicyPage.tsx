import React, { useState } from 'react';
import AdminLayout from '../components/AdminLayout';
import {
  usePasswordPolicies,
  useCreatePasswordPolicy,
  useUpdatePasswordPolicy,
  useDeletePasswordPolicy,
  useSetDefaultPasswordPolicy,
  PasswordPolicy,
} from '../hooks/useAdminApi';
import './PasswordPolicyPage.css';

interface PolicyFormData {
  name: string;
  minLength: number;
  requireUppercase: boolean;
  requireLowercase: boolean;
  requireDigit: boolean;
  requireSpecial: boolean;
  maxAge: number;
  preventReuse: number;
}

export default function PasswordPolicyPage() {
  const [showModal, setShowModal] = useState(false);
  const [modalMode, setModalMode] = useState<'create' | 'edit'>('create');
  const [selectedPolicy, setSelectedPolicy] = useState<PasswordPolicy | null>(null);
  const [formData, setFormData] = useState<PolicyFormData>({
    name: '',
    minLength: 8,
    requireUppercase: true,
    requireLowercase: true,
    requireDigit: true,
    requireSpecial: false,
    maxAge: 0,
    preventReuse: 5,
  });
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const { data: policies, isLoading } = usePasswordPolicies();
  const createPolicy = useCreatePasswordPolicy();
  const updatePolicy = useUpdatePasswordPolicy();
  const deletePolicy = useDeletePasswordPolicy();
  const setDefaultPolicy = useSetDefaultPasswordPolicy();

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

  const openCreateModal = () => {
    setModalMode('create');
    setSelectedPolicy(null);
    setFormData({
      name: '',
      minLength: 8,
      requireUppercase: true,
      requireLowercase: true,
      requireDigit: true,
      requireSpecial: false,
      maxAge: 0,
      preventReuse: 5,
    });
    setShowModal(true);
  };

  const openEditModal = (policy: PasswordPolicy) => {
    setModalMode('edit');
    setSelectedPolicy(policy);
    setFormData({
      name: policy.name,
      minLength: policy.minLength,
      requireUppercase: policy.requireUppercase,
      requireLowercase: policy.requireLowercase,
      requireDigit: policy.requireDigit,
      requireSpecial: policy.requireSpecial,
      maxAge: policy.maxAge,
      preventReuse: policy.preventReuse,
    });
    setShowModal(true);
  };

  const closeModal = () => {
    setShowModal(false);
    setSelectedPolicy(null);
    setError(null);
  };

  const handleCreate = async () => {
    if (!formData.name.trim()) {
      setError('Policy name is required');
      return;
    }
    try {
      await createPolicy.mutateAsync(formData);
      showMessage(`Password policy "${formData.name}" created successfully`);
      closeModal();
    } catch (err: any) {
      showMessage(err?.message || 'Failed to create policy', true);
    }
  };

  const handleUpdate = async () => {
    if (!formData.name.trim()) {
      setError('Policy name is required');
      return;
    }
    try {
      await updatePolicy.mutateAsync({ id: selectedPolicy!.id, data: formData });
      showMessage(`Password policy "${formData.name}" updated successfully`);
      closeModal();
    } catch (err: any) {
      showMessage(err?.message || 'Failed to update policy', true);
    }
  };

  const handleDelete = async (policy: PasswordPolicy) => {
    if (policy.isDefault) {
      showMessage('Cannot delete the default password policy', true);
      return;
    }
    if (!confirm(`Are you sure you want to delete the password policy "${policy.name}"?`)) return;
    try {
      await deletePolicy.mutateAsync(policy.id);
      showMessage(`Password policy "${policy.name}" deleted successfully`);
    } catch (err: any) {
      showMessage(err?.message || 'Failed to delete policy', true);
    }
  };

  const handleSetDefault = async (policy: PasswordPolicy) => {
    try {
      await setDefaultPolicy.mutateAsync(policy.id);
      showMessage(`"${policy.name}" is now the default password policy`);
    } catch (err: any) {
      showMessage(err?.message || 'Failed to set default policy', true);
    }
  };

  return (
    <AdminLayout>
      <div className="admin-page">
        <div className="admin-page-header">
          <h1 className="admin-page-title">Password Policies</h1>
          <p className="admin-page-description">
            Configure password requirements and security settings for user accounts.
          </p>
        </div>

        {error && <div className="admin-alert admin-alert-error">{error}</div>}
        {success && <div className="admin-alert admin-alert-success">{success}</div>}

        <div className="admin-stats-grid">
          <div className="admin-stat-card">
            <div className="admin-stat-label">Total Policies</div>
            <div className="admin-stat-value">{policies?.length || 0}</div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-label">Default Policy</div>
            <div className="admin-stat-value">{policies?.find(p => p.isDefault)?.name || 'None'}</div>
          </div>
        </div>

        <div className="admin-toolbar">
          <div className="admin-toolbar-left">
            <input
              type="text"
              placeholder="Search policies..."
              className="admin-search-input-toolbar"
            />
          </div>
          <div className="admin-toolbar-right">
            <button className="admin-btn-primary" onClick={openCreateModal}>Add Password Policy</button>
          </div>
        </div>

        <div className="admin-table-container">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Policy Name</th>
                <th>Requirements</th>
                <th>Password Age</th>
                <th>Reuse Policy</th>
                <th>Type</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr><td colSpan={6} className="loading-cell">Loading...</td></tr>
              ) : policies?.length === 0 ? (
                <tr><td colSpan={6} className="empty-cell">No password policies found. Click "Add Password Policy" to create one.</td></tr>
              ) : (
                policies?.map((policy) => (
                  <tr key={policy.id}>
                    <td>
                      <div className="policy-cell">
                        <span className="policy-name">{policy.name}</span>
                        {policy.isDefault && <span className="policy-default-badge">Default</span>}
                      </div>
                    </td>
                    <td>
                      <div className="requirements-list">
                        <span className={policy.minLength >= 8 ? 'req-met' : 'req-not-met'}>
                          Min {policy.minLength} chars
                        </span>
                        <span className={policy.requireUppercase ? 'req-met' : 'req-not-met'}>A-Z</span>
                        <span className={policy.requireLowercase ? 'req-met' : 'req-not-met'}>a-z</span>
                        <span className={policy.requireDigit ? 'req-met' : 'req-not-met'}>0-9</span>
                        <span className={policy.requireSpecial ? 'req-met' : 'req-not-met'}>!@#$</span>
                      </div>
                    </td>
                    <td>{policy.maxAge === 0 ? 'Never expires' : `Every ${policy.maxAge} days`}</td>
                    <td>{policy.preventReuse === 0 ? 'Allow reuse' : `Last ${policy.preventReuse} passwords`}</td>
                    <td>
                      {policy.isDefault ? (
                        <span className="policy-type-badge policy-type-default">Default</span>
                      ) : (
                        <span className="policy-type-badge policy-type-custom">Custom</span>
                      )}
                    </td>
                    <td>
                      <div className="action-buttons">
                        <button className="admin-btn-secondary" onClick={() => openEditModal(policy)}>Edit</button>
                        {!policy.isDefault && (
                          <>
                            <button className="admin-btn-secondary" onClick={() => handleSetDefault(policy)}>Set Default</button>
                            <button className="admin-btn-danger" onClick={() => handleDelete(policy)}>Delete</button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Create/Edit Modal */}
        {showModal && (
          <div className="admin-modal-overlay" onClick={closeModal}>
            <div className="admin-modal admin-modal-wide" onClick={(e) => e.stopPropagation()}>
              <div className="admin-modal-header">
                <h2 className="admin-modal-title">
                  {modalMode === 'create' ? 'Add Password Policy' : 'Edit Password Policy'}
                </h2>
                <button className="admin-modal-close" onClick={closeModal}>×</button>
              </div>
              <div className="admin-modal-body">
                <div className="admin-form-group">
                  <label className="admin-form-label admin-form-label-required">Policy Name</label>
                  <input
                    type="text"
                    className="admin-form-input"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    placeholder="e.g., Standard Policy, Strict Policy"
                  />
                </div>

                <h3 className="form-section-title">Password Requirements</h3>

                <div className="form-row">
                  <div className="admin-form-group">
                    <label className="admin-form-label">Minimum Length</label>
                    <input
                      type="number"
                      className="admin-form-input admin-form-input-narrow"
                      value={formData.minLength}
                      onChange={(e) => setFormData({ ...formData, minLength: parseInt(e.target.value) || 8 })}
                      min={6}
                      max={128}
                    />
                  </div>
                </div>

                <div className="form-checkbox-group">
                  <label className="form-checkbox-label">
                    <input
                      type="checkbox"
                      checked={formData.requireUppercase}
                      onChange={(e) => setFormData({ ...formData, requireUppercase: e.target.checked })}
                    />
                    Require at least one uppercase letter (A-Z)
                  </label>
                  <label className="form-checkbox-label">
                    <input
                      type="checkbox"
                      checked={formData.requireLowercase}
                      onChange={(e) => setFormData({ ...formData, requireLowercase: e.target.checked })}
                    />
                    Require at least one lowercase letter (a-z)
                  </label>
                  <label className="form-checkbox-label">
                    <input
                      type="checkbox"
                      checked={formData.requireDigit}
                      onChange={(e) => setFormData({ ...formData, requireDigit: e.target.checked })}
                    />
                    Require at least one digit (0-9)
                  </label>
                  <label className="form-checkbox-label">
                    <input
                      type="checkbox"
                      checked={formData.requireSpecial}
                      onChange={(e) => setFormData({ ...formData, requireSpecial: e.target.checked })}
                    />
                    Require at least one special character (!@#$%^&*)
                  </label>
                </div>

                <h3 className="form-section-title">Password Age</h3>

                <div className="form-row">
                  <div className="admin-form-group">
                    <label className="admin-form-label">Maximum Password Age (days)</label>
                    <input
                      type="number"
                      className="admin-form-input admin-form-input-narrow"
                      value={formData.maxAge}
                      onChange={(e) => setFormData({ ...formData, maxAge: parseInt(e.target.value) || 0 })}
                      min={0}
                      max={365}
                    />
                    <span className="form-help">Set to 0 for passwords that never expire</span>
                  </div>
                </div>

                <h3 className="form-section-title">Password History</h3>

                <div className="form-row">
                  <div className="admin-form-group">
                    <label className="admin-form-label">Prevent Password Reuse</label>
                    <input
                      type="number"
                      className="admin-form-input admin-form-input-narrow"
                      value={formData.preventReuse}
                      onChange={(e) => setFormData({ ...formData, preventReuse: parseInt(e.target.value) || 0 })}
                      min={0}
                      max={24}
                    />
                    <span className="form-help">Set to 0 to allow reuse of any previous password</span>
                  </div>
                </div>
              </div>
              <div className="admin-modal-footer">
                <button className="admin-btn-secondary" onClick={closeModal}>Cancel</button>
                <button
                  className="admin-btn-primary"
                  onClick={modalMode === 'create' ? handleCreate : handleUpdate}
                  disabled={modalMode === 'create' ? createPolicy.isPending : updatePolicy.isPending}
                >
                  {modalMode === 'create' ? 'Create' : 'Update'}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </AdminLayout>
  );
}