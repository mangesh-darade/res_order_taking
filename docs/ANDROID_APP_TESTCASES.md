# Android Order Taking — Test Cases

**App:** `res_order_taking`  
**API:** `http://192.168.5.215/ElintOm/ordertakingapi/`  
**Emulator:** `http://10.0.2.2/ElintOm/ordertakingapi/`  

Har case: **P** = Pass, **F** = Fail. Table number note kar (e.g. T-03).

**Precondition (saglya cases):** WAMP up, phone/PC same Wi‑Fi, branding URL browser madhe JSON yetoy.

```
http://192.168.5.215/ElintOm/ordertakingapi/branding
```

---

## Test run results (2026-08-30) — device SM-S711B

**Executed on:** Samsung Galaxy S23 FE (`com.aistudio.elintompos.restaurant`)  
**Base URL on device:** `http://192.168.5.215/ElintOm/ordertakingapi/`  
**Happy-path table:** **T-03** · Order **#37** · Sale **#45**  
**Screenshots:** `test_screens/run_01_*.png` … `run_13_*.png`

| Result | Count |
|--------|------:|
| **PASS** | 22 |
| **FAIL** | 0 |
| **SKIP** (not run this session) | rest of sheet |

| ID | P/F | Evidence |
|----|-----|----------|
| TC-A02 | **P** | Settings already had LAN URL |
| TC-A03 | **P** | Login shows Invites Hospitality + branding |
| TC-A04 | **P** | Login `captain` → Sections + toast Login Successful |
| TC-A05 | **P** | API login `admin`/`wrongpass999` → 401 |
| TC-B01 | **P** | Sections: AC Family Hall, Main Dining, Rooftop |
| TC-B02 | **P** | Hall A tables grid |
| TC-B03 | **P** | Tabs Sections/Tables/Orders work |
| TC-B04 | **P** | LIVE + poll (~3s) Ready green after API mark_ready |
| TC-B05 | **P** | Footer chips White/Blue/Pink/Green/Yellow/Red |
| TC-B06 Occupied | **P** | T-03 blue after add item |
| TC-B06 Ready | **P** | T-03 green + dish icon |
| TC-B06 Available | **P** | T-03 white after finalize |
| TC-C01–C11 | **P** | Full path T-03 → Occupied → KOT → Ready → Served → Finalize sale 45 → Available |
| TC-D03 | **P** | Veg customize: no Meat Wellness |
| TC-D05 | **P** | Spice Mild saved on line |
| TC-D09 | **P** | Back from Menu → Orders |
| TC-G01 | **P** | Low/Zero stock warning; ADD still allowed (`overselling=1`) |
| TC-F01 (API) | **P** | `reserve_table` T-02 → reserved; unreserve → available (UI dialog validation also: Reserved By required) |
| TC-H03 | **P** | Empty finalize → `No items found in order to finalize` |
| TC-I03 | **P** | `mark_ready` order 37 → app green via poll |

**Not run this session:** TC-A01/A06/A07, second KOT/cancel (E*), Transfer/Merge (F06/F07), Free table (F04), strict stock (G02), offline (H06), full KDS UI (I01).

**Note:** Kitchen Ready tested via API `mark_ready` (not physical KDS tablet). Order-placed pink for T-03 was brief (Ready called soon after KOT); pink verified earlier on AC-01/AC-02 in same session.

---

## A. Setup / Login

| ID | Title | Steps | Expected | P/F |
|----|-------|-------|----------|-----|
| TC-A01 | Wrong API URL | Settings → Base URL galat IP save → app use | Sections/tables fail / error; crash nahi | SKIP |
| TC-A02 | Correct API URL | Settings → `http://192.168.5.215/ElintOm` save | `ordertakingapi/` append; sections load | **P** |
| TC-A03 | Branding | App open / splash | Logo, site name, currency branding API pasun | **P** |
| TC-A04 | Login success | Valid waiter/captain | Sections screen | **P** |
| TC-A05 | Login fail | Galat password | Error message; login rahte | **P** |
| TC-A06 | Logout | Header logout | Login screen; back ne floor nahi | SKIP |
| TC-A07 | Relaunch logged in | Login → kill app → reopen | Splash → Sections (session) | SKIP |

---

## B. Sections / Tables

