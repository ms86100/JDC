import React from 'react';

interface Props {
  status?: string | null;
}

export default function VirusScanStatusBadge({ status }: Props) {
  if (!status) return null;
  const color =
    status === 'CLEAN'
      ? 'bg-green-100 text-green-800'
      : status === 'INFECTED'
      ? 'bg-red-100 text-red-800'
      : status === 'PENDING'
      ? 'bg-gray-100 text-gray-700'
      : 'bg-blue-100 text-blue-800';
  return (
    <span className={`inline-flex px-2 py-0.5 rounded text-xs font-medium ${color}`}>
      Scan: {status}
    </span>
  );
}
