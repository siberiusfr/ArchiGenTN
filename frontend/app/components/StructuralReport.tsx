"use client";

import type { StructuralAnalysisResponse } from "@/src/api/models/structuralAnalysisResponse";

interface StructuralReportProps {
  analysis: StructuralAnalysisResponse;
  onClose: () => void;
}

export default function StructuralReport({ analysis, onClose }: StructuralReportProps) {
  return (
    <div className="rounded-2xl border border-border bg-surface p-5">
      <div className="mb-4 flex items-center justify-between">
        <h3 className="text-sm font-semibold tracking-wide uppercase text-muted">
          Rapport structurel
        </h3>
        <button
          onClick={onClose}
          className="flex h-7 w-7 items-center justify-center rounded-lg text-muted hover:bg-surface-hover hover:text-foreground"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>

      {/* Global status */}
      <div className={`mb-4 flex items-center gap-3 rounded-xl p-4 ${analysis.globalValid ? "bg-success/10" : "bg-danger/10"}`}>
        <div className={`flex h-10 w-10 items-center justify-center rounded-full ${analysis.globalValid ? "bg-success/20 text-success" : "bg-danger/20 text-danger"}`}>
          {analysis.globalValid ? (
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
          ) : (
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          )}
        </div>
        <div>
          <div className={`font-semibold ${analysis.globalValid ? "text-success" : "text-danger"}`}>
            {analysis.globalValid ? "Structure conforme" : "Structure non conforme"}
          </div>
          <div className="text-xs text-muted">
            Type : {analysis.buildingType} &middot; Charge totale : {analysis.chargeTotale?.toFixed(1)} kN/m&sup2;
          </div>
        </div>
      </div>

      {analysis.summary && (
        <p className="mb-4 text-sm text-muted">{analysis.summary}</p>
      )}

      {/* Elements table */}
      {analysis.elements && analysis.elements.length > 0 && (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-border text-xs text-muted">
                <th className="pb-2 pr-3 font-medium">Element</th>
                <th className="pb-2 pr-3 font-medium">Portee</th>
                <th className="pb-2 pr-3 font-medium">Moment</th>
                <th className="pb-2 pr-3 font-medium">Fleche</th>
                <th className="pb-2 pr-3 font-medium">Hauteur rec.</th>
                <th className="pb-2 font-medium">Statut</th>
              </tr>
            </thead>
            <tbody>
              {analysis.elements.map((el, i) => (
                <tr key={i} className="border-b border-border/50 last:border-none">
                  <td className="py-2.5 pr-3">
                    <div className="font-medium">{el.elementId}</div>
                    <div className="text-xs text-muted">{el.elementType}</div>
                  </td>
                  <td className="py-2.5 pr-3">{el.portee?.toFixed(1)} m</td>
                  <td className="py-2.5 pr-3">{el.momentMax?.toFixed(1)} kN.m</td>
                  <td className="py-2.5 pr-3">
                    <span>{el.flecheMax?.toFixed(1)} mm</span>
                    <span className="text-xs text-muted"> / {el.flecheAdmissible?.toFixed(1)}</span>
                  </td>
                  <td className="py-2.5 pr-3">{el.hauteurRecommandee?.toFixed(2)} m</td>
                  <td className="py-2.5">
                    <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${el.valid ? "bg-success/10 text-success" : "bg-danger/10 text-danger"}`}>
                      {el.valid ? "OK" : "KO"}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
