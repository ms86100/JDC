import React, { useState, useMemo } from 'react';
import { Plus, Edit2, Trash2, Search, X, Columns, LayoutGrid } from 'lucide-react';
import AdminLayout from '../components/AdminLayout';
import {
  useBoardTypes,
  useCreateBoardType,
  useUpdateBoardType,
  useDeleteBoardType,
  BoardType,
  BoardColumnTemplate,
} from '../hooks/useAdminApi';
import './BoardTypesPage.css';

// ----------------------------------------------------------------
// Form data shapes
// ----------------------------------------------------------------

interface BoardTypeFormData {
  typeKey: string;
  displayName: string;
  description: string;
  isActive: boolean;
}

interface ColumnFormData {
  columnName: string;
  statusCategory: string;
  color: string;
  wipLimit: string; // kept as string for input; converted on save
  sortOrder: number;
}

const EMPTY_BOARD_TYPE_FORM: BoardTypeFormData = {
  typeKey: '',
  displayName: '',
  description: '',
  isActive: true,
};

const EMPTY_COLUMN_FORM: ColumnFormData = {
  columnName: '',
  statusCategory: 'TODO',
  color: '#4a9df8',
  wipLimit: '',
  sortOrder: 1,
};

const STATUS_CATEGORIES = [
  { value: 'TODO', label: 'To Do' },
  { value: 'IN_PROGRESS', label: 'In Progress' },
  { value: 'DONE', label: 'Done' },
] as const;

function generateTypeKey(name: string): string {
  return name
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, '_')
    .replace(/^_|_$/g, '');
}

function getBoardIconClass(typeKey: string): string {
  const k = typeKey.toUpperCase();
  if (k.includes('SCRUM')) return 'scrum';
  if (k.includes('KANBAN')) return 'kanban';
  return 'default';
}

function categoryLabel(cat: string): string {
  const found = STATUS_CATEGORIES.find(c => c.value === cat);
  return found ? found.label : cat.replace('_', ' ');
}

// ----------------------------------------------------------------
// Component
// ----------------------------------------------------------------

