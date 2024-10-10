package com.companyname.HotWax_Systems_Training.services;

import org.apache.ofbiz.base.util.*;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
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
import java.util.ArrayList;
import java.util.List;
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
    private static void associateFeatureWithProduct(DispatchContext dctx, String productId, String productFeatureId, GenericValue userLogin) throws GenericEntityException, GenericServiceException {
        Map<String, Object> serviceParams = UtilMisc.toMap(
                "productId", productId,
                "productFeatureId", productFeatureId,
                "sequenceNum", (featureSeq = featureSeq + 1),
                "productFeatureApplTypeId", "STANDARD_FEATURE",
                "userLogin", userLogin
        );
        Map<String, Object> result = dctx.getDispatcher().runSync("applyFeatureToProduct", serviceParams);
        Debug.logInfo("Associated : " + productId + "<==to==>" + productFeatureId, MODULE);
    }


    private static void createHazmatFeature(Delegator delegator, String hazardousMaterialCode, String productId, DispatchContext dctx, GenericValue userLogin) throws GenericEntityException, GenericServiceException {
        Map<String, Object> productFeatureParams = UtilMisc.toMap(
                "productFeatureTypeId", "HAZMAT",
                "description", hazardousMaterialCode,
                "userLogin", userLogin
        );

        Map<String, Object> result = dctx.getDispatcher().runSync("createProductFeature", productFeatureParams);

        String productFeatureId = (String) result.get("productFeatureId");

        Debug.log("Successfully created ProductFeature with ID: " + productFeatureId);

        associateFeatureWithProduct(dctx, productId, productFeatureId, userLogin);
    }


    public static void saveItemLevelGTIN(Delegator delegator, Element itemElement, String productId, GenericValue userLogin, DispatchContext dctx) throws GenericEntityException, GenericServiceException {

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

        if (UtilValidate.isEmpty(goodIdentification)) {
            // Create new GoodIdentification for the product
            Map<String, Object> goodIdentificationParams = UtilMisc.toMap(
                    "goodIdentificationTypeId", gtinQualifier,
                    "productId", productId,
                    "idValue", itemLevelGTIN,
                    "userLogin", userLogin
            );
            Map<String, Object> result = dctx.getDispatcher().runSync("createGoodIdentification", goodIdentificationParams);
            Debug.logInfo("Created new GoodIdentification for productId: " + productId + " with GTIN: " + itemLevelGTIN + "with s message  :" + result.get("successMessage"), MODULE);
        } else {
            Debug.logInfo("GoodIdentification already exists for productId: " + productId + " and GTIN: " + itemLevelGTIN, MODULE);
        }
    }

    public static void createItem(Delegator delegator, String productId, Timestamp introductionDate, Timestamp releaseDate, String quantityPerApplication, String minimumOrderQuantity, String baseItemId, DispatchContext dctx, GenericValue userLogin) throws GenericEntityException, GenericServiceException {
        // Checking if the product already exists
        GenericValue existingProduct = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();

        if (existingProduct != null) {
            Debug.logWarning("Product with productId: " + productId + " already exists.", MODULE);
            return;
        }

        Map<String, Object> productParams = UtilMisc.toMap(
                "productId", productId,
                "introductionDate", introductionDate,
                "quantityIncluded", quantityPerApplication,
                "releaseDate", releaseDate,
                "internalName", baseItemId,
                "requireAmount", minimumOrderQuantity,
                "userLogin", userLogin
        );

        Map<String, Object> result = dctx.getDispatcher().runSync("createProduct", productParams);

        // Log product creation
        Debug.logInfo("Product created successfully with productId: " + result.get("productId"), MODULE);
    }

    public static void storeProductDescriptions(String productId, Element descriptionElements, DispatchContext dctx, GenericValue userLogin) throws GenericEntityException, GenericServiceException {
        NodeList descriptionList = descriptionElements.getElementsByTagName("Description");

        for (int i = 0; i < descriptionList.getLength(); i++) {
            // Extract data from each Description element
            Element descriptionElement = (Element) descriptionList.item(i);
            String languageCode = descriptionElement.getAttribute("LanguageCode");
            String descriptionCode = descriptionElement.getAttribute("DescriptionCode");
            String sequenceAttr = descriptionElement.getAttribute("Sequence");
            String descriptionText = descriptionElement.getTextContent();

            Map<String, Object> contentParams = UtilMisc.toMap(
                    "contentName", descriptionCode,
                    "description", descriptionText,
                    "localeString", languageCode,
                    "userLogin", userLogin
            );

            Map<String, Object> result = dctx.getDispatcher().runSync("createContent", contentParams);

            String contentId = String.valueOf(result.get("contentId"));

            // Now associate this content with the product in the ProductContent table
            Map<String, Object> productContentParams = UtilMisc.toMap(
                    "contentId", contentId,
                    "productContentTypeId", "DESCRIPTION",
                    "productId", productId,
                    "sequenceNum", sequenceAttr,
                    "userLogin", userLogin
            );

            Map<String, Object> prodContentResult = dctx.getDispatcher().runSync("createProductContent", productContentParams);
            Debug.logInfo("Descriptions stored successfully with contentId: " + prodContentResult.get("contentId"), MODULE);
        }

        Debug.logInfo("Descriptions stored successfully for productId: " + productId, MODULE);
    }

    public static String createProductPriceType(Delegator delegator, String priceTypeId, String description, DispatchContext dctx, GenericValue userLogin) throws GenericEntityException, GenericServiceException {
        // Check if the ProductPriceType already exists
        GenericValue existingProductPriceType = EntityQuery.use(delegator)
                .from("ProductPriceType")
                .where("productPriceTypeId", priceTypeId)
                .queryOne();

        if (UtilValidate.isEmpty(existingProductPriceType)) {
            // Create a new ProductPriceType record
            Map<String, Object> productPriceTypeParams = UtilMisc.toMap(
                    "productPriceTypeId", priceTypeId,
                    "description", description,
                    "userLogin", userLogin
            );
            Map<String, Object> result = dctx.getDispatcher().runSync("createProductPriceType", productPriceTypeParams);
            Debug.logInfo("ProductPriceType created successfully with productPriceTypeId: " + result.get("productPriceTypeId"), MODULE);
            return (String) result.get("ProductPriceType");
        } else {
            Debug.logInfo("ProductPriceType already exists with productPriceTypeId: " + priceTypeId, MODULE);
        }
        return priceTypeId;
    }

    public static void storePrices(Delegator delegator, String productId, Element priceElements, DispatchContext dctx, GenericValue userLogin) throws GenericEntityException, GenericServiceException {

        NodeList pricingList = priceElements.getElementsByTagName("Pricing");

        for (int i = 0; i < pricingList.getLength(); i++) {

            // Extract data from each Pricing element
            Element pricingElement = (Element) pricingList.item(i);
            String priceTypeId = pricingElement.getAttribute("PriceType");
            String priceSheetNumber = pricingElement.getElementsByTagName("PriceSheetNumber").item(0).getTextContent();
            String currencyCode = pricingElement.getElementsByTagName("CurrencyCode").item(0).getTextContent();
            String effectiveDateStr = pricingElement.getElementsByTagName("EffectiveDate").item(0).getTextContent();
            String expirationDateStr = pricingElement.getElementsByTagName("ExpirationDate").item(0).getTextContent();

            // Extract Price and UOM
            Element priceUOMElement = (Element) pricingElement.getElementsByTagName("Price").item(0);
            String priceValue = pricingElement.getElementsByTagName("Price").item(0).getTextContent();

            // Extract PriceBreak and UOM (if needed)
            Element priceBreakUOMElement = (Element) pricingElement.getElementsByTagName("PriceBreak").item(0);
            String priceBreakUOM = priceBreakUOMElement.getAttribute("UOM");
            String priceBreakValue = pricingElement.getElementsByTagName("PriceBreak").item(0).getTextContent();

            // Check if the ProductPrice already exists
            boolean priceExists = EntityQuery.use(delegator)
                    .from("ProductPrice")
                    .where("productId", productId,
                            "productPriceTypeId", priceTypeId,
                            "currencyUomId", currencyCode,
                            "productPricePurposeId", "PURCHASE")
                    .queryCount() > 0;

            if (!priceExists) {

                priceTypeId = createProductPriceType(delegator, priceTypeId, "", dctx, userLogin);

                Map<String, Object> priceParams = UtilMisc.toMap(
                        "productId", productId,
                        "productPriceTypeId", priceTypeId,
                        "currencyUomId", currencyCode,
                        "fromDate", UtilValidate.isNotEmpty(effectiveDateStr) ? effectiveDateStr : UtilDateTime.nowTimestamp(),
                        "thruDate", expirationDateStr,
                        "productPricePurposeId", "PURCHASE",
                        "productStoreGroupId", "_NA_",
                        "price", priceValue,
                        "userLogin", userLogin
                );
                Map<String, Object> result = dctx.getDispatcher().runSync("createProductPrice", priceParams);
                Debug.logInfo("Price stored successfully with productId+(PriceTypeId): " + priceValue + ":" + priceTypeId + ", Success Message : " + result.get("successMessage"), MODULE);
            } else {
                Debug.logInfo("Price already exists for productId: " + productId + " with priceTypeId: " + priceTypeId + " and currencyUomId: " + currencyCode, MODULE);
            }
        }
        Debug.logInfo("Prices stored successfully for productId: " + productId, MODULE);
    }

    public static void storeExtendedProductInformation(Delegator delegator, String productId, Element extendedInfoElements, DispatchContext dctx, GenericValue userLogin) throws GenericEntityException, GenericServiceException {
        NodeList extendedInfoList = extendedInfoElements.getElementsByTagName("ExtendedProductInformation");

        // Check if the ProductFeatureType "EXPI" exists, if not, create it , also we  can comment it if making manually
        boolean productFeatureTypeEntityExist = EntityQuery.use(delegator)
                .from("ProductFeatureType")
                .where("productFeatureTypeId", "EXPI")
                .queryCount() == 0;

        if (productFeatureTypeEntityExist) {
            Map<String, Object> productFeatureTypeParams = UtilMisc.toMap(
                    "productFeatureTypeId", "EXPI",
                    "description", "Extended Product Information",
                    "userLogin", userLogin
            );
            Map<String, Object> result = dctx.getDispatcher().runSync("createProductFeatureType", productFeatureTypeParams);
            Debug.logInfo("ProductFeatureType 'EXPI' created with description: Extended Product Information | " + result.get("successMessage"), MODULE);
        }

        for (int i = 0; i < extendedInfoList.getLength(); i++) {
            // Extract data from each ExtendedProductInformation element
            Element extendedInfoElement = (Element) extendedInfoList.item(i);
            String expiCode = extendedInfoElement.getAttribute("EXPICode");
            String languageCode = extendedInfoElement.getAttribute("LanguageCode");
            String description = extendedInfoElement.getTextContent(); // Value inside the element

            Map<String, Object> productFeatureParams = UtilMisc.toMap(
                    "productFeatureTypeId", "EXPI",
                    "idCode", expiCode,
                    "description", description,
                    "userLogin", userLogin
            );

            Map<String, Object> result = dctx.getDispatcher().runSync("createProductFeature", productFeatureParams);

            String productFeatureId = (String) result.get("productFeatureId");

            // Create a new record in the ProductFeatureAppl table
            associateFeatureWithProduct(dctx, productId, productFeatureId, userLogin);

            Debug.logInfo("Product Feature stored successfully with productId: " + productId + ", productFeatureId: " + result.get("productFeatureId") + ", expiCode: " + expiCode, MODULE);
        }
        Debug.logInfo("Extended Product Information stored successfully for productId: " + productId, MODULE);
    }

    private static void storeProductAttributes(Delegator delegator, String partNumber, Element itemElement, DispatchContext dctx, GenericValue userLogin) throws GenericEntityException, GenericServiceException {
        // Assuming itemElement contains the <ProductAttributes> element
        Element productAttributesElement = UtilXml.firstChildElement(itemElement, "ProductAttributes");

        if (productAttributesElement != null) {
            List<? extends Element> productAttributeList = UtilXml.childElementList(productAttributesElement, "ProductAttribute");

            for (Element productAttributeElement : productAttributeList) {
                String maintenanceType = productAttributeElement.getAttribute("MaintenanceType");
                String attributeId = productAttributeElement.getAttribute("AttributeID");
                String padbAttribute = productAttributeElement.getAttribute("PADBAttribute");
                String attributeUom = productAttributeElement.getAttribute("AttributeUOM");
                String recordNumber = productAttributeElement.getAttribute("RecordNumber");
                String attributeValue = productAttributeElement.getTextContent(); // The value inside <ProductAttribute>

                // Build the description using RecordNumber
                String attributeDescr = "RecordNumber_" + recordNumber;

                // Prepare the fields for insertion into the ProductAttribute table
                Map<String, Object> fields = UtilMisc.toMap(
                        "productId", partNumber,
                        "attrName", attributeId,
                        "attrValue", attributeValue,
                        "attrType", attributeUom,
                        "attrDescription", attributeDescr
                );

                // Check if the attribute already exists in the ProductAttribute table
                GenericValue existingProductAttribute = delegator.findOne("ProductAttribute",
                        UtilMisc.toMap("productId", partNumber, "attrName", attributeId), false);

                fields.put("userLogin", userLogin);

                if (UtilValidate.isEmpty(existingProductAttribute)) {
                    Map<String, Object> result = dctx.getDispatcher().runSync("createProductAttribute", fields);
                    fields.remove("userLogin");
                    Debug.log("Created new ProductAttribute: " + fields);
                } else {
                    Debug.log("ProductAttribute already exists: " + attributeId + " for productId: " + partNumber);
                }
            }
        } else {
            Debug.logWarning("No ProductAttributes found for partNumber: " + partNumber, MODULE);
        }
    }


    private static void storeDigitalAssets(Delegator delegator, String partNumber, Element itemElement, DispatchContext dctx, GenericValue userLogin) throws GenericEntityException, GenericServiceException {
        Element digitalAssetsElement = UtilXml.firstChildElement(itemElement, "DigitalAssets");
        int seqNum = 0;
        if (digitalAssetsElement != null) {
            List<? extends Element> digitalFileInfoList = UtilXml.childElementList(digitalAssetsElement, "DigitalFileInformation");

            for (Element digitalFileInfoElement : digitalFileInfoList) {
                // Generate unique IDs for content and data resource
                String dataResourceId = delegator.getNextSeqId("DataResource");
                String contentId = delegator.getNextSeqId("Content");

                // Extract values from XML
                String assetId = digitalFileInfoElement.getAttribute("AssetID");
                String maintenanceType = digitalFileInfoElement.getAttribute("MaintenanceType");
                String languageCode = digitalFileInfoElement.getAttribute("LanguageCode");

                String fileName = UtilXml.childElementValue(digitalFileInfoElement, "FileName", null);
                String assetType = UtilXml.childElementValue(digitalFileInfoElement, "AssetType", null);
                String fileType = UtilXml.childElementValue(digitalFileInfoElement, "FileType", null);
                String representation = UtilXml.childElementValue(digitalFileInfoElement, "Representation", null);
                String fileSize = UtilXml.childElementValue(digitalFileInfoElement, "FileSize", null);
                String resolution = UtilXml.childElementValue(digitalFileInfoElement, "Resolution", null);
                String colorMode = UtilXml.childElementValue(digitalFileInfoElement, "ColorMode", null);
                String background = UtilXml.childElementValue(digitalFileInfoElement, "Background", null);
                String orientationView = UtilXml.childElementValue(digitalFileInfoElement, "OrientationView", null);
                String filePath = UtilXml.childElementValue(digitalFileInfoElement, "FilePath", null);
                String uri = UtilXml.childElementValue(digitalFileInfoElement, "URI", null);
                String country = UtilXml.childElementValue(digitalFileInfoElement, "Country", null);
                String frame = UtilXml.childElementValue(digitalFileInfoElement, "Frame", null);
                String totalFrames = UtilXml.childElementValue(digitalFileInfoElement, "TotalFrames", null);
                String plane = UtilXml.childElementValue(digitalFileInfoElement, "Plane", null);
                String plunge = UtilXml.childElementValue(digitalFileInfoElement, "Plunge", null);
                String totalPlanes = UtilXml.childElementValue(digitalFileInfoElement, "TotalPlanes", null);


                // Dimensions
                Element assetDimensionsElement = UtilXml.firstChildElement(digitalFileInfoElement, "AssetDimensions");
                String assetHeight = (assetDimensionsElement != null) ? UtilXml.childElementValue(assetDimensionsElement, "AssetHeight", null) : null;
                String assetWidth = (assetDimensionsElement != null) ? UtilXml.childElementValue(assetDimensionsElement, "AssetWidth", null) : null;
                String uom = UtilValidate.isNotEmpty(assetDimensionsElement) ? assetDimensionsElement.getAttribute("UOM") : null;

                // Asset Date
                Element assetDatesElement = UtilXml.firstChildElement(digitalFileInfoElement, "AssetDates");
                Element assetDateElement = UtilXml.firstChildElement(assetDatesElement, "AssetDate");
                String assetDate = UtilValidate.isNotEmpty(assetDateElement) ? assetDateElement.getTextContent() : "";
                Debug.logInfo(filePath, MODULE);
                // Create DataResource
                Map<String, Object> dataResourceFields = UtilMisc.toMap(
                        "dataResourceId", dataResourceId,
                        "mimeTypeId", fileType,
                        "objectInfo", filePath,
                        "dataResourceName", fileName,
                        "userLogin", userLogin
                );

                Map<String, Object> createDataResourceResult = dctx.getDispatcher().runSync("createDataResource", dataResourceFields);

                // Create Content
                Map<String, Object> contentFields = UtilMisc.toMap(
                        "contentId", contentId,
                        "contentTypeId", "DFI", // Assuming "DFI" as the content type
                        "mimeTypeId", fileType,
                        "contentName", fileName,
                        "description", uri,
                        "serviceName", assetId,
                        "localeString", languageCode,
                        "dataResourceId", dataResourceId,
                        "userLogin", userLogin
                );

                Map<String, Object> createContentResult = dctx.getDispatcher().runSync("createContent", contentFields);

                // Create ProductContent
                Map<String, Object> productContentFields = UtilMisc.toMap(
                        "productId", partNumber,
                        "productContentTypeId", "DA", // Assuming "DA" as the product content type
                        "contentId", contentId,
                        "sequenceNum", (seqNum = seqNum + 1),
                        "userLogin", userLogin
                );

                Map<String, Object> createProductContentResult = dctx.getDispatcher().runSync("createProductContent", productContentFields);

                // Create Content Attributes (Dimensions)
                if (UtilValidate.isNotEmpty(assetHeight) && UtilValidate.isNotEmpty(assetWidth)) {
                    Map<String, Object> contentAttrHeightFields = UtilMisc.toMap(
                            "contentId", contentId,
                            "attrName", "AssetHeight",
                            "attrValue", assetHeight,
                            "attrDescription", uom,
                            "userLogin", userLogin
                    );

                    dctx.getDispatcher().runSync("createContentAttribute", contentAttrHeightFields);

                    Map<String, Object> contentAttrWidthFields = UtilMisc.toMap(
                            "contentId", contentId,
                            "attrName", "AssetWidth",
                            "attrValue", assetWidth,
                            "attrDescription", uom,
                            "userLogin", userLogin
                    );

                    dctx.getDispatcher().runSync("createContentAttribute", contentAttrWidthFields);

                }

                // Create DataResourceMetadata for additional fields
                List<Map<String, String>> metadataEntries = new ArrayList<>();

                if (UtilValidate.isNotEmpty(representation)) {
                    metadataEntries.add(UtilMisc.toMap("metaDataPredicateId", "REP", "metaDataValue", representation));
                }

                if (UtilValidate.isNotEmpty(fileSize)) {
                    metadataEntries.add(UtilMisc.toMap("metaDataPredicateId", "FS", "metaDataValue", fileSize));
                }

                if (UtilValidate.isNotEmpty(resolution)) {
                    metadataEntries.add(UtilMisc.toMap("metaDataPredicateId", "RES", "metaDataValue", resolution));
                }

                if (UtilValidate.isNotEmpty(colorMode)) {
                    metadataEntries.add(UtilMisc.toMap("metaDataPredicateId", "COM", "metaDataValue", colorMode));
                }

                if (UtilValidate.isNotEmpty(background)) {
                    metadataEntries.add(UtilMisc.toMap("metaDataPredicateId", "BG", "metaDataValue", background));
                }

                if (UtilValidate.isNotEmpty(orientationView)) {
                    metadataEntries.add(UtilMisc.toMap("metaDataPredicateId", "OV", "metaDataValue", orientationView));
                }

                // Add new fields to metadata entries
                if (UtilValidate.isNotEmpty(frame)) {
                    metadataEntries.add(UtilMisc.toMap("metaDataPredicateId", "FR", "metaDataValue", frame));
                }

                if (UtilValidate.isNotEmpty(totalFrames)) {
                    metadataEntries.add(UtilMisc.toMap("metaDataPredicateId", "TF", "metaDataValue", totalFrames));
                }

                if (UtilValidate.isNotEmpty(plane)) {
                    metadataEntries.add(UtilMisc.toMap("metaDataPredicateId", "PL", "metaDataValue", plane));
                }

                if (UtilValidate.isNotEmpty(plunge)) {
                    metadataEntries.add(UtilMisc.toMap("metaDataPredicateId", "PU", "metaDataValue", plunge));
                }

                if (UtilValidate.isNotEmpty(totalPlanes)) {
                    metadataEntries.add(UtilMisc.toMap("metaDataPredicateId", "TP", "metaDataValue", totalPlanes));
                }

                // Create all DataResourceMetadata entries
                for (Map<String, String> entry : metadataEntries) {
                    // Checking for null values before creating the entry
                    String metaDataPredicateId = entry.get("metaDataPredicateId");
                    String metaDataValue = entry.get("metaDataValue");

                    if (UtilValidate.isNotEmpty(metaDataPredicateId) && UtilValidate.isNotEmpty(metaDataValue)) {
                        Map<String, Object> dataResourceMetaFields = UtilMisc.toMap(
                                "dataResourceId", dataResourceId, // Use of generated dataResourceId
                                "metaDataPredicateId", metaDataPredicateId,
                                "metaDataValue", metaDataValue,
                                "userLogin", userLogin
                        );
                        Map<String, Object> createDataResourceMetaDataResult = dctx.getDispatcher().runSync("createDataResourceMetaData", dataResourceMetaFields);
                    }
                }

            }
        }
    }

    private static void createProductCategory(Delegator delegator, String productCategoryId, String productCategoryTypeId, String description, DispatchContext dctx, GenericValue userLogin) throws GenericEntityException, GenericServiceException {
        // Check if the product category already exists
        GenericValue existingCategory = delegator.findOne("ProductCategory", UtilMisc.toMap("productCategoryId", productCategoryId), false);
        if (UtilValidate.isNotEmpty(existingCategory)) {
            // Log that the category already exists
            Debug.log("Product category with ID " + productCategoryId + " already exists. Skipping creation.");
            return; // Skip creating the category
        }

        // Create the new product category since it doesn't exist
        Map<String, Object> categoryMap = UtilMisc.toMap(
                "productCategoryId", productCategoryId,
                "productCategoryTypeId", productCategoryTypeId,
                "description", description,
                "userLogin", userLogin
        );
        Map<String, Object> createProductCategoryResult = dctx.getDispatcher().runSync("createProductCategory", categoryMap);
    }


    // Helper method to link a product to a category (ProductCategoryMember)
    private static void linkProductToCategory(Delegator delegator, String productId, String productCategoryId, DispatchContext dctx, GenericValue userLogin) throws GenericEntityException, GenericServiceException {
        Map<String, Object> categoryMemberMap = UtilMisc.toMap(
                "productCategoryId", productCategoryId,
                "productId", productId,
                "userLogin", userLogin
        );

        Map<String, Object> addProductToCategoryResult = dctx.getDispatcher().runSync("addProductToCategory", categoryMemberMap);
    }

    // Helper method to add or update a ProductAttribute
    private static void createOrUpdateProductAttribute(Delegator delegator, String productId, String attrName, String attrValue, DispatchContext dctx, GenericValue userLogin) throws GenericEntityException, GenericServiceException {
        GenericValue productAttr = delegator.findOne("ProductAttribute", UtilMisc.toMap("productId", productId, "attrName", attrName), false);
        if (UtilValidate.isEmpty(productAttr)) {
            Map<String, Object> attrMap = UtilMisc.toMap(
                    "productId", productId,
                    "attrName", attrName,
                    "attrValue", attrValue,
                    "userLogin", userLogin
            );
            Map<String, Object> createProductAttributeResult = dctx.getDispatcher().runSync("createProductAttribute", attrMap);
        }
    }

    private static void createIdentifiers(Delegator delegator, Element itemElement, String productId, DispatchContext dctx, GenericValue userLogin) throws GenericEntityException, GenericServiceException {
        String brandAAIAID = getTextContent(itemElement, "BrandAAIAID");
        String brandLabel = getTextContent(itemElement, "BrandLabel");
        String vmrsBrandId = getTextContent(itemElement, "VMRSBrandID");
        String UNSPSC = getTextContent(itemElement, "UNSPSC");
        String vmrsCode = getTextContent(itemElement, "VMRSCode");
        String manufacturerGroup = getTextContent(itemElement, "Group");
        String manufacturerSubGroup = getTextContent(itemElement, "SubGroup");
        String partTerminologyId = getTextContent(itemElement, "PartTerminologyID");
        String aaiaProductCategoryCode = getTextContent(itemElement, "AAIAProductCategoryCode");

        // Create Product Category for BrandAAIAID
        if (UtilValidate.isNotEmpty(brandAAIAID)) {
            createProductCategory(delegator, brandAAIAID, "BRAND", brandLabel, dctx, userLogin); // Using BrandLabel as description
            // Link the product to the BrandAAIAID category
            linkProductToCategory(delegator, productId, brandAAIAID, dctx, userLogin);
        }

        // Always create the product category for VMRSBrandID
        if (UtilValidate.isNotEmpty(vmrsBrandId)) {
            createProductCategory(delegator, vmrsBrandId, "VMRS", "VMRS Brand", dctx, userLogin);
            // Link the product to the VMRSBrand category
            linkProductToCategory(delegator, productId, vmrsBrandId, dctx, userLogin);
        }

        // Create and link the product category for AAIAProductCategoryCode
        if (UtilValidate.isNotEmpty(aaiaProductCategoryCode)) {
            createProductCategory(delegator, aaiaProductCategoryCode, "AAIA", "AAIA Product Category", dctx, userLogin);
            // Link the product to the AAIA category
            linkProductToCategory(delegator, productId, aaiaProductCategoryCode, dctx, userLogin);
        }

        // Add VMRSCode as a Product Attribute, if vmrsCode is not null or empty
        if (UtilValidate.isNotEmpty(vmrsCode)) {
            createOrUpdateProductAttribute(delegator, productId, "VMRSCode", vmrsCode, dctx, userLogin);
        }

        // Add UNSPSC as a Product Attribute, if UNSPSC is not null or empty
        if (UtilValidate.isNotEmpty(UNSPSC)) {
            createOrUpdateProductAttribute(delegator, productId, "UNSPSC", UNSPSC, dctx, userLogin);
        }

        // Add Manufacturer Group and SubGroup as Product Attributes, if they are not null or empty
        if (UtilValidate.isNotEmpty(manufacturerGroup)) {
            createOrUpdateProductAttribute(delegator, productId, "ManufacturerGroup", manufacturerGroup, dctx, userLogin);
        }
        if (UtilValidate.isNotEmpty(manufacturerSubGroup)) {
            createOrUpdateProductAttribute(delegator, productId, "ManufacturerSubGroup", manufacturerSubGroup, dctx, userLogin);
        }

        // Add PartTerminologyID as a Product Attribute, if it's not null or empty
        if (UtilValidate.isNotEmpty(partTerminologyId)) {
            createOrUpdateProductAttribute(delegator, productId, "PartTerminologyID", partTerminologyId, dctx, userLogin);
        }
        Debug.log(brandAAIAID, brandLabel, vmrsBrandId, UNSPSC, vmrsCode, manufacturerGroup, manufacturerSubGroup, partTerminologyId, aaiaProductCategoryCode);
    }


    /*=======  END ======== */
    public static void extractDataFromXML(DispatchContext dctx, Document document, Map<String, Object> context) throws GenericServiceException {
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            NodeList itemList = rootElement.getElementsByTagName("Item");

            for (int i = 0; i < itemList.getLength(); i++) {
                Element itemElement = (Element) itemList.item(i);

                // Extracting data from the XML
                String baseItemId = getTextContent(itemElement, "BaseItemID");
                String partNumber = getTextContent(itemElement, "PartNumber");
                String hazardousMaterialCode = getTextContent(itemElement, "HazardousMaterialCode");
                String itemEffectiveDate = getTextContent(itemElement, "ItemEffectiveDate");
                String availableDate = getTextContent(itemElement, "AvailableDate");
                String quantityPerApplication = getTextContent(itemElement, "QuantityPerApplication");
                String minimumOrderQuantity = getTextContent(itemElement, "MinimumOrderQuantity");

                Delegator delegator = dctx.getDelegator();

                // Parsing dates to the correct format
                Timestamp introductionDate = itemEffectiveDate.isEmpty() ? null : Timestamp.valueOf(itemEffectiveDate + " 00:00:00");
                Timestamp releaseDate = availableDate.isEmpty() ? null : Timestamp.valueOf(availableDate + " 00:00:00");

                // Data injections starts from here..

                GenericValue userLogin = (GenericValue) context.get("userLogin");

                //product
                try {
                    createItem(delegator, partNumber, introductionDate, releaseDate, quantityPerApplication, minimumOrderQuantity, baseItemId, dctx, userLogin);
                } catch (GenericEntityException e) {
                    throw new RuntimeException(e);
                }

                // ID's , Code's and Identifier's

                try {
                    createIdentifiers(delegator, itemElement, partNumber, dctx, userLogin);
                } catch (GenericEntityException e) {
                    throw new RuntimeException(e);
                }

                // Feature
                try {
                    createHazmatFeature(delegator, hazardousMaterialCode, partNumber, dctx, userLogin);
                } catch (GenericEntityException e) {
                    throw new RuntimeException(e);
                } catch (GenericServiceException e) {
                    throw new RuntimeException(e);
                }
                try {
                    saveItemLevelGTIN(delegator, itemElement, partNumber, userLogin, dctx);
                } catch (GenericEntityException e) {
                    throw new RuntimeException(e);
                }
                // Description
                try {
                    storeProductDescriptions(partNumber, itemElement, dctx, userLogin);
                } catch (GenericEntityException e) {
                    throw new RuntimeException(e);
                }

                // Price
                try {
                    storePrices(delegator, partNumber, itemElement, dctx, userLogin);
                } catch (GenericEntityException e) {
                    throw new RuntimeException(e);
                }

                // EXPI
                try {
                    storeExtendedProductInformation(delegator, partNumber, itemElement, dctx, userLogin);
                } catch (GenericEntityException | GenericServiceException e) {
                    throw new RuntimeException(e);
                }


                // Product Attributes
                try {
                    storeProductAttributes(delegator, partNumber, itemElement, dctx, userLogin);
                } catch (GenericEntityException e) {
                    throw new RuntimeException(e);
                }

                // Digital Asset
                try {
                    storeDigitalAssets(delegator, partNumber, itemElement, dctx, userLogin);
                } catch (GenericEntityException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static Map<String, Object> processPIESXML(DispatchContext dctx, Map<String, Object> context) throws GenericServiceException {
        Debug.log("========Started processing the ByteBuffer==================");

        ByteBuffer fileBuffer = (ByteBuffer) context.get("fileName");

        if (fileBuffer.remaining() == 0) {
            return ServiceUtil.returnError("No file passed or the file is Empty ! ");
        }

        String successMessage = "Successfully parsed the XML";

        Debug.log("========Creating a new read-only copy for XML parsing==================");

        ByteBuffer parseBuffer = fileBuffer.asReadOnlyBuffer();

        Debug.log("========Creating Input Stream==================");

        InputStream inputStream = byteBufferToInputStream(parseBuffer);

        Debug.log("========XML parsing using DocumentBuilderFactory and DocumentBuilder==================");

        Document document = parseXML(inputStream);

        Debug.log("========Extracting Data from the XML==================");

        extractDataFromXML(dctx, document, context);

        Debug.log("========Returned success message==================");

        return ServiceUtil.returnSuccess(successMessage);

    }

}

// The buffer get's depleted (or "Consumed" ) when you read from it.
// when trying to delete the content  I first need to delete it's 3 reference in contenKeyword table.
