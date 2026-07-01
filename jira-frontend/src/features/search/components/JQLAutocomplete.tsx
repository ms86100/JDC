import React, { useState, useRef, useCallback, useEffect } from 'react';

interface JQLAutocompleteProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  error?: string | null;
}

// JQL field definitions
const JQL_FIELDS = [
  { name: 'project', type: 'project', description: 'Project' },
  { name: ' issuetype', type: 'type', description: 'Issue Type' },
  { name: 'status', type: 'status', description: 'Status' },
  { name: 'priority', type: 'priority', description: 'Priority' },
  { name: 'assignee', type: 'user', description: 'Assignee' },
  { name: 'reporter', type: 'user', description: 'Reporter' },
  { name: 'created', type: 'date', description: 'Created Date' },
  { name: 'updated', type: 'date', description: 'Updated Date' },
  { name: 'duedate', type: 'date', description: 'Due Date' },
  { name: 'summary', type: 'text', description: 'Summary' },
  { name: 'description', type: 'text', description: 'Description' },
  { name: 'labels', type: 'labels', description: 'Labels' },
  { name: 'sprint', type: 'sprint', description: 'Sprint' },
  { name: 'epic', type: 'epic', description: 'Epic' },
  { name: 'parent', type: 'issue', description: 'Parent Issue' },
  { name: 'component', type: 'component', description: 'Component' },
  { name: 'fixversion', type: 'version', description: 'Fix Version' },
  { name: 'affectsversion', type: 'version', description: 'Affects Version' },
  { name: 'resolution', type: 'resolution', description: 'Resolution' },
  { name: 'votes', type: 'number', description: 'Votes' },
  { name: 'watcher', type: 'user', description: 'Watcher' },
];

const JQL_OPERATORS = [
  { value: '=', description: 'equals' },
  { value: '!=', description: 'not equals' },
  { value: '>', description: 'greater than' },
  { value: '<', description: 'less than' },
  { value: '>=', description: 'greater than or equal' },
  { value: '<=', description: 'less than or equal' },
  { value: '~', description: 'contains' },
  { value: '!~', description: 'does not contain' },
  { value: 'IN', description: 'in list' },
  { value: 'NOT IN', description: 'not in list' },
  { value: 'IS', description: 'is' },
  { value: 'IS NOT', description: 'is not' },
];

const JQL_KEYWORDS = [
  { type: 'keyword', value: 'AND', description: 'Logical AND' },
  { type: 'keyword', value: 'OR', description: 'Logical OR' },
  { type: 'keyword', value: 'ORDER BY', description: 'Sort results' },
  { type: 'keyword', value: 'GROUP BY', description: 'Group results' },
];

const JQL_FUNCTIONS = [
  { type: 'function', name: 'currentUser()', value: 'currentUser()', description: 'Current logged in user' },
  { type: 'function', name: 'now()', value: 'now()', description: 'Current date/time' },
  { type: 'function', name: 'startOfDay()', value: 'startOfDay()', description: 'Start of today' },
  { type: 'function', name: 'endOfDay()', value: 'endOfDay()', description: 'End of today' },
  { type: 'function', name: 'startOfWeek()', value: 'startOfWeek()', description: 'Start of week' },
  { type: 'function', name: 'endOfWeek()', value: 'endOfWeek()', description: 'End of week' },
  { type: 'function', name: 'startOfMonth()', value: 'startOfMonth()', description: 'Start of month' },
  { type: 'function', name: 'endOfMonth()', value: 'endOfMonth()', description: 'End of month' },
  { type: 'function', name: 'issuekeyin()', value: 'issuekeyin()', description: 'Issues with keys in list' },
];

