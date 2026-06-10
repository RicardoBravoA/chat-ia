import { getComponent } from "../catalog";

export function isContainerCatalogId(catalogId: string): boolean {
  return getComponent(catalogId)?.isContainer === true;
}
