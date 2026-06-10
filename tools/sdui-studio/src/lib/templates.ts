import { catalog } from "../catalog";
import { createNodeFromCatalog } from "./exportUiTree";
import type { StudioNode } from "../catalog/types";
import { newInstanceId } from "./ids";

export function emptyRoot(): StudioNode {
  const column = createNodeFromCatalog("Column");
  if (!column) {
    throw new Error("Column missing from catalog");
  }
  column.instanceId = newInstanceId("root");
  column.children = [];
  return column;
}

export function buildTemplate(templateId: string): StudioNode | null {
  const template = catalog.templates.find((t) => t.id === templateId);
  if (!template) return null;

  const column = emptyRoot();

  for (const childCatalogId of template.rootChildren) {
    const child = createNodeFromCatalog(childCatalogId);
    if (child) {
      child.instanceId = newInstanceId(childCatalogId);
      column.children.push(child);
    }
  }

  return column;
}
