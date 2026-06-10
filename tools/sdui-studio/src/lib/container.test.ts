import { describe, expect, it } from "vitest";
import { isContainerCatalogId } from "./container";

describe("isContainerCatalogId", () => {
  it("returns true for Column and Row", () => {
    expect(isContainerCatalogId("Column")).toBe(true);
    expect(isContainerCatalogId("Row")).toBe(true);
  });

  it("returns false for leaf components", () => {
    expect(isContainerCatalogId("AssistantText")).toBe(false);
    expect(isContainerCatalogId("PayCardPanel")).toBe(false);
  });

  it("returns false for unknown ids", () => {
    expect(isContainerCatalogId("Unknown")).toBe(false);
  });
});
