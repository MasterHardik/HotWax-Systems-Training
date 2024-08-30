package com.companyname.HotWax_Systems_Training.masterLogin;

import org.apache.ofbiz.base.util.*;
import org.apache.ofbiz.common.authentication.AuthHelper;
import org.apache.ofbiz.common.authentication.api.AuthenticatorException;
import org.apache.ofbiz.common.login.LdapAuthenticationServices;
import org.apache.ofbiz.common.login.LoginServices;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.security.SecurityUtil;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.webapp.control.LoginWorker;
import org.apache.tomcat.util.res.StringManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transaction;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static org.apache.ofbiz.common.login.LoginServices.*;

public class MasterUserLogin {
    private static final String MODULE = LoginServices.class.getName();
    private static final String RESOURCE = "SecurityextUiLabels";
    public static Map<String, Object> masterUserLogin(DispatchContext ctx, Map<String, ?> context) {
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = ctx.getDelegator();

        // load the external auth modules -- note: this will only run once and cache the objects
        if (!AuthHelper.authenticatorsLoaded()) {
            AuthHelper.loadAuthenticators(dispatcher);
        }

        // Authenticate to LDAP if configured to do so
        // TODO: this should be moved to using the NEW Authenticator API
        if ("true".equals(EntityUtilProperties.getPropertyValue("security", "security.ldap.enable", delegator))) {
            if (!LdapAuthenticationServices.userLogin(ctx, context)) {
                String errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.ldap_authentication_failed", locale);
                if ("true".equals(EntityUtilProperties.getPropertyValue("security", "security.ldap.fail.login", delegator))) {
                    return ServiceUtil.returnError(errMsg);
                }
                Debug.logInfo(errMsg, MODULE);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        boolean useEncryption = "true".equals(EntityUtilProperties.getPropertyValue("security", "password.encrypt", delegator));

        // if isServiceAuth is not specified, default to not a service auth
        boolean isServiceAuth = context.get("isServiceAuth") != null && (Boolean) context.get("isServiceAuth");

        String username = (String) context.get("login.username");
        if (username == null) {
            username = (String) context.get("username");
        }
        String password = (String) context.get("login.password");
        if (password == null) {
            password = (String) context.get("password");
        }
        String jwtToken = (String) context.get("login.token");
        if (jwtToken == null) {
            jwtToken = (String) context.get("token");
        }

        // get the visitId for the history entity
        String visitId = (String) context.get("visitId");

        String errMsg = "";
        if (UtilValidate.isEmpty(username)) {
            errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.username_missing", locale);
        } else if (UtilValidate.isEmpty(password) && UtilValidate.isEmpty(jwtToken)) {
            errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.password_missing", locale);
        } else {

            if ("true".equalsIgnoreCase(EntityUtilProperties.getPropertyValue("security", "username.lowercase", delegator))) {
                username = username.toLowerCase(Locale.getDefault());
            }
            if ("true".equalsIgnoreCase(EntityUtilProperties.getPropertyValue("security", "password.lowercase", delegator))) {
                password = password.toLowerCase(Locale.getDefault());
            }

            boolean repeat = true;
            // starts at zero but it incremented at the beginning so in the first pass passNumber will be 1
            int passNumber = 0;

            while (repeat) {
                repeat = false;
                // pass number is incremented here because there are continues in this loop so it may never get to the end
                passNumber++;

                GenericValue userLogin = null;

                try {
                    // only get userLogin from cache for service calls; for web and other manual logins there is less time sensitivity
                    userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", username).cache(isServiceAuth).queryOne();
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, "", MODULE);
                }

                // see if any external auth modules want to sync the user info
                if (userLogin == null) {
                    try {
                        AuthHelper.syncUser(username);
                    } catch (AuthenticatorException e) {
                        Debug.logWarning(e, MODULE);
                    }

                    // check the user login object again
                    try {
                        userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", username).cache(isServiceAuth).queryOne();
                    } catch (GenericEntityException e) {
                        Debug.logWarning(e, "", MODULE);
                    }
                }

                if (userLogin != null) {
                    String ldmStr = EntityUtilProperties.getPropertyValue("security", "login.disable.minutes", delegator);
                    long loginDisableMinutes;

                    try {
                        loginDisableMinutes = Long.parseLong(ldmStr);
                    } catch (Exception e) {
                        loginDisableMinutes = 30;
                        Debug.logWarning("Could not parse login.disable.minutes from security.properties, using default of 30", MODULE);
                    }

                    Timestamp disabledDateTime = userLogin.getTimestamp("disabledDateTime");
                    Timestamp reEnableTime = null;

                    if (loginDisableMinutes > 0 && disabledDateTime != null) {
                        reEnableTime = new Timestamp(disabledDateTime.getTime() + loginDisableMinutes * 60000);
                    }

                    boolean doStore = true;
                    // we might change & store this userLogin, so we should clone it here to get a mutable copy
                    userLogin = GenericValue.create(userLogin);

                    // get the is system flag -- system accounts can only be used for service authentication
                    boolean isSystem = (isServiceAuth && userLogin.get("isSystem") != null) ? "Y".equalsIgnoreCase(userLogin.getString("isSystem"))
                            : false;

                    // grab the hasLoggedOut flag
                    Boolean hasLoggedOut = userLogin.getBoolean("hasLoggedOut");

                    if ((UtilValidate.isEmpty(userLogin.getString("enabled")) || "Y".equals(userLogin.getString("enabled"))
                            || (reEnableTime != null && reEnableTime.before(UtilDateTime.nowTimestamp())) || (isSystem))
                            && UtilValidate.isEmpty(userLogin.getString("disabledBy"))) {
                        String successfulLogin;
                        if (!isSystem) {
                            userLogin.set("enabled", "Y");
                            userLogin.set("disabledBy", null);
                        }
                        // attempt to authenticate with Authenticator class(es)
                        boolean authFatalError = false;
                        boolean externalAuth = false;
                        try {
                            externalAuth = AuthHelper.authenticate(username, password, isServiceAuth);
                        } catch (AuthenticatorException e) {
                            // fatal error -- or single authenticator found -- fail now
                            Debug.logWarning(e, MODULE);
                            authFatalError = true;

                        }

                        // check whether to sign in with Tomcat SSO
                        boolean useTomcatSSO = EntityUtilProperties.propertyValueEquals("security", "security.login.tomcat.sso", "true");
                        HttpServletRequest request = (javax.servlet.http.HttpServletRequest) context.get("request");
                        // when request is not supplied, we will treat that SSO is not required as
                        // in the usage of userLogin service in ICalWorker.java
                        useTomcatSSO = useTomcatSSO && (request != null);

                        // resolve the key for decrypt the token and control the validity
                        boolean jwtTokenValid = SecurityUtil.authenticateUserLoginByJWT(delegator, username, jwtToken);

                        // if the password.accept.encrypted.and.plain property in security is set to true allow plain or encrypted passwords
                        // if this is a system account don't bother checking the passwords
                        // if externalAuth passed; this is run as well
                        if ((!authFatalError && externalAuth) || (useTomcatSSO && tomcatSSOLogin(request, username, password))
                                || (jwtToken != null && jwtTokenValid)
                                || (password != null && checkPassword(userLogin.getString("currentPassword"), useEncryption, password))) {
                            Debug.logVerbose("[LoginServices.userLogin] : Password Matched or Token Validated", MODULE);

                            // update the hasLoggedOut flag
                            if (hasLoggedOut == null || hasLoggedOut) {
                                userLogin.set("hasLoggedOut", "N");
                            }

                            // reset failed login count if necessary
                            Long currentFailedLogins = userLogin.getLong("successiveFailedLogins");
                            if (currentFailedLogins != null && currentFailedLogins > 0) {
                                userLogin.set("successiveFailedLogins", 0L);
                            } else if (hasLoggedOut != null && !hasLoggedOut) {
                                // successful login & no logout flag, no need to change anything, so don't do the store
                                doStore = false;
                            }

                            successfulLogin = "Y";

                            if (!isServiceAuth) {
                                // get the UserLoginSession if this is not a service auth
                                Map<?, ?> userLoginSessionMap = LoginWorker.getUserLoginSession(userLogin);

                                // return the UserLoginSession Map
                                if (userLoginSessionMap != null) {
                                    result.put("userLoginSession", userLoginSessionMap);
                                }
                            }

                            result.put("userLogin", userLogin);
                            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
                        } else {
                            // password is incorrect, but this may be the result of a stale cache entry,
                            // so lets clear the cache and try again if this is the first pass
                            // but only if authFatalError is not true; this would mean the single authenticator failed
                            if (!authFatalError && isServiceAuth && passNumber <= 1) {
                                delegator.clearCacheLine("UserLogin", UtilMisc.toMap("userLoginId", username));
                                repeat = true;
                                continue;
                            }
                            Debug.logInfo("[LoginServices.userLogin] : Password Incorrect", MODULE);
                            // password invalid...
                            if (password != null) {
                                errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.password_incorrect", locale);
                            } else if (jwtToken != null) {
                                errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.token_incorrect", locale);
                            }
                            // increment failed login count
                            Long currentFailedLogins = userLogin.getLong("successiveFailedLogins");

                            if (currentFailedLogins == null) {
                                currentFailedLogins = 1L;
                            } else {
                                currentFailedLogins = currentFailedLogins + 1;
                            }
                            userLogin.set("successiveFailedLogins", currentFailedLogins);

                            // if failed logins over amount in properties file, disable account
                            String mflStr = EntityUtilProperties.getPropertyValue("security", "max.failed.logins", delegator);
                            long maxFailedLogins = 3;
                            try {
                                maxFailedLogins = Long.parseLong(mflStr);
                            } catch (Exception e) {
                                maxFailedLogins = 3;
                                Debug.logWarning("Could not parse max.failed.logins from security.properties, using default of 3", MODULE);
                            }

                            if (maxFailedLogins > 0 && currentFailedLogins >= maxFailedLogins) {
                                userLogin.set("enabled", "N");
                                userLogin.set("disabledDateTime", UtilDateTime.nowTimestamp());
                            }

                            successfulLogin = "N";
                        }

                        // this section is being done in its own transaction rather than in the
                        // current/existing transaction because we may return error and we don't
                        // want that to stop this from getting stored
                        Transaction parentTx = null;
                        boolean beganTransaction = false;

                        try {
                            try {
                                parentTx = TransactionUtil.suspend();
                            } catch (GenericTransactionException e) {
                                Debug.logError(e, "Could not suspend transaction: " + e.getMessage(), MODULE);
                            }

                            try {
                                beganTransaction = TransactionUtil.begin();

                                if (doStore) {
                                    userLogin.store();
                                }

                                if ("true".equals(EntityUtilProperties.getPropertyValue("security", "store.login.history", delegator))) {
                                    boolean createHistory = true;

                                    // only save info on service auth if option set to true to do so
                                    if (isServiceAuth && !"true".equals(
                                            EntityUtilProperties.getPropertyValue("security", "store.login.history.on.service.auth", delegator))) {
                                        createHistory = false;
                                    }

                                    if (createHistory) {
                                        Map<String, Object> ulhCreateMap = UtilMisc.toMap("userLoginId", username, "visitId", visitId, "fromDate",
                                                UtilDateTime.nowTimestamp(), "successfulLogin", successfulLogin);

                                        ModelEntity modelUserLogin = userLogin.getModelEntity();
                                        if (modelUserLogin.isField("partyId")) {
                                            ulhCreateMap.put("partyId", userLogin.get("partyId"));
                                        }

                                        // ONLY save the password if it was incorrect
                                        // we will check in the hash size isn't too huge for the store other wise store a fix string
                                        if ("N".equals(successfulLogin) && !"false".equals(EntityUtilProperties.getPropertyValue("security",
                                                "store.login.history.incorrect.password", delegator))) {
                                            ulhCreateMap.put("passwordUsed", isGivenPasswordCanBeStored(delegator, password)
                                                    ? " TOO LONG FOR STORAGE "
                                                    : password);
                                        }

                                        delegator.create("UserLoginHistory", ulhCreateMap);
                                    }
                                }
                            } catch (GenericEntityException e) {
                                String geeErrMsg = "Error saving UserLoginHistory";
                                if (doStore) {
                                    geeErrMsg += " and updating login status to reset hasLoggedOut, unsuccessful login count, etc.";
                                }
                                try {
                                    TransactionUtil.rollback(beganTransaction, geeErrMsg, e);
                                } catch (GenericTransactionException e2) {
                                    Debug.logError(e2, "Could not rollback nested transaction: " + e2.getMessage(), MODULE);
                                }

                                // if doStore is true then this error should not be ignored and we shouldn't consider it a successful login if this
                                // happens as there is something very wrong lower down that will bite us again later
                                if (doStore) {
                                    return ServiceUtil.returnError(geeErrMsg);
                                }
                            } finally {
                                try {
                                    TransactionUtil.commit(beganTransaction);
                                } catch (GenericTransactionException e) {
                                    Debug.logError(e, "Could not commit nested transaction: " + e.getMessage(), MODULE);
                                }
                            }
                        } finally {
                            // resume/restore parent transaction
                            if (parentTx != null) {
                                try {
                                    TransactionUtil.resume(parentTx);
                                    Debug.logVerbose("Resumed the parent transaction.", MODULE);
                                } catch (GenericTransactionException e) {
                                    Debug.logError(e, "Could not resume parent nested transaction: " + e.getMessage(), MODULE);
                                }
                            }
                        }
                    } else {
                        // account is disabled, but this may be the result of a stale cache entry,
                        // so lets clear the cache and try again if this is the first pass
                        if (isServiceAuth && passNumber <= 1) {
                            delegator.clearCacheLine("UserLogin", UtilMisc.toMap("userLoginId", username));
                            repeat = true;
                            continue;
                        }
                        Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("username", username);
                        errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.account_for_user_login_id_disabled", messageMap, locale);
                        if (disabledDateTime != null) {
                            messageMap = UtilMisc.<String, Object>toMap("disabledDateTime", disabledDateTime);
                            errMsg += " " + UtilProperties.getMessage(RESOURCE, "loginservices.since_datetime", messageMap, locale);
                        } else {
                            errMsg += ".";
                        }

                        if (loginDisableMinutes > 0 && reEnableTime != null) {
                            messageMap = UtilMisc.<String, Object>toMap("reEnableTime", reEnableTime);
                            errMsg += " " + UtilProperties.getMessage(RESOURCE, "loginservices.will_be_reenabled", messageMap, locale);
                        } else {
                            errMsg += " " + UtilProperties.getMessage(RESOURCE, "loginservices.not_scheduled_to_be_reenabled", locale);
                        }
                    }
                } else {
                    // no userLogin object; there may be a non-syncing authenticator
                    boolean externalAuth = false;
                    try {
                        externalAuth = AuthHelper.authenticate(username, password, isServiceAuth);
                    } catch (AuthenticatorException e) {
                        errMsg = e.getMessage();
                        Debug.logError(e, "External Authenticator had fatal exception : " + e.getMessage(), MODULE);
                    }
                    if (externalAuth) {
                        // external auth passed - create a placeholder object for session
                        userLogin = delegator.makeValue("UserLogin");
                        userLogin.set("userLoginId", username);
                        userLogin.set("enabled", "Y");
                        userLogin.set("hasLoggedOut", "N");
                        result.put("userLogin", userLogin);
                        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
                        // TODO: more than this is needed to support 100% external authentication
                        // TODO: party + security information is needed; Userlogin will need to be stored
                    } else {
                        // userLogin record not found, user does not exist
                        errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.user_not_found", locale);
                        Debug.logInfo("[LoginServices.userLogin] Invalid User : '" + username + "'; " + errMsg, MODULE);
                    }
                }
            }
        }

        if (!errMsg.isEmpty()) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_FAIL);
            result.put(ModelService.ERROR_MESSAGE, errMsg);
        }
        return result;
    }
    private static boolean tomcatSSOLogin(HttpServletRequest request, String userName, String currentPassword) {
        try {
            request.login(userName, currentPassword);
        } catch (ServletException e) {

            StringManager sm = StringManager.getManager("org.apache.catalina.connector");
            if (sm.getString("coyoteRequest.alreadyAuthenticated").equals(e.getMessage())) {
                return true;
            } else {
                Debug.logError(e, MODULE);
                return false;
            }
        }
        return true;
    }

    private static boolean isGivenPasswordCanBeStored(Delegator delegator, String password)
            throws GenericEntityException {
        ModelEntity modelEntityUserLoginHistory = delegator.getModelEntity("UserLoginHistory");
        ModelField passwordUsedField = modelEntityUserLoginHistory.getField("passwordUsed");
        int maxPasswordSize = delegator.getEntityFieldType(
                modelEntityUserLoginHistory,
                passwordUsedField.getType()).stringLength();
        int passwordUsedCurrentSize = password.length();

        // if the field is encrypted, we check the size of the hashed result
        ModelField.EncryptMethod encryptMethod = passwordUsedField.getEncryptMethod();
        if (encryptMethod.isEncrypted()) {
            passwordUsedCurrentSize = delegator.encryptFieldValue("UserLoginHistory", encryptMethod, password).toString().length();
        }
        return passwordUsedCurrentSize > maxPasswordSize;
    }


}
