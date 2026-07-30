import React, { useState, useEffect } from 'react';
import combinedApi, { TestSetResponse } from '../../../api/testApi';
import { Plus, Edit2, Trash2, X, Check, Loader2, CheckCircle, AlertCircle } from 'lucide-react';

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

interface TestSetsListProps {
  projectId: string;
}

interface TestSetFormData {
  name: string;
  description: string;
  testType?: string;
}

export const TestSetsList: React.FC<TestSetsListProps> = ({ projectId }) => {
  const [testSets, setTestSets] = useState<TestSetResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [formData, setFormData] = useState<TestSetFormData>({ name: '', description: '' });
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null);

  useEffect(() => {
    loadTestSets();
  }, [projectId]);

  const loadTestSets = async () => {
    setLoading(true);
    try {
      const data = await combinedApi.getTestSetsByProject(projectId);
      setTestSets(data);
    } catch (error) {
      console.error('Failed to load test sets:', error);
      setToast({ message: 'Failed to load test sets', type: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async () => {
    if (!formData.name.trim()) return;
    setSaving(true);
    try {
      const newSet = await combinedApi.createTestSet({
        projectId,
        name: formData.name,
        description: formData.description,
      });
      setTestSets([...testSets, newSet]);
      setShowCreateModal(false);
      setFormData({ name: '', description: '' });
      setToast({ message: 'Test set created successfully', type: 'success' });
    } catch (error) {
      console.error('Failed to create test set:', error);
      setToast({ message: 'Failed to create test set', type: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleUpdate = async (id: string) => {
    if (!formData.name.trim()) return;
    setSaving(true);
    try {
      const updated = await combinedApi.updateTestSet(id, {
        name: formData.name,
        description: formData.description,
      });
      setTestSets(testSets.map(s => s.id === id ? updated : s));
      setEditingId(null);
      setFormData({ name: '', description: '' });
      setToast({ message: 'Test set updated successfully', type: 'success' });
    } catch (error) {
      console.error('Failed to update test set:', error);
      setToast({ message: 'Failed to update test set', type: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Are you sure you want to delete this test set?')) return;
    try {
      await combinedApi.deleteTestSet(id);
      setTestSets(testSets.filter(s => s.id !== id));
      setToast({ message: 'Test set deleted successfully', type: 'success' });
    } catch (error) {
      console.error('Failed to delete test set:', error);
      setToast({ message: 'Failed to delete test set', type: 'error' });
    }
  };

  const startEdit = (testSet: TestSetResponse) => {
    setEditingId(testSet.id);
    setFormData({ name: testSet.name, description: testSet.description || '' });
  };

  const cancelEdit = () => {
    setEditingId(null);
    setFormData({ name: '', description: '' });
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center py-12">
        <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
      </div>
    );
  }

  return (
    <div className="test-sets-list">
      {/* Toast notification */}
      {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}

      <div className="flex justify-between items-center mb-4">
        <h2 className="text-lg font-semibold">Test Sets</h2>
        <button
          onClick={() => setShowCreateModal(true)}
          className="btn btn-secondary flex items-center gap-2"
        >
          <Plus className="w-4 h-4" />
          + Create Test Set
        </button>
      </div>

      {testSets.length === 0 ? (
        <div className="text-center py-12 text-gray-500">
          <p className="text-lg">No test sets found</p>
          <p className="text-sm mt-2">Create a test set to organize your tests</p>
        </div>
      ) : (
        <div className="grid gap-4">
          {testSets.map((testSet) => (
            <div key={testSet.id} className="bg-white border rounded-lg p-4">
              {editingId === testSet.id ? (
                <div className="space-y-3">
                  <input
                    type="text"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    className="w-full px-3 py-2 border rounded"
                    placeholder="Test Set Name"
                  />
                  <textarea
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    className="w-full px-3 py-2 border rounded"
                    rows={2}
                    placeholder="Description"
                  />
                  <div className="flex gap-2 justify-end">
                    <button onClick={cancelEdit} className="btn btn-secondary flex items-center gap-1">
                      <X className="w-4 h-4" /> Cancel
                    </button>
                    <button
                      onClick={() => handleUpdate(testSet.id)}
                      className="btn btn-primary flex items-center gap-1"
                    >
                      <Check className="w-4 h-4" /> Save
                    </button>
                  </div>
                </div>
              ) : (
                <div className="flex justify-between items-start">
                  <div>
                    <h3 className="font-medium text-gray-900">{testSet.name}</h3>
                    <p className="text-sm text-gray-500 mt-1">
                      {testSet.description || 'No description'}
                    </p>
                    <div className="flex items-center gap-4 mt-2 text-sm text-gray-500">
                      <span>{testSet.testCount || 0} tests</span>
                      {testSet.lastExecutedAt && (
                        <span>Last executed: {new Date(testSet.lastExecutedAt).toLocaleDateString()}</span>
                      )}
                    </div>
                  </div>
                  <div className="flex gap-2">
                    <button
                      onClick={() => startEdit(testSet)}
                      className="p-2 hover:bg-gray-100 rounded text-gray-600"
                      aria-label="Edit test set"
                    >
                      <Edit2 className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => handleDelete(testSet.id)}
                      className="p-2 hover:bg-gray-100 rounded text-red-600"
                      aria-label="Delete test set"
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
                <h3 className="text-lg font-semibold">Create Test Set</h3>
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
                    placeholder="e.g., Smoke Tests, Regression Suite"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">Description</label>
                  <textarea
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                    rows={3}
                    placeholder="Describe this test set..."
                  />
                </div>
                <div className="flex justify-end gap-3 pt-4">
                  <button onClick={() => setShowCreateModal(false)} className="btn btn-secondary" disabled={saving}>
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

export default TestSetsList;