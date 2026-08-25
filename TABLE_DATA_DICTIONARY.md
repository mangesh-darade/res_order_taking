# 📊 Restaurant Order Taking — Database Tables & Sample Data

This document provides a clean, simple, and easy-to-understand breakdown of every database table and the exact real-world data stored inside it.

---

## 📑 Table Index

1. [`sma_res_sections` (Dining Areas / Floors)](#1-sma_res_sections)
2. [`sma_res_subsections` (Rooms / Zones / Halls)](#2-sma_res_subsections)
3. [`sma_res_tables` (Dining Tables)](#3-sma_res_tables)
4. [`sma_res_table_status` (Table Statuses & Colors)](#4-sma_res_table_status)
5. [`sma_res_orders` (Running Table Orders)](#5-sma_res_orders)
6. [`sma_res_orders_guests` (Guests per Table)](#6-sma_res_orders_guests)
7. [`sma_res_orders_items` (Ordered Items & Customizations)](#7-sma_res_orders_items)
8. [`sma_categories` (Menu Categories)](#8-sma_categories)
9. [`sma_products` (Dishes / Products Master)](#9-sma_products)
10. [`sma_res_product_details` (Restaurant Dining Price & Meal Type)](#10-sma_res_product_details)
11. [`sma_res_meal_type` (Veg / Non-Veg Master)](#11-sma_res_meal_type)
12. [`sma_res_meat_wellness` (Meat Cooking Levels)](#12-sma_res_meat_wellness)
13. [`sma_res_common_allergies` (Allergies List)](#13-sma_res_common_allergies)
14. [`sma_res_add_ons` (Extra Add-ons)](#14-sma_res_add_ons)
15. [`sma_res_toppings` (Extra Toppings)](#15-sma_res_toppings)
16. [`sma_sales` (Final POS Sale Bill)](#16-sma_sales)
17. [`sma_sale_items` (Final Sale Bill Items)](#17-sma_sale_items)

---

### <a id="1-sma_res_sections"></a>1. `sma_res_sections`
> **Purpose:** Stores the main dining floors or sections of the restaurant.

| id | name | is_active | created_at |
| :--- | :--- | :--- | :--- |
| `1` | Main AC Dining | 1 | 2026-01-10 10:00:00 |
| `2` | Garden / Lawn | 1 | 2026-01-10 10:00:00 |
| `3` | Rooftop Lounge | 1 | 2026-01-10 10:00:00 |
| `4` | Family Banquet | 1 | 2026-01-10 10:00:00 |

---

### <a id="2-sma_res_subsections"></a>2. `sma_res_subsections`
> **Purpose:** Stores subsections, halls, or rooms inside a main section.

| id | section_id | name | is_active |
| :--- | :--- | :--- | :--- |
| `101` | 1 | Hall A | 1 |
| `102` | 1 | VIP Room | 1 |
| `103` | 2 | Poolside Deck | 1 |
| `104` | 3 | Sunset Lounge | 1 |

---

### <a id="3-sma_res_tables"></a>3. `sma_res_tables`
> **Purpose:** Stores all dining tables, their seating capacity, and current status.

| id | name | section_id | subsection_id | status_id | guests_count | reserved_by | reserved_until |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `1` | Table 1 | 1 | 101 | 2 (Occupied) | 4 | *NULL* | *NULL* |
| `2` | Table 2 | 1 | 101 | 1 (Available) | 2 | *NULL* | *NULL* |
| `3` | Table 3 | 1 | 102 | 7 (Order Placed)| 6 | *NULL* | *NULL* |
| `4` | Table 4 | 2 | 103 | 3 (Reserved) | 4 | John Doe | 2026-08-25 21:00:00 |

---

### <a id="4-sma_res_table_status"></a>4. `sma_res_table_status`
> **Purpose:** Stores table status labels and UI badge colors.

| id | value | color | Meaning |
| :--- | :--- | :--- | :--- |
| `1` | Available | `#2E7D32` | Green (Table is empty / ready) |
| `2` | Occupied | `#C2185B` | Pink/Red (Guests are seated) |
| `3` | Reserved | `#7B1FA2` | Purple (Table is booked) |
| `4` | Ready | `#0288D1` | Blue (Food is prepared in kitchen) |
| `7` | Order Placed | `#E65100` | Orange (KOT sent to kitchen) |

---

### <a id="5-sma_res_orders"></a>5. `sma_res_orders`
> **Purpose:** Stores the active running order for a dining table.

| id | res_tables_id | guest_count | status | payment_status | sgst_percent | cgst_percent | created_at |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `501` | 1 (Table 1) | 3 | `Active` | `Pending` | 2.50 | 2.50 | 2026-08-25 19:30:00 |
| `502` | 3 (Table 3) | 6 | `Order Placed` | `Pending` | 2.50 | 2.50 | 2026-08-25 19:15:00 |

---

### <a id="6-sma_res_orders_guests"></a>6. `sma_res_orders_guests`
> **Purpose:** Stores individual guests sitting at the same table (Guest 1, Guest 2, etc.).

| id (Guest ID) | res_orders_id | created_at |
| :--- | :--- | :--- |
| `1` | 501 (Table 1 Order) | 2026-08-25 19:30:00 |
| `2` | 501 (Table 1 Order) | 2026-08-25 19:30:00 |
| `3` | 501 (Table 1 Order) | 2026-08-25 19:30:00 |

---

### <a id="7-sma_res_orders_items"></a>7. `sma_res_orders_items`
> **Purpose:** Stores all items ordered by each guest or shared across all guests on the table.

| id | res_orders_id | res_orders_guests_id | sma_product_id | qty | unit_price | amount | spice_level | add_ons / notes | status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `1001` | 501 | **0 (All Guests / Table)** | 20 (Water Bottle) | 2 | 20.00 | 40.00 | *NULL* | Chilled bottle | `kot` |
| `1002` | 501 | **1 (Guest 1)** | 45 (Paneer Tikka) | 1 | 240.00 | 270.00 | `Medium` | Extra Cheese (30.00), No Onion | `kot` |
| `1003` | 501 | **2 (Guest 2)** | 88 (Butter Chicken)| 1 | 320.00 | 320.00 | `Spicy` | Well Done | `kot` |
| `1004` | 501 | **0 (All Guests / Table)** | 12 (Butter Naan) | 4 | 40.00 | 160.00 | *NULL* | Crispy hot | `pending` |

---

### <a id="8-sma_categories"></a>8. `sma_categories`
> **Purpose:** Stores food menu categories.

| id | name | image |
| :--- | :--- | :--- |
| `1` | Starters & Appetizers | `starters.png` |
| `2` | Main Course (Veg) | `veg_main.png` |
| `3` | Main Course (Non-Veg) | `nonveg_main.png` |
| `4` | Tandoor & Breads | `breads.png` |
| `5` | Beverages & Drinks | `drinks.png` |

---

### <a id="9-sma_products"></a>9. `sma_products`
> **Purpose:** Master product catalog of all food and drink dishes.

| id | code | name | category_id | price | flag_visible |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `12` | BREAD01 | Butter Naan | 4 | 40.00 | 1 |
| `20` | BEV01 | Mineral Water 1L | 5 | 20.00 | 1 |
| `45` | STAR05 | Paneer Tikka Dry | 1 | 240.00 | 1 |
| `88` | NVEG02 | Butter Chicken Masala| 3 | 320.00 | 1 |

---

### <a id="10-sma_res_product_details"></a>10. `sma_res_product_details`
> **Purpose:** Stores restaurant dining price and meal type (Veg/Non-Veg) for each product.

| id | product_id | meal_type_id | price (Dining Rate) | is_active |
| :--- | :--- | :--- | :--- | :--- |
| `1` | 45 (Paneer Tikka) | 1 (Veg) | 240.00 | 1 |
| `2` | 88 (Butter Chicken) | 2 (Non-Veg) | 320.00 | 1 |
| `3` | 12 (Butter Naan) | 1 (Veg) | 40.00 | 1 |

---

### <a id="11-sma_res_meal_type"></a>11. `sma_res_meal_type`
> **Purpose:** Dietary classification for dishes.

| id | name | Meaning |
| :--- | :--- | :--- |
| `1` | Veg | Vegetarian food (Green Icon 🟢) |
| `2` | Non-Veg | Non-Vegetarian food (Red Icon 🔺) |
| `3` | Egg | Contains Egg |

---

### <a id="12-sma_res_meat_wellness"></a>12. `sma_res_meat_wellness`
> **Purpose:** Cooking wellness levels for non-veg meat items.

| id | type | Meaning |
| :--- | :--- | :--- |
| `1` | Rare | Lightly cooked / tender |
| `2` | Medium Rare | Moderately tender |
| `3` | Medium | Standard cooked |
| `4` | Well Done | Fully cooked and browned |

---

### <a id="13-sma_res_common_allergies"></a>13. `sma_res_common_allergies`
> **Purpose:** Master list of food allergies for customer dietary alerts.

| id | name | is_active |
| :--- | :--- | :--- |
| `1` | Peanuts | 1 |
| `2` | Gluten Free | 1 |
| `3` | Dairy / Lactose | 1 |
| `4` | Soy | 1 |
| `5` | Shellfish | 1 |

---

### <a id="14-sma_res_add_ons"></a>14. `sma_res_add_ons`
> **Purpose:** Extra add-ons with additional charges.

| id | name | price | is_active |
| :--- | :--- | :--- | :--- |
| `1` | Extra Cheese | $1.50 | 1 |
| `2` | Extra Mayo Dip | $1.00 | 1 |
| `3` | Mint Chutney Jar | $0.50 | 1 |

---

### <a id="15-sma_res_toppings"></a>15. `sma_res_toppings`
> **Purpose:** Extra toppings for pizzas, pastas, and dishes.

| id | name | price | is_active |
| :--- | :--- | :--- | :--- |
| `1` | Fresh Mushroom | $1.20 | 1 |
| `2` | Black Olives | $1.00 | 1 |
| `3` | Jalapenos | $0.80 | 1 |

---

### <a id="16-sma_sales"></a>16. `sma_sales`
> **Purpose:** Official POS sale transaction created when an order is finalized.

| id | reference_no | total (Subtotal) | total_tax (GST) | grand_total | sale_status | payment_status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `9801` | `SALE/2026/08/012` | $790.00 | $39.50 | **$829.50** | `completed` | `paid` |

---

### <a id="17-sma_sale_items"></a>17. `sma_sale_items`
> **Purpose:** Printed line items on the customer's final POS receipt.

| id | sale_id | product_id | product_name | quantity | unit_price | subtotal |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `1` | 9801 | 45 | Paneer Tikka Dry | 1 | 270.00 | $270.00 |
| `2` | 9801 | 88 | Butter Chicken Masala | 1 | 320.00 | $320.00 |
| `3` | 9801 | 12 | Butter Naan | 4 | 40.00 | $160.00 |
| `4` | 9801 | 20 | Mineral Water 1L | 2 | 20.00 | $40.00 |
