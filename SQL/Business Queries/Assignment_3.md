# SQL Assignment 3 Questions

**1. How many single-item orders were fulfilled from warehouses in the last month?**
-   This query counts the orders that were completed and shipped from warehouses in the last month. The orders are identified based on the status of the items (`ITEM_COMPLETED`) and the warehouse facility type. It uses `GROUP BY` to ensure that only single-item orders are counted.

    ```sql
    SELECT
        oisg.ORDER_ID
    FROM 
        order_item_ship_group oisg
    JOIN 
        order_item oi USING(order_id)
    JOIN 
        facility f USING(facility_id)
    WHERE 
        oi.STATUS_ID = 'ITEM_COMPLETED'
        AND f.FACILITY_TYPE_ID LIKE '%WAREHOUSE%'
        AND oi.LAST_UPDATED_STAMP BETWEEN DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 MONTH), '%Y-%m-01')
        AND LAST_DAY(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))
    GROUP BY
        oisg.ORDER_ID
    HAVING
        COUNT(oisg.ORDER_ID) = 1;
    ```

**2. Leading up to the New Year, what is the count of orders shipped from stores in the 25 days preceding the New Year?**
- This query counts orders that were shipped from retail or outlet stores in the 25 days before New Year's Day (2023-12-06 to 2023-12-30).

    ```sql
    SELECT 
        COUNT(*) AS orders_shipped_count
    FROM 
        shipment s
    JOIN 
        facility f ON s.ORIGIN_FACILITY_ID = f.FACILITY_ID
    JOIN 
        shipment_status ss ON s.SHIPMENT_ID = ss.SHIPMENT_ID
    WHERE 
        ss.STATUS_ID = 'SHIPMENT_SHIPPED'
        AND f.FACILITY_TYPE_ID IN ('RETAIL_STORE', 'OUTLET_STORE')
        AND ss.STATUS_DATE BETWEEN '2023-12-06' AND DATE_ADD('2023-12-06', INTERVAL 25 DAY);
    ```

**3. In the period following the New Year, what is the number of orders shipped from stores in the first 25 days?**
- This query counts the number of orders shipped from stores (retail or outlet) in the first 25 days of the year following New Year's Day (2024-01-01 to 2024-01-25).

    ```sql
    SELECT 
        COUNT(*) AS orders_shipped_count
    FROM 
        shipment s
    JOIN 
        facility f ON s.ORIGIN_FACILITY_ID = f.FACILITY_ID
    JOIN 
        shipment_status ss ON s.SHIPMENT_ID = ss.SHIPMENT_ID
    WHERE 
        ss.STATUS_ID = 'SHIPMENT_SHIPPED'
        AND f.FACILITY_TYPE_ID IN ('RETAIL_STORE', 'OUTLET_STORE')
        AND ss.STATUS_DATE BETWEEN DATE_FORMAT(CURDATE(), '%Y-01-01') AND DATE_ADD(DATE_FORMAT(CURDATE(), '%Y-01-01'), INTERVAL 25 DAY);
    ```

**4. What is the total number of orders originating from New York?**
- This query counts the number of unique orders that originated from New York by checking the city in the postal address associated with the order.

    ```sql
    SELECT
        COUNT(DISTINCT ocm.ORDER_ID) AS Total_Unique_Orders_ORG_NY
    FROM 
        order_contact_mech ocm
    JOIN 
        postal_address pa USING(contact_mech_id)
    WHERE 
        pa.CITY = 'New York';
    ```

**5. In New York, which product has the highest sales?**
- This query calculates the total sales of each product in New York, ordered by the highest sales, and returns the product with the highest sales.

    ```sql
    SELECT
        p.PRODUCT_ID, p.PRODUCT_NAME, SUM(oi.QUANTITY) AS TOTAL_SALES
    FROM 
        order_item oi
    JOIN 
        order_header oh ON oi.ORDER_ID = oh.ORDER_ID
    JOIN 
        product p ON oi.PRODUCT_ID = p.PRODUCT_ID
    JOIN 
        order_contact_mech ocm ON oh.ORDER_ID = ocm.ORDER_ID
    JOIN 
        contact_mech cm ON ocm.CONTACT_MECH_ID = cm.CONTACT_MECH_ID
    JOIN 
        postal_address pa ON cm.CONTACT_MECH_ID = pa.CONTACT_MECH_ID
    WHERE 
        pa.CITY = 'New York'
        AND oi.STATUS_ID = 'ITEM_COMPLETED'
    GROUP BY 
        p.PRODUCT_ID
    ORDER BY 
        TOTAL_SALES DESC
    LIMIT 1;
    ```

**6. In the past month, which store has the highest number of one-day shipped orders?**
- This query counts the number of one-day shipped orders per store in the last month and returns the store with the highest count.

    ```sql
    SELECT 
        f.FACILITY_ID, f.FACILITY_NAME, COUNT(s.SHIPMENT_ID) AS one_day_shipped_orders
    FROM 
        shipment s
    JOIN 
        facility f ON s.ORIGIN_FACILITY_ID = f.FACILITY_ID
    JOIN 
        shipment_status ss ON s.SHIPMENT_ID = ss.SHIPMENT_ID
    WHERE 
        ss.STATUS_ID = 'SHIPMENT_SHIPPED'
        AND ss.STATUS_DATE BETWEEN DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 MONTH), '%Y-%m-01')
        AND LAST_DAY(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))
        AND DATEDIFF(ss.STATUS_DATE, s.ORDER_DATE) = 1
    GROUP BY 
        f.FACILITY_ID
    ORDER BY 
        one_day_shipped_orders DESC
    LIMIT 1;
    ```

**7. On a city-wise basis, what is the analysis of returns?**
- This query calculates the total number of returns for each city.

    ```sql
    SELECT 
        pa.CITY, COUNT(pa.CITY) AS Total_Returns
    FROM 
        return_header rh
    JOIN 
        contact_mech cm ON rh.ORIGIN_CONTACT_MECH_ID = cm.CONTACT_MECH_ID
    JOIN 
        postal_address pa ON cm.CONTACT_MECH_ID = pa.CONTACT_MECH_ID
    WHERE 
        STATUS_ID IN ('RETURN_REQUESTED', 'RETURN_COMPLETED', 'RETURN_RECEIVED')
    GROUP BY 
        pa.CITY;
    ```

**8. How many orders with a single return were recorded in the last month?**
- This query counts the number of orders that had only one accepted return within the past month.

    ```sql
    SELECT 
        COUNT(*) AS Total_Orders
    FROM (
        SELECT 
            ri.ORDER_ID
        FROM 
            return_item ri
        JOIN 
            return_status rs ON ri.RETURN_ID = rs.RETURN_ID
        AND 
            ri.RETURN_ITEM_SEQ_ID = rs.RETURN_ITEM_SEQ_ID
        WHERE 
            rs.STATUS_ID = 'RETURN_ACCEPTED'
            AND rs.STATUS_DATETIME <= DATE_SUB(CURRENT_DATE(), INTERVAL 1 MONTH)
        GROUP BY 
            ri.ORDER_ID
        HAVING 
            COUNT(ri.ORDER_ID) = 1
    ) AS subquery;
    ```