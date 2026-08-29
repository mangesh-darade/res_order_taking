# Restaurant Order Taking — End-to-End Testing Flow

**App:** Captain / Waiter Android (`res_order_taking`)  
**API:** `http://…/ElintOm/ordertakingapi/`  
**Purpose:** Full functional test from app start → table colors → KOT/KDS → cancel → bill → free/available.

Staff / full product guide (kay aahe, kasa chaltoy): [ORDER_TAKING_MANUAL_GUIDE.md](./ORDER_TAKING_MANUAL_GUIDE.md)

---

## 1. Setup (before testing)

| Check | Expected |
|-------|----------|
| WAMP / PHP API up | `GET …/ordertakingapi/branding` → SUCCESS |
| App Settings → Base URL | Emulator: `http://10.0.2.2/ElintOm/ordertakingapi/` · Device: PC LAN IP |
| Cleartext HTTP | Allowed (`usesCleartextTraffic=true`) |
| Captain login | Optional admin for settings; floor works after sections load |

---

## 2. App start → navigation map

```text
Launch
  → Sections tab
      → Select Section (dropdown)
      → Tap Subsection card
  → Tables tab (auto / tap)
      → Table cards (color = status)
  → Tap table → Orders tab
      → Add items (Menu) → KOT → Ready/Served → Finalize
```

| Screen | Main actions |
|--------|----------------|
| **Sections** | Pick section + subsection |
| **Tables** | See colors; tap = open order; long-press = Reserve / Free / Transfer / Merge / Mark Available |
| **Orders** | Guests, items, qty, KOT, Served, Finalize |
| **Menu** | Add products + customizations |
| **Finalize** | Cash bill → sale |

---

## 3. Table status → color → icons (Android)

| API `status` | UI label | Card color | Hex | Icons | How you get it |
|--------------|----------|------------|-----|-------|----------------|
| `available` | Available | White | `#FFFFFF` | None | Empty / after finalize / Mark Available |
| `reserved` | Reserved | Yellow | `#FFFF99` | Clock | Long-press Available/Free → Reserve |
| `occupied` | Occupied | Blue | `#A2E5FF` | Plus + Guest | Create order / pending items only |
| `order-placed` | Order Placed | Pink | `#FF7EB6` | Plus + Guest | After KOT (kitchen has items) |
| `ready` | Order Ready | Green | `#C8E6C9` | Dish + Guest | Any item Ready (partial OK) |
| `served` | Served | Pink | `#FF7EB6` | Plus + Guest | All active items Served |
| `free` | Free | Red tint | `#FFCDD2` | Bill + Guest | Long-press active → **Free table** |

**Footer legend chips (left → right):** White · Blue · Pink (order-placed+served) · Green · Yellow · Red  

**Note:** Colors are hardcoded in `TableCard.kt` from `status` string — not from DB color names.

---

## 4. Item status lifecycle

| Item `status` | Meaning | Qty edit | Cancel (qty → 0) | Bill |
|---------------|---------|----------|------------------|------|
| `pending` | Not sent to kitchen | Yes | Hard **delete** row | Included |
| `kot` | On KOT / KDS | Yes (syncs KDS) | Mark **Cancelled** + KDS CANCELLED | Excluded after cancel |
| `ready` | Kitchen ready | Blocked (except cancel) | Cancelled + KDS | Excluded after cancel |
| `served` | Delivered to guest | Blocked (except cancel) | Cancelled + KDS | Excluded after cancel |
| `cancelled` | Cancelled after KOT+ | Locked | Already done | Excluded |

**UI after KOT cancel:** Item stays with qty (e.g. ×1), shows **CANCELLED** (not `0`). Total drops. KDS shows CANCELLED.

---

## 5. Master test flow (happy path)

Use one empty table (e.g. **AC-02** or **T-03**).

| # | App action | Expected table status | Color | DB / side effects |
|---|------------|----------------------|-------|-------------------|
| 1 | Sections → pick section/subsection → Tables | `available` | White | — |
| 2 | Tap table → Orders (create if needed) | `occupied` | Blue | `sma_res_orders` insert; `sma_res_tables.status_id` → Occupied |
| 3 | Guests +/− | still `occupied` | Blue | `guest_count` / guests rows |
| 4 | Menu → add 2 items | `occupied` | Blue | `sma_res_orders_items` status=`pending` |
| 5 | Tap **KOT** | `order-placed` | Pink | Items → `kot`; `suspended_bills` KOT ticket; KDS shows ticket |
| 6 | (KDS / API) Mark item1 Ready | `ready` | Green | Item1 `ready`; others may stay `kot` |
| 7 | Orders → Served (or mark all served) | `served` | Pink | All active items `served` |
| 8 | **Finalize Order** → pay | `available` | White | Order Completed; `sma_sales` (+ items); table Available |
| 9 | (Alt) Before finalize: long-press → **Free table** | `free` | Red | Open orders closed; status Free |
| 10 | Long-press Free → **Mark Available** | `available` | White | status Available |

