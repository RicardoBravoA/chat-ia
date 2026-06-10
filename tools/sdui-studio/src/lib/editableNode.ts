import { getComponent } from "../catalog";
import { isContainerCatalogId } from "./container";

/** Nodos con props o acciones editables en el inspector. */
export function isEditableCatalogId(catalogId: string): boolean {
  if (isContainerCatalogId(catalogId)) return false;
  const def = getComponent(catalogId);
  if (!def) return false;
  if (Object.keys(def.propsSchema).length > 0) return true;
  if (catalogId === "GreetingCard" && (def.defaultActions?.length ?? 0) > 0) return true;
  return false;
}
