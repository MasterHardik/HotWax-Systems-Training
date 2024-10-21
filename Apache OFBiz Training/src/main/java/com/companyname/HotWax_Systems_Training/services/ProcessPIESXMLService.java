package com.companyname.HotWax_Systems_Training.services;

import org.apache.ofbiz.base.util.*;
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
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;

import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.util.EntityQuery;

public class ProcessPIESXMLService {

    private static final String MODULE = ProcessPIESXMLService.class.getName();

    private static String getElementTextContent(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        return (nodeList.getLength() > 0) ? nodeList.item(0).getTextContent() : ""; // or return a default value
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

    private static void createHazmatFeature(String hazardousMaterialCode, String productId, DispatchContext dctx, GenericValue userLogin) throws GenericEntityException, GenericServiceException {

        boolean exist = EntityQuery.use(dctx.getDelegator()).from("ProductFeature").where("description", hazardousMaterialCode, "productFeatureTypeId", "HAZMAT").queryCount() > 0;
        if (!exist) {

            Map<String, Object> productFeatureParams = UtilMisc.toMap(
                    "productFeatureTypeId", "HAZMAT",
                    "description", hazardousMaterialCode,
                    "userLogin", userLogin
            );

            Map<String, Object> result = dctx.getDispatcher().runSync("createProductFeature", productFeatureParams);

            String productFeatureId = (String) result.get("productFeatureId");

            Debug.log("Successfully created ProductFeature with ID: " + productFeatureId, MODULE);

            Map<String, Object> addFeatureToProdParams = UtilMisc.toMap(
                    "productId", productId,
                    "productFeatureId", productFeatureId,
                    "productFeatureApplTypeId", "STANDARD_FEATURE",
                    "userLogin", userLogin
            );
            dctx.getDispatcher().runSync("applyFeatureToProduct", addFeatureToProdParams);
            Debug.logInfo("Associated : " + productId + "<==to==>" + productFeatureId, MODULE);
        }
    }


    public static void saveItemLevelGTIN(String gtinQualifier, String itemLevelGTIN, String productId, GenericValue userLogin, DispatchContext dctx) throws GenericEntityException, GenericServiceException {

        // Parsing 'ItemLevelGTIN' with 'GTINQualifier' attribute
        if (UtilValidate.isEmpty(gtinQualifier) && UtilValidate.isEmpty(itemLevelGTIN)) {
            Debug.logWarning("ItemLevelGTIN element not found", MODULE);
            return;
        }

        // Logging or processing the GTIN data
        Debug.logInfo("GTINQualifier: " + gtinQualifier + ", ItemLevelGTIN: " + itemLevelGTIN, MODULE);


        // Check if a GoodIdentification entry already exists for this product and gtinQualifier
        GenericValue goodIdentification = EntityQuery.use(dctx.getDelegator())
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

    public static void createItem(String productId, String itemEffectiveDate, String availableDate, String quantityPerApplication, String minimumOrderQuantity, String baseItemId, DispatchContext dctx, GenericValue userLogin) throws GenericEntityException, GenericServiceException {
        // Checking if the product already exists
        GenericValue existingProduct = EntityQuery.use(dctx.getDelegator()).from("Product").where("productId", productId).queryOne();

        // Parsing dates to the correct format
        Timestamp introductionDate = itemEffectiveDate == null ? UtilDateTime.nowTimestamp() : Timestamp.valueOf(itemEffectiveDate + " 00:00:00");
        Timestamp releaseDate = availableDate == null ? null : Timestamp.valueOf(availableDate + " 00:00:00");


        if (existingProduct != null) {
            Debug.logWarning("Product with productId: " + productId + " already exists.", MODULE);
            return;
        }

        Map<String, Object> productParams = UtilMisc.toMap(
                "productId", productId,
                "productTypeId", "FINISHED_GOOD",
                "introductionDate", introductionDate,
                "quantityIncluded", quantityPerApplication,
                "releaseDate", releaseDate,
                "internalName", baseItemId,
                "quantityIncluded", minimumOrderQuantity,
                "userLogin", userLogin
        );

        Map<String, Object> result = dctx.getDispatcher().runSync("createProduct", productParams);

        // Log product creation
        Debug.logInfo("Product created successfully with productId: " + result.get("productId"), MODULE);
    }

    public static void storeProductDescriptions(String languageCode, String descriptionCode, String sequenceAttr, String descriptionText, String productId, DispatchContext dctx, GenericValue userLogin) throws GenericEntityException, GenericServiceException {
        String contentId = dctx.getDelegator().getNextSeqId("contentId");
        descriptionText = (descriptionText != null && descriptionText.length() > 255) ? descriptionText.substring(0, 255) : descriptionText;

        Debug.logInfo("=========" + descriptionText + "=============", MODULE);
        Map<String, Object> contentParams = UtilMisc.toMap(
                "contentId", contentId,
                "contentName", descriptionCode,
                "description", descriptionText,
                "localeString", languageCode,
                "userLogin", userLogin
        );

        Map<String, Object> result = dctx.getDispatcher().runSync("createContent", contentParams);

        // Now associate this content with the product in the ProductContent table
        Map<String, Object> productContentParams = UtilMisc.toMap(
                "contentId", contentId,
                "productContentTypeId", "DESCRIPTION",
                "productId", productId,
                "sequenceNum", sequenceAttr,
                "userLogin", userLogin
        );

        Map<String, Object> prodContentResult = dctx.getDispatcher().runSync("createProductContent", productContentParams);
        Debug.logInfo("Descriptions stored successfully with contentId: " + prodContentResult.get("contentId") + " and productId: " + productId, MODULE);
    }

    public static String createProductPriceType(String priceTypeId, String description, DispatchContext dctx, GenericValue userLogin) throws GenericEntityException, GenericServiceException {
        // Check if the ProductPriceType already exists
        GenericValue existingProductPriceType = EntityQuery.use(dctx.getDelegator())
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

    public static void storePrices(String productId, String priceTypeId, String currencyCode, String effectiveDateStr, String expirationDateStr, String priceValue, String priceBreakUOM, String priceBreakValue, DispatchContext dctx, GenericValue userLogin) throws GenericEntityException, GenericServiceException {
        // Check if the ProductPrice already exists
        boolean priceExists = EntityQuery.use(dctx.getDelegator())
                .from("ProductPrice")
                .where("productId", productId,
                        "productPriceTypeId", priceTypeId,
                        "currencyUomId", currencyCode,
                        "productPricePurposeId", "PURCHASE")
                .queryCount() > 0;

        if (!priceExists) {

            priceTypeId = createProductPriceType(priceTypeId, "", dctx, userLogin);

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

    public static void storeExtendedProductInformation(String productId, String expiCode, String languageCode, String description, DispatchContext dctx, GenericValue userLogin) throws GenericEntityException, GenericServiceException {
        // Check if the ProductFeatureType "EXPI" exists, if not, create it , also we  can comment it if making manually
        boolean productFeatureTypeEntityExist = EntityQuery.use(dctx.getDelegator())
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
        Map<String, Object> productFeatureParams = UtilMisc.toMap(
                "productFeatureTypeId", "EXPI",
                "idCode", expiCode,
                "description", description,
                "userLogin", userLogin
        );

        Map<String, Object> result = dctx.getDispatcher().runSync("createProductFeature", productFeatureParams);

        String productFeatureId = (String) result.get("productFeatureId");

        // Create a new record in the ProductFeatureAppl table

        Map<String, Object> addFeatureToProdParams = UtilMisc.toMap(
                "productId", productId,
                "productFeatureId", productFeatureId,
                "productFeatureApplTypeId", "STANDARD_FEATURE",
                "fromDate", UtilDateTime.nowTimestamp(),
                "userLogin", userLogin
        );
        dctx.getDispatcher().runSync("applyFeatureToProduct", addFeatureToProdParams);
        Debug.logInfo("Associated: " + productId + "<==to==>" + productFeatureId + " | Stored with productFeatureId: " + result.get("productFeatureId") + ", expiCode: " + expiCode, MODULE);
//        }
        Debug.logInfo("Extended Product Information stored successfully for productId: " + productId, MODULE);
    }

    private static void storeProductAttributes(String partNumber, String attributeId, String padbAttribute, String attributeUom, String recordNumber, String attributeValue, DispatchContext dctx, GenericValue userLogin) throws GenericEntityException, GenericServiceException {

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
        GenericValue existingProductAttribute = dctx.getDelegator().findOne("ProductAttribute",
                UtilMisc.toMap("productId", partNumber, "attrName", attributeId), false);

        fields.put("userLogin", userLogin);

        if (UtilValidate.isEmpty(existingProductAttribute)) {
            Map<String, Object> result = dctx.getDispatcher().runSync("createProductAttribute", fields);
            fields.remove("userLogin");
            Debug.log("Created new ProductAttribute: " + fields, MODULE);
        } else {
            Debug.log("ProductAttribute already exists: " + attributeId + " for productId: " + partNumber, MODULE);
        }
    }


    private static void storeDigitalAssets(String partNumber, String assetId, String languageCode, String fileName, String fileType, String representation, String fileSize, String resolution, String colorMode, String background, String orientationView, String filePath, String uri, String country, String frame, String totalFrames, String plane, String plunge, String totalPlanes, String assetHeight, String assetWidth, String uom, String assetDate, DispatchContext dctx, GenericValue userLogin) throws GenericEntityException, GenericServiceException {
            Debug.logInfo(filePath, MODULE);
            // Create DataResource
            Map<String, Object> dataResourceFields = UtilMisc.toMap(
                    "mimeTypeId", fileType,
                    "objectInfo", filePath,
                    "dataResourceName", fileName,
                    "userLogin", userLogin
            );

            Map<String, Object> createDataResourceResult = dctx.getDispatcher().runSync("createDataResource", dataResourceFields);

            // Create Content
            Map<String, Object> contentFields = UtilMisc.toMap(
                    "contentTypeId", "DFI", // Assuming "DFI" as the content type
                    "mimeTypeId", fileType,
                    "contentName", fileName,
                    "description", uri,
                    "serviceName", assetId,
                    "localeString", languageCode,
                    "dataResourceId", createDataResourceResult.get("dataResourceId"),
                    "userLogin", userLogin
            );

            Map<String, Object> createContentResult = dctx.getDispatcher().runSync("createContent", contentFields);

            // Create ProductContent
            Map<String, Object> productContentFields = UtilMisc.toMap(
                    "productId", partNumber,
                    "productContentTypeId", "DA", // Assuming "DA" as the product content type
                    "contentId", createContentResult.get("contentId"),
                    "userLogin", userLogin
            );

            Map<String, Object> createProductContentResult = dctx.getDispatcher().runSync("createProductContent", productContentFields);

            // Create Content Attributes (Dimensions)
            if (UtilValidate.isNotEmpty(assetHeight) && UtilValidate.isNotEmpty(assetWidth)) {
                Map<String, Object> contentAttrHeightFields = UtilMisc.toMap(
                        "contentId", createContentResult.get("contentId"),
                        "attrName", "AssetHeight",
                        "attrValue", assetHeight,
                        "attrDescription", uom,
                        "userLogin", userLogin
                );

                dctx.getDispatcher().runSync("createContentAttribute", contentAttrHeightFields);

                Map<String, Object> contentAttrWidthFields = UtilMisc.toMap(
                        "contentId", createContentResult.get("contentId"),
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
                            "dataResourceId", createDataResourceResult.get("dataResourceId"), // Use of generated dataResourceId
                            "metaDataPredicateId", metaDataPredicateId,
                            "metaDataValue", metaDataValue,
                            "userLogin", userLogin
                    );
                    Map<String, Object> createDataResourceMetaDataResult = dctx.getDispatcher().runSync("createDataResourceMetaData", dataResourceMetaFields);
                }
            }
    }

    private static void createProductCategory(String productCategoryId, String productCategoryTypeId, String description, DispatchContext dctx, GenericValue userLogin) throws GenericEntityException, GenericServiceException {
        // Check if the product category already exists
        GenericValue existingCategory = dctx.getDelegator().findOne("ProductCategory", UtilMisc.toMap("productCategoryId", productCategoryId), false);
        if (UtilValidate.isNotEmpty(existingCategory)) {
            // Log that the category already exists
            Debug.log("Product category with ID " + productCategoryId + " already exists. Skipping creation.", MODULE);
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
    private static void linkProductToCategory(String productId, String productCategoryId, DispatchContext dctx, GenericValue userLogin) throws GenericEntityException, GenericServiceException {
        Map<String, Object> categoryMemberMap = UtilMisc.toMap(
                "fromDate", UtilDateTime.nowTimestamp(),
                "productCategoryId", productCategoryId,
                "productId", productId,
                "userLogin", userLogin
        );

        Map<String, Object> addProductToCategoryResult = dctx.getDispatcher().runSync("addProductToCategory", categoryMemberMap);
    }

    // Helper method to add or update a ProductAttribute
    private static void createOrUpdateProductAttribute(String productId, String attrName, String attrValue, DispatchContext dctx, GenericValue userLogin) throws GenericEntityException, GenericServiceException {
        GenericValue productAttr = dctx.getDelegator().findOne("ProductAttribute", UtilMisc.toMap("productId", productId, "attrName", attrName), false);
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

    private static void createIdentifiers(String brandAAIAID, String brandLabel, String vmrsBrandId, String UNSPSC, String vmrsCode, String manufacturerGroup, String manufacturerSubGroup, String partTerminologyId, String aaiaProductCategoryCode, String productId, DispatchContext dctx, GenericValue userLogin) throws GenericEntityException,

            GenericServiceException {

        // Create Product Category for BrandAAIAID
        if (UtilValidate.isNotEmpty(brandAAIAID)) {
            createProductCategory(brandAAIAID, "BRAND", brandLabel, dctx, userLogin); // Using BrandLabel as description
            // Link the product to the BrandAAIAID category
            linkProductToCategory(productId, brandAAIAID, dctx, userLogin);
        }

        // Always create the product category for VMRSBrandID
        if (UtilValidate.isNotEmpty(vmrsBrandId)) {
            createProductCategory(vmrsBrandId, "VMRS", "VMRS Brand", dctx, userLogin);
            // Link the product to the VMRSBrand category
            linkProductToCategory(productId, vmrsBrandId, dctx, userLogin);
        }

        // Create and link the product category for AAIAProductCategoryCode
        if (UtilValidate.isNotEmpty(aaiaProductCategoryCode)) {
            createProductCategory(aaiaProductCategoryCode, "AAIA", "AAIA Product Category", dctx, userLogin);
            // Link the product to the AAIA category
            linkProductToCategory(productId, aaiaProductCategoryCode, dctx, userLogin);
        }

        // Add VMRSCode as a Product Attribute, if vmrsCode is not null or empty
        if (UtilValidate.isNotEmpty(vmrsCode)) {
            createOrUpdateProductAttribute(productId, "VMRSCode", vmrsCode, dctx, userLogin);
        }

        // Add UNSPSC as a Product Attribute, if UNSPSC is not null or empty
        if (UtilValidate.isNotEmpty(UNSPSC)) {
            createOrUpdateProductAttribute(productId, "UNSPSC", UNSPSC, dctx, userLogin);
        }

        // Add Manufacturer Group and SubGroup as Product Attributes, if they are not null or empty
        if (UtilValidate.isNotEmpty(manufacturerGroup)) {
            createOrUpdateProductAttribute(productId, "ManufacturerGroup", manufacturerGroup, dctx, userLogin);
        }
        if (UtilValidate.isNotEmpty(manufacturerSubGroup)) {
            createOrUpdateProductAttribute(productId, "ManufacturerSubGroup", manufacturerSubGroup, dctx, userLogin);
        }

        // Add PartTerminologyID as a Product Attribute, if it's not null or empty
        if (UtilValidate.isNotEmpty(partTerminologyId)) {
            createOrUpdateProductAttribute(productId, "PartTerminologyID", partTerminologyId, dctx, userLogin);
        }
        Debug.log(brandAAIAID, brandLabel, vmrsBrandId, UNSPSC, vmrsCode, manufacturerGroup, manufacturerSubGroup, partTerminologyId, aaiaProductCategoryCode);
    }


    /*=======  END ======== */
    static class ItemHandler extends DefaultHandler {

        private int ItemNumberTrack = 0;
        private boolean insideDescriptionElement = false; // Flag to know when you're inside the <Description> element

        private boolean insideEXPIElement = false; // Flag to know when you're inside the <Description> element
        private boolean insidePAElement = false; // Flag to know when you're inside the <Description> element
        private boolean insideItemLevelGTIN = false; // Flag to know when you're inside the <Description> element
        private boolean insideHazardousMaterialCode = false; // Flag for <HazardousMaterialCode>
        private boolean insideBaseItemID = false; // Flag for <BaseItemID>
        private boolean insidePartNumber = false; // Flag for <PartNumber>
        private boolean insideBrandAAIAID = false; // Flag for <BrandAAIAID>
        private boolean insideBrandLabel = false; // Flag for <BrandLabel>
        private boolean insideVMRSBrandID = false; // Flag for <VMRSBrandID>
        private boolean insideQuantityPerApplication = false; // Flag for <QuantityPerApplication>

        private boolean insideAvailableDate = false;  // Flag for <AvailableDate>
        private boolean insideMinimumOrderQuantity = false;  // Flag for <MinimumOrderQuantity>
        private boolean isGroup = false;
        private boolean isSubGroup = false;
        private boolean isAAIAProductCategoryCode = false;
        private boolean isUNSPSC = false;
        private boolean isPartTerminologyID = false;
        private boolean isVMRSCode = false;
        private String minOrderUOM = ""; // To store UOM for <MinimumOrderQuantity>

        private StringBuilder characterBuffer = new StringBuilder();
        private StringBuilder content = new StringBuilder();
        private Item currentItem;
        private List<Item> items = new ArrayList<>();
        private String uomValue = "";
        private boolean insideItemEffectiveDate = false;
        private String currencyCodeValue = "";
        private boolean isPriceBreak = false;
        private String priceValue = "";
        private String priceBreakValue;
        private String effectiveDate;
        private String expirationDate;
        private boolean isCurrencycode = false;
        private boolean isEffectiveDate = false;
        private boolean isExpirationDate = false;
        private boolean isCurrencyCode = false;
        private boolean insidePriceValue = false;
        private String priceType;
        private boolean isPrice;
        private String uom;
        private String currencyCode;
        private String maintenanceType;
        private String assetID;
        private String languageCode;
        private String assetDateType;
        private boolean isFileName;
        private boolean isAssetType;
        private boolean isFileType;
        private boolean isRepresentation;
        private boolean isFileSize;
        private boolean isColorMode;
        private boolean isOrientationView;
        private boolean isAssetHeight;
        private boolean isAssetWidth;
        private boolean isFilePath;
        private boolean isBackground;
        private boolean isResolution;
        private boolean isCountry;
        private boolean isTotalFrames;
        private boolean isFrame;
        private boolean isPlane;
        private boolean isHemisphere;
        private boolean isTotalPlanes;
        private boolean isPlunge;
        private List<DigitalFileInformation> digitalFiles;
        private String fileName;
        private String assetType;
        private String fileType;
        private String representation;
        private String fileSize;
        private String country;
        private String plane;
        private String frame;
        private String hemisphere;
        private String plunge;
        private String resolution;
        private String colorMode;
        private String totalFrames;
        private String background;
        private String orientationView;
        private String assetHeight;
        private String assetDate;

        private String totalPlanes;
        private String assetWidth;
        private String filePath;
        private String uri;
        private boolean isURI;

        private DispatchContext dctx;

        Map<String, Object> context;

        public ItemHandler(DispatchContext dctx, Map<String, Object> context) {
            this.dctx = dctx;
            this.context = context;
        }

        // Inner class to represent Item data
        class Item {
            public String uom;
            public String minOrderUOM;
            public String AAIAProductCategoryCode;
            public String UNSPSC;
            public String VMRSCode;
            public String mfsg;
            public String partTerminologyId;
            String maintenanceType;
            String hazardousMaterialCode;
            String baseItemID;
            String partNumber;
            String brandAAIAID;
            String brandLabel;
            String vmrsBrandID;
            String quantityPerApplication;
            String itemEffectiveDate;
            String availableDate;
            String minimumOrderQuantity;
            String mfg;

            ItemLevelGTIN gtin;

            private boolean isFirstPN = true;

            List<Description> descriptions = new ArrayList<>();
            List<ExtendedProductInformation> ExtendedInformation = new ArrayList<>();
            List<Price> prices = new ArrayList<>();
            List<ProductAttribute> productAttributes = new ArrayList<>();
            List<DigitalFileInformation> digitalAssets = new ArrayList<>();
        }

        // Inner classes for nested structures
        public class Description {
            String languageCode;
            String maintenanceType;
            String descriptionCode;
            String sequence;
            String descriptionText; // This will store the actual content

            @Override
            public String toString() {
                return "Description [languageCode=" + languageCode + ", maintenanceType=" + maintenanceType
                        + ", descriptionCode=" + descriptionCode + ", sequence=" + sequence + ", descriptionText="
                        + descriptionText + "]";
            }
        }

        public class ItemLevelGTIN {
            String GTINQualifier;
            String value;

            @Override
            public String toString() {
                return "ItemLevelGTIN [GTINQualifier=" + GTINQualifier + ", value=" + value + "]";
            }
        }


        class Price {
            String priceType;
            String currencyCode;
            String effectiveDate;
            String expirationDate;
            String priceValue;
            String priceBreak; // Added for PriceBreak
            String uom;

            // Override toString for logging
            @Override
            public String toString() {
                return "Price{" +
                        "priceType='" + priceType + '\'' +
                        ", currencyCode='" + currencyCode + '\'' +
                        ", effectiveDate='" + effectiveDate + '\'' +
                        ", expirationDate='" + expirationDate + '\'' +
                        ", price='" + priceValue + '\'' +
                        ", priceBreak='" + priceBreak + '\'' +
                        ", uom='" + uom + '\'' +
                        '}';
            }
        }

        public class ProductAttribute {
            String attributeID;
            String attributeUOM;
            String RecordNumber;
            String PADBAttribute;
            String value;

            @Override
            public String toString() {
                return "ProductAttribute [attributeID=" + attributeID + ", attributeID=" + attributeUOM
                        + ", RecordNumber=" + RecordNumber + " value=" + value + "PADBAttribute" + PADBAttribute + "]";
            }
        }

        public class ExtendedProductInformation {
            String maintenanceType;
            String languageCode;
            String expiCode;
            String content; // This will store the actual text content

            @Override
            public String toString() {
                return "ExtendedProductInformation [maintenanceType=" + maintenanceType + ", languageCode=" + languageCode
                        + ", expiCode=" + expiCode + ", content=" + content + "]";
            }
        }

        public class DigitalFileInformation {
            private String maintenanceType;
            private String assetID;
            private String languageCode;
            private String fileName;
            private String assetType;
            private String fileType;
            private String representation;
            private String fileSize;
            private String resolution;
            private String colorMode;
            private String background;
            private String orientationView;
            private String uom;
            private String assetHeight;
            private String assetWidth;
            private String filePath;
            private String uri;
            private String assetDateType;
            private String assetDate;
            private String country;

            // Other fields for 360-specific assets
            private String frame;
            private String totalFrames;
            private String plane;
            private String hemisphere;
            private String plunge;
            private String totalPlanes;

            @Override
            public String toString() {
                return "DigitalFileInformation{" +
                        "maintenanceType='" + maintenanceType + '\'' +
                        ", assetID='" + assetID + '\'' +
                        ", languageCode='" + languageCode + '\'' +
                        ", fileName='" + fileName + '\'' +
                        ", assetType='" + assetType + '\'' +
                        ", fileType='" + fileType + '\'' +
                        ", representation='" + representation + '\'' +
                        ", fileSize=" + fileSize +
                        ", resolution=" + resolution +
                        ", colorMode='" + colorMode + '\'' +
                        ", background='" + background + '\'' +
                        ", orientationView='" + orientationView + '\'' +
                        ", uom='" + uom + '\'' +
                        ", assetHeight=" + assetHeight +
                        ", assetWidth=" + assetWidth +
                        ", filePath='" + filePath + '\'' +
                        ", uri='" + uri + '\'' +
                        ", assetDateType='" + assetDateType + '\'' +
                        ", assetDate='" + assetDate + '\'' +
                        ", country='" + country + '\'' +
                        ", frame=" + frame +
                        ", totalFrames=" + totalFrames +
                        ", plane=" + plane +
                        ", hemisphere='" + hemisphere + '\'' +
                        ", plunge=" + plunge +
                        ", totalPlanes=" + totalPlanes +
                        '}';
            }
        }


        @Override
        public void startDocument() {
            Debug.logInfo("==== At Start of the Document ====", MODULE);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            content.setLength(0); // Reset content buffer
            if (qName.equals("Item")) {
                ItemNumberTrack++; // Increment the item number first
                System.out.println("======== Processing Item No. : " + ItemNumberTrack + " =================="); // Debug output
                currentItem = new Item();
                currentItem.maintenanceType = attributes.getValue("MaintenanceType");
                System.out.println("Item MaintenanceType: " + currentItem.maintenanceType);
            }

            if (qName.equals("Description")) {
                Description desc = new Description();
                desc.languageCode = attributes.getValue("LanguageCode");
                desc.maintenanceType = attributes.getValue("MaintenanceType");
                desc.descriptionCode = attributes.getValue("DescriptionCode");
                desc.sequence = attributes.getValue("Sequence");
                // Set the flag to true indicating you're inside a <Description> element
                insideDescriptionElement = true;
                // Clear the buffer in case there was any previous text
                characterBuffer.setLength(0);
                currentItem.descriptions.add(desc);
            }

            if (qName.equals("ExtendedProductInformation")) {
                ExtendedProductInformation expi = new ExtendedProductInformation();
                expi.maintenanceType = attributes.getValue("MaintenanceType");
                expi.languageCode = attributes.getValue("LanguageCode");
                expi.expiCode = attributes.getValue("EXPICode");
                insideEXPIElement = true;
                // Clear the buffer in case there was any previous text
                characterBuffer.setLength(0);
                currentItem.ExtendedInformation.add(expi);
            }

            if (qName.equals("ProductAttribute")) {
                ProductAttribute pa = new ProductAttribute();
                pa.attributeID = attributes.getValue("AttributeID");
                pa.attributeUOM = attributes.getValue("AttributeUOM");
                pa.RecordNumber = attributes.getValue("RecordNumber");
                pa.PADBAttribute = attributes.getValue("PADBAttribute");
                insideEXPIElement = true;
                characterBuffer.setLength(0);
                currentItem.productAttributes.add(pa);
            }

            if (qName.equals("ItemLevelGTIN")) {
                currentItem.gtin = new ItemLevelGTIN();
                currentItem.gtin.GTINQualifier = attributes.getValue("GTINQualifier");
                insideItemLevelGTIN = true;
                characterBuffer.setLength(0); // Clear buffer for GTIN value
            }

            if (qName.equals("HazardousMaterialCode")) {
                insideHazardousMaterialCode = true;
                characterBuffer.setLength(0); // Clear buffer for HazardousMaterialCode
            }

            if (qName.equals("BaseItemID")) {
                insideBaseItemID = true;
                characterBuffer.setLength(0); // Clear buffer for BaseItemID
            }

            if (qName.equals("PartNumber")) {
                insidePartNumber = true;
                characterBuffer.setLength(0); // Clear buffer for PartNumber
            }

            if (qName.equals("BrandAAIAID")) {
                insideBrandAAIAID = true;
                characterBuffer.setLength(0); // Clear buffer for BrandAAIAID
            }

            if (qName.equals("BrandLabel")) {
                insideBrandLabel = true;
                characterBuffer.setLength(0); // Clear buffer for BrandLabel
            }

            if (qName.equals("VMRSBrandID")) {
                insideVMRSBrandID = true;
                characterBuffer.setLength(0); // Clear buffer for VMRSBrandID
            }

            if (qName.equals("QuantityPerApplication")) {
                insideQuantityPerApplication = true;
                uomValue = attributes.getValue("UOM"); // Capture UOM attribute value
                characterBuffer.setLength(0); // Clear buffer for QuantityPerApplication
            }

            if (qName.equals("ItemEffectiveDate")) {
                insideItemEffectiveDate = true;  // Set flag when entering <ItemEffectiveDate>
                characterBuffer.setLength(0);    // Clear buffer for ItemEffectiveDate value
            }

            if (qName.equals("AvailableDate")) {
                insideAvailableDate = true;  // Set flag when entering <AvailableDate>
                characterBuffer.setLength(0); // Clear buffer for AvailableDate value
            }

            if (qName.equals("MinimumOrderQuantity")) {
                insideMinimumOrderQuantity = true;  // Set flag for <MinimumOrderQuantity>
                minOrderUOM = attributes.getValue("UOM");  // Capture UOM attribute
                characterBuffer.setLength(0); // Clear buffer for quantity value
            }

            if (qName.equalsIgnoreCase("Group")) {
                isGroup = true;
            }

            if (qName.equalsIgnoreCase("SubGroup")) {
                isSubGroup = true;
            }
            if (qName.equalsIgnoreCase("AAIAProductCategoryCode")) {
                isAAIAProductCategoryCode = true;
            }
            if (qName.equalsIgnoreCase("UNSPSC")) {
                isUNSPSC = true;
            }
            if (qName.equalsIgnoreCase("PartTerminologyID")) {
                isPartTerminologyID = true;
            }
            if (qName.equalsIgnoreCase("VMRSCode")) {
                isVMRSCode = true;
            }

            if (qName.equalsIgnoreCase("Pricing")) {
                priceType = attributes.getValue("PriceType"); // Get PriceType
            }

            if (qName.equalsIgnoreCase("Price")) {
                isPrice = true;
                uom = attributes.getValue("UOM"); // Get Price UOM
            }

            if (qName.equalsIgnoreCase("PriceBreak")) {
                isPriceBreak = true;
            }

            if (qName.equalsIgnoreCase("CurrencyCode")) {
                isCurrencyCode = true; // Set flag for capturing CurrencyCode
            }

            if (qName.equalsIgnoreCase("EffectiveDate")) {
                isEffectiveDate = true; // Set flag for capturing EffectiveDate
            }

            if (qName.equalsIgnoreCase("ExpirationDate")) {
                isExpirationDate = true; // Set flag for capturing ExpirationDate
            }

            if (qName.equalsIgnoreCase("DigitalFileInformation")) {
                maintenanceType = attributes.getValue("MaintenanceType");
                assetID = attributes.getValue("AssetID");
                languageCode = attributes.getValue("LanguageCode");
            }

            if (qName.equalsIgnoreCase("AssetDimensions")) {
                uom = attributes.getValue("UOM");
            }

            if (qName.equalsIgnoreCase("AssetDate")) {
                assetDateType = attributes.getValue("assetDateType");
            }

            // Set flags for other tags
            if (qName.equalsIgnoreCase("FileName")) {
                isFileName = true;
            }
            if (qName.equalsIgnoreCase("AssetType")) {
                isAssetType = true;
            }
            if (qName.equalsIgnoreCase("FileType")) {
                isFileType = true;
            }
            if (qName.equalsIgnoreCase("Representation")) {
                isRepresentation = true;
            }
            if (qName.equalsIgnoreCase("FileSize")) {
                isFileSize = true;
            }
            if (qName.equalsIgnoreCase("Resolution")) {
                isResolution = true;
            }
            if (qName.equalsIgnoreCase("ColorMode")) {
                isColorMode = true;
            }
            if (qName.equalsIgnoreCase("Background")) {
                isBackground = true;
            }
            if (qName.equalsIgnoreCase("OrientationView")) {
                isOrientationView = true;
            }
            if (qName.equalsIgnoreCase("AssetHeight")) {
                isAssetHeight = true;
            }
            if (qName.equalsIgnoreCase("AssetWidth")) {
                isAssetWidth = true;
            }
            if (qName.equalsIgnoreCase("FilePath")) {
                isFilePath = true;
            }
            if (qName.equalsIgnoreCase("URI")) {
                isURI = true;
            }
            if (qName.equalsIgnoreCase("Country")) {
                isCountry = true;
            }
            // Handle 360-specific elements
            if (qName.equalsIgnoreCase("Frame")) {
                isFrame = true;
            }
            if (qName.equalsIgnoreCase("TotalFrames")) {
                isTotalFrames = true;
            }
            if (qName.equalsIgnoreCase("Plane")) {
                isPlane = true;
            }
            if (qName.equalsIgnoreCase("Hemisphere")) {
                isHemisphere = true;
            }
            if (qName.equalsIgnoreCase("Plunge")) {
                isPlunge = true;
            }
            if (qName.equalsIgnoreCase("TotalPlanes")) {
                isTotalPlanes = true;
            }

        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {

            if (qName.equals("Description")) {
                // Assign the accumulated text to descriptionText for the last added Description object
                Description lastDesc = currentItem.descriptions.get(currentItem.descriptions.size() - 1);
                lastDesc.descriptionText = characterBuffer.toString().trim();  // Assign the accumulated content

                System.out.println("Completed Description Element: " + lastDesc);

                // Reset the flag as we've exited the <Description> element
                insideDescriptionElement = false;
            }


            if (qName.equals("ExtendedProductInformation")) {
                ExtendedProductInformation expi = currentItem.ExtendedInformation.get(currentItem.ExtendedInformation.size() - 1);
                expi.content = characterBuffer.toString().trim();

                System.out.println("Completed ExtendedProductInformation Element: " + expi);

                // Reset the current reference
                insideEXPIElement = false;
            }

            if (qName.equals("ProductAttribute")) {
                ProductAttribute pa = currentItem.productAttributes.get(currentItem.productAttributes.size() - 1);
                pa.value = characterBuffer.toString().trim();

                System.out.println("Completed ExtendedProductInformation Element: " + pa);

                // Reset the current reference
                insidePAElement = false;
            }

            if (qName.equals("ItemLevelGTIN")) {
                currentItem.gtin.value = characterBuffer.toString().trim();
                System.out.println("ItemLevelGTIN: " + currentItem.gtin);
                insideItemLevelGTIN = false;
            }

            if (qName.equals("HazardousMaterialCode")) {
                currentItem.hazardousMaterialCode = characterBuffer.toString().trim();
                System.out.println("HazardousMaterialCode: " + currentItem.hazardousMaterialCode);
                insideHazardousMaterialCode = false;
            }

            if (qName.equals("BaseItemID")) {
                currentItem.baseItemID = characterBuffer.toString().trim();
                System.out.println("BaseItemID: " + currentItem.baseItemID);
                insideBaseItemID = false;
            }
            if (qName.equals("PartNumber")) {
                if (currentItem.isFirstPN) {
                    currentItem.partNumber = characterBuffer.toString().trim();
                    currentItem.isFirstPN = false;
                    System.out.println("PartNumber: " + currentItem.partNumber);
                }
                insidePartNumber = false;
            }

            if (qName.equals("BrandAAIAID")) {
                currentItem.brandAAIAID = characterBuffer.toString().trim();
                System.out.println("BrandAAIAID: " + currentItem.brandAAIAID);
                insideBrandAAIAID = false;
            }

            if (qName.equals("BrandLabel")) {
                currentItem.brandLabel = characterBuffer.toString().trim();
                System.out.println("BrandLabel: " + currentItem.brandLabel);
                insideBrandLabel = false;
            }

            if (qName.equals("VMRSBrandID")) {
                currentItem.vmrsBrandID = characterBuffer.toString().trim();
                System.out.println("VMRSBrandID: " + currentItem.vmrsBrandID);
                insideVMRSBrandID = false;
            }

            if (qName.equals("QuantityPerApplication")) {
                currentItem.quantityPerApplication = String.valueOf(Integer.parseInt(characterBuffer.toString().trim()));
                currentItem.uom = uomValue; // Assign UOM attribute
                System.out.println("QuantityPerApplication: " + currentItem.quantityPerApplication + " " + currentItem.uom);
                insideQuantityPerApplication = false;
            }
            if (qName.equals("ItemEffectiveDate")) {
                currentItem.itemEffectiveDate = characterBuffer.toString().trim(); // Capture the date value
                System.out.println("ItemEffectiveDate: " + currentItem.itemEffectiveDate);
                insideItemEffectiveDate = false;
            }

            if (qName.equals("AvailableDate")) {
                currentItem.availableDate = characterBuffer.toString().trim(); // Capture AvailableDate value
                System.out.println("AvailableDate: " + currentItem.availableDate);
                insideAvailableDate = false;
            }

            if (qName.equals("MinimumOrderQuantity")) {
                currentItem.minimumOrderQuantity = characterBuffer.toString().trim();  // Capture quantity value
                currentItem.minOrderUOM = minOrderUOM;  // Capture UOM value
                System.out.println("MinimumOrderQuantity: " + currentItem.minimumOrderQuantity + " UOM: " + currentItem.minOrderUOM);
                insideMinimumOrderQuantity = false;
            }

            if (qName.equals("Pricing")) {
                Price p = new Price();
                p.priceValue = priceValue;
                p.priceType = priceType;
                p.priceBreak = priceBreakValue;
                p.uom = uomValue;
                p.currencyCode = currencyCode;
                p.effectiveDate = effectiveDate;
                p.expirationDate = expirationDate;
                currentItem.prices.add(p);
                System.out.println(p);
            }

            if (qName.equals("DigitalFileInformation")) {
                DigitalFileInformation digitalFile = new DigitalFileInformation();
                digitalFile.maintenanceType = maintenanceType;
                digitalFile.assetID = assetID;
                digitalFile.languageCode = languageCode;
                digitalFile.fileName = fileName;
                digitalFile.assetType = assetType;
                digitalFile.fileType = fileType;
                digitalFile.representation = representation;
                digitalFile.fileSize = fileSize;
                digitalFile.resolution = resolution;
                digitalFile.colorMode = colorMode;
                digitalFile.background = background;
                digitalFile.orientationView = orientationView;
                digitalFile.uom = uom;
                digitalFile.assetHeight = assetHeight;
                digitalFile.assetWidth = assetWidth;
                digitalFile.filePath = filePath;
                digitalFile.uri = uri;
                digitalFile.assetDateType = assetDateType;
                digitalFile.assetDate = assetDate;
                digitalFile.country = country;
                digitalFile.frame = frame;
                digitalFile.totalFrames = totalFrames;
                digitalFile.plane = plane;
                digitalFile.hemisphere = hemisphere;
                digitalFile.plunge = plunge;
                digitalFile.totalPlanes = totalPlanes;

                // Add the digitalFile to the list or process it as needed
                currentItem.digitalAssets.add(digitalFile);
                System.out.println(digitalFile);
            }

            if (qName.equals("Item")) {
                characterBuffer.setLength(0);

                GenericValue userLogin = (GenericValue) context.get("userLogin");

                //product
                try {
                    currentItem.minimumOrderQuantity = (currentItem.minimumOrderQuantity != null && currentItem.minimumOrderQuantity.length() > 255) ? currentItem.minimumOrderQuantity.substring(0, 255) : currentItem.minimumOrderQuantity;
                    createItem(currentItem.partNumber, currentItem.itemEffectiveDate, currentItem.availableDate, currentItem.quantityPerApplication, currentItem.minimumOrderQuantity, currentItem.baseItemID, dctx, userLogin);
                } catch (GenericEntityException | GenericServiceException e) {
                    throw new RuntimeException(e);
                }

                // ID's , Code's and Identifier's

                try {
                    createIdentifiers(currentItem.brandAAIAID, currentItem.brandLabel, currentItem.vmrsBrandID, currentItem.UNSPSC, currentItem.VMRSCode, currentItem.mfg, currentItem.mfsg, currentItem.partTerminologyId, currentItem.AAIAProductCategoryCode, currentItem.partNumber, dctx, userLogin);
                } catch (GenericEntityException | GenericServiceException e) {
                    throw new RuntimeException(e);
                }

                // Feature
                try {
                    createHazmatFeature(currentItem.hazardousMaterialCode, currentItem.partNumber, dctx, userLogin);
                } catch (GenericEntityException e) {
                    throw new RuntimeException(e);
                } catch (GenericServiceException e) {
                    throw new RuntimeException(e);
                }
                try {
                    ItemLevelGTIN itemLevelGTINElement = currentItem.gtin;
                    saveItemLevelGTIN(itemLevelGTINElement.GTINQualifier, itemLevelGTINElement.value, currentItem.partNumber, userLogin, dctx);
                } catch (GenericEntityException | GenericServiceException e) {
                    throw new RuntimeException(e);
                }

                // Description
                try {
                    if (currentItem.descriptions.size() > 0) {
                        Description d = currentItem.descriptions.get(0);
                        Debug.logInfo("Total descp's found :  " + currentItem.descriptions.size(), MODULE);
                        storeProductDescriptions(d.languageCode, d.descriptionCode, d.sequence, d.descriptionText, currentItem.partNumber, dctx, userLogin);
                    } else {
                        Debug.logInfo("No Descriptions Exist", MODULE);
                    }
                } catch (GenericEntityException | GenericServiceException e) {
                    throw new RuntimeException(e);
                }

                // Price
                try {
                    if (currentItem.prices.size() > 0) {
                        Price p = currentItem.prices.get(0);
                        Debug.logInfo("Total descp's found :  " + currentItem.prices.size(), MODULE);
                        storePrices(currentItem.partNumber, p.priceType, p.currencyCode, p.effectiveDate, p.expirationDate, p.priceValue, p.uom, p.priceBreak, dctx, userLogin);
                    } else {
                        Debug.logInfo("No Pricing Element Exist", MODULE);
                    }
                } catch (GenericEntityException | GenericServiceException e) {
                    throw new RuntimeException(e);
                }


                // EXPI
                try {
                    ExtendedProductInformation epi = currentItem.ExtendedInformation.get(0);
                    storeExtendedProductInformation(currentItem.partNumber, epi.expiCode, epi.languageCode, epi.content, dctx, userLogin);
                } catch (GenericEntityException | GenericServiceException e) {
                    throw new RuntimeException(e);
                }

                // Product Attributes
                try {
                    if (currentItem.productAttributes.size() > 0) {
                        ProductAttribute pa = currentItem.productAttributes.get(0);
                        storeProductAttributes(currentItem.partNumber, pa.attributeID, pa.PADBAttribute, pa.attributeUOM, pa.RecordNumber, pa.value, dctx, userLogin);
                    } else {
                        Debug.logWarning("No ProductAttributes found for partNumber: " + currentItem.partNumber, MODULE);
                    }
                } catch (GenericEntityException | GenericServiceException e) {
                    throw new RuntimeException(e);
                }

                // Digital Asset
                try {
                    if(currentItem.digitalAssets.size()>0){
                    DigitalFileInformation dfi = currentItem.digitalAssets.get(0);
                    storeDigitalAssets(currentItem.partNumber, dfi.assetID, dfi.languageCode, dfi.fileName, dfi.fileType, dfi.representation, dfi.fileSize, dfi.resolution, dfi.colorMode, dfi.background, dfi.orientationView, dfi.filePath, dfi.uri, dfi.country, dfi.frame, dfi.totalFrames, dfi.plane, dfi.plunge, dfi.totalPlanes, dfi.assetHeight, dfi.assetWidth, dfi.uom, dfi.assetDate, dctx, userLogin);
                    }
                    else{
                        Debug.logWarning("No digital assets found for productId : " + currentItem.partNumber, MODULE);
                    }
                } catch (GenericEntityException | GenericServiceException e) {
                    throw new RuntimeException(e);
                }

                Debug.logInfo("======== END Processing Item No. : " + ItemNumberTrack + "==================", MODULE);
            }

        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (insideDescriptionElement || insideEXPIElement || insidePAElement || insideItemLevelGTIN || insideHazardousMaterialCode || insideBaseItemID || insidePartNumber || insideBrandAAIAID || insideBrandLabel || insideVMRSBrandID || insideQuantityPerApplication || insideItemEffectiveDate || insideAvailableDate || insideMinimumOrderQuantity || isExpirationDate || isEffectiveDate || isCurrencycode) {
                // Append characters to the buffer while inside <Description>
                characterBuffer.append(new String(ch, start, length));
            }


            if (isGroup) {
                currentItem.mfg = new String(ch, start, length);
                System.out.println("Manufacturer Group: " + currentItem.mfg);
                isGroup = false;
            }

            if (isSubGroup) {
                currentItem.mfsg = new String(ch, start, length);
                System.out.println("Manufacturer SubGroup: " + currentItem.mfsg);
                isSubGroup = false;
            }

            if (isAAIAProductCategoryCode) {
                currentItem.AAIAProductCategoryCode = new String(ch, start, length);
                System.out.println("AAIA Product Category Code: " + currentItem.AAIAProductCategoryCode);
                isAAIAProductCategoryCode = false;
            }

            if (isUNSPSC) {
                currentItem.UNSPSC = new String(ch, start, length);
                System.out.println("UNSPSC: " + currentItem.UNSPSC);
                isUNSPSC = false;
            }

            if (isPartTerminologyID) {
                currentItem.partTerminologyId = new String(ch, start, length);
                System.out.println("Part Terminology ID: " + currentItem.partTerminologyId);
                isPartTerminologyID = false;
            }

            if (isVMRSCode) {
                currentItem.VMRSCode = new String(ch, start, length);
                System.out.println("VMRS Code: " + currentItem.VMRSCode);
                isVMRSCode = false;
            }

            String value = new String(ch, start, length).trim(); // Get trimmed value

            if (isCurrencyCode) {
                currencyCode = value;
                isCurrencyCode = false;
            }

            if (isEffectiveDate) {
                effectiveDate = value;
                isEffectiveDate = false;
            }

            if (isExpirationDate) {
                expirationDate = value;
                isExpirationDate = false;
            }

            if (isPrice) {
                priceValue = value;
                isPrice = false;
            }

            if (isPriceBreak) {
                priceBreakValue = value;
                isPriceBreak = false;
            }

            if (isFileName) {
                fileName = value;
                isFileName = false;
            }
            if (isAssetType) {
                assetType = value;
                isAssetType = false;
            }
            if (isFileType) {
                fileType = value;
                isFileType = false;
            }
            if (isRepresentation) {
                representation = value;
                isRepresentation = false;
            }
            if (isFileSize) {
                fileSize = value;
                isFileSize = false;
            }
            if (isResolution) {
                resolution = value;
                isResolution = false;
            }
            if (isColorMode) {
                colorMode = value;
                isColorMode = false;
            }
            if (isBackground) {
                background = value;
                isBackground = false;
            }
            if (isOrientationView) {
                orientationView = value;
                isOrientationView = false;
            }
            if (isAssetHeight) {
                assetHeight = value;
                isAssetHeight = false;
            }
            if (isAssetWidth) {
                assetWidth = value;
                isAssetWidth = false;
            }
            if (isFilePath) {
                filePath = value;
                isFilePath = false;
            }
            if (isURI) {
                uri = value;
                isURI = false;
            }
            if (isCountry) {
                country = value;
                isCountry = false;
            }

            // 360-specific elements
            if (isFrame) {
                frame = value;
                isFrame = false;
            }
            if (isTotalFrames) {
                totalFrames = value;
                isTotalFrames = false;
            }
            if (isPlane) {
                plane = value;
                isPlane = false;
            }
            if (isHemisphere) {
                hemisphere = value;
                isHemisphere = false;
            }
            if (isPlunge) {
                plunge = value;
                isPlunge = false;
            }
            if (isTotalPlanes) {
                totalPlanes = value;
                isTotalPlanes = false;
            }

            content.append(ch, start, length);
        }

        public List<Item> getItems() {
            return items;
        }

        @Override
        public void endDocument() {
            Debug.logInfo("==== End of the Document ====", MODULE);
        }
    }


    public static Map<String, Object> processPIESXML(DispatchContext dctx, Map<String, Object> context) throws GenericServiceException, ParserConfigurationException, IOException, SAXException {

        Debug.log("======== Inside processPIESXML service ==================", MODULE);

        // Get the input file path from the context
        String InputFilePath = String.valueOf(context.get("filePath"));

        // Check if the file path ends with .xml (case-insensitive) and append if necessary
        if (!InputFilePath.toLowerCase().endsWith(".xml")) {
            InputFilePath += ".xml";
        }

        // Construct the full file path
        String filePath = "/home/hardik/Desktop/OFBiz_Training/ofbiz-framework/plugins/HotWax-Systems-Training/src/main/java/com/companyname/HotWax_Systems_Training/services/InputXMLFiles/" + InputFilePath;

        // Create a File object to check if the file exists
        File file = new File(filePath);

        // Validate if the file exists
        if (!file.exists()) {
            // Return error if the file is not found
            return ServiceUtil.returnError("File not found: " + InputFilePath);
        }

        Debug.log(context + "======== Parsing XML using SAX Parser ==================", MODULE);

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            ItemHandler handler = new ItemHandler(dctx, context);
            saxParser.parse(filePath, handler);
        } catch (Exception e) {
            e.printStackTrace();
            return ServiceUtil.returnError(e.getMessage());
        }

        Debug.log("======== Successfully Parsed XML ==================", MODULE);

        return ServiceUtil.returnSuccess("Parsed XML : " + filePath);

    }

}