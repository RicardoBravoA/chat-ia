import { catalog } from "../catalog";
import type { AtomicLevel, CatalogComponent } from "../catalog/types";
import { ComponentPreview } from "../preview/ComponentPreview";
import { useStudio } from "../store/StudioContext";

const LEVEL_ORDER: AtomicLevel[] = ["atom", "molecule", "organism"];
const LEVEL_LABEL: Record<AtomicLevel, string> = {
  atom: "Átomos",
  molecule: "Moléculas",
  organism: "Organismos",
};
const LEVEL_DESC: Record<AtomicLevel, string> = {
  atom: "Layouts (Column, Row), banners y piezas mínimas",
  molecule: "Tarjetas, campos y bloques compuestos",
  organism: "Paneles completos de flujo (login, pago, recibo)",
};

function GalleryCard({ component }: { component: CatalogComponent }) {
  const { addToRoot, appendToCanvasEnd } = useStudio();
  const isRootOnly = component.id === "Column";

  return (
    <article className="gallery-card">
      <div className="gallery-card-preview chat-bg">
        <ComponentPreview catalogId={component.id} variant="full" />
      </div>
      <div className="gallery-card-meta">
        <div className="gallery-card-title">
          <strong>{component.label}</strong>
          <span className={`badge ${component.sduiExportable ? "badge-sdui" : "badge-app"}`}>
            {component.sduiExportable ? "SDUI" : "App"}
          </span>
        </div>
        <p className="gallery-card-desc">{component.description}</p>
        <code className="gallery-card-file">{component.composeFile}</code>
        {!isRootOnly && (
          <div className="gallery-card-actions">
            <button type="button" className="btn-gallery-add" onClick={() => addToRoot(component.id)}>
              Añadir al seleccionado
            </button>
            <button
              type="button"
              className="btn-gallery-add-end"
              onClick={() => appendToCanvasEnd(component.id)}
            >
              + Al final
            </button>
          </div>
        )}
      </div>
    </article>
  );
}

export function ComponentGallery() {
  const grouped = LEVEL_ORDER.map((level) => ({
    level,
    items: catalog.components.filter((c) => c.atomicLevel === level),
  }));

  return (
    <div className="component-gallery">
      <p className="panel-hint">
        Vista previa de todos los componentes mobile por nivel atomic design. Clic en{' '}
        <strong>+ Añadir</strong> para insertar en el canvas.
      </p>
      {grouped.map(({ level, items }) => (
        <section key={level} className="gallery-section">
          <header className="gallery-section-header">
            <h3>{LEVEL_LABEL[level]}</h3>
            <span>{LEVEL_DESC[level]}</span>
            <span className="gallery-count">{items.length}</span>
          </header>
          <div className="gallery-grid">
            {items.map((c) => (
              <GalleryCard key={c.id} component={c} />
            ))}
          </div>
        </section>
      ))}
    </div>
  );
}
