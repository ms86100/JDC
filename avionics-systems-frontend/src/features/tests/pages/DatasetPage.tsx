import React, { useState, useMemo, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import advancedApi, {
  DatasetResponse,
  DatasetVersionResponse,
  CreateDatasetRequest
} from '../../../api/testApi';
import {
  Plus, Search, Download, Upload, MoreVertical, Edit2, Trash2, Eye,
  ChevronRight, ChevronDown, Clock, Table, FileText, Grid3X3, X, Copy, History
} from 'lucide-react';

// Confirmation Dialog
const ConfirmDialog: React.FC<{
  open: boolean;
  title: string;
  message: string;
  confirmLabel?: string;
  onConfirm: () => void;
  onCancel: () => void;
  variant?: 'default' | 'danger';
}> = ({ open, title, message, confirmLabel = 'Confirm', onConfirm, onCancel, variant = 'default' }) => {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex items-center justify-center min-h-screen px-4">
        <div className="fixed inset-0 bg-black bg-opacity-50" onClick={onCancel}></div>
        <div className="relative bg-white rounded-lg shadow-xl max-w-md w-full p-6">
          <h3 className="text-lg font-semibold mb-2">{title}</h3>
          <p className="text-gray-600 mb-6">{message}</p>
          <div className="flex justify-end gap-3">
            <button onClick={onCancel} className="btn btn-secondary">Cancel</button>
            <button onClick={onConfirm} className={`btn ${variant === 'danger' ? 'bg-red-600 hover:bg-red-700 text-white' : 'btn-primary'}`}>
              {confirmLabel}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

// Create Dataset Modal
interface CreateDatasetModalProps {
  projectId: string;
  onClose: () => void;
  onSuccess: (dataset: DatasetResponse) => void;
}

const CreateDatasetModal: React.FC<CreateDatasetModalProps> = ({ projectId, onClose, onSuccess }) => {
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    dataFormat: 'TABULAR' as 'TABULAR' | 'CSV' | 'JSON',
  });
  const [columns, setColumns] = useState<{ name: string; type: string }[]>([
    { name: 'Column1', type: 'STRING' },
    { name: 'Column2', type: 'STRING' },
  ]);
  const [rows, setRows] = useState<string[][]>([['', '']]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const handleAddColumn = () => {
    setColumns([...columns, { name: `Column${columns.length + 1}`, type: 'STRING' }]);
    setRows(rows.map(row => [...row, '']));
  };

  const handleRemoveColumn = (index: number) => {
    setColumns(columns.filter((_, i) => i !== index));
    setRows(rows.map(row => row.filter((_, i) => i !== index)));
  };

  const handleAddRow = () => {
    setRows([...rows, new Array(columns.length).fill('')]);
  };

  const handleCellChange = (rowIndex: number, colIndex: number, value: string) => {
    const newRows = [...rows];
    newRows[rowIndex][colIndex] = value;
    setRows(newRows);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.name.trim()) {
      setError('Dataset name is required');
      return;
    }

    setSubmitting(true);
    try {
      const response = await advancedApi.datasets.create({
        projectId,
        name: formData.name,
        description: formData.description,
        dataFormat: formData.dataFormat,
        columnNames: columns.map(c => c.name),
        columnTypes: columns.map(c => c.type),
        rows: rows.filter(row => row.some(cell => cell.trim())),
      });
      onSuccess(response);
      onClose();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create dataset');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex items-center justify-center min-h-screen px-4">
        <div className="fixed inset-0 bg-black bg-opacity-50" onClick={onClose}></div>
        <div className="relative bg-white rounded-lg shadow-xl max-w-4xl w-full max-h-[90vh] overflow-y-auto">
          <div className="flex items-center justify-between p-4 border-b">
            <h3 className="text-lg font-semibold">Create Data Set</h3>
            <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
              <X className="w-5 h-5" />
            </button>
          </div>

          <form onSubmit={handleSubmit} className="p-4">
            {error && (
              <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded text-red-600 text-sm">
                {error}
              </div>
            )}

            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-1">Name <span className="text-red-600">*</span></label>
                  <input
                    type="text"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="e.g., User Login Test Data"
                    required
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">Format</label>
                  <select
                    value={formData.dataFormat}
                    onChange={(e) => setFormData({ ...formData, dataFormat: e.target.value as any })}
                    className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="TABULAR">Tabular</option>
                    <option value="CSV">CSV</option>
                    <option value="JSON">JSON</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Description</label>
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                  rows={2}
                  placeholder="Describe this dataset..."
                />
              </div>

              <div>
                <div className="flex items-center justify-between mb-2">
                  <label className="block text-sm font-medium">Columns & Data</label>
                  <button type="button" onClick={handleAddColumn} className="text-sm text-blue-600 hover:text-blue-800">
                    + Add Column
                  </button>
                </div>
                <div className="border rounded overflow-hidden">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="bg-gray-50">
                        {columns.map((col, i) => (
                          <th key={i} className="p-2 border-b border-r last:border-r-0">
                            <div className="flex items-center gap-2">
                              <input
                                type="text"
                                value={col.name}
                                onChange={(e) => {
                                  const newCols = [...columns];
                                  newCols[i].name = e.target.value;
                                  setColumns(newCols);
                                }}
                                className="w-24 px-2 py-1 border rounded text-sm"
                              />
                              <select
                                value={col.type}
                                onChange={(e) => {
                                  const newCols = [...columns];
                                  newCols[i].type = e.target.value;
                                  setColumns(newCols);
                                }}
                                className="px-2 py-1 border rounded text-xs"
                              >
                                <option value="STRING">String</option>
                                <option value="NUMBER">Number</option>
                                <option value="BOOLEAN">Boolean</option>
                                <option value="DATE">Date</option>
                              </select>
                              <button type="button" onClick={() => handleRemoveColumn(i)} className="text-gray-400 hover:text-red-600">
                                <X className="w-3 h-3" />
                              </button>
                            </div>
                          </th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {rows.map((row, rowIndex) => (
                        <tr key={rowIndex}>
                          {row.map((cell, colIndex) => (
                            <td key={colIndex} className="border-b border-r last:border-r-0 p-1">
                              <input
                                type="text"
                                value={cell}
                                onChange={(e) => handleCellChange(rowIndex, colIndex, e.target.value)}
                                className="w-full px-2 py-1 border rounded"
                              />
                            </td>
                          ))}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <button type="button" onClick={handleAddRow} className="mt-2 text-sm text-blue-600 hover:text-blue-800">
                  + Add Row
                </button>
              </div>
            </div>

            <div className="flex justify-end gap-3 mt-6 pt-4 border-t">
              <button type="button" onClick={onClose} className="btn btn-secondary">Cancel</button>
              <button type="submit" disabled={submitting} className="btn btn-primary">
                {submitting ? 'Creating...' : 'Create Dataset'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

// Dataset Detail Panel
interface DatasetDetailPanelProps {
  dataset: DatasetResponse;
  onClose: () => void;
}

const DatasetDetailPanel: React.FC<DatasetDetailPanelProps> = ({ dataset, onClose }) => {
  const [showAllRows, setShowAllRows] = useState(false);

  return (
    <div className="w-96 border-l border-gray-200 bg-white flex flex-col">
      <div className="flex items-center justify-between p-4 border-b">
        <h3 className="font-semibold">Dataset Details</h3>
        <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
          <X className="w-5 h-5" />
        </button>
      </div>
      <div className="flex-1 overflow-auto p-4">
        <div className="mb-4">
          <h4 className="text-lg font-medium">{dataset.name}</h4>
          {dataset.description && <p className="text-sm text-gray-600 mt-1">{dataset.description}</p>}
        </div>

        <div className="grid grid-cols-2 gap-4 mb-4 text-sm">
          <div className="p-3 bg-gray-50 rounded">
            <span className="text-gray-500">Rows</span>
            <p className="font-medium">{dataset.rowCount}</p>
          </div>
          <div className="p-3 bg-gray-50 rounded">
            <span className="text-gray-500">Version</span>
            <p className="font-medium">v{dataset.version}</p>
          </div>
          <div className="p-3 bg-gray-50 rounded">
            <span className="text-gray-500">Format</span>
            <p className="font-medium">{dataset.dataFormat}</p>
          </div>
          <div className="p-3 bg-gray-50 rounded">
            <span className="text-gray-500">Immutable</span>
            <p className="font-medium">{dataset.isImmutable ? 'Yes' : 'No'}</p>
          </div>
        </div>

        {dataset.columnNames && dataset.columnNames.length > 0 && (
          <div className="mb-4">
            <h5 className="text-sm font-medium mb-2">Columns</h5>
            <div className="space-y-1">
              {dataset.columnNames.map((col, i) => (
                <div key={i} className="flex items-center justify-between p-2 bg-gray-50 rounded text-sm">
                  <span>{col}</span>
                  {dataset.columnTypes && dataset.columnTypes[i] && (
                    <span className="text-xs text-gray-500">{dataset.columnTypes[i]}</span>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}

        <div>
          <div className="flex items-center justify-between mb-2">
            <h5 className="text-sm font-medium">Data Preview</h5>
            <button
              onClick={() => setShowAllRows(!showAllRows)}
              className="text-sm text-blue-600 hover:text-blue-800"
            >
              {showAllRows ? 'Show Less' : 'Show All'}
            </button>
          </div>
          {dataset.rows && dataset.rows.length > 0 ? (
            <div className="border rounded overflow-hidden text-sm">
              <table className="w-full">
                <thead>
                  <tr className="bg-gray-50">
                    {dataset.columnNames?.map((col, i) => (
                      <th key={i} className="p-2 border-b text-left font-medium">{col}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {(showAllRows ? dataset.rows : dataset.rows.slice(0, 5)).map((row, i) => (
                    <tr key={i}>
                      {(Array.isArray(row) ? row : []).map((cell, j) => (
                        <td key={j} className="p-2 border-b">{cell}</td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="text-gray-500 text-sm">No data rows</p>
          )}
        </div>
      </div>
    </div>
  );
};

// Version History Sidebar
interface VersionHistorySidebarProps {
  datasetId: string;
  onClose: () => void;
  onRestore: (versionId: string) => void;
}

const VersionHistorySidebar: React.FC<VersionHistorySidebarProps> = ({ datasetId, onClose, onRestore }) => {
  const { data: versions, isLoading } = useQuery({
    queryKey: ['dataset-versions', datasetId],
    queryFn: () => advancedApi.datasets.getVersions(datasetId),
  });

  return (
    <div className="w-80 border-l border-gray-200 bg-white flex flex-col">
      <div className="flex items-center justify-between p-4 border-b">
        <h3 className="font-semibold">Version History</h3>
        <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
          <X className="w-5 h-5" />
        </button>
      </div>
      <div className="flex-1 overflow-auto p-4">
        {isLoading ? (
          <div className="flex items-center justify-center h-32">
            <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-500"></div>
          </div>
        ) : versions && versions.length > 0 ? (
          <div className="space-y-3">
            {versions.map((version) => (
              <div key={version.id} className={`p-3 border rounded ${version.isImmutable ? 'border-blue-500 bg-blue-50' : 'border-gray-200'}`}>
                <div className="flex items-center justify-between mb-1">
                  <span className="font-medium">Version {version.versionNumber}</span>
                  {version.isImmutable && <span className="text-xs bg-blue-500 text-white px-2 py-0.5 rounded">Current</span>}
                </div>
                {version.changeSummary && <p className="text-sm text-gray-600 mb-2">{version.changeSummary}</p>}
                <div className="flex items-center justify-between text-xs text-gray-500">
                  <span>{version.createdBy || 'System'}</span>
                  <span>{new Date(version.createdAt).toLocaleDateString()}</span>
                </div>
                <div className="text-xs text-gray-500 mt-1">
                  {version.rowCount} rows
                </div>
                {!version.isImmutable && (
                  <button onClick={() => onRestore(version.id)} className="mt-2 text-sm text-blue-600 hover:text-blue-800">
                    Restore this version
                  </button>
                )}
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center text-gray-500 py-8">No version history available</div>
        )}
      </div>
    </div>
  );
};

// Main Page Component
export const DatasetPage: React.FC<{ projectId?: string }> = ({ projectId: propProjectId }) => {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = useState('');
  const [sortField, setSortField] = useState<'name' | 'updatedAt' | 'rowCount'>('updatedAt');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('desc');
  const [expandedDatasetId, setExpandedDatasetId] = useState<string | null>(null);
  const [selectedDataset, setSelectedDataset] = useState<DatasetResponse | null>(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showVersionHistory, setShowVersionHistory] = useState(false);
  const [deleteConfirm, setDeleteConfirm] = useState<{ open: boolean; datasetId: string | null }>({ open: false, datasetId: null });

  const { data: datasets = [], isLoading, refetch } = useQuery({
    queryKey: ['datasets', propProjectId],
    queryFn: () => advancedApi.datasets.list(propProjectId || ''),
    enabled: !!propProjectId,
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => advancedApi.datasets.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['datasets'] });
      setDeleteConfirm({ open: false, datasetId: null });
    },
  });

  const duplicateMutation = useMutation({
    mutationFn: (dataset: DatasetResponse) => advancedApi.datasets.create({
      projectId: propProjectId || '',
      name: `${dataset.name} (Copy)`,
      description: dataset.description,
      dataFormat: dataset.dataFormat as any,
      columnNames: dataset.columnNames,
      columnTypes: dataset.columnTypes,
      rows: dataset.rows,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['datasets'] });
    },
  });

  const exportMutation = useMutation({
    mutationFn: async (dataset: DatasetResponse) => {
      if (!dataset.rows) return;
      const csv = [
        dataset.columnNames?.join(',') || '',
        ...dataset.rows.map(row => row.join(','))
      ].join('\n');
      const blob = new Blob([csv], { type: 'text/csv' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${dataset.name}.csv`;
      a.click();
      URL.revokeObjectURL(url);
    },
  });

  const filteredDatasets = useMemo(() => {
    return datasets
      .filter(ds => ds.name.toLowerCase().includes(searchQuery.toLowerCase()))
      .sort((a, b) => {
        const aVal = a[sortField];
        const bVal = b[sortField];
        const direction = sortDirection === 'asc' ? 1 : -1;
        if (typeof aVal === 'string' && typeof bVal === 'string') {
          return aVal.localeCompare(bVal) * direction;
        }
        return ((aVal as number) < (bVal as number) ? -1 : 1) * direction;
      });
  }, [datasets, searchQuery, sortField, sortDirection]);

  const handleSort = (field: typeof sortField) => {
    if (sortField === field) {
      setSortDirection(d => d === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDirection('desc');
    }
  };

  const handleDelete = () => {
    if (deleteConfirm.datasetId) {
      deleteMutation.mutate(deleteConfirm.datasetId);
    }
  };

  return (
    <div className="h-full flex">
      {/* Main Content */}
      <div className="flex-1 flex flex-col bg-gray-50">
        {/* Header */}
        <div className="bg-white px-6 py-4 border-b border-gray-200">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Test Data Sets</h1>
              <p className="text-sm text-gray-500 mt-1">Manage test data for parameterized testing</p>
            </div>
            {propProjectId && (
              <div className="flex items-center gap-3">
                <button
                  onClick={() => setShowCreateModal(true)}
                  className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg"
                >
                  <Plus className="w-4 h-4" />
                  Create Data Set
                </button>
              </div>
            )}
          </div>

          {/* Search */}
          <div className="mt-4 relative max-w-md">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              type="text"
              placeholder="Search datasets..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
        </div>

        {/* Table */}
        <div className="flex-1 overflow-auto p-6">
          {isLoading ? (
            <div className="flex items-center justify-center h-64">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
            </div>
          ) : filteredDatasets.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-64 text-center bg-white rounded-lg border">
              <Table className="w-12 h-12 text-gray-400 mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">No data sets yet</h3>
              <p className="text-gray-500 mb-4">Create data sets to use in your parameterized tests</p>
              {propProjectId && (
                <button
                  onClick={() => setShowCreateModal(true)}
                  className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg"
                >
                  <Plus className="w-4 h-4" />
                  Create Data Set
                </button>
              )}
            </div>
          ) : (
            <div className="bg-white rounded-lg border overflow-hidden">
              <table className="w-full">
                <thead>
                  <tr className="text-left text-gray-500 bg-gray-50 border-b">
                    <th className="p-4 w-10"></th>
                    <th className="p-4 cursor-pointer" onClick={() => handleSort('name')}>
                      <span className="flex items-center gap-2">
                        Name
                        {sortField === 'name' && (
                          <span className="text-blue-600">{sortDirection === 'asc' ? '↑' : '↓'}</span>
                        )}
                      </span>
                    </th>
                    <th className="p-4">Format</th>
                    <th className="p-4 cursor-pointer" onClick={() => handleSort('rowCount')}>
                      <span className="flex items-center gap-2">
                        Rows
                        {sortField === 'rowCount' && (
                          <span className="text-blue-600">{sortDirection === 'asc' ? '↑' : '↓'}</span>
                        )}
                      </span>
                    </th>
                    <th className="p-4">Columns</th>
                    <th className="p-4">Version</th>
                    <th className="p-4 cursor-pointer" onClick={() => handleSort('updatedAt')}>
                      <span className="flex items-center gap-2">
                        Last Modified
                        {sortField === 'updatedAt' && (
                          <span className="text-blue-600">{sortDirection === 'asc' ? '↑' : '↓'}</span>
                        )}
                      </span>
                    </th>
                    <th className="p-4 w-12"></th>
                  </tr>
                </thead>
                <tbody>
                  {filteredDatasets.map((dataset) => (
                    <React.Fragment key={dataset.id}>
                      <tr
                        className="border-b hover:bg-gray-50 cursor-pointer"
                        onClick={() => setExpandedDatasetId(expandedDatasetId === dataset.id ? null : dataset.id)}
                      >
                        <td className="p-4">
                          {expandedDatasetId === dataset.id ? (
                            <ChevronDown className="w-4 h-4 text-gray-400" />
                          ) : (
                            <ChevronRight className="w-4 h-4 text-gray-400" />
                          )}
                        </td>
                        <td className="p-4">
                          <span className="font-medium text-gray-900">{dataset.name}</span>
                        </td>
                        <td className="p-4">
                          <span className={`px-2 py-0.5 rounded text-xs ${
                            dataset.dataFormat === 'CSV' ? 'bg-green-100 text-green-800' :
                            dataset.dataFormat === 'JSON' ? 'bg-purple-100 text-purple-800' :
                            'bg-gray-100 text-gray-800'
                          }`}>
                            {dataset.dataFormat}
                          </span>
                        </td>
                        <td className="p-4 text-gray-600">{dataset.rowCount}</td>
                        <td className="p-4 text-gray-600">
                          {dataset.columnNames?.length || 0}
                        </td>
                        <td className="p-4">
                          <span className="text-sm bg-gray-100 px-2 py-0.5 rounded">v{dataset.version}</span>
                        </td>
                        <td className="p-4 text-gray-500 text-sm">
                          {new Date(dataset.updatedAt).toLocaleDateString()}
                        </td>
                        <td className="p-4" onClick={(e) => e.stopPropagation()}>
                          <DatasetActionsMenu
                            dataset={dataset}
                            onView={() => setSelectedDataset(dataset)}
                            onEdit={() => setSelectedDataset(dataset)}
                            onDelete={() => setDeleteConfirm({ open: true, datasetId: dataset.id })}
                            onDuplicate={() => duplicateMutation.mutate(dataset)}
                            onExport={() => exportMutation.mutate(dataset)}
                            onVersionHistory={() => { setSelectedDataset(dataset); setShowVersionHistory(true); }}
                          />
                        </td>
                      </tr>
                      {expandedDatasetId === dataset.id && dataset.rows && dataset.rows.length > 0 && (
                        <tr>
                          <td colSpan={8} className="bg-gray-50 p-4">
                            <div className="border rounded overflow-hidden">
                              <table className="w-full text-sm">
                                <thead>
                                  <tr className="bg-gray-100">
                                    {dataset.columnNames?.map((col, i) => (
                                      <th key={i} className="p-2 text-left font-medium border-r last:border-r-0">{col}</th>
                                    ))}
                                  </tr>
                                </thead>
                                <tbody>
                                  {(Array.isArray(dataset.rows) ? dataset.rows : []).slice(0, 5).map((row, i) => (
                                    <tr key={i}>
                                      {(Array.isArray(row) ? row : []).map((cell, j) => (
                                        <td key={j} className="p-2 border-r last:border-r-0">{cell}</td>
                                      ))}
                                    </tr>
                                  ))}
                                </tbody>
                              </table>
                              {Array.isArray(dataset.rows) && dataset.rows.length > 5 && (
                                <div className="p-2 text-center text-sm text-gray-500 bg-gray-100">
                                  Showing 5 of {dataset.rows.length} rows
                                </div>
                              )}
                            </div>
                          </td>
                        </tr>
                      )}
                    </React.Fragment>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* Detail Panel */}
      {selectedDataset && !showVersionHistory && (
        <DatasetDetailPanel dataset={selectedDataset} onClose={() => setSelectedDataset(null)} />
      )}

      {/* Version History */}
      {showVersionHistory && selectedDataset && (
        <VersionHistorySidebar
          datasetId={selectedDataset.id}
          onClose={() => setShowVersionHistory(false)}
          onRestore={(versionId) => console.log('Restore:', versionId)}
        />
      )}

      {/* Create Modal */}
      {showCreateModal && propProjectId && (
        <CreateDatasetModal
          projectId={propProjectId}
          onClose={() => setShowCreateModal(false)}
          onSuccess={() => {
            setShowCreateModal(false);
            refetch();
          }}
        />
      )}

      {/* Delete Confirmation */}
      <ConfirmDialog
        open={deleteConfirm.open}
        title="Delete Data Set"
        message="Are you sure you want to delete this data set? This action cannot be undone."
        confirmLabel="Delete"
        onConfirm={handleDelete}
        onCancel={() => setDeleteConfirm({ open: false, datasetId: null })}
        variant="danger"
      />
    </div>
  );
};

// Dataset Actions Menu
const DatasetActionsMenu: React.FC<{
  dataset: DatasetResponse;
  onView: () => void;
  onEdit: () => void;
  onDelete: () => void;
  onDuplicate: () => void;
  onExport: () => void;
  onVersionHistory: () => void;
}> = ({ onView, onEdit, onDelete, onDuplicate, onExport, onVersionHistory }) => {
  const [open, setOpen] = useState(false);

  return (
    <div className="relative">
      <button onClick={() => setOpen(!open)} className="p-1 hover:bg-gray-200 rounded">
        <MoreVertical className="w-4 h-4 text-gray-400" />
      </button>
      {open && (
        <div className="absolute right-0 top-8 bg-white border border-gray-200 rounded-lg shadow-lg z-10 min-w-[160px]">
          <button onClick={() => { onView(); setOpen(false); }} className="w-full px-4 py-2 text-left text-sm hover:bg-gray-100 flex items-center gap-2">
            <Eye className="w-4 h-4" /> View Details
          </button>
          <button onClick={() => { onEdit(); setOpen(false); }} className="w-full px-4 py-2 text-left text-sm hover:bg-gray-100 flex items-center gap-2">
            <Edit2 className="w-4 h-4" /> Edit
          </button>
          <button onClick={() => { onDuplicate(); setOpen(false); }} className="w-full px-4 py-2 text-left text-sm hover:bg-gray-100 flex items-center gap-2">
            <Copy className="w-4 h-4" /> Duplicate
          </button>
          <button onClick={() => { onExport(); setOpen(false); }} className="w-full px-4 py-2 text-left text-sm hover:bg-gray-100 flex items-center gap-2">
            <Download className="w-4 h-4" /> Export CSV
          </button>
          <button onClick={() => { onVersionHistory(); setOpen(false); }} className="w-full px-4 py-2 text-left text-sm hover:bg-gray-100 flex items-center gap-2">
            <History className="w-4 h-4" /> Version History
          </button>
          <div className="border-t border-gray-200 my-1" />
          <button onClick={() => { onDelete(); setOpen(false); }} className="w-full px-4 py-2 text-left text-sm hover:bg-gray-100 text-red-600 flex items-center gap-2">
            <Trash2 className="w-4 h-4" /> Delete
          </button>
        </div>
      )}
    </div>
  );
};

export default DatasetPage;