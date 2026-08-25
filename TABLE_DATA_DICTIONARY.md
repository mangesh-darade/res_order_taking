# 📊 Restaurant Database Tables & Sample Data Dictionary

या फाईलमध्ये रेस्टॉरंट ऑर्डर टेकिंग सिस्टीममधील **प्रत्येक टेबल (Table)**, त्याचे **कॉलम्स (Columns)** आणि **त्यात साठवला जाणारा प्रत्यक्ष डेटा (Real-world Sample Data)** अत्यंत सुटसुटीत आणि स्वच्छ (Clean) स्वरूपात दिलेला आहे.

---

## 📑 Index

1. [`sma_res_sections`](#1-sma_res_sections)
2. [`sma_res_subsections`](#2-sma_res_subsections)
3. [`sma_res_tables`](#3-sma_res_tables)
4. [`sma_res_table_status`](#4-sma_res_table_status)
5. [`sma_res_orders`](#5-sma_res_orders)
6. [`sma_res_orders_guests`](#6-sma_res_orders_guests)
7. [`sma_res_orders_items`](#7-sma_res_orders_items)
8. [`sma_categories`](#8-sma_categories)
9. [`sma_products`](#9-sma_products)
10. [`sma_res_product_details`](#10-sma_res_product_details)
11. [`sma_res_meal_type`](#11-sma_res_meal_type)
12. [`sma_res_meat_wellness`](#12-sma_res_meat_wellness)
13. [`sma_res_common_allergies`](#13-sma_res_common_allergies)
14. [`sma_res_add_ons`](#14-sma_res_add_ons)
15. [`sma_res_toppings`](#15-sma_res_toppings)
16. [`sma_sales`](#16-sma_sales)
17. [`sma_sale_items`](#17-sma_sale_items)

---

### <a id="1-sma_res_sections"></a>1. `sma_res_sections`
> **उद्देश:** हॉटेलचे मुख्य विभाग किंवा मजले साठवण्यासाठी.

| id | name | is_active | created_at |
| :--- | :--- | :--- | :--- |
| `1` | Main AC Dining | 1 | 2026-01-10 10:00:00 |
| `2` | Garden / Lawn Area | 1 | 2026-01-10 10:00:00 |
| `3` | Rooftop Lounge | 1 | 2026-01-10 10:00:00 |
| `4` | Family Banquet | 1 | 2026-01-10 10:00:00 |

---

### <a id="2-sma_res_subsections"></a>2. `sma_res_subsections`
> **उद्देश:** मुख्य विभागातील उपविभाग किंवा केबिन्स साठवण्यासाठी.

| id | section_id | name | is_active |
| :--- | :--- | :--- | :--- |
| `101` | 1 | Hall A | 1 |
| `102` | 1 | VIP Room | 1 |
| `103` | 2 | Poolside | 1 |
| `104` | 3 | Sunset Deck | 1 |

---

### <a id="3-sma_res_tables"></a>3. `sma_res_tables`
> **उद्देश:** सर्व डायनिंग टेबल्स आणि त्यांची चालू स्थिती साठवण्यासाठी.

| id | name | section_id | subsection_id | status_id | guests_count | reserved_by | reserved_until |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `1` | Table 1 | 1 | 101 | 2 | 4 | *NULL* | *NULL* |
| `2` | Table 2 | 1 | 101 | 1 | 2 | *NULL* | *NULL* |
| `3` | Table 3 | 1 | 102 | 7 | 6 | *NULL* | *NULL* |
| `4` | Table 4 | 2 | 103 | 3 | 4 | Rajesh Sharma | 2026-08-25 21:00:00 |

---

### <a id="4-sma_res_table_status"></a>4. `sma_res_table_status`
> **उद्देश:** टेबलची स्थिती व रंग (Color Code) ठरवण्यासाठी.

| id | value | color | वर्णन |
| :--- | :--- | :--- | :--- |
| `1` | Available | `#2E7D32` | हिरवा (टेबल रिकामे आहे) |
| `2` | Occupied | `#C2185B` | लाल/गुलाबी (ग्राहक बसले आहेत) |
| `3` | Reserved | `#7B1FA2` | जांभळा (टेबल बुक आहे) |
| `4` | Ready | `#0288D1` | निळा (किचनमधून अन्न तयार आहे) |
| `7` | Order Placed | `#E65100` | केशरी (KOT किचनमध्ये गेलेली आहे) |

---

### <a id="5-sma_res_orders"></a>5. `sma_res_orders`
> **उद्देश:** टेबलवर चालू असलेल्या मुख्य रनिंग ऑर्डरचा रेकॉर्ड.

| id | res_tables_id | guest_count | status | payment_status | sgst_percent | cgst_percent | created_at |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `501` | 1 | 3 | `Active` | `Pending` | 2.50 | 2.50 | 2026-08-25 19:30:00 |
| `502` | 3 | 6 | `Order Placed`| `Pending` | 2.50 | 2.50 | 2026-08-25 19:15:00 |

---

### <a id="6-sma_res_orders_guests"></a>6. `sma_res_orders_guests`
> **उद्देश:** एका टेबलवरील स्वतंत्र गेस्ट्स (Guest 1, Guest 2, Guest 3) ची यादी.

| id | res_orders_id | created_at |
| :--- | :--- | :--- |
| `1` | 501 | 2026-08-25 19:30:00 |
| `2` | 501 | 2026-08-25 19:30:00 |
| `3` | 501 | 2026-08-25 19:30:00 |

---

### <a id="7-sma_res_orders_items"></a>7. `sma_res_orders_items`
> **उद्देश:** ऑर्डर केलेले पदार्थ, प्रमाण, कस्टमायझेशन आणि कुकिंग सूचना.

| id | res_orders_id | res_orders_guests_id | sma_product_id | quantity | unit_price | amount | spice_level | add_ons | special_instructions | status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `1001` | 501 | **0** (All Guests) | 20 | 2 | 20.00 | 40.00 | *NULL* | *NULL* | Chilled water bottle | `kot` |
| `1002` | 501 | **1** (Guest 1) | 45 | 1 | 240.00 | 270.00 | `Medium` | Extra Cheese | No Onion, No Garlic | `kot` |
| `1003` | 501 | **2** (Guest 2) | 88 | 1 | 320.00 | 320.00 | `Spicy` | *NULL* | Well Done | `kot` |
| `1004` | 501 | **0** (All Guests) | 12 | 4 | 40.00 | 160.00 | *NULL* | *NULL* | Hot & crispy | `pending` |

---

### <a id="8-sma_categories"></a>8. `sma_categories`
> **उद्देश:** खाद्यपदार्थांचे वर्गीकरण (Categories).

| id | name | image |
| :--- | :--- | :--- |
| `1` | Starters & Appetizers | `starters.png` |
| `2` | Main Course (Veg) | `veg_main.png` |
| `3` | Main Course (Non-Veg) | `nonveg_main.png` |
| `4` | Tandoor & Breads | `breads.png` |
| `5` | Beverages & Drinks | `drinks.png` |

---

### <a id="9-sma_products"></a>9. `sma_products`
> **उद्देश:** हॉटेल मेन्यूमधील सर्व डिशेस / प्रॉडक्ट्स मास्टर.

| id | code | name | category_id | price | flag_visible |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `12` | BREAD01 | Butter Naan | 4 | 40.00 | 1 |
| `20` | BEV01 | Mineral Water 1L | 5 | 20.00 | 1 |
| `45` | STAR05 | Paneer Tikka Dry | 1 | 240.00 | 1 |
| `88` | NVEG02 | Butter Chicken Masala| 3 | 320.00 | 1 |

---

### <a id="10-sma_res_product_details"></a>10. `sma_res_product_details`
> **उद्देश:** पदार्थाचा रेस्टॉरंट डायनिंग दर आणि व्हेज/नॉन-व्हेज मॅपिंग.

| id | product_id | meal_type_id | price | is_active |
| :--- | :--- | :--- | :--- | :--- |
| `1` | 45 (Paneer Tikka) | 1 (Veg) | 240.00 | 1 |
| `2` | 88 (Butter Chicken) | 2 (Non-Veg) | 320.00 | 1 |
| `3` | 12 (Butter Naan) | 1 (Veg) | 40.00 | 1 |

---

### <a id="11-sma_res_meal_type"></a>11. `sma_res_meal_type`
> **उद्देश:** मील प्रकार (Veg / Non-Veg / Egg).

| id | name | अर्थ |
| :--- | :--- | :--- |
| `1` | Veg | शाकाहारी |
| `2` | Non-Veg | मांसाहारी |
| `3` | Egg | अंड्याचे पदार्थ |

---

### <a id="12-sma_res_meat_wellness"></a>12. `sma_res_meat_wellness`
> **उद्देश:** नॉन-व्हेज कुकिंग वेलनेस.

| id | type | अर्थ |
| :--- | :--- | :--- |
| `1` | Rare | किंचित भाजलेले / मऊ |
| `2` | Medium Rare | मध्यम मऊ |
| `3` | Medium | नेहमीसारखे शिजवलेले |
| `4` | Well Done | पूर्ण कडक शिजवलेले |

---

### <a id="13-sma_res_common_allergies"></a>13. `sma_res_common_allergies`
> **उद्देश:** ग्राहकांची ॲलर्जी माहिती.

| id | name | is_active |
| :--- | :--- | :--- |
| `1` | Peanuts (शेंगदाणे) | 1 |
| `2` | Gluten Free (गहू/मैदा मुक्त) | 1 |
| `3` | Dairy (दूध/चीज) | 1 |
| `4` | Soy (सोयाबीन) | 1 |
| `5` | Shellfish (सी-फूड) | 1 |

---

### <a id="14-sma_res_add_ons"></a>14. `sma_res_add_ons`
> **उद्देश:** एक्स्ट्रा ॲड-ऑन्स आणि त्यांचे दर.

| id | name | price | is_active |
| :--- | :--- | :--- | :--- |
| `1` | Extra Cheese | ₹ 30.00 | 1 |
| `2` | Extra Mayo Dip | ₹ 20.00 | 1 |
| `3` | Mint Chutney Jar | ₹ 15.00 | 1 |

---

### <a id="15-sma_res_toppings"></a>15. `sma_res_toppings`
> **उद्देश:** पिझ्झा/पास्ता/डिश वरील टॉपिंग्स आणि दर.

| id | name | price | is_active |
| :--- | :--- | :--- | :--- |
| `1` | Fresh Mushroom | ₹ 25.00 | 1 |
| `2` | Black Olives | ₹ 20.00 | 1 |
| `3` | Jalapenos | ₹ 20.00 | 1 |

---

### <a id="16-sma_sales"></a>16. `sma_sales`
> **उद्देश:** ऑर्डर फायनलाइज झाल्यानंतर तयार होणारे मुख्य POS विक्री बिल.

| id | reference_no | total (Subtotal) | total_tax (GST) | grand_total | sale_status | payment_status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `9801` | `SALE/2026/08/012` | ₹ 790.00 | ₹ 39.50 | **₹ 829.50** | `completed` | `paid` |

---

### <a id="17-sma_sale_items"></a>17. `sma_sale_items`
> **उद्देश:** अंतिम बिलावरील छापील पदार्थांची यादी.

| id | sale_id | product_id | product_name | quantity | unit_price | subtotal |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `1` | 9801 | 45 | Paneer Tikka Dry | 1 | 270.00 | ₹ 270.00 |
| `2` | 9801 | 88 | Butter Chicken Masala | 1 | 320.00 | ₹ 320.00 |
| `3` | 9801 | 12 | Butter Naan | 4 | 40.00 | ₹ 160.00 |
| `4` | 9801 | 20 | Mineral Water 1L | 2 | 20.00 | ₹ 40.00 |