export default function BoardTypesPage() {
  // --- data fetching ---
  const { data: boardTypes, isLoading, isError } = useBoardTypes();
  const createMutation = useCreateBoardType();
  const updateMutation = useUpdateBoardType();
  const deleteMutation = useDeleteBoardType();

  // --- UI state ---
  const [search, setSearch] = useState('');
  const [selectedId, setSelectedId] = useState<string | null>(null);

  // board type modal
  const [showBoardTypeModal, setShowBoardTypeModal] = useState(false);
  const [editingBoardType, setEditingBoardType] = useState<BoardType | null>(null);
  const [boardTypeForm, setBoardTypeForm] = useState<BoardTypeFormData>(EMPTY_BOARD_TYPE_FORM);

  // column modal
  const [showColumnModal, setShowColumnModal] = useState(false);
  const [editingColumnIndex, setEditingColumnIndex] = useState<number | null>(null);
  const [columnForm, setColumnForm] = useState<ColumnFormData>(EMPTY_COLUMN_FORM);

  // delete confirm
  const [deleteConfirm, setDeleteConfirm] = useState<string | null>(null); // board type id
  const [deleteColumnIndex, setDeleteColumnIndex] = useState<number | null>(null);

  // alerts
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  // --- derived ---
  const filteredBoardTypes = useMemo(() => {
    const list = boardTypes || [];
    if (!search) return list;
    const s = search.toLowerCase();
    return list.filter(
      bt =>
        bt.displayName.toLowerCase().includes(s) ||
        bt.typeKey.toLowerCase().includes(s)
    );
  }, [boardTypes, search]);

  const selectedBoardType = useMemo(
    () => (boardTypes || []).find(bt => bt.id === selectedId) || null,
    [boardTypes, selectedId]
  );

  const columns = useMemo(() => {
    if (!selectedBoardType?.columnTemplates) return [];
    return [...selectedBoardType.columnTemplates].sort((a, b) => a.sortOrder - b.sortOrder);
  }, [selectedBoardType]);

  const totalBoardTypes = boardTypes?.length ?? 0;
  const activeBoardTypes = boardTypes?.filter(bt => bt.isActive).length ?? 0;

  // --- helpers ---

  const showMsg = (msg: string, isErr = false) => {
    if (isErr) {
      setActionError(msg);
      setActionSuccess(null);
    } else {
      setActionSuccess(msg);
      setActionError(null);
    }
    setTimeout(() => {
      setActionError(null);
      setActionSuccess(null);
    }, 4000);
  };

  const extractError = (err: unknown): string => {
    const e = err as { response?: { data?: { message?: string } } };
    return e?.response?.data?.message || (err instanceof Error ? err.message : 'Unknown error');
  };

  // ----------------------------------------------------------------
  // Board Type CRUD
  // ----------------------------------------------------------------

  const openCreateBoardTypeModal = () => {
    setEditingBoardType(null);
    setBoardTypeForm({ ...EMPTY_BOARD_TYPE_FORM });
    setActionError(null);
    setShowBoardTypeModal(true);
  };

  const openEditBoardTypeModal = (bt: BoardType) => {
    setEditingBoardType(bt);
    setBoardTypeForm({
      typeKey: bt.typeKey,
      displayName: bt.displayName,
      description: bt.description || '',
      isActive: bt.isActive,
    });
    setActionError(null);
    setShowBoardTypeModal(true);
  };

  const closeBoardTypeModal = () => {
    setShowBoardTypeModal(false);
    setEditingBoardType(null);
    setBoardTypeForm(EMPTY_BOARD_TYPE_FORM);
    setActionError(null);
  };

  const handleBoardTypeNameChange = (name: string) => {
    setBoardTypeForm(prev => ({
      ...prev,
      displayName: name,
      typeKey: editingBoardType ? prev.typeKey : generateTypeKey(name),
    }));
  };

  const handleSaveBoardType = () => {
    if (!boardTypeForm.displayName.trim() || !boardTypeForm.typeKey.trim()) return;

    const payload: Partial<BoardType> = {
      typeKey: boardTypeForm.typeKey,
      displayName: boardTypeForm.displayName.trim(),
      description: boardTypeForm.description.trim(),
      isActive: boardTypeForm.isActive,
    };

    if (editingBoardType) {
      // preserve existing column templates on update
      payload.columnTemplates = editingBoardType.columnTemplates;
      updateMutation.mutate(
        { id: editingBoardType.id, data: payload },
        {
          onSuccess: () => {
            showMsg(`Board type "${boardTypeForm.displayName}" updated.`);
            closeBoardTypeModal();
          },
          onError: (err) => setActionError(extractError(err)),
        }
      );
    } else {
      payload.columnTemplates = [];
      createMutation.mutate(payload, {
        onSuccess: (res) => {
          showMsg(`Board type "${boardTypeForm.displayName}" created.`);
          closeBoardTypeModal();
          // auto-select the new board type
          if (res?.data?.id) setSelectedId(res.data.id);
        },
        onError: (err) => setActionError(extractError(err)),
      });
    }
  };

  const handleDeleteBoardType = () => {
    if (!deleteConfirm) return;
    const target = boardTypes?.find(bt => bt.id === deleteConfirm);
    deleteMutation.mutate(deleteConfirm, {
      onSuccess: () => {
        if (selectedId === deleteConfirm) setSelectedId(null);
        setDeleteConfirm(null);
        showMsg(`Board type "${target?.displayName}" deleted.`);
      },
      onError: (err) => {
        setDeleteConfirm(null);
        showMsg(extractError(err), true);
      },
    });
  };

  // ----------------------------------------------------------------
  // Column Template CRUD (operates via board type update)
  // ----------------------------------------------------------------

  const openCreateColumnModal = () => {
    if (!selectedBoardType) return;
    setEditingColumnIndex(null);
    setColumnForm({
      ...EMPTY_COLUMN_FORM,
      sortOrder: (columns.length > 0 ? Math.max(...columns.map(c => c.sortOrder)) + 1 : 1),
    });
    setActionError(null);
    setShowColumnModal(true);
  };

  const openEditColumnModal = (index: number) => {
    const col = columns[index];
    if (!col) return;
    setEditingColumnIndex(index);
    setColumnForm({
      columnName: col.columnName,
      statusCategory: col.statusCategory,
      color: col.color || '#4a9df8',
      wipLimit: col.wipLimit != null ? String(col.wipLimit) : '',
      sortOrder: col.sortOrder,
    });
    setActionError(null);
    setShowColumnModal(true);
  };

  const closeColumnModal = () => {
    setShowColumnModal(false);
    setEditingColumnIndex(null);
    setColumnForm(EMPTY_COLUMN_FORM);
    setActionError(null);
  };

  const handleSaveColumn = () => {
    if (!selectedBoardType || !columnForm.columnName.trim()) return;

    const newCol: BoardColumnTemplate = {
      id: editingColumnIndex !== null ? columns[editingColumnIndex].id : '',
      boardTypeId: selectedBoardType.id,
      columnName: columnForm.columnName.trim(),
      statusCategory: columnForm.statusCategory,
      color: columnForm.color,
      wipLimit: columnForm.wipLimit.trim() ? parseInt(columnForm.wipLimit, 10) : null,
      sortOrder: columnForm.sortOrder,
    };

    let updatedCols: BoardColumnTemplate[];
    if (editingColumnIndex !== null) {
      updatedCols = columns.map((c, i) => (i === editingColumnIndex ? newCol : c));
    } else {
      updatedCols = [...columns, newCol];
    }

    updateMutation.mutate(
      {
        id: selectedBoardType.id,
        data: {
          typeKey: selectedBoardType.typeKey,
          displayName: selectedBoardType.displayName,
          description: selectedBoardType.description,
          isActive: selectedBoardType.isActive,
          columnTemplates: updatedCols,
        },
      },
      {
        onSuccess: () => {
          showMsg(
            editingColumnIndex !== null
              ? `Column "${columnForm.columnName}" updated.`
              : `Column "${columnForm.columnName}" added.`
          );
          closeColumnModal();
        },
        onError: (err) => setActionError(extractError(err)),
      }
    );
  };

  const handleDeleteColumn = () => {
    if (deleteColumnIndex === null || !selectedBoardType) return;
    const target = columns[deleteColumnIndex];
    const updatedCols = columns.filter((_, i) => i !== deleteColumnIndex);

    updateMutation.mutate(
      {
        id: selectedBoardType.id,
        data: {
          typeKey: selectedBoardType.typeKey,
          displayName: selectedBoardType.displayName,
          description: selectedBoardType.description,
          isActive: selectedBoardType.isActive,
          columnTemplates: updatedCols,
        },
      },
      {
        onSuccess: () => {
          setDeleteColumnIndex(null);
          showMsg(`Column "${target?.columnName}" removed.`);
        },
        onError: (err) => {
          setDeleteColumnIndex(null);
          showMsg(extractError(err), true);
        },
      }
    );
  };

  const isSaving = createMutation.isPending || updateMutation.isPending;

  // ----------------------------------------------------------------
  // Render
  // ----------------------------------------------------------------

  return (
    <AdminLayout>
      <div className="admin-page">
        {/* Header */}
        <div className="admin-page-header">
          <h1 className="admin-page-title">Board Types</h1>
          <p className="admin-page-description">
            Configure board types (Scrum, Kanban, etc.) and their default column templates.
          </p>
        </div>

        {/* Stats */}
        <div className="admin-stats-grid">
          <div className="admin-stat-card">
            <div className="admin-stat-value">{totalBoardTypes}</div>
            <div className="admin-stat-label">Total Board Types</div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-value">{activeBoardTypes}</div>
            <div className="admin-stat-label">Active</div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-value">
              {selectedBoardType ? columns.length : '-'}
            </div>
            <div className="admin-stat-label">
              {selectedBoardType ? `Columns in ${selectedBoardType.displayName}` : 'Select a Board Type'}
            </div>
          </div>
        </div>

        {/* Alerts */}
        {actionSuccess && (
          <div className="admin-alert admin-alert-success">
            {actionSuccess}
            <button type="button" className="admin-alert-dismiss" onClick={() => setActionSuccess(null)}>
              &times;
            </button>
          </div>
        )}
        {actionError && !showBoardTypeModal && !showColumnModal && (
          <div className="admin-alert admin-alert-error">
            {actionError}
            <button type="button" className="admin-alert-dismiss" onClick={() => setActionError(null)}>
              &times;
            </button>
          </div>
        )}

        {/* Two-panel layout */}
        <div className="board-types-layout">
          {/* ========== Left panel: Board types list ========== */}
          <div className="board-types-panel">
            <div className="board-types-panel-header">
              <h3>Board Types</h3>
              <button className="admin-btn-primary admin-btn-sm" onClick={openCreateBoardTypeModal}>
                <Plus size={14} style={{ marginRight: 4 }} />
                Add
              </button>
            </div>

            <div className="board-types-search">
              <input
                type="text"
                placeholder="Search board types..."
                value={search}
                onChange={e => setSearch(e.target.value)}
              />
            </div>

            <div className="board-types-list">
              {isLoading ? (
                <>
                  {[...Array(3)].map((_, i) => (
                    <div className="board-type-skeleton" key={i}>
                      <div className="ab-skeleton sk-icon" />
                      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 6 }}>
                        <div className="ab-skeleton sk-text" style={{ width: '60%' }} />
                        <div className="ab-skeleton sk-text" style={{ width: '40%' }} />
                      </div>
                    </div>
                  ))}
                </>
              ) : isError ? (
                <div className="board-types-empty">
                  <p>Failed to load board types.</p>
                </div>
              ) : filteredBoardTypes.length === 0 ? (
                <div className="board-types-empty">
                  <div className="board-types-empty-icon">
                    <LayoutGrid size={40} />
                  </div>
                  <p>{search ? 'No board types match your search.' : 'No board types configured yet.'}</p>
                </div>
              ) : (
                filteredBoardTypes.map(bt => (
                  <div
                    key={bt.id}
                    className={`board-type-card${selectedId === bt.id ? ' selected' : ''}`}
                    onClick={() => setSelectedId(bt.id)}
                  >
                    <div className={`board-type-card-icon ${getBoardIconClass(bt.typeKey)}`}>
                      <Columns size={18} />
                    </div>
                    <div className="board-type-card-info">
                      <div className="board-type-card-name">{bt.displayName}</div>
                      <div className="board-type-card-meta">
                        <span className="board-type-card-key">{bt.typeKey}</span>
                        <span className="board-type-card-cols">
                          {bt.columnTemplates?.length ?? 0} column{(bt.columnTemplates?.length ?? 0) !== 1 ? 's' : ''}
                        </span>
                      </div>
                    </div>
                    <div className="board-type-card-status">
                      <div className={`board-type-active-dot ${bt.isActive ? 'active' : 'inactive'}`} title={bt.isActive ? 'Active' : 'Inactive'} />
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

          {/* ========== Right panel: Column templates ========== */}
          <div className="column-templates-panel">
            {selectedBoardType ? (
              <>
                {/* Header */}
                <div className="column-templates-header">
                  <div className="column-templates-header-left">
                    <h3>{selectedBoardType.displayName} — Columns</h3>
                    {!selectedBoardType.isActive && (
                      <span className="admin-status" style={{ background: '#ffebe6', color: '#de350b', fontSize: 11, padding: '2px 8px', borderRadius: 10 }}>
                        Inactive
                      </span>
                    )}
                  </div>
                  <div className="column-templates-header-actions">
                    <button className="admin-btn-secondary admin-btn-sm" onClick={() => openEditBoardTypeModal(selectedBoardType)}>
                      <Edit2 size={13} style={{ marginRight: 4 }} />
                      Edit Type
                    </button>
                    <button className="admin-btn-danger admin-btn-sm" onClick={() => setDeleteConfirm(selectedBoardType.id)}>
                      <Trash2 size={13} style={{ marginRight: 4 }} />
                      Delete
                    </button>
                  </div>
                </div>

                {selectedBoardType.description && (
                  <div style={{ padding: '8px 20px 0', fontSize: 13, color: 'var(--sa-n600, #6b778c)' }}>
                    {selectedBoardType.description}
                  </div>
                )}

                {/* Board preview strip */}
                {columns.length > 0 && (
                  <div className="board-preview-strip">
                    {columns.map((col, idx) => (
                      <div
                        key={idx}
                        className="board-preview-column"
                        style={{ backgroundColor: col.color || '#4a9df8' }}
                      >
                        <div className="board-preview-column-name">{col.columnName}</div>
                        {col.wipLimit != null && (
                          <div className="board-preview-column-wip">WIP: {col.wipLimit}</div>
                        )}
                      </div>
                    ))}
                  </div>
                )}

                {/* Column toolbar */}
                <div className="admin-toolbar" style={{ padding: '12px 20px', borderBottom: '1px solid var(--sa-n200, #dfe1e6)' }}>
                  <div className="admin-toolbar-left">
                    <span style={{ fontSize: 13, color: 'var(--sa-n600)' }}>
                      {columns.length} column{columns.length !== 1 ? 's' : ''} configured
                    </span>
                  </div>
                  <div className="admin-toolbar-right">
                    <button className="admin-btn-primary admin-btn-sm" onClick={openCreateColumnModal}>
                      <Plus size={14} style={{ marginRight: 4 }} />
                      Add Column
                    </button>
                  </div>
                </div>

                {/* Column list */}
                <div className="column-templates-body">
                  {columns.length === 0 ? (
                    <div className="column-templates-empty">
                      <p>No columns defined yet. Add columns to configure the board layout.</p>
                      <button className="admin-btn-primary admin-btn-sm" onClick={openCreateColumnModal}>
                        <Plus size={14} style={{ marginRight: 4 }} />
                        Add First Column
                      </button>
                    </div>
                  ) : (
                    columns.map((col, idx) => (
                      <div className="column-template-row" key={col.id || idx}>
                        <div
                          className="column-template-color-bar"
                          style={{ backgroundColor: col.color || '#4a9df8' }}
                        />
                        <div className="column-template-order">{col.sortOrder}</div>
                        <div className="column-template-info">
                          <div className="column-template-name">{col.columnName}</div>
                          <span className={`column-template-category ${col.statusCategory.toLowerCase()}`}>
                            {categoryLabel(col.statusCategory)}
                          </span>
                        </div>
                        <div className="column-template-wip">
                          {col.wipLimit != null ? (
                            <>WIP: <span className="column-template-wip-value">{col.wipLimit}</span></>
                          ) : (
                            <span style={{ color: 'var(--sa-n400)' }}>No limit</span>
                          )}
                        </div>
                        <div
                          className="column-template-color-swatch"
                          style={{ backgroundColor: col.color || '#4a9df8' }}
                          title={col.color}
                        />
                        <div className="column-template-actions">
                          <button
                            className="column-template-action-btn"
                            title="Edit column"
                            onClick={() => openEditColumnModal(idx)}
                          >
                            <Edit2 size={14} />
                          </button>
                          <button
                            className="column-template-action-btn danger"
                            title="Delete column"
                            onClick={() => setDeleteColumnIndex(idx)}
                          >
                            <Trash2 size={14} />
                          </button>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </>
            ) : (
              <div className="column-templates-placeholder">
                <div className="column-templates-placeholder-icon">
                  <Columns size={48} />
                </div>
                <h4>Select a Board Type</h4>
                <p>Choose a board type from the list to view and manage its column templates.</p>
              </div>
            )}
          </div>
        </div>

        {/* ============================================================
            MODALS
            ============================================================ */}

        {/* --- Board Type Create / Edit Modal --- */}
        {showBoardTypeModal && (
          <div className="admin-modal-overlay" onClick={closeBoardTypeModal}>
            <div className="admin-modal" onClick={e => e.stopPropagation()}>
              <div className="admin-modal-header">
                <h3>{editingBoardType ? 'Edit Board Type' : 'Add Board Type'}</h3>
                <button onClick={closeBoardTypeModal}>&times;</button>
              </div>
              <div className="admin-modal-body">
                {actionError && (
                  <div className="admin-alert admin-alert-error" style={{ marginBottom: 16 }}>
                    {actionError}
                  </div>
                )}

                <div className="admin-form-group">
                  <label className="admin-form-label">
                    Display Name <span style={{ color: '#d73a49' }}>*</span>
                  </label>
                  <input
                    className="admin-form-input"
                    type="text"
                    value={boardTypeForm.displayName}
                    onChange={e => handleBoardTypeNameChange(e.target.value)}
                    placeholder="e.g., Scrum Board, Kanban Board"
                  />
                </div>

                <div className="admin-form-group">
                  <label className="admin-form-label">
                    Type Key <span style={{ color: '#d73a49' }}>*</span>
                  </label>
                  <input
                    className="admin-form-input"
                    type="text"
                    value={boardTypeForm.typeKey}
                    onChange={e =>
                      setBoardTypeForm(prev => ({
                        ...prev,
                        typeKey: e.target.value.toUpperCase().replace(/[^A-Z0-9_]/g, '_'),
                      }))
                    }
                    placeholder="e.g., SCRUM, KANBAN"
                    readOnly={!!editingBoardType}
                    style={editingBoardType ? { backgroundColor: 'var(--sa-n100, #f4f5f7)', cursor: 'default' } : undefined}
                  />
                  {!editingBoardType && (
                    <span style={{ fontSize: 12, color: 'var(--sa-n500)' }}>
                      Auto-generated from name. Uppercase letters, digits, and underscores only.
                    </span>
                  )}
                </div>

                <div className="admin-form-group">
                  <label className="admin-form-label">Description</label>
                  <textarea
                    className="admin-form-input"
                    value={boardTypeForm.description}
                    onChange={e => setBoardTypeForm(prev => ({ ...prev, description: e.target.value }))}
                    placeholder="Describe this board type and when it should be used"
                    rows={3}
                    style={{ resize: 'vertical' }}
                  />
                </div>

                <div className="admin-form-group">
                  <label className="board-type-active-toggle">
                    <input
                      type="checkbox"
                      checked={boardTypeForm.isActive}
                      onChange={e => setBoardTypeForm(prev => ({ ...prev, isActive: e.target.checked }))}
                    />
                    <span>Active</span>
                  </label>
                  <span style={{ fontSize: 12, color: 'var(--sa-n500)' }}>
                    Inactive board types will not appear when creating new boards.
                  </span>
                </div>
              </div>
              <div className="admin-modal-footer">
                <button className="admin-btn-secondary" onClick={closeBoardTypeModal}>
                  Cancel
                </button>
                <button
                  className="admin-btn-primary"
                  onClick={handleSaveBoardType}
                  disabled={!boardTypeForm.displayName.trim() || !boardTypeForm.typeKey.trim() || isSaving}
                >
                  {isSaving
                    ? (editingBoardType ? 'Saving...' : 'Creating...')
                    : (editingBoardType ? 'Save Changes' : 'Create Board Type')}
                </button>
              </div>
            </div>
          </div>
        )}

        {/* --- Column Template Create / Edit Modal --- */}
        {showColumnModal && (
          <div className="admin-modal-overlay" onClick={closeColumnModal}>
            <div className="admin-modal" onClick={e => e.stopPropagation()}>
              <div className="admin-modal-header">
                <h3>{editingColumnIndex !== null ? 'Edit Column' : 'Add Column'}</h3>
                <button onClick={closeColumnModal}>&times;</button>
              </div>
              <div className="admin-modal-body">
                {actionError && (
                  <div className="admin-alert admin-alert-error" style={{ marginBottom: 16 }}>
                    {actionError}
                  </div>
                )}

                <div className="admin-form-group">
                  <label className="admin-form-label">
                    Column Name <span style={{ color: '#d73a49' }}>*</span>
                  </label>
                  <input
                    className="admin-form-input"
                    type="text"
                    value={columnForm.columnName}
                    onChange={e => setColumnForm(prev => ({ ...prev, columnName: e.target.value }))}
                    placeholder="e.g., To Do, In Progress, Done"
                  />
                </div>

                <div className="admin-form-group">
                  <label className="admin-form-label">
                    Status Category <span style={{ color: '#d73a49' }}>*</span>
                  </label>
                  <select
                    className="admin-form-input"
                    value={columnForm.statusCategory}
                    onChange={e => setColumnForm(prev => ({ ...prev, statusCategory: e.target.value }))}
                  >
                    {STATUS_CATEGORIES.map(cat => (
                      <option key={cat.value} value={cat.value}>
                        {cat.label}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="admin-form-group">
                  <label className="admin-form-label">Color</label>
                  <div className="board-color-input-row">
                    <input
                      type="color"
                      className="board-color-picker"
                      value={columnForm.color}
                      onChange={e => setColumnForm(prev => ({ ...prev, color: e.target.value }))}
                    />
                    <input
                      className="admin-form-input"
                      type="text"
                      value={columnForm.color}
                      onChange={e => setColumnForm(prev => ({ ...prev, color: e.target.value }))}
                      placeholder="#4a9df8"
                      style={{ flex: 1 }}
                    />
                    <span className="board-color-preview" style={{ backgroundColor: columnForm.color }} />
                  </div>
                </div>

                <div className="admin-form-group">
                  <label className="admin-form-label">WIP Limit</label>
                  <input
                    className="admin-form-input"
                    type="number"
                    min={0}
                    value={columnForm.wipLimit}
                    onChange={e => setColumnForm(prev => ({ ...prev, wipLimit: e.target.value }))}
                    placeholder="Leave empty for no limit"
                  />
                  <span style={{ fontSize: 12, color: 'var(--sa-n500)' }}>
                    Maximum number of issues allowed in this column. Leave empty for unlimited.
                  </span>
                </div>

                <div className="admin-form-group">
                  <label className="admin-form-label">Sort Order</label>
                  <input
                    className="admin-form-input"
                    type="number"
                    min={1}
                    value={columnForm.sortOrder}
                    onChange={e =>
                      setColumnForm(prev => ({ ...prev, sortOrder: parseInt(e.target.value, 10) || 1 }))
                    }
                  />
                  <span style={{ fontSize: 12, color: 'var(--sa-n500)' }}>
                    Columns are displayed left-to-right by sort order.
                  </span>
                </div>
              </div>
              <div className="admin-modal-footer">
                <button className="admin-btn-secondary" onClick={closeColumnModal}>
                  Cancel
                </button>
                <button
                  className="admin-btn-primary"
                  onClick={handleSaveColumn}
                  disabled={!columnForm.columnName.trim() || isSaving}
                >
                  {isSaving
                    ? (editingColumnIndex !== null ? 'Saving...' : 'Adding...')
                    : (editingColumnIndex !== null ? 'Save Changes' : 'Add Column')}
                </button>
              </div>
            </div>
          </div>
        )}

        {/* --- Delete Board Type Confirmation --- */}
        {deleteConfirm && (
          <div className="admin-modal-overlay" onClick={() => setDeleteConfirm(null)}>
            <div className="admin-modal" onClick={e => e.stopPropagation()}>
              <div className="admin-modal-header">
                <h3>Delete Board Type</h3>
                <button onClick={() => setDeleteConfirm(null)}>&times;</button>
              </div>
              <div className="admin-modal-body">
                <p>
                  Are you sure you want to delete the board type{' '}
                  <strong>{boardTypes?.find(bt => bt.id === deleteConfirm)?.displayName}</strong>?
                  All associated column templates will also be removed. This action cannot be undone.
                </p>
              </div>
              <div className="admin-modal-footer">
                <button className="admin-btn-secondary" onClick={() => setDeleteConfirm(null)}>
                  Cancel
                </button>
                <button
                  className="admin-btn-danger"
                  onClick={handleDeleteBoardType}
                  disabled={deleteMutation.isPending}
                >
                  {deleteMutation.isPending ? 'Deleting...' : 'Delete'}
                </button>
              </div>
            </div>
          </div>
        )}

        {/* --- Delete Column Confirmation --- */}
        {deleteColumnIndex !== null && (
          <div className="admin-modal-overlay" onClick={() => setDeleteColumnIndex(null)}>
            <div className="admin-modal" onClick={e => e.stopPropagation()}>
              <div className="admin-modal-header">
                <h3>Delete Column</h3>
                <button onClick={() => setDeleteColumnIndex(null)}>&times;</button>
              </div>
              <div className="admin-modal-body">
                <p>
                  Are you sure you want to remove the column{' '}
                  <strong>{columns[deleteColumnIndex]?.columnName}</strong> from this board type?
                </p>
              </div>
              <div className="admin-modal-footer">
                <button className="admin-btn-secondary" onClick={() => setDeleteColumnIndex(null)}>
                  Cancel
                </button>
                <button
                  className="admin-btn-danger"
                  onClick={handleDeleteColumn}
                  disabled={updateMutation.isPending}
                >
                  {updateMutation.isPending ? 'Removing...' : 'Remove Column'}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </AdminLayout>
  );
}
