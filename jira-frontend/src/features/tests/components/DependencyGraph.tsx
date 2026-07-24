import React, { useState, useRef, useEffect, useCallback } from 'react';
import { ZoomIn, ZoomOut, Maximize2, RotateCcw } from 'lucide-react';
import { chartColors } from '../../../utils/chartColors';

export interface GraphNode {
  id: string;
  label: string;
  type: 'TEST' | 'COMPONENT' | 'REQUIREMENT';
  impactLevel?: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  x?: number;
  y?: number;
}

export interface GraphEdge {
  id: string;
  source: string;
  target: string;
  weight: number;
  impactType: 'DIRECT' | 'TRANSITIVE' | 'CASCADING';
}

export interface DependencyGraphData {
  nodes: GraphNode[];
  edges: GraphEdge[];
}

interface DependencyGraphProps {
  data: DependencyGraphData;
  onNodeClick?: (node: GraphNode) => void;
  highlightedNodeId?: string;
  selectedNodeId?: string;
}

const DependencyGraph: React.FC<DependencyGraphProps> = ({
  data,
  onNodeClick,
  highlightedNodeId,
  selectedNodeId,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const [zoom, setZoom] = useState(1);
  const [pan, setPan] = useState({ x: 0, y: 0 });
  const [isDragging, setIsDragging] = useState(false);
  const [dragStart, setDragStart] = useState({ x: 0, y: 0 });
  const [hoveredNode, setHoveredNode] = useState<string | null>(null);

  const getImpactColor = (level?: string): string => {
    switch (level) {
      case 'CRITICAL': return chartColors.danger;
      case 'HIGH': return chartColors.orange;
      case 'MEDIUM': return chartColors.warningDark;
      case 'LOW': return chartColors.success;
      default: return chartColors.neutral600;
    }
  };

  const getNodeTypeIcon = (type: string): string => {
    switch (type) {
      case 'TEST': return '🧪';
      case 'COMPONENT': return '📦';
      case 'REQUIREMENT': return '📋';
      default: return '⚫';
    }
  };

  const getEdgeStyle = (edge: GraphEdge): React.CSSProperties => {
    const baseOpacity = hoveredNode && (edge.source === hoveredNode || edge.target === hoveredNode)
      ? 1
      : 0.5;
    return {
      opacity: baseOpacity,
      strokeWidth: edge.impactType === 'DIRECT' ? 2 : 1,
      strokeDasharray: edge.impactType === 'TRANSITIVE' ? '5,5' : edge.impactType === 'CASCADING' ? '3,3' : undefined,
    };
  };

  const handleZoomIn = () => setZoom(z => Math.min(z + 0.2, 3));
  const handleZoomOut = () => setZoom(z => Math.max(z - 0.2, 0.3));
  const handleReset = () => {
    setZoom(1);
    setPan({ x: 0, y: 0 });
  };
  const handleFit = () => {
    setZoom(1);
    setPan({ x: 0, y: 0 });
  };

  const handleMouseDown = (e: React.MouseEvent) => {
    if (e.target === containerRef.current || (e.target as HTMLElement).classList.contains('graph-svg')) {
      setIsDragging(true);
      setDragStart({ x: e.clientX - pan.x, y: e.clientY - pan.y });
    }
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    if (isDragging) {
      setPan({
        x: e.clientX - dragStart.x,
        y: e.clientY - dragStart.y,
      });
    }
  };

  const handleMouseUp = () => {
    setIsDragging(false);
  };

  const handleWheel = (e: React.WheelEvent) => {
    e.preventDefault();
    const delta = e.deltaY > 0 ? -0.1 : 0.1;
    setZoom(z => Math.max(0.3, Math.min(3, z + delta)));
  };

  // Calculate node positions using force-directed layout simulation
  const calculateLayout = useCallback(() => {
    const nodes = [...data.nodes];
    const edges = data.edges;

    if (nodes.length === 0) return data;

    // Initialize positions
    const centerX = 400;
    const centerY = 300;
    const radius = Math.max(150, nodes.length * 30);

    nodes.forEach((node, index) => {
      if (!node.x || !node.y) {
        const angle = (2 * Math.PI * index) / nodes.length;
        node.x = centerX + radius * Math.cos(angle);
        node.y = centerY + radius * Math.sin(angle);
      }
    });

    // Simple force simulation
    const iterations = 50;
    for (let i = 0; i < iterations; i++) {
      // Repulsion between nodes
      nodes.forEach(node1 => {
        nodes.forEach(node2 => {
          if (node1.id === node2.id) return;
          const dx = (node1.x || 0) - (node2.x || 0);
          const dy = (node1.y || 0) - (node2.y || 0);
          const distance = Math.sqrt(dx * dx + dy * dy) || 1;
          const force = 1000 / (distance * distance);
          node1.x! += (dx / distance) * force * 0.1;
          node1.y! += (dy / distance) * force * 0.1;
        });
      });

      // Attraction along edges
      edges.forEach(edge => {
        const source = nodes.find(n => n.id === edge.source);
        const target = nodes.find(n => n.id === edge.target);
        if (source && target) {
          const dx = (target.x || 0) - (source.x || 0);
          const dy = (target.y || 0) - (source.y || 0);
          const distance = Math.sqrt(dx * dx + dy * dy) || 1;
          const force = (distance - 100) * 0.01;
          source.x! += (dx / distance) * force;
          source.y! += (dy / distance) * force;
          target.x! -= (dx / distance) * force;
          target.y! -= (dy / distance) * force;
        }
      });

      // Center gravity
      nodes.forEach(node => {
        node.x! += (centerX - (node.x || 0)) * 0.01;
        node.y! += (centerY - (node.y || 0)) * 0.01;
      });
    }

    return { nodes, edges };
  }, [data]);

  const layout = calculateLayout();
  const nodeMap = new Map(layout.nodes.map(n => [n.id, n]));

  return (
    <div className="dependency-graph">
      {/* Controls */}
      <div className="graph-controls absolute top-4 right-4 flex flex-col gap-2 bg-white rounded-lg shadow-md p-2 z-10">
        <button
          onClick={handleZoomIn}
          className="p-2 hover:bg-gray-100 rounded transition-colors"
          title="Zoom In"
        >
          <ZoomIn size={18} />
        </button>
        <button
          onClick={handleZoomOut}
          className="p-2 hover:bg-gray-100 rounded transition-colors"
          title="Zoom Out"
        >
          <ZoomOut size={18} />
        </button>
        <button
          onClick={handleFit}
          className="p-2 hover:bg-gray-100 rounded transition-colors"
          title="Fit to View"
        >
          <Maximize2 size={18} />
        </button>
        <button
          onClick={handleReset}
          className="p-2 hover:bg-gray-100 rounded transition-colors"
          title="Reset View"
        >
          <RotateCcw size={18} />
        </button>
      </div>

      {/* Legend */}
      <div className="graph-legend absolute bottom-4 left-4 bg-white rounded-lg shadow-md p-3 z-10">
        <div className="text-xs font-semibold mb-2">Impact Level</div>
        <div className="flex flex-col gap-1 text-xs">
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 rounded-full" style={{ backgroundColor: chartColors.danger }} />
            <span>Critical</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 rounded-full" style={{ backgroundColor: chartColors.orange }} />
            <span>High</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 rounded-full" style={{ backgroundColor: chartColors.warningDark }} />
            <span>Medium</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 rounded-full" style={{ backgroundColor: chartColors.success }} />
            <span>Low</span>
          </div>
        </div>
      </div>

      {/* Graph Container */}
      <div
        ref={containerRef}
        className="graph-container w-full h-full min-h-[400px] bg-gray-50 rounded-lg overflow-hidden cursor-grab"
        onMouseDown={handleMouseDown}
        onMouseMove={handleMouseMove}
        onMouseUp={handleMouseUp}
        onMouseLeave={handleMouseUp}
        onWheel={handleWheel}
      >
        <svg className="graph-svg w-full h-full" viewBox="0 0 800 600">
          <defs>
            <marker
              id="arrowhead"
              markerWidth="10"
              markerHeight="7"
              refX="10"
              refY="3.5"
              orient="auto"
            >
              <polygon points="0 0, 10 3.5, 0 7" fill={chartColors.neutral600} />
            </marker>
            <marker
              id="arrowhead-active"
              markerWidth="10"
              markerHeight="7"
              refX="10"
              refY="3.5"
              orient="auto"
            >
              <polygon points="0 0, 10 3.5, 0 7" fill={chartColors.primary} />
            </marker>
          </defs>

          <g transform={`translate(${pan.x}, ${pan.y}) scale(${zoom})`}>
            {/* Edges */}
            {layout.edges.map(edge => {
              const sourceNode = nodeMap.get(edge.source);
              const targetNode = nodeMap.get(edge.target);
              if (!sourceNode || !targetNode) return null;

              const isHighlighted = hoveredNode === edge.source || hoveredNode === edge.target;
              const midX = ((sourceNode.x || 0) + (targetNode.x || 0)) / 2;
              const midY = ((sourceNode.y || 0) + (targetNode.y || 0)) / 2;

              return (
                <g key={edge.id}>
                  <line
                    x1={sourceNode.x}
                    y1={sourceNode.y}
                    x2={targetNode.x}
                    y2={targetNode.y}
                    stroke={isHighlighted ? chartColors.primary : chartColors.neutral400}
                    markerEnd={isHighlighted ? 'url(#arrowhead-active)' : 'url(#arrowhead)'}
                    style={getEdgeStyle(edge)}
                  />
                  {edge.impactType !== 'DIRECT' && (
                    <text
                      x={midX}
                      y={midY - 5}
                      textAnchor="middle"
                      fontSize="10"
                      fill={chartColors.neutral600}
                    >
                      {edge.impactType === 'TRANSITIVE' ? '↔' : '⇢'}
                    </text>
                  )}
                </g>
              );
            })}

            {/* Nodes */}
            {layout.nodes.map(node => {
              const isHovered = hoveredNode === node.id;
              const isSelected = selectedNodeId === node.id;
              const isHighlighted = highlightedNodeId === node.id;
              const nodeColor = getImpactColor(node.impactLevel);

              return (
                <g
                  key={node.id}
                  transform={`translate(${node.x}, ${node.y})`}
                  onMouseEnter={() => setHoveredNode(node.id)}
                  onMouseLeave={() => setHoveredNode(null)}
                  onClick={() => onNodeClick?.(node)}
                  style={{ cursor: 'pointer' }}
                >
                  {/* Node circle */}
                  <circle
                    r={isHovered || isSelected ? 35 : 30}
                    fill={nodeColor}
                    opacity={0.15}
                    stroke={nodeColor}
                    strokeWidth={isSelected || isHighlighted ? 3 : 2}
                  />
                  <circle
                    r={isHovered || isSelected ? 28 : 24}
                    fill="white"
                    stroke={nodeColor}
                    strokeWidth={isSelected || isHighlighted ? 2 : 1}
                  />

                  {/* Node icon */}
                  <text
                    textAnchor="middle"
                    dy="0.35em"
                    fontSize="16"
                  >
                    {getNodeTypeIcon(node.type)}
                  </text>

                  {/* Node label */}
                  <text
                    y={40}
                    textAnchor="middle"
                    fontSize="11"
                    fontWeight={isSelected || isHighlighted ? '600' : '400'}
                    fill={chartColors.neutral900}
                  >
                    {node.label.length > 20 ? node.label.substring(0, 17) + '...' : node.label}
                  </text>

                  {/* Impact badge */}
                  {node.impactLevel && (
                    <g transform="translate(20, -25)">
                      <rect
                        x="-12"
                        y="-8"
                        width="24"
                        height="16"
                        rx="4"
                        fill={nodeColor}
                      />
                      <text
                        textAnchor="middle"
                        dy="0.35em"
                        fontSize="8"
                        fill="white"
                        fontWeight="600"
                      >
                        {node.impactLevel.charAt(0)}
                      </text>
                    </g>
                  )}
                </g>
              );
            })}
          </g>
        </svg>

        {/* Empty state */}
        {layout.nodes.length === 0 && (
          <div className="absolute inset-0 flex items-center justify-center text-gray-400">
            <div className="text-center">
              <div className="text-4xl mb-2">📊</div>
              <div className="text-sm">No dependency data available</div>
              <div className="text-xs mt-1">Select a test to view its dependencies</div>
            </div>
          </div>
        )}
      </div>

      {/* Zoom indicator */}
      <div className="absolute bottom-4 right-4 bg-white rounded-lg shadow-md px-3 py-1 text-xs text-gray-600">
        {Math.round(zoom * 100)}%
      </div>
    </div>
  );
};

export default DependencyGraph;