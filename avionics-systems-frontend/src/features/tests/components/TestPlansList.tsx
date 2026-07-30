import React, { useState, useEffect } from 'react';
import combinedApi, { TestPlanResponse } from '../../../api/testApi';
import { Plus, Edit2, Trash2, X, Check, Calendar, Play, Loader2, CheckCircle, AlertCircle } from 'lucide-react';

// Toast notification
const Toast: React.FC<{ message: string; type: 'success' | 'error'; onClose: () => void }> = ({ message, type, onClose }) => {
  useEffect(() => {
    const timer = setTimeout(onClose, 3000);
    return () => clearTimeout(timer);
  }, [onClose]);

  return (
    <div className={`fixed bottom-4 right-4 ${type === 'success' ? 'bg-green-500' : 'bg-red-500'} text-white px-4 py-3 rounded-lg shadow-lg flex items-center gap-2 z-50`}>
      {type === 'success' ? <CheckCircle className="w-5 h-5" /> : <AlertCircle className="w-5 h-5" />}
      <span>{message}</span>
      <button onClick={onClose} className="ml-2 hover:opacity-80">
        <X className="w-4 h-4" />
      </button>
    </div>
  );
};

interface TestPlansListProps {
  projectId: string;
}

interface TestPlanFormData {
  name: string;
  description: string;
  startDate?: string;
  endDate?: string;
}

