import { describe, expect, it, vi, beforeEach } from "vitest";
import type { StudioNode } from "../catalog/types";
import {
  buildExportPayload,
  createNodeFromCatalog,
  inferMetadataFromCanvas,
  mobileRenderSnippet,
  studioNodeToCanvasJson,
  studioNodeToUiJson,
} from "./exportUiTree";
import { getComponent } from "../catalog";

function columnNode(children: StudioNode[] = [], instanceId = "root-1"): StudioNode {
  return {
    instanceId,
    catalogId: "Column",
    props: {},
    actions: [],
    children,
  };
}

describe("createNodeFromCatalog", () => {
  it("returns null for unknown catalog id", () => {
    expect(createNodeFromCatalog("DoesNotExist")).toBeNull();
  });

  it("copies default props and actions from catalog", () => {
    const node = createNodeFromCatalog("AssistantText");
    expect(node).not.toBeNull();
    expect(node!.catalogId).toBe("AssistantText");
    expect(node!.props.text).toBeTruthy();
    expect(node!.children).toEqual([]);
  });
});

describe("studioNodeToUiJson", () => {
  it("exports SDUI leaf with props and actions", () => {
    const leaf: StudioNode = {
      instanceId: "abc",
      catalogId: "AssistantText",
      props: { text: "Hola" },
      actions: [],
      children: [],
    };
    const json = studioNodeToUiJson(leaf);
    expect(json).not.toBeNull();
    expect(json!.type).toBe("AssistantText");
    expect(json!.props.text).toBe("Hola");
    expect(json!.children).toEqual([]);
  });

  it("skips non-exportable app-only components", () => {
    const node = createNodeFromCatalog("LoginFormCard");
    expect(node).not.toBeNull();
    expect(studioNodeToUiJson(node!)).toBeNull();
  });

  it("exports nested Column with SDUI children only", () => {
    const assistant = createNodeFromCatalog("AssistantText")!;
    assistant.instanceId = "a1";
    const login = createNodeFromCatalog("LoginFormCard")!;
    login.instanceId = "login-1";
    const root = columnNode([assistant, login]);

    const json = studioNodeToUiJson(root);
    expect(json!.type).toBe("Column");
    expect(json!.children).toHaveLength(1);
    expect(json!.children[0].type).toBe("AssistantText");
  });

  it("exports Row container", () => {
    const row = createNodeFromCatalog("Row")!;
    row.instanceId = "row-1";
    const json = studioNodeToUiJson(row);
    expect(json!.type).toBe("Row");
    expect(json!.children).toEqual([]);
  });

  it("exports PayCardPanel nested inside Row", () => {
    const row = createNodeFromCatalog("Row")!;
    row.instanceId = "row-1";
    const panel = createNodeFromCatalog("PayCardPanel")!;
    panel.instanceId = "panel-1";
    panel.props.alias = "Platinum";
    row.children = [panel];
    const root = columnNode([row]);

    const json = studioNodeToUiJson(root, { isRoot: true });
    expect(json!.children[0].type).toBe("Row");
    expect(json!.children[0].children).toHaveLength(1);
    expect(json!.children[0].children[0].type).toBe("PayCardPanel");
    expect(json!.children[0].children[0].props.alias).toBe("Platinum");
  });
});

describe("studioNodeToCanvasJson", () => {
  it("includes app-only login form components", () => {
    const row = createNodeFromCatalog("Row")!;
    row.instanceId = "row-1";
    const form = createNodeFromCatalog("LoginFormCard")!;
    form.instanceId = "form-1";
    const email = createNodeFromCatalog("LoginEmailField")!;
    email.instanceId = "email-1";
    form.children = [email];
    row.children = [form];
    const root = columnNode([row]);

    const canvas = studioNodeToCanvasJson(root);
    expect(canvas.children[0].catalogId).toBe("Row");
    expect(canvas.children[0].children[0].catalogId).toBe("LoginFormCard");
    expect(canvas.children[0].children[0].children[0].catalogId).toBe("LoginEmailField");
    expect(canvas.children[0].children[0].sduiExportable).toBe(false);
  });
});

describe("inferMetadataFromCanvas", () => {
  it("infers PAY_CREDIT_CARD when PayCardPanel is present", () => {
    const root = columnNode([createNodeFromCatalog("PayCardPanel")!]);
    const meta = inferMetadataFromCanvas(root);
    expect(meta.intent).toBe("PAY_CREDIT_CARD");
  });

  it("infers login preview for login form components", () => {
    const root = columnNode([createNodeFromCatalog("LoginFormCard")!]);
    expect(inferMetadataFromCanvas(root).intent).toBe("STUDIO_LOGIN_PREVIEW");
  });
});

describe("buildExportPayload", () => {
  beforeEach(() => {
    vi.stubGlobal("crypto", {
      randomUUID: () => "00000000-0000-4000-8000-000000000001",
    });
  });

  it("builds ChatMessageResponse with studioCanvas and inferred metadata", () => {
    const panel = createNodeFromCatalog("PayCardPanel")!;
    panel.instanceId = "p1";
    const root = columnNode([panel]);
    const { full, uiTreeOnly, studioCanvas } = buildExportPayload(root);
    expect(full.schemaVersion).toBe(1);
    expect(full.userMessage).toBe("pagar mi tc");
    expect(full.metadata.intent).toBe("PAY_CREDIT_CARD");
    expect(full.metadata.routerSource).toBe("studio");
    expect(full.studioCanvas?.children[0].catalogId).toBe("PayCardPanel");
    expect(studioCanvas.children[0].catalogId).toBe("PayCardPanel");
    expect(uiTreeOnly?.type).toBe("Column");
    expect(uiTreeOnly?.id).toBe("column-root");
  });

  it("collects skipped non-SDUI catalog ids", () => {
    const login = createNodeFromCatalog("LoginFormCard")!;
    login.instanceId = "login-1";
    const root = columnNode([login]);
    const { skippedCatalogIds } = buildExportPayload(root);
    expect(skippedCatalogIds).toContain("LoginFormCard");
  });
});

describe("mobileRenderSnippet", () => {
  it("returns mobile render hint from catalog", () => {
    const def = getComponent("BalanceCard");
    expect(mobileRenderSnippet(def)).toContain("ChatBalanceMiniCard");
  });
});
