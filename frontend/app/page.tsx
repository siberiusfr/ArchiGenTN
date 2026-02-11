"use client";

import { useState, useCallback } from "react";
import PlanForm from "./components/PlanForm";
import PlanViewer from "./components/PlanViewer";
import StructuralReport from "./components/StructuralReport";
import type { PlanGenerateRequest } from "@/src/api/models/planGenerateRequest";
import type { PlanResponse } from "@/src/api/models/planResponse";
import type { StructuralAnalysisResponse } from "@/src/api/models/structuralAnalysisResponse";
import { generatePlan } from "@/src/api/endpoints/plans/plans";
import { exportDxf, analyzePlan } from "@/src/api/endpoints/plans/plans";
import { generatePlan1 } from "@/src/api/endpoints/plans-ia/plans-ia";

export default function Home() {
  const [plan, setPlan] = useState<PlanResponse | null>(null);
  const [analysis, setAnalysis] = useState<StructuralAnalysisResponse | null>(null);
  const [isGenerating, setIsGenerating] = useState(false);
  const [isExporting, setIsExporting] = useState(false);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [lastRequest, setLastRequest] = useState<PlanGenerateRequest | null>(null);

  const handleGenerate = useCallback(async (request: PlanGenerateRequest, useAI: boolean) => {
    setIsGenerating(true);
    setError(null);
    setAnalysis(null);
    setLastRequest(request);

    try {
      const response = useAI
        ? await generatePlan1(request)
        : await generatePlan(request);

      if (response.status === 200) {
        setPlan(response.data as unknown as PlanResponse);
      } else {
        const errData = response.data as unknown as PlanResponse;
        setError(errData?.metrics?.complianceMessage || "Erreur lors de la generation du plan.");
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Erreur de connexion au serveur.");
    } finally {
      setIsGenerating(false);
    }
  }, []);

  const handleExportDxf = useCallback(async () => {
    if (!plan) return;
    setIsExporting(true);

    try {
      const response = await exportDxf(plan);
      if (response.status === 200) {
        const blob = new Blob([JSON.stringify(response.data)], { type: "application/dxf" });
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = "plan-archigentn.dxf";
        a.click();
        URL.revokeObjectURL(url);
      }
    } catch {
      setError("Erreur lors de l'export DXF.");
    } finally {
      setIsExporting(false);
    }
  }, [plan]);

  const handleAnalyze = useCallback(async () => {
    if (!plan) return;
    setIsAnalyzing(true);

    try {
      const response = await analyzePlan(plan, { buildingType: "habitation" as never });
      if (response.status === 200) {
        setAnalysis(response.data as unknown as StructuralAnalysisResponse);
      }
    } catch {
      setError("Erreur lors de l'analyse structurelle.");
    } finally {
      setIsAnalyzing(false);
    }
  }, [plan]);

  return (
    <div className="flex min-h-screen flex-col">
      {/* Header */}
      <header className="sticky top-0 z-50 border-b border-border bg-surface/80 backdrop-blur-lg">
        <div className="mx-auto flex h-14 max-w-[1600px] items-center gap-4 px-6">
          <div className="flex items-center gap-2.5">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-accent text-white">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M3 21h18"/>
                <path d="M5 21V7l7-4 7 4v14"/>
                <path d="M9 21v-6h6v6"/>
              </svg>
            </div>
            <span className="text-lg font-bold tracking-tight">
              Archi<span className="text-accent">Gen</span><span className="text-muted">TN</span>
            </span>
          </div>
          <div className="hidden text-sm text-muted sm:block">
            Generateur de plans architecturaux
          </div>
          {lastRequest && (
            <div className="ml-auto flex items-center gap-2 text-xs text-muted">
              <div className="h-1.5 w-1.5 rounded-full bg-success" />
              Plan genere
            </div>
          )}
        </div>
      </header>

      {/* Main */}
      <main className="mx-auto flex w-full max-w-[1600px] flex-1 flex-col lg:flex-row">
        {/* Left panel: Form */}
        <aside className="w-full shrink-0 overflow-y-auto border-r border-border p-6 lg:w-[440px] xl:w-[480px]">
          <PlanForm onSubmit={handleGenerate} isLoading={isGenerating} />
        </aside>

        {/* Right panel: Viewer */}
        <section className="flex-1 overflow-y-auto p-6">
          {error && (
            <div className="mb-4 flex items-center gap-3 rounded-2xl border border-danger/20 bg-danger/5 p-4 text-sm text-danger">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
              </svg>
              {error}
              <button onClick={() => setError(null)} className="ml-auto text-danger/60 hover:text-danger">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
          )}

          {plan ? (
            <div className="flex flex-col gap-4">
              <PlanViewer
                plan={plan}
                onExportDxf={handleExportDxf}
                onAnalyze={handleAnalyze}
                isExporting={isExporting}
                isAnalyzing={isAnalyzing}
              />
              {analysis && (
                <StructuralReport
                  analysis={analysis}
                  onClose={() => setAnalysis(null)}
                />
              )}
            </div>
          ) : (
            <div className="flex h-full min-h-[400px] flex-col items-center justify-center text-center">
              <div className="mb-6 flex h-20 w-20 items-center justify-center rounded-3xl bg-accent/5">
                <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" className="text-accent/40">
                  <rect x="3" y="3" width="7" height="7"/>
                  <rect x="14" y="3" width="7" height="7"/>
                  <rect x="3" y="14" width="7" height="7"/>
                  <rect x="14" y="14" width="7" height="7"/>
                </svg>
              </div>
              <h2 className="mb-2 text-lg font-semibold">Aucun plan genere</h2>
              <p className="max-w-sm text-sm text-muted">
                Configurez les parametres de votre terrain et vos pieces dans le formulaire, puis cliquez sur &laquo; Generer le plan &raquo;.
              </p>
            </div>
          )}
        </section>
      </main>
    </div>
  );
}
