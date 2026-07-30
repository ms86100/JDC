import React from 'react';
import { useMutation } from '@tanstack/react-query';
import { migrationApi } from '../../../api/serviceApi';

interface Props {
  jobId: string | null;
  jobStatus?: string;
  onUpdated?: () => void;
}

export default function JobPauseResumeControls({ jobId, jobStatus, onUpdated }: Props) {
  const pause = useMutation({
    mutationFn: async () => {
      await migrationApi.pauseJob(jobId!);
    },
    onSuccess: () => onUpdated?.(),
  });

  const resume = useMutation({
    mutationFn: async () => {
      await migrationApi.resumePausedJob(jobId!);
    },
    onSuccess: () => onUpdated?.(),
  });

  if (!jobId) return null;

  const inProgress = jobStatus === 'IN_PROGRESS' || jobStatus === 'IMPORTING';
  const paused = jobStatus === 'PAUSED';

  if (!inProgress && !paused) return null;

  return (
    <div className="flex gap-2">
      {inProgress && (
        <button
          type="button"
          onClick={() => pause.mutate()}
          disabled={pause.isPending}
          className="px-3 py-1.5 text-sm bg-amber-100 text-amber-800 rounded hover:bg-amber-200"
        >
          Pause
        </button>
      )}
      {paused && (
        <button
          type="button"
          onClick={() => resume.mutate()}
          disabled={resume.isPending}
          className="px-3 py-1.5 text-sm bg-green-100 text-green-800 rounded hover:bg-green-200"
        >
          Resume
        </button>
      )}
    </div>
  );
}
