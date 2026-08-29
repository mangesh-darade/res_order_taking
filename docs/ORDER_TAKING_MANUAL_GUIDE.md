# Restaurant Order Taking — Manual Guide

**App:** Captain / Waiter Android (`res_order_taking`)  
**Backend:** ElintOm `ordertakingapi`  
**Audience:** Captain, waiter, floor staff, and anyone who needs to know *kay aahe / kasa chaltoy*

Related doc (tester checklist): [ORDER_TAKING_TESTING_FLOW.md](./ORDER_TAKING_TESTING_FLOW.md)

---

## 1. System madhe kay aahe? (Big picture)

| Piece | Role |
|-------|------|
| **Android app** | Floor: sections → tables → orders → menu → KOT → bill |
| **ElintOm API** | All business logic, DB, stock policy, KOT/KDS sync |
| **Kitchen (KDS)** | Sees KOT tickets after you tap **KOT** |
| **POS Settings** | Stock rule = **Overselling** only (System Settings) |

**Happy path (ek table):**  
Table select → guests set → items add → **KOT** → kitchen Ready → **Served** → **Finalize** → table Available

```text
Sections → Tables → Orders → Menu (add/edit items)
                ↓
         KOT → Ready → Served → Finalize (bill)
```

---

## 2. App screens (kay kay screens)

| Screen / Tab | Kaam |
|--------------|------|
| **Login** | Captain/waiter login (optional for some setups) |
| **Sections** | Area choose (AC, Outdoor…) + subsection |
| **Tables** | Table cards — color = status; tap / long-press actions |
| **Orders** | Guests, order items, qty, edit customize, KOT, Served, Finalize |
| **Menu** | Products add + spice / meat / allergies / add-ons / toppings |
| **Finalize** | Payment / bill complete → sale |
| **Settings** | API Base URL (emulator `10.0.2.2` / device LAN IP) |

---

## 3. First-time setup

1. WAMP / server up — API: `…/ElintOm/ordertakingapi/branding` → SUCCESS  
2. App **Settings → Base URL**
   - Emulator: `http://10.0.2.2/ElintOm/ordertakingapi/`
   - Real phone: `http://<PC-LAN-IP>/ElintOm/ordertakingapi/`
3. Cleartext HTTP allowed in app build  
4. Sections load → floor ready

---

## 4. Table status — color meaning

| Status | Color (approx) | Meaning | Typical next step |
|--------|----------------|---------|-------------------|
| **Available** | White | Empty table | Tap → start order |
| **Reserved** | Yellow + clock | Booked for guest | Unreserve / start order |
| **Occupied** | Blue | Order open, items pending (or empty order) | Add items → KOT |
| **Order Placed** | Pink | KOT sent to kitchen | Wait Ready / add more + 2nd KOT |
| **Order Ready** | Green | Kitchen marked ready (partial OK) | Serve guests |
| **Served** | Pink | All active items served | Finalize bill |
| **Free** | Red tint | Manually freed / cleaning | Long-press → Mark Available |

**Footer legend:** White · Blue · Pink · Green · Yellow · Red

---

## 5. Tables screen — gestures

| Action | How | Result |
|--------|-----|--------|
| **Open order** | Short tap table | Opens **Orders** (creates order if needed) |
| **Reserve** | Long-press Available/Free → Reserve | Name + until → Yellow |
| **Unreserve** | Long-press Reserved → Unreserve | Back to Available |
| **Free table** | Long-press active → Free | Closes open order path → Free (red) |
| **Mark Available** | Long-press Free → Mark Available | White again |
| **Transfer** | Long-press → Transfer → tap empty table | Order moves to new table |
| **Merge** | Long-press → Merge → tap other table | Bills/orders combine on target |

---

## 6. Orders screen — full detail

### 6.1 Header area

- **Table dropdown** — switch table without leaving Orders  
- **Guests − / +** — increase / decrease guest count  
- **Guest chips** — filter: All / Table Items / Guest 1…N  
- **Grid icon** — popup grid of all items (qty change + KOT from popup)  
- **Total** — live grand total (cancelled items out)

### 6.2 Guests model

| Guest | Meaning |
|-------|---------|
| **Guest 0 — Table Items** | Shared for whole table (roti, water, salad…) |
| **Guest 1…N** | Per-person items |

