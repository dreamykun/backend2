package in.myecash.services;

import com.backendless.BackendlessUser;
import com.backendless.exceptions.BackendlessException;
import com.backendless.servercode.IBackendlessService;
import in.myecash.messaging.SmsHelper;
import in.myecash.utilities.BackendOps;
import in.myecash.utilities.BackendUtils;
import in.myecash.utilities.MyLogger;

import in.myecash.common.constants.*;
import in.myecash.constants.*;
import in.myecash.database.*;

/**
 * Created by adgangwa on 17-07-2016.
 */
public class InternalUserServicesNoLogin implements IBackendlessService {

    private MyLogger mLogger = new MyLogger("services.InternalUserServicesNoLogin");
    private String[] mEdr = new String[BackendConstants.BACKEND_EDR_MAX_FIELDS];;

    /*
     * Public methods: Backend REST APIs
     */
    public void setDeviceForInternalUserLogin(String loginId, String deviceId) {
        //initCommon();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "setDeviceForInternalUserLogin";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = loginId+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                deviceId;
        // debug logs for this fx. will never be written
        // taking this as default - as dont have access to internal user object
        // taking USER_TYPE_AGENT as default
        mLogger.setProperties(loginId, DbConstants.USER_TYPE_AGENT, false);

        try {
            if (deviceId == null || deviceId.isEmpty()) {
                throw new BackendlessException(String.valueOf(ErrorCodes.WRONG_INPUT_DATA), "");
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
            BackendUtils.handleException(e,false,mLogger,mEdr);
            throw e;
        } finally {
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    public void resetInternalUserPassword(String userId, String secret1) {
        BackendUtils.initAll();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "resetInternalUserPassword";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = userId+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                secret1;

        try {
            mLogger.debug("In resetInternalUserPassword: " + userId);

            // fetch user with the given id with related object
            // taking USER_TYPE_AGENT default - doesnt matter as such
            BackendlessUser user = BackendOps.fetchUser(userId, DbConstants.USER_TYPE_AGENT, false);
            int userType = (Integer)user.getProperty("user_type");
            if(userType != DbConstants.USER_TYPE_AGENT &&
                    userType != DbConstants.USER_TYPE_CC &&
                    userType != DbConstants.USER_TYPE_CNT) {
                throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED),userId+" is not an internal user.");
            }

            InternalUser internalUser = (InternalUser) user.getProperty("internalUser");
            mLogger.setProperties(internalUser.getId(), userType, internalUser.getDebugLogs());

            mEdr[BackendConstants.EDR_USER_ID_IDX] = (String)user.getProperty("user_id");
            mEdr[BackendConstants.EDR_USER_TYPE_IDX] = ((Integer)user.getProperty("user_type")).toString();
            mEdr[BackendConstants.EDR_INTERNAL_USER_ID_IDX] = internalUser.getId();

            // check admin status
            BackendUtils.checkInternalUserStatus(internalUser);

            // check for 'extra verification'
            String dob = internalUser.getDob();
            if (dob == null || !dob.equalsIgnoreCase(secret1)) {
                BackendUtils.handleWrongAttempt(userId, internalUser, userType,
                        DbConstantsBackend.WRONG_PARAM_TYPE_VERIFICATION, DbConstants.OP_RESET_PASSWD, mEdr, mLogger);
                throw new BackendlessException(String.valueOf(ErrorCodes.VERIFICATION_FAILED), "");
            }

            internalUserPwdResetImmediate(user, internalUser);
            mLogger.debug("Processed passwd reset op for: " + internalUser.getMobile_num());

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch(Exception e) {
            BackendUtils.handleException(e,false,mLogger,mEdr);
            throw e;
        } finally {
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }


    /*
     * Private helper methods
     */
    private void internalUserPwdResetImmediate(BackendlessUser user, InternalUser internalUser) {
        // generate password
        String passwd = BackendUtils.generateTempPassword();
        // update user account for the password
        user.setPassword(passwd);
        user = BackendOps.updateUser(user);
        mLogger.debug("Updated internal user for password reset: "+internalUser.getId());

        // Send SMS through HTTP
        String smsText = SmsHelper.buildPwdResetSMS(internalUser.getId(), passwd);
        if( !SmsHelper.sendSMS(smsText, internalUser.getMobile_num(), mEdr, mLogger) ){
            throw new BackendlessException(String.valueOf(ErrorCodes.SEND_SMS_FAILED), "");
        }
        mLogger.debug("Sent first password reset SMS: "+internalUser.getMobile_num());
    }

}
