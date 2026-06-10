import { useMemo, useState } from "react";
import { buildExportPayload } from "../lib/exportUiTree";
import { useStudio } from "../store/StudioContext";

type ExportTab = "full" | "tree" | "canvas";

export function ExportPanel() {
  const { root } = useStudio();
  const [copied, setCopied] = useState(false);
  const [tab, setTab] = useState<ExportTab>("full");

  const exportData = useMemo(() => buildExportPayload(root), [root]);

  const jsonText = useMemo(() => {
    if (tab === "full") return JSON.stringify(exportData.full, null, 2);
    if (tab === "tree") return JSON.stringify(exportData.uiTreeOnly, null, 2);
    return JSON.stringify(exportData.studioCanvas, null, 2);
  }, [exportData, tab]);

  const copy = async () => {
    await navigator.clipboard.writeText(jsonText);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const canvasNodeCount = useMemo(() => {
    const count = (n: typeof exportData.studioCanvas): number =>
      1 + n.children.reduce((sum, c) => sum + count(c), 0);
    return count(exportData.studioCanvas);
  }, [exportData.studioCanvas]);

  const sduiNodeCount = useMemo(() => {
    const count = (n: NonNullable<typeof exportData.uiTreeOnly>): number =>
      1 + n.children.reduce((sum, c) => sum + count(c), 0);
    return exportData.uiTreeOnly ? count(exportData.uiTreeOnly) : 0;
  }, [exportData.uiTreeOnly]);

  return (
    <section className="export-panel">
      <div className="export-header">
        <h2>Export SDUI</h2>
        <div className="tab-row">
          <button
            type="button"
            className={tab === "full" ? "tab active" : "tab"}
            onClick={() => setTab("full")}
          >
            ChatMessageResponse
          </button>
          <button
            type="button"
            className={tab === "tree" ? "tab active" : "tab"}
            onClick={() => setTab("tree")}
          >
            solo uiTree
          </button>
          <button
            type="button"
            className={tab === "canvas" ? "tab active" : "tab"}
            onClick={() => setTab("canvas")}
          >
            Canvas completo
          </button>
          <button type="button" className="btn-primary" onClick={copy}>
            {copied ? "Copiado ✓" : "Copiar JSON"}
          </button>
        </div>
      </div>

      <p className="export-summary panel-hint">
        Canvas: <strong>{canvasNodeCount}</strong> nodos · uiTree mobile:{" "}
        <strong>{sduiNodeCount}</strong> nodos · intent inferido:{" "}
        <strong>{exportData.full.metadata.intent}</strong>
      </p>

      {exportData.skippedCatalogIds.length > 0 && (
        <div className="export-warning">
          <strong>Fuera de uiTree (App / LoginScreen, no chat SDUI):</strong>{" "}
          {exportData.skippedCatalogIds.join(", ")}. Están en <code>studioCanvas</code> — selecciónalos
          en el canvas para ver el snippet Kotlin en <strong>Render en mobile</strong>. En mobile
          real el chat usa solo badge <strong>SDUI</strong> (p. ej. PayCardPanel).
        </div>
      )}

      {!exportData.uiTreeOnly && (
        <div className="export-warning">
          El canvas no tiene nodos SDUI exportables a mobile. Revisa la pestaña{" "}
          <strong>Canvas completo</strong>.
        </div>
      )}

      <pre className="json-output">{jsonText}</pre>
    </section>
  );
}
