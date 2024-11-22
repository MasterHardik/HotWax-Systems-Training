# Billing Related Queries

### Query 1: Total Orders and Charges
This query calculates the total number of distinct orders and associated charges for non-`POS_COMPLETED` shipment methods, grouped by order date.

*Note: The `X` base number of orders is charged with a fixed amount, followed by a cent amount, as per the agreement.*

```sql
SELECT date(ORDER_DATE) AS `ORDER_DATE`,
    SUM(COUNT(DISTINCT order_id)) OVER (
                                        ORDER BY ORDER_DATE) AS `TotalOrder`,
                                        CASE
                                            WHEN SUM(COUNT(DISTINCT order_id)) OVER (
                                                                                    ORDER BY ORDER_DATE) <= [X] THEN 0
                                            ELSE (SUM(COUNT(DISTINCT order_id)) OVER (
                                                                                        ORDER BY ORDER_DATE) - [X]) * [cents charged]
                                        END AS `Charge`
FROM
(SELECT oh.ORDER_DATE,
        oh.ORDER_ID
FROM order_header oh
JOIN order_item oi ON oi.order_id = oh.order_id
JOIN order_item_ship_group oisg ON (oisg.order_id = oi.order_id
                                    AND oisg.ship_group_seq_id = oi.ship_group_seq_id)
WHERE oisg.shipment_method_type_id <> 'POS_COMPLETED'
    AND oh.order_type_id = 'SALES_ORDER') AS virtual_table
WHERE `ORDER_DATE` >= STR_TO_DATE('2024-10-01 00:00:00.000000', '%Y-%m-%d %H:%i:%s.%f')
AND `ORDER_DATE` < STR_TO_DATE('2024-11-01 00:00:00.000000', '%Y-%m-%d %H:%i:%s.%f')
GROUP BY date(ORDER_DATE)
ORDER BY date(ORDER_DATE) ASC
LIMIT 1000;
```

### Query 2: Total Orders and Base Charge
This query calculates the total number of orders and applies a base charge of `[cent's charged]` after the first `X` orders.

```sql
SELECT date(ORDER_DATE) AS `Day`,
       SUM(COUNT(distinct order_id)) OVER (
                                           ORDER BY ORDER_DATE) AS `TotalOrder`,
                                          CASE
                                              WHEN SUM(COUNT(distinct order_id)) OVER (
                                                                                       ORDER BY ORDER_DATE) <= [X] THEN [cent's charged]
                                              ELSE [cent's charged] + (SUM(COUNT(distinct order_id)) OVER (
                                                                                              ORDER BY ORDER_DATE) - [X]) * [cents charged]
                                          END AS `Charge`
FROM
  (SELECT oh.ORDER_DATE,
          oh.order_id
   FROM order_header oh
   JOIN order_item oi ON oi.order_id = oh.order_id
   JOIN order_item_ship_group oisg ON (oisg.order_id = oi.order_id
                                       AND oisg.ship_group_seq_id = oi.ship_group_seq_id)
   WHERE oh.order_type_id = 'SALES_ORDER'
     AND oisg.SHIPMENT_METHOD_TYPE_ID <> 'POS_COMPLETED') AS virtual_table
WHERE `ORDER_DATE` >= STR_TO_DATE('2024-10-01 00:00:00.000000', '%Y-%m-%d %H:%i:%s.%f')
  AND `ORDER_DATE` < STR_TO_DATE('2024-11-01 00:00:00.000000', '%Y-%m-%d %H:%i:%s.%f')
GROUP BY date(ORDER_DATE)
ORDER BY date(ORDER_DATE) ASC
LIMIT 1000;
```

### Query 3: Store Pickup Charges
This query calculates charges for orders with the shipment method `STOREPICKUP`.

```sql
SELECT date(ORDER_DATE) AS `Day`,
       SUM(COUNT(distinct order_id)) OVER (
                                           ORDER BY ORDER_DATE) AS `TotalOrder`,
                                          CASE
                                              WHEN SUM(COUNT(distinct order_id)) OVER (
                                                                                       ORDER BY ORDER_DATE) <= [X] THEN [cents charged]
                                              ELSE [cents charged] + (SUM(COUNT(distinct order_id)) OVER (
                                                                                              ORDER BY ORDER_DATE) - [X]) * [cents charged]
                                          END AS `Charge`
FROM
  (SELECT oh.ORDER_DATE,
          oh.order_id
   FROM order_header oh
   JOIN order_item oi ON oi.order_id = oh.order_id
   JOIN order_item_ship_group oisg ON (oisg.order_id = oi.order_id
                                       AND oisg.ship_group_seq_id = oi.ship_group_seq_id)
   WHERE oisg.SHIPMENT_METHOD_TYPE_ID = 'STOREPICKUP'
     AND oh.order_type_id = 'SALES_ORDER') AS virtual_table
WHERE `ORDER_DATE` >= STR_TO_DATE('2024-10-01 00:00:00.000000', '%Y-%m-%d %H:%i:%s.%f')
  AND `ORDER_DATE` < STR_TO_DATE('2024-11-01 00:00:00.000000', '%Y-%m-%d %H:%i:%s.%f')
GROUP BY date(ORDER_DATE)
ORDER BY date(ORDER_DATE) ASC
LIMIT 1000;
```

