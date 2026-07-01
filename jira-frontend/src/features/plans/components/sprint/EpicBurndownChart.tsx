import { useRef, useEffect } from 'react';
import { EpicBurndownResponse } from '../../hooks/useSprint';

interface EpicBurndownChartProps {
  data: EpicBurndownResponse;
}

export default function EpicBurndownChart({ data }: EpicBurndownChartProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const width = canvas.width;
    const height = canvas.height;

    // Clear canvas
    ctx.clearRect(0, 0, width, height);

    const entries = data.sprintEntries;
    if (entries.length === 0) return;

    // Layout constants
    const paddingTop = 50;
    const paddingBottom = 60;
    const paddingLeft = 50;
    const paddingRight = 30;
    const legendHeight = 20;
    const chartWidth = width - paddingLeft - paddingRight;
    const chartHeight = height - paddingTop - paddingBottom - legendHeight;

    // Calculate max value for Y-axis
    const maxPoints = Math.max(...entries.map((e) => Math.max(e.totalPoints, e.completedPoints)), 1);
    const yScale = chartHeight / maxPoints;

    // Bar dimensions
    const groupWidth = chartWidth / entries.length;
    const barWidth = groupWidth * 0.3;
    const barGap = 4;

    // Draw title
    ctx.fillStyle = '#1e293b';
    ctx.font = 'bold 14px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText(data.epicName, width / 2, 24);

    // Draw Y-axis
    ctx.strokeStyle = '#cbd5e1';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(paddingLeft, paddingTop);
    ctx.lineTo(paddingLeft, paddingTop + chartHeight);
    ctx.stroke();

    // Draw Y-axis ticks and labels
    const yTickCount = 5;
    ctx.fillStyle = '#64748b';
    ctx.font = '11px sans-serif';
    ctx.textAlign = 'right';
    for (let i = 0; i <= yTickCount; i++) {
      const value = Math.round((maxPoints / yTickCount) * i);
      const y = paddingTop + chartHeight - value * yScale;

      // Grid line
      ctx.strokeStyle = '#e2e8f0';
      ctx.beginPath();
      ctx.moveTo(paddingLeft, y);
      ctx.lineTo(paddingLeft + chartWidth, y);
      ctx.stroke();

      // Label
      ctx.fillStyle = '#64748b';
      ctx.fillText(String(value), paddingLeft - 8, y + 4);
    }

    // Draw X-axis
    ctx.strokeStyle = '#cbd5e1';
    ctx.beginPath();
    ctx.moveTo(paddingLeft, paddingTop + chartHeight);
    ctx.lineTo(paddingLeft + chartWidth, paddingTop + chartHeight);
    ctx.stroke();

    // Draw bars for each sprint
    entries.forEach((entry, index) => {
      const groupX = paddingLeft + index * groupWidth + groupWidth / 2;

      // Total Points bar (light blue)
      const totalBarHeight = entry.totalPoints * yScale;
      const totalBarX = groupX - barWidth - barGap / 2;
      const totalBarY = paddingTop + chartHeight - totalBarHeight;
      ctx.fillStyle = '#93c5fd';
      ctx.fillRect(totalBarX, totalBarY, barWidth, totalBarHeight);

      // Completed Points bar (dark blue)
      const completedBarHeight = entry.completedPoints * yScale;
      const completedBarX = groupX + barGap / 2;
      const completedBarY = paddingTop + chartHeight - completedBarHeight;
      ctx.fillStyle = '#2563eb';
      ctx.fillRect(completedBarX, completedBarY, barWidth, completedBarHeight);

      // X-axis label (sprint name)
      ctx.fillStyle = '#64748b';
      ctx.font = '10px sans-serif';
      ctx.textAlign = 'center';
      const labelY = paddingTop + chartHeight + 16;
      const labelText =
        entry.sprintName.length > 12
          ? entry.sprintName.substring(0, 12) + '...'
          : entry.sprintName;
      ctx.fillText(labelText, groupX, labelY);
    });

    // Draw legend
    const legendY = height - 16;
    const legendStartX = width / 2 - 120;

    // Total Points legend
    ctx.fillStyle = '#93c5fd';
    ctx.fillRect(legendStartX, legendY - 10, 14, 14);
    ctx.fillStyle = '#1e293b';
    ctx.font = '12px sans-serif';
    ctx.textAlign = 'left';
    ctx.fillText('Total Points', legendStartX + 20, legendY + 2);

    // Completed Points legend
    const completedLegendX = legendStartX + 130;
    ctx.fillStyle = '#2563eb';
    ctx.fillRect(completedLegendX, legendY - 10, 14, 14);
    ctx.fillStyle = '#1e293b';
    ctx.fillText('Completed Points', completedLegendX + 20, legendY + 2);
  }, [data]);

  return (
    <div className="ab-epic-burndown">
      <canvas
        ref={canvasRef}
        width={600}
        height={300}
        style={{ border: '1px solid var(--ab-color-border, #e2e8f0)', borderRadius: '8px' }}
      />
    </div>
  );
}
