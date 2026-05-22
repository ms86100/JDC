/**
 * Compatibility shim. The real implementation now lives in
 * src/components/layout/AppShell.tsx.
 * Full prop signature preserved so KanbanBoardPage.tsx is not modified.
 * Project-context props (projectName, projectKey, projectAvatar, boardName,
 * activeSection) are surfaced via the page itself rather than the shell;
 * they are accepted here for backward compatibility and ignored by AppShell.
 */
import React from 'react';

interface JiraGlobalLayoutProps {
  children: React.ReactNode;
  projectName?: string;
  projectKey?: string;
  projectAvatar?: string;
  boardName?: string;
  activeSection?: string;
}

/** Legacy wrapper — routes already render inside AppShell; do not nest another shell. */
export default function JiraGlobalLayout({ children }: JiraGlobalLayoutProps) {
  return <>{children}</>;
}
