import React, { useState } from 'react';
import { FieldDefinition } from '../../context/FieldDefinitionContext';

interface DynamicUserPickerProps {
  fieldDef: FieldDefinition;
  value?: any;
  onChange?: (value: any) => void;
  readOnly?: boolean;
  type?: 'user' | 'group';
  issueId?: string;
}

interface UserInfo {
  id: string;
  name: string;
  avatar?: string;
  email?: string;
}

export default function DynamicUserPicker({
  fieldDef,
  value,
  onChange,
  readOnly = false,
  type = 'user'
}: DynamicUserPickerProps) {
  const [searchTerm, setSearchTerm] = useState('');
  const [isOpen, setIsOpen] = useState(false);
  const [users, setUsers] = useState<UserInfo[]>([]);

  const currentUser = value as UserInfo | null;

  const searchUsers = async (query: string) => {
    if (query.length < 2) {
      setUsers([]);
      return;
    }
    // Simulated search - in real app, call user API
    setUsers([
      { id: '1', name: 'John Doe', email: 'john@example.com' },
      { id: '2', name: 'Jane Smith', email: 'jane@example.com' }
    ].filter(u => u.name.toLowerCase().includes(query.toLowerCase())));
  };

  const selectUser = (user: UserInfo) => {
    onChange?.(user);
    setIsOpen(false);
    setSearchTerm('');
  };

  const clearUser = () => {
    onChange?.(null);
  };

  if (readOnly) {
    if (!currentUser) {
      return <span className="no-value">Unassigned</span>;
    }
    return (
      <div className="dynamic-user-chip">
        <span className="dynamic-user-avatar">
          {currentUser.name?.charAt(0) || 'U'}
        </span>
        <span className="dynamic-user-name">{currentUser.name}</span>
      </div>
    );
  }

  return (
    <div className="dynamic-user-picker">
      {currentUser && (
        <div className="dynamic-user-selected">
          <div className="dynamic-user-chip">
            <span className="dynamic-user-avatar">
              {currentUser.name?.charAt(0) || 'U'}
            </span>
            <span className="dynamic-user-name">{currentUser.name}</span>
          </div>
          <button type="button" className="dynamic-user-clear" onClick={clearUser}>
            ×
          </button>
        </div>
      )}

      <input
        type="text"
        className="dynamic-user-search"
        placeholder={`Search for ${type}...`}
        value={searchTerm}
        onChange={(e) => {
          setSearchTerm(e.target.value);
          searchUsers(e.target.value);
          if (!isOpen) setIsOpen(true);
        }}
        onFocus={() => setIsOpen(true)}
      />

      {isOpen && users.length > 0 && (
        <div className="dynamic-user-dropdown">
          {users.map((user) => (
            <div
              key={user.id}
              className="dynamic-user-option"
              onClick={() => selectUser(user)}
            >
              <span className="dynamic-user-avatar">
                {user.name?.charAt(0) || 'U'}
              </span>
              <div className="dynamic-user-info">
                <span className="dynamic-user-name">{user.name}</span>
                {user.email && (
                  <span className="dynamic-user-email">{user.email}</span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}