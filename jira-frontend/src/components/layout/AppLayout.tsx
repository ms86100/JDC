/**
 * Compatibility shim. The real implementation now lives in AppShell.tsx.
 * Default export name preserved so App.tsx import line is unchanged.
 */
import AppShell from './AppShell';

export default function AppLayout() {
  return <AppShell mode="workspace" />;
}
