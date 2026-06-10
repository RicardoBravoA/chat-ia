import { act, renderHook } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type { ReactNode } from "react";
import { StudioProvider, useStudio } from "./StudioContext";

function wrapper({ children }: { children: ReactNode }) {
  return <StudioProvider>{children}</StudioProvider>;
}

describe("StudioProvider", () => {
  it("starts with empty Column root", () => {
    const { result } = renderHook(() => useStudio(), { wrapper });
    expect(result.current.root.catalogId).toBe("Column");
    expect(result.current.root.children).toHaveLength(0);
  });

  it("addChild appends to specified parent", () => {
    const { result } = renderHook(() => useStudio(), { wrapper });
    const rootId = result.current.root.instanceId;

    act(() => {
      result.current.addChild(rootId, "Row");
    });

    expect(result.current.root.children).toHaveLength(1);
    expect(result.current.root.children[0].catalogId).toBe("Row");
  });

  it("addToRoot adds to selected Column/Row container", () => {
    const { result } = renderHook(() => useStudio(), { wrapper });

    act(() => {
      result.current.addChild(result.current.root.instanceId, "Row");
    });
    const rowId = result.current.root.children[0].instanceId;

    act(() => {
      result.current.selectNode(rowId);
      result.current.addToRoot("AssistantText");
    });

    const row = result.current.root.children[0];
    expect(row.children).toHaveLength(1);
    expect(row.children[0].catalogId).toBe("AssistantText");
  });

  it("appendToCanvasEnd always adds to root column", () => {
    const { result } = renderHook(() => useStudio(), { wrapper });

    act(() => {
      result.current.addChild(result.current.root.instanceId, "Row");
    });
    const rowId = result.current.root.children[0].instanceId;

    act(() => {
      result.current.selectNode(rowId);
    });

    act(() => {
      result.current.appendToCanvasEnd("BalanceCard");
    });

    expect(result.current.root.children).toHaveLength(2);
    expect(result.current.root.children[1].catalogId).toBe("BalanceCard");
    expect(result.current.root.children[0].children).toHaveLength(0);
  });

  it("moveChildById reorders siblings in parent", () => {
    const { result } = renderHook(() => useStudio(), { wrapper });
    const rootId = result.current.root.instanceId;

    act(() => {
      result.current.addChild(rootId, "AssistantText");
      result.current.addChild(rootId, "BalanceCard");
      result.current.addChild(rootId, "InfoBanner");
    });

    const [a, , c] = result.current.root.children.map((n) => n.instanceId);

    act(() => {
      result.current.moveChildById(rootId, c, a);
    });

    expect(result.current.root.children.map((n) => n.instanceId)).toEqual([c, a, result.current.root.children[2].instanceId]);
  });

  it("removeSelected deletes node but not root", () => {
    const { result } = renderHook(() => useStudio(), { wrapper });

    act(() => {
      result.current.addChild(result.current.root.instanceId, "AssistantText");
    });
    const leafId = result.current.root.children[0].instanceId;

    act(() => {
      result.current.selectNode(leafId);
      result.current.removeSelected();
    });

    expect(result.current.root.children).toHaveLength(0);
    expect(result.current.selectedId).toBeNull();
  });

  it("loadTemplate replaces canvas tree", () => {
    const { result } = renderHook(() => useStudio(), { wrapper });

    act(() => {
      result.current.loadTemplate("balance");
    });

    expect(result.current.root.children.map((c) => c.catalogId)).toEqual([
      "AssistantText",
      "BalanceCard",
    ]);
  });
});
