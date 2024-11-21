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



    ```sql
    SELECT
        oh.ORDER_ID,
        oi.ORDER_ITEM_SEQ_ID,
        p.PRODUCT_ID,
        p.PRODUCT_TYPE_ID,
        pt.IS_PHYSICAL,
        pt.IS_DIGITAL,
        oh.SALES_CHANNEL_ENUM_ID,
        oh.ORDER_DATE,
        oh.ENTRY_DATE,
        oh.STATUS_ID,
        oi.STATUS_ID,
        os.STATUS_DATETIME,
        oh.ORDER_TYPE_ID,
        oh.PRODUCT_STORE_ID
    FROM
        order_header oh
    JOIN order_item oi ON
        oh.ORDER_ID = oi.ORDER_ID
    JOIN order_status os ON
        oi.ORDER_ID = os.ORDER_ID
    JOIN product p ON
        oi.PRODUCT_ID = p.PRODUCT_ID
    JOIN product_type pt ON
        p.PRODUCT_TYPE_ID = pt.PRODUCT_TYPE_ID
    WHERE
        oh.ORDER_TYPE_ID = 'SALES_ORDER'
        AND oi.STATUS_ID = 'ITEM_COMPLETED'
        AND os.STATUS_ID = 'ITEM_COMPLETED'
        AND pt.IS_PHYSICAL = 'Y'
        AND oh.PRODUCT_STORE_ID = 'SM_STORE';
    ```

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

    ```sql
    SELECT
        rh.RETURN_ID,
        ri.ORDER_ID,
        oh.PRODUCT_STORE_ID,
        rs.STATUS_DATETIME,
        oh.ORDER_NAME,
        rh.FROM_PARTY_ID,
        rh.RETURN_DATE,
        rh.ENTRY_DATE,
        rh.RETURN_CHANNEL_ENUM_ID
    FROM
        return_header rh
    JOIN return_item ri ON
        rh.RETURN_ID = ri.RETURN_ID
    JOIN return_status rs ON
        ri.RETURN_ID = rs.RETURN_ID
        AND ri.RETURN_ITEM_SEQ_ID = rs.RETURN_ITEM_SEQ_ID
    JOIN order_header oh ON
        ri.ORDER_ID = oh.ORDER_ID
    WHERE
        rs.STATUS_ID = 'RETURN_COMPLETED'
        AND oh.PRODUCT_STORE_ID = 'SM_STORE'
        AND rh.RETURN_CHANNEL_ENUM_ID = 'ECOM_RTN_CHANNEL';
    ```

3. **Order id and contact mech id for shipping address of orders completed in October of 2023:**
- ORDER_ID
- CONTACT_MECH_ID

    ```sql
    SELECT
        oh.ORDER_ID,
        ocm.CONTACT_MECH_ID
    FROM
        order_header oh
    JOIN order_status os ON
        oh.ORDER_ID = os.ORDER_ID
    JOIN order_contact_mech ocm ON
        oh.ORDER_ID = ocm.ORDER_ID
    WHERE
        oh.STATUS_ID = 'ORDER_COMPLETED'
        AND os.STATUS_ID = 'ORDER_COMPLETED'
        AND YEAR(os.STATUS_DATETIME) = 2023
        AND MONTH(os.STATUS_DATETIME) = 10
        AND ocm.CONTACT_MECH_PURPOSE_TYPE_ID = 'SHIPPING_LOCATION';
    ```

4. **Created sales orders:**
- ORDER_ID
- TOTAL_AMOUNT
- PAYMENT_METHOD
- SHOPIFY_ORDER_NAME

    ```sql
    SELECT
        oh.ORDER_ID,
        SUM(GRAND_TOTAL) AS Total_Amount,
        opp.PAYMENT_METHOD_TYPE_ID,
        oh.ORDER_NAME AS SHOPIFY_ORDER_NAME
    FROM
        order_header oh
    JOIN order_payment_preference opp ON
        oh.ORDER_ID = opp.ORDER_ID
    JOIN order_identification oi ON
        oh.ORDER_ID = oi.ORDER_ID
    WHERE
        oh.STATUS_ID = 'ORDER_CREATED'
        AND oh.ORDER_TYPE_ID = 'SALES_ORDER'
        AND oi.ORDER_IDENTIFICATION_TYPE_ID IN ('SHOPIFY_ORD_NAME', 'OLD_SHOPIFY_ORD_NAME')
    GROUP BY
        oh.ORDER_ID;
    ```

