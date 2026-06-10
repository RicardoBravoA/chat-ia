export type AtomicLevel = "atom" | "molecule" | "organism";

export interface PropFieldSchema {
  type: "string";
  label: string;
  multiline?: boolean;
}

export interface UiActionDef {
  id: string;
  label: string;
  actionType: string;
  payload: Record<string, string>;
  requiresConfirmation: boolean;
}

export interface CatalogComponent {
  id: string;
  label: string;
  atomicLevel: AtomicLevel;
  sduiType?: string;
  sduiExportable: boolean;
  composeFile: string;
  mobileRender?: string;
  description: string;
  propsSchema: Record<string, PropFieldSchema>;
  defaultProps: Record<string, string>;
  defaultActions?: UiActionDef[];
  isContainer?: boolean;
}

export interface CatalogTemplate {
  id: string;
  label: string;
  description: string;
  rootChildren: string[];
}

export interface Catalog {
  schemaVersion: number;
  components: CatalogComponent[];
  templates: CatalogTemplate[];
}

export interface StudioNode {
  instanceId: string;
  catalogId: string;
  props: Record<string, string>;
  actions: UiActionDef[];
  children: StudioNode[];
}

export interface UiNodeJson {
  id: string;
  type: string;
  props: Record<string, string>;
  children: UiNodeJson[];
  actions: UiActionDef[];
}

export interface StudioCanvasNodeJson {
  catalogId: string;
  instanceId: string;
  label: string;
  sduiExportable: boolean;
  sduiType?: string;
  props: Record<string, string>;
  actions: UiActionDef[];
  children: StudioCanvasNodeJson[];
}

export interface ChatMessageResponseJson {
  schemaVersion: number;
  correlationId: string;
  sessionId: string;
  userMessage: string;
  metadata: {
    intent: string;
    confidence: number;
    clarificationNeeded: boolean;
    reason: string;
    routerSource: string;
  };
  uiTree: UiNodeJson;
  /** Árbol completo del canvas (incluye componentes App-only). Solo studio. */
  studioCanvas?: StudioCanvasNodeJson;
}
