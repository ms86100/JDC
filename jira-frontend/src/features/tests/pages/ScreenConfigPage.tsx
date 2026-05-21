import React, { useState, useEffect, useCallback } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import apiClient from '../../../api/axiosClient';
import {
  Monitor, Layers, Settings, Plus, Trash2, Save, RotateCcw, Edit2, Eye, EyeOff,
  ChevronRight, ChevronDown, GripVertical, X, Check, AlertCircle, Search,
  Copy, ArrowRight, Info, FileText, Calendar, Hash, ToggleLeft, ToggleRight,
  List, CheckSquare, Radio, Type, AlignLeft, Link, Mail, Tag
} from 'lucide-react';

// Types
interface Screen {
  id: string;
  name: string;
  screenType: 'CREATE' | 'EDIT' | 'VIEW' | 'SEARCH';
  position: number;
  createdAt: string;
  updatedAt: string;
}

interface ScreenScheme {
  id: string;
  projectId: string;
  name: string;
  description: string;
  isDefault: boolean;
  screens: ScreenSchemeScreen[];
  createdAt: string;
  updatedAt: string;
}

interface ScreenSchemeScreen {
  id: string;
  screenId: string;
  screenName: string;
  screenType: string;
}

interface CustomField {
  id: string;
  name: string;
  fieldKey: string;
  fieldType: string;
  description: string;
  options: any;
  defaultValue: any;
  validationRules: any;
  projectId: string;
}

interface ScreenField {
  id: string;
  screenId: string;
  fieldId: string;
  position: number;
  isRequired: boolean;
  isEditable: boolean;
  isVisible: boolean;
}

interface FieldType {
  type: string;
  displayName: string;
  description: string;
  editorComponent: string;
  supportsOptions: boolean;
}

// Constants
const SCREEN_TYPES = [
  { value: 'CREATE', label: 'Create', icon: '📝' },
  { value: 'EDIT', label: 'Edit', icon: '✏️' },
  { value: 'VIEW', label: 'View', icon: '👁️' },
  { value: 'SEARCH', label: 'Search', icon: '🔍' }
];

const FIELD_TYPES = [
  { value: 'TEXT', label: 'Text', icon: Type },
  { value: 'TEXTAREA', label: 'Text Area', icon: AlignLeft },
  { value: 'NUMBER', label: 'Number', icon: Hash },
  { value: 'DATE', label: 'Date', icon: Calendar },
  { value: 'DATETIME', label: 'Date Time', icon: Calendar },
  { value: 'SELECT', label: 'Select', icon: List },
  { value: 'MULTI_SELECT', label: 'Multi-Select', icon: List },
  { value: 'CHECKBOX', label: 'Checkbox', icon: CheckSquare },
  { value: 'RADIO', label: 'Radio', icon: Radio },
  { value: 'LABEL', label: 'Label', icon: Tag },
  { value: 'URL', label: 'URL', icon: Link },
  { value: 'EMAIL', label: 'Email', icon: Mail }
];

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
            <button
              onClick={onConfirm}
              className={`btn ${variant === 'danger' ? 'bg-red-600 hover:bg-red-700 text-white' : 'btn-primary'}`}
            >
              {confirmLabel}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

