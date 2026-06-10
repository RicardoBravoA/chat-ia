import { Canvas } from "./Canvas";
import { PreviewPanel } from "./PreviewPanel";

/** Canvas y preview mobile lado a lado. */
export function DesignWorkspace() {
  return (
    <div className="design-workspace">
      <Canvas />
      <PreviewPanel />
    </div>
  );
}
