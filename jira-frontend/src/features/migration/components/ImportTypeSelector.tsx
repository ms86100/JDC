import React from 'react';
import type { ImportType } from '../types/migration';

export type { ImportType };

interface ImportTypeSelectorProps {
  selectedType: ImportType | null;
  onTypeSelect: (type: ImportType) => void;
  disabled?: boolean;
}

const IMPORT_TYPES = [
  {
    type: 'csv' as const,
    title: 'CSV Import',
    description: 'Import issues, projects, and users from CSV files. Download templates for each data type.',
    icon: '📄',
    iconBg: 'bg-green-100',
    iconColor: 'text-green-600',
    borderColor: 'hover:border-green-400',
    selectedColor: 'border-green-500 bg-green-50',
  },
  {
    type: 'workflow-xml' as const,
    title: 'Workflow XML (DC)',
    description: 'Import Jira Data Center workflow-descriptor XML with validators, conditions, post-functions, and scheme binding.',
    icon: '⚙',
    iconBg: 'bg-indigo-100',
    iconColor: 'text-indigo-600',
    borderColor: 'hover:border-indigo-400',
    selectedColor: 'border-indigo-500 bg-indigo-50',
  },
  {
    type: 'issue-xml' as const,
    title: 'Issue XML (Jira DC)',
    description:
      'Import Jira Data Center issue export XML (RSS/channel). Issues, comments, worklogs, links, custom fields, and optional attachment bundle.',
    icon: '📋',
    iconBg: 'bg-teal-100',
    iconColor: 'text-teal-700',
    borderColor: 'hover:border-teal-400',
    selectedColor: 'border-teal-500 bg-teal-50',
  },
  {
    type: 'jira-dc' as const,
    title: 'Systems and Avionics Backup',
    description: 'Full backup ZIP or entities.xml. Preserves workflows, custom fields, attachments, and full history.',
    icon: '🔄',
    iconBg: 'bg-purple-100',
    iconColor: 'text-purple-600',
    borderColor: 'hover:border-purple-400',
    selectedColor: 'border-purple-500 bg-purple-50',
  },
  {
    type: 'project-import' as const,
    title: 'Project Copy',
    description: 'Copy issues and configuration from one project to another within the platform.',
    icon: '📁',
    iconBg: 'bg-blue-100',
    iconColor: 'text-blue-600',
    borderColor: 'hover:border-blue-400',
    selectedColor: 'border-blue-500 bg-blue-50',
  },
  {
    type: 'project-export' as const,
    title: 'Project Export',
    description: 'Export project data to XML, JSON, or CSV format for backup or migration to another system.',
    icon: '📤',
    iconBg: 'bg-orange-100',
    iconColor: 'text-orange-600',
    borderColor: 'hover:border-orange-400',
    selectedColor: 'border-orange-500 bg-orange-50',
  },
];

