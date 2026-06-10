import type { StudioNode } from "../catalog/types";
import { isContainerCatalogId } from "./container";

export function findNode(
  root: StudioNode,
  instanceId: string,
): { node: StudioNode; parent: StudioNode | null } | null {
  if (root.instanceId === instanceId) return { node: root, parent: null };
  for (const child of root.children) {
    if (child.instanceId === instanceId) return { node: child, parent: root };
    const nested = findNode(child, instanceId);
    if (nested) return nested;
  }
  return null;
}

export function removeNode(root: StudioNode, instanceId: string): StudioNode {
  if (root.instanceId === instanceId) return root;
  return {
    ...root,
    children: root.children
      .filter((c) => c.instanceId !== instanceId)
      .map((c) => removeNode(c, instanceId)),
  };
}

export function updateNode(
  root: StudioNode,
  instanceId: string,
  updater: (n: StudioNode) => StudioNode,
): StudioNode {
  if (root.instanceId === instanceId) return updater(root);
  return {
    ...root,
    children: root.children.map((c) => updateNode(c, instanceId, updater)),
  };
}

export function appendChildNode(root: StudioNode, parentId: string, child: StudioNode): StudioNode {
  return updateNode(root, parentId, (n) => ({
    ...n,
    children: [...n.children, child],
  }));
}

export function insertChildNode(
  root: StudioNode,
  parentId: string,
  child: StudioNode,
  index: number,
): StudioNode {
  return updateNode(root, parentId, (n) => {
    const next = [...n.children];
    next.splice(index, 0, child);
    return { ...n, children: next };
  });
}

export function moveChildByIdInTree(
  root: StudioNode,
  parentId: string,
  activeId: string,
  overId: string,
): StudioNode {
  if (activeId === overId) return root;
  return updateNode(root, parentId, (n) => {
    const fromIndex = n.children.findIndex((c) => c.instanceId === activeId);
    const toIndex = n.children.findIndex((c) => c.instanceId === overId);
    if (fromIndex < 0 || toIndex < 0) return n;
    const next = [...n.children];
    const [item] = next.splice(fromIndex, 1);
    next.splice(toIndex, 0, item);
    return { ...n, children: next };
  });
}

export function resolveAddParentId(tree: StudioNode, activeSelection: string | null): string {
  if (activeSelection) {
    const found = findNode(tree, activeSelection);
    if (found && isContainerCatalogId(found.node.catalogId)) {
      return found.node.instanceId;
    }
  }
  return tree.instanceId;
}
