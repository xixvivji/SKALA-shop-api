import { existsSync, readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const frontendRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const errors = [];

const htmlPath = resolve(frontendRoot, "index.html");
const html = readFileSync(htmlPath, "utf8");

const localReferences = [...html.matchAll(/\b(?:href|src)=["']([^"']+)["']/g)]
  .map((match) => match[1])
  .filter((reference) => !/^(?:[a-z]+:|\/\/|#)/i.test(reference));

for (const reference of localReferences) {
  const pathWithoutQuery = reference.split(/[?#]/, 1)[0];
  if (!existsSync(resolve(frontendRoot, pathWithoutQuery))) {
    errors.push(`index.html에서 찾을 수 없는 파일을 참조합니다: ${reference}`);
  }
}

const ids = [...html.matchAll(/\bid=["']([^"']+)["']/g)].map((match) => match[1]);
const duplicateIds = [...new Set(ids.filter((id, index) => ids.indexOf(id) !== index))];
for (const id of duplicateIds) {
  errors.push(`index.html에 중복된 id가 있습니다: ${id}`);
}

for (const relativePath of ["config.js", "js/api.js", "js/app.js"]) {
  const sourcePath = resolve(frontendRoot, relativePath);
  const source = readFileSync(sourcePath, "utf8");
  const imports = [...source.matchAll(/\b(?:import|export)\s+(?:[^"']*?\s+from\s+)?["'](\.[^"']+)["']/g)]
    .map((match) => match[1]);

  for (const importedPath of imports) {
    if (!existsSync(resolve(dirname(sourcePath), importedPath))) {
      errors.push(`${relativePath}에서 찾을 수 없는 모듈을 참조합니다: ${importedPath}`);
    }
  }
}

if (errors.length > 0) {
  for (const error of errors) {
    console.error(error);
  }
  process.exitCode = 1;
} else {
  console.log("Frontend static file validation passed.");
}
