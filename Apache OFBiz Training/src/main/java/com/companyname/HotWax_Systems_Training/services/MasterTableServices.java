package com.companyname.HotWax_Systems_Training.services;

import java.util.Map;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

public class MasterTableServices {
    public static final String module = MasterTableServices.class.getName();

    public static Map<String, Object> createMasterTable(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        try {
            GenericValue masterTable = delegator.makeValue("MasterTable");
            // Auto-generating the next sequence for the masterTableId primary key
            masterTable.setNextSeqId(); // Ensure this method exists and works as intended
            // Setting up all non-primary key field values from context map
            masterTable.setNonPKFields(context); // Ensure this method exists and works as intended
            // Creating record in the database for MasterTable entity with the prepared value
            delegator.create(masterTable); // Added this to persist the record
            result.put("MasterTableId", masterTable.getString("MasterTableId"));
            Debug.logInfo("========== This is my first Java Service implementation in Apache OFBiz. MasterTable record created successfully with MasterTableId:" + masterTable.getString("MasterTableId"), module);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Error in creating record in MasterTable entity. " + module);
        }
        return result;
    }
}
