import { describe, expect, it } from "vitest";
import type { StudioNode } from "../catalog/types";
import { createNodeFromCatalog } from "./exportUiTree";
import {
  appendChildNode,
  findNode,
  insertChildNode,
  moveChildByIdInTree,
  removeNode,
  resolveAddParentId,
} from "./studioTree";

function node(id: string, catalogId: string, children: StudioNode[] = []): StudioNode {
  return { instanceId: id, catalogId, props: {}, actions: [], children };
}

describe("findNode", () => {
  const tree = node("root", "Column", [
    node("row-1", "Row", [node("leaf-1", "AssistantText")]),
    node("leaf-2", "BalanceCard"),
  ]);

  it("finds root", () => {
    const found = findNode(tree, "root");
    expect(found?.node.instanceId).toBe("root");
    expect(found?.parent).toBeNull();
  });

  it("finds nested leaf with parent", () => {
    const found = findNode(tree, "leaf-1");
    expect(found?.node.catalogId).toBe("AssistantText");
    expect(found?.parent?.instanceId).toBe("row-1");
  });
});

describe("removeNode", () => {
  it("removes nested node without touching root", () => {
    const tree = node("root", "Column", [node("leaf-1", "AssistantText")]);
    const next = removeNode(tree, "leaf-1");
    expect(next.children).toHaveLength(0);
    expect(next.instanceId).toBe("root");
  });
});

describe("appendChildNode / insertChildNode", () => {
  it("appends child to nested container", () => {
    const root = node("root", "Column", [node("row-1", "Row")]);
    const child = createNodeFromCatalog("AssistantText")!;
    child.instanceId = "new-1";

    const next = appendChildNode(root, "row-1", child);
    const row = findNode(next, "row-1")!.node;
    expect(row.children).toHaveLength(1);
    expect(row.children[0].catalogId).toBe("AssistantText");
  });

  it("inserts child at index among siblings", () => {
    const root = node("root", "Column", [
      node("a", "AssistantText"),
      node("c", "BalanceCard"),
    ]);
    const child = createNodeFromCatalog("InfoBanner")!;
    child.instanceId = "b";

    const next = insertChildNode(root, "root", child, 1);
    expect(next.children.map((c) => c.instanceId)).toEqual(["a", "b", "c"]);
  });
});

describe("moveChildByIdInTree", () => {
  it("reorders siblings within same parent", () => {
    const root = node("root", "Column", [
      node("a", "AssistantText"),
      node("b", "BalanceCard"),
      node("c", "InfoBanner"),
    ]);

    const next = moveChildByIdInTree(root, "root", "c", "a");
    expect(next.children.map((c) => c.instanceId)).toEqual(["c", "a", "b"]);
  });

  it("no-ops when active and over are the same", () => {
    const root = node("root", "Column", [node("a", "AssistantText")]);
    const next = moveChildByIdInTree(root, "root", "a", "a");
    expect(next).toBe(root);
  });
});

describe("resolveAddParentId", () => {
  const root = node("root", "Column", [node("row-1", "Row"), node("leaf-1", "AssistantText")]);

  it("returns selected container id", () => {
    expect(resolveAddParentId(root, "row-1")).toBe("row-1");
  });

  it("returns root when selection is a leaf", () => {
    expect(resolveAddParentId(root, "leaf-1")).toBe("root");
  });

  it("returns root when nothing selected", () => {
    expect(resolveAddParentId(root, null)).toBe("root");
  });
});