export default function ImportTypeSelector({
  selectedType,
  onTypeSelect,
  disabled = false,
}: ImportTypeSelectorProps) {
  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold text-gray-900">Select Import Type</h3>
        <span className="text-sm text-gray-500">Choose one option</span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {IMPORT_TYPES.map((importType) => {
          const isSelected = selectedType === importType.type;

          return (
            <div
              key={importType.type}
              data-testid={`import-type-${importType.type}`}
              className={`
                relative p-6 border-2 rounded-lg cursor-pointer transition-all
                ${isSelected ? importType.selectedColor : `border-gray-200 ${importType.borderColor}`}
                ${disabled ? 'opacity-50 cursor-not-allowed' : ''}
              `}
              onClick={() => !disabled && onTypeSelect(importType.type)}
              role="button"
              tabIndex={disabled ? -1 : 0}
              onKeyDown={(e) => {
                if (!disabled && (e.key === 'Enter' || e.key === ' ')) {
                  onTypeSelect(importType.type);
                }
              }}
            >
              {isSelected && (
                <div className="absolute top-3 right-3">
                  <span className="text-jira-blue text-xl">✓</span>
                </div>
              )}

              <div className="flex items-start gap-4">
                <div
                  className={`w-12 h-12 ${importType.iconBg} rounded-lg flex items-center justify-center flex-shrink-0`}
                >
                  <span className={`text-2xl ${importType.iconColor}`}>{importType.icon}</span>
                </div>

                <div className="flex-1">
                  <h4 className="text-base font-semibold text-gray-900">{importType.title}</h4>
                  <p className="text-sm text-gray-500 mt-1 leading-relaxed">
                    {importType.description}
                  </p>
                </div>
              </div>

              <div className="mt-4 pt-4 border-t border-gray-100">
                <ul className="space-y-1.5">
                  {importType.type === 'csv' && (
                    <>
                      <li className="flex items-center text-xs text-gray-600">
                        <span className="text-green-500 mr-2">✓</span>
                        Bulk import issues and projects
                      </li>
                      <li className="flex items-center text-xs text-gray-600">
                        <span className="text-green-500 mr-2">✓</span>
                        Download pre-built templates
                      </li>
                      <li className="flex items-center text-xs text-gray-600">
                        <span className="text-green-500 mr-2">✓</span>
                        Field mapping and validation
                      </li>
                    </>
                  )}
                  {importType.type === 'issue-xml' && (
                    <>
                      <li className="flex items-center text-xs text-gray-600">
                        <span className="text-teal-600 mr-2">✓</span>
                        RSS/channel issue export XML
                      </li>
                      <li className="flex items-center text-xs text-gray-600">
                        <span className="text-teal-600 mr-2">✓</span>
                        Comments, worklogs, links, custom fields
                      </li>
                      <li className="flex items-center text-xs text-gray-600">
                        <span className="text-teal-600 mr-2">✓</span>
                        Conflict resolution and dry-run
                      </li>
                    </>
                  )}
                  {importType.type === 'jira-dc' && (
                    <>
                      <li className="flex items-center text-xs text-gray-600">
                        <span className="text-green-500 mr-2">✓</span>
                        Full backup ZIP / entities.xml
                      </li>
                      <li className="flex items-center text-xs text-gray-600">
                        <span className="text-green-500 mr-2">✓</span>
                        Workflows and custom fields
                      </li>
                      <li className="flex items-center text-xs text-gray-600">
                        <span className="text-green-500 mr-2">✓</span>
                        Keep full history
                      </li>
                    </>
                  )}
                  {importType.type === 'project-import' && (
                    <>
                      <li className="flex items-center text-xs text-gray-600">
                        <span className="text-green-500 mr-2">✓</span>
                        Copy all project data
                      </li>
                      <li className="flex items-center text-xs text-gray-600">
                        <span className="text-green-500 mr-2">✓</span>
                        Include attachments
                      </li>
                      <li className="flex items-center text-xs text-gray-600">
                        <span className="text-green-500 mr-2">✓</span>
                        Preserve links and relations
                      </li>
                    </>
                  )}
                  {importType.type === 'project-export' && (
                    <>
                      <li className="flex items-center text-xs text-gray-600">
                        <span className="text-green-500 mr-2">✓</span>
                        Export to XML format
                      </li>
                      <li className="flex items-center text-xs text-gray-600">
                        <span className="text-green-500 mr-2">✓</span>
                        JSON and CSV options
                      </li>
                      <li className="flex items-center text-xs text-gray-600">
                        <span className="text-green-500 mr-2">✓</span>
                        Downloadable archive
                      </li>
                    </>
                  )}
                </ul>
              </div>
            </div>
          );
        })}
      </div>

      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 flex items-start gap-3">
        <span className="text-blue-500 text-lg mt-0.5">ℹ</span>
        <div>
          <p className="text-blue-800 text-sm font-medium">Need help choosing?</p>
          <p className="text-blue-600 text-xs mt-1">
            <strong>Issue XML (Jira DC):</strong> Single issue export XML from Jira DC (RSS/channel format).
            <strong> Systems and Avionics Backup:</strong> Full backup ZIP with workflows and history.
            <strong> Workflow XML:</strong> Workflow descriptors only.
            <strong> CSV Import:</strong> Spreadsheets and templates.
            <strong> Project Copy / Export:</strong> Within-platform copy or export.
          </p>
        </div>
      </div>
    </div>
  );
}