5. **Completed order items in July of 2023:**
- ORDER_ID
- ORDER_ITEM_SEQ_ID
- SHOPIFY_ORDER_ID
- SHOPIFY_PRODUCT_ID

    ```sql
    SELECT
        oi.ORDER_ID,
        oi.ORDER_ITEM_SEQ_ID,
        oi2.ID_VALUE AS SHOPIFY_ORDER_ID,
        gi.ID_VALUE AS SHOPIFY_PRODUCT_ID
    FROM
        order_item oi
    JOIN order_status os ON
        oi.ORDER_ID = os.ORDER_ID
        AND oi.ORDER_ITEM_SEQ_ID = os.ORDER_ITEM_SEQ_ID
    JOIN order_identification oi2 ON
        oi.ORDER_ID = oi2.ORDER_ID
    JOIN good_identification gi ON
        oi.PRODUCT_ID = gi.PRODUCT_ID
    WHERE
        oi.STATUS_ID = 'ITEM_COMPLETED'
        AND os.STATUS_ID = 'ITEM_COMPLETED'
        AND YEAR(os.STATUS_DATETIME) = 2023
        AND MONTH(os.STATUS_DATETIME) = 7
        AND oi2.ORDER_IDENTIFICATION_TYPE_ID = 'SHOPIFY_ORD_ID'
        AND gi.GOOD_IDENTIFICATION_TYPE_ID = 'SHOPIFY_PROD_ID';
    ```

6. **Physical items completed from Warehouse in September of 2023**:

    ```sql
    SELECT
        oh.ORDER_ID,
        oi.ORDER_ITEM_SEQ_ID,
        pt.IS_PHYSICAL AS PHYSICAL_ITEM,
        f.FACILITY_TYPE_ID,
        os.STATUS_DATETIME
    FROM
        order_header oh
    JOIN order_item oi ON
        oh.ORDER_ID = oi.ORDER_ID
    JOIN order_status os ON
        oi.ORDER_ID = os.ORDER_ID
        AND os.ORDER_ITEM_SEQ_ID = oi.ORDER_ITEM_SEQ_ID
    JOIN product p ON
        oi.PRODUCT_ID = p.PRODUCT_ID
    JOIN product_type pt ON
        p.PRODUCT_TYPE_ID = pt.PRODUCT_TYPE_ID
    JOIN facility f ON
        oh.ORIGIN_FACILITY_ID = f.FACILITY_ID
    WHERE
        pt.IS_PHYSICAL = 'Y'
        AND oi.STATUS_ID = 'ITEM_COMPLETED'
        AND os.STATUS_ID = 'ITEM_COMPLETED'
        AND f.FACILITY_TYPE_ID LIKE '%WAREHOUSE%'
        AND YEAR(os.STATUS_DATETIME) = 2023
        AND MONTH(os.STATUS_DATETIME) = 9;
    ```

7. **Physical items ordered in the month of September 2023:**

    ```sql
    SELECT
        oh.ORDER_ID,
        oi.ORDER_ITEM_SEQ_ID,
        pt.IS_PHYSICAL AS PHYSICAL_ITEM,
        os.STATUS_DATETIME
    FROM
        order_header oh
    JOIN order_item oi ON
        oh.ORDER_ID = oi.ORDER_ID
    JOIN order_status os ON
        oi.ORDER_ID = os.ORDER_ID
        AND os.ORDER_ITEM_SEQ_ID = oi.ORDER_ITEM_SEQ_ID
    JOIN product p ON
        oi.PRODUCT_ID = p.PRODUCT_ID
    JOIN product_type pt ON
        p.PRODUCT_TYPE_ID = pt.PRODUCT_TYPE_ID
    WHERE
        pt.IS_PHYSICAL = 'Y'
        AND oi.STATUS_ID = 'ITEM_COMPLETED'
        AND os.STATUS_ID = 'ITEM_COMPLETED'
        AND MONTH(os.STATUS_DATETIME) = 9;
    ```

8. **Orders where two or more items are completed but orders are still in the approved status:**

    ```sql
    SELECT
        oh.ORDER_ID
    FROM
        order_item oi
    JOIN order_header oh ON
        oi.ORDER_ID = oh.ORDER_ID
    WHERE
        oi.STATUS_ID = 'ITEM_COMPLETED'
        AND oh.STATUS_ID = 'ORDER_APPROVED'
    GROUP BY
        oi.ORDER_ID
    HAVING
        COUNT(oi.ORDER_ID) > 1;
    ```

