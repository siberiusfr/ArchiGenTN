"use client";

import { useMemo } from "react";
import type { PlanResponse } from "@/src/api/models/planResponse";

const ROOM_COLORS: Record<string, { fill: string; stroke: string; text: string }> = {
  salon:   { fill: "#dbeafe", stroke: "#3b82f6", text: "#1e40af" },
  chambre: { fill: "#ede9fe", stroke: "#8b5cf6", text: "#5b21b6" },
  cuisine: { fill: "#ffedd5", stroke: "#f97316", text: "#c2410c" },
  sdb:     { fill: "#cffafe", stroke: "#06b6d4", text: "#0e7490" },
  wc:      { fill: "#e0f2fe", stroke: "#0ea5e9", text: "#0369a1" },
  entree:  { fill: "#fef3c7", stroke: "#f59e0b", text: "#b45309" },
  couloir: { fill: "#f3f4f6", stroke: "#9ca3af", text: "#4b5563" },
  bureau:  { fill: "#dcfce7", stroke: "#22c55e", text: "#15803d" },
  garage:  { fill: "#e5e7eb", stroke: "#6b7280", text: "#374151" },
};

const DEFAULT_COLOR = { fill: "#f5f5f4", stroke: "#a8a29e", text: "#57534e" };

interface PlanViewerProps {
  plan: PlanResponse;
  onExportDxf?: () => void;
  onAnalyze?: () => void;
  isExporting?: boolean;
  isAnalyzing?: boolean;
}

