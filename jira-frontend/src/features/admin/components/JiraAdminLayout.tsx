/**
 * Compatibility shim. The real implementation now lives in
 * src/components/layout/AppShell.tsx.
 * Default export name and {children} prop preserved so AdminRoutes.tsx
 * and the four admin pages that import this file directly do not change.
 */
import React from 'react';
import AppShell from '../../../components/layout/AppShell';

interface JiraAdminLayoutProps { children: React.ReactNode; }

export default function JiraAdminLayout({ children }: JiraAdminLayoutProps) {
  return <AppShell mode="admin">{children}</AppShell>;
}
