import { useDndContext } from "@dnd-kit/core";

export function usePaletteDragActive(): boolean {
  const { active } = useDndContext();
  return active?.data.current?.type === "palette";
}
