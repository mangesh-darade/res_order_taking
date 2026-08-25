# 🍽️ Restaurant Order Taking — Database Tables & Real-World Data Examples

हे डॉक्युमेंट समजण्यासाठी अत्यंत सोप्या भाषेत **प्रत्येक टेबलचे काम काय आहे** आणि **त्या टेबलमध्ये प्रत्यक्षात कसला डेटा (Sample Real-World Rows)** सेव्ह होतो, हे उदाहरणांसहित दाखवते.

---

## 📑 Quick Table Index (सर्व टेबल्सची यादी)

1. [🍽️ 1. `sma_res_sections` (रेस्टॉरंट सेक्शन्स / मजले / भाग)](#1-sma_res_sections)
2. [🚪 2. `sma_res_subsections` (उपविभाग / हॉल्स / केबिन्स)](#2-sma_res_subsections)
3. [🪑 3. `sma_res_tables` (डायनिंग टेबल्स)](#3-sma_res_tables)
4. [🚦 4. `sma_res_table_status` (टेबल स्थिती मास्टर)](#4-sma_res_table_status)
5. [🧾 5. `sma_res_orders` (चालू टेबल ऑर्डर्स)](#5-sma_res_orders)
6. [👥 6. `sma_res_orders_guests` (टेबलवरील गेस्ट्स)](#6-sma_res_orders_guests)
7. [🍲 7. `sma_res_orders_items` (ऑर्डर मधील मागवलेले आयटम्स)](#7-sma_res_orders_items)
8. [📂 8. `sma_categories` (मेन्यू कॅटेगरीज)](#8-sma_categories)
9. [🍕 9. `sma_products` (प्रॉडक्ट्स / डिशेस)](#9-sma_products)
10. [💰 10. `sma_res_product_details` (डायनिंग प्राईस व मील टाईप)](#10-sma_res_product_details)
11. [🥗 11. `sma_res_meal_type` (व्हेज / नॉन-व्हेज मास्टर)](#11-sma_res_meal_type)
12. [🥩 12. `sma_res_meat_wellness` (कुकिंग वेलनेस लेव्हल)](#12-sma_res_meat_wellness)
13. [⚠️ 13. `sma_res_common_allergies` (ॲलर्जी मास्टर)](#13-sma_res_common_allergies)
14. [🧀 14. `sma_res_add_ons` & `sma_res_toppings` (एक्स्ट्रा ॲड-ऑन्स आणि टॉपिंग्स)](#14-sma_res_add_ons--sma_res_toppings)
15. [💳 15. `sma_sales` & `sma_sale_items` (फायनल POS बिलिंग)](#15-sma_sales--sma_sale_items)

---

## 🔍 Detailed Tables with Real-World Example Data

---

### <a id="1-sma_res_sections"></a>1. `sma_res_sections`
* **काय काम करते:** हॉटेलमधील प्रमुख डायनिंग झोन्स किंवा एरियाज ठरवते.
* **टेबलमधील प्रत्यक्ष डेटाचे उदाहरण (Sample Data):**

| id | name | is_active | created_at |
| :--- | :--- | :--- | :--- |
| **1** | Main AC Dining | 1 | 2026-01-10 10:00:00 |
| **2** | Garden / Lawn Area | 1 | 2026-01-10 10:00:00 |
| **3** | Rooftop Lounge | 1 | 2026-01-10 10:00:00 |
| **4** | Family Banquet | 1 | 2026-01-10 10:00:00 |

---

### <a id="2-sma_res_subsections"></a>2. `sma_res_subsections`
* **काय काम करते:** एका सेक्शनमधील छोट्या रूम्स, हॉल्स किंवा सब-झोन्स मॅनेज करते.
* **टेबलमधील प्रत्यक्ष डेटाचे उदाहरण (Sample Data):**

| id | section_id | name | is_active |
| :--- | :--- | :--- | :--- |
| **101** | 1 (Main AC) | Hall A | 1 |
| **102** | 1 (Main AC) | VIP Family Room | 1 |
| **103** | 2 (Garden) | Poolside Garden | 1 |
| **104** | 3 (Rooftop) | Sunset Deck | 1 |

---

### <a id="3-sma_res_tables"></a>3. `sma_res_tables`
* **काय काम करते:** प्रत्येक प्रत्यक्ष टेबल, त्याची आसन क्षमता (capacity) आणि चालू स्थिती साठवते.
* **टेबलमधील प्रत्यक्ष डेटाचे उदाहरण (Sample Data):**

| id | name | section_id | subsection_id | status_id | guests_count | reserved_by |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **1** | Table 1 | 1 | 101 | **2** (Occupied) | 4 | *NULL* |
| **2** | Table 2 | 1 | 101 | **1** (Available)| 2 | *NULL* |
| **3** | Table 3 | 1 | 102 | **7** (Order Placed)| 6 | *NULL* |
| **4** | Table 4 | 2 | 103 | **3** (Reserved) | 4 | Rajesh Sharma |

---

### <a id="4-sma_res_table_status"></a>4. `sma_res_table_status`
* **काय काम करते:** ॲपवर टेबलचा रंग आणि स्थिती (Badge) दाखवण्यासाठी मास्टर डेटा.
* **टेबलमधील प्रत्यक्ष डेटाचे उदाहरण (Sample Data):**

| id | value (स्थिती) | color (HEX Code) | अर्थ |
| :--- | :--- | :--- | :--- |
| **1** | Available | `#2E7D32` (हिरवा 🟢) | टेबल मोकळे आहे |
| **2** | Occupied | `#C2185B` (गुलाबी/लाल 🔴) | गेस्ट बसले आहेत |
| **3** | Reserved | `#7B1FA2` (जांभळा 🟣) | टेबल बुक केलेले आहे |
| **4** | Ready | `#0288D1` (निळा 🔵) | अन्न तयार झाले आहे |
| **7** | Order Placed | `#E65100` (केशरी 🟡) | KOT किचनमध्ये गेलेली आहे |

---

### <a id="5-sma_res_orders"></a>5. `sma_res_orders`
* **काय काम करते:** जेव्हा Captain टेबलवर ऑर्डर सुरू करतो, तेव्हा या टेबलमध्ये मुख्य रनिंग ऑर्डर तयार होते.
* **टेबलमधील प्रत्यक्ष डेटाचे उदाहरण (Sample Data):**

| id | res_tables_id | guest_count | status | payment_status | sgst_percent | cgst_percent | created_at |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **501** | 1 (Table 1) | 3 | `Active` | `Pending` | 2.50 | 2.50 | 2026-08-25 19:30:00 |
| **502** | 3 (Table 3) | 6 | `Order Placed` | `Pending` | 2.50 | 2.50 | 2026-08-25 19:15:00 |

---

### <a id="6-sma_res_orders_guests"></a>6. `sma_res_orders_guests`
* **काय काम करते:** एकाच टेबलवर बसलेल्या स्वतंत्र व्यक्तींना (Guest 1, Guest 2, Guest 3) ट्रॅक करते.
* **टेबलमधील प्रत्यक्ष डेटाचे उदाहरण (Sample Data):**

| id (Guest Row) | res_orders_id | created_at |
| :--- | :--- | :--- |
| **1** | 501 (Table 1 Order) | 2026-08-25 19:30:00 |
| **2** | 501 (Table 1 Order) | 2026-08-25 19:30:00 |
| **3** | 501 (Table 1 Order) | 2026-08-25 19:30:00 |

---

### <a id="7-sma_res_orders_items"></a>7. `sma_res_orders_items`
* **काय काम करते:** प्रत्येक गेस्टने किंवा संपूर्ण टेबलने (Common) काय मागवले, किती प्रमाणात मागवले, तिखट किती हवे आणि काय ॲड केले.
* **टेबलमधील प्रत्यक्ष डेटाचे उदाहरण (Sample Data):**

| id | res_orders_id | res_orders_guests_id | sma_product_id | qty | unit_price | amount | spice_level | add_ons / notes | status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **1001** | 501 | **0 (Table Common)** | 20 (Mineral Water) | 2 | 20.00 | 40.00 | *NULL* | Chilled water bottle | `kot` |
| **1002** | 501 | **1 (Guest 1)** | 45 (Paneer Tikka) | 1 | 240.00 | 270.00 | `Medium` | Extra Cheese (30.00), No Onion | `kot` |
| **1003** | 501 | **2 (Guest 2)** | 88 (Butter Chicken)| 1 | 320.00 | 320.00 | `Spicy` | Less oil, Well Done | `kot` |
| **1004** | 501 | **0 (Table Common)** | 12 (Butter Naan) | 4 | 40.00 | 160.00 | *NULL* | Crispy hot | `pending`|

---

### <a id="8-sma_categories"></a>8. `sma_categories`
* **काय काम करते:** मेन्यूमधील कॅटेगरी वर्गीकरण (Starters, Main Course, Breads इ.).
* **टेबलमधील प्रत्यक्ष डेटाचे उदाहरण (Sample Data):**

| id | name | image |
| :--- | :--- | :--- |
| **1** | Starters & Appetizers | `starters.png` |
| **2** | Main Course (Veg) | `veg_main.png` |
| **3** | Main Course (Non-Veg) | `nonveg_main.png` |
| **4** | Tandoor & Breads | `breads.png` |
| **5** | Beverages & Drinks | `drinks.png` |

---

### <a id="9-sma_products"></a>9. `sma_products`
* **काय काम करते:** किचनमधील सर्व खाद्यपदार्थांची मूळ यादी (Master Catalog).
* **टेबलमधील प्रत्यक्ष डेटाचे उदाहरण (Sample Data):**

| id | code | name | category_id | price | flag_visible |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **12** | BREAD01 | Butter Naan | 4 (Breads) | 40.00 | 1 |
| **20** | BEV01 | Mineral Water 1L | 5 (Beverages)| 20.00 | 1 |
| **45** | STAR05 | Paneer Tikka Dry | 1 (Starters) | 240.00 | 1 |
| **88** | NVEG02 | Butter Chicken Masala| 3 (Non-Veg) | 320.00 | 1 |

---

### <a id="10-sma_res_product_details"></a>10. `sma_res_product_details`
* **काय काम करते:** एखाद्या प्रॉडक्टची रेस्टॉरंटमधील डायनिंग प्राईस आणि तो व्हेज आहे की नॉन-व्हेज हे ठरवते.
* **टेबलमधील प्रत्यक्ष डेटाचे उदाहरण (Sample Data):**

| id | product_id | meal_type_id | price (Dining Rate) | is_active |
| :--- | :--- | :--- | :--- | :--- |
| **1** | 45 (Paneer Tikka) | **1 (Veg)** | 240.00 | 1 |
| **2** | 88 (Butter Chicken) | **2 (Non-Veg)** | 320.00 | 1 |
| **3** | 12 (Butter Naan) | **1 (Veg)** | 40.00 | 1 |

---

### <a id="11-sma_res_meal_type"></a>11. `sma_res_meal_type`
* **काय काम करते:** मेन्यू स्क्रीनवर **Veg** (हिरवा चौकोन 🟢) आणि **Non-Veg** (लाल त्रिकोण 🔺) फिल्टर करण्यासाठी.
* **टेबलमधील प्रत्यक्ष डेटाचे उदाहरण (Sample Data):**

| id | name | अर्थ |
| :--- | :--- | :--- |
| **1** | Veg | शाकाहारी पदार्थ |
| **2** | Non-Veg | मांसाहारी पदार्थ |
| **3** | Egg | अंड्याचे पदार्थ (Eggitarian) |

---

### <a id="12-sma_res_meat_wellness"></a>12. `sma_res_meat_wellness`
* **काय काम करते:** मटण/चिकन स्टेकसाठी ग्राहकाला कुकिंग वेलनेस निवडता यावी.
* **टेबलमधील प्रत्यक्ष डेटाचे उदाहरण (Sample Data):**

| id | type | अर्थ |
| :--- | :--- | :--- |
| **1** | Rare | किंचित भाजलेले / सॉफ्ट |
| **2** | Medium Rare | मध्यम मऊ |
| **3** | Medium | सर्वसामान्य शिजवलेले |
| **4** | Well Done | पूर्णपणे आणि कडक भाजलेले |

---

### <a id="13-sma_res_common_allergies"></a>13. `sma_res_common_allergies`
* **काय काम करते:** ग्राहकांना असलेल्या ॲलर्जीची माहिती शेफपर्यंत पोहोचवणे.
* **टेबलमधील प्रत्यक्ष डेटाचे उदाहरण (Sample Data):**

| id | name |
| :--- | :--- |
| **1** | Peanuts / शेंगदाणे |
| **2** | Gluten Free / गहू-मैदा मुक्त |
| **3** | Dairy / दुग्धजन्य पदार्थ |
| **4** | Soy / सोयाबीन |
| **5** | Shellfish / सी-फूड |

---

### <a id="14-sma_res_add_ons--sma_res_toppings"></a>14. `sma_res_add_ons` & `sma_res_toppings`
* **काय काम करते:** पदार्थांसोबत एक्स्ट्रा चीज, डिप्स किंवा टॉपिंग्सचे दर ॲड करणे.
* **टेबलमधील प्रत्यक्ष डेटाचे उदाहरण (Sample Data):**

#### `sma_res_add_ons`:
| id | name | price (अतिरिक्त दर) |
| :--- | :--- | :--- |
| **1** | Extra Cheese | ₹ 30.00 |
| **2** | Extra Mayo Dip | ₹ 20.00 |
| **3** | Mint Chutney Jar | ₹ 15.00 |

#### `sma_res_toppings`:
| id | name | price (अतिरिक्त दर) |
| :--- | :--- | :--- |
| **1** | Fresh Mushroom | ₹ 25.00 |
| **2** | Black Olives | ₹ 20.00 |
| **3** | Jalapenos | ₹ 20.00 |

---

### <a id="15-sma_sales--sma_sale_items"></a>15. `sma_sales` & `sma_sale_items` (POS Final Billing)
* **काय काम करते:** जेव्हा Captain **"Finalize Order"** बटण दाबतो, तेव्हा टेबलच्या चालू ऑर्डरचे अधिकृत बिलामध्ये रूपांतर होते.
* **टेबलमधील प्रत्यक्ष डेटाचे उदाहरण (Sample Data):**

#### `sma_sales` (मुख्य बिल):
| id | reference_no | total (Subtotal) | total_tax (GST) | grand_total | sale_status | payment_status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **9801** | `SALE/2026/08/012`| ₹ 790.00 | ₹ 39.50 | **₹ 829.50** | `completed` | `paid` |

#### `sma_sale_items` (बिलातील आयटम यादी):
| id | sale_id | product_id | product_name | quantity | unit_price | subtotal |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **1** | 9801 | 45 | Paneer Tikka Dry | 1 | 270.00 | 270.00 |
| **2** | 9801 | 88 | Butter Chicken Masala | 1 | 320.00 | 320.00 |
| **3** | 9801 | 12 | Butter Naan | 4 | 40.00 | 160.00 |
| **4** | 9801 | 20 | Mineral Water 1L | 2 | 20.00 | 40.00 |

---

## 🎯 Summary (थोडक्यात सारांश)

1. **फ्लोअर आणि टेबल्स:** `sma_res_sections` ➡️ `sma_res_subsections` ➡️ `sma_res_tables` ➡️ `sma_res_table_status`
2. **मेन्यू आणि दर:** `sma_categories` ➡️ `sma_products` ➡️ `sma_res_product_details`
3. **कस्टमायझेशन:** `sma_res_meal_type` + `sma_res_add_ons` + `sma_res_toppings` + `sma_res_common_allergies`
4. **ऑर्डर आणि गेस्ट्स:** `sma_res_orders` ➡️ `sma_res_orders_guests` ➡️ `sma_res_orders_items`
5. **अंतिम बिल:** `sma_sales` ➡️ `sma_sale_items`
