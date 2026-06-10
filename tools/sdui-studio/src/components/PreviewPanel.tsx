import { PreviewRenderer } from "../preview/PreviewRenderer";
import { useStudio } from "../store/StudioContext";

export function PreviewPanel() {
  const { root } = useStudio();

  return (
    <section className="panel preview-panel">
      <h2>Preview mobile</h2>
      <p className="panel-hint">Vista en vivo del diseño (320px).</p>
      <div className="phone-frame">
        <div className="phone-screen chat-bg">
          <PreviewRenderer node={root} />
        </div>
      </div>
    </section>
  );
}
