import { useRef, useEffect } from 'react';
import { CumulativeFlowResponse } from '../../hooks/useSprint';

interface CumulativeFlowChartProps {
  data: CumulativeFlowResponse;
}

const COLORS = ['#3b82f6', '#22c55e', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899'];

const PADDING = { top: 30, right: 20, bottom: 60, left: 50 };

export default function CumulativeFlowChart({ data }: CumulativeFlowChartProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const { columns, dataPoints } = data;
    if (!dataPoints || dataPoints.length === 0 || !columns || columns.length === 0) return;

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
    ctx.fillText('Cumulative Flow Diagram', W / 2, 18);

    // Compute stacked totals per data point to find the Y max
    const stackedTotals = dataPoints.map((dp) =>
      columns.reduce((sum, col) => sum + (dp.columnCounts[col] ?? 0), 0),
    );
    const maxY = Math.max(...stackedTotals, 1);

    // Helpers
    const xStep = dataPoints.length > 1 ? chartW / (dataPoints.length - 1) : chartW;
    const toX = (i: number) => PADDING.left + i * xStep;
    const toY = (val: number) => PADDING.top + chartH - (val / maxY) * chartH;

    // Draw stacked areas (draw from top column to bottom so lower columns paint over upper)
    for (let colIdx = columns.length - 1; colIdx >= 0; colIdx--) {
      const color = COLORS[colIdx % COLORS.length];

      // For each data point compute the cumulative value up through this column
      const cumulativeValues = dataPoints.map((dp) => {
        let sum = 0;
        for (let c = 0; c <= colIdx; c++) {
          sum += dp.columnCounts[columns[c]] ?? 0;
        }
        return sum;
      });

      ctx.beginPath();
      ctx.moveTo(toX(0), toY(cumulativeValues[0]));
      for (let i = 1; i < cumulativeValues.length; i++) {
        ctx.lineTo(toX(i), toY(cumulativeValues[i]));
      }
      // Close path along the bottom (or along the previous column's top)
      ctx.lineTo(toX(cumulativeValues.length - 1), toY(0));
      ctx.lineTo(toX(0), toY(0));
      ctx.closePath();

      ctx.fillStyle = color + 'cc'; // slight transparency
      ctx.fill();
    }

    // Y-axis ticks and labels
    ctx.fillStyle = '#94a3b8';
    ctx.strokeStyle = '#334155';
    ctx.font = '10px sans-serif';
    ctx.textAlign = 'right';
    const yTicks = 5;
    for (let t = 0; t <= yTicks; t++) {
      const val = Math.round((maxY / yTicks) * t);
      const y = toY(val);
      ctx.fillText(String(val), PADDING.left - 6, y + 3);

      // grid line
      ctx.beginPath();
      ctx.moveTo(PADDING.left, y);
      ctx.lineTo(W - PADDING.right, y);
      ctx.lineWidth = 0.5;
      ctx.stroke();
    }

    // X-axis labels (show a subset to avoid overlap)
    ctx.fillStyle = '#94a3b8';
    ctx.font = '9px sans-serif';
    ctx.textAlign = 'center';
    const maxLabels = 8;
    const labelStep = Math.max(1, Math.floor(dataPoints.length / maxLabels));
    for (let i = 0; i < dataPoints.length; i += labelStep) {
      const dateStr = dataPoints[i].date;
      const label = dateStr.length >= 10 ? dateStr.substring(5, 10) : dateStr; // MM-DD
      ctx.fillText(label, toX(i), PADDING.top + chartH + 14);
    }

    // Axes lines
    ctx.strokeStyle = '#475569';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(PADDING.left, PADDING.top);
    ctx.lineTo(PADDING.left, PADDING.top + chartH);
    ctx.lineTo(W - PADDING.right, PADDING.top + chartH);
    ctx.stroke();

    // Legend
    const legendY = H - 20;
    const legendStartX = PADDING.left;
    let cursorX = legendStartX;
    ctx.font = '10px sans-serif';
    ctx.textAlign = 'left';
    columns.forEach((col, idx) => {
      const color = COLORS[idx % COLORS.length];
      ctx.fillStyle = color;
      ctx.fillRect(cursorX, legendY - 8, 10, 10);
      ctx.fillStyle = '#cbd5e1';
      ctx.fillText(col, cursorX + 14, legendY);
      cursorX += ctx.measureText(col).width + 28;
    });
  }, [data]);

  if (!data.dataPoints || data.dataPoints.length === 0) {
    return (
      <div
        style={{
          padding: 'var(--ab-spacing-md)',
          color: 'var(--ab-color-text-secondary)',
          textAlign: 'center',
        }}
      >
        <h3 style={{ margin: '0 0 var(--ab-spacing-sm)' }}>Cumulative Flow Diagram</h3>
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
