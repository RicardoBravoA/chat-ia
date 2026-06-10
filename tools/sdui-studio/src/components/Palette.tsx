import { useDraggable } from "@dnd-kit/core";
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

function PaletteItem({ component }: { component: CatalogComponent }) {
  const { addToRoot, appendToCanvasEnd } = useStudio();
  const { attributes, listeners, setNodeRef, isDragging } = useDraggable({
    id: `palette-${component.id}`,
    data: { type: "palette", catalogId: component.id },
  });

  return (
    <div
      ref={setNodeRef}
      className={`palette-card ${isDragging ? "dragging" : ""}`}
    >
      <div className="palette-card-preview chat-bg">
        <ComponentPreview catalogId={component.id} variant="thumb" />
      </div>
      <div className="palette-card-actions">
        <button
          type="button"
          className="palette-item"
          onClick={() => addToRoot(component.id)}
          title="Añadir al Column/Row seleccionado (o al canvas raíz)"
        >
          <span className="palette-item-label">{component.label}</span>
          <span className={`badge ${component.sduiExportable ? "badge-sdui" : "badge-app"}`}>
            {component.sduiExportable ? "SDUI" : "App"}
          </span>
        </button>
        <button
          type="button"
          className="palette-add-end"
          onClick={() => appendToCanvasEnd(component.id)}
          title="Añadir al final del canvas"
          aria-label={`Añadir ${component.label} al final del canvas`}
        >
          +
        </button>
        <button
          type="button"
          className="palette-drag-handle"
          {...listeners}
          {...attributes}
          title="Arrastrar al canvas"
          aria-label={`Arrastrar ${component.label}`}
        >
          ⠿
        </button>
      </div>
    </div>
  );
}

export function Palette() {
  const grouped = LEVEL_ORDER.map((level) => ({
    level,
    items: catalog.components.filter((c) => c.atomicLevel === level),
  }));

  return (
    <div className="palette-content">
      <p className="panel-hint">
        Mini preview · <strong>clic</strong> al contenedor seleccionado · <strong>+</strong> al final del canvas · arrastra ⠿ al área punteada.
      </p>
      {grouped.map(({ level, items }) => (
        <section key={level} className="palette-section">
          <h3>{LEVEL_LABEL[level]}</h3>
          <div className="palette-grid">
            {items.map((c) => (
              <PaletteItem key={c.id} component={c} />
            ))}
          </div>
        </section>
      ))}
    </div>
  );
}