export default function JQLAutocomplete({
  value,
  onChange,
  placeholder = 'Enter JQL query...',
  error,
}: JQLAutocompleteProps) {
  const [isFocused, setIsFocused] = useState(false);
  const [cursorPosition, setCursorPosition] = useState(0);
  const [suggestions, setSuggestions] = useState<Array<{ type: string; value: string; description: string }>>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState(-1);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  // Detect what the user is typing based on cursor position
  const getCurrentToken = useCallback((text: string, position: number) => {
    // Get text before cursor
    const beforeCursor = text.substring(0, position);
    const afterCursor = text.substring(position);

    // Find the last space or special character
    const lastSpace = beforeCursor.lastIndexOf(' ');
    const lastOpenParen = beforeCursor.lastIndexOf('(');
    const lastCloseParen = beforeCursor.lastIndexOf(')');
    const lastOperator = Math.max(
      beforeCursor.lastIndexOf('='),
      beforeCursor.lastIndexOf('!'),
      beforeCursor.lastIndexOf('<'),
      beforeCursor.lastIndexOf('>'),
      beforeCursor.lastIndexOf('~')
    );

    // Find where the current token starts
    const tokenStart = Math.max(
      lastSpace + 1,
      lastOpenParen + 1,
      lastOperator + 1
    );

    // Get the current token
    const currentToken = beforeCursor.substring(tokenStart);

    // Determine what we're completing
    const lastToken = beforeCursor.trim().split(/\s+/).pop() || '';

    // Check if we're after an operator
    const operatorMatch = beforeCursor.match(/(\S+)\s*(=|!=|>|<|>=|<=|~)?\s*$/);
    const isAfterOperator = operatorMatch && operatorMatch[2];

    return {
      currentToken,
      lastToken,
      isAfterOperator,
      beforeCursor,
      afterCursor,
    };
  }, []);

  // Calculate suggestions based on current input
  const updateSuggestions = useCallback(() => {
    if (!inputRef.current) return;

    const position = inputRef.current.selectionStart;
    const { currentToken, lastToken, isAfterOperator } = getCurrentToken(value, position);

    let newSuggestions: typeof suggestions = [];

    if (isAfterOperator) {
      // Show value suggestions based on field type
      // For now, show some example values
      newSuggestions = [
        { type: 'value', value: '"To Do"', description: 'To Do status' },
        { type: 'value', value: '"In Progress"', description: 'In Progress status' },
        { type: 'value', value: '"Done"', description: 'Done status' },
        { type: 'value', value: 'currentUser()', description: 'Current user' },
      ];
    } else if (currentToken.startsWith('issuekey')) {
      newSuggestions = JQL_FUNCTIONS.filter(f => f.name.includes(currentToken));
    } else if (currentToken.startsWith('start') || currentToken.startsWith('end')) {
      newSuggestions = JQL_FUNCTIONS.filter(f => f.name.includes(currentToken));
    } else if (currentToken.toLowerCase() === 'and' || currentToken.toLowerCase() === 'or') {
      newSuggestions = JQL_KEYWORDS.filter(k => k.value.toLowerCase().startsWith(currentToken.toLowerCase()));
    } else if (currentToken.toLowerCase().startsWith('order')) {
      newSuggestions = [{ type: 'keyword', value: 'ORDER BY', description: 'Sort results' }];
    } else if (currentToken.length > 0) {
      // Check if it's a field name
      const fieldMatches = JQL_FIELDS.filter(f =>
        f.name.toLowerCase().startsWith(currentToken.toLowerCase())
      );
      const operatorMatches = JQL_OPERATORS.filter(o =>
        o.value.toLowerCase().startsWith(currentToken.toLowerCase())
      );
      const keywordMatches = JQL_KEYWORDS.filter(k =>
        k.value.toLowerCase().startsWith(currentToken.toLowerCase())
      );
      const functionMatches = JQL_FUNCTIONS.filter(f =>
        f.name.toLowerCase().startsWith(currentToken.toLowerCase())
      );

      newSuggestions = [
        ...fieldMatches.map(f => ({ type: 'field', value: f.name, description: f.description })),
        ...operatorMatches.map(o => ({ type: 'operator', value: o.value, description: o.description })),
        ...keywordMatches.map(k => ({ type: 'keyword', value: k.value, description: k.description })),
        ...functionMatches.map(f => ({ type: 'function', value: f.name, description: f.description })),
      ];
    }

    setSuggestions(newSuggestions);
    setShowSuggestions(newSuggestions.length > 0 && currentToken.length > 0);
    setSelectedIndex(-1);
  }, [value, getCurrentToken]);

  // Handle input change
  const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const newValue = e.target.value;
    onChange(newValue);
    setCursorPosition(e.target.selectionStart);
  };

  // Handle cursor position change
  const handleSelect = (e: React.SyntheticEvent) => {
    setCursorPosition((e.target as HTMLTextAreaElement).selectionStart);
  };

  // Handle keyboard navigation
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (!showSuggestions) return;

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        setSelectedIndex(prev =>
          prev < suggestions.length - 1 ? prev + 1 : 0
        );
        break;
      case 'ArrowUp':
        e.preventDefault();
        setSelectedIndex(prev =>
          prev > 0 ? prev - 1 : suggestions.length - 1
        );
        break;
      case 'Enter':
      case 'Tab':
        if (selectedIndex >= 0 && suggestions[selectedIndex]) {
          e.preventDefault();
          applySuggestion(suggestions[selectedIndex]);
        }
        break;
      case 'Escape':
        setShowSuggestions(false);
        setSelectedIndex(-1);
        break;
    }
  };

  // Apply a suggestion
  const applySuggestion = (suggestion: typeof suggestions[0]) => {
    if (!inputRef.current) return;

    const position = inputRef.current.selectionStart;
    const beforeCursor = value.substring(0, position);
    const afterCursor = value.substring(position);

    // Find where to insert (after current token)
    const lastSpace = beforeCursor.lastIndexOf(' ');
    const insertPosition = lastSpace + 1;

    let newValue = value.substring(0, insertPosition) + suggestion.value + ' ' + afterCursor;

    // Special handling for functions
    if (suggestion.value.includes('()')) {
      newValue = value.substring(0, insertPosition) + suggestion.value + ' ' + afterCursor;
    }

    onChange(newValue);
    setShowSuggestions(false);
    setSelectedIndex(-1);

    // Focus back on input
    setTimeout(() => {
      inputRef.current?.focus();
      const newPosition = insertPosition + suggestion.value.length + 1;
      inputRef.current?.setSelectionRange(newPosition, newPosition);
    }, 0);
  };

  // Update suggestions on change
  useEffect(() => {
    updateSuggestions();
  }, [value, cursorPosition, updateSuggestions]);

  // Get suggestion type icon
  const getSuggestionIcon = (type: string) => {
    switch (type) {
      case 'field': return '🏷️';
      case 'operator': return '🔣';
      case 'keyword': return '🔑';
      case 'function': return 'ƒ';
      case 'value': return '"';
      default: return '•';
    }
  };

  return (
    <div className="ab-jql-autocomplete">
      <div className={`ab-jql-input-container ${isFocused ? 'focused' : ''} ${error ? 'error' : ''}`}>
        <textarea
          ref={inputRef}
          value={value}
          onChange={handleChange}
          onSelect={handleSelect}
          onFocus={() => setIsFocused(true)}
          onBlur={() => setTimeout(() => setIsFocused(false), 200)}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          className="ab-jql-input"
          rows={1}
          spellCheck={false}
        />
      </div>

      {/* Suggestions dropdown */}
      {showSuggestions && suggestions.length > 0 && (
        <div className="ab-jql-suggestions">
          {suggestions.map((suggestion, index) => (
            <div
              key={`${suggestion.type}-${suggestion.value}`}
              className={`ab-jql-suggestion ${index === selectedIndex ? 'selected' : ''}`}
              onMouseDown={() => applySuggestion(suggestion)}
              onMouseEnter={() => setSelectedIndex(index)}
            >
              <span className="ab-suggestion-icon">{getSuggestionIcon(suggestion.type)}</span>
              <span className="ab-suggestion-value">{suggestion.value}</span>
              <span className="ab-suggestion-description">{suggestion.description}</span>
            </div>
          ))}
        </div>
      )}

      {/* Error message */}
      {error && (
        <div className="ab-jql-error">
          <span className="ab-error-icon">⚠️</span>
          {error}
        </div>
      )}

      {/* Help text */}
      <div className="ab-jql-help">
        <span className="ab-help-text">
          Press <kbd>Ctrl</kbd>+<kbd>Space</kbd> for suggestions
        </span>
      </div>

      <style>{`
        .ab-jql-autocomplete {
          position: relative;
          flex: 1;
        }

        .ab-jql-input-container {
          border: 1px solid var(--ab-gray-300);
          border-radius: var(--ab-radius-md);
          background: var(--ab-white);
          transition: all var(--ab-transition-fast);
        }

        .ab-jql-input-container.focused {
          border-color: var(--ab-primary-500);
          box-shadow: 0 0 0 3px rgba(0, 102, 255, 0.15);
        }

        .ab-jql-input-container.error {
          border-color: var(--ab-danger-500);
        }

        .ab-jql-input {
          width: 100%;
          padding: var(--ab-spacing-sm) var(--ab-spacing-md);
          font-family: var(--ab-font-mono);
          font-size: var(--ab-font-size-sm);
          border: none;
          background: transparent;
          resize: none;
          min-height: 40px;
        }

        .ab-jql-input:focus {
          outline: none;
        }

        .ab-jql-suggestions {
          position: absolute;
          top: 100%;
          left: 0;
          right: 0;
          background: var(--ab-white);
          border: 1px solid var(--ab-gray-200);
          border-radius: var(--ab-radius-md);
          box-shadow: var(--ab-shadow-lg);
          max-height: 300px;
          overflow-y: auto;
          z-index: 100;
        }

        .ab-jql-suggestion {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-sm);
          padding: var(--ab-spacing-sm) var(--ab-spacing-md);
          cursor: pointer;
          transition: background var(--ab-transition-fast);
        }

        .ab-jql-suggestion:hover,
        .ab-jql-suggestion.selected {
          background: var(--ab-gray-50);
        }

        .ab-suggestion-icon {
          font-size: var(--ab-font-size-sm);
          width: 20px;
          text-align: center;
        }

        .ab-suggestion-value {
          font-family: var(--ab-font-mono);
          font-size: var(--ab-font-size-sm);
          font-weight: 500;
          color: var(--ab-gray-800);
        }

        .ab-suggestion-description {
          font-size: var(--ab-font-size-xs);
          color: var(--ab-gray-500);
          margin-left: auto;
        }

        .ab-jql-error {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-xs);
          margin-top: var(--ab-spacing-xs);
          padding: var(--ab-spacing-xs) var(--ab-spacing-sm);
          background: var(--ab-danger-100);
          border-radius: var(--ab-radius-sm);
          font-size: var(--ab-font-size-sm);
          color: var(--ab-danger-700);
        }

        .ab-jql-help {
          margin-top: var(--ab-spacing-xs);
        }

        .ab-help-text {
          font-size: var(--ab-font-size-xs);
          color: var(--ab-gray-400);
        }

        .ab-help-text kbd {
          display: inline-block;
          padding: 1px 4px;
          background: var(--ab-gray-100);
          border-radius: var(--ab-radius-sm);
          font-size: 10px;
          font-family: var(--ab-font-mono);
        }
      `}</style>
    </div>
  );
}