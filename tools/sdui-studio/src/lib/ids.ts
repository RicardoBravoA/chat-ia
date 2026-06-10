export function newInstanceId(prefix = "node"): string {
  return `${prefix}-${crypto.randomUUID().slice(0, 8)}`;
}
