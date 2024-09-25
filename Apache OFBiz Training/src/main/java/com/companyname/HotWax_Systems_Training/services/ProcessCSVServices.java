package com.companyname.HotWax_Systems_Training.services;

import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class ProcessCSVServices {

    private static final String MODULE = ProcessCSVServices.class.getName();
    private static final String DELIMITER = "\t"; // Assuming tab-separated CSV

    public static Map<String, Object> updateProductPrice(DispatchContext dctx, Map<String, Object> context) {
        Debug.log("=============== Processing CSV ======================");

        ByteBuffer fileContentBuffer = (ByteBuffer) context.get("fileName");
        String fileContent = StandardCharsets.UTF_8.decode(fileContentBuffer).toString();
        Delegator delegator = dctx.getDelegator();

        try (BufferedReader br = new BufferedReader(new StringReader(fileContent))) {
            String line;
            boolean skipHeader = true;

            while ((line = br.readLine()) != null) {
                if (skipHeader) {
                    skipHeader = false;
                    continue;
                }
                processLine(line, delegator);
            }
        } catch (Exception e) {
            Debug.logError(e, "Error processing CSV", MODULE);
        }

        Debug.log("=============== CSV Processing Complete ======================");
        return UtilMisc.toMap("success", "CSV processed successfully");
    }

    private static void processLine(String line, Delegator delegator) throws GenericEntityException {
        String[] values = line.split(DELIMITER);
        if (values.length < 6) {
            Debug.logWarning("Invalid line: " + line, MODULE);
            return;
        }

        injectPriceData(values[0], values[1], values[2], values[3], values[4], values[5], delegator);
    }

    private static void injectPriceData(String mfr, String mfrSubLineCode, String partNumber, String regionId, String price,String corePrice, Delegator delegator) throws GenericEntityException {
        String facilityGroupId = getOrCreateFacilityGroup(regionId, delegator);
        String partyId = getOrCreateParty("APH", delegator);
        String productId = getOrCreateProduct(mfr,partNumber,mfrSubLineCode, partyId, delegator);
        upsertPriceRecord(productId, price, delegator);
    }

    private static String getOrCreateFacilityGroup(String regionId, Delegator delegator) throws GenericEntityException {
        GenericValue facilityGroup = EntityQuery.use(delegator)
                .from("FacilityGroup")
                .where("facilityGroupTypeId", "PRICE_REGION", "facilityGroupName", regionId)
                .queryOne();

        if (facilityGroup == null) {
            facilityGroup = delegator.makeValue("FacilityGroup", UtilMisc.toMap(
                    "facilityGroupId", delegator.getNextSeqId("FacilityGroup"),
                    "facilityGroupTypeId", "PRICE_REGION",
                    "facilityGroupName", regionId,
                    "description", "Created for region " + regionId
            ));
            delegator.create(facilityGroup);
            Debug.log("Created new FacilityGroup for regionId: " + regionId);
        }
        return facilityGroup.getString("facilityGroupId");
    }

    private static String getOrCreateParty(String externalId, Delegator delegator) throws GenericEntityException {
        GenericValue party = EntityQuery.use(delegator).from("Party").where("externalId", externalId).queryFirst();
        if (party == null) {
            String partyId = createParty(delegator, externalId, "PARTY_GROUP");
            Debug.log("Created new party with partyID: " + partyId);
            return partyId;
        }
        return party.getString("partyId");
    }

    private static String getOrCreateProduct(String mfr , String partNumber, String mfrSubLineCode, String partyId, Delegator delegator) throws GenericEntityException {
        String mfrLineCode = partyId + "_" +  mfr + "_" +mfrSubLineCode;

        GenericValue product = EntityQuery.use(delegator)
                .from("Product")
                .where("productName", partNumber, "configId", mfrLineCode)
                .queryOne();

        if (product == null) {
            String productId = delegator.getNextSeqId("Product");
            product = delegator.makeValue("Product", UtilMisc.toMap(
                    "productId", productId,
                    "configId", mfrLineCode,
                    "productName", partNumber
            ));
            delegator.create(product);
            Debug.log("Created new Product for partNumber: " + partNumber + " and productID: " + productId);
            return productId;
        }
        return product.getString("productId");
    }

    private static void upsertPriceRecord(String productId, String price, Delegator delegator) throws GenericEntityException {
        GenericValue priceRecord = EntityQuery.use(delegator)
                .from("ProductPrice")
                .where("productId", productId)
                .queryFirst();

        Map<String, Object> fields = new HashMap<>();
        fields.put("currencyUomId", "USD");
        fields.put("productId", productId);
        fields.put("price", price);
        fields.put("productPricePurposeId", "PURCHASE");
        fields.put("productPriceTypeId", "B2C_OFFER_PRICE");
        fields.put("productStoreGroupId", "_NA_");

        if (priceRecord == null) {
            fields.put("fromDate", UtilDateTime.nowTimestamp());
            delegator.create(delegator.makeValue("ProductPrice", fields));
            Debug.log("Created new product price record for productId: " + productId);
        } else {
            priceRecord.set("price", price);
            delegator.store(priceRecord);
            Debug.log("Updated existing product price record for productId: " + productId);
        }
    }

    private static String createParty(Delegator delegator, String externalId, String partyTypeId) {
        String partyId = String.valueOf((int) Math.floor(Math.random() * 10000));
        GenericValue party = delegator.makeValue("Party", UtilMisc.toMap(
                "partyId", partyId,
                "partyTypeId", partyTypeId,
                "externalId", externalId,
                "description", "new party by createParty() method"
        ));

        try {
            delegator.create(party);
        } catch (GenericEntityException e) {
            throw new RuntimeException(e);
        }
        return partyId;
    }
}