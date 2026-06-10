import type { StudioNode } from "../catalog/types";
import { NodePreview } from "./ComponentPreview";

interface PreviewRendererProps {
  node: StudioNode;
}

export function PreviewRenderer({ node }: PreviewRendererProps) {
  return <NodePreview node={node} variant="full" />;
}
