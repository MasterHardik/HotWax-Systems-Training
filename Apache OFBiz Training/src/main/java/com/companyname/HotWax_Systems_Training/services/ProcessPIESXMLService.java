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
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class ProcessPIESXMLService {

    public static Map<String, Object> processPIESXML(DispatchContext dctx, Map<String, Object> context) {
        Debug.log("========Debug log START 01==================");
        System.out.println("======== Code reached here START ==================");
        ByteBuffer fileBuffer = (ByteBuffer) context.get("fileName");
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