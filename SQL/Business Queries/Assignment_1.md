# SQL Query Assignment Questions and Descriptions

### Business Queries

1. **Total number of shipments in January 2022 first quarter**  
   Determine the total count of shipments made during the first quarter of 2022, specifically in the month of January.

   ```sql
   SELECT
       COUNT(shipment_id) AS in_quarter,
       SUM(CASE
           WHEN MONTH(CREATED_DATE) = 1 THEN 1
           ELSE 0
       END) AS in_jan
   FROM
       shipment
   WHERE
       status_id = 'shipment_shipped'
           AND MONTH(CREATED_DATE) BETWEEN 1 AND 4;
   ```

2. **Shipment by Tracking Number**  
    View or analyze shipments based on their unique tracking numbers. Each shipment is identified and tracked using a specific tracking number.

   ```sql
    SELECT
        S.shipment_id, SRS.tracking_id_number
    FROM
        Shipment S
    JOIN
        Shipment_route_segment SRS USING (shipment_id)
    WHERE
        tracking_id_number IS NOT NULL;
    ```


3. **Average Number of Shipments Per Month**  
   Calculate the average number of shipments made per month by dividing the total number of shipments by the number of months.

    ```sql
    SELECT 
    COUNT(shipment_id)/(timestampdiff(month,min(status_date),now())) AS     shipments_per_month
    FROM
        shipment_status ss
    WHERE
        ss.status_id = 'SHIPMENT_SHIPPED';
    ```
4. **Shipped Units By Location**  
   Identify the number of units that have been shipped, categorized by different locations. Gain insights into the distribution of shipped units across various locations.

    ```sql
    SELECT 
        city, COUNT(status_id)
    FROM
        shipment s
            JOIN
        postal_address poa ON s.DESTINATION_CONTACT_MECH_ID = poa.CONTACT_MECH_ID
    WHERE
        s.status_id = 'shipment_shipped'
    GROUP BY poa.city;
    ```
5. **Last Week Imported Orders & Items Count**  
   Identify and count the orders and items that were imported in the system during the last week. "Last week" refers to the previous week or the last 7 days.

   ```sql
    SELECT 
        city, COUNT(status_id)
    FROM
        shipment s
            JOIN
        postal_address poa ON s.DESTINATION_CONTACT_MECH_ID = poa.CONTACT_MECH_ID
    WHERE
        s.status_id = 'shipment_shipped'
    GROUP BY poa.city;
   ```

6. **Total $ Value of Shipments Shipped From Facility 904/906 in First Quarter**  
   Calculate the total monetary value of shipments that originated from facilities 904 and 906 during the first quarter.

    ```sql
    SELECT
        f.FACILITY_ID,
        SUM(oi.QUANTITY * oi.UNIT_PRICE) AS totalValue
    FROM
        order_header oh
    JOIN
        facility f ON oh.ORIGIN_FACILITY_ID = f.FACILITY_ID
    JOIN
        order_item oi ON oh.ORDER_ID = oi.ORDER_ID
    JOIN order_status os ON oh.STATUS_ID = os.STATUS_ID
        AND os.STATUS_ID = 'ORDER_COMPLETED'
    WHERE
        f.FACILITY_ID IN ('904', '906')
        AND MONTH(os.STATUS_DATETIME) BETWEEN 1 AND 4
    GROUP BY
        f.FACILITY_ID;
    ```

7. **Payment Captured But Not Shipped Order Items**  
   Identify orders where payment has been captured, but the items have not been shipped yet or shipment has not been created/initiated.

    ```sql
    SELECT 
        opp.order_id, oh.order_id, opp.status_id, oh.status_id
    FROM
        order_payment_preference opp
            JOIN
        order_header oh ON oh.order_id = opp.order_id
    WHERE
        opp.status_id = 'PAYMENT_RECEIVED'
            AND oh.status_id != 'ORDER_COMPLETED';
    ```

8. **Orders With More Than One Item in a Single Ship Group**  
   Identify orders that have more than one item grouped in a single ship group.
   
   ```sql
    SELECT
        SHIP_GROUP_SEQ_ID, ORDER_ID,
    COUNT(*) AS item_count
    FROM
        order_item_ship_group oisg
    GROUP BY
        SHIP_GROUP_SEQ_ID, ORDER_ID
    HAVING
        COUNT(*) > 1;
   ```

9. **Orders Where Multiple Items Are Grouped and Shipped Together**  
   Find orders where multiple items are grouped and shipped together in a single shipment.

   ```sql
    SELECT
        OS.ORDER_ID, COUNT(order_id) AS ITEM_COUNT, os.SHIPMENT_ID
    FROM
        ORDER_SHIPMENT OS
    GROUP BY
        os.SHIPMENT_ID
    HAVING
        ITEM_COUNT > 1;
   ```

10. **Orders Brokered But Not Shipped**  
    Identify orders that have been brokered (arranged or negotiated) but have not been shipped yet or shipment has not been created/initiated.
    ```sql
    SELECT
        oh.ORDER_ID,
        oh.STATUS_ID
    FROM
        order_header oh
    LEFT JOIN shipment s ON oh.ORDER_ID = s.PRIMARY_ORDER_ID
    WHERE
        (oh.STATUS_ID = 'ORDER_CREATED'
            OR oh.STATUS_ID = 'ORDER_APPROVED')
        AND s.status_id <> 'SHIPMENT_SHIPPED';
    ```