9. **Orders where two or more items are canceled but orders are still in the approved status:**

    ```sql
    SELECT
        oh.ORDER_ID
    FROM
        order_item oi
    JOIN order_header oh ON
        oi.ORDER_ID = oh.ORDER_ID
    WHERE
        oi.STATUS_ID = 'ITEM_CANCELLED'
        AND oh.STATUS_ID = 'ORDER_APPROVED'
    GROUP BY
        oi.ORDER_ID
    HAVING
        COUNT(oi.ORDER_ID) > 1;
    ```

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

    ```sql
    SELECT
        oh.ORDER_ID,
        oi.ORDER_ITEM_SEQ_ID,
        p.PRODUCT_TYPE_ID,
        oi.EXTERNAL_ID,
        oh.SALES_CHANNEL_ENUM_ID AS SALES_CHANNEL,
        oi.QUANTITY,
        oi.STATUS_ID AS ITEM_STATUS,
        oi.PRODUCT_ID,
        -- Billing Address Fields
    CASE
        WHEN ocm.CONTACT_MECH_PURPOSE_TYPE_ID = 'BILLING_LOCATION' THEN pa.CITY
        ELSE NULL
    END AS BILL_CITY,
    CASE
        WHEN ocm.CONTACT_MECH_PURPOSE_TYPE_ID = 'BILLING_LOCATION' THEN pa.POSTAL_CODE
        ELSE NULL
    END AS BILL_POSTALCODE,
    CASE
        WHEN ocm.CONTACT_MECH_PURPOSE_TYPE_ID = 'BILLING_LOCATION' THEN pa.ADDRESS1
        ELSE NULL
    END AS BILL_ADDRESS1,
    CASE
        WHEN ocm.CONTACT_MECH_PURPOSE_TYPE_ID = 'BILLING_LOCATION' THEN pa.ADDRESS2
        ELSE NULL
    END AS BILL_ADDRESS2,
    -- Shipping Address Fields
    CASE
        WHEN ocm.CONTACT_MECH_PURPOSE_TYPE_ID = 'SHIPPING_LOCATION' THEN pa.CITY
        ELSE NULL
    END AS SHIP_CITY,
    CASE
        WHEN ocm.CONTACT_MECH_PURPOSE_TYPE_ID = 'SHIPPING_LOCATION' THEN pa.POSTAL_CODE
        ELSE NULL
    END AS SHIP_POSTALCODE,
    CASE
        WHEN ocm.CONTACT_MECH_PURPOSE_TYPE_ID = 'SHIPPING_LOCATION' THEN pa.ADDRESS1
        ELSE NULL
    END AS SHIP_ADDRESS1,
    CASE
        WHEN ocm.CONTACT_MECH_PURPOSE_TYPE_ID = 'SHIPPING_LOCATION' THEN pa.ADDRESS2
        ELSE NULL
    END AS SHIP_ADDRESS2
    FROM
        order_item oi
    JOIN
        order_header oh USING(order_id)
    JOIN
        product p USING(product_id)
    JOIN
        order_contact_mech ocm USING(order_id)
    JOIN
        contact_mech cm USING(CONTACT_MECH_ID)
    JOIN
        postal_address pa USING(contact_mech_id)
    WHERE
        oi.STATUS_ID = 'ITEM_CREATED'
        AND oh.ORDER_TYPE_ID = 'SALES_ORDER'
        AND ocm.CONTACT_MECH_PURPOSE_TYPE_ID IN ('BILLING_LOCATION', 'SHIPPING_LOCATION');
    ```

11. **Customers created in June 2023:**

    ```sql
    SELECT
        PARTY_ID AS CustomerPartyID
    FROM
        party
    JOIN party_status ps ON
        party.PARTY_ID = ps.PARTY_ID
    WHERE
        ps.STATUS_ID = 'ACTIVE'
        AND ps.STATUS_DATETIME BETWEEN '2023-06-01' AND '2023-06-30';
    ```

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

    ```sql
    SELECT
        oh.ORDER_ID,
        oh.ORDER_NAME,
        oh.ORDER_TYPE_ID
    FROM
        order_header oh
    JOIN order_status os ON
        oh.ORDER_ID = os.ORDER_ID
    WHERE
        os.STATUS_ID = 'ORDER_CREATED'
        AND os.STATUS_DATETIME BETWEEN '2023-06-01' AND '2023-06-30';
    ```

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

    ```sql
    SELECT
        oi.ORDER_ITEM_SEQ_ID,
        oi.ORDER_ID,
        oi.PRODUCT_ID
    FROM
        order_item oi
    JOIN order_status os ON
        oi.ORDER_ID = os.ORDER_ID
        AND oi.ORDER_ITEM_SEQ_ID = os.ORDER_ITEM_SEQ_ID
    WHERE
        oi.STATUS_ID = 'ITEM_CANCELLED'
        AND os.STATUS_DATETIME BETWEEN '2023-06-01' AND '2023-06-30';
    ```

14. **Inventory variances of products where the reason is `VAR_LOST` or `VAR_DAMAGED` :**

    ```sql
    SELECT
        si.SHIPMENT_ID,
        si.SHIPMENT_DATE,
        si.STATUS_ID
    FROM
        shipment_header si
    JOIN shipment_status ss ON
        si.SHIPMENT_ID = ss.SHIPMENT_ID
    WHERE
        ss.STATUS_ID = 'DELIVERED'
    AND ss.STATUS_DATETIME BETWEEN '2023-06-01' AND '2023-06-30';
    ```

15. **Orders with more than one return:**

    ```sql
    SELECT
        oi.ORDER_ITEM_SEQ_ID,
        oi.ORDER_ID,
        oi.PRODUCT_ID,
        oi.QUANTITY
    FROM
        order_item oi
    JOIN order_header oh ON
        oi.ORDER_ID = oh.ORDER_ID
    WHERE
        oi.STATUS_ID = 'ITEM_CREATED'
    AND oi.QUANTITY > 10;
    ```