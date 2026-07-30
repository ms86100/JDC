export type MigrationRole = 'MIGRATION_VIEWER' | 'MIGRATION_OPERATOR' | 'MIGRATION_ADMIN';

export function getMigrationRole(): MigrationRole {
  const r = localStorage.getItem('migrationRole') || 'MIGRATION_OPERATOR';
  if (r === 'MIGRATION_VIEWER' || r === 'MIGRATION_ADMIN') return r;
  return 'MIGRATION_OPERATOR';
}

export function canReadMigration(): boolean {
  return true;
}

export function canWriteMigration(): boolean {
  const role = getMigrationRole();
  return role === 'MIGRATION_OPERATOR' || role === 'MIGRATION_ADMIN';
}

export function canAdminMigration(): boolean {
  return getMigrationRole() === 'MIGRATION_ADMIN';
}
