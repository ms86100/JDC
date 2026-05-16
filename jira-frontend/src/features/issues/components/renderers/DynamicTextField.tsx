import React from 'react';
import { FieldDefinition } from '../../context/FieldDefinitionContext';

interface DynamicTextFieldProps {
  fieldDef: FieldDefinition;
  value?: string;
  onChange?: (value: string) => void;
  readOnly?: boolean;
  multiline?: boolean;
  placeholder?: string;
}

/**
 * Sanitize HTML content to prevent XSS attacks.
 * Strips dangerous tags and event handlers.
 */
function sanitizeHtml(input: string): string {
  if (!input) return '';

  return input
    // Remove script tags and their contents
    .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
    // Remove event handlers (onclick, onerror, etc.)
    .replace(/\bon\w+\s*=\s*(?:"[^"]*"|'[^']*'|[^\s>]*)/gi, '')
    // Remove javascript: URLs
    .replace(/javascript:/gi, '')
    // Remove data: URLs that could be used for XSS
    .replace(/data:/gi, '')
    // Remove expression() CSS
    .replace(/expression\s*\(/gi, '')
    // Remove vbscript: URLs
    .replace(/vbscript:/gi, '');
}

/**
 * Escape plain text for safe rendering.
 */
function escapeHtml(input: string): string {
  return input
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#x27;');
}

export default function DynamicTextField({
  fieldDef,
  value,
  onChange,
  readOnly = false,
  multiline = false,
  placeholder
}: DynamicTextFieldProps) {
  const maxLength = fieldDef.schemaDefinition?.maxLength || 5000;
  const allowHtml = fieldDef.fieldType === 'RICHTEXT';

  if (readOnly) {
    if (multiline || allowHtml) {
      // For rich text fields, sanitize but allow basic formatting
      const sanitizedHtml = sanitizeHtml(value || '');
      return (
        <div
          className="dynamic-field-text-view"
          dangerouslySetInnerHTML={{
            __html: sanitizedHtml || '<span class="no-value">No value</span>'
          }}
        />
      );
    }
    // For plain text, escape HTML
    const escapedText = escapeHtml(value || '');
    return (
      <span className="dynamic-field-text-view">
        {escapedText ? <span>{escapedText}</span> : <span className="no-value">-</span>}
      </span>
    );
  }

  if (multiline || fieldDef.fieldType === 'TEXTAREA') {
    return (
      <textarea
        className="dynamic-textarea"
        value={value || ''}
        onChange={(e) => onChange?.(e.target.value)}
        maxLength={maxLength}
        placeholder={placeholder || `Enter ${fieldDef.displayName.toLowerCase()}...`}
        rows={4}
      />
    );
  }

  return (
    <input
      type="text"
      className="dynamic-text-input"
      value={value || ''}
      onChange={(e) => onChange?.(e.target.value)}
      maxLength={maxLength}
      placeholder={placeholder || `Enter ${fieldDef.displayName.toLowerCase()}...`}
    />
  );
}