package com.companyname.HotWax_Systems_Training.services;

import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProcessXMLClass {

    public static Map<String, Object> processXMLService(DispatchContext ctx, Map<String, ? extends Object> context) {
        String fulltext = (String) context.get("fulltext");
        List<String> messages = new ArrayList<>();

        if (fulltext != null) {
            System.out.println("Received XML content: " + fulltext);

            messages.add("XML processed successfully");
        } else {
            messages.add("No XML content provided");
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("messages", messages);
        return result;
    }
}


//
////
//
//
//package com.companyname.HotWax_Systems_Training.services;
//
//import org.apache.ofbiz.base.util.UtilMisc;
//import org.apache.ofbiz.service.DispatchContext;
//import org.apache.ofbiz.service.ServiceUtil;
//import org.apache.ofbiz.entity.Delegator;
//import org.apache.ofbiz.entity.GenericValue;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.w3c.dom.Document;
//
//import javax.xml.parsers.DocumentBuilder;
//import javax.xml.parsers.DocumentBuilderFactory;
//import java.io.StringReader;
//import java.util.Map;
//
//public class ProcessXMLClass {
//
//    private static final Logger logger = LoggerFactory.getLogger(ProcessXMLClass.class);
//
//    public static Map<String, Object> processXMLService(DispatchContext dctx, Map<String, Object> context) {
//        String xmlContent = (String) context.get("fulltext");
//        if (xmlContent == null) {
//            return ServiceUtil.returnError("No XML content provided.");
//        }
//
//        try {
//            Document doc = XMLParser.parseXML(xmlContent);
//            XMLParser.processXML(doc, dctx);
//
//            return ServiceUtil.returnSuccess();
//        } catch (Exception e) {
//            logger.error("Error processing XML", e);
//            return ServiceUtil.returnError("Error processing XML: " + e.getMessage());
//        }
//    }
//}
//
//
