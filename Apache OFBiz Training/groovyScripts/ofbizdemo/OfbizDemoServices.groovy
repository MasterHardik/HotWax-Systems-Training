import org.apache.ofbiz.entity.GenericEntityException;

def createMasterTable() {
    result = [:];
    try {
        masterTable = delegator.makeValue("MasterTable");
        // Auto generating next sequence of ofbizDemoId primary key
        masterTable.setNextSeqId();
        // Setting up all non primary key field values from context map
        masterTable.setNonPKFields(context);
        // Creating record in database for OfbizDemo entity for prepared value
        masterTable = delegator.create(masterTable);
        result.MasterTableId = masterTable.MasterTableId;
        logInfo("==========This is my first Groovy Service implementation in Apache OFBiz. OfbizDemo record "
                +"created successfully with ofbizDemoId: "+masterTable.getString("MasterTableId"));
    } catch (GenericEntityException e) {
        logError(e.getMessage());
        return error("Error in creating record in OfbizDemo entity ........");
    }
    return result;
}