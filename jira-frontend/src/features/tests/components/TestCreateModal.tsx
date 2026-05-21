import React, { useState, useEffect } from 'react';
import combinedApi, { CreateTestRequest } from '../../../api/testApi';

interface TestCreateModalProps {
  projectId: string;
  folderId?: string;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (test: any) => void;
}

export const TestCreateModal: React.FC<TestCreateModalProps> = ({
  projectId,
  folderId,
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [formData, setFormData] = useState<CreateTestRequest>({
    projectId,
    name: '',
    description: '',
    testType: 'MANUAL',
    labels: [],
    steps: [],
  });
  const [newLabel, setNewLabel] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!formData.name?.trim()) {
      setError('Test name is required');
      return;
    }

    setSubmitting(true);
    try {
      const response = await combinedApi.createTest({
        ...formData,
        folderId,
      });
      onSuccess(response);
      onClose();
      setFormData({ projectId, name: '', description: '', testType: 'MANUAL', labels: [], steps: [] });
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create test');
    } finally {
      setSubmitting(false);
    }
  };

  const addLabel = () => {
    if (newLabel.trim() && !formData.labels?.includes(newLabel.trim())) {
      setFormData({
        ...formData,
        labels: [...(formData.labels || []), newLabel.trim()],
      });
      setNewLabel('');
    }
  };

  const removeLabel = (label: string) => {
    setFormData({
      ...formData,
      labels: formData.labels?.filter(l => l !== label) || [],
    });
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex items-center justify-center min-h-screen px-4">
        <div className="fixed inset-0 bg-black bg-opacity-50" onClick={onClose}></div>

        <div className="relative bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
          <div className="flex items-center justify-between p-4 border-b">
            <h3 className="text-lg font-semibold">Create New Test</h3>
            <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
              ✕
            </button>
          </div>

          <form onSubmit={handleSubmit} className="p-4">
            {error && (
              <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded text-red-600 text-sm">
                {error}
              </div>
            )}

            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-1">
                  Test Name <span className="text-red-600">*</span>
                </label>
                <input
                  type="text"
                  value={formData.name || ''}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="e.g., Verify user login with valid credentials"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Description</label>
                <textarea
                  value={formData.description || ''}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                  rows={3}
                  placeholder="Describe the test scenario..."
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-1">Test Type</label>
                  <select
                    value={formData.testType || 'MANUAL'}
                    onChange={(e) => setFormData({ ...formData, testType: e.target.value as any })}
                    className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="MANUAL">Manual</option>
                    <option value="AUTOMATED">Automated</option>
                    <option value="BDD">BDD (Gherkin)</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium mb-1">Priority</label>
                  <select
                    value={formData.priority || ''}
                    onChange={(e) => setFormData({ ...formData, priority: e.target.value })}
                    className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="">Select Priority</option>
                    <option value="CRITICAL">Critical</option>
                    <option value="HIGH">High</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="LOW">Low</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Labels</label>
                <div className="flex gap-2 mb-2 flex-wrap">
                  {formData.labels?.map((label) => (
                    <span
                      key={label}
                      className="inline-flex items-center px-2 py-1 bg-blue-100 text-blue-800 rounded text-sm"
                    >
                      {label}
                      <button
                        type="button"
                        onClick={() => removeLabel(label)}
                        className="ml-1 hover:text-red-600"
                      >
                        ×
                      </button>
                    </span>
                  ))}
                </div>
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={newLabel}
                    onChange={(e) => setNewLabel(e.target.value)}
                    onKeyPress={(e) => e.key === 'Enter' && (e.preventDefault(), addLabel())}
                    className="flex-1 px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="Add label..."
                  />
                  <button
                    type="button"
                    onClick={addLabel}
                    className="btn btn-secondary"
                  >
                    Add
                  </button>
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Requirement Keys</label>
                <input
                  type="text"
                  value={formData.requirementKeys?.join(', ') || ''}
                  onChange={(e) => setFormData({
                    ...formData,
                    requirementKeys: e.target.value.split(',').map(k => k.trim()).filter(Boolean),
                  })}
                  className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="PROJ-1, PROJ-2 (comma separated)"
                />
                <p className="text-xs text-gray-500 mt-1">
                  Link this test to requirement issue keys
                </p>
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Precondition</label>
                <textarea
                  value={formData.precondition || ''}
                  onChange={(e) => setFormData({ ...formData, precondition: e.target.value })}
                  className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                  rows={2}
                  placeholder="Prerequisites before running this test..."
                />
              </div>
            </div>

            <div className="flex justify-end gap-3 mt-6 pt-4 border-t">
              <button
                type="button"
                onClick={onClose}
                className="btn btn-secondary"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={submitting}
                className="btn btn-primary"
              >
                {submitting ? 'Creating...' : 'Create Test'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default TestCreateModal;