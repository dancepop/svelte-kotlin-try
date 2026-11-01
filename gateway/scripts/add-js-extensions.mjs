// proto-loader-gen-types writes `from './x'`, but this ESM project needs `from './x.js'`.
// Usage: node scripts/add-js-extensions.mjs <dir>
import { readdirSync, readFileSync, writeFileSync } from "node:fs";
import path from "node:path";

const dir = process.argv[2];
for (const file of readdirSync(dir, { recursive: true, withFileTypes: true })) {
  if (!file.isFile() || !file.name.endsWith(".ts")) continue;
  const filePath = path.join(file.parentPath, file.name);
  const source = readFileSync(filePath, "utf8");
  writeFileSync(filePath, source.replace(/from '(\.{1,2}\/[^']+)'/g, "from '$1.js'"));
}
