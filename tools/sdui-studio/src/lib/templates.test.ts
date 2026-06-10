import { describe, expect, it } from "vitest";
import { buildTemplate, emptyRoot } from "./templates";

describe("emptyRoot", () => {
  it("creates empty Column root", () => {
    const root = emptyRoot();
    expect(root.catalogId).toBe("Column");
    expect(root.children).toEqual([]);
    expect(root.instanceId).toMatch(/^root-/);
  });
});

describe("buildTemplate", () => {
  it("returns null for unknown template", () => {
    expect(buildTemplate("missing")).toBeNull();
  });

  it("loads pay-tc template children", () => {
    const root = buildTemplate("pay-tc");
    expect(root).not.toBeNull();
    expect(root!.catalogId).toBe("Column");
    expect(root!.children.map((c) => c.catalogId)).toEqual(["AssistantText", "PayCardPanel"]);
  });

  it("loads greeting template", () => {
    const root = buildTemplate("greeting");
    expect(root!.children).toHaveLength(1);
    expect(root!.children[0].catalogId).toBe("GreetingCard");
  });
});
