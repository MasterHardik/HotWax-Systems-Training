package com.companyname.HotWax_Systems_Training.services;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class ProcessPIESXMLService {

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


    public static void extractDataFromXML(Document document) {
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            NodeList itemList = rootElement.getElementsByTagName("Item");

            for (int i = 0; i < itemList.getLength(); i++) {
                Element itemElement = (Element) itemList.item(i);

                String baseItemId = getTextContent(itemElement, "BaseItemID");
                String brandLabel = getTextContent(itemElement, "BrandLabel");
                String itemEffectiveDate = getTextContent(itemElement, "ItemEffectiveDate");
                String availableDate = getTextContent(itemElement, "AvailableDate");
                String hazardousMaterialCode = getTextContent(itemElement, "HazardousMaterialCode");

                System.out.println("BaseItemID: " + baseItemId);
                System.out.println("BrandLabel: " + brandLabel);
                System.out.println("ItemEffectiveDate: " + itemEffectiveDate);
                System.out.println("AvailableDate: " + availableDate);
                System.out.println("HazardousMaterialCode: " + hazardousMaterialCode);
            }
        }
    }
    public static Map<String, Object> processPIESXML(DispatchContext dctx, Map<String, Object> context) {
        Debug.log("========Debug log START 01==================");
        System.out.println("======== Code reached here START ==================");
        ByteBuffer fileBuffer = (ByteBuffer) context.get("fileName");

        /* ===== Logic Start ========== */
        InputStream inputStream = byteBufferToInputStream(fileBuffer);
        Document document = parseXML(inputStream);
        extractDataFromXML(document);
        /* ======= Logic End ============ */

        String successMessage;



        try {
            // Convert the ByteBuffer to a String for processing
            String fileContent = byteBufferToString(fileBuffer);

            // Print the file content (or process it as needed)
            System.out.println("XML Content: ");
            System.out.println(fileContent);

            // Set the success message
            successMessage = "File processed successfully";

        } catch (Exception e) {
            e.printStackTrace();
            // Set the error message
            return ServiceUtil.returnError("Error processing file: " + e.getMessage());
        }

        // Return success message
        return ServiceUtil.returnSuccess(successMessage);
    }

    // Utility method to convert ByteBuffer content to a String
    private static String byteBufferToString(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return new String(bytes);
    }
}