import { useRef, useState, useCallback } from 'react';
import type { PlanItemResponse } from '../../../../api/planApi';

interface RoadmapTimelineProps {
  rows: { key: string; item?: PlanItemResponse }[];
  zoom: '3M' | '1Y' | 'Fit';
  getStartDate: (itemId: string) => string | undefined;
  getEndDate: (itemId: string) => string | undefined;
  onBarDatesChange?: (itemId: string, start: string, end: string) => void;
}

function weeksForZoom(zoom: '3M' | '1Y' | 'Fit'): number {
  if (zoom === '1Y') return 52;
  if (zoom === 'Fit') return 16;
  return 13;
}

function parseDate(s?: string): Date | null {
  if (!s) return null;
  const d = new Date(s);
  return Number.isNaN(d.getTime()) ? null : d;
}

function toIso(d: Date) {
  return d.toISOString().slice(0, 10);
}

export default function RoadmapTimeline({
  rows,
  zoom,
  getStartDate,
  getEndDate,
  onBarDatesChange,
}: RoadmapTimelineProps) {
  const weekCount = weeksForZoom(zoom);
  const start = new Date();
  start.setDate(start.getDate() - 7);
  const weeks: Date[] = [];
  for (let i = 0; i < weekCount; i++) {
    const d = new Date(start);
    d.setDate(d.getDate() + i * 7);
    weeks.push(d);
  }

  const rangeStart = weeks[0].getTime();
  const rangeEnd = weeks[weeks.length - 1].getTime() + 7 * 86400000;
  const rangeMs = rangeEnd - rangeStart || 1;

  const today = new Date();
  const todayPct = Math.min(100, Math.max(0, ((today.getTime() - rangeStart) / rangeMs) * 100));

  const fmt = (d: Date) =>
    `${String(d.getDate()).padStart(2, '0')}/${d.toLocaleString('en', { month: 'short' })}`;

  const bodyRef = useRef<HTMLDivElement>(null);
  const dragRef = useRef<{
    itemId: string;
    mode: 'move' | 'resize-end';
    originX: number;
    startMs: number;
    endMs: number;
  } | null>(null);
  const [draggingId, setDraggingId] = useState<string | null>(null);

  const pctToDate = useCallback(
    (pct: number) => new Date(rangeStart + (pct / 100) * rangeMs),
    [rangeStart, rangeMs],
  );

  const onPointerMove = useCallback(
    (e: PointerEvent) => {
      const drag = dragRef.current;
      const body = bodyRef.current;
      if (!drag || !body || !onBarDatesChange) return;
      const rect = body.getBoundingClientRect();
      const deltaPct = ((e.clientX - drag.originX) / rect.width) * 100;
      const deltaMs = (deltaPct / 100) * rangeMs;
      let newStart = drag.startMs;
      let newEnd = drag.endMs;
      if (drag.mode === 'move') {
        newStart = drag.startMs + deltaMs;
        newEnd = drag.endMs + deltaMs;
      } else {
        newEnd = Math.max(drag.startMs + 86400000, drag.endMs + deltaMs);
      }
      onBarDatesChange(drag.itemId, toIso(new Date(newStart)), toIso(new Date(newEnd)));
    },
    [onBarDatesChange, rangeMs],
  );

  const endDrag = useCallback(() => {
    dragRef.current = null;
    setDraggingId(null);
    window.removeEventListener('pointermove', onPointerMove);
    window.removeEventListener('pointerup', endDrag);
  }, [onPointerMove]);

  const startDrag = (
    e: React.PointerEvent,
    itemId: string,
    mode: 'move' | 'resize-end',
    startMs: number,
    endMs: number,
  ) => {
    if (!onBarDatesChange) return;
    e.preventDefault();
    e.stopPropagation();
    dragRef.current = { itemId, mode, originX: e.clientX, startMs, endMs };
    setDraggingId(itemId);
    window.addEventListener('pointermove', onPointerMove);
    window.addEventListener('pointerup', endDrag);
  };

  return (
    <div className="jdc-roadmap-timeline-wrap">
      <div className="jdc-timeline-header">
        {weeks.map((w, i) => (
          <div key={i} className="jdc-timeline-week">
            {fmt(w)}
          </div>
        ))}
      </div>
      <div ref={bodyRef} className="jdc-timeline-body" style={{ position: 'relative' }}>
        <div className="jdc-timeline-today" style={{ left: `${todayPct}%` }} title="Today" />
        {rows.map((row) => {
          if (!row.item) {
            return <div key={row.key} className="jdc-timeline-row" />;
          }
          const s = parseDate(getStartDate(row.item.id) ?? row.item.targetDate);
          const e = parseDate(getEndDate(row.item.id) ?? row.item.targetEndDate);
          const end = e ?? (s ? new Date(s.getTime() + 7 * 86400000) : null);
          if (!s || !end) {
            return <div key={row.key} className="jdc-timeline-row" />;
          }
          const left = ((s.getTime() - rangeStart) / rangeMs) * 100;
          const width = Math.max(2, ((end.getTime() - s.getTime()) / rangeMs) * 100);
          const dragging = draggingId === row.item.id;
          return (
            <div key={row.key} className="jdc-timeline-row">
              <div
                className={`jdc-timeline-bar ${dragging ? 'dragging' : ''}`}
                style={{ left: `${left}%`, width: `${width}%` }}
                title={`${row.item.issueKey} ${row.item.summary ?? ''}`}
                onPointerDown={(ev) =>
                  startDrag(ev, row.item!.id, 'move', s.getTime(), end.getTime())
                }
              >
                <span
                  className="jdc-timeline-bar-handle"
                  onPointerDown={(ev) => {
                    ev.stopPropagation();
                    startDrag(ev, row.item!.id, 'resize-end', s.getTime(), end.getTime());
                  }}
                />
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
