import React from 'react';

interface AdminLayoutProps {
  children: React.ReactNode;
}

/**
 * Simple pass-through layout wrapper.
 * The actual layout is handled by JiraAdminLayout in AdminRoutes.tsx
 * This component exists for backward compatibility.
 */
export default function AdminLayout({ children }: AdminLayoutProps) {
  return <>{children}</>;
}
