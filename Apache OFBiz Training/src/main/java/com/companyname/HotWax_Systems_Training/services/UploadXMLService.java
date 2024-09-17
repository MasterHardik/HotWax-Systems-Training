package com.companyname.HotWax_Systems_Training.services;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.ofbiz.base.util.Debug;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UploadXMLService {
        private static final String FILE_PATH = "/home/hardik/Desktop/OFBiz_Training/ofbiz-framework/Items.xml"; // Hardcoded file path

        public static Map<String, Object> uploadXML(Map<String, ?> context) {
            Map<String, Object> result = new HashMap<>();
            Debug.log("===========dfg =====================");
            // Use the hardcoded file path
            File file = new File(FILE_PATH);

            if (!file.exists() || !file.isFile()) {
                result.put("status", "error");
                result.put("message", "File does not exist or is not a valid file.");
                return result;
            }

            // Process the XML file
            try (InputStream inputStream = new FileInputStream(file)) {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(inputStream);
                doc.getDocumentElement().normalize();

                // Processing logic can be added here

                result.put("status", "success");
                result.put("message", "File processed successfully.");
            } catch (Exception e) {
                // Log the exception and return error
                result.put("status", "error");
                result.put("message", "Error processing file: " + e.getMessage());
            }

            return result;
        }
    }

