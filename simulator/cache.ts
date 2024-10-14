import { existsSync, readFileSync, writeFileSync } from "fs";
import type { Point } from "./messages";

const cacheFile = ".cache";

const isProduction = process.env.NODE_ENV === "production";

export function loadCache(): Map<string, Point> {
  if (isProduction || !existsSync(cacheFile)) return new Map();
  const json = JSON.parse(readFileSync(cacheFile).toString());
  return new Map(json);
}

export function saveCache(state: Map<string, Point>) {
  if (isProduction) return;
  const json = JSON.stringify([...state.entries()]);
  writeFileSync(cacheFile, json);
}