Each guest card: **+** opens Menu for that guest.

### 6.3 Item row actions

| Action | How | Allowed when |
|--------|-----|--------------|
| **Qty + / −** | Stepper on row | Pending / KOT (Ready/Served: only cancel via qty→0) |
| **Cancel** | Qty down to **0** | Pending = **row deleted**; KOT+ = stays as **CANCELLED** |
| **Edit customize** | **Tap item** | Pending / KOT only |
| **Cannot edit** | Tap Ready/Served | Snackbar: cancel & re-add |

### 6.4 Edit item (customize again)

Tap a **pending** or **kot** item → same customize dialog as Menu, titled **Edit …**

You can change:

- Quantity  
- Spice level  
- Meat wellness (non-veg only)  
- Add-ons / toppings  
- Allergies + custom allergy **+**  
- No Onion / No Garlic  
- Special instructions  

Tap **Save Changes** → saved on server (and offline queue if needed).  
KOT already sent → kitchen/KDS gets updated ticket sync when customization changes.

### 6.5 Bottom buttons

| Button | When shown | Effect |
|--------|------------|--------|
| **KOT** | Any **pending** items | Pending → `kot`; ticket to kitchen/KDS |
| **Served** | Ready items waiting, no pending | Marks ready → served |
| **Finalize Order** | After KOT path (no open pending) | Goes to bill / payment |

Rules:

- Empty order → cannot KOT / finalize  
- Pending left → finalize blocked (“Send KOT first”)  
- Double KOT with no pending → safe / idempotent  

---

## 7. Menu screen — add items

1. Orders → guest **+** → Menu  
2. Filter: All / Veg / Non-veg + categories + search  
3. Tap **Add** on product → **Customize** dialog  

### Customize options (kay dikhta)

| Section | When shown |
|---------|------------|
| **Toppings** | Master / product toppings exist |
| **Add-ons** | Master / product add-ons exist |
| **Spice Level** | Mild / Medium / Spicy / Extra Hot (from API) |
| **Meat Wellness** | **Non-veg only** (veg = hidden) |
| **Dietary & Allergies** | No Onion, No Garlic + allergy chips |
| **+ allergy** | Popup → name → saved to DB master + auto-selected |
| **Special Instructions** | Free text |
| **Qty** | Before **Add to Order** |

### Stock

| POS **Overselling** | App behavior |
|---------------------|--------------|
| **On (1)** — default | Soft warning; add still allowed |
| **Off (0)** | Out of stock → add blocked; API error |

Change only in **ElintOm System / POS Settings → Overselling**. No separate app “strict stock” switch.

---

## 8. Item status lifecycle

```text
pending  →  kot  →  ready  →  served
                ↘ cancelled (after KOT+, via qty 0)
```

| Status | Meaning | Qty edit | Customize edit | Cancel |
|--------|---------|----------|----------------|--------|
| **pending** | Not in kitchen | Yes | Yes (tap item) | Deletes row |
| **kot** | On KOT / KDS | Yes | Yes (tap item) | CANCELLED badge; KDS updated |
| **ready** | Kitchen done | No (except cancel) | No | Cancelled |
| **served** | At table | No (except cancel) | No | Cancelled |
| **cancelled** | Dropped after kitchen | Locked | No | — |

**Bill:** Cancelled lines excluded from sale.

---

## 9. Day-to-day playbooks

### A) Normal dine-in

1. Sections → subsection → Tables  
2. Tap Available table → Orders  
3. Set guests  
4. Per guest / Table Items → Menu → add with customize  
5. **KOT**  
6. Kitchen marks Ready (KDS / API) → table Green  
7. App **Served** when food delivered  
8. **Finalize Order** → pay → table Available (white)

### B) Add more after first KOT

1. Add new items (they stay **pending**)  
2. Tap **KOT** again — only pending go to kitchen  
3. Ready/Served items untouched  

### C) Wrong spice / allergy after add

1. Orders → tap that item (pending/kot)  
2. Edit sheet → fix → **Save Changes**  
3. Ready/Served already? Cancel → re-add correct item  

### D) Guest cancelled dish after kitchen

1. Qty − until 0 on that item  
2. Row shows **CANCELLED**; total drops; KDS shows cancelled  

