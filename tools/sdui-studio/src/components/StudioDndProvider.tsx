import {
  DndContext,
  DragOverlay,
  PointerSensor,
  closestCenter,
  pointerWithin,
  useSensor,
  useSensors,
  type Collision,
  type CollisionDetection,
  type DragEndEvent,
  type DragStartEvent,
} from "@dnd-kit/core";
import { useState, type ReactNode } from "react";
import { getComponent } from "../catalog";
import { useStudio } from "../store/StudioContext";

function isCanvasSortableType(type: unknown): boolean {
  return type === "canvas" || type === "root-canvas";
}

function pickPaletteDropTarget(hits: Collision[]): Collision | null {
  if (hits.length === 0) return null;

  const containerDrops = hits.filter((h) => h.data?.current?.type === "container-drop");
  if (containerDrops.length > 0) {
    return containerDrops.sort((a, b) => {
      const depthA = (a.data?.current?.depth as number) ?? 0;
      const depthB = (b.data?.current?.depth as number) ?? 0;
      return depthB - depthA;
    })[0];
  }

  const rootCanvas = hits.find((h) => h.data?.current?.type === "root-canvas");
  if (rootCanvas) return rootCanvas;

  return hits[0];
}

const collisionDetection: CollisionDetection = (args) => {
  const activeType = args.active.data.current?.type;

  if (activeType === "canvas" || activeType === "root-canvas") {
    const sortables = args.droppableContainers.filter((c) =>
      isCanvasSortableType(c.data.current?.type),
    );
    if (sortables.length === 0) return [];
    return closestCenter({ ...args, droppableContainers: sortables });
  }

  if (activeType === "palette") {
    const paletteTargets = args.droppableContainers.filter((c) => {
      const type = c.data.current?.type;
      return type === "container-drop" || type === "root-canvas";
    });
    if (paletteTargets.length === 0) return [];
    const scoped = { ...args, droppableContainers: paletteTargets };
    const hits = pointerWithin(scoped);
    const best = pickPaletteDropTarget(hits);
    if (best) return [best];
    return closestCenter(scoped);
  }

  return closestCenter(args);
};

export function StudioDndProvider({ children }: { children: ReactNode }) {
  const { addChild, insertChild, moveChildById } = useStudio();
  const [overlayLabel, setOverlayLabel] = useState<string | null>(null);

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 5 } }),
  );

  const handleDragStart = (event: DragStartEvent) => {
    const data = event.active.data.current;
    if (data?.type === "palette") {
      const catalogId = data.catalogId as string;
      setOverlayLabel(getComponent(catalogId)?.label ?? catalogId);
      return;
    }
    if (data?.type === "canvas" || data?.type === "root-canvas") {
      const catalogId = data.catalogId as string | undefined;
      setOverlayLabel(getComponent(catalogId ?? "")?.label ?? "Mover");
    }
  };

  const handleDragEnd = (event: DragEndEvent) => {
    setOverlayLabel(null);
    const { active, over } = event;
    if (!over) return;

    const activeData = active.data.current;
    const overData = over.data.current;
    const overId = String(over.id);
    const activeId = String(active.id);

    if (activeData?.type === "palette") {
      const catalogId = activeData.catalogId as string;

      if (overData?.type === "container-drop") {
        addChild(overData.parentId as string, catalogId);
        return;
      }

      if (overData?.type === "root-canvas") {
        const parentId = overData.parentId as string;
        const index = overData.index as number;
        insertChild(parentId, catalogId, index);
      }
      return;
    }

    if (isCanvasSortableType(activeData?.type) && isCanvasSortableType(overData?.type)) {
      const parentId = activeData!.parentId as string;
      if (parentId === overData!.parentId) {
        moveChildById(parentId, activeId, overId);
      }
    }
  };

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={collisionDetection}
      onDragStart={handleDragStart}
      onDragEnd={handleDragEnd}
    >
      {children}
      <DragOverlay dropAnimation={null}>
        {overlayLabel ? <div className="drag-overlay">{overlayLabel}</div> : null}
      </DragOverlay>
    </DndContext>
  );
}
