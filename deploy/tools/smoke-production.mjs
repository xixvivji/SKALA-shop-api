import { requiredEnvironment } from "./http-client.mjs";

const frontendOrigin = new URL(requiredEnvironment("SKALA_FRONTEND_ORIGIN")).origin;
const apiOrigin = new URL(requiredEnvironment("SKALA_API_BASE_URL")).origin;

async function check(name, url, validate) {
  const startedAt = performance.now();
  const response = await fetch(url, { headers: { Accept: "application/json,text/html" }, redirect: "error" });
  const text = await response.text();
  if (!response.ok) throw new Error(`${name}: ${response.status}`);
  if (validate && !validate(response, text)) throw new Error(`${name}: 응답 내용이 올바르지 않습니다.`);
  console.log(`ok ${name} ${response.status} ${Math.round(performance.now() - startedAt)}ms`);
}

await check("frontend", `${frontendOrigin}/`, (response, text) =>
  response.headers.get("content-type")?.includes("text/html") && text.includes("SKALA"));
await check("health", `${apiOrigin}/actuator/health`, (_response, text) =>
  JSON.parse(text).status === "UP");
await check("categories", `${apiOrigin}/api/categories`, (_response, text) =>
  Array.isArray(JSON.parse(text)));
await check("products", `${apiOrigin}/api/products?page=0&size=1`, (_response, text) =>
  Array.isArray(JSON.parse(text).content));
await check("openapi", `${apiOrigin}/v3/api-docs`, (_response, text) =>
  JSON.parse(text).openapi?.startsWith("3."));

console.log(`production smoke passed: ${frontendOrigin} -> ${apiOrigin}`);
