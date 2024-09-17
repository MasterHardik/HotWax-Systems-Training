package com.companyname.HotWax_Systems_Training.services;

import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.DispatchContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.Map;

public class XMLParser {

    public static Document parseXML(String xmlContent) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xmlContent));
        return builder.parse(is);
    }

    public static void processXML(Document doc, DispatchContext dctx) throws Exception {
        Delegator delegator = dctx.getDelegator();
        Element root = doc.getDocumentElement();
        NodeList items = root.getElementsByTagName("item");

        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            String id = item.getElementsByTagName("id").item(0).getTextContent();
            String name = item.getElementsByTagName("name").item(0).getTextContent();
            saveData(delegator, id, name);
        }
    }

    private static void saveData(Delegator delegator, String id, String name) throws Exception {
        GenericValue entity = delegator.makeValue("YourEntityName");
        entity.set("id", id);
        entity.set("name", name);
        delegator.createOrStore(entity);
    }
}
