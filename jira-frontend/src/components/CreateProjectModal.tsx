import React, { useState } from 'react';
import './CreateProjectModal.css';

interface Template {
  id: string;
  category: 'SOFTWARE' | 'BUSINESS';
  name: string;
  description: string;
  icon: string;
  color: string;
}

const SOFTWARE_TEMPLATES: Template[] = [
  {
    id: 'scrum',
    category: 'SOFTWARE',
    name: 'Scrum software development',
    description: 'For teams that use Scrum agile software development to plan and track work in sprints.',
    icon: '🔄',
    color: '#0052cc',
  },
  {
    id: 'kanban',
    category: 'SOFTWARE',
    name: 'Kanban software development',
    description: 'For teams that use Kanban to visualize work, limit work-in-progress, and maximize efficiency.',
    icon: '📋',
    color: '#00875a',
  },
  {
    id: 'basic',
    category: 'SOFTWARE',
    name: 'Basic software development',
    description: 'A simplified project for tracking and managing software development tasks and bugs.',
    icon: '💻',
    color: '#6554c0',
  },
];

const BUSINESS_TEMPLATES: Template[] = [
  {
    id: 'project_management',
    category: 'BUSINESS',
    name: 'Project management',
    description: 'Manage projects from conception to completion using issues, plans, and roadmaps.',
    icon: '📁',
    color: '#ff8b00',
  },
  {
    id: 'task_management',
    category: 'BUSINESS',
    name: 'Task management',
    description: 'Track and manage everyday tasks that your team works on throughout the day.',
    icon: '✓',
    color: '#00b8d9',
  },
  {
    id: 'process_management',
    category: 'BUSINESS',
    name: 'Process management',
    description: 'Manage business processes and workflows by creating, tracking, and resolving issues.',
    icon: '🔧',
    color: '#ff5630',
  },
];

interface CreateProjectModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSelect: (templateId: string) => void;
}

export default function CreateProjectModal({ isOpen, onClose, onSelect }: CreateProjectModalProps) {
  const [selectedTemplate, setSelectedTemplate] = useState<string>('scrum');

  if (!isOpen) return null;

  return (
    <div className="ab-modal-overlay" onClick={onClose}>
      <div className="ab-jira-modal" onClick={(e) => e.stopPropagation()}>
        <div className="ab-modal-header">
          <h2 className="ab-modal-title">Create project</h2>
          <a href="#" className="ab-modal-sublink" onClick={(e) => e.preventDefault()}>
            View Marketplace Workflows →
          </a>
        </div>

        <div className="ab-modal-body">
          {/* Software Templates */}
          <div className="ab-template-category">
            <div className="ab-template-cat-label">
              <span>📂</span> Software
            </div>
            <div className="ab-template-grid">
              {SOFTWARE_TEMPLATES.map((tpl) => (
                <button
                  key={tpl.id}
                  className={`ab-template-option ${selectedTemplate === tpl.id ? 'selected' : ''}`}
                  onClick={() => setSelectedTemplate(tpl.id)}
                >
                  <div className="ab-template-option-header">
                    <div className="ab-template-opt-icon" style={{ background: tpl.color }}>
                      {tpl.icon}
                    </div>
                    <span className="ab-template-opt-name">{tpl.name}</span>
                  </div>
                  <p className="ab-template-opt-desc">{tpl.description}</p>
                </button>
              ))}
            </div>
          </div>

          {/* Business Templates */}
          <div className="ab-template-category">
            <div className="ab-template-cat-label">
              <span>📋</span> Business
            </div>
            <div className="ab-template-grid">
              {BUSINESS_TEMPLATES.map((tpl) => (
                <button
                  key={tpl.id}
                  className={`ab-template-option ${selectedTemplate === tpl.id ? 'selected' : ''}`}
                  onClick={() => setSelectedTemplate(tpl.id)}
                >
                  <div className="ab-template-option-header">
                    <div className="ab-template-opt-icon" style={{ background: tpl.color }}>
                      {tpl.icon}
                    </div>
                    <span className="ab-template-opt-name">{tpl.name}</span>
                  </div>
                  <p className="ab-template-opt-desc">{tpl.description}</p>
                </button>
              ))}
            </div>
          </div>
        </div>

        <div className="ab-modal-footer">
          <div className="ab-modal-footer-left">
            <button className="ab-footer-link">Import a project</button>
            <button className="ab-footer-link">Create with shared configuration</button>
            <button className="ab-footer-link">Create sample data</button>
          </div>
          <div className="ab-modal-footer-right">
            <button className="ab-btn-cancel" onClick={onClose}>Cancel</button>
            <button className="ab-btn-next" onClick={() => { onSelect(selectedTemplate); onClose(); }}>
              Next
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}