---

## 6. Extra flows to test

### 6.1 Reserve

| Step | Action | Status | Color |
|------|--------|--------|-------|
| 1 | Long-press Available → Reserve (name + until) | `reserved` | Yellow + Clock |
| 2 | Unreserve / expire | `available` | White |
| 3 | Reserved → Start order | `occupied` | Blue |

### 6.2 Second KOT (add after first send)

| Step | Action | Expect |
|------|--------|--------|
| 1 | After KOT, add new item | New item `pending`; table may stay pink if others still kot/ready |
| 2 | KOT again | Only pending → kot; Ready/Served untouched |
| 3 | Double KOT with no pending | Idempotent success; no reset |

### 6.3 Cancel after KOT

| Step | Action | App | KDS |
|------|--------|-----|-----|
| 1 | Orders → item qty − to 0 | Item **CANCELLED** badge, qty locked, Total ↓ | Line **CANCELLED** |
| 2 | Pending item qty − to 0 | Item **removed** | No KDS line (never sent) |

### 6.4 Transfer / Merge

| Action | How | Expect |
|--------|-----|--------|
| Transfer | Long-press occupied+ → Transfer → tap empty table | Order moves; source freed/available; target occupied+ |
| Merge | Long-press → Merge → tap other table | Bills combined on target |

### 6.5 Stock (POS `Settings.overselling`)

| Check | Expect |
|-------|--------|
| `overselling=1` (default) | Low/Zero stock warning; **ADD allowed** |
| `overselling=0` (System Settings / POS) | `in_stock=false` when qty≤0; ADD disabled; API `Out of stock` |
| Branding | `overselling` + derived `strict_stock` (`1` when overselling off) |

Change stock policy only in **ElintOm System Settings → Overselling** — no separate app API.

### 6.6 Customize (veg / masters)

| Check | Expect |
|-------|--------|
| Veg (`meal_type_id=1` in `sma_res_product_details`) | No **Meat Wellness** |
| Non-veg (`meal_type_id=2`) | Meat Wellness shown |
| Add-ons / toppings | From master tables |
| Custom allergy + | DB save + selected |

### 6.7 Offline / multi-device

| Check | Expect |
|-------|--------|
| Create order on table that already has open order | API returns existing order (no duplicate) |
| Offline ADD then online | Queue syncs; ID maps persist across restart |
| Double FINALIZE / KOT from 2 devices | Second treated as done / dropped, queue not stuck |
| Stale queue > 72h | Dropped on sync |

---

## 7. Tables / columns affected

### 7.1 Floor & status

| Table | Key columns | When updated |
|-------|-------------|--------------|
| `sma_res_sections` | id, name | Read-only in app |
| `sma_res_subsections` | id, section_id, name | Read-only |
| `sma_res_tables` | id, section_id, subsection_id, **status_id**, reserved_by, reserved_until, reserved_note | Reserve / free / available / occupied / derive |
| `sma_res_table_status` | id, name, value, color | Lookup (Available, Reserved, Occupied, Order Placed, Order Ready, Served, Free) |

### 7.2 Orders

| Table | Key columns | When updated |
|-------|-------------|--------------|
| `sma_res_orders` | id, res_tables_id, guest_count, **status**, waiter… | create / complete / free |
| `sma_res_orders_guests` | id, res_orders_id | guest +/− |
| `sma_res_orders_items` | id, res_orders_id, product, qty, price, **status**, customizations | add / qty / KOT / ready / served / cancel |

**Order header statuses (examples):** `active`, `KOT Sent`, `Completed`, …  
**Item statuses:** `pending` → `kot` → `Ready`/`ready` → `Served`/`served` · or `Cancelled`

### 7.3 Kitchen (KOT → KDS)