| ID | Title | Steps | Expected | P/F |
|----|-------|-------|----------|-----|
| TC-B01 | Sections list | Login → Sections | Sections + subsections distat | |
| TC-B02 | Open tables | Section + subsection tap | Tables grid tyacha floor | |
| TC-B03 | Tab switch | Sections ↔ Tables ↔ Orders | Last section/table context rahte | |
| TC-B04 | Poll live | Tables 10s bagh (dusra device/KDS change) | ~3s madhe color/status update | |
| TC-B05 | Footer legend | Tables bottom chips | White · Blue · Pink · Green · Yellow · Red | |

**Table colors (TC-B06 checklist — happy path / extra flows nantar tick):**

| Status | Color | Icons |
|--------|-------|-------|
| Available | White `#FFFFFF` | none |
| Occupied | Blue `#A2E5FF` | Plus + Guest |
| Order Placed | Pink `#FF7EB6` | Plus + Guest |
| Ready | Green `#C8E6C9` | Dish + Guest |
| Served | Pink `#FF7EB6` | Plus + Guest |
| Reserved | Yellow `#FFFF99` | Clock |
| Free | Red `#FFCDD2` | Bill + Guest |

---

## C. Happy path (ek empty table)

**Table:** ________  **Order ID:** ________

| ID | Title | Steps | Expected | P/F |
|----|-------|-------|----------|-----|
| TC-C01 | Open empty table | Available table tap | Orders; empty guests (order create add/guest var hoil) | |
| TC-C02 | Seat / guests | Guest + (2–3) | Guest count; table **Occupied** blue | |
| TC-C03 | Add Guest 1 item | Menu → product → Add | Item **pending**; back Orders | |
| TC-C04 | Add Guest 2 item | Dusra guest → add | Don guests accordion | |
| TC-C05 | Table common item | Guest 0 / All guests → add | “Table Items” bucket | |
| TC-C06 | Qty +/− pending | Orders qty change | Qty/total update; row pending | |
| TC-C07 | Send KOT | **KOT** tap | Table **order-placed** pink; items `kot`; KDS ticket `KOT-{id}` | |
| TC-C08 | Kitchen Ready | KDS ready (1 item OK) | Table **ready** green (~3s); button **Served** | |
| TC-C09 | Mark Served | **Served** | All active **served**; table pink Served | |
| TC-C10 | Finalize | Finalize → complete | `sale_id`; table **Available** white; ~5–7s Tables | |
| TC-C11 | Sale DB | ElintOm / SQL | `sma_sales` + `sma_sale_items` + payment; res order Completed | |

---

## D. Menu / customization

| ID | Title | Steps | Expected | P/F |
|----|-------|-------|----------|-----|
| TC-D01 | Categories | Menu open | Categories + items (~100 cap) | |
| TC-D02 | Search | Item name type | Filter | |
| TC-D03 | Veg — no meat | Veg item customize | Meat Wellness **nahi** | |
| TC-D04 | Non-veg meat | Non-veg customize | Meat Wellness disto | |
| TC-D05 | Spice / notes | Spice + special instruction | Order line var save | |
| TC-D06 | Add-on / topping | Select + add | Price/total vadhte | |
| TC-D07 | Allergy + | + allergy name save | Master + selected | |
| TC-D08 | No onion/garlic | Flags on | Item note/flags | |
| TC-D09 | Back from menu | Add nantar back | **Orders** (Tables nahi) | |

---

## E. Second KOT / cancel

| ID | Title | Steps | Expected | P/F |
|----|-------|-------|----------|-----|
| TC-E01 | Add after KOT | KOT nantar navi item | New = **pending**; table pink rahil (kot/ready asel) | |
| TC-E02 | 2nd KOT | KOT again | Fakt pending → kot; ready/served untouched | |
| TC-E03 | Empty KOT | Pending nahi, KOT again | Success; status reset nahi | |
| TC-E04 | Cancel pending | Qty 0 pending item | Row **delete**; total ↓; KDS line nahi | |
| TC-E05 | Cancel after KOT | Qty 0 kot/ready/served | Row rahte + **CANCELLED**; qty 0 nahi; total ↓; KDS CANCELLED | |
| TC-E06 | Cancelled on bill | Cancel + finalize | Cancelled lines sale madhe **nahi** | |
| TC-E07 | Qty after ready | Ready item qty + | Block (cancel sodun) | |

---

