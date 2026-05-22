import type { ImportType } from '../components/ImportTypeSelector';

/** True when CSV import has a persisted server upload with parsed rows. */
export function csvServerUploadReady(
  importType: ImportType | null,
  serverUploadOk: boolean,
  serverRowCount: number | undefined | null,
): boolean {
  if (importType !== 'csv') return true;
  return serverUploadOk && (serverRowCount ?? 0) > 0;
}

export function formatRowCountSummary(
  clientRows: number | undefined | null,
  serverRows: number | undefined | null,
): string {
  const client = clientRows ?? 0;
  const server = serverRows ?? 0;
  if (client === server) {
    return `${server} row${server === 1 ? '' : 's'} detected`;
  }
  return `Browser: ${client} row${client === 1 ? '' : 's'} · Server: ${server} row${server === 1 ? '' : 's'}`;
}
