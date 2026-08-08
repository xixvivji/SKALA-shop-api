import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));

const colors = {
  auth: "#7C3AED", member: "#2563EB", wallet: "#0891B2", catalog: "#EA580C",
  inventory: "#16A34A", cart: "#D97706", orders: "#DC2626", coupon: "#DB2777",
  wishlist: "#E11D48", reviews: "#9333EA", stockalert: "#0D9488", payment: "#4F46E5",
  returns: "#B45309", outbox: "#475569",
};

const C = (name, type, key = "") => ({ name, type, key });
const tables = [
  { id: "auth.accounts", schema: "auth", name: "accounts", columns: [
    C("id", "UUID", "PK"), C("login_id", "VARCHAR(100)", "UK"), C("password_hash", "VARCHAR(100)"),
    C("role", "VARCHAR(30)"), C("status", "VARCHAR(30)"), C("version", "BIGINT"),
    C("created_at", "TIMESTAMPTZ"), C("updated_at", "TIMESTAMPTZ"),
  ]},
  { id: "member.members", schema: "member", name: "members", columns: [
    C("id", "UUID", "PK"), C("customer_id", "VARCHAR(100)", "UK"), C("name", "VARCHAR(100)"),
    C("status", "VARCHAR(30)"), C("version", "BIGINT"), C("created_at", "TIMESTAMPTZ"), C("updated_at", "TIMESTAMPTZ"),
  ]},
  { id: "member.member_addresses", schema: "member", name: "member_addresses", columns: [
    C("id", "UUID", "PK"), C("member_id", "UUID", "FK"), C("address_name", "VARCHAR(50)", "UK"),
    C("recipient_name", "VARCHAR(100)"), C("phone_number", "VARCHAR(30)"), C("postal_code", "VARCHAR(20)"),
    C("address_line1", "VARCHAR(300)"), C("address_line2", "VARCHAR(300)"), C("is_default", "BOOLEAN"),
    C("version", "BIGINT"), C("created_at", "TIMESTAMPTZ"), C("updated_at", "TIMESTAMPTZ"),
  ]},
  { id: "wallet.point_accounts", schema: "wallet", name: "point_accounts", columns: [
    C("member_id", "UUID", "PK"), C("balance", "NUMERIC(19,2)"), C("version", "BIGINT"),
    C("created_at", "TIMESTAMPTZ"), C("updated_at", "TIMESTAMPTZ"),
  ]},
  { id: "wallet.point_transactions", schema: "wallet", name: "point_transactions", columns: [
    C("id", "UUID", "PK"), C("member_id", "UUID", "FK"), C("transaction_type", "VARCHAR(30)"),
    C("amount", "NUMERIC(19,2)"), C("balance_after", "NUMERIC(19,2)"), C("reference_id", "UUID"),
    C("command_id", "UUID", "UK"), C("created_at", "TIMESTAMPTZ"),
  ]},
  { id: "catalog.categories", schema: "catalog", name: "categories", columns: [
    C("id", "UUID", "PK"), C("name", "VARCHAR(100)", "UK"), C("description", "VARCHAR(500)"),
    C("status", "VARCHAR(30)"), C("version", "BIGINT"), C("created_at", "TIMESTAMPTZ"), C("updated_at", "TIMESTAMPTZ"),
  ]},
  { id: "catalog.products", schema: "catalog", name: "products", columns: [
    C("id", "UUID", "PK"), C("category_id", "UUID", "FK"), C("name", "VARCHAR(200)", "UK*"),
    C("price", "NUMERIC(19,2)"), C("description", "VARCHAR(2000)"), C("image_url", "VARCHAR(1000)"),
    C("status", "VARCHAR(30)"), C("version", "BIGINT"), C("created_at", "TIMESTAMPTZ"), C("updated_at", "TIMESTAMPTZ"),
  ]},
  { id: "catalog.product_variants", schema: "catalog", name: "product_variants", columns: [
    C("id", "UUID", "PK"), C("product_id", "UUID", "FK"), C("sku", "VARCHAR(100)", "UK"),
    C("option_name", "VARCHAR(50)"), C("option_value", "VARCHAR(100)"), C("additional_price", "NUMERIC(19,2)"),
    C("status", "VARCHAR(30)"), C("version", "BIGINT"), C("created_at", "TIMESTAMPTZ"), C("updated_at", "TIMESTAMPTZ"),
  ]},
  { id: "inventory.stocks", schema: "inventory", name: "stocks", columns: [
    C("product_id", "UUID", "PK"), C("available_quantity", "INTEGER"), C("status", "VARCHAR(30)"),
    C("version", "BIGINT"), C("created_at", "TIMESTAMPTZ"), C("updated_at", "TIMESTAMPTZ"),
  ]},
  { id: "inventory.stock_movements", schema: "inventory", name: "stock_movements", columns: [
    C("id", "UUID", "PK"), C("operation_id", "UUID", "UK"), C("product_id", "UUID", "FK"),
    C("movement_type", "VARCHAR(30)"), C("quantity", "INTEGER"), C("available_after", "INTEGER"),
    C("active_after", "BOOLEAN"), C("request_fingerprint", "VARCHAR(512)"), C("reason", "VARCHAR(200)"), C("created_at", "TIMESTAMPTZ"),
  ]},
  { id: "cart.carts", schema: "cart", name: "carts", columns: [
    C("member_id", "UUID", "PK"), C("created_at", "TIMESTAMPTZ"), C("updated_at", "TIMESTAMPTZ"),
  ]},
  { id: "cart.cart_items", schema: "cart", name: "cart_items", columns: [
    C("id", "UUID", "PK"), C("member_id", "UUID", "FK"), C("product_id", "UUID", "REF"),
    C("variant_id", "UUID", "REF"), C("quantity", "INTEGER"), C("created_at", "TIMESTAMPTZ"), C("updated_at", "TIMESTAMPTZ"),
  ]},
  { id: "orders.orders", schema: "orders", name: "orders", columns: [
    C("id", "UUID", "PK"), C("member_id", "UUID", "REF"), C("request_id", "UUID", "UK"), C("request_fingerprint", "VARCHAR(2048)"),
    C("order_number", "VARCHAR(50)", "UK"), C("status", "VARCHAR(30)"), C("fulfillment_status", "VARCHAR(30)"),
    C("original_amount", "NUMERIC(19,2)"), C("discount_amount", "NUMERIC(19,2)"), C("total_amount", "NUMERIC(19,2)"),
    C("point_used_amount", "NUMERIC(19,2)"), C("payment_amount", "NUMERIC(19,2)"), C("canceled_amount", "NUMERIC(19,2)"),
    C("balance_after", "NUMERIC(19,2)"), C("used_coupon_code", "VARCHAR(50)"), C("tracking_carrier", "VARCHAR(80)"),
    C("tracking_number", "VARCHAR(100)"), C("tracking_url", "VARCHAR(500)"), C("estimated_delivery_at", "TIMESTAMPTZ"),
    C("version", "BIGINT"), C("ordered_at", "TIMESTAMPTZ"), C("updated_at", "TIMESTAMPTZ"),
  ]},
  { id: "orders.order_items", schema: "orders", name: "order_items", columns: [
    C("id", "UUID", "PK"), C("order_id", "UUID", "FK"), C("line_number", "INTEGER", "UK"),
    C("product_id", "UUID", "REF"), C("variant_id", "UUID", "REF"), C("sku", "VARCHAR(100)"),
    C("product_name", "VARCHAR(200)"), C("option_name", "VARCHAR(50)"), C("option_value", "VARCHAR(100)"),
    C("unit_price", "NUMERIC(19,2)"), C("ordered_quantity", "INTEGER"), C("canceled_quantity", "INTEGER"),
    C("paid_amount", "NUMERIC(19,2)"), C("refunded_amount", "NUMERIC(19,2)"),
  ]},
  { id: "orders.order_cancellations", schema: "orders", name: "order_cancellations", columns: [
    C("id", "UUID", "PK"), C("command_id", "UUID", "UK"), C("member_id", "UUID", "REF"), C("product_id", "UUID", "REF"),
    C("quantity", "INTEGER"), C("refund_amount", "NUMERIC(19,2)"), C("request_fingerprint", "VARCHAR(128)"),
    C("balance_after", "NUMERIC(19,2)"), C("canceled_at", "TIMESTAMPTZ"),
  ]},
  { id: "orders.order_shipping_addresses", schema: "orders", name: "order_shipping_addresses", columns: [
    C("order_id", "UUID", "PK/FK"), C("recipient_name", "VARCHAR(100)"), C("phone_number", "VARCHAR(30)"),
    C("postal_code", "VARCHAR(20)"), C("address_line1", "VARCHAR(300)"), C("address_line2", "VARCHAR(300)"),
  ]},
  { id: "orders.order_status_histories", schema: "orders", name: "order_status_histories", columns: [
    C("id", "UUID", "PK"), C("order_id", "UUID", "FK"), C("from_status", "VARCHAR(30)"), C("to_status", "VARCHAR(30)"),
    C("changed_by", "UUID", "REF"), C("changed_at", "TIMESTAMPTZ"),
  ]},
  { id: "coupon.coupon_usages", schema: "coupon", name: "coupon_usages", columns: [
    C("id", "UUID", "PK"), C("coupon_id", "UUID", "REF"), C("coupon_code", "VARCHAR(50)"), C("member_id", "UUID", "REF"),
    C("order_id", "UUID", "FK/UK"), C("command_id", "UUID", "UK"), C("discount_amount", "NUMERIC(19,2)"), C("used_at", "TIMESTAMPTZ"),
  ]},
  { id: "wishlist.wishlist_items", schema: "wishlist", name: "wishlist_items", columns: [
    C("id", "UUID", "PK"), C("member_id", "UUID", "REF"), C("product_id", "UUID", "REF"), C("created_at", "TIMESTAMPTZ"), C("version", "BIGINT"),
  ]},
  { id: "reviews.product_reviews", schema: "reviews", name: "product_reviews", columns: [
    C("id", "UUID", "PK"), C("member_id", "UUID", "REF"), C("product_id", "UUID", "REF"), C("rating", "INTEGER"),
    C("comment", "VARCHAR(2000)"), C("created_at", "TIMESTAMPTZ"), C("updated_at", "TIMESTAMPTZ"), C("version", "BIGINT"),
  ]},
  { id: "stockalert.stock_alert_subscriptions", schema: "stockalert", name: "stock_alert_subscriptions", columns: [
    C("id", "UUID", "PK"), C("member_id", "UUID", "REF"), C("product_id", "UUID", "REF"), C("created_at", "TIMESTAMPTZ"),
    C("notified_at", "TIMESTAMPTZ"), C("available_quantity_at_notification", "INTEGER"), C("version", "BIGINT"),
  ]},
  { id: "payment.payments", schema: "payment", name: "payments", columns: [
    C("id", "UUID", "PK"), C("order_id", "UUID", "REF/UK"), C("member_id", "UUID", "REF"), C("prepare_command_id", "UUID", "UK"),
    C("prepare_fingerprint", "VARCHAR(300)"), C("approve_command_id", "UUID"), C("approve_fingerprint", "VARCHAR(80)"),
    C("approve_result_status", "VARCHAR(30)"), C("approve_result_failure_code", "VARCHAR(50)"),
    C("approve_result_failure_message", "VARCHAR(200)"), C("approve_result_transaction_id", "VARCHAR(100)"),
    C("approve_result_approved_amount", "NUMERIC(19,2)"), C("approve_result_refunded_amount", "NUMERIC(19,2)"),
    C("approve_result_approved_at", "TIMESTAMPTZ"), C("provider", "VARCHAR(30)"),
    C("provider_transaction_id", "VARCHAR(100)"), C("method", "VARCHAR(30)"), C("masked_number", "VARCHAR(30)"),
    C("requested_amount", "NUMERIC(19,2)"), C("approved_amount", "NUMERIC(19,2)"), C("refunded_amount", "NUMERIC(19,2)"),
    C("status", "VARCHAR(30)"), C("failure_code", "VARCHAR(50)"), C("failure_message", "VARCHAR(200)"),
    C("approved_at", "TIMESTAMPTZ"), C("created_at", "TIMESTAMPTZ"), C("updated_at", "TIMESTAMPTZ"), C("version", "BIGINT"),
  ]},
  { id: "payment.payment_refunds", schema: "payment", name: "payment_refunds", columns: [
    C("id", "UUID", "PK"), C("payment_id", "UUID", "FK"), C("command_id", "UUID", "UK"), C("amount", "NUMERIC(19,2)"), C("created_at", "TIMESTAMPTZ"),
  ]},
  { id: "payment.payment_webhook_events", schema: "payment", name: "payment_webhook_events", columns: [
    C("event_id", "UUID", "PK"), C("payment_id", "UUID", "FK"), C("event_type", "VARCHAR(50)"), C("processed_at", "TIMESTAMPTZ"),
  ]},
  { id: "returns.return_requests", schema: "returns", name: "return_requests", columns: [
    C("id", "UUID", "PK"), C("command_id", "UUID", "UK"), C("member_id", "UUID", "REF"), C("order_id", "UUID", "REF"),
    C("order_item_id", "UUID", "REF"), C("product_id", "UUID", "REF"), C("product_name", "VARCHAR(200)"), C("quantity", "INTEGER"),
    C("reason", "VARCHAR(50)"), C("evidence_image_url", "VARCHAR(1000)"), C("status", "VARCHAR(30)"),
    C("gross_refund_amount", "NUMERIC(19,2)"), C("shipping_fee", "NUMERIC(19,2)"), C("refund_amount", "NUMERIC(19,2)"),
    C("point_refund_amount", "NUMERIC(19,2)"), C("payment_refund_amount", "NUMERIC(19,2)"), C("balance_after", "NUMERIC(19,2)"),
    C("admin_note", "VARCHAR(500)"), C("requested_at", "TIMESTAMPTZ"), C("updated_at", "TIMESTAMPTZ"),
    C("processed_by", "UUID", "REF"), C("version", "BIGINT"),
  ]},
  { id: "returns.return_status_commands", schema: "returns", name: "return_status_commands", columns: [
    C("command_id", "UUID", "PK"), C("return_id", "UUID", "FK"), C("admin_id", "UUID", "REF"),
    C("requested_status", "VARCHAR(30)"), C("requested_admin_note", "VARCHAR(500)"),
    C("result_status", "VARCHAR(30)"), C("result_balance_after", "NUMERIC(19,2)"),
    C("result_admin_note", "VARCHAR(500)"), C("result_updated_at", "TIMESTAMPTZ"),
  ]},
  { id: "outbox.outbox_events", schema: "outbox", name: "outbox_events", columns: [
    C("id", "UUID", "PK"), C("aggregate_type", "VARCHAR(100)"), C("aggregate_id", "UUID", "REF"),
    C("event_type", "VARCHAR(200)"), C("payload", "TEXT"), C("status", "VARCHAR(20)"), C("retry_count", "INTEGER"),
    C("next_attempt_at", "TIMESTAMPTZ"), C("occurred_at", "TIMESTAMPTZ"), C("published_at", "TIMESTAMPTZ"), C("last_error", "VARCHAR(1000)"),
  ]},
];

