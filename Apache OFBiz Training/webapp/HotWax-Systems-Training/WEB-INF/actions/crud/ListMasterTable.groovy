//ofbizDemoTypes = delegator.findList("OfbizDemoType", null, null, null, null, false);
//context.ofbizDemoTypes = ofbizDemoTypes;
//ofbizDemoList = delegator.findList("OfbizDemo", null, null, null, null, false);
//context.ofbizDemoList = ofbizDemoList;

// The new way of doing the above in Groovy is by using DSL(Domain Specific Language) capabilities.
// 'Groovy DSL for OFBiz logic' document is already shared above in this tutorial
// Read about EntityQuery API in OFBiz

context.masterTableTypes = from("masterTableType").queryList()
context.masterTableList = from("masterTable").queryList()