import { useState } from "react";
import { ComponentGallery } from "./ComponentGallery";
import { Palette } from "./Palette";

type SidebarTab = "palette" | "gallery";

export function PaletteSidebar() {
  const [tab, setTab] = useState<SidebarTab>("palette");

  return (
    <aside className="panel palette-sidebar">
      <div className="sidebar-tabs">
        <button
          type="button"
          className={tab === "palette" ? "sidebar-tab active" : "sidebar-tab"}
          onClick={() => setTab("palette")}
        >
          Paleta
        </button>
        <button
          type="button"
          className={tab === "gallery" ? "sidebar-tab active" : "sidebar-tab"}
          onClick={() => setTab("gallery")}
        >
          Galería
        </button>
      </div>
      {tab === "palette" ? <Palette /> : <ComponentGallery />}
    </aside>
  );
}