//    public static Map<String, Object> uploadXML(Map<String, ?> context) {
//        FileItem fileItem = (FileItem) context.get("file");
//        if (fileItem == null) {
//            return Map.of("result", "No file uploaded.");
//        }
//
//        StringBuilder result = new StringBuilder();
//
//        try (InputStream inputStream = fileItem.getInputStream()) {
//            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//            DocumentBuilder builder = factory.newDocumentBuilder();
//            Document document = builder.parse(inputStream);
//            document.getDocumentElement().normalize();
//
//            // Process the XML
//            NodeList itemList = document.getElementsByTagName("Item");
//            for (int i = 0; i < itemList.getLength(); i++) {
//                Node itemNode = itemList.item(i);
//                if (itemNode.getNodeType() == Node.ELEMENT_NODE) {
//                    Element itemElement = (Element) itemNode;
//                    result.append("Item MaintenanceType: ").append(itemElement.getAttribute("MaintenanceType")).append("\n");
//
//                    // Process each child element
//                    appendChildElement(result, itemElement, "HazardousMaterialCode");
//                    appendChildElement(result, itemElement, "BaseItemID");
//                    appendChildElement(result, itemElement, "ItemLevelGTIN", "GTINQualifier");
//                    appendChildElement(result, itemElement, "PartNumber");
//                    appendChildElement(result, itemElement, "BrandAAIAID");
//                    appendChildElement(result, itemElement, "BrandLabel");
//                    appendChildElement(result, itemElement, "VMRSBrandID");
//                    appendChildElement(result, itemElement, "QuantityPerApplication", "UOM");
//                    appendChildElement(result, itemElement, "ItemEffectiveDate");
//                    appendChildElement(result, itemElement, "AvailableDate");
//                    appendChildElement(result, itemElement, "MinimumOrderQuantity", "UOM");
//
//                    // Process ManufacturerProductCodes
//                    NodeList manufacturerProductCodesList = itemElement.getElementsByTagName("ManufacturerProductCodes");
//                    if (manufacturerProductCodesList.getLength() > 0) {
//                        Node manufacturerProductCodesNode = manufacturerProductCodesList.item(0);
//                        if (manufacturerProductCodesNode.getNodeType() == Node.ELEMENT_NODE) {
//                            Element manufacturerProductCodesElement = (Element) manufacturerProductCodesNode;
//                            result.append("  Group: ").append(getTextContent(manufacturerProductCodesElement, "Group")).append("\n");
//                            result.append("  SubGroup: ").append(getTextContent(manufacturerProductCodesElement, "SubGroup")).append("\n");
//                        }
//                    }
//
//                    // Process Descriptions
//                    NodeList descriptionsList = itemElement.getElementsByTagName("Description");
//                    for (int j = 0; j < descriptionsList.getLength(); j++) {
//                        Node descriptionNode = descriptionsList.item(j);
//                        if (descriptionNode.getNodeType() == Node.ELEMENT_NODE) {
//                            Element descriptionElement = (Element) descriptionNode;
//                            result.append("  Description (")
//                                    .append(descriptionElement.getAttribute("LanguageCode")).append(", ")
//                                    .append(descriptionElement.getAttribute("MaintenanceType")).append(", ")
//                                    .append(descriptionElement.getAttribute("DescriptionCode")).append("): ")
//                                    .append(descriptionElement.getTextContent()).append("\n");
//                        }
//                    }
//
//                    // Process Prices
//                    NodeList pricingList = itemElement.getElementsByTagName("Pricing");
//                    for (int j = 0; j < pricingList.getLength(); j++) {
//                        Node pricingNode = pricingList.item(j);
//                        if (pricingNode.getNodeType() == Node.ELEMENT_NODE) {
//                            Element pricingElement = (Element) pricingNode;
//                            result.append("  PriceSheetNumber: ").append(getTextContent(pricingElement, "PriceSheetNumber")).append("\n");
//                            result.append("  CurrencyCode: ").append(getTextContent(pricingElement, "CurrencyCode")).append("\n");
//                            result.append("  EffectiveDate: ").append(getTextContent(pricingElement, "EffectiveDate")).append("\n");
//                            result.append("  ExpirationDate: ").append(getTextContent(pricingElement, "ExpirationDate")).append("\n");
//                            result.append("  Price: ").append(getTextContent(pricingElement, "Price", "UOM")).append("\n");
//                            result.append("  PriceBreak: ").append(getTextContent(pricingElement, "PriceBreak", "UOM")).append("\n");
//                        }
//                    }
//
//                    // Process Extended Information
//                    NodeList extendedInformationList = itemElement.getElementsByTagName("ExtendedProductInformation");
//                    for (int j = 0; j < extendedInformationList.getLength(); j++) {
//                        Node extendedInfoNode = extendedInformationList.item(j);
//                        if (extendedInfoNode.getNodeType() == Node.ELEMENT_NODE) {
//                            Element extendedInfoElement = (Element) extendedInfoNode;
//                            result.append("  ExtendedProductInformation (")
//                                    .append(extendedInfoElement.getAttribute("EXPICode")).append("): ")
//                                    .append(extendedInfoElement.getTextContent()).append("\n");
//                        }
//                    }
//
//                    // Process Product Attributes
//                    NodeList productAttributesList = itemElement.getElementsByTagName("ProductAttribute");
//                    for (int j = 0; j < productAttributesList.getLength(); j++) {
//                        Node productAttributeNode = productAttributesList.item(j);
//                        if (productAttributeNode.getNodeType() == Node.ELEMENT_NODE) {
//                            Element productAttributeElement = (Element) productAttributeNode;
//                            result.append("  ProductAttribute (")
//                                    .append(productAttributeElement.getAttribute("AttributeID")).append(", ")
//                                    .append(productAttributeElement.getAttribute("PADBAttribute")).append(", ")
//                                    .append(productAttributeElement.getAttribute("AttributeUOM")).append("): ")
//                                    .append(productAttributeElement.getTextContent()).append("\n");
//                        }
//                    }
//
//                    // Process Packages
//                    NodeList packagesList = itemElement.getElementsByTagName("Package");
//                    for (int j = 0; j < packagesList.getLength(); j++) {
//                        Node packageNode = packagesList.item(j);
//                        if (packageNode.getNodeType() == Node.ELEMENT_NODE) {
//                            Element packageElement = (Element) packageNode;
//                            result.append("  PackageLevelGTIN: ").append(getTextContent(packageElement, "PackageLevelGTIN")).append("\n");
//                            result.append("  PackageBarCodeCharacters: ").append(getTextContent(packageElement, "PackageBarCodeCharacters")).append("\n");
//                            result.append("  PackageUOM: ").append(getTextContent(packageElement, "PackageUOM")).append("\n");
//                            result.append("  QuantityofEaches: ").append(getTextContent(packageElement, "QuantityofEaches")).append("\n");
//
//                            // Process Dimensions
//                            NodeList dimensionsList = packageElement.getElementsByTagName("Dimensions");
//                            if (dimensionsList.getLength() > 0) {
//                                Node dimensionsNode = dimensionsList.item(0);
//                                if (dimensionsNode.getNodeType() == Node.ELEMENT_NODE) {
//                                    Element dimensionsElement = (Element) dimensionsNode;
//                                    result.append("    MerchandisingHeight: ").append(getTextContent(dimensionsElement, "MerchandisingHeight")).append("\n");
//                                    result.append("    MerchandisingWidth: ").append(getTextContent(dimensionsElement, "MerchandisingWidth")).append("\n");
//                                    result.append("    MerchandisingLength: ").append(getTextContent(dimensionsElement, "MerchandisingLength")).append("\n");
//                                    result.append("    ShippingHeight: ").append(getTextContent(dimensionsElement, "ShippingHeight")).append("\n");
//                                    result.append("    ShippingWidth: ").append(getTextContent(dimensionsElement, "ShippingWidth")).append("\n");
//                                    result.append("    ShippingLength: ").append(getTextContent(dimensionsElement, "ShippingLength")).append("\n");
//                                }
//                            }
//
//                            // Process Weights
//                            NodeList weightsList = packageElement.getElementsByTagName("Weights");
//                            if (weightsList.getLength() > 0) {
//                                Node weightsNode = weightsList.item(0);
//                                if (weightsNode.getNodeType() == Node.ELEMENT_NODE) {
//                                    Element weightsElement = (Element) weightsNode;
//                                    result.append("    Weight: ").append(getTextContent(weightsElement, "Weight", "UOM")).append("\n");
//                                    result.append("    DimensionalWeight: ").append(getTextContent(weightsElement, "DimensionalWeight")).append("\n");
//                                }
//                            }
//
//                            result.append("  WeightVariance: ").append(getTextContent(packageElement, "WeightVariance")).append("\n");
//                        }
//                    }
//
//                    // Process Part Interchange Info
//                    NodeList partInterchangeInfoList = itemElement.getElementsByTagName("PartInterchange");
//                    for (int j = 0; j < partInterchangeInfoList.getLength(); j++) {
//                        Node partInterchangeNode = partInterchangeInfoList.item(j);
//                        if (partInterchangeNode.getNodeType() == Node.ELEMENT_NODE) {
//                            Element partInterchangeElement = (Element) partInterchangeNode;
//                            result.append("  PartInterchange (")
//                                    .append(partInterchangeElement.getAttribute("BrandAAIAID")).append(", ")
//                                    .append(partInterchangeElement.getAttribute("BrandLabel")).append("): ")
//                                    .append(getTextContent(partInterchangeElement, "PartNumber")).append("\n");
//                        }
//                    }
//
//                    // Process Digital Assets
//                    NodeList digitalAssetsList = itemElement.getElementsByTagName("DigitalFileInformation");
//                    for (int j = 0; j < digitalAssetsList.getLength(); j++) {
//                        Node digitalAssetNode = digitalAssetsList.item(j);
//                        if (digitalAssetNode.getNodeType() == Node.ELEMENT_NODE) {
//                            Element digitalAssetElement = (Element) digitalAssetNode;
//                            result.append("  DigitalFileInformation (")
//                                    .append(digitalAssetElement.getAttribute("AssetID")).append(", ")
//                                    .append(digitalAssetElement.getAttribute("LanguageCode")).append("): ")
//                                    .append("\n    FileName: ").append(getTextContent(digitalAssetElement, "FileName")).append("\n")
//                                    .append("    AssetType: ").append(getTextContent(digitalAssetElement, "AssetType")).append("\n")
//                                    .append("    FileType: ").append(getTextContent(digitalAssetElement, "FileType")).append("\n")
//                                    .append("    Representation: ").append(getTextContent(digitalAssetElement, "Representation")).append("\n")
//                                    .append("    FileSize: ").append(getTextContent(digitalAssetElement, "FileSize")).append("\n")
//                                    .append("    Resolution: ").append(getTextContent(digitalAssetElement, "Resolution")).append("\n")
//                                    .append("    ColorMode: ").append(getTextContent(digitalAssetElement, "ColorMode")).append("\n")
//                                    .append("    Background: ").append(getTextContent(digitalAssetElement, "Background")).append("\n")
//                                    .append("    OrientationView: ").append(getTextContent(digitalAssetElement, "OrientationView")).append("\n")
//                                    .append("    FilePath: ").append(getTextContent(digitalAssetElement, "FilePath")).append("\n")
//                                    .append("    URI: ").append(getTextContent(digitalAssetElement, "URI")).append("\n");
//
//                            // Process Asset Dates
//                            NodeList assetDatesList = digitalAssetElement.getElementsByTagName("AssetDates");
//                            if (assetDatesList.getLength() > 0) {
//                                Node assetDatesNode = assetDatesList.item(0);
//                                if (assetDatesNode.getNodeType() == Node.ELEMENT_NODE) {
//                                    Element assetDatesElement = (Element) assetDatesNode;
//                                    NodeList assetDateList = assetDatesElement.getElementsByTagName("AssetDate");
//                                    for (int k = 0; k < assetDateList.getLength(); k++) {
//                                        Node assetDateNode = assetDateList.item(k);
//                                        if (assetDateNode.getNodeType() == Node.ELEMENT_NODE) {
//                                            Element assetDateElement = (Element) assetDateNode;
//                                            result.append("    AssetDate (")
//                                                    .append(assetDateElement.getAttribute("assetDateType")).append("): ")
//                                                    .append(assetDateElement.getTextContent()).append("\n");
//                                        }
//                                    }
//                                }
//                            }
//
//                            result.append("    Country: ").append(getTextContent(digitalAssetElement, "Country")).append("\n");
//                        }
//                    }
//                }
//            }
//        } catch (ParserConfigurationException | SAXException | IOException e) {
//            result.append("Error: ").append(e.getMessage());
//        }
//
//        return Map.of("result", result.toString());
//    }
//
//    private static void appendChildElement(StringBuilder result, Element parent, String tagName) {
//        appendChildElement(result, parent, tagName, null);
//    }
//
//    private static void appendChildElement(StringBuilder result, Element parent, String tagName, String attrName) {
//        NodeList nodeList = parent.getElementsByTagName(tagName);
//        if (nodeList.getLength() > 0) {
//            Node node = nodeList.item(0);
//            if (node.getNodeType() == Node.ELEMENT_NODE) {
//                Element element = (Element) node;
//                if (attrName != null) {
//                    result.append("  ").append(tagName).append(" (").append(element.getAttribute(attrName)).append("): ");
//                } else {
//                    result.append("  ").append(tagName).append(": ");
//                }
//                result.append(element.getTextContent()).append("\n");
//            }
//        }
//    }
//
//    private static String getTextContent(Element element, String tagName) {
//        return getTextContent(element, tagName, null);
//    }
//
//    private static String getTextContent(Element element, String tagName, String attrName) {
//        NodeList nodeList = element.getElementsByTagName(tagName);
//        if (nodeList.getLength() > 0) {
//            Node node = nodeList.item(0);
//            if (node.getNodeType() == Node.ELEMENT_NODE) {
//                Element el = (Element) node;
//                if (attrName != null) {
//                    return el.getAttribute(attrName);
//                } else {
//                    return el.getTextContent();
//                }
//            }
//        }
//        return "";
//    }
// }
