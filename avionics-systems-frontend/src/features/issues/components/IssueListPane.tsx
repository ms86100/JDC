import { useState } from 'react';
import { Link } from 'react-router-dom';
import { IssueResponse } from '../../../api/issueApi';

interface IssueListPaneProps {
  issues: IssueResponse[];
  selectedId?: string;
  orderBy: string;
  page: number;
  total: number;
  onOrderChange: (o: string) => void;
  onCreateIssue: () => void;
  textFilter?: string;
  onTextFilterChange?: (v: string) => void;
  statusFilter?: string;
  onStatusFilterChange?: (v: string) => void;
  onPrevPage?: () => void;
  onNextPage?: () => void;
  pageLabel?: string;
  issueLink?: (issueId: string) => string;
  title?: string;
}

function issueTypeClass(type: string) {
  const t = (type ?? '').toLowerCase();
  if (t.includes('bug')) return 'bug';
  if (t.includes('story')) return 'story';
  if (t.includes('epic')) return 'epic';
  if (t.includes('task')) return 'task';
  return 'default';
}

function statusClass(status?: string) {
  const s = (status ?? '').toLowerCase();
  if (s.includes('progress')) return 'in-progress';
  if (s === 'done' || s.includes('done')) return 'done';
  return 'todo';
}

export default function IssueListPane({
  issues,
  selectedId,
  orderBy,
  page,
  total,
  onOrderChange,
  onCreateIssue,
  textFilter = '',
  onTextFilterChange,
  statusFilter = 'all',
  onStatusFilterChange,
  onPrevPage,
  onNextPage,
  pageLabel,
  issueLink = (id) => `/issues/${id}`,
  title = 'Open issues',
}: IssueListPaneProps) {
  const [checkedIds, setCheckedIds] = useState<Set<string>>(new Set());

  const toggleChecked = (id: string) => {
    setCheckedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  return (
    <div className="jdc-issue-list-pane">
      <div className="jdc-issue-list-header">
        <div className="jdc-issue-list-header-top">
          <h1>{title}</h1>
          <Link to="/issues/batch" className="jdc-issue-filter-link">
            All filters
          </Link>
        </div>

        {onTextFilterChange && (
          <div className="jdc-search-field jdc-field-spaced">
            <span className="jdc-search-field-icon" aria-hidden="true">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="11" cy="11" r="7" />
                <path d="M20 20L16 16" strokeLinecap="round" />
              </svg>
            </span>
            <input
              type="search"
              className="jdc-input jdc-input--search"
              placeholder="Filter issues"
              value={textFilter}
              onChange={(e) => onTextFilterChange(e.target.value)}
              aria-label="Filter issues"
            />
          </div>
        )}

        {onStatusFilterChange && (
          <div className="jdc-field-spaced">
            <label className="jdc-field-label" htmlFor="jdc-issue-status-filter">
              Status
            </label>
            <select
              id="jdc-issue-status-filter"
              className="jdc-select"
              value={statusFilter}
              onChange={(e) => onStatusFilterChange(e.target.value)}
            >
              <option value="all">All statuses</option>
              <option value="to do">To Do</option>
              <option value="in progress">In Progress</option>
              <option value="done">Done</option>
            </select>
          </div>
        )}

        <div className="jdc-issue-list-toolbar">
          <div className="jdc-field-grow">
            <label className="jdc-field-label" htmlFor="jdc-issue-order">
              Sort
            </label>
            <select
              id="jdc-issue-order"
              className="jdc-select jdc-select--compact"
              value={orderBy}
              onChange={(e) => onOrderChange(e.target.value)}
            >
              <option value="priority">Priority</option>
              <option value="updated">Recently updated</option>
              <option value="key">Issue key</option>
            </select>
          </div>
          <div className="jdc-issue-list-pagination" aria-label="Issue list pagination">
            {onPrevPage && (
              <button
                type="button"
                className="jdc-icon-btn"
                onClick={onPrevPage}
                aria-label="Previous page"
              >
                ‹
              </button>
            )}
            <span className="jdc-issue-list-page-label">{pageLabel ?? `${page} of ${total}`}</span>
            {onNextPage && (
              <button
                type="button"
                className="jdc-icon-btn"
                onClick={onNextPage}
                aria-label="Next page"
              >
                ›
              </button>
            )}
          </div>
        </div>
      </div>

      <div className="jdc-issue-list-scroll">
        {issues.length === 0 ? (
          <div className="jdc-issue-list-empty">No issues match your filters</div>
        ) : (
          issues.map((issue) => (
            <Link
              key={issue.id}
              to={issueLink(issue.id)}
              className={`jdc-issue-list-item ${selectedId === issue.id ? 'selected' : ''}`}
            >
              <span
                className="jdc-issue-list-check"
                role="presentation"
                onClick={(e) => {
                  e.preventDefault();
                  e.stopPropagation();
                  toggleChecked(issue.id);
                }}
              >
                <input
                  type="checkbox"
                  className="jdc-checkbox"
                  checked={checkedIds.has(issue.id)}
                  onChange={() => toggleChecked(issue.id)}
                  onClick={(e) => e.stopPropagation()}
                  aria-label={`Select ${issue.issueKey}`}
                />
              </span>
              <span
                className={`jdc-issue-type-dot jdc-issue-type-dot--${issueTypeClass(issue.issueType)}`}
                title={issue.issueType}
                aria-hidden="true"
              />
              <div className="jdc-issue-list-content">
                <div className="jdc-issue-list-meta">
                  <strong>{issue.issueKey}</strong>
                  {issue.status && (
                    <span className={`jdc-status-lozenge jdc-status-lozenge--${statusClass(issue.status)}`}>
                      {issue.status}
                    </span>
                  )}
                </div>
                <div className="jdc-issue-list-title">{issue.title}</div>
              </div>
            </Link>
          ))
        )}
      </div>

      <div className="jdc-issue-list-footer">
        <button type="button" className="jdc-btn jdc-btn-primary jdc-btn-block" onClick={onCreateIssue}>
          + Create issue
        </button>
      </div>
    </div>
  );
}
