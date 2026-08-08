import { chromium } from "../../frontend/node_modules/playwright/index.mjs";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const browser = await chromium.launch({ headless: true });
try {
  for (const name of ["skala-shopping-erd-overview", "skala-shopping-erd-full"]) {
    const page = await browser.newPage({ viewport: { width: 1280, height: 720 }, deviceScaleFactor: 1 });
    await page.goto(pathToFileURL(path.join(here, `${name}.svg`)).href);
    const svg = page.locator("svg");
    await svg.screenshot({ path: path.join(here, `${name}.png`), animations: "disabled" });
    await page.close();
  }
} finally {
  await browser.close();
}
console.log("Rendered overview and full ERD PNG files.");
