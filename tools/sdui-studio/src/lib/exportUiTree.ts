import { getComponent } from "../catalog";
import type {
  CatalogComponent,
  ChatMessageResponseJson,
  StudioCanvasNodeJson,
  StudioNode,
  UiNodeJson,
} from "../catalog/types";

const INTENT_HINTS: Array<{
  catalogIds: string[];
  intent: string;
  userMessage: string;
  reason: string;
}> = [
  {
    catalogIds: ["PayCardPanel", "CreditCardChatVisual", "PaymentModeOptionBox"],
    intent: "PAY_CREDIT_CARD",
    userMessage: "pagar mi tc",
    reason: "Canvas incluye flujo de pago TC",
  },
  {
    catalogIds: ["BalanceCard"],
    intent: "CHECK_BALANCE",
    userMessage: "consultar saldo",
    reason: "Canvas incluye consulta de saldo",
  },
  {
    catalogIds: ["GreetingCard"],
    intent: "GREETING",
    userMessage: "hola",
    reason: "Canvas incluye saludo",
  },
  {
    catalogIds: ["SupportChannelsCard"],
    intent: "OUT_OF_SCOPE",
    userMessage: "ayuda",
    reason: "Canvas incluye canales de soporte",
  },
  {
    catalogIds: ["ChatHistoryRow"],
    intent: "CHAT_HISTORY",
    userMessage: "historial de chat",
    reason: "Canvas incluye historial",
  },
  {
    catalogIds: ["SpendingCategoryRow"],
    intent: "MONTHLY_EXPENSES",
    userMessage: "gastos del mes",
    reason: "Canvas incluye gastos por categoría",
  },
  {
    catalogIds: [
      "LoginFormCard",
      "LoginEmailField",
      "LoginPasswordField",
      "LoginSubmitButton",
    ],
    intent: "STUDIO_LOGIN_PREVIEW",
    userMessage: "iniciar sesión",
    reason: "Canvas con formulario login (preview App, no SDUI mobile)",
  },
];

function collectCatalogIds(node: StudioNode, out: Set<string> = new Set()): Set<string> {
  out.add(node.catalogId);
  node.children.forEach((c) => collectCatalogIds(c, out));
  return out;
}

export function inferMetadataFromCanvas(root: StudioNode): {
  intent: string;
  userMessage: string;
  reason: string;
} {
  const ids = collectCatalogIds(root);
  for (const hint of INTENT_HINTS) {
    if (hint.catalogIds.some((id) => ids.has(id))) {
      return {
        intent: hint.intent,
        userMessage: hint.userMessage,
        reason: hint.reason,
      };
    }
  }
  return {
    intent: "STUDIO_COMPOSITION",
    userMessage: "composición studio",
    reason: "SDUI Studio export",
  };
}

function uiNodeId(node: StudioNode, type: string, isRootColumn: boolean): string {
  if (isRootColumn) return "column-root";
  const kebab = type.replace(/([a-z])([A-Z])/g, "$1-$2").toLowerCase();
  const suffix = node.instanceId.includes("-")
    ? node.instanceId.split("-").slice(-1)[0]
    : node.instanceId;
  return `${kebab}-${suffix}`;
}

export function createNodeFromCatalog(catalogId: string): StudioNode | null {
  const def = getComponent(catalogId);
  if (!def) return null;
  return {
    instanceId: crypto.randomUUID().slice(0, 8),
    catalogId,
    props: { ...def.defaultProps },
    actions: def.defaultActions ? def.defaultActions.map((a) => ({ ...a, payload: { ...a.payload } })) : [],
    children: [],
  };
}

export function studioNodeToCanvasJson(node: StudioNode): StudioCanvasNodeJson {
  const def = getComponent(node.catalogId);
  return {
    catalogId: node.catalogId,
    instanceId: node.instanceId,
    label: def?.label ?? node.catalogId,
    sduiExportable: def?.sduiExportable ?? false,
    sduiType: def?.sduiType,
    props: { ...node.props },
    actions: node.actions.map((a) => ({ ...a, payload: { ...a.payload } })),
    children: node.children.map(studioNodeToCanvasJson),
  };
}

export function studioNodeToUiJson(
  node: StudioNode,
  options: { isRoot?: boolean } = {},
): UiNodeJson | null {
  const def = getComponent(node.catalogId);
  if (!def) return null;

  if (!def.sduiExportable || !def.sduiType) {
    return null;
  }

  const type = def.sduiType;
  const isRootColumn = (options.isRoot ?? false) && type === "Column";
  const id = uiNodeId(node, type, isRootColumn);

  if (def.isContainer) {
    const children = node.children
      .map((c) => studioNodeToUiJson(c))
      .filter((c): c is UiNodeJson => c !== null);
    return {
      id,
      type,
      props: { ...node.props },
      children,
      actions: [],
    };
  }

  return {
    id,
    type,
    props: { ...node.props },
    children: [],
    actions: node.actions.map((a) => ({ ...a, payload: { ...a.payload } })),
  };
}

export function buildExportPayload(
  root: StudioNode,
  options: {
    userMessage?: string;
    intent?: string;
    confidence?: number;
  } = {},
): {
  full: ChatMessageResponseJson;
  uiTreeOnly: UiNodeJson | null;
  studioCanvas: StudioCanvasNodeJson;
  skippedCatalogIds: string[];
} {
  const skipped: string[] = [];
  const collectSkipped = (n: StudioNode) => {
    const def = getComponent(n.catalogId);
    if (def && !def.sduiExportable) skipped.push(n.catalogId);
    n.children.forEach(collectSkipped);
  };
  collectSkipped(root);

  const studioCanvas = studioNodeToCanvasJson(root);
  const uiTree = studioNodeToUiJson(root, { isRoot: true });
  const inferred = inferMetadataFromCanvas(root);

  const full: ChatMessageResponseJson = {
    schemaVersion: 1,
    correlationId: crypto.randomUUID(),
    sessionId: "studio-preview",
    userMessage: options.userMessage ?? inferred.userMessage,
    metadata: {
      intent: options.intent ?? inferred.intent,
      confidence: options.confidence ?? 0.9,
      clarificationNeeded: false,
      reason: inferred.reason,
      routerSource: "studio",
    },
    uiTree: uiTree ?? {
      id: "column-root",
      type: "Column",
      props: {},
      children: [],
      actions: [],
    },
    studioCanvas,
  };

  return { full, uiTreeOnly: uiTree, studioCanvas, skippedCatalogIds: [...new Set(skipped)] };
}

export function mobileRenderSnippet(def: CatalogComponent | undefined): string {
  if (!def) return "";
  if (def.mobileRender) return def.mobileRender;
  if (def.sduiType) {
    return `SduiRenderer → when (type == "${def.sduiType}")`;
  }
  return def.composeFile;
}
