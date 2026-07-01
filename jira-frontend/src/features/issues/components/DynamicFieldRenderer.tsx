import React, { useMemo } from 'react';
import { useFieldDefinitions, FieldDefinition, RendererType } from '../context/FieldDefinitionContext';
import DynamicTextField from './renderers/DynamicTextField';
import DynamicSelectField from './renderers/DynamicSelectField';
import DynamicUserPicker from './renderers/DynamicUserPicker';
import DynamicDateTimePicker from './renderers/DynamicDateTimePicker';
import DynamicMultiSelect from './renderers/DynamicMultiSelect';
import DynamicNumberField from './renderers/DynamicNumberField';
import DynamicDurationField from './renderers/DynamicDurationField';
import DynamicLabelEditor from './renderers/DynamicLabelEditor';
import DynamicReadOnly from './renderers/DynamicReadOnly';
import './DynamicFieldRenderer.css';

interface DynamicFieldRendererProps {
  fieldKey: string;
  value: any;
  onChange?: (value: any) => void;
  mode?: 'view' | 'edit';
  issueId?: string;
  className?: string;
  disabled?: boolean;
  placeholder?: string;
}

export default function DynamicFieldRenderer({
  fieldKey,
  value,
  onChange,
  mode = 'view',
  issueId,
  className = '',
  disabled = false,
  placeholder
}: DynamicFieldRendererProps) {
  const { getFieldByKey } = useFieldDefinitions();
  const fieldDef = getFieldByKey(fieldKey);

  if (!fieldDef) {
    return (
      <div className="dynamic-field-unknown">
        <span className="dynamic-field-unknown-label">Unknown Field</span>
        <span className="dynamic-field-unknown-key">{fieldKey}</span>
      </div>
    );
  }

  if (fieldDef.hidden && mode === 'view') {
    return null;
  }

  const renderer = fieldDef.renderer || getDefaultRenderer(fieldDef.fieldType);
  const isReadOnly = fieldDef.readOnly || disabled || mode === 'view';

  const renderComponent = () => {
    switch (renderer) {
      case 'TEXT':
      case 'TEXTAREA':
        return (
          <DynamicTextField
            fieldDef={fieldDef}
            value={value}
            onChange={onChange}
            readOnly={isReadOnly}
            placeholder={placeholder}
          />
        );

      case 'RICHTEXT':
        return (
          <DynamicTextField
            fieldDef={fieldDef}
            value={value}
            onChange={onChange}
            readOnly={isReadOnly}
            multiline
            placeholder={placeholder}
          />
        );

      case 'SELECT':
      case 'SINGLE_SELECT':
        return (
          <DynamicSelectField
            fieldDef={fieldDef}
            value={value}
            onChange={onChange}
            readOnly={isReadOnly}
            placeholder={placeholder}
          />
        );

      case 'MULTI_SELECT':
        return (
          <DynamicMultiSelect
            fieldDef={fieldDef}
            value={value}
            onChange={onChange}
            readOnly={isReadOnly}
            placeholder={placeholder}
          />
        );

      case 'USER_PICKER':
        return (
          <DynamicUserPicker
            fieldDef={fieldDef}
            value={value}
            onChange={onChange}
            readOnly={isReadOnly}
            issueId={issueId}
          />
        );

      case 'GROUP_PICKER':
        return (
          <DynamicUserPicker
            fieldDef={fieldDef}
            value={value}
            onChange={onChange}
            readOnly={isReadOnly}
            type="group"
          />
        );

      case 'DATETIME_PICKER':
        return (
          <DynamicDateTimePicker
            fieldDef={fieldDef}
            value={value}
            onChange={onChange}
            readOnly={isReadOnly}
          />
        );

      case 'NUMBER':
      case 'SLIDER':
        return (
          <DynamicNumberField
            fieldDef={fieldDef}
            value={value}
            onChange={onChange}
            readOnly={isReadOnly}
            placeholder={placeholder}
          />
        );

      case 'DURATION':
        return (
          <DynamicDurationField
            fieldDef={fieldDef}
            value={value}
            onChange={onChange}
            readOnly={isReadOnly}
          />
        );

      case 'LABEL_EDITOR':
        return (
          <DynamicLabelEditor
            fieldDef={fieldDef}
            value={value}
            onChange={onChange}
            readOnly={isReadOnly}
          />
        );

      case 'READ_ONLY':
      case 'VOTES':
      case 'WATCHERS':
        return (
          <DynamicReadOnly
            fieldDef={fieldDef}
            value={value}
          />
        );

      case 'SECURITY_LEVEL':
        return (
          <DynamicSelectField
            fieldDef={fieldDef}
            value={value}
            onChange={onChange}
            readOnly={isReadOnly}
          />
        );

      case 'SPRINT_SELECTOR':
        return (
          <DynamicSelectField
            fieldDef={fieldDef}
            value={value}
            onChange={onChange}
            readOnly={isReadOnly}
            placeholder={placeholder || 'Select Sprint'}
          />
        );

      case 'EPIC_LINK':
        return (
          <DynamicSelectField
            fieldDef={fieldDef}
            value={value}
            onChange={onChange}
            readOnly={isReadOnly}
            placeholder={placeholder || 'Select Epic'}
          />
        );

      case 'CUSTOM':
      default:
        return (
          <DynamicTextField
            fieldDef={fieldDef}
            value={value}
            onChange={onChange}
            readOnly={isReadOnly}
            placeholder={placeholder}
          />
        );
    }
  };

  return (
    <div className={`dynamic-field-renderer ${className}`}>
      <div className="dynamic-field-label">
        <span className="dynamic-field-name">{fieldDef.displayName}</span>
        {fieldDef.required && <span className="dynamic-field-required">*</span>}
      </div>
      <div className="dynamic-field-content">
        {renderComponent()}
      </div>
      {fieldDef.description && mode === 'edit' && (
        <div className="dynamic-field-description">{fieldDef.description}</div>
      )}
    </div>
  );
}

