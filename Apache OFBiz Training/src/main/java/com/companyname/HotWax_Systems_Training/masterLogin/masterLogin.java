package com.companyname.HotWax_Systems_Training.masterLogin;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;

import java.util.Map;
import java.util.HashMap;

public class masterLogin {

    public static String simpleLogin(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");

        // Retrieve login parameters
        String username = request.getParameter("USERNAME");
        String password = request.getParameter("PASSWORD");

        // Validate inputs
        if (UtilValidate.isEmpty(username)) {
            request.setAttribute("_ERROR_MESSAGE_", "Username is required.");
            return "error";
        }
        if (UtilValidate.isEmpty(password)) {
            request.setAttribute("_ERROR_MESSAGE_", "Password is required.");
            return "error";
        }

        // Prepare service context for the login check
        Map<String, Object> serviceContext = new HashMap<>();
        serviceContext.put("login.username", username);
        serviceContext.put("login.password", password);

        Map<String, Object> result;
        try {
            // Call the userLogin service
            result = dispatcher.runSync("userLogin", serviceContext);
        } catch (GenericServiceException e) {
            request.setAttribute("_ERROR_MESSAGE_", "Error occurred while checking credentials.");
            return "error";
        }

        // Check the result of the login attempt
        if (ModelService.RESPOND_SUCCESS.equals(result.get(ModelService.RESPONSE_MESSAGE))) {
            return "success";
        } else {
            String errorMessage = (String) result.get(ModelService.ERROR_MESSAGE);
            request.setAttribute("_ERROR_MESSAGE_", "Login failed: " + errorMessage);
            return "error";
        }
    }
}




//package com.companyname.HotWax_Systems_Training.masterLogin;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import javax.servlet.http.HttpSession;
//import org.apache.ofbiz.base.util.UtilValidate;
//import org.apache.ofbiz.base.util.UtilHttp;
//import org.apache.ofbiz.service.LocalDispatcher;
//import org.apache.ofbiz.service.ModelService;
//import org.apache.ofbiz.service.GenericServiceException;
//import org.apache.ofbiz.entity.Delegator;
//import org.apache.ofbiz.entity.GenericValue;
//import org.apache.ofbiz.entity.DelegatorFactory;
//import org.apache.ofbiz.entity.util.EntityUtil;
//
//import java.util.Map;
//import java.util.HashMap;
//
//public class masterLogin {
//
//    public static String simpleLogin(HttpServletRequest request, HttpServletResponse response) {
//        HttpSession session = request.getSession();
//        Delegator delegator = (Delegator) request.getAttribute("delegator");
//        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
//
//        // Retrieve login parameters
//        String username = request.getParameter("USERNAME");
//        String password = request.getParameter("PASSWORD");
//
//        // Validate inputs
//        if (UtilValidate.isEmpty(username)) {
//            request.setAttribute("_ERROR_MESSAGE_", "Username is required.");
//            return "error";
//        }
//        if (UtilValidate.isEmpty(password)) {
//            request.setAttribute("_ERROR_MESSAGE_", "Password is required.");
//            return "error";
//        }
//
//        // Prepare service context for the login check
//        Map<String, Object> serviceContext = new HashMap<>();
//        serviceContext.put("login.username", username);
//        serviceContext.put("login.password", password);
//
//        Map<String, Object> result;
//        try {
//            // Call the userLogin service
//            result = dispatcher.runSync("userLogin", serviceContext);
//        } catch (GenericServiceException e) {
//            request.setAttribute("_ERROR_MESSAGE_", "Error occurred while checking credentials.");
//            return "error";
//        }
//
//        // Check the result of the login attempt
//        if (ModelService.RESPOND_SUCCESS.equals(result.get(ModelService.RESPONSE_MESSAGE))) {
//            GenericValue userLogin = (GenericValue) result.get("userLogin");
//            session.setAttribute("userLogin", userLogin);
//            return "success";
//        } else {
//            String errorMessage = (String) result.get(ModelService.ERROR_MESSAGE);
//            request.setAttribute("_ERROR_MESSAGE_", "Login failed: " + errorMessage);
//            return "error";
//        }
//    }
//}