const physical = [
  ["wallet.point_accounts", "wallet.point_transactions", "member_id", "1:N"],
  ["orders.orders", "orders.order_items", "order_id", "1:N"],
  ["inventory.stocks", "inventory.stock_movements", "product_id", "1:N"],
  ["member.members", "member.member_addresses", "member_id", "1:N"],
  ["cart.carts", "cart.cart_items", "member_id", "1:N"],
  ["catalog.categories", "catalog.products", "category_id", "1:N"],
  ["catalog.products", "catalog.product_variants", "product_id", "1:N"],
  ["orders.orders", "orders.order_shipping_addresses", "order_id", "1:0..1"],
  ["orders.orders", "orders.order_status_histories", "order_id", "1:N"],
  ["orders.orders", "coupon.coupon_usages", "order_id", "1:0..1"],
  ["payment.payments", "payment.payment_refunds", "payment_id", "1:N"],
  ["payment.payments", "payment.payment_webhook_events", "payment_id", "1:N"],
  ["returns.return_requests", "returns.return_status_commands", "return_id", "1:N"],
];

const logical = [
  ["member.members", "auth.accounts", "shared id", "1:1"],
  ["member.members", "wallet.point_accounts", "member_id", "1:1"],
  ["member.members", "cart.carts", "member_id", "1:1"],
  ["member.members", "orders.orders", "member_id", "1:N"],
  ["member.members", "orders.order_cancellations", "member_id", "1:N"],
  ["member.members", "wishlist.wishlist_items", "member_id", "1:N"],
  ["member.members", "reviews.product_reviews", "member_id", "1:N"],
  ["member.members", "stockalert.stock_alert_subscriptions", "member_id", "1:N"],
  ["member.members", "payment.payments", "member_id", "1:N"],
  ["member.members", "returns.return_requests", "member_id", "1:N"],
  ["catalog.products", "inventory.stocks", "product_id", "1:1"],
  ["catalog.products", "cart.cart_items", "product_id", "1:N"],
  ["catalog.product_variants", "cart.cart_items", "variant_id", "1:N"],
  ["catalog.products", "orders.order_items", "product_id", "1:N"],
  ["catalog.product_variants", "orders.order_items", "variant_id", "1:N"],
  ["catalog.products", "wishlist.wishlist_items", "product_id", "1:N"],
  ["catalog.products", "reviews.product_reviews", "product_id", "1:N"],
  ["catalog.products", "stockalert.stock_alert_subscriptions", "product_id", "1:N"],
  ["orders.orders", "payment.payments", "order_id", "1:0..1"],
  ["orders.orders", "returns.return_requests", "order_id", "1:N"],
  ["orders.order_items", "returns.return_requests", "order_item_id", "1:N"],
  ["catalog.products", "returns.return_requests", "product_id", "1:N"],
];

