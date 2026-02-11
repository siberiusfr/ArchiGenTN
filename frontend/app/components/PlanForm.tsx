"use client";

import { useState } from "react";
import type { PlanGenerateRequest } from "@/src/api/models/planGenerateRequest";
import type { RoomRequirementDto } from "@/src/api/models/roomRequirementDto";
import { RoomRequirementDtoType } from "@/src/api/models/roomRequirementDtoType";
import { RequirementsDtoStyle } from "@/src/api/models/requirementsDtoStyle";

const ROOM_TYPE_LABELS: Record<string, string> = {
  salon: "Salon",
  chambre: "Chambre",
  cuisine: "Cuisine",
  sdb: "Salle de bain",
  wc: "WC",
  entree: "Entree",
  couloir: "Couloir",
  bureau: "Bureau",
  garage: "Garage",
};

const ROOM_TYPE_ICONS: Record<string, string> = {
  salon: "M",
  chambre: "B",
  cuisine: "K",
  sdb: "S",
  wc: "W",
  entree: "E",
  couloir: "C",
  bureau: "O",
  garage: "G",
};

const ROOM_DEFAULT_AREAS: Record<string, number> = {
  salon: 25,
  chambre: 14,
  cuisine: 12,
  sdb: 6,
  wc: 2.5,
  entree: 5,
  couloir: 4,
  bureau: 10,
  garage: 18,
};

const STYLE_LABELS: Record<string, string> = {
  moderne: "Moderne",
  traditionnel: "Traditionnel",
  neo_mauresque: "Neo-Mauresque",
  colonial: "Colonial",
  mediterraneen: "Mediterraneen",
};

const STYLE_DESCRIPTIONS: Record<string, string> = {
  moderne: "Lignes epurees, grandes ouvertures, toits plats",
  traditionnel: "Architecture tunisienne classique, cour interieure",
  neo_mauresque: "Arcs, moucharabiehs, zellige revisites",
  colonial: "Facades symetriques, balcons, moulures",
  mediterraneen: "Murs blancs, tuiles, terrasses ombragees",
};

interface PlanFormProps {
  onSubmit: (request: PlanGenerateRequest, useAI: boolean) => void;
  isLoading: boolean;
}

