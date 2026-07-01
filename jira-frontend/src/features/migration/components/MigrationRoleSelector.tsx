import React, { useEffect, useState } from 'react';

const ROLES = ['MIGRATION_VIEWER', 'MIGRATION_OPERATOR', 'MIGRATION_ADMIN'] as const;

export default function MigrationRoleSelector() {
  const [role, setRole] = useState(() => localStorage.getItem('migrationRole') || 'MIGRATION_ADMIN');

  useEffect(() => {
    localStorage.setItem('migrationRole', role);
  }, [role]);

  return (
    <div className="flex items-center gap-2 text-sm">
      <label className="text-gray-600">Migration role</label>
      <select
        value={role}
        onChange={(e) => setRole(e.target.value)}
        className="border rounded px-2 py-1"
      >
        {ROLES.map((r) => (
          <option key={r} value={r}>
            {r}
          </option>
        ))}
      </select>
    </div>
  );
}
