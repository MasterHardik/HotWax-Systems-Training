# SQL Query Assignment 2 Questions and Description
### Business Queries

1. **Completed order items for sales orders of SM_STORE product store and physical items:**
- ORDER_ID
- ORDER_ITEM_SEQ_ID
- PRODUCT_ID
- PRODUCT_TYPE_ID
- IS_PHYSICAL
- IS_DIGITAL
- SALES_CHANNEL_ENUM_ID
- ORDER_DATE
- ENTRY_DATE
- STATUS_ID
- STATUS_DATETIME
- ORDER_TYPE_ID
- PRODUCT_STORE_ID

**2. Completed return items of SM_STORE for ecom return channel:**
- RETURN_ID
- ORDER_ID
- PRODUCT_STORE_ID
- STATUS_DATETIME
- ORDER_NAME
- FROM_PARTY_ID
- RETURN_DATE
- ENTRY_DATE
- RETURN_CHANNEL_ENUM_ID

3. **Order id and contact mech id for shipping address of orders completed in October of 2023:**
- ORDER_ID
- CONTACT_MECH_ID

4. **Created sales orders:**
- ORDER_ID
- TOTAL_AMOUNT
- PAYMENT_METHOD
- SHOPIFY_ORDER_NAME

5. **Completed order items in July of 2023:**
- ORDER_ID
- ORDER_ITEM_SEQ_ID
- SHOPIFY_ORDER_ID
- SHOPIFY_PRODUCT_ID

6. **Physical items completed from Warehouse in September of 2023**:

7. **Physical items ordered in the month of September 2023:**

8. **Orders where two or more items are completed but orders are still in the approved status:**

9. **Orders where two or more items are canceled but orders are still in the approved status:**

10. **Order items that are in the created status and the order type is sales order:**
- ORDER_ID
- PRODUCT_TYPE_ID
- ORDER_LINE_ID
- EXTERNAL_ID
- SALES_CHANNEL
- QUANTITY
- ITEM_STATUS
- PRODUCT_ID
- BILL_CITY
- BILL_COUNTRY
- BILL_POSTALCODE
- BILL_ADDRESS1
- BILL_ADDRESS2
- SHIP_CITY
- SHIP_COUNTRY
- SHIP_POSTALCODE
- SHIP_ADDRESS1
- SHIP_ADDRESS2

11. **Customers created in June 2023:**

12. **Appeasements in July month:**
- RETURN_ID
- ENTRY_DATE
- RETURN_ADJUSTMENT_TYPE_ID
- AMOUNT
- COMMENTS
- ORDER_ID
- ORDER_DATE
- RETURN_DATE
- PRODUCT_STORE_ID

13. **Orders completed in August of 2023:**
- PRODUCT_ID
- PRODUCT_TYPE_ID
- PRODUCT_STORE_ID
- TOTAL_QUANTITY
- INTERNAL_NAME
- FACILITY_ID
- EXTERNAL_ID
- FACILITY_TYPE_ID
- ORDER_HISTORY_ID
- ORDER_ID
- ORDER_ITEM_SEQ_ID
- SHIP_GROUP_SEQ_ID

14. **Inventory variances of products where the reason is `VAR_LOST` or `VAR_DAMAGED` :**

15. **Orders with more than one return:**