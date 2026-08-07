import { readFile } from "node:fs/promises";
import { resolve } from "node:path";
import { ApiClient, confirmedOrigin, requiredEnvironment } from "./http-client.mjs";

const argumentsMap = new Map(
  process.argv.slice(2).map((argument) => {
    const separator = argument.indexOf("=");
    return separator < 0 ? [argument, ""] : [argument.slice(0, separator), argument.slice(separator + 1)];
  }),
);
const apiUrl = requiredEnvironment("SKALA_API_BASE_URL");
const origin = confirmedOrigin(apiUrl, argumentsMap.get("--confirm-origin") || "");
const seedPath = resolve(argumentsMap.get("--seed") || "deploy/seed/catalog.example.json");
const adminId = requiredEnvironment("SKALA_ADMIN_ID");
const adminPassword = requiredEnvironment("SKALA_ADMIN_PASSWORD");
const seed = JSON.parse(await readFile(seedPath, "utf8"));

if (!Array.isArray(seed.categories) || !Array.isArray(seed.products)) {
  throw new Error("초기 데이터에는 categories와 products 배열이 필요합니다.");
}

const client = new ApiClient(origin);
await client.issueCsrf();
await client.request("/api/customers/login", {
  method: "POST",
  body: { customerId: adminId, customerPassword: adminPassword },
});

try {
  const categories = await client.request("/api/categories");
  const categoryByName = new Map(categories.map((category) => [category.name, category]));
  for (const category of seed.categories) {
    if (categoryByName.has(category.name)) {
      console.log(`category exists: ${category.name}`);
      continue;
    }
    const created = await client.request("/api/categories", { method: "POST", body: category });
    categoryByName.set(created.name, created);
    console.log(`category created: ${created.name}`);
  }

  const firstPage = await client.request("/api/products?page=0&size=100");
  const productNames = new Set((firstPage.content || []).map((product) => product.name));
  for (const product of seed.products) {
    if (productNames.has(product.productName)) {
      console.log(`product exists: ${product.productName}`);
      continue;
    }
    const categoryId = product.categoryName
      ? categoryByName.get(product.categoryName)?.id
      : undefined;
    if (product.categoryName && !categoryId) {
      throw new Error(`상품 '${product.productName}'의 카테고리를 찾을 수 없습니다: ${product.categoryName}`);
    }
    const { categoryName: _categoryName, ...body } = product;
    const created = await client.request("/api/products", {
      method: "POST",
      body: { ...body, categoryId },
    });
    productNames.add(created.name);
    console.log(`product created: ${created.name}`);
  }
} finally {
  await client.request("/api/customers/logout", { method: "POST" }).catch(() => {});
}

console.log(`catalog bootstrap completed: ${origin}`);
console.log("BOOTSTRAP_ADMIN_ENABLED=false와 비어 있는 관리자 bootstrap 자격 증명을 다시 확인하세요.");