export default function PlanViewer({
  plan,
  onExportDxf,
  onAnalyze,
  isExporting,
  isAnalyzing,
}: PlanViewerProps) {
  const terrain = plan.terrain;
  const rooms = plan.rooms || [];
  const doors = plan.doors || [];
  const windows = plan.windows || [];
  const metrics = plan.metrics;
  const wallThickness = plan.wallThickness || 0.2;

  const SCALE = 30;
  const PADDING = 40;

  const svgWidth = (terrain?.width || 15) * SCALE + PADDING * 2;
  const svgHeight = (terrain?.height || 25) * SCALE + PADDING * 2;

  const toX = (x: number) => PADDING + x * SCALE;
  const toY = (y: number) => PADDING + ((terrain?.height || 25) - y) * SCALE;

  const legend = useMemo(() => {
    const types = new Set(rooms.map((r) => r.type));
    return Array.from(types);
  }, [rooms]);

  return (
    <div className="flex flex-col gap-4">
      {/* SVG Plan */}
      <div className="overflow-auto rounded-2xl border border-border bg-white p-4 dark:bg-[#1a1917]">
        <svg
          viewBox={`0 0 ${svgWidth} ${svgHeight}`}
          className="mx-auto"
          style={{ maxHeight: "65vh", width: "100%" }}
        >
          {/* Grid */}
          <defs>
            <pattern id="grid" width={SCALE} height={SCALE} patternUnits="userSpaceOnUse">
              <path d={`M ${SCALE} 0 L 0 0 0 ${SCALE}`} fill="none" stroke="#e7e5e4" strokeWidth="0.5" opacity="0.5" />
            </pattern>
          </defs>
          <rect x={PADDING} y={PADDING} width={(terrain?.width || 15) * SCALE} height={(terrain?.height || 25) * SCALE} fill="url(#grid)" />

          {/* Terrain boundary */}
          <rect
            x={PADDING}
            y={PADDING}
            width={(terrain?.width || 15) * SCALE}
            height={(terrain?.height || 25) * SCALE}
            fill="none"
            stroke="#78716c"
            strokeWidth="2"
            strokeDasharray="8 4"
          />

          {/* Terrain label */}
          <text x={PADDING + 4} y={PADDING - 8} fontSize="10" fill="#78716c" fontFamily="var(--font-mono)">
            {terrain?.width} x {terrain?.height} m
          </text>

          {/* Rooms */}
          {rooms.map((room, i) => {
            const color = ROOM_COLORS[room.type] || DEFAULT_COLOR;
            const rx = toX(room.x);
            const ry = toY(room.y + room.height);
            const rw = room.width * SCALE;
            const rh = room.height * SCALE;
            const area = (room.width * room.height).toFixed(1);

            return (
              <g key={i}>
                <rect
                  x={rx}
                  y={ry}
                  width={rw}
                  height={rh}
                  fill={color.fill}
                  stroke={color.stroke}
                  strokeWidth="1.5"
                  rx="2"
                />
                {/* Room name */}
                <text
                  x={rx + rw / 2}
                  y={ry + rh / 2 - 4}
                  textAnchor="middle"
                  fontSize={Math.min(11, rw / 6)}
                  fontWeight="600"
                  fill={color.text}
                >
                  {room.name}
                </text>
                {/* Room area */}
                <text
                  x={rx + rw / 2}
                  y={ry + rh / 2 + 10}
                  textAnchor="middle"
                  fontSize={Math.min(9, rw / 8)}
                  fill={color.text}
                  opacity="0.7"
                >
                  {area} m&sup2;
                </text>
              </g>
            );
          })}

          {/* Walls between rooms (simplified: draw thicker borders on overlapping edges) */}
          {rooms.map((room, i) => {
            const rx = toX(room.x);
            const ry = toY(room.y + room.height);
            const rw = room.width * SCALE;
            const rh = room.height * SCALE;
            return (
              <rect
                key={`wall-${i}`}
                x={rx - (wallThickness * SCALE) / 2}
                y={ry - (wallThickness * SCALE) / 2}
                width={rw + wallThickness * SCALE}
                height={rh + wallThickness * SCALE}
                fill="none"
                stroke="#44403c"
                strokeWidth={wallThickness * SCALE}
                rx="1"
                opacity="0.15"
              />
            );
          })}

          {/* Doors */}
          {doors.map((door, i) => {
            const dx = toX(door.x || 0);
            const dy = toY(door.y || 0);
            const dw = (door.width || 0.9) * SCALE;
            const isHorizontal = door.orientation === "horizontal";

            return (
              <g key={`door-${i}`}>
                {isHorizontal ? (
                  <>
                    <line x1={dx} y1={dy} x2={dx + dw} y2={dy} stroke="#b45309" strokeWidth="3" strokeLinecap="round" />
                    <path d={`M ${dx} ${dy} A ${dw} ${dw} 0 0 1 ${dx + dw} ${dy}`} fill="none" stroke="#b45309" strokeWidth="1" opacity="0.4" />
                  </>
                ) : (
                  <>
                    <line x1={dx} y1={dy} x2={dx} y2={dy - dw} stroke="#b45309" strokeWidth="3" strokeLinecap="round" />
                    <path d={`M ${dx} ${dy} A ${dw} ${dw} 0 0 0 ${dx} ${dy - dw}`} fill="none" stroke="#b45309" strokeWidth="1" opacity="0.4" />
                  </>
                )}
              </g>
            );
          })}

          {/* Windows */}
          {windows.map((win, i) => {
            const wx = toX(win.x || 0);
            const wy = toY(win.y || 0);
            const ww = (win.width || 1.2) * SCALE;
            const isHorizontal = win.orientation === "horizontal";

            return (
              <g key={`win-${i}`}>
                {isHorizontal ? (
                  <>
                    <line x1={wx} y1={wy} x2={wx + ww} y2={wy} stroke="#0ea5e9" strokeWidth="3" strokeLinecap="round" />
                    <line x1={wx + ww * 0.2} y1={wy} x2={wx + ww * 0.8} y2={wy} stroke="#7dd3fc" strokeWidth="5" strokeLinecap="round" opacity="0.5" />
                  </>
                ) : (
                  <>
                    <line x1={wx} y1={wy} x2={wx} y2={wy - ww} stroke="#0ea5e9" strokeWidth="3" strokeLinecap="round" />
                    <line x1={wx} y1={wy - ww * 0.2} x2={wx} y2={wy - ww * 0.8} stroke="#7dd3fc" strokeWidth="5" strokeLinecap="round" opacity="0.5" />
                  </>
                )}
              </g>
            );
          })}

          {/* Scale bar */}
          <g transform={`translate(${PADDING}, ${svgHeight - 15})`}>
            <line x1="0" y1="0" x2={5 * SCALE} y2="0" stroke="#78716c" strokeWidth="2" />
            <line x1="0" y1="-3" x2="0" y2="3" stroke="#78716c" strokeWidth="1.5" />
            <line x1={5 * SCALE} y1="-3" x2={5 * SCALE} y2="3" stroke="#78716c" strokeWidth="1.5" />
            <text x={2.5 * SCALE} y="12" textAnchor="middle" fontSize="9" fill="#78716c">5 m</text>
          </g>
        </svg>

        {/* Legend */}
        <div className="mt-3 flex flex-wrap items-center gap-3 border-t border-border pt-3">
          {legend.map((type) => {
            const color = ROOM_COLORS[type] || DEFAULT_COLOR;
            return (
              <div key={type} className="flex items-center gap-1.5 text-xs">
                <div className="h-3 w-3 rounded-sm border" style={{ backgroundColor: color.fill, borderColor: color.stroke }} />
                <span className="capitalize">{type}</span>
              </div>
            );
          })}
          <div className="flex items-center gap-1.5 text-xs">
            <div className="h-0.5 w-3 rounded bg-[#b45309]" />
            <span>Porte</span>
          </div>
          <div className="flex items-center gap-1.5 text-xs">
            <div className="h-0.5 w-3 rounded bg-[#0ea5e9]" />
            <span>Fenetre</span>
          </div>
        </div>
      </div>

      {/* Metrics */}
      {metrics && (
        <div className="rounded-2xl border border-border bg-surface p-5">
          <h3 className="mb-3 text-sm font-semibold tracking-wide uppercase text-muted">Metriques</h3>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            <div className="rounded-xl bg-background p-3">
              <div className="text-xs text-muted">Surface totale</div>
              <div className="mt-1 text-lg font-semibold">{metrics.totalArea?.toFixed(1)} <span className="text-sm text-muted">m&sup2;</span></div>
            </div>
            <div className="rounded-xl bg-background p-3">
              <div className="text-xs text-muted">COS</div>
              <div className="mt-1 text-lg font-semibold">{metrics.cos?.toFixed(2)}</div>
            </div>
            <div className="rounded-xl bg-background p-3">
              <div className="text-xs text-muted">CUF</div>
              <div className="mt-1 text-lg font-semibold">{metrics.cuf?.toFixed(2)}</div>
            </div>
            <div className="rounded-xl bg-background p-3">
              <div className="text-xs text-muted">Conformite</div>
              <div className={`mt-1 text-lg font-semibold ${metrics.regulationsCompliant ? "text-success" : "text-danger"}`}>
                {metrics.regulationsCompliant ? "Conforme" : "Non conforme"}
              </div>
            </div>
          </div>
          {metrics.complianceMessage && (
            <div className="mt-3 rounded-lg bg-warning/10 px-3 py-2 text-xs text-warning">
              {metrics.complianceMessage}
            </div>
          )}
        </div>
      )}

      {/* Actions */}
      <div className="flex gap-3">
        <button
          type="button"
          onClick={onExportDxf}
          disabled={isExporting}
          className="flex flex-1 items-center justify-center gap-2 rounded-xl border border-border bg-surface py-3 text-sm font-medium transition-colors hover:bg-surface-hover disabled:opacity-50"
        >
          {isExporting ? (
            <svg className="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" /><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" /></svg>
          ) : (
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
          )}
          Exporter DXF
        </button>
        <button
          type="button"
          onClick={onAnalyze}
          disabled={isAnalyzing}
          className="flex flex-1 items-center justify-center gap-2 rounded-xl border border-border bg-surface py-3 text-sm font-medium transition-colors hover:bg-surface-hover disabled:opacity-50"
        >
          {isAnalyzing ? (
            <svg className="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" /><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" /></svg>
          ) : (
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M22 12h-4l-3 9L9 3l-3 9H2"/></svg>
          )}
          Analyse structurelle
        </button>
      </div>
    </div>
  );
}
