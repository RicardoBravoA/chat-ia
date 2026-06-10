import { getComponent } from "../catalog";
import type { StudioNode } from "../catalog/types";

export function studioNodeFromCatalog(catalogId: string, instanceId = "preview"): StudioNode | null {
  const def = getComponent(catalogId);
  if (!def) return null;

  return {
    instanceId,
    catalogId,
    props: { ...def.defaultProps },
    actions: def.defaultActions
      ? def.defaultActions.map((a) => ({ ...a, payload: { ...a.payload } }))
      : [],
    children: [],
  };
}