###  Backorder Charges
This query calculates charges for backorders and pre-orders.

```sql
SELECT DATE(ORDER_DATE) AS `Day`,
       SUM(COUNT(DISTINCT order_id)) OVER (
                                           ORDER BY ORDER_DATE) AS `TotalOrder`,
                                          CASE
                                              WHEN SUM(COUNT(DISTINCT order_id)) OVER (
                                                                                       ORDER BY ORDER_DATE) <= [X] THEN [cents charged]
                                              ELSE [cents charged] + (SUM(COUNT(DISTINCT order_id)) OVER (
                                                                                               ORDER BY ORDER_DATE) - [X]) * [cents charged]
                                          END AS `Charge`
FROM
  (SELECT oh.ORDER_DATE,
          oh.ORDER_id
   FROM order_header oh
   JOIN order_item oi ON oi.order_id = oh.order_id
   JOIN order_item_ship_group oisg ON (oisg.order_id = oi.order_id
                                       AND oisg.ship_group_seq_id = oi.ship_group_seq_id)
   WHERE oisg.FACILITY_ID IN ('BACKORDER_PARKING',
                              'PRE_ORDER_PARKING')
     AND oh.ORDER_TYPE_ID ='SALES_ORDER') AS virtual_table
WHERE `ORDER_DATE` >= STR_TO_DATE('2024-10-01 00:00:00.000000', '%Y-%m-%d %H:%i:%s.%f')
  AND `ORDER_DATE` < STR_TO_DATE('2024-11-01 00:00:00.000000', '%Y-%m-%d %H:%i:%s.%f')
GROUP BY DATE(ORDER_DATE)
ORDER BY DATE(ORDER_DATE) ASC
LIMIT 1000;
```

### Query 5: Monthly Revenue for a Year
This query calculates the net revenue by transaction month.

```sql
SELECT MonthName(TRANSACTION_MONTH) AS `Transaction Month`,
       `NET_REVENUE` AS `NET_REVENUE`
FROM
  (SELECT oh.ORDER_DATE AS TRANSACTION_MONTH,
          SUM(oi.UNIT_PRICE * oi.QUANTITY - COALESCE(adj.TOTAL_ADJUSTMENT, 0)) AS NET_REVENUE
   FROM order_item oi
   JOIN order_header oh ON oi.ORDER_ID = oh.ORDER_ID
   JOIN order_item_ship_group oisg ON oi.ORDER_ID = oisg.ORDER_ID
   AND oisg.SHIP_GROUP_SEQ_ID = oi.SHIP_GROUP_SEQ_ID
   LEFT JOIN
     (SELECT ori.ORDER_ID,
             ori.ORDER_ITEM_SEQ_ID,
             COALESCE(SUM(CASE
                              WHEN oa.ORDER_ADJUSTMENT_TYPE_ID = 'EXT_PROMO_ADJUSTMENT' THEN oa.AMOUNT
                              ELSE 0
                          END), 0) AS TOTAL_ADJUSTMENT
      FROM order_item ori
      INNER JOIN order_header oh ON ori.ORDER_ID = oh.ORDER_ID
      LEFT JOIN order_adjustment oa ON ori.ORDER_ID = oa.ORDER_ID
      AND ori.ORDER_ITEM_SEQ_ID = oa.ORDER_ITEM_SEQ_ID
      WHERE oh.ORDER_TYPE_ID = 'SALES_ORDER'
        AND oh.ORDER_DATE BETWEEN '2024-01-01' AND CURRENT_DATE - INTERVAL 1 DAY
      GROUP BY ori.ORDER_ID,
               ori.ORDER_ITEM_SEQ_ID) adj ON oi.ORDER_ID = adj.ORDER_ID
   AND oi.ORDER_ITEM_SEQ_ID = adj.ORDER_ITEM_SEQ_ID
   LEFT JOIN
     (SELECT DISTINCT order_id
      FROM return_item) ret ON oi.ORDER_ID = ret.order_id
   WHERE CAST(oh.ORDER_DATE AS DATE) BETWEEN '2024-01-01' AND CURRENT_DATE - INTERVAL 1 DAY
     AND oh.ORDER_TYPE_ID = 'SALES_ORDER'
     AND oi.STATUS_ID = 'ITEM_COMPLETED'
     AND ret.order_id IS NULL
   GROUP BY MONTHNAME(oh.ORDER_DATE)) AS virtual_table
ORDER BY `TRANSACTION_MONTH` ASC
LIMIT 1000;
```

- For Foundational Knowledge about Window Function used in this Advance SQL check [here](https://www.geeksforgeeks.org/window-functions-in-sql/).