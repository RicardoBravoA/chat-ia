import raw from "./catalog.json";
import type { Catalog, CatalogComponent } from "./types";

export const catalog = raw as Catalog;

export const componentById = new Map<string, CatalogComponent>(
  catalog.components.map((c) => [c.id, c]),
);

export function getComponent(id: string): CatalogComponent | undefined {
  return componentById.get(id);
}