function getDefaultRenderer(fieldType: string): RendererType {
  const mapping: Record<string, RendererType> = {
    TEXT: 'TEXT',
    TEXTAREA: 'TEXTAREA',
    RICHTEXT: 'RICHTEXT',
    NUMBER: 'NUMBER',
    DATE: 'DATETIME_PICKER',
    DATETIME: 'DATETIME_PICKER',
    SINGLE_SELECT: 'SELECT',
    MULTI_SELECT: 'MULTI_SELECT',
    CHECKBOX: 'CHECKBOX',
    RADIO: 'RADIO',
    USER: 'USER_PICKER',
    GROUP: 'GROUP_PICKER',
    PROJECT: 'PROJECT_PICKER',
    VERSION: 'MULTI_SELECT',
    LABEL: 'LABEL_EDITOR',
    SECURITY_LEVEL: 'SECURITY_LEVEL',
    URL: 'TEXT',
    EMAIL: 'TEXT',
    DURATION: 'DURATION',
    SPRINT: 'SPRINT_SELECTOR',
    EPIC: 'EPIC_LINK',
    VOTES: 'VOTES',
    WATCHERS: 'WATCHERS',
    CUSTOM: 'CUSTOM',
    UNKNOWN: 'TEXT'
  };
  return mapping[fieldType] || 'TEXT';
}

export function DynamicFieldGroup({
  region,
  fields,
  values,
  onChange,
  mode = 'view',
  issueId
}: {
  region: string;
  fields: FieldDefinition[];
  values: Record<string, any>;
  onChange?: (key: string, value: any) => void;
  mode?: 'view' | 'edit';
  issueId?: string;
}) {
  const regionFields = useMemo(() =>
    fields.filter(f => f.screenRegion === region),
    [fields, region]
  );

  if (regionFields.length === 0) {
    return null;
  }

  return (
    <div className={`dynamic-field-group region-${region.toLowerCase()}`}>
      {regionFields.map(field => (
        <DynamicFieldRenderer
          key={field.fieldKey}
          fieldKey={field.fieldKey}
          value={values[field.fieldKey]}
          onChange={onChange ? (val) => onChange(field.fieldKey, val) : undefined}
          mode={mode}
          issueId={issueId}
        />
      ))}
    </div>
  );
}