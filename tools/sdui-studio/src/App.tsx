import { DesignWorkspace } from "./components/DesignWorkspace";
import { ExportPanel } from "./components/ExportPanel";
import { InspectorDrawer } from "./components/Inspector";
import { MobileGuide } from "./components/MobileGuide";
import { PaletteSidebar } from "./components/PaletteSidebar";
import { StudioDndProvider } from "./components/StudioDndProvider";
import { Toolbar } from "./components/Toolbar";
import { StudioProvider } from "./store/StudioContext";

export default function App() {
  return (
    <StudioProvider>
      <div className="app">
        <Toolbar />
        <StudioDndProvider>
          <div className="workspace">
            <PaletteSidebar />
            <DesignWorkspace />
          </div>
        </StudioDndProvider>
        <InspectorDrawer />
        <div className="workspace-bottom">
          <ExportPanel />
          <MobileGuide />
        </div>
      </div>
    </StudioProvider>
  );
}