| Table | Role |
|-------|------|
| `sma_suspended_bills` | KOT ticket (`reference_no` like `KOT-{order_id}`) |
| `sma_suspended_items` | KDS lines; note may include `ROI:{item_id}`; cancel flagged in note/UI |

### 7.4 Billing

| Table | Role |
|-------|------|
| `sma_sales` | Finalized sale |
| `sma_sale_items` | Bill lines (cancelled items skipped) |
| Payments / related | Per finalize payment method |

### 7.5 Menu / stock (read + soft warn)

| Table | Role |
|-------|------|
| `sma_products` | Menu |
| `sma_res_product_details` | Restaurant product flags |
| `sma_warehouses_products` | `stock_qty` / warning (not hard-block by default) |
| Add-ons / toppings / allergies / meat wellness | Customization masters |

---

## 8. API cheat sheet (manual / Postman)

Base: `/ordertakingapi/`

| Step | Method | Endpoint | Main fields |
|------|--------|----------|-------------|
| Sections | GET | `sections` | — |
| Tables | GET/POST | `tables` | `section_id`, `subsection_id` |
| Reserve | POST | `reserve_table` | `table_id`, `reserved_by`, `reserved_until` |
| Unreserve | POST | `unreserve_table` | `table_id` |
| Free | POST | `free_table` | `table_id` → status **free** |
| Available | POST | `mark_available` | `table_id` → **available** |
| Create order | POST | `create_order` | `table_id`, `guest_count` |
| Menu | POST | `menu_items` | — |
| Add item | POST | `add_item` | `order_id`, `product_id`, `quantity`, guest… |
| Update qty | POST | `update_item` | `item_id`, `quantity` |
| Cancel / delete | POST | `delete_item` | `item_id` |
| KOT | POST | `update_kot_status` | `order_id` |
| Ready | POST | `mark_ready` | `order_id`, `item_id` (optional; omit = all kot) |
| Served | POST | `mark_served` | `order_id`, `item_id` (optional) |
| Finalize | POST | `finalize_order` | `order_id`, payment… |
| Transfer | POST | `transfer_table` | `from_table_id`, `to_table_id` |
| Merge | POST | `merge_tables` | `from_table_id`, `to_table_id` |

**Verify after each step:**

```http
GET/POST tables?section_id={id}
→ data[].status, guests_count, order_id, reserved_*
```

---

## 9. Status derive rules (`tables` API)

With **open order**, status from **items** (cancelled ignored):

| Condition | Table status |
|-----------|--------------|
| No active items | `occupied` |
| Any `ready` | `ready` |
| Else any `kot` | `order-placed` |
| Else any `pending` | `occupied` |
| All active = `served` | `served` |

No open order → keep `available` / `free` / `reserved` from DB.

---

## 10. Pass / fail checklist

| Area | Pass if |
|------|---------|
| Sections load | Branding + section list |
| Table colors | Match section 3 for each status |
| Create → Occupied | Blue + guest icon |
| KOT → Pink | KDS ticket appears |
| Partial Ready → Green | Other items stay kot |
| Cancel KOT item | App CANCELLED + KDS CANCELLED + Total ↓ |
| All Served → Pink served | — |
| Finalize → White available | Sale created |
| Free → Red | Then Mark Available → White |
| Reserve → Yellow | Clock + until |
| 2nd KOT | Only pending fires |
| Transfer/Merge | Orders/tables consistent |

---

## 11. Known behaviors (not bugs)

| Behavior | Why |
|----------|-----|
| Cancelled item qty not `0` | Row kept for kitchen audit; UI shows CANCELLED |
| Finalize → Available (not Free) | Free is manual cleaning state |
| Footer pink = order-placed **+** served | Same card pink |
| Stock warning but ADD works | Soft stock policy |
| Order Placed & Served same pink | Spec |

---

## 12. Quick SQL sanity (optional)

```sql
-- Table floor
SELECT t.id, t.name, ts.value AS status, t.reserved_by
FROM sma_res_tables t
LEFT JOIN sma_res_table_status ts ON ts.id = t.status_id;

-- Open order items
SELECT o.id AS order_id, o.res_tables_id, i.id AS item_id, i.status, i.quantity
FROM sma_res_orders o
JOIN sma_res_orders_items i ON i.res_orders_id = o.id
WHERE o.status NOT IN ('Completed','Cancelled');
```

---

*Doc generated for Captain Order Taking + Ordertakingapi + KDS path. Update when Phase 3 billing / new statuses land.*
