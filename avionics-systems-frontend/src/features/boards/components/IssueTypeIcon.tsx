import React from 'react';

/** Avionics Systems-style issue type icons (SVG, not emoji). */
export default function IssueTypeIcon({
  type,
  size = 16,
  className = '',
}: {
  type?: string;
  size?: number;
  className?: string;
}) {
  const t = (type ?? 'task').toLowerCase().replace(/\s+/g, '');
  const props = {
    width: size,
    height: size,
    viewBox: '0 0 16 16',
    className,
    'aria-hidden': true as const,
  };

  switch (t) {
    case 'bug':
      return (
        <svg {...props}>
          <rect x="1" y="1" width="14" height="14" rx="2" fill="#E5493A" />
          <path
            fill="#fff"
            d="M8 3.5a2.5 2.5 0 0 0-2.45 2h4.9A2.5 2.5 0 0 0 8 3.5zm-3.5 4a1 1 0 0 0 0 2h7a1 1 0 0 0 0-2h-7zm1.5 3.5a2.5 2.5 0 0 0 4 0H6z"
          />
        </svg>
      );
    case 'story':
      return (
        <svg {...props}>
          <rect x="1" y="1" width="14" height="14" rx="2" fill="#63BA3C" />
          <path
            fill="#fff"
            d="M5 4.5h6v1.5H8.5V11H7V6H5V4.5z"
          />
        </svg>
      );
    case 'epic':
      return (
        <svg {...props}>
          <rect x="1" y="1" width="14" height="14" rx="2" fill="#6554C0" />
          <path fill="#fff" d="M5 5h6v1.2H5V5zm0 2.4h6v1.2H5V7.4zm0 2.4h4v1.2H5V9.8z" />
        </svg>
      );
    case 'sub-task':
    case 'subtask':
      return (
        <svg {...props}>
          <rect x="1" y="1" width="14" height="14" rx="2" fill="#4BAEE8" />
          <path fill="#fff" d="M6 5h5v1H6V5zm0 2.5h5v1H6v-1zm0 2.5h3v1H6v-1z" />
        </svg>
      );
    default:
      return (
        <svg {...props}>
          <rect x="1" y="1" width="14" height="14" rx="2" fill="#4BAEE8" />
          <path
            fill="#fff"
            d="M6.2 5.5h3.6l-.9 4H7.1l-.9-4zm-.7-1.5h5l.3 1.5H5.2V4z"
          />
        </svg>
      );
  }
}

/** Deterministic Avionics Systems-like label colors from label text. */
const LABEL_PALETTE = [
  { bg: '#DEEBFF', text: '#0747A6' },
  { bg: '#E3FCEF', text: '#006644' },
  { bg: '#FFEBE6', text: '#BF2600' },
  { bg: '#EAE6FF', text: '#403294' },
  { bg: '#FFF0B3', text: '#974F0C' },
  { bg: '#DFE1E6', text: '#42526E' },
];

export function labelStyle(label: string): React.CSSProperties {
  let hash = 0;
  for (let i = 0; i < label.length; i++) hash = (hash + label.charCodeAt(i) * 17) % LABEL_PALETTE.length;
  const c = LABEL_PALETTE[hash]!;
  return { backgroundColor: c.bg, color: c.text };
}