### E) Reserve walk-in later

1. Long-press Available → Reserve (name + time)  
2. Guest arrives → open table / unreserve as needed  

### F) Wrong table / party join

- **Transfer** — move whole order to empty table  
- **Merge** — combine onto another table’s order  

### G) Cleaning / close without bill path

1. Long-press → **Free table** (red)  
2. When clean → **Mark Available** (white)  

---

## 10. Offline & multi-device (short)

| Situation | Behavior |
|-----------|----------|
| Network drop while adding/updating | Action queued; syncs when online |
| Same table, second device creates order | API reuses existing open order |
| Double KOT / Finalize from 2 devices | Second treated as already done; queue not stuck |
| Very old queued actions (> ~72h) | Dropped on sync |

---

## 11. Backend / data (developers & admins)

### Main tables

| Table | Stores |
|-------|--------|
| `sma_res_sections` / `subsections` / `tables` | Floor layout + `status_id` |
| `sma_res_table_status` | Available, Reserved, Occupied, Order Placed, Ready, Served, Free |
| `sma_res_orders` | Open / completed orders |
| `sma_res_orders_guests` | Guests under order |
| `sma_res_orders_items` | Lines: qty, price, status, spice, meat, allergies, add-ons, toppings, flags, notes |
| `sma_suspended_bills` / `sma_suspended_items` | KOT → KDS tickets |
| `sma_sales` / `sma_sale_items` | After finalize |
| `sma_products` + `sma_res_product_details` | Menu + veg/non-veg (`meal_type_id`) |
| Add-ons / toppings / allergies / meat wellness masters | Customize options |

### Useful API endpoints

Base: `/ordertakingapi/`

| Action | Endpoint |
|--------|----------|
| Branding / overselling flag | `GET branding` |
| Sections / tables | `sections`, `tables` |
| Order load | `order_bootstrap` |
| Create / guests | `create_order`, `increase_guest`, `decrease_guest` |
| Menu + customize | `menu_items`, `product_customizations`, `add_allergy` |
| Cart | `add_item`, `update_item` (qty **and** full customize), `delete_item` |
| Kitchen | `update_kot_status`, `mark_ready`, `mark_served` |
| Bill | `finalize_order` |
| Floor ops | `reserve_table`, `unreserve_table`, `transfer_table`, `merge_tables`, `free_table`, `mark_available` |

### Table status derive (open order)

| Item state (ignore cancelled) | Table status |
|-------------------------------|--------------|
| Any ready | `ready` |
| Else any kot | `order-placed` |
| Else any pending | `occupied` |
| All served | `served` |
| No active items | `occupied` |

---

## 12. Known expected behaviors (bugs nahi)

| Behavior | Why |
|----------|-----|
| Cancelled item qty not shown as 0 | Row kept for kitchen audit; badge = CANCELLED |
| Finalize → Available, not Free | Free = manual cleaning state |
| Order Placed & Served same pink family | Design |
| Soft stock warning but add works | Overselling on |
| Meat wellness missing on veg | Correct |
| Item tap opens edit, not grid | Grid = header grid icon only |

---

## 13. Quick troubleshooting

| Problem | Check |
|---------|-------|
| Tables not loading | Base URL, WAMP, branding API |
| Empty add-ons / toppings | Master tables seeded in DB |
| Cannot add out-of-stock | Overselling = 0 in POS settings |
| Edit not opening | Item Ready/Served/Cancelled — only pending/kot editable |
| KOT button grey | No pending items |
| Finalize blocked | Pending items still there — send KOT first |
| Emulator API fail | Use `10.0.2.2`, not `localhost` |

---

## 14. One-page cheat sheet

```text
TAP table          → Orders
LONG-PRESS table   → Reserve / Free / Transfer / Merge / Available
GUEST +            → Menu → Customize → Add
TAP item           → Edit customize (pending/kot)
QTY → 0            → Remove (pending) or CANCELLED (kot+)
KOT                → Kitchen
SERVED             → Food delivered
FINALIZE           → Bill → table Available
GRID icon          → Overview popup
```

---

*Manual for Captain Order Taking app + ElintOm ordertakingapi. Update this file when new floor/billing features ship.*
