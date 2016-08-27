package com.mytest.services;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.exceptions.BackendlessException;
import com.backendless.logging.Logger;
import com.backendless.servercode.IBackendlessService;
import com.mytest.constants.BackendConstants;
import com.mytest.constants.BackendResponseCodes;
import com.mytest.constants.DbConstants;
import com.mytest.constants.DbConstantsBackend;
import com.mytest.database.Agents;
import com.mytest.database.InternalUserDevice;
import com.mytest.database.Merchants;
import com.mytest.messaging.SmsConstants;
import com.mytest.messaging.SmsHelper;
import com.mytest.utilities.BackendOps;
import com.mytest.utilities.CommonUtils;
import com.mytest.utilities.MyLogger;

/**
 * Created by adgangwa on 17-07-2016.
 */
public class AgentServicesNoLogin implements IBackendlessService {

    private MyLogger mLogger = new MyLogger("services.AgentServicesNoLogin");
    private String[] mEdr = new String[BackendConstants.BACKEND_EDR_MAX_FIELDS];;

    /*
     * Public methods: Backend REST APIs
     */
    public void setDeviceForAgentLogin(String loginId, String deviceId) {
        //initCommon();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "setDeviceForAgentLogin";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = loginId+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                deviceId;

        try {
            if (deviceId == null || deviceId.isEmpty()) {
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_WRONG_INPUT_DATA, "");
            }

            mLogger.debug("In setDeviceForLogin: " + loginId + ": " + deviceId);
            //mLogger.debug(InvocationContext.asString());
            //mLogger.debug("Before: "+HeadersManager.getInstance().getHeaders().toString());

            // fetch internal user device data
            InternalUserDevice deviceData = BackendOps.fetchInternalUserDevice(loginId);
            if(deviceData==null) {
                deviceData = new InternalUserDevice();
                deviceData.setUserId(loginId);
                deviceData.setInstanceId("");
            }

            // Update device Info in merchant object
            deviceData.setTempId(deviceId);
            BackendOps.saveInternalUserDevice(deviceData);

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch(Exception e) {
            CommonUtils.handleException(e,false,mLogger,mEdr);
            throw e;
        } finally {
            CommonUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    public void resetAgentPassword(String userId, String secret1) {
        //initCommon();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "resetAgentPassword";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = userId+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                secret1;

        try {
            mLogger.debug("In resetAgentPassword: " + userId);

            // fetch user with the given id with related agent object
            BackendlessUser user = BackendOps.fetchUser(userId, DbConstants.USER_TYPE_AGENT);
            Agents agent = (Agents) user.getProperty("agent");
            mEdr[BackendConstants.EDR_USER_ID_IDX] = (String)user.getProperty("user_id");
            mEdr[BackendConstants.EDR_USER_TYPE_IDX] = ((Integer)user.getProperty("user_type")).toString();
            mEdr[BackendConstants.EDR_AGENT_ID_IDX] = agent.getId();

            // check admin status
            CommonUtils.checkAgentStatus(agent);

            // check for 'extra verification'
            String dob = agent.getDob();
            if (dob == null || !dob.equalsIgnoreCase(secret1)) {
                CommonUtils.handleWrongAttempt(userId, agent, DbConstants.USER_TYPE_AGENT, DbConstantsBackend.ATTEMPT_TYPE_PASSWORD_RESET);
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_VERIFICATION_FAILED, "");
            }

            handlePasswdResetImmediate(user, agent);
            mLogger.debug("Processed passwd reset op for: " + agent.getMobile_num());

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch (Exception e) {
            mLogger.error("Exception in resetAgentPassword: "+e.toString());
            mLogger.flush();
            throw e;
        }
    }


    /*
     * Private helper methods
     */
    private void handlePasswdResetImmediate(BackendlessUser user, Agents agent) {
        // generate password
        String passwd = CommonUtils.generateTempPassword();
        // update user account for the password
        user.setPassword(passwd);
        user = BackendOps.updateUser(user);
        mLogger.debug("Updated agent for password reset: "+agent.getId());

        // Send SMS through HTTP
        String smsText = SmsHelper.buildAgentPwdResetSMS(agent.getId(), passwd);
        if( SmsHelper.sendSMS(smsText, agent.getMobile_num()) ){
            mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_OK;
        } else {
            mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_NOK;
            throw new BackendlessException(BackendResponseCodes.BE_ERROR_SEND_SMS_FAILED, "");
        };
        mLogger.debug("Sent first password reset SMS: "+agent.getMobile_num());
    }
}
