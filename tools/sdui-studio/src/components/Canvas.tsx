import { useDroppable } from "@dnd-kit/core";
import {
  SortableContext,
  horizontalListSortingStrategy,
  useSortable,
  verticalListSortingStrategy,
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { getComponent } from "../catalog";
import type { StudioNode } from "../catalog/types";
import { usePaletteDragActive } from "../hooks/usePaletteDragActive";
import { isContainerCatalogId } from "../lib/container";
import { isEditableCatalogId } from "../lib/editableNode";
import { useStudio } from "../store/StudioContext";

function SortableLeafNode({
  node,
  parentId,
  index,
  isRootLevel,
}: {
  node: StudioNode;
  parentId: string;
  index: number;
  isRootLevel: boolean;
}) {
  const paletteActive = usePaletteDragActive();
  const { selectedId, selectNode, removeSelected, openInspector } = useStudio();
  const def = getComponent(node.catalogId);
  const editable = isEditableCatalogId(node.catalogId);
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } =
    useSortable({
      id: node.instanceId,
      data: isRootLevel
        ? { type: "root-canvas" as const, parentId, index, catalogId: node.catalogId }
        : { type: "canvas" as const, parentId, index, catalogId: node.catalogId },
      disabled: paletteActive,
    });

  const isSelected = selectedId === node.instanceId;

  return (
    <div
      ref={setNodeRef}
      style={{ transform: CSS.Transform.toString(transform), transition }}
      className={`canvas-node ${isSelected ? "selected" : ""} ${isDragging ? "dragging" : ""}`}
      {...attributes}
      {...(paletteActive ? {} : listeners)}
      onClick={(e) => {
        e.stopPropagation();
        selectNode(node.instanceId);
      }}
      onDoubleClick={(e) => {
        e.stopPropagation();
        if (editable) {
          selectNode(node.instanceId);
          openInspector();
        }
      }}
      role="button"
      tabIndex={0}
    >
      <span className="drag-handle" title="Arrastrar para reordenar">
        ⋮⋮
      </span>
      <div className="canvas-node-body">
        <strong>{def?.label ?? node.catalogId}</strong>
        <span className={`badge ${def?.sduiExportable ? "badge-sdui" : "badge-app"}`}>
          {def?.sduiType ?? "App UI"}
        </span>
        {node.props.text && <p className="canvas-preview-text">{node.props.text}</p>}
        {node.props.message && <p className="canvas-preview-text">{node.props.message}</p>}
        {node.props.alias && <p className="canvas-preview-text">TC: {node.props.alias}</p>}
      </div>
      {isSelected && (
        <div className="canvas-node-actions">
          {editable && (
            <button
              type="button"
              className="btn-canvas-edit"
              onPointerDown={(e) => e.stopPropagation()}
              onClick={(e) => {
                e.stopPropagation();
                openInspector();
              }}
            >
              Editar
            </button>
          )}
          <button
            type="button"
            className="btn-icon danger"
            onPointerDown={(e) => e.stopPropagation()}
            onClick={(e) => {
              e.stopPropagation();
              removeSelected();
            }}
          >
            ×
          </button>
        </div>
      )}
    </div>
  );
}

function ContainerChildren({
  node,
  depth,
  rootId,
}: {
  node: StudioNode;
  depth: number;
  rootId: string;
}) {
  const paletteActive = usePaletteDragActive();
  const isRow = node.catalogId === "Row";
  const { setNodeRef, isOver } = useDroppable({
    id: `drop-container-${node.instanceId}`,
    data: { type: "container-drop", parentId: node.instanceId, depth },
  });

  return (
    <SortableContext
      items={node.children.map((c) => c.instanceId)}
      strategy={isRow ? horizontalListSortingStrategy : verticalListSortingStrategy}
    >
      <div
        ref={setNodeRef}
        className={`canvas-container-children ${isOver ? "drop-over" : ""} ${paletteActive ? "palette-drop-target" : ""} ${isRow ? "layout-row" : "layout-column"}`}
      >
        {node.children.length === 0 && (
          <p className="drop-hint">Suelta componentes aquí</p>
        )}
        {node.children.map((child, childIndex) => (
          <CanvasTreeNode
            key={child.instanceId}
            node={child}
            parentId={node.instanceId}
            index={childIndex}
            depth={depth + 1}
            rootId={rootId}
          />
        ))}
      </div>
    </SortableContext>
  );
}