export default function PlanForm({ onSubmit, isLoading }: PlanFormProps) {
  const [terrainWidth, setTerrainWidth] = useState(15);
  const [terrainHeight, setTerrainHeight] = useState(25);
  const [totalArea, setTotalArea] = useState(120);
  const [floors, setFloors] = useState(1);
  const [style, setStyle] = useState<string>("moderne");
  const [useAI, setUseAI] = useState(false);
  const [showRegulations, setShowRegulations] = useState(false);

  const [rooms, setRooms] = useState<RoomRequirementDto[]>([
    { type: RoomRequirementDtoType.salon, name: "Salon", minArea: 25, count: 1 },
    { type: RoomRequirementDtoType.chambre, name: "Chambre 1", minArea: 14, count: 1 },
    { type: RoomRequirementDtoType.chambre, name: "Chambre 2", minArea: 12, count: 1 },
    { type: RoomRequirementDtoType.cuisine, name: "Cuisine", minArea: 12, count: 1 },
    { type: RoomRequirementDtoType.sdb, name: "Salle de bain", minArea: 6, count: 1 },
    { type: RoomRequirementDtoType.wc, name: "WC", minArea: 2.5, count: 1 },
  ]);

  const [regulations, setRegulations] = useState({
    cos: 0.4,
    cuf: 1.2,
    retraitFrontal: 5,
    retraitLateral: 3,
    retraitArriere: 3,
    hauteurMax: 9,
  });

  const addRoom = () => {
    setRooms([
      ...rooms,
      {
        type: RoomRequirementDtoType.chambre,
        name: `Chambre ${rooms.filter((r) => r.type === "chambre").length + 1}`,
        minArea: 14,
        count: 1,
      },
    ]);
  };

  const removeRoom = (index: number) => {
    if (rooms.length <= 1) return;
    setRooms(rooms.filter((_, i) => i !== index));
  };

  const updateRoom = (index: number, field: keyof RoomRequirementDto, value: string | number) => {
    const updated = [...rooms];
    if (field === "type") {
      const typeValue = value as string;
      updated[index] = {
        ...updated[index],
        type: typeValue as RoomRequirementDto["type"],
        name: ROOM_TYPE_LABELS[typeValue] || typeValue,
        minArea: ROOM_DEFAULT_AREAS[typeValue] ?? updated[index].minArea,
      };
    } else {
      updated[index] = { ...updated[index], [field]: value };
    }
    setRooms(updated);
  };

  const totalRoomArea = rooms.reduce((sum, r) => sum + (r.minArea || 0) * (r.count || 1), 0);
  const terrainArea = terrainWidth * terrainHeight;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const request: PlanGenerateRequest = {
      terrain: { width: terrainWidth, height: terrainHeight },
      requirements: {
        totalArea,
        floors,
        rooms,
        style: style as RequirementsDtoStyle,
      },
      ...(showRegulations ? { regulations } : {}),
    };
    onSubmit(request, useAI);
  };

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-6">
      {/* Terrain */}
      <section className="rounded-2xl border border-border bg-surface p-5">
        <h2 className="mb-4 text-sm font-semibold tracking-wide uppercase text-muted">
          Terrain
        </h2>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="mb-1.5 block text-sm font-medium">Largeur (m)</label>
            <input
              type="number"
              min={5}
              step={0.5}
              value={terrainWidth}
              onChange={(e) => setTerrainWidth(Number(e.target.value))}
              className="w-full rounded-xl border border-border bg-background px-4 py-2.5 text-sm transition-colors focus:border-border-focus focus:outline-none"
            />
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-medium">Profondeur (m)</label>
            <input
              type="number"
              min={5}
              step={0.5}
              value={terrainHeight}
              onChange={(e) => setTerrainHeight(Number(e.target.value))}
              className="w-full rounded-xl border border-border bg-background px-4 py-2.5 text-sm transition-colors focus:border-border-focus focus:outline-none"
            />
          </div>
        </div>
        <div className="mt-3 flex items-center gap-2 text-xs text-muted">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/></svg>
          Surface terrain : <span className="font-semibold text-foreground">{terrainArea} m&sup2;</span>
        </div>
      </section>

      {/* Parametres generaux */}
      <section className="rounded-2xl border border-border bg-surface p-5">
        <h2 className="mb-4 text-sm font-semibold tracking-wide uppercase text-muted">
          Parametres
        </h2>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="mb-1.5 block text-sm font-medium">Surface souhaitee (m&sup2;)</label>
            <input
              type="number"
              min={20}
              step={5}
              value={totalArea}
              onChange={(e) => setTotalArea(Number(e.target.value))}
              className="w-full rounded-xl border border-border bg-background px-4 py-2.5 text-sm transition-colors focus:border-border-focus focus:outline-none"
            />
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-medium">Etages</label>
            <input
              type="number"
              min={1}
              max={4}
              value={floors}
              onChange={(e) => setFloors(Number(e.target.value))}
              className="w-full rounded-xl border border-border bg-background px-4 py-2.5 text-sm transition-colors focus:border-border-focus focus:outline-none"
            />
          </div>
        </div>
      </section>

      {/* Pieces */}
      <section className="rounded-2xl border border-border bg-surface p-5">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-sm font-semibold tracking-wide uppercase text-muted">
            Pieces ({rooms.length})
          </h2>
          <button
            type="button"
            onClick={addRoom}
            className="flex items-center gap-1.5 rounded-lg bg-accent/10 px-3 py-1.5 text-xs font-medium text-accent transition-colors hover:bg-accent/20"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            Ajouter
          </button>
        </div>

        <div className="flex flex-col gap-3">
          {rooms.map((room, i) => (
            <div
              key={i}
              className="group flex items-center gap-3 rounded-xl border border-border bg-background p-3 transition-colors hover:border-border-focus/30"
            >
              <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-accent/10 text-xs font-bold text-accent">
                {ROOM_TYPE_ICONS[room.type] || "?"}
              </div>

              <select
                value={room.type}
                onChange={(e) => updateRoom(i, "type", e.target.value)}
                className="w-28 shrink-0 rounded-lg border border-border bg-surface px-2 py-1.5 text-sm focus:border-border-focus focus:outline-none"
              >
                {Object.values(RoomRequirementDtoType).map((t) => (
                  <option key={t} value={t}>
                    {ROOM_TYPE_LABELS[t]}
                  </option>
                ))}
              </select>

              <input
                type="text"
                value={room.name || ""}
                onChange={(e) => updateRoom(i, "name", e.target.value)}
                placeholder="Nom"
                className="min-w-0 flex-1 rounded-lg border border-border bg-surface px-2 py-1.5 text-sm focus:border-border-focus focus:outline-none"
              />

              <div className="flex items-center gap-1">
                <input
                  type="number"
                  min={1.5}
                  step={0.5}
                  value={room.minArea ?? ""}
                  onChange={(e) => updateRoom(i, "minArea", Number(e.target.value))}
                  className="w-16 rounded-lg border border-border bg-surface px-2 py-1.5 text-right text-sm focus:border-border-focus focus:outline-none"
                />
                <span className="text-xs text-muted">m&sup2;</span>
              </div>

              <button
                type="button"
                onClick={() => removeRoom(i)}
                disabled={rooms.length <= 1}
                className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-muted opacity-0 transition-all hover:bg-danger/10 hover:text-danger group-hover:opacity-100 disabled:invisible"
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
          ))}
        </div>

        <div className="mt-3 flex items-center justify-between text-xs text-muted">
          <span>Surface totale pieces : <span className="font-semibold text-foreground">{totalRoomArea.toFixed(1)} m&sup2;</span></span>
          {totalRoomArea > totalArea && (
            <span className="text-warning">Depasse la surface souhaitee</span>
          )}
        </div>
      </section>

      {/* Style */}
      <section className="rounded-2xl border border-border bg-surface p-5">
        <h2 className="mb-4 text-sm font-semibold tracking-wide uppercase text-muted">
          Style architectural
        </h2>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-3">
          {Object.values(RequirementsDtoStyle).map((s) => (
            <button
              key={s}
              type="button"
              onClick={() => setStyle(s)}
              className={`rounded-xl border px-4 py-3 text-left transition-all ${
                style === s
                  ? "border-accent bg-accent/5 ring-1 ring-accent/30"
                  : "border-border hover:border-border-focus/30 hover:bg-surface-hover"
              }`}
            >
              <div className={`text-sm font-medium ${style === s ? "text-accent" : ""}`}>
                {STYLE_LABELS[s]}
              </div>
              <div className="mt-0.5 text-xs text-muted">{STYLE_DESCRIPTIONS[s]}</div>
            </button>
          ))}
        </div>
      </section>

      {/* Reglementations */}
      <section className="rounded-2xl border border-border bg-surface">
        <button
          type="button"
          onClick={() => setShowRegulations(!showRegulations)}
          className="flex w-full items-center justify-between p-5 text-left"
        >
          <h2 className="text-sm font-semibold tracking-wide uppercase text-muted">
            Reglementations (PAU)
          </h2>
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            className={`text-muted transition-transform ${showRegulations ? "rotate-180" : ""}`}
          >
            <polyline points="6 9 12 15 18 9" />
          </svg>
        </button>

        {showRegulations && (
          <div className="border-t border-border px-5 pb-5 pt-4">
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
              <div>
                <label className="mb-1.5 block text-xs font-medium text-muted">COS max</label>
                <input
                  type="number"
                  min={0.1}
                  max={1}
                  step={0.05}
                  value={regulations.cos}
                  onChange={(e) => setRegulations({ ...regulations, cos: Number(e.target.value) })}
                  className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm focus:border-border-focus focus:outline-none"
                />
              </div>
              <div>
                <label className="mb-1.5 block text-xs font-medium text-muted">CUF max</label>
                <input
                  type="number"
                  min={0.1}
                  max={3}
                  step={0.1}
                  value={regulations.cuf}
                  onChange={(e) => setRegulations({ ...regulations, cuf: Number(e.target.value) })}
                  className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm focus:border-border-focus focus:outline-none"
                />
              </div>
              <div>
                <label className="mb-1.5 block text-xs font-medium text-muted">Hauteur max (m)</label>
                <input
                  type="number"
                  min={3}
                  max={30}
                  step={0.5}
                  value={regulations.hauteurMax}
                  onChange={(e) => setRegulations({ ...regulations, hauteurMax: Number(e.target.value) })}
                  className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm focus:border-border-focus focus:outline-none"
                />
              </div>
              <div>
                <label className="mb-1.5 block text-xs font-medium text-muted">Retrait frontal (m)</label>
                <input
                  type="number"
                  min={0}
                  max={20}
                  step={0.5}
                  value={regulations.retraitFrontal}
                  onChange={(e) => setRegulations({ ...regulations, retraitFrontal: Number(e.target.value) })}
                  className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm focus:border-border-focus focus:outline-none"
                />
              </div>
              <div>
                <label className="mb-1.5 block text-xs font-medium text-muted">Retrait lateral (m)</label>
                <input
                  type="number"
                  min={0}
                  max={20}
                  step={0.5}
                  value={regulations.retraitLateral}
                  onChange={(e) => setRegulations({ ...regulations, retraitLateral: Number(e.target.value) })}
                  className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm focus:border-border-focus focus:outline-none"
                />
              </div>
              <div>
                <label className="mb-1.5 block text-xs font-medium text-muted">Retrait arriere (m)</label>
                <input
                  type="number"
                  min={0}
                  max={20}
                  step={0.5}
                  value={regulations.retraitArriere}
                  onChange={(e) => setRegulations({ ...regulations, retraitArriere: Number(e.target.value) })}
                  className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm focus:border-border-focus focus:outline-none"
                />
              </div>
            </div>
          </div>
        )}
      </section>

      {/* AI Toggle + Submit */}
      <div className="flex flex-col gap-4">
        <label className="flex cursor-pointer items-center gap-3 rounded-2xl border border-border bg-surface p-4 transition-colors hover:bg-surface-hover">
          <div className="relative">
            <input
              type="checkbox"
              checked={useAI}
              onChange={(e) => setUseAI(e.target.checked)}
              className="peer sr-only"
            />
            <div className="h-6 w-11 rounded-full bg-border transition-colors peer-checked:bg-accent" />
            <div className="absolute left-0.5 top-0.5 h-5 w-5 rounded-full bg-white shadow-sm transition-transform peer-checked:translate-x-5" />
          </div>
          <div>
            <div className="text-sm font-medium">Generation IA (Claude)</div>
            <div className="text-xs text-muted">Utilise l&apos;IA pour un plan plus intelligent</div>
          </div>
        </label>

        <button
          type="submit"
          disabled={isLoading}
          className="flex h-12 items-center justify-center gap-2 rounded-2xl bg-accent text-sm font-semibold text-white shadow-lg shadow-accent/20 transition-all hover:brightness-110 disabled:opacity-50 disabled:shadow-none"
        >
          {isLoading ? (
            <>
              <svg className="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
              </svg>
              Generation en cours...
            </>
          ) : (
            <>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
              </svg>
              Generer le plan
            </>
          )}
        </button>
      </div>
    </form>
  );
}
