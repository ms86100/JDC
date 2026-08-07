import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import combinedApi, {
  TestStatusConfigRequest,
  TestStatusConfigResponse,
  ExecutionStatusConfigRequest,
  ExecutionStatusConfigResponse,
  TestTypeConfigRequest,
  TestTypeConfigResponse,
} from '../../../api/testApi';
import {
  Settings, Plus, Edit2, Trash2, X, CheckCircle, AlertCircle, Loader2,
  ListChecks, Play, Tag,
} from 'lucide-react';

// ─── Tab definitions ───────────────────────────────────────────────────────────

interface Tab {
  id: 'testStatuses' | 'executionStatuses' | 'testTypes';
  label: string;
  icon: React.ReactNode;
}

const TABS: Tab[] = [
  { id: 'testStatuses', label: 'Test Statuses', icon: <ListChecks className="w-4 h-4" /> },
  { id: 'executionStatuses', label: 'Execution Statuses', icon: <Play className="w-4 h-4" /> },
  { id: 'testTypes', label: 'Test Types', icon: <Tag className="w-4 h-4" /> },
];

// ─── Toast ─────────────────────────────────────────────────────────────────────

const Toast: React.FC<{ message: string; type: 'success' | 'error'; onClose: () => void }> = ({ message, type, onClose }) => {
  React.useEffect(() => {
    const timer = setTimeout(onClose, 3000);
    return () => clearTimeout(timer);
  }, [onClose]);

  return (
    <div className={`fixed bottom-4 right-4 ${type === 'success' ? 'bg-green-500' : 'bg-red-500'} text-white px-4 py-3 rounded-lg shadow-lg flex items-center gap-2 z-50`}>
      {type === 'success' ? <CheckCircle className="w-5 h-5" /> : <AlertCircle className="w-5 h-5" />}
      <span>{message}</span>
      <button onClick={onClose} className="ml-2 hover:opacity-80"><X className="w-4 h-4" /></button>
    </div>
  );
};

// ─── Generic Config Modal ──────────────────────────────────────────────────────

interface ConfigFormFields {
  name: string;
  displayName: string;
  color: string;
  sortOrder: number;
  isActive: boolean;
  // extra fields per type
  category?: string;
  isDefault?: boolean;
  isFinal?: boolean;
  isPass?: boolean;
  isFail?: boolean;
  description?: string;
  icon?: string;
}

const EMPTY_FORM: ConfigFormFields = {
  name: '',
  displayName: '',
  color: '#0065FF',
  sortOrder: 0,
  isActive: true,
  category: 'TODO',
  isDefault: false,
  isFinal: false,
  isPass: false,
  isFail: false,
  description: '',
  icon: '',
};

interface ConfigModalProps {
  open: boolean;
  title: string;
  tabId: string;
  form: ConfigFormFields;
  isSaving: boolean;
  onFormChange: (form: ConfigFormFields) => void;
  onSave: () => void;
  onClose: () => void;
}

