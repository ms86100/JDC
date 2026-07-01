import { useRef, useEffect } from 'react';
import { ControlChartResponse } from '../../hooks/useSprint';

interface ControlChartProps {
  data: ControlChartResponse;
}

const PADDING = { top: 30, right: 20, bottom: 60, left: 50 };

const DOT_RADIUS = 4;
const DOT_COLOR = '#3b82f6';

export default function ControlChart({ data }: ControlChartProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const { issues, averageCycleTime, standardDeviation } = data;
    if (!issues || issues.length === 0) return;

    const W = canvas.width;
    const H = canvas.height;
    const chartW = W - PADDING.left - PADDING.right;
    const chartH = H - PADDING.top - PADDING.bottom;

    // Clear
    ctx.clearRect(0, 0, W, H);

    // Title
    ctx.fillStyle = '#e2e8f0';
    ctx.font = 'bold 14px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('Control Chart — Cycle Time', W / 2, 18);

    // Sort issues by completedAt for x-axis ordering
    const sorted = [...issues].sort(
      (a, b) => new Date(a.completedAt).getTime() - new Date(b.completedAt).getTime(),
    );

    // Determine axis ranges
    const minDate = new Date(sorted[0].completedAt).getTime();
    const maxDate = new Date(sorted[sorted.length - 1].completedAt).getTime();
    const dateRange = maxDate - minDate || 1;

    const meanPlusStd = averageCycleTime + standardDeviation;
    const meanMinusStd = Math.max(averageCycleTime - standardDeviation, 0);
    const maxCycle = Math.max(...sorted.map((i) => i.cycleTimeDays), meanPlusStd);
    const yMax = maxCycle * 1.15; // a bit of headroom

    // Helpers
    const toX = (dateStr: string) => {
      const t = new Date(dateStr).getTime();
      return PADDING.left + ((t - minDate) / dateRange) * chartW;
    };
    const toY = (val: number) => PADDING.top + chartH - (val / yMax) * chartH;

    // Grid lines (Y)
    ctx.strokeStyle = '#334155';
    ctx.fillStyle = '#94a3b8';
    ctx.font = '10px sans-serif';
    ctx.textAlign = 'right';
    ctx.lineWidth = 0.5;
    const yTicks = 5;
    for (let t = 0; t <= yTicks; t++) {
      const val = +((yMax / yTicks) * t).toFixed(1);
      const y = toY(val);
      ctx.fillText(String(val), PADDING.left - 6, y + 3);
      ctx.beginPath();
      ctx.moveTo(PADDING.left, y);
      ctx.lineTo(W - PADDING.right, y);
      ctx.stroke();
    }

    // Axes
    ctx.strokeStyle = '#475569';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(PADDING.left, PADDING.top);
    ctx.lineTo(PADDING.left, PADDING.top + chartH);
    ctx.lineTo(W - PADDING.right, PADDING.top + chartH);
    ctx.stroke();

    // --- Horizontal reference lines ---

    // Mean line (solid orange)
    const meanY = toY(averageCycleTime);
    ctx.strokeStyle = '#f59e0b';
    ctx.lineWidth = 1.5;
    ctx.setLineDash([]);
    ctx.beginPath();
    ctx.moveTo(PADDING.left, meanY);
    ctx.lineTo(W - PADDING.right, meanY);
    ctx.stroke();

    // Mean + 1 stddev (dashed red)
    const upperY = toY(meanPlusStd);
    ctx.strokeStyle = '#ef4444';
    ctx.lineWidth = 1;
    ctx.setLineDash([6, 4]);
    ctx.beginPath();
    ctx.moveTo(PADDING.left, upperY);
    ctx.lineTo(W - PADDING.right, upperY);
    ctx.stroke();

    // Mean - 1 stddev (dashed green)
    const lowerY = toY(meanMinusStd);
    ctx.strokeStyle = '#22c55e';
    ctx.setLineDash([6, 4]);
    ctx.beginPath();
    ctx.moveTo(PADDING.left, lowerY);
    ctx.lineTo(W - PADDING.right, lowerY);
    ctx.stroke();

    ctx.setLineDash([]);

    // --- Scatter dots ---
    sorted.forEach((issue) => {
      const x = toX(issue.completedAt);
      const y = toY(issue.cycleTimeDays);
      ctx.beginPath();
      ctx.arc(x, y, DOT_RADIUS, 0, Math.PI * 2);
      ctx.fillStyle = DOT_COLOR;
      ctx.fill();
      ctx.strokeStyle = '#1e3a5f';
      ctx.lineWidth = 1;
      ctx.stroke();
    });

    // --- X-axis date labels ---
    ctx.fillStyle = '#94a3b8';
    ctx.font = '9px sans-serif';
    ctx.textAlign = 'center';
    const maxLabels = 8;
    const labelStep = Math.max(1, Math.floor(sorted.length / maxLabels));
    for (let i = 0; i < sorted.length; i += labelStep) {
      const dateStr = sorted[i].completedAt;
      const label = dateStr.length >= 10 ? dateStr.substring(0, 10) : dateStr;
      const x = toX(dateStr);
      ctx.save();
      ctx.translate(x, PADDING.top + chartH + 12);
      ctx.rotate(-0.4);
      ctx.fillText(label, 0, 0);
      ctx.restore();
    }

    // --- Stats box ---
    const boxX = W - PADDING.right - 160;
    const boxY = PADDING.top + 6;
    const boxW = 150;
    const boxH = 48;
    ctx.fillStyle = 'rgba(15, 23, 42, 0.85)';
    ctx.strokeStyle = '#475569';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.roundRect(boxX, boxY, boxW, boxH, 4);
    ctx.fill();
    ctx.stroke();

    ctx.fillStyle = '#e2e8f0';
    ctx.font = '10px sans-serif';
    ctx.textAlign = 'left';
    ctx.fillText(`Avg Cycle: ${averageCycleTime.toFixed(1)} days`, boxX + 8, boxY + 18);
    ctx.fillText(`Std Dev:   ${standardDeviation.toFixed(1)} days`, boxX + 8, boxY + 36);

    // --- Legend ---
    const legendY = H - 14;
    let cursorX = PADDING.left;
    ctx.font = '10px sans-serif';
    ctx.textAlign = 'left';

    // Dot
    ctx.beginPath();
    ctx.arc(cursorX + 5, legendY - 3, 4, 0, Math.PI * 2);
    ctx.fillStyle = DOT_COLOR;
    ctx.fill();
    ctx.fillStyle = '#cbd5e1';
    ctx.fillText('Issue', cursorX + 14, legendY);
    cursorX += 60;

    // Mean
    ctx.strokeStyle = '#f59e0b';
    ctx.lineWidth = 2;
    ctx.setLineDash([]);
    ctx.beginPath();
    ctx.moveTo(cursorX, legendY - 3);
    ctx.lineTo(cursorX + 14, legendY - 3);
    ctx.stroke();
    ctx.fillStyle = '#cbd5e1';
    ctx.fillText('Mean', cursorX + 18, legendY);
    cursorX += 60;

    // +1 Std Dev
    ctx.strokeStyle = '#ef4444';
    ctx.lineWidth = 1;
    ctx.setLineDash([4, 3]);
    ctx.beginPath();
    ctx.moveTo(cursorX, legendY - 3);
    ctx.lineTo(cursorX + 14, legendY - 3);
    ctx.stroke();
    ctx.fillStyle = '#cbd5e1';
    ctx.fillText('+1σ', cursorX + 18, legendY);
    cursorX += 50;

    // -1 Std Dev
    ctx.strokeStyle = '#22c55e';
    ctx.setLineDash([4, 3]);
    ctx.beginPath();
    ctx.moveTo(cursorX, legendY - 3);
    ctx.lineTo(cursorX + 14, legendY - 3);
    ctx.stroke();
    ctx.fillStyle = '#cbd5e1';
    ctx.fillText('−1σ', cursorX + 18, legendY);

    ctx.setLineDash([]);
  }, [data]);

  if (!data.issues || data.issues.length === 0) {
    return (
      <div
        style={{
          padding: 'var(--ab-spacing-md)',
          color: 'var(--ab-color-text-secondary)',
          textAlign: 'center',
        }}
      >
        <h3 style={{ margin: '0 0 var(--ab-spacing-sm)' }}>Control Chart &mdash; Cycle Time</h3>
        <p>No data available.</p>
      </div>
    );
  }

  return (
    <div
      style={{
        background: 'var(--ab-color-bg-card, #1e293b)',
        borderRadius: 'var(--ab-border-radius-md, 8px)',
        padding: 'var(--ab-spacing-md, 16px)',
      }}
    >
      <canvas
        ref={canvasRef}
        width={600}
        height={300}
        style={{ width: '100%', display: 'block' }}
      />
    </div>
  );
}
