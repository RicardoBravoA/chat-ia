import { catalog } from "../catalog";
import { useStudio } from "../store/StudioContext";

export function Toolbar() {
  const { loadTemplate, resetCanvas } = useStudio();

  return (
    <header className="toolbar">
      <div className="toolbar-brand">
        <h1>SDUI Studio</h1>
        <span className="toolbar-sub">Banking App — catálogo mobile + export uiTree</span>
      </div>
      <div className="toolbar-actions">
        <label className="template-select">
          Plantilla
          <select
            defaultValue=""
            onChange={(e) => {
              if (e.target.value) loadTemplate(e.target.value);
              e.target.value = "";
            }}
          >
            <option value="">Cargar plantilla…</option>
            {catalog.templates.map((t) => (
              <option key={t.id} value={t.id}>
                {t.label} — {t.description}
              </option>
            ))}
          </select>
        </label>
        <button type="button" className="btn-secondary" onClick={resetCanvas}>
          Canvas vacío
        </button>
      </div>
    </header>
  );
}
