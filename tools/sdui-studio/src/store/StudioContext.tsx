import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import type { StudioNode, UiActionDef } from "../catalog/types";
import { createNodeFromCatalog } from "../lib/exportUiTree";
import { newInstanceId } from "../lib/ids";
import {
  appendChildNode,
  findNode,
  insertChildNode,
  moveChildByIdInTree,
  removeNode,
  resolveAddParentId,
  updateNode,
} from "../lib/studioTree";
import { buildTemplate, emptyRoot } from "../lib/templates";

interface StudioContextValue {
  root: StudioNode;
  selectedId: string | null;
  selectNode: (id: string | null) => void;
  addChild: (parentId: string, catalogId: string) => void;
  insertChild: (parentId: string, catalogId: string, index: number) => void;
  addToRoot: (catalogId: string) => void;
  /** Siempre añade al final del Column raíz del canvas. */
  appendToCanvasEnd: (catalogId: string) => void;
  updateProps: (instanceId: string, props: Record<string, string>) => void;
  updateActions: (instanceId: string, actions: UiActionDef[]) => void;
  removeSelected: () => void;
  moveChild: (parentId: string, fromIndex: number, toIndex: number) => void;
  moveChildById: (parentId: string, activeId: string, overId: string) => void;
  loadTemplate: (templateId: string) => void;
  resetCanvas: () => void;
  selectedNode: StudioNode | null;
  inspectorOpen: boolean;
  openInspector: () => void;
  closeInspector: () => void;
}

const StudioContext = createContext<StudioContextValue | null>(null);

export function StudioProvider({ children }: { children: ReactNode }) {
  const [root, setRoot] = useState<StudioNode>(() => emptyRoot());
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [inspectorOpen, setInspectorOpen] = useState(false);

  const selectNode = useCallback((id: string | null) => {
    setSelectedId(id);
    setInspectorOpen(false);
  }, []);

  const openInspector = useCallback(() => setInspectorOpen(true), []);
  const closeInspector = useCallback(() => setInspectorOpen(false), []);

  const selectedNode = useMemo(() => {
    if (!selectedId) return null;
    return findNode(root, selectedId)?.node ?? null;
  }, [root, selectedId]);

  const addChild = useCallback((parentId: string, catalogId: string) => {
    const child = createNodeFromCatalog(catalogId);
    if (!child) return;
    child.instanceId = newInstanceId(catalogId);
    setRoot((r) => appendChildNode(r, parentId, child));
    setSelectedId(child.instanceId);
    setInspectorOpen(false);
  }, []);

  const insertChild = useCallback((parentId: string, catalogId: string, index: number) => {
    const child = createNodeFromCatalog(catalogId);
    if (!child) return;
    child.instanceId = newInstanceId(catalogId);
    setRoot((r) => insertChildNode(r, parentId, child, index));
    setSelectedId(child.instanceId);
    setInspectorOpen(false);
  }, []);

  const addToRoot = useCallback(
    (catalogId: string) => {
      const child = createNodeFromCatalog(catalogId);
      if (!child) return;
      child.instanceId = newInstanceId(catalogId);
      setRoot((r) => appendChildNode(r, resolveAddParentId(r, selectedId), child));
      setSelectedId(child.instanceId);
      setInspectorOpen(false);
    },
    [selectedId],
  );

  const appendToCanvasEnd = useCallback((catalogId: string) => {
    const child = createNodeFromCatalog(catalogId);
    if (!child) return;
    child.instanceId = newInstanceId(catalogId);
    setRoot((r) => appendChildNode(r, r.instanceId, child));
    setSelectedId(child.instanceId);
    setInspectorOpen(false);
  }, []);

  const updateProps = useCallback((instanceId: string, props: Record<string, string>) => {
    setRoot((r) =>
      updateNode(r, instanceId, (n) => ({
        ...n,
        props: { ...n.props, ...props },
      })),
    );
  }, []);

  const updateActions = useCallback((instanceId: string, actions: UiActionDef[]) => {
    setRoot((r) =>
      updateNode(r, instanceId, (n) => ({
        ...n,
        actions,
      })),
    );
  }, []);

  const removeSelected = useCallback(() => {
    if (!selectedId || selectedId === root.instanceId) return;
    setRoot((r) => removeNode(r, selectedId));
    setSelectedId(null);
    setInspectorOpen(false);
  }, [selectedId, root.instanceId]);

  const moveChild = useCallback((parentId: string, fromIndex: number, toIndex: number) => {
    setRoot((r) =>
      updateNode(r, parentId, (n) => {
        const next = [...n.children];
        const [item] = next.splice(fromIndex, 1);
        next.splice(toIndex, 0, item);
        return { ...n, children: next };
      }),
    );
  }, []);

  const moveChildById = useCallback((parentId: string, activeId: string, overId: string) => {
    setRoot((r) => moveChildByIdInTree(r, parentId, activeId, overId));
  }, []);

  const loadTemplate = useCallback((templateId: string) => {
    const t = buildTemplate(templateId);
    if (t) {
      setRoot(t);
      setSelectedId(t.instanceId);
      setInspectorOpen(false);
    }
  }, []);

  const resetCanvas = useCallback(() => {
    const r = emptyRoot();
    setRoot(r);
    setSelectedId(r.instanceId);
    setInspectorOpen(false);
  }, []);

  const value: StudioContextValue = {
    root,
    selectedId,
    selectNode,
    addChild,
    insertChild,
    addToRoot,
    appendToCanvasEnd,
    updateProps,
    updateActions,
    removeSelected,
    moveChild,
    moveChildById,
    loadTemplate,
    resetCanvas,
    selectedNode,
    inspectorOpen,
    openInspector,
    closeInspector,
  };

  return <StudioContext.Provider value={value}>{children}</StudioContext.Provider>;
}

export function useStudio(): StudioContextValue {
  const ctx = useContext(StudioContext);
  if (!ctx) throw new Error("useStudio outside provider");
  return ctx;
}
