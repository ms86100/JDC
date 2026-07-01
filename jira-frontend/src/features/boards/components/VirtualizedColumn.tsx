import React, { useState, useEffect, useRef, useMemo, useCallback } from 'react';
import type { BoardIssue } from '../../../api/boardApi';
import type { CardCustomFieldRow } from './IssueCard';

const CARD_HEIGHT = 120;
const CARD_GAP = 8;
const OVERSCAN = 3;

interface VirtualizedColumnProps {
  issues: BoardIssue[];
  renderCard: (issue: BoardIssue, index: number) => React.ReactNode;
  totalHeight: number;
  scrollTop: number;
  onScroll?: (scrollTop: number) => void;
}

export default function VirtualizedColumn({
  issues,
  renderCard,
  totalHeight,
  scrollTop,
  onScroll,
}: VirtualizedColumnProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [internalScrollTop, setInternalScrollTop] = useState(0);

  const currentScrollTop = onScroll ? scrollTop : internalScrollTop;

  const handleScroll = useCallback((e: React.UIEvent<HTMLDivElement>) => {
    if (onScroll) {
      onScroll(e.currentTarget.scrollTop);
    } else {
      setInternalScrollTop(e.currentTarget.scrollTop);
    }
  }, [onScroll]);

  const visibleRange = useMemo(() => {
    const containerHeight = totalHeight || 600;
    const startIndex = Math.max(0, Math.floor(currentScrollTop / (CARD_HEIGHT + CARD_GAP) - OVERSCAN);
    const visibleCount = Math.ceil(containerHeight / (CARD_HEIGHT + CARD_GAP);
    const endIndex = Math.min(issues.length - 1, startIndex + visibleCount + OVERSCAN * 2);

    return { startIndex, endIndex };
  }, [currentScrollTop, issues.length, totalHeight]);

  const visibleItems = useMemo(() => {
    const items = [];
    for (let i = visibleRange.startIndex; i <= visibleRange.endIndex; i++) {
      if (issues[i]) {
        items.push({ index: i, issue: issues[i] });
      }
    }
    return items;
  }, [visibleRange, issues]);

  const totalContentHeight = issues.length * (CARD_HEIGHT + CARD_GAP) - CARD_GAP;

  return (
    <div
      ref={containerRef}
      className="ab-virtual-column-container"
      style={{ height: totalHeight, overflowY: 'auto' }}
      onScroll={handleScroll}
    >
      <div style={{ height: totalContentHeight, position: 'relative' }}>
        {visibleItems.map(({ index, issue }) => (
          <div
            key={issue.id}
            style={{
              position: 'absolute',
              top: index * (CARD_HEIGHT + CARD_GAP),
              left: 0,
              right: 0,
              height: CARD_HEIGHT,
            }}
          >
            {renderCard(issue, index)}
          </div>
        ))}
      </div>
      {issues.length > 0 && (
        <div className="ab-virtual-column-info">
          {issues.length} issues
          {visibleRange.startIndex + 1}-{visibleRange.endIndex + 1} visible
        </div>
      )}
    </div>
  );
}
