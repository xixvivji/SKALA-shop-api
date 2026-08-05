import { cp, mkdir, readFile, rm, writeFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const frontendRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const outputDirectory = resolve(frontendRoot, "dist");
const configuredApiUrl = process.env.SKALA_API_BASE_URL?.trim();
const isVercelBuild = process.env.VERCEL === "1";

if (isVercelBuild && !configuredApiUrl) {
  throw new Error(
    "Vercel 환경변수 SKALA_API_BASE_URL에 실제 HTTPS API 주소를 설정해야 합니다.",
  );
}

let normalizedApiUrl = "";
if (configuredApiUrl) {
  const parsedUrl = new URL(configuredApiUrl);
  if (isVercelBuild && parsedUrl.protocol !== "https:") {
    throw new Error("Vercel의 SKALA_API_BASE_URL은 HTTPS 주소여야 합니다.");
  }
  if (!["http:", "https:"].includes(parsedUrl.protocol)) {
    throw new Error("SKALA_API_BASE_URL은 HTTP 또는 HTTPS 주소여야 합니다.");
  }
  normalizedApiUrl = parsedUrl.href.replace(/\/+$/, "");
}

await rm(outputDirectory, { recursive: true, force: true });
await mkdir(resolve(outputDirectory, "js"), { recursive: true });

await Promise.all([
  cp(resolve(frontendRoot, "index.html"), resolve(outputDirectory, "index.html")),
  cp(resolve(frontendRoot, "styles.css"), resolve(outputDirectory, "styles.css")),
  cp(resolve(frontendRoot, "config.js"), resolve(outputDirectory, "config.js")),
  cp(resolve(frontendRoot, "js"), resolve(outputDirectory, "js"), { recursive: true }),
]);

const runtimeConfig = `window.SKALA_CONFIG = ${JSON.stringify({
  API_BASE_URL: normalizedApiUrl,
})};\n`;
await writeFile(resolve(outputDirectory, "runtime-config.js"), runtimeConfig, "utf8");

const builtHtml = await readFile(resolve(outputDirectory, "index.html"), "utf8");
if (!builtHtml.includes("./runtime-config.js")) {
  throw new Error("빌드 결과에 runtime-config.js 참조가 없습니다.");
}

console.log(
  `Frontend build completed: API=${normalizedApiUrl || "environment-aware local default"}`,
);