## F. Reserve / Free / Transfer / Merge

**Use 2–3 empty tables.**

| ID | Title | Steps | Expected | P/F |
|----|-------|-------|----------|-----|
| TC-F01 | Reserve | Long-press Available → Reserve name+until | Yellow + Clock + until | |
| TC-F02 | Unreserve | Long-press Reserved → Unreserve | Available white | |
| TC-F03 | Reserve → order | Reserved table tap → add item | Occupied blue | |
| TC-F04 | Free active | Long-press occupied/pink → Free | Open order close; **Free** red | |
| TC-F05 | Mark Available | Long-press Free → Available | White | |
| TC-F06 | Transfer | Occupied long-press Transfer → empty table | Order target; source available | |
| TC-F07 | Merge | Two occupied → Merge to B | Bills B var; A free/available | |
| TC-F08 | Transfer to occupied | Transfer onto busy table | Error / block; crash nahi | |

---

## G. Stock (ElintOm POS **Overselling**)

| ID | Title | Setup | Steps | Expected | P/F |
|----|-------|-------|-------|----------|-----|
| TC-G01 | Soft (default) | Overselling **ON** | Zero-stock item add | Warning; **ADD chalte** | |
| TC-G02 | Hard | Overselling **OFF** | Zero-stock add | ADD disable / `Out of stock` | |
| TC-G03 | Track off | `track_quantity=0` | Add | Stock block nahi | |
| TC-G04 | Branding flags | GET branding | — | `overselling` + `strict_stock` match settings | |

**Note:** Android finalize **warehouse qty deduct nahi** karto. Stock test = menu warning/block, POS qty kam nahi.

---

## H. Negative / edge

| ID | Title | Steps | Expected | P/F |
|----|-------|-------|----------|-----|
| TC-H01 | Decrease guest with items | Last guest items asel, guest − | Block / guest rahte | |
| TC-H02 | Decrease empty guest | Empty last guest − | Guest remove | |
| TC-H03 | Finalize empty order | Items nahi Finalize | Error; sale nahi | |
| TC-H04 | Double finalize | Same order 2nda Finalize | 2nd fail/idempotent; 2 sales nahi | |
| TC-H05 | Two captains same table | Device A+B same table | Ek open order; duplicate nahi | |
| TC-H06 | Airplane then add | Offline add (overselling on) | Queue / local; online sync | |
| TC-H07 | Offline out of stock | Strict + OOS + offline add | Blocked; queue stuck nahi | |
| TC-H08 | Wrong Wi‑Fi / server down | API down | Error; crash nahi | |
| TC-H09 | Rotate / background | Order mid-flow rotate | State lose nahi | |

---

## I. KDS bridge (PC kitchen + phone)

| ID | Title | Steps | Expected | P/F |
|----|-------|-------|----------|-----|
| TC-I01 | Ticket after KOT | KOT → KDS open | Ticket table name + items | |
| TC-I02 | Notes on KDS | Spice/add-on/notes | KDS note madhe | |
| TC-I03 | Ready → app | KDS Ready | App green **ready** (poll) | |
| TC-I04 | Ready ID | Ready nantar app stuck pink | Fail = KDS `suspended_bills.id` pathavtoy, `KOT-{res_order_id}` nahi | |

---

## Suggested run order (1 table session)

1. A02, A04  
2. B01–B05  
3. C01–C11 (happy path)  
4. D03–D08 (customize)  
5. E01–E06 (2nd KOT + cancel) — **navi table**  
6. F01–F07  
7. G01 or G02  
8. H03–H05, I01–I03  

---

## After-test SQL (optional)

```sql
SELECT t.id, t.name, ts.name AS status
FROM sma_res_tables t
LEFT JOIN sma_res_table_status ts ON ts.id = t.status_id;

SELECT o.id, o.res_tables_id, o.status, o.payment_status, i.status AS item_st, i.quantity
FROM sma_res_orders o
LEFT JOIN sma_res_orders_items i ON i.res_orders_id = o.id
ORDER BY o.id DESC
LIMIT 30;

SELECT id, reference_no, table_id FROM sma_suspended_bills
WHERE reference_no LIKE 'KOT-%'
ORDER BY id DESC LIMIT 10;
```

---

*Source of truth: live Android + `Ordertakingapi`. Update colors/status if `TableCard.kt` change.*
