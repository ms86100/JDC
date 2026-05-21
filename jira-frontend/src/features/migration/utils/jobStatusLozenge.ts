import type { StatusLozengeProps } from '../../../components/ui/StatusLozenge';

type LozengeStatus = StatusLozengeProps['status'];

const JOB_STATUS_MAP: Record<string, { status: LozengeStatus; label: string }> = {
  PENDING: { status: 'todo', label: 'Pending' },
  VALIDATING: { status: 'inprogress', label: 'Validating' },
  MAPPING: { status: 'inprogress', label: 'Mapping' },
  IMPORTING: { status: 'inprogress', label: 'Importing' },
  IN_PROGRESS: { status: 'inprogress', label: 'In progress' },
  INDEXING: { status: 'inprogress', label: 'Indexing' },
  PAUSED: { status: 'blocked', label: 'Paused' },
  COMPLETED: { status: 'done', label: 'Completed' },
  FAILED: { status: 'blocked', label: 'Failed' },
  CANCELLED: { status: 'blocked', label: 'Cancelled' },
  ROLLED_BACK: { status: 'blocked', label: 'Rolled back' },
};

export function jobStatusLozenge(jobStatus: string): { status: LozengeStatus; label: string } {
  return JOB_STATUS_MAP[jobStatus] ?? { status: 'todo', label: jobStatus };
}
