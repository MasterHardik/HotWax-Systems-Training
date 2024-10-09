package com.companyname.HotWax_Systems_Training.services;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ServiceUtil;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ProcessCSVServices {

    private static final String MODULE = ProcessCSVServices.class.getName();
    private static final String DELIMITER = "\t"; // Assuming tab-separated CSV

    public static Map<String, Object> updateProductPrice(DispatchContext dctx, Map<String, Object> context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Debug.log("=============== Processing CSV ======================");

        ByteBuffer fileContentBuffer = (ByteBuffer) context.get("fileName");
        String fileContent = StandardCharsets.UTF_8.decode(fileContentBuffer).toString();
        Delegator delegator = dctx.getDelegator();

        try {
            // Split the file content into lines
            List<String> lines = Arrays.asList(fileContent.split("\n"));

            // Process the lines in parallel, skipping the header
            lines.stream()
                    .skip(1).parallel()
                    .forEach(line -> {
                        try {
                            processLine(line, dctx, delegator ,userLogin);
                        } catch (Exception e) {
                            Debug.logError(e, "Error processing line: " + line, MODULE);
                        }
                    });
        } catch (Exception e) {
            Debug.logError(e, "Error processing CSV", MODULE);
        }

        Debug.log("=============== CSV Processing Complete ======================");
        return UtilMisc.toMap("success", "CSV processed successfully");
    }

    private static void processLine(String line, DispatchContext dctx, Delegator delegator, GenericValue userLogin) throws Exception {
        String[] values = line.split(DELIMITER);
        if (values.length < 6) {
            Debug.logWarning("Invalid line: " + line, MODULE);
            return;
        }

        injectPriceData(values[0], values[1], values[2], values[3], values[4], values[5], dctx, delegator , userLogin);
    }

    private static void injectPriceData(String mfr, String mfrSubLineCode, String partNumber, String regionId, String price, String corePrice, DispatchContext dctx, Delegator delegator, GenericValue userLogin) throws Exception {
        String facilityGroupId = getOrCreateFacilityGroup(dctx, regionId, delegator, userLogin);
        String partyId = getOrCreateParty("APH", delegator);
        String productId = createProductCall(dctx, mfr, partNumber, mfrSubLineCode, partyId,userLogin);
        createProductPrice(dctx, productId, price, facilityGroupId, userLogin);
    }

    private static String getOrCreateFacilityGroup(DispatchContext dctx, String regionId, Delegator delegator, GenericValue userLogin) throws GenericEntityException, GenericServiceException {
        GenericValue facilityGroup = EntityQuery.use(delegator)
                .from("FacilityGroup")
                .where("facilityGroupTypeId", "PRICE_REGION", "facilityGroupName", regionId)
                .queryFirst();

        if (facilityGroup == null) {
            Map<String,Object> context = UtilMisc.toMap(
                    "facilityGroupId", delegator.getNextSeqId("FacilityGroup"),
                    "facilityGroupTypeId", "PRICE_REGION",
                    "facilityGroupName", regionId,
                    "description", "Created for region " + regionId,
                    "userLogin",userLogin
            );
            Map<String , Object> result = dctx.getDispatcher().runSync("facilityGroup",context);
            Debug.logInfo("Created new FacilityGroup for regionId: " + regionId,MODULE);

            return (String)result.get("facilityGroupId");
        }
        return facilityGroup.getString("facilityGroupId");
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

    private static String getOrCreateParty(String externalId, Delegator delegator) throws GenericEntityException {
        GenericValue party = EntityQuery.use(delegator).from("Party").where("externalId", externalId).queryFirst();
        if (party == null) {
            String partyId = createParty(delegator, externalId, "PARTY_GROUP");
            Debug.log("Created new party with partyID: " + partyId);
            return partyId;
        }
        return party.getString("partyId");
    }
    private static String createProductCall(DispatchContext dctx, String mfr, String partNumber, String mfrSubLineCode, String partyId, GenericValue userLogin) throws Exception {
        String configId = partyId + "_" + mfr + "_" + mfrSubLineCode;
        Map<String, Object> context = UtilMisc.toMap(
                "productTypeId", "FINISHED_GOOD",
                "internalName", partNumber,
                "configId", configId,
                "userLogin",userLogin
        );

        Map<String, Object> result = dctx.getDispatcher().runSync("createProduct", context );
        if (ServiceUtil.isError(result)) {
            throw new RuntimeException("Error creating product: " + ServiceUtil.getErrorMessage(result));
        }
        return (String) result.get("productId");
    }

    private static void createProductPrice(DispatchContext dctx, String productId, String price, String facilityGroupId, GenericValue userLogin) throws Exception {
        Map<String, Object> context = UtilMisc.toMap(
                "productId", productId,
                "price", price,
                "currencyUomId", "USD",
                "facilityGroupId", facilityGroupId,
                "productPricePurposeId", "PURCHASE",
                "productPriceTypeId", "B2C_OFFER_PRICE",
                "productStoreGroupId","_NA_",
                "userLogin",userLogin
        );

        Map<String, Object> result = dctx.getDispatcher().runSync("createProductPrice", context);
        if (ServiceUtil.isError(result)) {
            throw new RuntimeException("Error creating product price: " + ServiceUtil.getErrorMessage(result));
        }
    }
}
