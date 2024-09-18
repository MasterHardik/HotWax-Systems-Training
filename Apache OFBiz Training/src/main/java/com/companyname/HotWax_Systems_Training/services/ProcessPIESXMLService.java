package com.companyname.HotWax_Systems_Training.services;

import org.apache.calcite.util.Static;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.Map;

import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.util.EntityQuery;
public class ProcessPIESXMLService {

    private static int featureSeq = 0;
    private static final String MODULE = ProcessPIESXMLService.class.getName();
    private static String getTextContent(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return "";
    }

    public static InputStream byteBufferToInputStream(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return new ByteArrayInputStream(bytes);
    }


    public static Document parseXML(InputStream inputStream) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(inputStream);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

 /* Small functions for the injecting the data into the fields for our PIES*/

    //Associate feature with product
    private static void associateFeatureWithProduct(Delegator delegator, String productId, String productFeatureId) {
        try {
            GenericValue productFeatureAppl = delegator.makeValue("ProductFeatureAppl");
            productFeatureAppl.set("productId", productId);
            productFeatureAppl.set("productFeatureId", productFeatureId);
            productFeatureAppl.set("sequenceNum", ++featureSeq);
            Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
            productFeatureAppl.set("fromDate", nowTimestamp);

            // Assuming you want to save the value after setting it
            delegator.create(productFeatureAppl);
        } catch (GenericEntityException e) {
            // Handle exception (e.g., log it or rethrow)
            System.err.println("Error associating feature with product: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private static void createHazmatFeature(Delegator delegator, String hazardousMaterialCode,String productId) {
        // Generate a unique ID for the ProductFeature
        String productFeatureId = delegator.getNextSeqId("ProductFeature");

        // Create a new ProductFeature record
        GenericValue productFeature = delegator.makeValue("ProductFeature");

        // Set the primary key field
        productFeature.set("productFeatureId", productFeatureId);
        productFeature.set("productFeatureTypeId", "HAZMAT");
        productFeature.set("description", hazardousMaterialCode);
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        productFeature.set("createdStamp", nowTimestamp);
        productFeature.set("lastUpdatedTxStamp", nowTimestamp);

        // Save the record to the database
        try {
            delegator.create(productFeature);
            Debug.log("Successfully created ProductFeature with ID: " + productFeatureId);
        } catch (GenericEntityException e) {
            throw new RuntimeException(e);
        }

        associateFeatureWithProduct(delegator,productId,productFeatureId);

    }


    public static void saveItemLevelGTIN(Delegator delegator, Element itemElement, String productId) throws GenericEntityException {

        // Parsing 'ItemLevelGTIN' with 'GTINQualifier' attribute
        Element itemLevelGTINElement = (Element) itemElement.getElementsByTagName("ItemLevelGTIN").item(0);
        if (itemLevelGTINElement == null) {
            Debug.logWarning("ItemLevelGTIN element not found", MODULE);
            return;
        }

        String gtinQualifier = itemLevelGTINElement.getAttribute("GTINQualifier");
        String itemLevelGTIN = itemLevelGTINElement.getTextContent();

        // Logging or processing the GTIN data
        Debug.logInfo("GTINQualifier: " + gtinQualifier + ", ItemLevelGTIN: " + itemLevelGTIN, MODULE);


        // Check if a GoodIdentification entry already exists for this product and gtinQualifier
        GenericValue goodIdentification = EntityQuery.use(delegator)
                .from("GoodIdentification")
                .where("goodIdentificationTypeId", gtinQualifier, "productId", productId)
                .queryOne();

        if (goodIdentification == null) {
            // Create new GoodIdentification for the product
            goodIdentification = delegator.makeValue("GoodIdentification");
            goodIdentification.set("goodIdentificationTypeId", gtinQualifier);
            goodIdentification.set("productId", productId);
            goodIdentification.set("idValue", itemLevelGTIN);
            Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
            goodIdentification.set("createdStamp",nowTimestamp);
            goodIdentification.create();
            Debug.logInfo("Created new GoodIdentification for productId: " + productId + " with GTIN: " + itemLevelGTIN, MODULE);
        } else {
            Debug.logInfo("GoodIdentification already exists for productId: " + productId + " and GTIN: " + itemLevelGTIN, MODULE);
        }
    }


    public static void createItem(Delegator delegator, String productId, Timestamp introductionDate, Timestamp releaseDate, String quantityPerApplication, String minimumOrderQuantity) throws GenericEntityException {
        // Checking if the product already exists
        GenericValue existingProduct = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();

        if (existingProduct != null) {
            Debug.logWarning("Product with productId: " + productId + " already exists.", MODULE);
            return;
        }

        // Create new product
        GenericValue product = delegator.makeValue("Product");
        product.set("productId", productId);
        product.set("introductionDate", introductionDate);
        product.set("quantityIncluded", quantityPerApplication);
        product.set("releaseDate", releaseDate);
        product.set("requireAmount", minimumOrderQuantity);

        // Setting the current timestamp as createdStamp
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        product.set("createdStamp", nowTimestamp);

        // Save the product to the database
        delegator.create(product);

        // Log product creation
        Debug.logInfo("Product created successfully with productId: " + productId, MODULE);
    }

        /*=======  END ======== */
    public static void extractDataFromXML(DispatchContext dctx,Document document) {
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            NodeList itemList = rootElement.getElementsByTagName("Item");

            for (int i = 0; i < itemList.getLength(); i++) {
                Element itemElement = (Element) itemList.item(i);

                // Extracting data from the XML
                String baseItemId = getTextContent(itemElement, "BaseItemID");
                String partNumber = getTextContent(itemElement, "PartNumber");
                String brandLabel = getTextContent(itemElement, "BrandLabel");
                String vmrsBrandId = getTextContent(itemElement, "VMRSBrandID");
                String hazardousMaterialCode = getTextContent(itemElement, "HazardousMaterialCode");
                String itemEffectiveDate = getTextContent(itemElement, "ItemEffectiveDate");
                String availableDate = getTextContent(itemElement, "AvailableDate");
                String quantityPerApplication = getTextContent(itemElement, "QuantityPerApplication");
                String minimumOrderQuantity = getTextContent(itemElement, "MinimumOrderQuantity");

                Delegator delegator = dctx.getDelegator();

                // Parsing dates to the correct format
                Timestamp introductionDate = itemEffectiveDate.isEmpty() ? null : Timestamp.valueOf(itemEffectiveDate + " 00:00:00");
                Timestamp releaseDate = availableDate.isEmpty() ? null : Timestamp.valueOf(availableDate + " 00:00:00");
                Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

                // Data injections starts from here..

                //product
                try {
                    createItem(delegator, partNumber,introductionDate,releaseDate,quantityPerApplication,minimumOrderQuantity);
                } catch (GenericEntityException e) {
                    throw new RuntimeException(e);
                }
                // Feature
                createHazmatFeature(delegator,hazardousMaterialCode,partNumber);
                try {
                    saveItemLevelGTIN(delegator,itemElement, partNumber);
                } catch (GenericEntityException e) {
                    throw new RuntimeException(e);
                }
//                    // Inserting Brand as a ProductFeature
//                    if (!brandLabel.isEmpty()) {
//                        GenericValue brandFeature = delegator.makeValue("ProductFeature");
//                        brandFeature.set("productFeatureId", partNumber + "_BRAND");
//                        brandFeature.set("description", brandLabel);
//                        brandFeature.set("productFeatureTypeId", "Brand");
//                        delegator.create(brandFeature);
//
//                        GenericValue brandAppl = delegator.makeValue("ProductFeatureAppl");
//                        brandAppl.set("productId", partNumber);
//                        brandAppl.set("productFeatureId", brandFeature.get("productFeatureId"));
//                        brandAppl.set("fromDate", UtilDateTime.nowTimestamp());
//                        delegator.create(brandAppl);
//                    }
//
//                    // Inserting VMRSBrandID as ProductFeature
//                    if (!vmrsBrandId.isEmpty()) {
//                        GenericValue vmrsFeature = delegator.makeValue("ProductFeature");
//                        vmrsFeature.set("productFeatureId", partNumber + "_VMRS");
//                        vmrsFeature.set("description", vmrsBrandId);
//                        vmrsFeature.set("productFeatureTypeId", "VMRSBrandID");
//                        delegator.create(vmrsFeature);
//
//                        GenericValue vmrsAppl = delegator.makeValue("ProductFeatureAppl");
//                        vmrsAppl.set("productId", partNumber);
//                        vmrsAppl.set("productFeatureId", vmrsFeature.get("productFeatureId"));
//                        vmrsAppl.set("fromDate", UtilDateTime.nowTimestamp());
//                        delegator.create(vmrsAppl);
//                    }
//
//                    // Inserting Hazardous Material Code as ProductFeature
//                    if (productFeatureId != null) {
//                        GenericValue hazmatFeatureAppl = delegator.makeValue("ProductFeatureAppl");
//                        hazmatFeatureAppl.set("productId", partNumber);
//                        hazmatFeatureAppl.set("productFeatureId", productFeatureId);
//                        hazmatFeatureAppl.set("fromDate", UtilDateTime.nowTimestamp());
//                        delegator.create(hazmatFeatureAppl);
//                    }
//
//                    // Process Prices
//                    NodeList priceList = itemElement.getElementsByTagName("Pricing");
//                    for (int j = 0; j < priceList.getLength(); j++) {
//                        Element priceElement = (Element) priceList.item(j);
//                        String priceAmount = getTextContent(priceElement, "Price");
//
//                        if (!priceAmount.isEmpty()) {
//                            GenericValue productPrice = delegator.makeValue("ProductPrice");
//                            productPrice.set("productId", partNumber);
//                            productPrice.set("productPriceTypeId", "DEFAULT_PRICE");
//                            productPrice.set("currencyUomId", "USD");
//                            productPrice.set("price", priceAmount);
//                            productPrice.set("fromDate", UtilDateTime.nowTimestamp());
//                            delegator.create(productPrice);
//                        }
//                    }
//
//                    // Process Digital Assets
//                    NodeList digitalAssets = itemElement.getElementsByTagName("DigitalFileInformation");
//                    for (int k = 0; k < digitalAssets.getLength(); k++) {
//                        Element assetElement = (Element) digitalAssets.item(k);
//                        String fileName = getTextContent(assetElement, "FileName");
//                        String fileType = getTextContent(assetElement, "FileType");
//                        String assetType = getTextContent(assetElement, "AssetType");
//                        String filePath = getTextContent(assetElement, "FilePath");
//
//                        if (!fileName.isEmpty() && !fileType.isEmpty()) {
//                            GenericValue content = delegator.makeValue("Content");
//                            content.set("contentId", partNumber + "_" + assetType);
//                            content.set("contentTypeId", assetType);
//                            content.set("dataResourceId", partNumber + "_DATA");
//                            content.set("description", fileName);
//                            delegator.create(content);
//
//                            GenericValue dataResource = delegator.makeValue("DataResource");
//                            dataResource.set("dataResourceId", partNumber + "_DATA");
//                            dataResource.set("dataResourceTypeId", "DIGITAL_ASSET");
//                            dataResource.set("mimeTypeId", fileType);
//                            dataResource.set("objectInfo", filePath);
//                            delegator.create(dataResource);
//                        }
//                    }
//
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    Debug.logError(e, "Error processing item: " + baseItemId);
//                }
//
            }
        }
    }


    // Utility method to convert ByteBuffer content to a String
    public static String byteBufferToString(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        Debug.log("inside the byteBuferToString");
        return new String(bytes);
    }
    public static Map<String, Object> processPIESXML(DispatchContext dctx, Map<String, Object> context) {
        Debug.log("========Started processing the ByteBuffer==================");

        ByteBuffer fileBuffer = (ByteBuffer) context.get("fileName");
        System.out.println(fileBuffer.limit());
        System.out.println(fileBuffer);

        if(fileBuffer.remaining()==0) {
            return ServiceUtil.returnError("No file passed or the file is Empty ! ");
        }

        String successMessage = "";

        Debug.log("========Creating a new read-only copy for XML parsing==================");

        ByteBuffer parseBuffer = fileBuffer.asReadOnlyBuffer();

        Debug.log("========Creating Input Stream==================");

        InputStream inputStream = byteBufferToInputStream(parseBuffer);

        Debug.log("========XML parsing using DocumentBuilderFactory and DocumentBuilder==================");

        Document document = parseXML(inputStream);

        Debug.log("========Extracting Data from the XML==================");

        extractDataFromXML(dctx, document);

        Debug.log("========Returned success message==================");

        return ServiceUtil.returnSuccess(successMessage);

    }

}

// The buffer get's depleted (or "Consumed" ) when you read from it.