const esc = (s) => String(s).replace(/[&<>\"]/g, (m) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[m]));
const defs = `
<defs>
  <filter id="shadow" x="-20%" y="-20%" width="140%" height="140%"><feDropShadow dx="0" dy="8" stdDeviation="12" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#FFFFFF"/><stop offset="1" stop-color="#F6F9FD"/></linearGradient>
</defs>`;

function svgShell(width, height, title, subtitle, body) {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">
${defs}<rect width="100%" height="100%" fill="url(#bg)"/>
<text x="70" y="82" font-family="Inter, Arial, sans-serif" font-size="44" font-weight="800" fill="#14213D">${esc(title)}</text>
<text x="72" y="126" font-family="Inter, Arial, sans-serif" font-size="20" fill="#64748B">${esc(subtitle)}</text>
<g font-family="Inter, Arial, sans-serif">${body}</g></svg>`;
}

function pill(x, y, text, color) {
  const w = 18 + text.length * 8;
  return `<rect x="${x}" y="${y - 17}" width="${w}" height="23" rx="11" fill="${color}" opacity="0.13"/><text x="${x + w / 2}" y="${y}" text-anchor="middle" font-size="12" font-weight="800" fill="${color}">${esc(text)}</text>`;
}

const placements = {
  "auth.accounts": [70, 210], "member.members": [70, 650], "member.member_addresses": [70, 1030],
  "wallet.point_accounts": [70, 1660], "wallet.point_transactions": [70, 2040],
  "catalog.categories": [870, 210], "catalog.products": [870, 650], "catalog.product_variants": [870, 1160],
  "inventory.stocks": [870, 1740], "inventory.stock_movements": [870, 2160],
  "cart.carts": [870, 2740], "cart.cart_items": [870, 3040],
  "wishlist.wishlist_items": [1670, 210], "reviews.product_reviews": [1670, 590],
  "stockalert.stock_alert_subscriptions": [1670, 1060], "outbox.outbox_events": [1670, 1560],
  "orders.orders": [2470, 210], "orders.order_items": [2470, 1010], "orders.order_cancellations": [2470, 1570],
  "orders.order_shipping_addresses": [2470, 2070], "orders.order_status_histories": [2470, 2500],
  "coupon.coupon_usages": [2470, 2930],
  "payment.payments": [3270, 210], "payment.payment_refunds": [3270, 1190], "payment.payment_webhook_events": [3270, 1510],
  "returns.return_requests": [3270, 1810], "returns.return_status_commands": [3270, 2610],
};

function tableBox(table) {
  const [x, y] = placements[table.id];
  const w = 700, row = 27, header = 68, h = header + table.columns.length * row + 18;
  table.box = { x, y, w, h };
  const color = colors[table.schema];
  let out = `<g filter="url(#shadow)"><rect x="${x}" y="${y}" width="${w}" height="${h}" rx="17" fill="#FFFFFF" stroke="#CBD5E1" stroke-width="1.5"/>
  <path d="M ${x + 17} ${y} H ${x + w - 17} Q ${x + w} ${y} ${x + w} ${y + 17} V ${y + header} H ${x} V ${y + 17} Q ${x} ${y} ${x + 17} ${y}" fill="${color}"/>
  <circle cx="${x + 31}" cy="${y + 28}" r="12" fill="#FFFFFF" opacity="0.24"/><text x="${x + 31}" y="${y + 33}" text-anchor="middle" font-size="14" font-weight="800" fill="#FFFFFF">DB</text>
  <text x="${x + 54}" y="${y + 25}" font-size="14" font-weight="700" fill="#FFFFFF" opacity="0.8">${esc(table.schema)}</text>
  <text x="${x + 54}" y="${y + 51}" font-size="24" font-weight="800" fill="#FFFFFF">${esc(table.name)}</text>`;
  table.columns.forEach((c, i) => {
    const cy = y + header + (i + 1) * row - 8;
    if (i % 2 === 1) out += `<rect x="${x + 1}" y="${y + header + i * row}" width="${w - 2}" height="${row}" fill="#F8FAFC"/>`;
    let px = x + 18;
    if (c.key) { out += pill(px, cy, c.key, c.key.includes("FK") ? "#2563EB" : c.key.includes("REF") ? "#64748B" : color); px += 18 + c.key.length * 8 + 9; }
    out += `<text x="${px}" y="${cy}" font-size="15" font-weight="${c.key ? 700 : 500}" fill="#1E293B">${esc(c.name)}</text>
      <text x="${x + w - 18}" y="${cy}" text-anchor="end" font-size="14" fill="#64748B">${esc(c.type)}</text>`;
  });
  return out + `</g>`;
}

function relationPath(from, to, label, cardinality, kind) {
  const a = from.box, b = to.box;
  const acx = a.x + a.w / 2, acy = a.y + a.h / 2, bcx = b.x + b.w / 2, bcy = b.y + b.h / 2;
  let x1, y1, x2, y2, d;
  if (Math.abs(bcx - acx) > Math.abs(bcy - acy)) {
    const right = bcx > acx;
    x1 = right ? a.x + a.w : a.x; y1 = acy; x2 = right ? b.x : b.x + b.w; y2 = bcy;
    const bend = Math.max(55, Math.abs(x2 - x1) * 0.45);
    d = `M${x1},${y1} C${x1 + (right ? bend : -bend)},${y1} ${x2 + (right ? -bend : bend)},${y2} ${x2},${y2}`;
  } else {
    const down = bcy > acy;
    x1 = acx; y1 = down ? a.y + a.h : a.y; x2 = bcx; y2 = down ? b.y : b.y + b.h;
    const bend = Math.max(55, Math.abs(y2 - y1) * 0.45);
    d = `M${x1},${y1} C${x1},${y1 + (down ? bend : -bend)} ${x2},${y2 + (down ? -bend : bend)} ${x2},${y2}`;
  }
  const mx = (x1 + x2) / 2, my = (y1 + y2) / 2;
  const stroke = kind === "physical" ? "#334155" : "#94A3B8";
  const dash = kind === "physical" ? "" : ` stroke-dasharray="10 8"`;
  return `<path d="${d}" fill="none" stroke="#FFFFFF" stroke-width="8" opacity="0.92"/><path d="${d}" fill="none" stroke="${stroke}" stroke-width="2.5"${dash}/>
    <circle cx="${x1}" cy="${y1}" r="5" fill="${stroke}"/><circle cx="${x2}" cy="${y2}" r="5" fill="#FFFFFF" stroke="${stroke}" stroke-width="2"/>
    <rect x="${mx - 64}" y="${my - 13}" width="128" height="25" rx="12" fill="#FFFFFF" stroke="#E2E8F0"/>
    <text x="${mx}" y="${my + 4}" text-anchor="middle" font-size="12" font-weight="700" fill="${stroke}">${esc(label)} · ${esc(cardinality)}</text>`;
}

function generateFull() {
  tables.forEach(tableBox);
  const byId = Object.fromEntries(tables.map((t) => [t.id, t]));
  const edges = [
    ...logical.map((r) => relationPath(byId[r[0]], byId[r[1]], r[2], r[3], "logical")),
    ...physical.map((r) => relationPath(byId[r[0]], byId[r[1]], r[2], r[3], "physical")),
  ].join("");
  const nodes = tables.map(tableBox).join("");
  const legend = `<g transform="translate(70 3650)">
    <rect width="3980" height="90" rx="18" fill="#FFFFFF" stroke="#CBD5E1"/>
    <text x="28" y="37" font-size="18" font-weight="800" fill="#14213D">RELATION LEGEND</text>
    <line x1="245" y1="31" x2="325" y2="31" stroke="#334155" stroke-width="3"/><text x="340" y="37" font-size="16" fill="#334155">Physical FK constraint</text>
    <line x1="590" y1="31" x2="670" y2="31" stroke="#94A3B8" stroke-width="3" stroke-dasharray="10 8"/><text x="685" y="37" font-size="16" fill="#64748B">Logical application reference</text>
    <text x="28" y="69" font-size="14" fill="#64748B">PK primary key · FK database foreign key · REF UUID reference without FK · UK unique key · generated from Flyway V1–V26</text>
  </g>`;
  return svgShell(4120, 3790, "SKALA Shopping · Full ERD", "14 schemas · 27 tables · Flyway V1–V26 · PostgreSQL 17", edges + nodes + legend);
}

const schemas = [
  ["auth", ["accounts"]], ["member", ["members", "member_addresses"]], ["wallet", ["point_accounts", "point_transactions"]],
  ["catalog", ["categories", "products", "product_variants"]], ["inventory", ["stocks", "stock_movements"]], ["cart", ["carts", "cart_items"]],
  ["wishlist", ["wishlist_items"]], ["reviews", ["product_reviews"]], ["stockalert", ["stock_alert_subscriptions"]],
  ["orders", ["orders", "order_items", "order_cancellations", "order_shipping_addresses", "order_status_histories"]],
  ["coupon", ["coupon_usages"]], ["payment", ["payments", "payment_refunds", "payment_webhook_events"]],
  ["returns", ["return_requests", "return_status_commands"]], ["outbox", ["outbox_events"]],
];

const overviewPos = {
  auth: [80, 230], member: [390, 230], wallet: [80, 520], catalog: [760, 230], inventory: [760, 550],
  cart: [760, 870], wishlist: [1080, 870], reviews: [1080, 230], stockalert: [1080, 550],
  orders: [1430, 390], coupon: [1800, 230], payment: [1800, 520], returns: [1800, 820], outbox: [1430, 980],
};

function overviewBox(schema, list) {
  const [x, y] = overviewPos[schema], w = schema === "orders" ? 330 : 280, h = 82 + list.length * 27;
  const color = colors[schema];
  const node = { box: { x, y, w, h } };
  let out = `<g filter="url(#shadow)"><rect x="${x}" y="${y}" width="${w}" height="${h}" rx="18" fill="#FFFFFF" stroke="${color}" stroke-width="2"/>
  <rect x="${x}" y="${y}" width="${w}" height="58" rx="18" fill="${color}"/><rect x="${x}" y="${y + 40}" width="${w}" height="18" fill="${color}"/>
  <circle cx="${x + 30}" cy="${y + 29}" r="15" fill="#FFFFFF" opacity="0.2"/><text x="${x + 30}" y="${y + 35}" text-anchor="middle" font-size="15" font-weight="800" fill="#FFFFFF">DB</text>
  <text x="${x + 55}" y="${y + 36}" font-size="22" font-weight="800" fill="#FFFFFF">${esc(schema)}</text>`;
  list.forEach((name, i) => out += `<circle cx="${x + 24}" cy="${y + 79 + i * 27}" r="4" fill="${color}"/><text x="${x + 38}" y="${y + 85 + i * 27}" font-size="15" font-weight="600" fill="#334155">${esc(name)}</text>`);
  out += `</g>`;
  return { node, out };
}

function schemaEdge(nodes, a, b, label, physicalEdge = false) {
  return relationPath(nodes[a], nodes[b], label, "", physicalEdge ? "physical" : "logical");
}

function generateOverview() {
  const rendered = schemas.map(([s, list]) => [s, overviewBox(s, list)]);
  const nodes = Object.fromEntries(rendered.map(([s, r]) => [s, r.node]));
  const edgeSpecs = [
    ["member", "auth", "identity"], ["member", "wallet", "points"], ["member", "cart", "shopping"], ["member", "orders", "purchase"],
    ["member", "wishlist", "wishlist"], ["member", "reviews", "review"], ["member", "stockalert", "alert"],
    ["catalog", "inventory", "stock"], ["catalog", "cart", "cart item"], ["catalog", "orders", "order item"],
    ["catalog", "wishlist", "product"], ["catalog", "reviews", "product"], ["catalog", "stockalert", "product"],
    ["orders", "coupon", "discount", true], ["orders", "payment", "payment"], ["orders", "returns", "return"], ["orders", "outbox", "events"],
  ];
  const edges = edgeSpecs.map(([a, b, l, p]) => schemaEdge(nodes, a, b, l, p)).join("");
  const cards = rendered.map(([, r]) => r.out).join("");
  const bands = `<rect x="48" y="175" width="590" height="760" rx="25" fill="#EFF6FF" stroke="#BFDBFE"/><text x="72" y="210" font-size="16" font-weight="800" fill="#1D4ED8">IDENTITY &amp; CUSTOMER</text>
    <rect x="700" y="175" width="670" height="920" rx="25" fill="#FFF7ED" stroke="#FED7AA"/><text x="724" y="210" font-size="16" font-weight="800" fill="#C2410C">PRODUCT &amp; ENGAGEMENT</text>
    <rect x="1385" y="175" width="720" height="1020" rx="25" fill="#FEF2F2" stroke="#FECACA"/><text x="1409" y="210" font-size="16" font-weight="800" fill="#B91C1C">ORDER &amp; SETTLEMENT</text>`;
  const legend = `<g transform="translate(80 1285)"><rect width="1940" height="105" rx="18" fill="#FFFFFF" stroke="#CBD5E1"/>
    <text x="28" y="40" font-size="18" font-weight="800" fill="#14213D">DOMAIN OVERVIEW</text>
    <text x="230" y="40" font-size="16" fill="#64748B">14 schemas · 27 tables · Modular Monolith</text>
    <line x1="720" y1="35" x2="800" y2="35" stroke="#334155" stroke-width="3"/><text x="815" y="41" font-size="15" fill="#334155">Physical FK</text>
    <line x1="1030" y1="35" x2="1110" y2="35" stroke="#94A3B8" stroke-width="3" stroke-dasharray="10 8"/><text x="1125" y="41" font-size="15" fill="#64748B">Logical reference</text>
    <text x="28" y="77" font-size="14" fill="#64748B">Domain colors follow schema ownership. Outbox publishes domain events to Kafka; Elasticsearch remains outside the relational ERD.</text></g>`;
  return svgShell(2160, 1430, "SKALA Shopping · Domain ERD", "PostgreSQL 17 · schema-level relationship overview", bands + edges + cards + legend);
}

fs.writeFileSync(path.join(here, "skala-shopping-erd-overview.svg"), generateOverview());
fs.writeFileSync(path.join(here, "skala-shopping-erd-full.svg"), generateFull());
console.log("Generated overview and full ERD SVG files.");
