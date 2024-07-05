# Product UDM Activity

#### 1. Create a new product by entering all the required fields. Select is Virtual Product as Y for Parent Product. Click on Create Product.

![Step 1](Images/step1.png)

#### 2. Creating variant 

![Step 2](Images/step2.png)

#### 3. Search for prodAssoc to associate a variant product to a base product. Set the ProductAssocType as VARIANT_PRODUCT.

![Step 3](Images/step3.png)
![Step 3](Images/step3-2.png)

#### 4. Next creating Category HCC_000

![Step 4](Images/step4.png)

#### 5. Link it with ProductCategoryMem

![Step 5](Images/step5.png)

#### 6. Create a new product catalog HCPC_000

![Step 6](Images/step6.png)

#### 7. Linking product Catalog [ HCPC_000 ]& product Category [ HCC_000 ] via ProdCatalogCategory

![Step 7](Images/step7.png)

#### 8. Making new product Store

![Step 8](Images/step8.png)

#### 9. To link product Store [9000] and Product Catalog we will use another assoc Entity Product Store Catalog

![Step 9](Images/step9.png)
![Step 9](Images/step9-2.png)

#### 10. Adding product details

![Product Image](Images/step10.png)

#### 11. Creating Facility so that the product can be associated with any facility and can add its inventory to make it sellable.. (otherwise it goes to Backorder).

![Step 11](Images/step11.png)
![Step 11](Images/step11-2.png)

#### 12. Added Inventory for the product at the facility.

![Step 12](Images/step12.png)

#### 13. Adding a cross sell product [10009] using productAssoc
- For Variant HC_001
![Step 13](Images/step13.png)
- For Virtual HC_000
![Step 13](Images/step13-2.png)
- Validating 10009 is suggested when browsing HC_000.
![Step 13](Images/step13-3.png)



#### 14. Buying it! Added 2 quantities of it to the Shopping list

![Step 14](Images/step14.png)

#### 15. Shopping Cart and List

![Step 15](Images/step15.png)

#### 16. Checkout

![Step 16](Images/step16.png)
![Step 16](Images/step16-2.png)

#### 17. Submitting Order

![Step 17](Images/step17.png)

#### 18. Order Confirmation

![Step 18](Images/step18.png)

## Adding feature to the product 
#### First creating a product feature category
![Step 1-1](Images/step18.png)

#### Then creating a product feature type 
![Step 1-2](Images/step18.png)

#### Creating the product Feature 
![Step 1-3](Images/step18.png)

#### Product Feature appl to add Product + feature
![Step 1-4](Images/step18.png)
