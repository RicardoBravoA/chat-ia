import { describe, expect, it } from "vitest";
import { isEditableCatalogId } from "./editableNode";

describe("isEditableCatalogId", () => {
  it("containers are not editable", () => {
    expect(isEditableCatalogId("Column")).toBe(false);
    expect(isEditableCatalogId("Row")).toBe(false);
  });

  it("components with props schema are editable", () => {
    expect(isEditableCatalogId("AssistantText")).toBe(true);
    expect(isEditableCatalogId("PayCardPanel")).toBe(true);
  });

  it("GreetingCard is editable for actions", () => {
    expect(isEditableCatalogId("GreetingCard")).toBe(true);
  });
});
