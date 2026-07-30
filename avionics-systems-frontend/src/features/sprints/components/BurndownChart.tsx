import React, { useRef, useEffect } from 'react';
import { BurndownDataPoint } from '../../../api/sprintApi';
import { chartColors } from '../../../utils/chartColors';

interface BurndownChartProps {
  data: BurndownDataPoint[];
  totalPoints: number;
}

export default function BurndownChart({ data, totalPoints }: BurndownChartProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas || data.length === 0) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const width = canvas.width;
    const height = canvas.height;
    const padding = { top: 20, right: 20, bottom: 40, left: 50 };
    const chartWidth = width - padding.left - padding.right;
    const chartHeight = height - padding.top - padding.bottom;

    // Clear canvas
    ctx.clearRect(0, 0, width, height);

    // Draw grid lines
    ctx.strokeStyle = chartColors.neutral200;
    ctx.lineWidth = 1;

    // Y-axis labels and grid
    const ySteps = 5;
    const maxY = Math.max(totalPoints, ...data.map(d => d.remainingPoints || 0)) * 1.1;
    for (let i = 0; i <= ySteps; i++) {
      const y = padding.top + (chartHeight / ySteps) * i;
      const value = Math.round(maxY - (maxY / ySteps) * i);

      ctx.fillStyle = chartColors.neutral600;
      ctx.font = '11px system-ui';
      ctx.fillText(String(value), padding.left - 8, y + 4);

      ctx.beginPath();
      ctx.moveTo(padding.left, y);
      ctx.lineTo(width - padding.right, y);
      ctx.stroke();
    }

    // X-axis labels
    const xStep = Math.max(1, Math.floor(data.length / 6));
    data.forEach((point, index) => {
      if (index % xStep === 0 || index === data.length - 1) {
        const x = padding.left + (chartWidth / (data.length - 1 || 1)) * index;
        const date = new Date(point.date);
        const label = `${date.getMonth() + 1}/${date.getDate()}`;
        ctx.fillStyle = chartColors.neutral600;
        ctx.fillText(label, x - 15, height - padding.bottom + 20);
      }
    });

    // Draw ideal line (diagonal from total to 0)
    ctx.strokeStyle = chartColors.neutral400;
    ctx.setLineDash([5, 5]);
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.moveTo(padding.left, padding.top);
    ctx.lineTo(width - padding.right, height - padding.bottom);
    ctx.stroke();
    ctx.setLineDash([]);

    // Draw actual line
    if (data.length > 0) {
      ctx.strokeStyle = chartColors.primary;
      ctx.lineWidth = 3;
      ctx.lineJoin = 'round';
      ctx.lineCap = 'round';
      ctx.beginPath();

      data.forEach((point, index) => {
        const x = padding.left + (chartWidth / (data.length - 1 || 1)) * index;
        const yValue = point.remainingPoints || 0;
        const y = padding.top + (chartHeight / maxY) * (maxY - yValue);

        if (index === 0) {
          ctx.moveTo(x, y);
        } else {
          ctx.lineTo(x, y);
        }
      });
      ctx.stroke();

      // Draw points
      ctx.fillStyle = chartColors.primary;
      data.forEach((point, index) => {
        const x = padding.left + (chartWidth / (data.length - 1 || 1)) * index;
        const yValue = point.remainingPoints || 0;
        const y = padding.top + (chartHeight / maxY) * (maxY - yValue);

        ctx.beginPath();
        ctx.arc(x, y, 4, 0, Math.PI * 2);
        ctx.fill();
      });
    }

    // Legend
    ctx.fillStyle = chartColors.neutral600;
    ctx.font = '11px system-ui';

    ctx.strokeStyle = chartColors.primary;
    ctx.lineWidth = 3;
    ctx.beginPath();
    ctx.moveTo(width - 140, padding.top - 5);
    ctx.lineTo(width - 115, padding.top - 5);
    ctx.stroke();
    ctx.fillStyle = chartColors.neutral700;
    ctx.fillText('Actual', width - 110, padding.top);

    ctx.strokeStyle = chartColors.neutral400;
    ctx.lineWidth = 2;
    ctx.setLineDash([5, 5]);
    ctx.beginPath();
    ctx.moveTo(width - 70, padding.top - 5);
    ctx.lineTo(width - 45, padding.top - 5);
    ctx.stroke();
    ctx.setLineDash([]);
    ctx.fillStyle = chartColors.neutral700;
    ctx.fillText('Ideal', width - 40, padding.top);

  }, [data, totalPoints]);

  return (
    <div className="ab-burndown-chart">
      <canvas
        ref={canvasRef}
        width={600}
        height={300}
        style={{ width: '100%', height: '300px' }}
      />
      <style>{`
        .ab-burndown-chart {
          background: var(--ab-white);
          border-radius: var(--ab-radius-md);
          padding: var(--ab-spacing-md);
        }
      `}</style>
    </div>
  );
}