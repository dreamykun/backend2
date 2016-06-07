package com.mytest.events.user_service;

import com.backendless.Backendless;
import com.backendless.logging.Logger;
import com.backendless.servercode.ExecutionResult;
import com.backendless.servercode.RunnerContext;
import com.mytest.constants.BackendConstants;
import com.mytest.constants.BackendResponseCodes;
import com.mytest.constants.CommonConstants;
import com.mytest.constants.DbConstants;
import com.mytest.database.*;
import com.mytest.utilities.*;

import java.util.*;

/**
* GenericUserEventHandler handles the User Service events.
* The event handlers are the individual methods implemented in the class.
* The "before" and "after" prefix determines if the handler is executed before
* or after the default handling logic provided by Backendless.
* The part after the prefix identifies the actual event.
* For example, the "beforeLogin" method is the "Login" event handler and will
* be called before Backendless applies the default login logic. The event
* handling pipeline looks like this:

* Client Request ---> Before Handler ---> Default Logic ---> After Handler --->
* Return Response
*/

public class GenericUserEventHandler extends com.backendless.servercode.extension.UserExtender
{
    private Logger mLogger;
    private BackendOps mBackendOps;

    @Override
    public void afterLogin( RunnerContext context, String login, String password, ExecutionResult<HashMap> result ) throws Exception
    {
        initCommon();
        mLogger.debug("In GenericUserEventHandler: afterLogin");

        String userId = (String) result.getResult().get("user_id");
        Integer userType = (Integer) result.getResult().get("user_type");

        if(result.getException()==null) {
            if (userType == DbConstants.USER_TYPE_MERCHANT) {
                Merchants merchant = mBackendOps.getMerchant(userId, true);
                if(merchant==null) {
                    mBackendOps.logoutUser();
                    CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
                }

                // check admin status
                String status = CommonUtils.checkMerchantStatus(merchant);
                if(status != null) {
                    mBackendOps.logoutUser();
                    CommonUtils.throwException(mLogger,status, "Merchant account is inactive", false);
                }

                // Check if 'device id' not set
                // This is set in setDeviceId() backend API, only after OTP verification
                String deviceInfo = merchant.getTempDevId();
                if(deviceInfo==null || deviceInfo.isEmpty()) {
                    mBackendOps.logoutUser();
                    CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_NOT_TRUSTED_DEVICE, "Login attempt from untrusted device", false);
                }

                // Add to 'trusted list', if not already there
                // deviceInfo format: <device id>,<manufacturer>,<model>,<os version>
                String[] csvFields = deviceInfo.split(CommonConstants.CSV_DELIMETER);
                String deviceId = csvFields[0];

                // Match device id
                boolean matched = false;
                List<MerchantDevice> trustedDevices = merchant.getTrusted_devices();
                if(trustedDevices != null) {
                    for (MerchantDevice device : trustedDevices) {
                        if(device.getDevice_id().equals(deviceId)) {
                            matched = true;
                            device.setLast_login(new Date());
                            break;
                        }
                    }
                } else {
                    trustedDevices = new ArrayList<>();
                }
                // Add new device in the trusted list
                if(!matched) {
                    // New device - add as trusted device
                    MerchantDevice device = new MerchantDevice();
                    device.setMerchant_id(merchant.getAuto_id());
                    device.setDevice_id(csvFields[0]);
                    device.setManufacturer(csvFields[1]);
                    device.setModel(csvFields[2]);
                    device.setOs_type("Android");
                    device.setOs_version(csvFields[3]);
                    device.setLast_login(new Date());

                    trustedDevices.add(device);

                    // Update merchant
                    merchant.setTempDevId(null);
                    merchant.setTrusted_devices(trustedDevices);
                    // when USER_STATUS_NEW_REGISTERED, the device will also be new
                    // so it is not required to update this outside this if block
                    if(merchant.getAdmin_status() == DbConstants.USER_STATUS_NEW_REGISTERED) {
                        merchant.setAdmin_status(DbConstants.USER_STATUS_ACTIVE);
                    }
                    Merchants merchant2 = mBackendOps.updateMerchant(merchant);
                    if(merchant2==null) {
                        mBackendOps.logoutUser();
                        CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
                    }
                }
            }
        } else {
            if (userType == DbConstants.USER_TYPE_MERCHANT) {
                // login failed - increase count if failed due to wrong password
                if(result.getException().getCode() == Integer.parseInt(BackendResponseCodes.BL_ERROR_INVALID_ID_PASSWD)) {
                    // fetch merchant
                    Merchants merchant = mBackendOps.getMerchant(userId, false);
                    if(merchant!=null &&
                            CommonUtils.handleMerchantWrongAttempt(mBackendOps, merchant, DbConstants.ATTEMPT_TYPE_USER_LOGIN) ) {
                        // override exception type
                        CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_FAILED_ATTEMPT_LIMIT_RCHD,
                                "Merchant wrong password attempt limit reached: "+merchant.getAuto_id(), false);
                    }
                }
            }
        }
    }

    @Override
    public void beforeRegister( RunnerContext context, HashMap userValue ) throws Exception
    {
        initCommon();
        mLogger.debug("In GenericUserEventHandler: beforeRegister");

        // If merchant, generate login id and password
        // If customer, generate private id and PIN
        String userId = (String) userValue.get("user_id");
        Integer userType = (Integer) userValue.get("user_type");

        if (userType == DbConstants.USER_TYPE_MERCHANT) {
            Merchants merchant = (Merchants) userValue.get("merchant");
            if (merchant != null) {
                // get merchant counter value and use the same to generate merchant id
                Double merchantCnt =  mBackendOps.fetchCounterValue(DbConstants.MERCHANT_ID_COUNTER);
                if(merchantCnt == null) {
                    CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
                }
                mLogger.debug("Fetched merchant cnt: "+merchantCnt.longValue());
                // set merchant id
                String merchantId = CommonUtils.generateMerchantId(merchantCnt.longValue());
                mLogger.debug("Generated merchant id: "+merchantId);
                userValue.put("user_id", merchantId);
                merchant.setAuto_id(merchantId);
                merchant.setAdmin_status(DbConstants.USER_STATUS_NEW_REGISTERED);
                merchant.setStatus_reason(DbConstants.ENABLED_NEW_USER);
                merchant.setStatus_update_time(new Date());
                merchant.setAdmin_remarks("New registered merchant");

                // generate and set password
                String pwd = CommonUtils.generateMerchantPassword();
                mLogger.debug("Generated passwd: "+pwd);
                userValue.put("password",pwd);

                // set cashback and transaction table names
                setCbAndTransTables(merchant, merchantCnt.longValue());
            } else {
                String errorMsg = "Merchant object is null: " + userId;
                CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_GENERAL, errorMsg, false);
            }
        }
        Backendless.Logging.flush();
    }

    @Override
    public void beforeUpdate( RunnerContext context, HashMap userValue ) throws Exception
    {
        initCommon();
        mLogger.debug("In GenericUserEventHandler: beforeRegister");

        // If merchant, generate login id and password
        // If customer, generate private id and PIN
        String userId = (String) userValue.get("user_id");
        Integer userType = (Integer) userValue.get("user_type");

        if (userType == DbConstants.USER_TYPE_MERCHANT) {
            Merchants merchant = (Merchants) userValue.get("merchant");
            if(merchant==null) {
                // fetch merchant
                merchant = mBackendOps.getMerchant(userId, false);
                if(merchant==null) {
                    CommonUtils.throwException(mLogger, BackendResponseCodes.BE_ERROR_NO_SUCH_USER, "No merchant object with id "+userId, false);
                }
            }

            String status = CommonUtils.checkMerchantStatus(merchant);
            if(status != null) {
                CommonUtils.throwException(mLogger, status, "Merchant account not active: "+userId, false);
            }

        } else if (userType == DbConstants.USER_TYPE_CUSTOMER) {
            Customers customer = (Customers) userValue.get("customer");
            if(customer==null) {
                // fetch merchant
                customer = mBackendOps.getCustomer(userId, true);
                if(customer==null) {
                    CommonUtils.throwException(mLogger, BackendResponseCodes.BE_ERROR_NO_SUCH_USER, "No customer object with id "+userId, false);
                }
            }

            String status = CommonUtils.checkCustomerStatus(customer);
            if(status != null) {
                CommonUtils.throwException(mLogger, status, "Customer account not active: "+userId, false);
            }
        }
    }

    /*
     * Private helper methods
     */
    private void initCommon() {
        // Init logger and utils
        Backendless.Logging.setLogReportingPolicy(BackendConstants.LOG_POLICY_NUM_MSGS, BackendConstants.LOG_POLICY_FREQ_SECS);
        mLogger = Backendless.Logging.getLogger("com.mytest.services.GenericUserEventHandler");
        mBackendOps = new BackendOps(mLogger);
    }

    private void setCbAndTransTables(Merchants merchant, long regCounter) {
        // decide on the cashback table using round robin
        //int pool_size = gSettings.getCb_table_pool_size();
        //int pool_start = gSettings.getCb_table_pool_start();
        int pool_size = BackendConstants.CASHBACK_TABLE_POOL_SIZE;
        int pool_start = BackendConstants.CASHBACK_TABLE_POOL_START;

        // use last 4 numeric digits for round-robin
        //int num = Integer.parseInt(getUser_id().substring(2));
        int table_suffix = pool_start + ((int)(regCounter % pool_size));
        //int table_suffix = pool_start + (num % pool_size);

        String cbTableName = DbConstants.CASHBACK_TABLE_NAME + String.valueOf(table_suffix);
        merchant.setCashback_table(cbTableName);
        mLogger.debug("Generated cashback table name:" + cbTableName);

        // use the same prefix for cashback and transaction tables
        // as there is 1-to-1 mapping in the table schema - transaction0 maps to cashback0 only
        //pool_size = MyGlobalSettings.getGlobalSettings().getTxn_table_pool_size();
        //pool_start = MyGlobalSettings.getGlobalSettings().getTxn_table_pool_start();
        //table_suffix = pool_start + ((int)(mRegCounter % pool_size));

        String transTableName = DbConstants.TRANSACTION_TABLE_NAME + String.valueOf(table_suffix);
        merchant.setTxn_table(transTableName);
        mLogger.debug("Generated transaction table name:" + transTableName);
    }
}

    /*
    private static int MAX_DEVICES_PER_MERCHANT = 3;

    @Override
    public void beforeLogin( RunnerContext context, String login, String password ) throws Exception
    {
        Backendless.Logging.setLogReportingPolicy(AppConstants.LOG_POLICY_NUM_MSGS, AppConstants.LOG_POLICY_FREQ_SECS);
        mLogger = Backendless.Logging.getLogger("com.mytest.events.GenericUserEventHandler");
        BackendOps backendOps = new BackendOps(mLogger);

        mLogger.debug("In beforeLogin: "+login);

        // Login id contains device id too - seperate them out
        String[] csvFields = login.split(AppConstants.CSV_DELIMETER);
        String loginId = csvFields[0];
        String deviceId = csvFields[1];

        BackendlessUser user = backendOps.fetchUser(loginId, DbConstants.USER_TYPE_MERCHANT);
        if(user==null) {
            BackendlessFault fault = new BackendlessFault(backendOps.mLastOpStatus,"Failed to fetch global settings");
            throw new BackendlessException(fault);
        }
        Merchants merchant = (Merchants) user.getProperty("merchant");

        // Check current admin status
        int status = merchant.getAdmin_status();
        switch(status) {
            case DbConstants.USER_STATUS_REGISTERED:
                logoutSync();
                return ErrorCodes.USER_NEW;
            case DbConstants.USER_STATUS_DISABLED:
                logoutSync();
                return ErrorCodes.USER_ACC_DISABLED;
        }

        List<MerchantDevice> trustedDevices = merchant.getTrusted_devices();
        int cnt = 0;
        boolean matched = false;
        if(trustedDevices != null) {
            cnt = trustedDevices.size();
            for (MerchantDevice device : trustedDevices) {
                if(device.getDevice_id().equals(deviceId)) {
                    matched = true;
                    break;
                }
            }
        }

        // if no matching device id found - means new device for this user
        // generate OTP if limit not reached
        if(!matched) {
            // Check for max devices allowed per user
            if(cnt >= MAX_DEVICES_PER_MERCHANT) {
                BackendlessFault fault = new BackendlessFault(AppConstants.BE_ERROR_MERCHANT_DEVICE_LIMIT_RCHD,"");
                throw new BackendlessException(fault);
            }
            // First login for this  - generate OTP and generate exception
            AllOtp newOtp = new AllOtp();
            newOtp.setUser_id(loginId);
            newOtp.setMobile_num(merchant.getMobile_num());
            newOtp.setOpcode(DbConstants.MERCHANT_OP_NEW_DEVICE_LOGIN);
            newOtp = backendOps.generateOtp(newOtp);
            if(newOtp == null) {
                // failed to generate otp
                BackendlessFault fault = new BackendlessFault(AppConstants.BE_ERROR_OTP_GENERATE_FAILED,"Failed to generate OTP");
                throw new BackendlessException(fault);
            } else {
                // OTP generated successfully - return exception to indicate so
                BackendlessFault fault = new BackendlessFault(AppConstants.BE_ERROR_OTP_GENERATED,"OTP generated");
                throw new BackendlessException(fault);
            }
        }
    }
    */

    /*
    @Override
    public void afterRegister( RunnerContext context, HashMap userValue, ExecutionResult<HashMap> result ) throws Exception {
        Backendless.Logging.setLogReportingPolicy(AppConstants.LOG_POLICY_NUM_MSGS, AppConstants.LOG_POLICY_FREQ_SECS);
        mLogger = Backendless.Logging.getLogger("com.mytest.events.GenericUserEventHandler");

        mLogger.debug("In GenericUserEventHandler: afterRegister");
        Backendless.Logging.flush();

        // send password in SMS, if registration is successful
        if (result.getException() == null) {
            mLogger.debug("A2");
            Backendless.Logging.flush();

            HashMap user = result.getResult();
            mLogger.debug("A3");
            Backendless.Logging.flush();
            String userId = (String) user.get("user_id");
            Integer userType = (Integer) user.get("user_type");

            if (userType == DbConstants.USER_TYPE_CUSTOMER) {
                Customers customer = (Customers) user.get("customer");
                if (customer != null) {
                    // Send sms to the customer with PIN
                    String pin = customer.getTxn_pin();
                    String smsText = String.format(SmsConstants., userId, pin);
                    // Send SMS through HTTP
                    if (!SmsHelper.sendSMS(smsText, customer.getMobile_num())) {
                        // TODO: write to alarm table for retry later
                    }
                } else {
                    mLogger.error("Customer object is null: " + userId);
                }
            }
        } else {
            mLogger.error("In afterRegister, received exception: " + result.getException().toString());
            Backendless.Logging.flush();
        }
    }*/

