import process from "node:process";

const integer = (name, fallback, minimum = 1) => {
  const value = Number.parseInt(process.env[name] ?? String(fallback), 10);
  if (!Number.isInteger(value) || value < minimum) {
    throw new Error(`${name} must be an integer greater than or equal to ${minimum}`);
  }
  return value;
};

const ratio = (name, fallback) => {
  const value = Number.parseFloat(process.env[name] ?? String(fallback));
  if (!Number.isFinite(value) || value < 0 || value > 1) {
    throw new Error(`${name} must be between 0 and 1`);
  }
  return value;
};

const baseUrl = new URL(process.env.LOAD_BASE_URL ?? "http://localhost:8080");
const scenario = process.env.LOAD_SCENARIO ?? "mixed";
const durationSeconds = integer("LOAD_DURATION_SECONDS", 15);
const concurrency = integer("LOAD_CONCURRENCY", 5);
const requestTimeoutMs = integer("LOAD_REQUEST_TIMEOUT_MS", 3_000);
const maxP95Ms = integer("LOAD_MAX_P95_MS", 1_000);
const maxErrorRate = ratio("LOAD_MAX_ERROR_RATE", 0.01);
const searchQuery = process.env.LOAD_SEARCH_QUERY ?? "상품";
const localHosts = new Set(["localhost", "127.0.0.1", "::1"]);

if (!localHosts.has(baseUrl.hostname) && process.env.ALLOW_PRODUCTION_LOAD !== "true") {
  throw new Error("Non-local load tests require ALLOW_PRODUCTION_LOAD=true");
}

const endpointsByScenario = {
  catalog: ["/api/categories", "/api/products?page=0&size=20"],
  search: [`/api/search/products?query=${encodeURIComponent(searchQuery)}&page=0&size=20`],
  mixed: [
    "/api/categories",
    "/api/products?page=0&size=20",
    `/api/search/products?query=${encodeURIComponent(searchQuery)}&page=0&size=20`,
  ],
};
const endpoints = endpointsByScenario[scenario];
if (!endpoints) {
  throw new Error("LOAD_SCENARIO must be catalog, search, or mixed");
}

const samples = [];
const statusCounts = new Map();
const endpointCounts = new Map();
let errors = 0;

const execute = async (path) => {
  const startedAt = performance.now();
  let status = "NETWORK_ERROR";
  try {
    const response = await fetch(new URL(path, baseUrl), {
      headers: { accept: "application/json" },
      signal: AbortSignal.timeout(requestTimeoutMs),
    });
    status = String(response.status);
    await response.arrayBuffer();
    if (!response.ok) errors += 1;
  } catch {
    errors += 1;
  } finally {
    samples.push(performance.now() - startedAt);
    statusCounts.set(status, (statusCounts.get(status) ?? 0) + 1);
    endpointCounts.set(path, (endpointCounts.get(path) ?? 0) + 1);
  }
};

for (const endpoint of endpoints) {
  await execute(endpoint);
}

const deadline = performance.now() + durationSeconds * 1_000;
const workers = Array.from({ length: concurrency }, (_, workerIndex) => (async () => {
  let requestIndex = workerIndex;
  while (performance.now() < deadline) {
    await execute(endpoints[requestIndex % endpoints.length]);
    requestIndex += concurrency;
  }
})());
await Promise.all(workers);

const sorted = [...samples].sort((left, right) => left - right);
const percentile = (value) => sorted[Math.max(0, Math.ceil(sorted.length * value) - 1)] ?? 0;
const errorRate = samples.length === 0 ? 1 : errors / samples.length;
const result = {
  baseUrl: baseUrl.origin,
  scenario,
  durationSeconds,
  concurrency,
  requests: samples.length,
  errors,
  errorRate: Number(errorRate.toFixed(4)),
  requestsPerSecond: Number((samples.length / durationSeconds).toFixed(2)),
  latencyMs: {
    p50: Number(percentile(0.5).toFixed(2)),
    p95: Number(percentile(0.95).toFixed(2)),
    p99: Number(percentile(0.99).toFixed(2)),
    max: Number((sorted.at(-1) ?? 0).toFixed(2)),
  },
  statusCounts: Object.fromEntries([...statusCounts.entries()].sort()),
  endpointCounts: Object.fromEntries(endpointCounts),
  thresholds: { maxErrorRate, maxP95Ms },
};

console.log(JSON.stringify(result, null, 2));

if (errorRate > maxErrorRate || percentile(0.95) > maxP95Ms) {
  process.exitCode = 1;
}
