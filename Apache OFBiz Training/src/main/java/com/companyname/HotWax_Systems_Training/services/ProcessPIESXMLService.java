package com.companyname.HotWax_Systems_Training.services;

import org.apache.calcite.util.Static;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilXml;
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

    public static void storeProductDescriptions(Delegator delegator, String productId, Element descriptionElements) throws GenericEntityException {
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp(); // Current timestamp
        NodeList descriptionList = descriptionElements.getElementsByTagName("Description"); // Change here

        for (int i = 0; i < descriptionList.getLength(); i++) {
            // Extract data from each Description element
            Element descriptionElement = (Element) descriptionList.item(i);
            String languageCode = descriptionElement.getAttribute("LanguageCode");
            String descriptionCode = descriptionElement.getAttribute("DescriptionCode");
            String sequenceAttr = descriptionElement.getAttribute("Sequence");
            String descriptionText = descriptionElement.getTextContent();

            // Generate a new ContentId
            String contentId = delegator.getNextSeqId("Content");
            System.out.println(languageCode + descriptionCode + sequenceAttr + descriptionText + contentId);

            // Create a new record in the Content table
            GenericValue content = delegator.makeValue("Content");
            content.set("contentId", contentId);
            content.set("contentName", descriptionCode);  // Mapping description code (e.g., ABR, DES)
            content.set("description", descriptionText);  // Storing the actual description text
            content.set("localeString", languageCode);    // Storing language code (e.g., EN)
            content.set("createdStamp", nowTimestamp);    // Current timestamp
            content.set("lastUpdatedStamp", nowTimestamp); // Set last updated timestamp

            // Save Content record
            delegator.create(content);

            // Now associate this content with the product in the ProductContent table
            GenericValue productContent = delegator.makeValue("ProductContent");
            productContent.set("contentId", contentId);                      // Linking the generated contentId
            productContent.set("productContentTypeId", "DESCRIPTION");       // Set productContentTypeId to DESCRIPTION
            productContent.set("productId", productId);                      // Associating the content with the product
            productContent.set("sequenceNum", sequenceAttr);                 // Sequence number from XML
            productContent.set("fromDate", nowTimestamp);                    // Set fromDate as the current timestamp
            productContent.set("createdStamp", nowTimestamp);                // Current timestamp
            productContent.set("lastUpdatedStamp", nowTimestamp);            // Last updated timestamp

            // Save ProductContent record
            delegator.create(productContent);

            Debug.logInfo("Descriptions stored successfully with contentId: " + contentId, MODULE);
        }

        Debug.logInfo("Descriptions stored successfully for productId: " + productId, MODULE);
    }
    public static void createProductPriceType(Delegator delegator, String priceTypeId, String description) throws GenericEntityException {
        // Check if the ProductPriceType already exists
        GenericValue existingProductPriceType = EntityQuery.use(delegator)
                .from("ProductPriceType")
                .where("productPriceTypeId", priceTypeId)
                .queryOne();

        if (existingProductPriceType == null) {
            // Create a new ProductPriceType record
            GenericValue productPriceType = delegator.makeValue("ProductPriceType");
            productPriceType.set("productPriceTypeId", priceTypeId);
            productPriceType.set("description", description);
            productPriceType.set("createdStamp", UtilDateTime.nowTimestamp());
            productPriceType.set("lastUpdatedStamp", UtilDateTime.nowTimestamp());

            // Save the new ProductPriceType
            delegator.create(productPriceType);
            Debug.logInfo("ProductPriceType created successfully with productPriceTypeId: " + priceTypeId, MODULE);
        } else {
            Debug.logInfo("ProductPriceType already exists with productPriceTypeId: " + priceTypeId, MODULE);
        }
    }
    public static void storePrices(Delegator delegator, String productId, Element priceElements) throws GenericEntityException {
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp(); // Current timestamp
        NodeList pricingList = priceElements.getElementsByTagName("Pricing");

        for (int i = 0; i < pricingList.getLength(); i++) {
            // Extract data from each Pricing element
            Element pricingElement = (Element) pricingList.item(i);
            String priceType = pricingElement.getAttribute("PriceType");
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
                            "productPriceTypeId", priceType,
                            "currencyUomId", currencyCode,
                            "productPricePurposeId", "PURCHASE")
                    .queryCount() > 0;

            if (!priceExists) {
                // Generate a random PriceId
                String priceId = delegator.getNextSeqId("ProductPrice");

                // Create a new record in the ProductPrice table
                GenericValue price = delegator.makeValue("ProductPrice");
                price.set("productId", productId);
                createProductPriceType(delegator, priceType, ""); // Ensure the price type is valid
                price.set("productPriceTypeId", priceType);
                price.set("currencyUomId", currencyCode);
                price.set("fromDate", effectiveDateStr);
                price.set("thruDate", expirationDateStr);
                price.set("productPricePurposeId", "PURCHASE");
                price.set("productStoreGroupId", "_NA_"); // Default value for store group
                price.set("price", priceValue);
                price.set("createdStamp", nowTimestamp);
                price.set("lastUpdatedStamp", nowTimestamp);

                // Save the ProductPrice record
                delegator.create(price);
                Debug.logInfo("Price stored successfully with productId+(PriceTypeId): " + priceId + ":" + priceType, MODULE);
            } else {
                Debug.logInfo("Price already exists for productId: " + productId + " with priceTypeId: " + priceType + " and currencyUomId: " + currencyCode, MODULE);
            }
        }

        Debug.logInfo("Prices stored successfully for productId: " + productId, MODULE);
    }

    public static void storeExtendedProductInformation(Delegator delegator, String productId, Element extendedInfoElements) throws GenericEntityException {
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp(); // Current timestamp
        NodeList extendedInfoList = extendedInfoElements.getElementsByTagName("ExtendedProductInformation");

        // Check if the ProductFeatureType "EXPI" exists, if not, create it
        if (EntityQuery.use(delegator)
                .from("ProductFeatureType")
                .where("productFeatureTypeId", "EXPI")
                .queryCount() == 0) {
            GenericValue productFeatureType = delegator.makeValue("ProductFeatureType");
            productFeatureType.set("productFeatureTypeId", "EXPI");
            productFeatureType.set("description", "Extended Product Information");
            productFeatureType.set("createdStamp", nowTimestamp);
            productFeatureType.set("lastUpdatedStamp", nowTimestamp);
            delegator.create(productFeatureType);

            Debug.logInfo("ProductFeatureType 'EXPI' created with description: Extended Product Information", MODULE);
        }

        for (int i = 0; i < extendedInfoList.getLength(); i++) {
            // Extract data from each ExtendedProductInformation element
            Element extendedInfoElement = (Element) extendedInfoList.item(i);
            String expiCode = extendedInfoElement.getAttribute("EXPICode");
            String languageCode = extendedInfoElement.getAttribute("LanguageCode");
            String description = extendedInfoElement.getTextContent(); // Value inside the element

            // Generate a random ProductFeatureId
            String productFeatureId = delegator.getNextSeqId("ProductFeature");

            // Create a new record in the ProductFeature table
            GenericValue productFeature = delegator.makeValue("ProductFeature");
            productFeature.set("productFeatureId", productFeatureId);
            productFeature.set("productFeatureTypeId", "EXPI");
            productFeature.set("idCode", expiCode);
            productFeature.set("description", description);
            productFeature.set("createdStamp", nowTimestamp);
            productFeature.set("lastUpdatedStamp", nowTimestamp);

            // Save ProductFeature record
            delegator.create(productFeature);

            // Create a new record in the ProductFeatureAppl table
            associateFeatureWithProduct(delegator,productId,productFeatureId);

            Debug.logInfo("Product Feature stored successfully with productId: " + productId + ", productFeatureId: " + productFeatureId + ", expiCode: " + expiCode, MODULE);
        }
        Debug.logInfo("Extended Product Information stored successfully for productId: " + productId, MODULE);
    }

    private static void storeProductAttributes(Delegator delegator, String partNumber, Element itemElement) throws GenericEntityException {
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

                if (existingProductAttribute == null) {
                    // If it does not exist, create and store the new product attribute record
                    GenericValue productAttribute = delegator.makeValue("ProductAttribute", fields);
                    delegator.create(productAttribute);
                    Debug.log("Created new ProductAttribute: " + fields.toString());
                } else {
                    Debug.log("ProductAttribute already exists: " + attributeId + " for productId: " + partNumber);
                }
            }
        } else {
            Debug.logWarning("No ProductAttributes found for partNumber: " + partNumber, MODULE);
        }
    }


    private static void storeDigitalAssets(Delegator delegator, String partNumber, Element itemElement) throws GenericEntityException {
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
                String uom = (assetDimensionsElement != null) ? assetDimensionsElement.getAttribute("UOM") : null;

                // Asset Date
                Element assetDatesElement = UtilXml.firstChildElement(digitalFileInfoElement, "AssetDates");
                Element assetDateElement = UtilXml.firstChildElement(assetDatesElement, "AssetDate");
                String assetDate = (assetDateElement != null) ? assetDateElement.getTextContent() : null;

                // Create DataResource
                Map<String, Object> dataResourceFields = UtilMisc.toMap(
                        "dataResourceId", dataResourceId,
                        "mimeTypeId", fileType,
                        "objectInfo", filePath,
                        "dataResourceName", fileName
                );
                delegator.create("DataResource", dataResourceFields);

                // Create Content
                Map<String, Object> contentFields = UtilMisc.toMap(
                        "contentId", contentId,
                        "contentTypeId", "DFI", // Assuming "DFI" as the content type
                        "mimeTypeId", fileType,
                        "contentName", fileName,
                        "description", uri,
                        "serviceName", assetId,
                        "localeString", languageCode,
                        "dataResourceId", dataResourceId
                );
                delegator.create("Content", contentFields);

                // Create ProductContent
                Map<String, Object> productContentFields = UtilMisc.toMap(
                        "productId", partNumber,
                        "productContentTypeId", "DA", // Assuming "DA" as the product content type
                        "contentId", contentId,
                        "sequenceNum", (seqNum = seqNum + 1),
                        "fromDate", assetDate
                );
                delegator.create("ProductContent", productContentFields);

                // Create Content Attributes (Dimensions)
                if (assetHeight != null && assetWidth != null) {
                    Map<String, Object> contentAttrHeightFields = UtilMisc.toMap(
                            "contentId", contentId,
                            "attrName", "AssetHeight",
                            "attrValue", assetHeight,
                            "attrDescription", uom
                    );
                    delegator.create("ContentAttribute", contentAttrHeightFields);

                    Map<String, Object> contentAttrWidthFields = UtilMisc.toMap(
                            "contentId", contentId,
                            "attrName", "AssetWidth",
                            "attrValue", assetWidth,
                            "attrDescription", uom
                    );
                    delegator.create("ContentAttribute", contentAttrWidthFields);
                }

                // Create DataResourceMetadata for additional fields
                List<Map<String, String>> metadataEntries = new ArrayList<>();

                if (representation != null) {
                    metadataEntries.add(UtilMisc.toMap("metaDataPredicateId", "REP", "metaDataValue", representation));
                }

                if (fileSize != null) {
                    metadataEntries.add(UtilMisc.toMap("metaDataPredicateId", "FS", "metaDataValue", fileSize));
                }

                if (resolution != null) {
                    metadataEntries.add(UtilMisc.toMap("metaDataPredicateId", "RES", "metaDataValue", resolution));
                }

                if (colorMode != null) {
                    metadataEntries.add(UtilMisc.toMap("metaDataPredicateId", "COM", "metaDataValue", colorMode));
                }

                if (background != null) {
                    metadataEntries.add(UtilMisc.toMap("metaDataPredicateId", "BG", "metaDataValue", background));
                }

                if (orientationView != null) {
                    metadataEntries.add(UtilMisc.toMap("metaDataPredicateId", "OV", "metaDataValue", orientationView));
                }

                // Add new fields to metadata entries
                if (frame != null) {
                    metadataEntries.add(UtilMisc.toMap("metaDataPredicateId", "FR", "metaDataValue", frame));
                }

                if (totalFrames != null) {
                    metadataEntries.add(UtilMisc.toMap("metaDataPredicateId", "TF", "metaDataValue", totalFrames));
                }

                if (plane != null) {
                    metadataEntries.add(UtilMisc.toMap("metaDataPredicateId", "PL", "metaDataValue", plane));
                }

                if (plunge != null) {
                    metadataEntries.add(UtilMisc.toMap("metaDataPredicateId", "PU", "metaDataValue", plunge));
                }

                if (totalPlanes != null) {
                    metadataEntries.add(UtilMisc.toMap("metaDataPredicateId", "TP", "metaDataValue", totalPlanes));
                }

                // Create all DataResourceMetadata entries
                for (Map<String, String> entry : metadataEntries) {
                    // Checking for null values before creating the entry
                    String metaDataPredicateId = entry.get("metaDataPredicateId");
                    String metaDataValue = entry.get("metaDataValue");

                    if (metaDataPredicateId != null && metaDataValue != null) {
                        Map<String, Object> dataResourceMetaFields = UtilMisc.toMap(
                                "dataResourceId", dataResourceId, // Use of generated dataResourceId
                                "metaDataPredicateId", metaDataPredicateId,
                                "metaDataValue", metaDataValue
                        );
                        delegator.create("DataResourceMetaData", dataResourceMetaFields);
                    }
                }

            }
        }
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
                // Description
//                try {
//                    storeProductDescriptions(delegator,partNumber,itemElement);
//                } catch (GenericEntityException e) {
//                    throw new RuntimeException(e);
//                }

                // Price
                try {
                    storePrices(delegator,partNumber,itemElement);
                } catch (GenericEntityException e) {
                    throw new RuntimeException(e);
                }

                // EXPI
                try {
                    storeExtendedProductInformation(delegator,partNumber,itemElement);
                } catch (GenericEntityException e) {
                    throw new RuntimeException(e);
                }


                // Product Attributes
                try {
                    storeProductAttributes(delegator, partNumber, itemElement);
                } catch (GenericEntityException e) {
                    throw new RuntimeException(e);
                }

                // Digital Asset
                try {
                    storeDigitalAssets(delegator,partNumber,itemElement);
                }
                catch (GenericEntityException e){
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
// when trying to delete the content  I first need to delete it's 3 reference in contenKeyword table.
