import React, { useCallback } from 'react';
import { FieldDefinition } from '../../context/FieldDefinitionContext';

interface DynamicReadOnlyProps {
  fieldDef: FieldDefinition;
  value?: any;
}

/**
 * Sanitize user input to prevent XSS attacks.
 * Escapes HTML special characters and handles rich text safely.
 */
function sanitizeValue(input: any): string {
  if (input === undefined || input === null) {
    return '';
  }

  const str = String(input);

  // Escape HTML special characters
  const escaped = str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#x27;')
    .replace(/\//g, '&#x2F;');

  return escaped;
}

/**
 * Safely render rich text content (if field allows HTML).
 * Uses DOMPurify-style approach for sanitization.
 */
function safeRichText(input: any, allowHtml: boolean): React.ReactNode {
  if (!allowHtml) {
    return <span dangerouslySetInnerHTML={{ __html: sanitizeValue(input) }} />;
  }

  // For allowed HTML, do minimal sanitization but preserve formatting
  const str = String(input);
  const sanitized = str
    .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
    .replace(/\bon\w+\s*=\s*(?:"[^"]*"|'[^']*'|[^\s>]*)/gi, '')
    .replace(/javascript\s*:/gi, '');

  return <span dangerouslySetInnerHTML={{ __html: sanitized }} />;
}

export default function DynamicReadOnly({
  fieldDef,
  value
}: DynamicReadOnlyProps) {
  const renderValue = () => {
    if (value === undefined || value === null) {
      return <span className="no-value">-</span>;
    }

    switch (fieldDef.fieldType) {
      case 'NUMBER':
      case 'DURATION':
        if (typeof value === 'number') {
          const totalHours = value / 3600;
          if (totalHours >= 24) {
            const days = Math.floor(totalHours / 8);
            const hours = Math.floor(totalHours % 8);
            const mins = Math.floor((value % 3600) / 60);
            return `${days}d ${hours}h ${mins}m`;
          }
          const h = Math.floor(totalHours);
          const m = Math.floor((value % 3600) / 60);
          return m > 0 ? `${h}h ${m}m` : `${h}h`;
        }
        return <span className="dynamic-number-value">{sanitizeValue(value)}</span>;

      case 'VOTES':
      case 'WATCHERS':
        return (
          <span className="dynamic-count-badge">
            <span className="dynamic-count-icon">
              {fieldDef.fieldType === 'VOTES' ? '▲' : '👁'}
            </span>
            <span className="dynamic-count-value">{value}</span>
          </span>
        );

      case 'RICHTEXT':
        // Allow HTML rendering for rich text fields but sanitize first
        return safeRichText(value, true);

      case 'TEXTAREA':
      case 'TEXT':
        // Render plain text safely
        return safeRichText(value, false);

      case 'DATETIME':
        try {
          const date = new Date(value);
          // Validate date is real before rendering
          if (isNaN(date.getTime())) {
            return <span className="dynamic-value-error">{sanitizeValue(value)}</span>;
          }
          return date.toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: 'numeric',
            minute: '2-digit'
          });
        } catch {
          return <span className="dynamic-value-error">{sanitizeValue(value)}</span>;
        }

      case 'DATE':
        try {
          const date = new Date(value);
          if (isNaN(date.getTime())) {
            return <span className="dynamic-value-error">{sanitizeValue(value)}</span>;
          }
          return date.toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
          });
        } catch {
          return <span className="dynamic-value-error">{sanitizeValue(value)}</span>;
        }

      case 'BOOLEAN':
        return value ? 'Yes' : 'No';

      case 'MULTI_SELECT':
      case 'LABEL':
        if (Array.isArray(value)) {
          // Sanitize each array item
          const sanitized = value.map(item => sanitizeValue(item));
          return <span className="dynamic-array-value">{sanitized.join(', ')}</span>;
        }
        return <span>{sanitizeValue(value)}</span>;

      case 'USER':
        if (typeof value === 'object' && value !== null) {
          // Safely render user object
          const name = value.name ? sanitizeValue(value.name) : '';
          const email = value.email ? sanitizeValue(value.email) : '';
          return <span className="dynamic-user-value">{name || email}</span>;
        }
        return <span>{sanitizeValue(value)}</span>;

      default:
        if (typeof value === 'object') {
          // Safely render JSON stringified objects
          try {
            const jsonStr = JSON.stringify(value);
            return <span className="dynamic-json-value">{sanitizeValue(jsonStr)}</span>;
          } catch {
            return <span className="no-value">-</span>;
          }
        }
        return <span>{sanitizeValue(String(value))}</span>;
    }
  };

  return (
    <div className="dynamic-readonly">
      {renderValue()}
    </div>
  );
}