import React, { useRef, useEffect } from 'react';
import { SprintVelocity } from '../../../api/sprintApi';

interface VelocityChartProps {
  data: SprintVelocity[];
  averageVelocity: number;
}

export default function VelocityChart({ data, averageVelocity }: VelocityChartProps) {
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

    ctx.clearRect(0, 0, width, height);

    const completedData = data.filter(d => d.isCompleted);
    const maxY = Math.max(
      averageVelocity * 1.5,
      ...completedData.map(d => Math.max(d.committedPoints, d.completedPoints))
    );

    // Draw grid and Y axis
    ctx.strokeStyle = '#e5e7eb';
    ctx.lineWidth = 1;
    ctx.fillStyle = '#6b7280';
    ctx.font = '11px system-ui';

    const ySteps = 5;
    for (let i = 0; i <= ySteps; i++) {
      const y = padding.top + (chartHeight / ySteps) * i;
      const value = Math.round(maxY - (maxY / ySteps) * i);
      ctx.fillText(String(value), padding.left - 8, y + 4);
      ctx.beginPath();
      ctx.moveTo(padding.left, y);
      ctx.lineTo(width - padding.right, y);
      ctx.stroke();
    }

    // Average line
    if (averageVelocity > 0) {
      const avgY = padding.top + (chartHeight / maxY) * (maxY - averageVelocity);
      ctx.strokeStyle = '#f59e0b';
      ctx.lineWidth = 2;
      ctx.setLineDash([8, 4]);
      ctx.beginPath();
      ctx.moveTo(padding.left, avgY);
      ctx.lineTo(width - padding.right, avgY);
      ctx.stroke();
      ctx.setLineDash([]);

      ctx.fillStyle = '#f59e0b';
      ctx.fillText(`Avg: ${averageVelocity}`, width - 60, avgY - 5);
    }

    // Bars
    const barWidth = Math.min(40, chartWidth / completedData.length - 10);
    const gap = (chartWidth - barWidth * completedData.length) / (completedData.length + 1);

    completedData.forEach((sprint, index) => {
      const x = padding.left + gap + (barWidth + gap) * index;

      // Committed bar
      const committedHeight = (sprint.committedPoints / maxY) * chartHeight;
      ctx.fillStyle = '#93c5fd';
      ctx.fillRect(x, height - padding.bottom - committedHeight, barWidth / 2 - 2, committedHeight);

      // Completed bar
      const completedHeight = (sprint.completedPoints / maxY) * chartHeight;
      ctx.fillStyle = '#3b82f6';
      ctx.fillRect(x + barWidth / 2 + 2, height - padding.bottom - completedHeight, barWidth / 2 - 2, completedHeight);

      // X-axis label
      const label = sprint.sprintName.length > 8
        ? sprint.sprintName.substring(0, 8) + '...'
        : sprint.sprintName;
      ctx.fillStyle = '#6b7280';
      ctx.fillText(label, x + barWidth / 2 - 15, height - padding.bottom + 20);
    });

    // Legend
    ctx.fillStyle = '#6b7280';
    ctx.font = '11px system-ui';

    ctx.fillStyle = '#93c5fd';
    ctx.fillRect(width - 150, padding.top - 10, 12, 12);
    ctx.fillStyle = '#374151';
    ctx.fillText('Committed', width - 135, padding.top);

    ctx.fillStyle = '#3b82f6';
    ctx.fillRect(width - 80, padding.top - 10, 12, 12);
    ctx.fillStyle = '#374151';
    ctx.fillText('Completed', width - 65, padding.top);

  }, [data, averageVelocity]);

  return (
    <div className="ab-velocity-chart">
      <canvas
        ref={canvasRef}
        width={500}
        height={250}
        style={{ width: '100%', height: '250px' }}
      />
      <style>{`
        .ab-velocity-chart {
          background: var(--ab-white);
          border-radius: var(--ab-radius-md);
          padding: var(--ab-spacing-md);
        }
      `}</style>
    </div>
  );
}