const ConfigModal: React.FC<ConfigModalProps> = ({
  open, title, tabId, form, isSaving, onFormChange, onSave, onClose,
}) => {
  if (!open) return null;

  const update = (patch: Partial<ConfigFormFields>) => onFormChange({ ...form, ...patch });

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex items-center justify-center min-h-screen px-4">
        <div className="fixed inset-0 bg-black bg-opacity-50" onClick={onClose}></div>
        <div className="relative bg-white rounded-lg shadow-xl max-w-lg w-full p-6">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-semibold">{title}</h3>
            <button onClick={onClose} className="text-gray-400 hover:text-gray-600"><X className="w-5 h-5" /></button>
          </div>

          <div className="space-y-4">
            {/* Name */}
            <div>
              <label className="block text-sm font-medium mb-1">Name <span className="text-red-500">*</span></label>
              <input
                type="text"
                value={form.name}
                onChange={(e) => update({ name: e.target.value })}
                className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="e.g. DRAFT"
              />
            </div>

            {/* Display Name */}
            <div>
              <label className="block text-sm font-medium mb-1">Display Name <span className="text-red-500">*</span></label>
              <input
                type="text"
                value={form.displayName}
                onChange={(e) => update({ displayName: e.target.value })}
                className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="e.g. Draft"
              />
            </div>

            {/* Color */}
            <div>
              <label className="block text-sm font-medium mb-1">Color</label>
              <div className="flex items-center gap-3">
                <input
                  type="color"
                  value={form.color}
                  onChange={(e) => update({ color: e.target.value })}
                  className="w-10 h-10 rounded cursor-pointer border-0"
                />
                <input
                  type="text"
                  value={form.color}
                  onChange={(e) => update({ color: e.target.value })}
                  className="flex-1 px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="#0065FF"
                />
                <span className="w-8 h-8 rounded" style={{ backgroundColor: form.color }}></span>
              </div>
            </div>

            {/* Sort Order */}
            <div>
              <label className="block text-sm font-medium mb-1">Sort Order</label>
              <input
                type="number"
                value={form.sortOrder}
                onChange={(e) => update({ sortOrder: parseInt(e.target.value, 10) || 0 })}
                className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                min={0}
              />
            </div>

            {/* Tab-specific fields */}
            {tabId === 'testStatuses' && (
              <>
                <div>
                  <label className="block text-sm font-medium mb-1">Category</label>
                  <select
                    value={form.category || 'TODO'}
                    onChange={(e) => update({ category: e.target.value })}
                    className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="TODO">TODO</option>
                    <option value="IN_PROGRESS">IN_PROGRESS</option>
                    <option value="DONE">DONE</option>
                  </select>
                </div>
                <div className="flex gap-6">
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input type="checkbox" checked={form.isDefault || false} onChange={(e) => update({ isDefault: e.target.checked })} className="w-4 h-4 rounded" />
                    <span className="text-sm">Default</span>
                  </label>
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input type="checkbox" checked={form.isFinal || false} onChange={(e) => update({ isFinal: e.target.checked })} className="w-4 h-4 rounded" />
                    <span className="text-sm">Final</span>
                  </label>
                </div>
              </>
            )}

            {tabId === 'executionStatuses' && (
              <div className="flex gap-6">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input type="checkbox" checked={form.isPass || false} onChange={(e) => update({ isPass: e.target.checked })} className="w-4 h-4 rounded" />
                  <span className="text-sm">Counts as Pass</span>
                </label>
                <label className="flex items-center gap-2 cursor-pointer">
                  <input type="checkbox" checked={form.isFail || false} onChange={(e) => update({ isFail: e.target.checked })} className="w-4 h-4 rounded" />
                  <span className="text-sm">Counts as Fail</span>
                </label>
              </div>
            )}

            {tabId === 'testTypes' && (
              <div>
                <label className="block text-sm font-medium mb-1">Description</label>
                <textarea
                  value={form.description || ''}
                  onChange={(e) => update({ description: e.target.value })}
                  className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                  rows={3}
                  placeholder="Describe this test type"
                />
              </div>
            )}
          </div>

          <div className="flex justify-end gap-3 mt-6">
            <button onClick={onClose} className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50">Cancel</button>
            <button
              onClick={onSave}
              disabled={!form.name.trim() || !form.displayName.trim() || isSaving}
              className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg disabled:opacity-50"
            >
              {isSaving && <Loader2 className="w-4 h-4 animate-spin" />}
              {isSaving ? 'Saving...' : 'Save'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

// ─── Delete Confirmation Modal ─────────────────────────────────────────────────

const DeleteConfirmModal: React.FC<{
  open: boolean;
  itemName: string;
  isDeleting: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}> = ({ open, itemName, isDeleting, onConfirm, onCancel }) => {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex items-center justify-center min-h-screen px-4">
        <div className="fixed inset-0 bg-black bg-opacity-50" onClick={onCancel}></div>
        <div className="relative bg-white rounded-lg shadow-xl max-w-md w-full p-6">
          <h3 className="text-lg font-semibold mb-2">Delete Confirmation</h3>
          <p className="text-gray-600 mb-6">
            Are you sure you want to delete <strong>{itemName}</strong>? This action cannot be undone.
          </p>
          <div className="flex justify-end gap-3">
            <button onClick={onCancel} className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50">Cancel</button>
            <button
              onClick={onConfirm}
              disabled={isDeleting}
              className="flex items-center gap-2 px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg disabled:opacity-50"
            >
              {isDeleting && <Loader2 className="w-4 h-4 animate-spin" />}
              {isDeleting ? 'Deleting...' : 'Delete'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

// ─── Tab Content: Test Statuses ────────────────────────────────────────────────

const TestStatusesTab: React.FC<{
  onToast: (msg: string, type: 'success' | 'error') => void;
}> = ({ onToast }) => {
  const queryClient = useQueryClient();
  const [modalOpen, setModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<TestStatusConfigResponse | null>(null);
  const [form, setForm] = useState<ConfigFormFields>(EMPTY_FORM);
  const [deleteTarget, setDeleteTarget] = useState<TestStatusConfigResponse | null>(null);

  const { data: statuses = [], isLoading } = useQuery({
    queryKey: ['test-admin-statuses'],
    queryFn: () => combinedApi.getTestStatuses(),
  });

  const createMutation = useMutation({
    mutationFn: (data: TestStatusConfigRequest) => combinedApi.createTestStatus(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['test-admin-statuses'] });
      onToast('Test status created successfully', 'success');
      closeModal();
    },
    onError: () => onToast('Failed to create test status', 'error'),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: TestStatusConfigRequest }) => combinedApi.updateTestStatus(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['test-admin-statuses'] });
      onToast('Test status updated successfully', 'success');
      closeModal();
    },
    onError: () => onToast('Failed to update test status', 'error'),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => combinedApi.deleteTestStatus(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['test-admin-statuses'] });
      onToast('Test status deleted', 'success');
      setDeleteTarget(null);
    },
    onError: () => onToast('Failed to delete test status', 'error'),
  });

  const openCreate = () => {
    setEditingItem(null);
    setForm({ ...EMPTY_FORM, sortOrder: statuses.length + 1 });
    setModalOpen(true);
  };

  const openEdit = (item: TestStatusConfigResponse) => {
    setEditingItem(item);
    setForm({
      name: item.name,
      displayName: item.displayName,
      color: item.color || '#0065FF',
      sortOrder: item.sortOrder,
      isActive: item.isActive,
      category: item.category || 'TODO',
      isDefault: item.isDefault,
      isFinal: item.isFinal,
    });
    setModalOpen(true);
  };

  const closeModal = () => {
    setModalOpen(false);
    setEditingItem(null);
    setForm(EMPTY_FORM);
  };

  const handleSave = () => {
    const payload: TestStatusConfigRequest = {
      name: form.name.trim(),
      displayName: form.displayName.trim(),
      color: form.color,
      sortOrder: form.sortOrder,
      category: form.category as 'TODO' | 'IN_PROGRESS' | 'DONE',
      isDefault: form.isDefault,
      isFinal: form.isFinal,
    };
    if (editingItem) {
      updateMutation.mutate({ id: editingItem.id, data: payload });
    } else {
      createMutation.mutate(payload);
    }
  };

  const isSaving = createMutation.isPending || updateMutation.isPending;
  const sorted = [...statuses].sort((a, b) => a.sortOrder - b.sortOrder);

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <div>
          <h3 className="font-semibold">Test Statuses</h3>
          <p className="text-sm text-gray-500">Define statuses for test case lifecycle</p>
        </div>
        <button onClick={openCreate} className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg">
          <Plus className="w-4 h-4" /> Add Status
        </button>
      </div>

      <div className="bg-white rounded-lg border overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Name</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Display Name</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Color</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Sort Order</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {isLoading ? (
              <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-500">Loading...</td></tr>
            ) : sorted.length === 0 ? (
              <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-500">No test statuses configured yet.</td></tr>
            ) : (
              sorted.map((item) => (
                <tr key={item.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-sm font-medium text-gray-900">{item.name}</td>
                  <td className="px-4 py-3 text-sm text-gray-700">{item.displayName}</td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <span className="w-5 h-5 rounded" style={{ backgroundColor: item.color }}></span>
                      <span className="text-xs text-gray-500">{item.color}</span>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-600">{item.sortOrder}</td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex px-2 py-0.5 text-xs font-medium rounded-full ${item.isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-600'}`}>
                      {item.isActive ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <button onClick={() => openEdit(item)} className="text-blue-600 hover:text-blue-800"><Edit2 className="w-4 h-4" /></button>
                      <button onClick={() => setDeleteTarget(item)} className="text-red-500 hover:text-red-700"><Trash2 className="w-4 h-4" /></button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <ConfigModal
        open={modalOpen}
        title={editingItem ? 'Edit Test Status' : 'Add Test Status'}
        tabId="testStatuses"
        form={form}
        isSaving={isSaving}
        onFormChange={setForm}
        onSave={handleSave}
        onClose={closeModal}
      />

      <DeleteConfirmModal
        open={!!deleteTarget}
        itemName={deleteTarget?.displayName || ''}
        isDeleting={deleteMutation.isPending}
        onConfirm={() => deleteTarget && deleteMutation.mutate(deleteTarget.id)}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
};

// ─── Tab Content: Execution Statuses ───────────────────────────────────────────

const ExecutionStatusesTab: React.FC<{
  onToast: (msg: string, type: 'success' | 'error') => void;
}> = ({ onToast }) => {
  const queryClient = useQueryClient();
  const [modalOpen, setModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<ExecutionStatusConfigResponse | null>(null);
  const [form, setForm] = useState<ConfigFormFields>(EMPTY_FORM);
  const [deleteTarget, setDeleteTarget] = useState<ExecutionStatusConfigResponse | null>(null);

  const { data: statuses = [], isLoading } = useQuery({
    queryKey: ['test-admin-execution-statuses'],
    queryFn: () => combinedApi.getExecutionStatuses(),
  });

  const createMutation = useMutation({
    mutationFn: (data: ExecutionStatusConfigRequest) => combinedApi.createExecutionStatus(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['test-admin-execution-statuses'] });
      onToast('Execution status created successfully', 'success');
      closeModal();
    },
    onError: () => onToast('Failed to create execution status', 'error'),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: ExecutionStatusConfigRequest }) => combinedApi.updateExecutionStatus(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['test-admin-execution-statuses'] });
      onToast('Execution status updated successfully', 'success');
      closeModal();
    },
    onError: () => onToast('Failed to update execution status', 'error'),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => combinedApi.deleteExecutionStatus(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['test-admin-execution-statuses'] });
      onToast('Execution status deleted', 'success');
      setDeleteTarget(null);
    },
    onError: () => onToast('Failed to delete execution status', 'error'),
  });

  const openCreate = () => {
    setEditingItem(null);
    setForm({ ...EMPTY_FORM, sortOrder: statuses.length + 1 });
    setModalOpen(true);
  };

  const openEdit = (item: ExecutionStatusConfigResponse) => {
    setEditingItem(item);
    setForm({
      name: item.name,
      displayName: item.displayName,
      color: item.color || '#0065FF',
      sortOrder: item.sortOrder,
      isActive: item.isActive,
      isPass: item.isPass,
      isFail: item.isFail,
    });
    setModalOpen(true);
  };

  const closeModal = () => {
    setModalOpen(false);
    setEditingItem(null);
    setForm(EMPTY_FORM);
  };

  const handleSave = () => {
    const payload: ExecutionStatusConfigRequest = {
      name: form.name.trim(),
      displayName: form.displayName.trim(),
      color: form.color,
      sortOrder: form.sortOrder,
      isPass: form.isPass,
      isFail: form.isFail,
    };
    if (editingItem) {
      updateMutation.mutate({ id: editingItem.id, data: payload });
    } else {
      createMutation.mutate(payload);
    }
  };

  const isSaving = createMutation.isPending || updateMutation.isPending;
  const sorted = [...statuses].sort((a, b) => a.sortOrder - b.sortOrder);

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <div>
          <h3 className="font-semibold">Execution Statuses</h3>
          <p className="text-sm text-gray-500">Define statuses for test execution results</p>
        </div>
        <button onClick={openCreate} className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg">
          <Plus className="w-4 h-4" /> Add Status
        </button>
      </div>

      <div className="bg-white rounded-lg border overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Name</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Display Name</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Color</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Sort Order</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {isLoading ? (
              <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-500">Loading...</td></tr>
            ) : sorted.length === 0 ? (
              <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-500">No execution statuses configured yet.</td></tr>
            ) : (
              sorted.map((item) => (
                <tr key={item.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-sm font-medium text-gray-900">{item.name}</td>
                  <td className="px-4 py-3 text-sm text-gray-700">{item.displayName}</td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <span className="w-5 h-5 rounded" style={{ backgroundColor: item.color }}></span>
                      <span className="text-xs text-gray-500">{item.color}</span>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-600">{item.sortOrder}</td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex px-2 py-0.5 text-xs font-medium rounded-full ${item.isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-600'}`}>
                      {item.isActive ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <button onClick={() => openEdit(item)} className="text-blue-600 hover:text-blue-800"><Edit2 className="w-4 h-4" /></button>
                      <button onClick={() => setDeleteTarget(item)} className="text-red-500 hover:text-red-700"><Trash2 className="w-4 h-4" /></button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <ConfigModal
        open={modalOpen}
        title={editingItem ? 'Edit Execution Status' : 'Add Execution Status'}
        tabId="executionStatuses"
        form={form}
        isSaving={isSaving}
        onFormChange={setForm}
        onSave={handleSave}
        onClose={closeModal}
      />

      <DeleteConfirmModal
        open={!!deleteTarget}
        itemName={deleteTarget?.displayName || ''}
        isDeleting={deleteMutation.isPending}
        onConfirm={() => deleteTarget && deleteMutation.mutate(deleteTarget.id)}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
};

// ─── Tab Content: Test Types ───────────────────────────────────────────────────

const TestTypesTab: React.FC<{
  onToast: (msg: string, type: 'success' | 'error') => void;
}> = ({ onToast }) => {
  const queryClient = useQueryClient();
  const [modalOpen, setModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<TestTypeConfigResponse | null>(null);
  const [form, setForm] = useState<ConfigFormFields>(EMPTY_FORM);
  const [deleteTarget, setDeleteTarget] = useState<TestTypeConfigResponse | null>(null);

  const { data: types = [], isLoading } = useQuery({
    queryKey: ['test-admin-test-types'],
    queryFn: () => combinedApi.getTestTypes(),
  });

  const createMutation = useMutation({
    mutationFn: (data: TestTypeConfigRequest) => combinedApi.createTestType(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['test-admin-test-types'] });
      onToast('Test type created successfully', 'success');
      closeModal();
    },
    onError: () => onToast('Failed to create test type', 'error'),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: TestTypeConfigRequest }) => combinedApi.updateTestType(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['test-admin-test-types'] });
      onToast('Test type updated successfully', 'success');
      closeModal();
    },
    onError: () => onToast('Failed to update test type', 'error'),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => combinedApi.deleteTestType(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['test-admin-test-types'] });
      onToast('Test type deleted', 'success');
      setDeleteTarget(null);
    },
    onError: () => onToast('Failed to delete test type', 'error'),
  });

  const openCreate = () => {
    setEditingItem(null);
    setForm({ ...EMPTY_FORM, sortOrder: types.length + 1 });
    setModalOpen(true);
  };

  const openEdit = (item: TestTypeConfigResponse) => {
    setEditingItem(item);
    setForm({
      name: item.name,
      displayName: item.displayName,
      color: item.color || '#0065FF',
      sortOrder: item.sortOrder,
      isActive: item.isActive,
      description: item.description || '',
    });
    setModalOpen(true);
  };

  const closeModal = () => {
    setModalOpen(false);
    setEditingItem(null);
    setForm(EMPTY_FORM);
  };

  const handleSave = () => {
    const payload: TestTypeConfigRequest = {
      name: form.name.trim(),
      displayName: form.displayName.trim(),
      color: form.color,
      sortOrder: form.sortOrder,
      description: form.description,
    };
    if (editingItem) {
      updateMutation.mutate({ id: editingItem.id, data: payload });
    } else {
      createMutation.mutate(payload);
    }
  };

  const isSaving = createMutation.isPending || updateMutation.isPending;
  const sorted = [...types].sort((a, b) => a.sortOrder - b.sortOrder);

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <div>
          <h3 className="font-semibold">Test Types</h3>
          <p className="text-sm text-gray-500">Define types of test cases available</p>
        </div>
        <button onClick={openCreate} className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg">
          <Plus className="w-4 h-4" /> Add Type
        </button>
      </div>

      <div className="bg-white rounded-lg border overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Name</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Display Name</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Color</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Sort Order</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {isLoading ? (
              <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-500">Loading...</td></tr>
            ) : sorted.length === 0 ? (
              <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-500">No test types configured yet.</td></tr>
            ) : (
              sorted.map((item) => (
                <tr key={item.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-sm font-medium text-gray-900">{item.name}</td>
                  <td className="px-4 py-3 text-sm text-gray-700">{item.displayName}</td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <span className="w-5 h-5 rounded" style={{ backgroundColor: item.color }}></span>
                      <span className="text-xs text-gray-500">{item.color}</span>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-600">{item.sortOrder}</td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex px-2 py-0.5 text-xs font-medium rounded-full ${item.isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-600'}`}>
                      {item.isActive ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <button onClick={() => openEdit(item)} className="text-blue-600 hover:text-blue-800"><Edit2 className="w-4 h-4" /></button>
                      <button onClick={() => setDeleteTarget(item)} className="text-red-500 hover:text-red-700"><Trash2 className="w-4 h-4" /></button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <ConfigModal
        open={modalOpen}
        title={editingItem ? 'Edit Test Type' : 'Add Test Type'}
        tabId="testTypes"
        form={form}
        isSaving={isSaving}
        onFormChange={setForm}
        onSave={handleSave}
        onClose={closeModal}
      />

      <DeleteConfirmModal
        open={!!deleteTarget}
        itemName={deleteTarget?.displayName || ''}
        isDeleting={deleteMutation.isPending}
        onConfirm={() => deleteTarget && deleteMutation.mutate(deleteTarget.id)}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
};

// ─── Main Page Component ───────────────────────────────────────────────────────

export const TestAdminConfigPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<Tab['id']>('testStatuses');
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null);

  const showToast = (message: string, type: 'success' | 'error') => {
    setToast({ message, type });
  };

  const renderTabContent = () => {
    switch (activeTab) {
      case 'testStatuses':
        return <TestStatusesTab onToast={showToast} />;
      case 'executionStatuses':
        return <ExecutionStatusesTab onToast={showToast} />;
      case 'testTypes':
        return <TestTypesTab onToast={showToast} />;
      default:
        return null;
    }
  };

  return (
    <div className="h-full flex flex-col bg-gray-50">
      {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}

      {/* Header */}
      <div className="bg-white px-6 py-4 border-b border-gray-200">
        <div className="flex items-center gap-3">
          <Settings className="w-6 h-6 text-gray-600" />
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Test Admin Configuration</h1>
            <p className="text-sm text-gray-500 mt-1">Manage test statuses, execution statuses, and test types</p>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="bg-white border-b border-gray-200">
        <div className="flex px-6">
          {TABS.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`flex items-center gap-2 px-4 py-3 text-sm font-medium border-b-2 transition-colors ${
                activeTab === tab.id
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              {tab.icon}
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-auto p-6">
        {renderTabContent()}
      </div>
    </div>
  );
};

export default TestAdminConfigPage;