function ContainerHeader({
  node,
  depth,
  sortable,
  dragProps,
}: {
  node: StudioNode;
  depth: number;
  sortable: boolean;
  dragProps?: { attributes: object; listeners: object | undefined };
}) {
  const { selectedId, selectNode, removeSelected } = useStudio();
  const def = getComponent(node.catalogId);
  const isSelected = selectedId === node.instanceId;
  const isRoot = depth === 0;

  return (
    <div
      className="canvas-container-header"
      onClick={(e) => {
        e.stopPropagation();
        selectNode(node.instanceId);
      }}
      role="button"
      tabIndex={0}
    >
      {sortable && dragProps && (
        <span className="drag-handle" {...dragProps.attributes} {...dragProps.listeners}>
          ⋮⋮
        </span>
      )}
      <strong>{def?.label ?? node.catalogId}</strong>
      <span className="badge badge-sdui">SDUI</span>
      <span className="canvas-child-count">{node.children.length} hijos</span>
      {isSelected && !isRoot && (
        <button
          type="button"
          className="btn-icon danger"
          onClick={(e) => {
            e.stopPropagation();
            removeSelected();
          }}
        >
          ×
        </button>
      )}
    </div>
  );
}

function NestedContainerNode({
  node,
  parentId,
  index,
  depth,
  rootId,
}: {
  node: StudioNode;
  parentId: string;
  index: number;
  depth: number;
  rootId: string;
}) {
  const paletteActive = usePaletteDragActive();
  const isRow = node.catalogId === "Row";
  const isRootLevel = parentId === rootId;
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } =
    useSortable({
      id: node.instanceId,
      data: isRootLevel
        ? { type: "root-canvas" as const, parentId, index, catalogId: node.catalogId }
        : { type: "canvas" as const, parentId, index, catalogId: node.catalogId },
      disabled: paletteActive,
    });
  const { selectedId } = useStudio();

  return (
    <div
      ref={setNodeRef}
      style={{ transform: CSS.Transform.toString(transform), transition }}
      className={`canvas-container-node ${selectedId === node.instanceId ? "selected" : ""} ${isDragging ? "dragging" : ""} ${isRow ? "is-row" : "is-column"}`}
      {...attributes}
    >
      <ContainerHeader
        node={node}
        depth={depth}
        sortable
        dragProps={{ attributes, listeners }}
      />
      <ContainerChildren node={node} depth={depth} rootId={rootId} />
    </div>
  );
}

function RootContainerNode({ node }: { node: StudioNode }) {
  const { selectedId } = useStudio();

  return (
    <div
      className={`canvas-root ${selectedId === node.instanceId ? "selected" : ""} ${node.catalogId === "Row" ? "is-row" : "is-column"}`}
    >
      <ContainerHeader node={node} depth={0} sortable={false} />
      <ContainerChildren node={node} depth={0} rootId={node.instanceId} />
    </div>
  );
}

function CanvasTreeNode({
  node,
  parentId,
  index,
  depth,
  rootId,
}: {
  node: StudioNode;
  parentId: string;
  index: number;
  depth: number;
  rootId: string;
}) {
  const isRootLevel = parentId === rootId;

  if (isContainerCatalogId(node.catalogId)) {
    return (
      <NestedContainerNode
        node={node}
        parentId={parentId}
        index={index}
        depth={depth}
        rootId={rootId}
      />
    );
  }
  return (
    <SortableLeafNode
      node={node}
      parentId={parentId}
      index={index}
      isRootLevel={isRootLevel}
    />
  );
}

export function Canvas() {
  const { root } = useStudio();

  return (
    <section className="panel canvas-panel">
      <h2>Canvas</h2>
      <p className="panel-hint canvas-hint">
        Selecciona un <strong>Column</strong> / <strong>Row</strong> y usa <strong>clic</strong> en la
        paleta, o arrastra ⠿ a su área punteada. Reordena hermanos con ⋮⋮.
      </p>
      <RootContainerNode node={root} />
    </section>
  );
}
