import { getComponent } from "../catalog";
import type { UiActionDef } from "../catalog/types";
import { useStudio } from "../store/StudioContext";

export function InspectorDrawer() {
  const { selectedNode, inspectorOpen, closeInspector, updateProps, updateActions } =
    useStudio();

  if (!inspectorOpen || !selectedNode) return null;

  const def = getComponent(selectedNode.catalogId);
  if (!def) return null;

  const handlePropChange = (key: string, value: string) => {
    updateProps(selectedNode.instanceId, { [key]: value });
  };

  const handleActionChange = (index: number, field: keyof UiActionDef, value: string) => {
    const next = selectedNode.actions.map((a, i) => {
      if (i !== index) return a;
      if (field === "payload") {
        return { ...a, payload: { ...a.payload, intent: value } };
      }
      return { ...a, [field]: value };
    });
    updateActions(selectedNode.instanceId, next);
  };

  return (
    <>
      <button
        type="button"
        className="inspector-backdrop"
        aria-label="Cerrar inspector"
        onClick={closeInspector}
      />
      <aside className="inspector-drawer" role="dialog" aria-labelledby="inspector-title">
        <header className="inspector-drawer-header">
          <h2 id="inspector-title">Editar contenido</h2>
          <button type="button" className="btn-icon inspector-close" onClick={closeInspector}>
            ×
          </button>
        </header>

        <div className="inspector-drawer-body">
          <div className="inspector-meta">
            <strong>{def.label}</strong>
            <span className={`badge ${def.sduiExportable ? "badge-sdui" : "badge-app"}`}>
              {def.sduiExportable ? `SDUI: ${def.sduiType}` : "Solo app"}
            </span>
          </div>
          <p className="inspector-desc">{def.description}</p>

          {Object.entries(def.propsSchema).length > 0 && (
            <fieldset className="inspector-fields">
              <legend>Props editables</legend>
              {Object.entries(def.propsSchema).map(([key, schema]) => (
                <label key={key} className="field">
                  <span>{schema.label}</span>
                  {schema.multiline ? (
                    <textarea
                      value={selectedNode.props[key] ?? ""}
                      onChange={(e) => handlePropChange(key, e.target.value)}
                      rows={3}
                    />
                  ) : (
                    <input
                      type="text"
                      value={selectedNode.props[key] ?? ""}
                      onChange={(e) => handlePropChange(key, e.target.value)}
                    />
                  )}
                </label>
              ))}
            </fieldset>
          )}

          {selectedNode.catalogId === "GreetingCard" && selectedNode.actions.length > 0 && (
            <fieldset className="inspector-fields">
              <legend>Quick replies (actions)</legend>
              {selectedNode.actions.map((action, index) => (
                <div key={action.id} className="action-row">
                  <label className="field">
                    <span>Label</span>
                    <input
                      type="text"
                      value={action.label}
                      onChange={(e) => handleActionChange(index, "label", e.target.value)}
                    />
                  </label>
                  <label className="field">
                    <span>Intent</span>
                    <input
                      type="text"
                      value={action.payload.intent ?? ""}
                      onChange={(e) => handleActionChange(index, "payload", e.target.value)}
                    />
                  </label>
                </div>
              ))}
            </fieldset>
          )}
        </div>
      </aside>
    </>
  );
}