export const TestPlansList: React.FC<TestPlansListProps> = ({ projectId }) => {
  const [testPlans, setTestPlans] = useState<TestPlanResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [formData, setFormData] = useState<TestPlanFormData>({
    name: '',
    description: '',
    startDate: '',
    endDate: '',
  });
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null);

  useEffect(() => {
    loadTestPlans();
  }, [projectId]);

  const loadTestPlans = async () => {
    setLoading(true);
    try {
      const data = await combinedApi.getTestPlansByProject(projectId);
      setTestPlans(data);
    } catch (error) {
      console.error('Failed to load test plans:', error);
      setToast({ message: 'Failed to load test plans', type: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async () => {
    if (!formData.name.trim()) return;
    setSaving(true);
    try {
      const newPlan = await combinedApi.createTestPlan({
        projectId,
        name: formData.name,
        description: formData.description,
        startDate: formData.startDate,
        endDate: formData.endDate,
      });
      setTestPlans([...testPlans, newPlan]);
      setShowCreateModal(false);
      setFormData({ name: '', description: '', startDate: '', endDate: '' });
      setToast({ message: 'Test plan created successfully', type: 'success' });
    } catch (error) {
      console.error('Failed to create test plan:', error);
      setToast({ message: 'Failed to create test plan', type: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleUpdate = async (id: string) => {
    if (!formData.name.trim()) return;
    setSaving(true);
    try {
      const updated = await combinedApi.updateTestPlan(id, {
        name: formData.name,
        description: formData.description,
        startDate: formData.startDate,
        endDate: formData.endDate,
      });
      setTestPlans(testPlans.map(p => p.id === id ? updated : p));
      setEditingId(null);
      setFormData({ name: '', description: '', startDate: '', endDate: '' });
      setToast({ message: 'Test plan updated successfully', type: 'success' });
    } catch (error) {
      console.error('Failed to update test plan:', error);
      setToast({ message: 'Failed to update test plan', type: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Are you sure you want to delete this test plan?')) return;
    try {
      await combinedApi.deleteTestPlan(id);
      setTestPlans(testPlans.filter(p => p.id !== id));
      setToast({ message: 'Test plan deleted successfully', type: 'success' });
    } catch (error) {
      console.error('Failed to delete test plan:', error);
      setToast({ message: 'Failed to delete test plan', type: 'error' });
    }
  };

  const handleStart = async (id: string) => {
    try {
      await combinedApi.startTestPlan(id);
      loadTestPlans();
      setToast({ message: 'Test plan started', type: 'success' });
    } catch (error) {
      console.error('Failed to start test plan:', error);
      setToast({ message: 'Failed to start test plan', type: 'error' });
    }
  };

  const startEdit = (testPlan: TestPlanResponse) => {
    setEditingId(testPlan.id);
    setFormData({
      name: testPlan.name,
      description: testPlan.description || '',
      startDate: testPlan.startDate || '',
      endDate: testPlan.endDate || '',
    });
  };

  const cancelEdit = () => {
    setEditingId(null);
    setFormData({ name: '', description: '', startDate: '', endDate: '' });
  };

  const getStatusBadge = (status?: string) => {
    const baseClasses = 'px-2 py-1 rounded-full text-xs font-medium';
    switch (status) {
      case 'IN_PROGRESS':
        return <span className={`${baseClasses} bg-blue-100 text-blue-800`}>In Progress</span>;
      case 'COMPLETED':
        return <span className={`${baseClasses} bg-green-100 text-green-800`}>Completed</span>;
      case 'PLANNED':
        return <span className={`${baseClasses} bg-gray-100 text-gray-600`}>Planned</span>;
      default:
        return <span className={`${baseClasses} bg-gray-100 text-gray-600`}>{status || 'Draft'}</span>;
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center py-12">
        <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
      </div>
    );
  }

  return (
    <div className="test-plans-list">
      {/* Toast notification */}
      {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-lg font-semibold">Test Plans</h2>
        <button
          onClick={() => setShowCreateModal(true)}
          className="btn btn-secondary flex items-center gap-2"
        >
          <Plus className="w-4 h-4" />
          + Create Test Plan
        </button>
      </div>

      {testPlans.length === 0 ? (
        <div className="text-center py-12 text-gray-500">
          <p className="text-lg">No test plans found</p>
          <p className="text-sm mt-2">Create a test plan to organize test execution cycles</p>
        </div>
      ) : (
        <div className="grid gap-4">
          {testPlans.map((testPlan) => (
            <div key={testPlan.id} className="bg-white border rounded-lg p-4">
              {editingId === testPlan.id ? (
                <div className="space-y-3">
                  <input
                    type="text"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    className="w-full px-3 py-2 border rounded"
                    placeholder="Test Plan Name"
                  />
                  <textarea
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    className="w-full px-3 py-2 border rounded"
                    rows={2}
                    placeholder="Description"
                  />
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm text-gray-500 mb-1">Start Date</label>
                      <input
                        type="date"
                        value={formData.startDate || ''}
                        onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
                        className="w-full px-3 py-2 border rounded"
                      />
                    </div>
                    <div>
                      <label className="block text-sm text-gray-500 mb-1">End Date</label>
                      <input
                        type="date"
                        value={formData.endDate || ''}
                        onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
                        className="w-full px-3 py-2 border rounded"
                      />
                    </div>
                  </div>
                  <div className="flex gap-2 justify-end">
                    <button onClick={cancelEdit} disabled={saving} className="btn btn-secondary flex items-center gap-1">
                      <X className="w-4 h-4" /> Cancel
                    </button>
                    <button
                      onClick={() => handleUpdate(testPlan.id)}
                      disabled={saving || !formData.name.trim()}
                      className="btn btn-primary flex items-center gap-1"
                    >
                      {saving && <Loader2 className="w-4 h-4 animate-spin" />}
                      <Check className="w-4 h-4" /> Save
                    </button>
                  </div>
                </div>
              ) : (
                <div className="flex justify-between items-start">
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <h3 className="font-medium text-gray-900">{testPlan.name}</h3>
                      {getStatusBadge(testPlan.status)}
                    </div>
                    <p className="text-sm text-gray-500 mt-1">
                      {testPlan.description || 'No description'}
                    </p>
                    <div className="flex items-center gap-4 mt-2 text-sm text-gray-500">
                      <span>{testPlan.testSetCount || testPlan.totalTests || 0} test sets</span>
                      {testPlan.startDate && (
                        <span className="flex items-center gap-1">
                          <Calendar className="w-3 h-3" />
                          {new Date(testPlan.startDate).toLocaleDateString()}
                          {testPlan.endDate && ` - ${new Date(testPlan.endDate).toLocaleDateString()}`}
                        </span>
                      )}
                    </div>
                  </div>
                  <div className="flex gap-2">
                    {testPlan.status !== 'IN_PROGRESS' && (
                      <button
                        onClick={() => handleStart(testPlan.id)}
                        className="p-2 hover:bg-green-100 rounded text-green-600 flex items-center gap-1"
                        aria-label="Start test plan execution"
                        title="Start Execution"
                      >
                        <Play className="w-4 h-4" />
                      </button>
                    )}
                    <button
                      onClick={() => startEdit(testPlan)}
                      className="p-2 hover:bg-gray-100 rounded text-gray-600"
                      aria-label="Edit test plan"
                    >
                      <Edit2 className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => handleDelete(testPlan.id)}
                      className="p-2 hover:bg-gray-100 rounded text-red-600"
                      aria-label="Delete test plan"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Create Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 overflow-y-auto">
          <div className="flex items-center justify-center min-h-screen px-4">
            <div className="fixed inset-0 bg-black bg-opacity-50" onClick={() => setShowCreateModal(false)}></div>
            <div className="relative bg-white rounded-lg shadow-xl max-w-md w-full p-6">
              <div className="flex justify-between items-center mb-4">
                <h3 className="text-lg font-semibold">Create Test Plan</h3>
                <button onClick={() => setShowCreateModal(false)} className="text-gray-400 hover:text-gray-600">
                  <X className="w-5 h-5" />
                </button>
              </div>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium mb-1">Name *</label>
                  <input
                    type="text"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="e.g., Sprint 45 Test Cycle"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">Description</label>
                  <textarea
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                    rows={3}
                    placeholder="Describe this test plan..."
                  />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium mb-1">Start Date</label>
                    <input
                      type="date"
                      value={formData.startDate || ''}
                      onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
                      className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium mb-1">End Date</label>
                    <input
                      type="date"
                      value={formData.endDate || ''}
                      onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
                      className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                  </div>
                </div>
                <div className="flex justify-end gap-3 pt-4">
                  <button onClick={() => setShowCreateModal(false)} disabled={saving} className="btn btn-secondary">
                    Cancel
                  </button>
                  <button onClick={handleCreate} disabled={saving || !formData.name.trim()} className="btn btn-primary flex items-center gap-2">
                    {saving && <Loader2 className="w-4 h-4 animate-spin" />}
                    Create
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default TestPlansList;