// Drag and Drop List Item
const DraggableFieldItem: React.FC<{
  field: any;
  onRemove: () => void;
  onUpdate: (updates: Partial<ScreenField>) => void;
}> = ({ field, onRemove, onUpdate }) => {
  const [isDragging, setIsDragging] = useState(false);

  return (
    <div
      className={`flex items-center gap-3 p-3 bg-white border rounded-lg mb-2 ${isDragging ? 'opacity-50' : ''}`}
      draggable
      onDragStart={() => setIsDragging(true)}
      onDragEnd={() => setIsDragging(false)}
    >
      <GripVertical className="w-4 h-4 text-gray-400 cursor-move" />
      <div className="flex-1">
        <div className="font-medium">{field.fieldName || field.name}</div>
        <div className="text-xs text-gray-500">{field.fieldType}</div>
      </div>
      <div className="flex items-center gap-4">
        <label className="flex items-center gap-1 text-xs">
          <input
            type="checkbox"
            checked={field.isRequired}
            onChange={(e) => onUpdate({ isRequired: e.target.checked })}
            className="rounded"
          />
          Required
        </label>
        <label className="flex items-center gap-1 text-xs">
          <input
            type="checkbox"
            checked={field.isVisible}
            onChange={(e) => onUpdate({ isVisible: e.target.checked })}
            className="rounded"
          />
          Visible
        </label>
        <label className="flex items-center gap-1 text-xs">
          <input
            type="checkbox"
            checked={field.isEditable}
            onChange={(e) => onUpdate({ isEditable: e.target.checked })}
            className="rounded"
          />
          Editable
        </label>
        <button onClick={onRemove} className="p-1 hover:bg-red-100 rounded text-red-600">
          <Trash2 className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
};

// Custom Field Builder Modal
const CustomFieldBuilderModal: React.FC<{
  open: boolean;
  onClose: () => void;
  onSave: (field: any) => void;
  editingField?: CustomField | null;
}> = ({ open, onClose, onSave, editingField }) => {
  const [formData, setFormData] = useState({
    name: '',
    fieldKey: '',
    fieldType: 'TEXT',
    description: '',
    options: '',
    defaultValue: '',
    validationRules: ''
  });

  useEffect(() => {
    if (editingField) {
      setFormData({
        name: editingField.name,
        fieldKey: editingField.fieldKey,
        fieldType: editingField.fieldType,
        description: editingField.description || '',
        options: editingField.options ? JSON.stringify(editingField.options, null, 2) : '',
        defaultValue: editingField.defaultValue || '',
        validationRules: editingField.validationRules ? JSON.stringify(editingField.validationRules, null, 2) : ''
      });
    } else {
      setFormData({
        name: '',
        fieldKey: '',
        fieldType: 'TEXT',
        description: '',
        options: '',
        defaultValue: '',
        validationRules: ''
      });
    }
  }, [editingField, open]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    let options = formData.options;
    let validationRules = formData.validationRules;

    try {
      if (options) options = JSON.parse(options);
    } catch { options = null; }

    try {
      if (validationRules) validationRules = JSON.parse(validationRules);
    } catch { validationRules = null; }

    onSave({
      ...formData,
      options,
      validationRules
    });
  };

  if (!open) return null;

  const showOptions = ['SELECT', 'MULTI_SELECT', 'RADIO'].includes(formData.fieldType);

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex items-center justify-center min-h-screen px-4">
        <div className="fixed inset-0 bg-black bg-opacity-50" onClick={onClose}></div>
        <div className="relative bg-white rounded-lg shadow-xl max-w-2xl w-full p-6 max-h-[90vh] overflow-y-auto">
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-xl font-semibold">{editingField ? 'Edit Custom Field' : 'Create Custom Field'}</h3>
            <button onClick={onClose} className="p-2 hover:bg-gray-100 rounded">
              <X className="w-5 h-5" />
            </button>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium mb-1">Field Name *</label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium mb-1">Field Key *</label>
                <input
                  type="text"
                  value={formData.fieldKey}
                  onChange={(e) => setFormData({ ...formData, fieldKey: e.target.value.toLowerCase().replace(/[^a-z0-9_]/g, '_') })}
                  className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                  required
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium mb-1">Field Type *</label>
              <select
                value={formData.fieldType}
                onChange={(e) => setFormData({ ...formData, fieldType: e.target.value })}
                className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                {FIELD_TYPES.map(type => (
                  <option key={type.value} value={type.value}>{type.label}</option>
                ))}
              </select>
            </div>

            {showOptions && (
              <div>
                <label className="block text-sm font-medium mb-1">
                  Options (JSON array with value and label)
                </label>
                <textarea
                  value={formData.options}
                  onChange={(e) => setFormData({ ...formData, options: e.target.value })}
                  placeholder='[{"value": "opt1", "label": "Option 1"}, {"value": "opt2", "label": "Option 2"}]'
                  className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500 h-32 font-mono text-sm"
                />
              </div>
            )}

            <div>
              <label className="block text-sm font-medium mb-1">Description</label>
              <textarea
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500 h-20"
              />
            </div>

            <div>
              <label className="block text-sm font-medium mb-1">Default Value</label>
              <input
                type="text"
                value={formData.defaultValue}
                onChange={(e) => setFormData({ ...formData, defaultValue: e.target.value })}
                className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium mb-1">
                Validation Rules (JSON)
              </label>
              <textarea
                value={formData.validationRules}
                onChange={(e) => setFormData({ ...formData, validationRules: e.target.value })}
                placeholder='{"minLength": 1, "maxLength": 100, "pattern": "^[A-Z].*"}'
                className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500 h-24 font-mono text-sm"
              />
            </div>

            <div className="flex justify-end gap-3 pt-4 border-t">
              <button type="button" onClick={onClose} className="btn btn-secondary">
                Cancel
              </button>
              <button type="submit" className="btn btn-primary">
                {editingField ? 'Update' : 'Create'} Field
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

// Validation Rule Editor
const ValidationRuleEditor: React.FC<{
  fieldType: string;
  onSave: (rules: any) => void;
  initialRules?: any;
}> = ({ fieldType, onSave, initialRules }) => {
  const [rules, setRules] = useState(initialRules || {});

  const handleSave = () => {
    onSave(rules);
  };

  const renderRuleInputs = () => {
    switch (fieldType) {
      case 'TEXT':
      case 'TEXTAREA':
      case 'LABEL':
        return (
          <>
            <div>
              <label className="block text-sm font-medium mb-1">Min Length</label>
              <input
                type="number"
                value={rules.minLength || ''}
                onChange={(e) => setRules({ ...rules, minLength: parseInt(e.target.value) || null })}
                className="w-full px-3 py-2 border rounded"
                min="0"
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Max Length</label>
              <input
                type="number"
                value={rules.maxLength || ''}
                onChange={(e) => setRules({ ...rules, maxLength: parseInt(e.target.value) || null })}
                className="w-full px-3 py-2 border rounded"
                min="0"
              />
            </div>
            <div className="col-span-2">
              <label className="block text-sm font-medium mb-1">Regex Pattern</label>
              <input
                type="text"
                value={rules.pattern || ''}
                onChange={(e) => setRules({ ...rules, pattern: e.target.value })}
                className="w-full px-3 py-2 border rounded font-mono"
                placeholder="^[A-Z].*"
              />
            </div>
          </>
        );

      case 'NUMBER':
        return (
          <>
            <div>
              <label className="block text-sm font-medium mb-1">Min Value</label>
              <input
                type="number"
                value={rules.min || ''}
                onChange={(e) => setRules({ ...rules, min: parseFloat(e.target.value) || null })}
                className="w-full px-3 py-2 border rounded"
                step="any"
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Max Value</label>
              <input
                type="number"
                value={rules.max || ''}
                onChange={(e) => setRules({ ...rules, max: parseFloat(e.target.value) || null })}
                className="w-full px-3 py-2 border rounded"
                step="any"
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Decimal Places</label>
              <input
                type="number"
                value={rules.decimalPlaces || ''}
                onChange={(e) => setRules({ ...rules, decimalPlaces: parseInt(e.target.value) || null })}
                className="w-full px-3 py-2 border rounded"
                min="0"
                max="10"
              />
            </div>
          </>
        );

      case 'DATE':
      case 'DATETIME':
        return (
          <>
            <div>
              <label className="block text-sm font-medium mb-1">Min Date</label>
              <input
                type="date"
                value={rules.minDate || ''}
                onChange={(e) => setRules({ ...rules, minDate: e.target.value })}
                className="w-full px-3 py-2 border rounded"
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Max Date</label>
              <input
                type="date"
                value={rules.maxDate || ''}
                onChange={(e) => setRules({ ...rules, maxDate: e.target.value })}
                className="w-full px-3 py-2 border rounded"
              />
            </div>
          </>
        );

      default:
        return <p className="text-gray-500 text-sm">No validation rules available for this field type.</p>;
    }
  };

  return (
    <div className="space-y-4">
      <h4 className="font-medium">Validation Rules</h4>
      <div className="grid grid-cols-2 gap-4">
        {renderRuleInputs()}
      </div>
      <button onClick={handleSave} className="btn btn-primary">
        Apply Rules
      </button>
    </div>
  );
};

// Main Screen Config Page
interface ScreenConfigPageProps {
  projectId: string;
}

const ScreenConfigPage: React.FC<ScreenConfigPageProps> = ({ projectId }) => {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<'schemes' | 'screens' | 'fields'>('schemes');
  const [selectedScheme, setSelectedScheme] = useState<ScreenScheme | null>(null);
  const [selectedScreen, setSelectedScreen] = useState<Screen | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [confirmDialog, setConfirmDialog] = useState<{ open: boolean; title: string; message: string; onConfirm: () => void } | null>(null);
  const [fieldBuilderOpen, setFieldBuilderOpen] = useState(false);
  const [editingField, setEditingField] = useState<CustomField | null>(null);
  const [previewMode, setPreviewMode] = useState(false);

  // Queries
  const { data: screenSchemes = [], isLoading: schemesLoading } = useQuery({
    queryKey: ['screenSchemes', projectId],
    queryFn: async () => {
      const response = await apiClient.get(`/screen-schemes/project/${projectId}`);
      return response.data;
    }
  });

  const { data: screens = [], isLoading: screensLoading } = useQuery({
    queryKey: ['screens'],
    queryFn: async () => {
      const response = await apiClient.get('/screens');
      return response.data;
    }
  });

  const { data: customFields = [], isLoading: fieldsLoading } = useQuery({
    queryKey: ['customFields', projectId],
    queryFn: async () => {
      const response = await apiClient.get(`/custom-fields/project/${projectId}`);
      return response.data;
    }
  });

  const { data: fieldTypes = [] } = useQuery({
    queryKey: ['fieldTypes'],
    queryFn: async () => {
      const response = await apiClient.get('/field-types');
      return response.data;
    }
  });

  // Mutations
  const createScreenScheme = useMutation({
    mutationFn: async (data: { name: string; description: string; isDefault: boolean }) => {
      const response = await apiClient.post('/screen-schemes', { ...data, projectId });
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['screenSchemes'] });
    }
  });

  const updateScreenScheme = useMutation({
    mutationFn: async ({ id, ...data }: { id: string; name?: string; description?: string }) => {
      const response = await apiClient.put(`/screen-schemes/${id}`, null, { params: data });
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['screenSchemes'] });
    }
  });

  const deleteScreenScheme = useMutation({
    mutationFn: async (id: string) => {
      await apiClient.delete(`/screen-schemes/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['screenSchemes'] });
      setSelectedScheme(null);
    }
  });

  const createScreen = useMutation({
    mutationFn: async (data: { name: string; screenType: string }) => {
      const response = await apiClient.post('/screens', data);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['screens'] });
    }
  });

  const deleteScreen = useMutation({
    mutationFn: async (id: string) => {
      await apiClient.delete(`/screens/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['screens'] });
      setSelectedScreen(null);
    }
  });

  const createCustomField = useMutation({
    mutationFn: async (data: any) => {
      const response = await apiClient.post('/custom-fields', { ...data, projectId });
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customFields'] });
      setFieldBuilderOpen(false);
      setEditingField(null);
    }
  });

  const deleteCustomField = useMutation({
    mutationFn: async (id: string) => {
      await apiClient.delete(`/custom-fields/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customFields'] });
    }
  });

  const addScreenToScheme = useMutation({
    mutationFn: async ({ schemeId, screenId, screenType }: { schemeId: string; screenId: string; screenType: string }) => {
      const response = await apiClient.post(`/screen-schemes/${schemeId}/screens`, { screenId, screenType });
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['screenSchemes'] });
    }
  });

  const addFieldToScreen = useMutation({
    mutationFn: async ({ screenId, fieldId, isRequired }: { screenId: string; fieldId: string; isRequired?: boolean }) => {
      const response = await apiClient.post(`/screens/${screenId}/fields`, { fieldId, isRequired });
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['screenFields', selectedScreen?.id] });
    }
  });

  const updateScreenField = useMutation({
    mutationFn: async ({ screenId, fieldId, ...updates }: { screenId: string; fieldId: string; isRequired?: boolean; isVisible?: boolean; isEditable?: boolean }) => {
      const response = await apiClient.put(`/screens/${screenId}/fields`, [
        { fieldId, ...updates }
      ]);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['screenFields', selectedScreen?.id] });
    }
  });

  const removeFieldFromScreen = useMutation({
    mutationFn: async ({ screenId, fieldId }: { screenId: string; fieldId: string }) => {
      await apiClient.delete(`/screens/${screenId}/fields/${fieldId}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['screenFields', selectedScreen?.id] });
    }
  });

  const reorderFields = useMutation({
    mutationFn: async ({ screenId, fieldOrder }: { screenId: string; fieldOrder: string[] }) => {
      const response = await apiClient.put(`/screens/${screenId}/fields/order`, { fieldOrder });
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['screenFields', selectedScreen?.id] });
    }
  });

  const cloneScheme = useMutation({
    mutationFn: async ({ sourceId, newName }: { sourceId: string; newName: string }) => {
      const response = await apiClient.post(`/screen-schemes/${sourceId}/clone`, null, { params: { newName } });
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['screenSchemes'] });
    }
  });

  // Screen fields query
  const { data: screenFields = [] } = useQuery({
    queryKey: ['screenFields', selectedScreen?.id],
    queryFn: async () => {
      if (!selectedScreen) return [];
      const response = await apiClient.get(`/screens/${selectedScreen.id}/fields`);
      return response.data;
    },
    enabled: !!selectedScreen
  });

  // Filter helpers
  const filteredSchemes = screenSchemes.filter(s =>
    s.name.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const filteredScreens = screens.filter(s =>
    s.name.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const filteredFields = customFields.filter(f =>
    f.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    f.fieldKey.toLowerCase().includes(searchTerm.toLowerCase())
  );

  // Get available fields not yet on screen
  const availableFields = customFields.filter(cf =>
    !screenFields.some((sf: any) => sf.fieldId === cf.id)
  );

  // Handlers
  const handleCreateScheme = () => {
    const name = prompt('Enter scheme name:');
    if (!name) return;

    const description = prompt('Enter description (optional):') || '';
    const isDefault = confirm('Set as default scheme?');

    createScreenScheme.mutate({ name, description, isDefault });
  };

  const handleCloneScheme = (scheme: ScreenScheme) => {
    const newName = prompt('Enter new scheme name:', `${scheme.name} (Copy)`);
    if (!newName) return;

    cloneScheme.mutate({ sourceId: scheme.id, newName });
  };

  const handleDeleteScheme = (scheme: ScreenScheme) => {
    setConfirmDialog({
      open: true,
      title: 'Delete Screen Scheme',
      message: `Are you sure you want to delete "${scheme.name}"? This action cannot be undone.`,
      onConfirm: () => {
        deleteScreenScheme.mutate(scheme.id);
        setConfirmDialog(null);
      }
    });
  };

  const handleCreateScreen = () => {
    const name = prompt('Enter screen name:');
    if (!name) return;

    const screenType = prompt('Enter screen type (CREATE, EDIT, VIEW, SEARCH):', 'CREATE');
    if (!screenType || !['CREATE', 'EDIT', 'VIEW', 'SEARCH'].includes(screenType.toUpperCase())) {
      alert('Invalid screen type');
      return;
    }

    createScreen.mutate({ name, screenType: screenType.toUpperCase() });
  };

  const handleDeleteScreen = (screen: Screen) => {
    setConfirmDialog({
      open: true,
      title: 'Delete Screen',
      message: `Are you sure you want to delete "${screen.name}"? This action cannot be undone.`,
      onConfirm: () => {
        deleteScreen.mutate(screen.id);
        setConfirmDialog(null);
      }
    });
  };

  const handleDeleteField = (field: CustomField) => {
    setConfirmDialog({
      open: true,
      title: 'Delete Custom Field',
      message: `Are you sure you want to delete "${field.name}"? This will remove the field from all screens and tests.`,
      onConfirm: () => {
        deleteCustomField.mutate(field.id);
        setConfirmDialog(null);
      }
    });
  };

  const handleAddScreenToScheme = (scheme: ScreenScheme) => {
    if (screens.length === 0) {
      alert('No screens available. Create a screen first.');
      return;
    }

    const screenType = prompt('Enter screen type to add (CREATE, EDIT, VIEW, SEARCH):');
    if (!screenType) return;

    const screenOptions = screens.filter(s => s.screenType === screenType.toUpperCase());
    if (screenOptions.length === 0) {
      alert(`No screens available with type ${screenType.toUpperCase()}. Create one first.`);
      return;
    }

    const screenId = screenOptions[0].id;
    addScreenToScheme.mutate({ schemeId: scheme.id, screenId, screenType: screenType.toUpperCase() });
  };

  const handleAddFieldToScreen = (fieldId: string) => {
    if (!selectedScreen) return;
    addFieldToScreen.mutate({ screenId: selectedScreen.id, fieldId });
  };

  const handleUpdateField = (fieldId: string, updates: Partial<ScreenField>) => {
    if (!selectedScreen) return;
    updateScreenField.mutate({ screenId: selectedScreen.id, fieldId, ...updates });
  };

  const handleRemoveField = (fieldId: string) => {
    if (!selectedScreen) return;
    removeFieldFromScreen.mutate({ screenId: selectedScreen.id, fieldId });
  };

  const handleReorderFields = (newOrder: string[]) => {
    if (!selectedScreen) return;
    reorderFields.mutate({ screenId: selectedScreen.id, fieldOrder: newOrder });
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white border-b px-6 py-4">
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Screen & Field Configuration</h1>
            <p className="text-gray-500 mt-1">Manage screens, screen schemes, and custom fields</p>
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => setPreviewMode(!previewMode)}
              className={`btn ${previewMode ? 'btn-primary' : 'btn-secondary'}`}
            >
              {previewMode ? 'Edit Mode' : 'Preview Mode'}
            </button>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="bg-white border-b">
        <div className="flex px-6">
          <button
            onClick={() => setActiveTab('schemes')}
            className={`px-4 py-3 font-medium border-b-2 ${
              activeTab === 'schemes'
                ? 'border-blue-600 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            Screen Schemes
          </button>
          <button
            onClick={() => setActiveTab('screens')}
            className={`px-4 py-3 font-medium border-b-2 ${
              activeTab === 'screens'
                ? 'border-blue-600 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            Screens
          </button>
          <button
            onClick={() => setActiveTab('fields')}
            className={`px-4 py-3 font-medium border-b-2 ${
              activeTab === 'fields'
                ? 'border-blue-600 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            Custom Fields
          </button>
        </div>
      </div>

      {/* Search Bar */}
      <div className="px-6 py-4">
        <div className="relative max-w-md">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder={`Search ${activeTab}...`}
            className="w-full pl-10 pr-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
      </div>

      {/* Content */}
      <div className="px-6 pb-6">
        {activeTab === 'schemes' && (
          <div className="grid grid-cols-12 gap-6">
            {/* Schemes List */}
            <div className="col-span-4">
              <div className="bg-white rounded-lg border">
                <div className="p-4 border-b flex justify-between items-center">
                  <h2 className="font-semibold">Screen Schemes</h2>
                  <button onClick={handleCreateScheme} className="btn btn-primary btn-sm">
                    <Plus className="w-4 h-4 mr-1" />
                    Add
                  </button>
                </div>
                <div className="p-2 max-h-[600px] overflow-y-auto">
                  {schemesLoading ? (
                    <div className="p-4 text-center text-gray-500">Loading...</div>
                  ) : filteredSchemes.length === 0 ? (
                    <div className="p-4 text-center text-gray-500">No schemes found</div>
                  ) : (
                    filteredSchemes.map(scheme => (
                      <div
                        key={scheme.id}
                        onClick={() => setSelectedScheme(scheme)}
                        className={`p-3 rounded-lg cursor-pointer mb-2 ${
                          selectedScheme?.id === scheme.id
                            ? 'bg-blue-50 border-blue-200'
                            : 'hover:bg-gray-50 border-transparent border'
                        }`}
                      >
                        <div className="flex justify-between items-start">
                          <div>
                            <div className="font-medium flex items-center gap-2">
                              {scheme.name}
                              {scheme.isDefault && (
                                <span className="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded">Default</span>
                              )}
                            </div>
                            <div className="text-sm text-gray-500 mt-1">
                              {scheme.screens?.length || 0} screens
                            </div>
                          </div>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>
            </div>

            {/* Scheme Details */}
            <div className="col-span-8">
              {selectedScheme ? (
                <div className="bg-white rounded-lg border">
                  <div className="p-4 border-b">
                    <div className="flex justify-between items-center">
                      <div>
                        <h2 className="text-xl font-semibold">{selectedScheme.name}</h2>
                        <p className="text-gray-500 text-sm mt-1">{selectedScheme.description}</p>
                      </div>
                      <div className="flex gap-2">
                        <button
                          onClick={() => handleCloneScheme(selectedScheme)}
                          className="btn btn-secondary btn-sm"
                        >
                          <Copy className="w-4 h-4 mr-1" />
                          Clone
                        </button>
                        <button
                          onClick={() => handleDeleteScheme(selectedScheme)}
                          className="btn btn-secondary btn-sm text-red-600"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </div>
                  </div>

                  <div className="p-4">
                    <h3 className="font-medium mb-4">Screens in Scheme</h3>
                    <div className="space-y-3">
                      {SCREEN_TYPES.map(st => {
                        const assignedScreen = selectedScheme.screens?.find(s => s.screenType === st.value);
                        return (
                          <div key={st.value} className="flex items-center justify-between p-3 border rounded-lg">
                            <div className="flex items-center gap-3">
                              <span className="text-2xl">{st.icon}</span>
                              <div>
                                <div className="font-medium">{st.label} Screen</div>
                                <div className="text-sm text-gray-500">
                                  {assignedScreen ? assignedScreen.screenName : 'Not assigned'}
                                </div>
                              </div>
                            </div>
                            <div>
                              {assignedScreen ? (
                                <button
                                  onClick={() => setSelectedScreen(screens.find(s => s.id === assignedScreen.screenId) || null)}
                                  className="btn btn-secondary btn-sm"
                                >
                                  Configure
                                </button>
                              ) : (
                                <button
                                  onClick={() => handleAddScreenToScheme(selectedScheme)}
                                  className="btn btn-secondary btn-sm"
                                >
                                  <Plus className="w-4 h-4 mr-1" />
                                  Assign
                                </button>
                              )}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                </div>
              ) : (
                <div className="bg-white rounded-lg border p-8 text-center text-gray-500">
                  Select a screen scheme to view details
                </div>
              )}
            </div>
          </div>
        )}

        {activeTab === 'screens' && (
          <div className="grid grid-cols-12 gap-6">
            {/* Screens List */}
            <div className="col-span-4">
              <div className="bg-white rounded-lg border">
                <div className="p-4 border-b flex justify-between items-center">
                  <h2 className="font-semibold">Screens</h2>
                  <button onClick={handleCreateScreen} className="btn btn-primary btn-sm">
                    <Plus className="w-4 h-4 mr-1" />
                    Add
                  </button>
                </div>
                <div className="p-2 max-h-[600px] overflow-y-auto">
                  {screensLoading ? (
                    <div className="p-4 text-center text-gray-500">Loading...</div>
                  ) : filteredScreens.length === 0 ? (
                    <div className="p-4 text-center text-gray-500">No screens found</div>
                  ) : (
                    filteredScreens.map(screen => (
                      <div
                        key={screen.id}
                        onClick={() => setSelectedScreen(screen)}
                        className={`p-3 rounded-lg cursor-pointer mb-2 ${
                          selectedScreen?.id === screen.id
                            ? 'bg-blue-50 border-blue-200'
                            : 'hover:bg-gray-50 border-transparent border'
                        }`}
                      >
                        <div className="flex justify-between items-start">
                          <div>
                            <div className="font-medium">{screen.name}</div>
                            <div className="text-sm text-gray-500">{screen.screenType}</div>
                          </div>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>
            </div>

            {/* Screen Configuration */}
            <div className="col-span-8">
              {selectedScreen ? (
                <div className="space-y-6">
                  {/* Screen Info */}
                  <div className="bg-white rounded-lg border">
                    <div className="p-4 border-b">
                      <div className="flex justify-between items-center">
                        <div>
                          <h2 className="text-xl font-semibold">{selectedScreen.name}</h2>
                          <p className="text-gray-500 text-sm mt-1">
                            Type: {selectedScreen.screenType}
                          </p>
                        </div>
                        <div className="flex gap-2">
                          <button
                            onClick={() => handleDeleteScreen(selectedScreen)}
                            className="btn btn-secondary btn-sm text-red-600"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </div>
                    </div>

                    {/* Available Fields */}
                    <div className="p-4 border-b">
                      <div className="flex justify-between items-center mb-4">
                        <h3 className="font-medium">Add Fields</h3>
                      </div>
                      <div className="flex flex-wrap gap-2">
                        {availableFields.slice(0, 10).map(field => (
                          <button
                            key={field.id}
                            onClick={() => handleAddFieldToScreen(field.id)}
                            className="px-3 py-1.5 bg-gray-100 hover:bg-gray-200 rounded text-sm flex items-center gap-2"
                          >
                            <Plus className="w-4 h-4" />
                            {field.name}
                          </button>
                        ))}
                        {availableFields.length === 0 && (
                          <span className="text-gray-500 text-sm">All fields have been added</span>
                        )}
                      </div>
                    </div>

                    {/* Screen Fields */}
                    <div className="p-4">
                      <h3 className="font-medium mb-4">
                        Fields on Screen ({screenFields.length})
                      </h3>
                      {screenFields.length === 0 ? (
                        <div className="text-center text-gray-500 py-8">
                          No fields configured. Add fields from the list above.
                        </div>
                      ) : (
                        <div className="space-y-2">
                          {screenFields.map((field: any) => (
                            <DraggableFieldItem
                              key={field.id}
                              field={{
                                ...field,
                                fieldName: customFields.find((cf: any) => cf.id === field.fieldId)?.name || field.fieldId
                              }}
                              onRemove={() => handleRemoveField(field.fieldId)}
                              onUpdate={(updates) => handleUpdateField(field.fieldId, updates)}
                            />
                          ))}
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Screen Preview */}
                  {previewMode && (
                    <div className="bg-white rounded-lg border">
                      <div className="p-4 border-b">
                        <h3 className="font-medium">Screen Preview</h3>
                      </div>
                      <div className="p-4">
                        <div className="border rounded-lg p-4 space-y-4">
                          {screenFields.filter((f: any) => f.isVisible).map((field: any) => {
                            const cf = customFields.find((c: any) => c.id === field.fieldId);
                            return (
                              <div key={field.id} className="space-y-1">
                                <label className="block text-sm font-medium">
                                  {cf?.name || 'Unknown Field'}
                                  {field.isRequired && <span className="text-red-500 ml-1">*</span>}
                                </label>
                                {previewMode && (
                                  cf?.fieldType === 'TEXT' && (
                                    <input
                                      type="text"
                                      disabled={!field.isEditable}
                                      className="w-full px-3 py-2 border rounded bg-gray-50"
                                      placeholder={cf?.defaultValue || ''}
                                    />
                                  ) ||
                                  cf?.fieldType === 'TEXTAREA' && (
                                    <textarea
                                      disabled={!field.isEditable}
                                      className="w-full px-3 py-2 border rounded bg-gray-50 h-24"
                                      placeholder={cf?.defaultValue || ''}
                                    />
                                  ) ||
                                  cf?.fieldType === 'NUMBER' && (
                                    <input
                                      type="number"
                                      disabled={!field.isEditable}
                                      className="w-full px-3 py-2 border rounded bg-gray-50"
                                      placeholder={cf?.defaultValue || ''}
                                    />
                                  ) ||
                                  cf?.fieldType === 'DATE' && (
                                    <input
                                      type="date"
                                      disabled={!field.isEditable}
                                      className="w-full px-3 py-2 border rounded bg-gray-50"
                                    />
                                  ) ||
                                  (cf?.fieldType === 'SELECT' || cf?.fieldType === 'RADIO') && (
                                    <select
                                      disabled={!field.isEditable}
                                      className="w-full px-3 py-2 border rounded bg-gray-50"
                                    >
                                      <option value="">Select...</option>
                                      {cf?.options?.map((opt: any) => (
                                        <option key={opt.value} value={opt.value}>{opt.label || opt.value}</option>
                                      ))}
                                    </select>
                                  ) ||
                                  cf?.fieldType === 'CHECKBOX' && (
                                    <input
                                      type="checkbox"
                                      disabled={!field.isEditable}
                                      className="rounded"
                                    />
                                  ) || (
                                    <input
                                      type="text"
                                      disabled
                                      className="w-full px-3 py-2 border rounded bg-gray-50"
                                      placeholder="Field preview not available"
                                    />
                                  )
                                )}
                              </div>
                            );
                          })}
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                <div className="bg-white rounded-lg border p-8 text-center text-gray-500">
                  Select a screen to configure fields
                </div>
              )}
            </div>
          </div>
        )}

        {activeTab === 'fields' && (
          <div className="bg-white rounded-lg border">
            <div className="p-4 border-b flex justify-between items-center">
              <h2 className="font-semibold">Custom Fields</h2>
              <button
                onClick={() => {
                  setEditingField(null);
                  setFieldBuilderOpen(true);
                }}
                className="btn btn-primary btn-sm"
              >
                <Plus className="w-4 h-4 mr-1" />
                Create Field
              </button>
            </div>
            <div className="p-4">
              {fieldsLoading ? (
                <div className="text-center text-gray-500">Loading...</div>
              ) : filteredFields.length === 0 ? (
                <div className="text-center text-gray-500 py-8">
                  {searchTerm ? 'No fields match your search' : 'No custom fields created yet'}
                </div>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                  {filteredFields.map(field => {
                    const fieldType = FIELD_TYPES.find(t => t.value === field.fieldType);
                    const FieldIcon = fieldType?.icon || Type;

                    return (
                      <div
                        key={field.id}
                        className="border rounded-lg p-4 hover:shadow-md transition-shadow"
                      >
                        <div className="flex items-start justify-between">
                          <div className="flex items-center gap-3">
                            <div className="p-2 bg-blue-100 rounded">
                              <FieldIcon className="w-5 h-5 text-blue-600" />
                            </div>
                            <div>
                              <div className="font-medium">{field.name}</div>
                              <div className="text-xs text-gray-500">{field.fieldKey}</div>
                            </div>
                          </div>
                        </div>
                        <div className="mt-3 flex items-center gap-2">
                          <span className="text-xs bg-gray-100 px-2 py-1 rounded">
                            {field.fieldType}
                          </span>
                          {(field.fieldType === 'SELECT' || field.fieldType === 'MULTI_SELECT' || field.fieldType === 'RADIO') && (
                            <span className="text-xs bg-gray-100 px-2 py-1 rounded">
                              {field.options?.length || 0} options
                            </span>
                          )}
                        </div>
                        <div className="mt-4 flex justify-end gap-2">
                          <button
                            onClick={() => {
                              setEditingField(field);
                              setFieldBuilderOpen(true);
                            }}
                            className="btn btn-secondary btn-sm"
                          >
                            <Edit2 className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => handleDeleteField(field)}
                            className="btn btn-secondary btn-sm text-red-600"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Modals */}
      <ConfirmDialog
        open={confirmDialog?.open || false}
        title={confirmDialog?.title || ''}
        message={confirmDialog?.message || ''}
        onConfirm={confirmDialog?.onConfirm || (() => {})}
        onCancel={() => setConfirmDialog(null)}
        variant="danger"
      />

      <CustomFieldBuilderModal
        open={fieldBuilderOpen}
        onClose={() => {
          setFieldBuilderOpen(false);
          setEditingField(null);
        }}
        onSave={(data) => createCustomField.mutate(data)}
        editingField={editingField}
      />
    </div>
  );
};

export default ScreenConfigPage;