11. **Orders Completed Hourly**  
    Analyze and present the distribution of completed orders on an hourly basis.

    ```sql
    SELECT
        HOUR(os.STATUS_DATETIME) AS Hour,
            COUNT(oh.ORDER_ID) AS Completed_Orders_Count
    FROM
        order_header oh
    JOIN
        order_status os ON oh.ORDER_ID = os.ORDER_ID
    WHERE
        os.STATUS_ID = 'ORDER_COMPLETED'
    GROUP BY
        HOUR(os.STATUS_DATETIME)
    ORDER BY
        HOUR(os.STATUS_DATETIME);
    ```

12. **Maximum Units Fulfilled By Location**  
    Identify the location that has fulfilled the maximum number of units, providing insights into the efficiency of different 
    fulfillment centers.

    ```sql
    SELECT
        pa.CITY,
        COUNT(*) AS fulfillment
    FROM
        shipment s
    JOIN
        postal_address pa ON s.ORIGIN_CONTACT_MECH_ID = pa.CONTACT_MECH_ID
    WHERE
        s.STATUS_ID = 'SHIPMENT_SHIPPED'
    GROUP BY
        pa.CITY
    ORDER BY
        fulfillment DESC LIMIT 1;
    ```

13. **Facility-wise Revenue for SM Store**  
    Break down the revenue generated by each store, helping to understand the contribution of individual stores to the overall revenue.

    ```sql
    SELECT
    f.FACILITY_ID,
    SUM(
        (oi.QUANTITY - oi.CANCEL_QUANTITY) * oi.UNIT_PRICE +
        s.ADDITIONAL_SHIPPING_CHARGE +
        s.ESTIMATED_SHIP_COST
    ) AS Revenue
    FROM
        shipment s
    JOIN
        order_item oi ON s.PRIMARY_ORDER_ID = oi.ORDER_ID
    JOIN
        facility f ON s.ORIGIN_FACILITY_ID = f.FACILITY_ID
    WHERE
        oi.STATUS_ID = 'ITEM_COMPLETED'
    AND s.STATUS_ID = 'SHIPMENT_SHIPPED'
    AND f.FACILITY_ID IN (
        SELECT FACILITY_ID
        FROM facility
        WHERE FACILITY_TYPE_ID IN ('RETAIL_STORE', 'PHYSICAL_STORE', 'OUTLET_STORE')
        AND OWNER_PARTY_ID = 'SM_COMPANY'
    )
    GROUP BY
        f.FACILITY_ID;
    ```

14. **Shipping Refund in the Last Month**  
    Calculate the refunds issued specifically for shipping charges in the last month.

    ```sql
    SELECT
        SUM(AMOUNT) AS Shipping_Refund
    FROM
        return_adjustment ra
    WHERE
        RETURN_ADJUSTMENT_TYPE_ID = 'RET_SHIPPING_ADJ'
        AND ra.LAST_UPDATED_STAMP BETWEEN DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 MONTH), '%Y-%m-01') 
        AND DATE_FORMAT(CURDATE(), '%Y-%m-01');
    ```

15. **Shipping Revenue in the Last Month**  
    Determine the total revenue generated from shipping in the last month.

    ```sql
    SELECT
        SUM((oi.QUANTITY - (CASE WHEN oi.cancel_quantity IS NULL THEN 0 ELSE 1 END)) * oi.UNIT_PRICE) AS Revenue
    FROM
        order_item oi
    WHERE
        LAST_UPDATED_STAMP BETWEEN DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 MONTH), '%Y-%m-01') 
        AND DATE_FORMAT(CURDATE(), '%Y-%m-01')
        AND STATUS_ID = 'ITEM_COMPLETED';
    ```

16. **Send Sale Orders Shipped From the Warehouse**  
    Identify send sale orders that have been shipped from the warehouse.

    ```sql
    SELECT
        *
    FROM
        order_header oh
    JOIN
        shipment s ON oh.ORDER_ID = s.PRIMARY_ORDER_ID
    JOIN
        facility f ON s.ORIGIN_FACILITY_ID = f.FACILITY_ID
    WHERE
        f.FACILITY_TYPE_ID LIKE '%WAREHOUSE%'
        AND oh.SALES_CHANNEL_ENUM_ID = 'POS_SALES_CHANNEL'
        AND s.STATUS_ID = 'SHIPMENT_SHIPPED';

    SELECT * FROM order_header oh WHERE SALES_CHANNEL_ENUM_ID = 'POS_SALES_CHANNEL';
    ```

17. ~~**BOPIS Orders Shipped to Home**~~ _(Skipped)_  
    Determine the number of "Buy Online Pick Up In Store" (BOPIS) orders that were shipped to the customer's home.



18. **BOPIS Orders Revenue in the Last Year**  
    Calculate the revenue generated from BOPIS orders over the past year.

    ```sql
    SELECT
        SUM(oi.QUANTITY * oi.UNIT_PRICE) AS revenue
    FROM
        order_header oh
    JOIN
        order_item oi USING (order_id)
    JOIN
        order_shipment os USING (order_id)
    JOIN
        shipment s ON os.SHIPMENT_ID = s.SHIPMENT_ID
    WHERE
        oh.ENTRY_DATE >= DATE_SUB(NOW(), INTERVAL 1 YEAR)
        AND s.SHIPMENT_METHOD_TYPE_ID = 'STOREPICKUP';
    ```