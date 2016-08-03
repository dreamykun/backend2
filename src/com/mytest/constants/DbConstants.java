package com.mytest.constants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by adgangwa on 07-05-2016.
 */
public class DbConstants {

    // Table names - for which class name is not same
    public static String CASHBACK_TABLE_NAME = "Cashback";
    public static String TRANSACTION_TABLE_NAME = "Transaction";

    // Users table - 'user_type' column values
    public static final int USER_TYPE_MERCHANT = 0;
    public static final int USER_TYPE_CUSTOMER = 1;
    public static final int USER_TYPE_AGENT = 2;

    // Merchant table - 'admin_status' column values
    public static final int USER_STATUS_NEW_REGISTERED = 1;
    public static final int USER_STATUS_ACTIVE = 2;
    // Disabled means permanent - until further action by the admin to explicitly enable the account
    public static final int USER_STATUS_DISABLED = 3;
    // Locked means temporary - and will be automatically unlocked after defined duration
    public static final int USER_STATUS_LOCKED = 4;
    // Error during registration - to be manually deleted
    public static final int USER_STATUS_REG_ERROR = 5;
    // status code to text description
    public static String userStatusDesc[] = {
            "",
            "New Registered",
            "Active",
            "Disabled",
            "Locked",
            "Not Registered"
    };

    // Merchant table - 'status_reason' column values
    public static final int ENABLED_NEW_USER = 1;
    public static final int ENABLED_ACTIVE = 2;
    public static final int DISABLED_AUTO_BY_SYSTEM = 3;
    public static final int DISABLED_ON_USER_REQUEST = 4;
    public static final int LOCKED_WRONG_PASSWORD_LIMIT_RCHD = 5;
    public static final int LOCKED_WRONG_PIN_LIMIT_RCHD = 6;
    public static final int LOCKED_FORGOT_PASSWORD_ATTEMPT_LIMIT_RCHD = 7;
    public static final int LOCKED_FORGOT_USERID_ATTEMPT_LIMIT_RCHD = 8;
    public static final int REG_ERROR_ROLE_ASSIGN_FAILED = 9;

    // Map int status values to corresponding descriptions
    public static String statusReasonDescriptions[] = {
            "",
            "User is new registered",
            "User is active",
            "By system for security purpose. Will be re-activated after verification.",
            "On user request.",
            "Wrong password attempts limit reached.",
            "Wrong PIN attempts limit reached.",
            "Wrong 'password reset' attempts limit reached.",
            ""
    };

    // CustomerCards table - 'status' column values
    public static final int CUSTOMER_CARD_STATUS_NEW = 0;
    public static final int CUSTOMER_CARD_STATUS_WITH_MERCHANT = 1;
    public static final int CUSTOMER_CARD_STATUS_ALLOTTED = 2;
    //public static final int CUSTOMER_CARD_STATUS_BLOCKED = 3;
    public static final int CUSTOMER_CARD_STATUS_REMOVED = 3;
    // Map int status values to corresponding descriptions
    public static String cardStatusDescriptions[] = {
            "Invalid",
            "Invalid",
            "Invalid",
            "Invalid"
    };


    // CustomerOps table - 'opcode' column values
    public static final String CUSTOMER_OP_NEW_CARD = "NEW CARD";
    public static final String CUSTOMER_OP_RESET_PIN = "RESET PIN";
    public static final String CUSTOMER_OP_CHANGE_MOBILE = "CHANGE MOBILE";
    // CustomerOps table - 'status' column values
    public static final String CUSTOMER_OP_STATUS_OTP_GENERATED = "statusOtpGenerated";
    public static final String CUSTOMER_OP_STATUS_OTP_MATCHED = "OTP Matched";

    // MerchantOps table - 'opcode' column values
    public static final String MERCHANT_OP_RESET_PASSWD = "Reset Password";
    public static final String MERCHANT_OP_CHANGE_MOBILE = "Change Mobile";
    // MerchantOps table - 'status' column values
    public static final String MERCHANT_OP_STATUS_PENDING = "pending";
    public static final String MERCHANT_OP_STATUS_LOCKED = "locked";
    public static final String MERCHANT_OP_STATUS_COMPLETE = "complete";

    // Otp table - 'opcode' column values - apart from ones from 'MerchantOps' and 'CustomerOps' tables
    public static final String MERCHANT_OP_NEW_DEVICE_LOGIN = "new_device_login";

    // Counters table - 'name' column values
    public static final String CUSTOMER_ID_COUNTER = "customer_id";
    public static final String MERCHANT_ID_COUNTER = "merchant_id";

    // WrongAttempts table - 'attempt_type' column values
    public static final String ATTEMPT_TYPE_FORGOT_USERID = "forgotUserId";
    public static final String ATTEMPT_TYPE_PASSWORD_RESET = "passwordReset";
    public static final String ATTEMPT_TYPE_USER_LOGIN = "userLogin";
    public static final String ATTEMPT_TYPE_USER_PIN = "userPin";

    // Map from backend error codes to local error codes
    public static final Map<String, Integer> attemptTypeToAccLockedReason;
    static {
        Map<String, Integer> aMap = new HashMap<>(10);

        // my own backend response codes
        aMap.put(ATTEMPT_TYPE_FORGOT_USERID, LOCKED_FORGOT_USERID_ATTEMPT_LIMIT_RCHD);
        aMap.put(ATTEMPT_TYPE_PASSWORD_RESET, LOCKED_FORGOT_PASSWORD_ATTEMPT_LIMIT_RCHD);
        aMap.put(ATTEMPT_TYPE_USER_LOGIN, LOCKED_WRONG_PASSWORD_LIMIT_RCHD);
        aMap.put(ATTEMPT_TYPE_USER_PIN, LOCKED_WRONG_PIN_LIMIT_RCHD);

        attemptTypeToAccLockedReason = Collections.unmodifiableMap(aMap);
    }


    // GlobalSettings table - should be exactly same as 'names' in DB
    public static final String SETTINGS_MERCHANT_PASSWD_RESET_MINS = "merchant_passwd_reset_mins";
    public static final String SETTINGS_MERCHANT_PASSWD_RESET_REQUEST_GAP = "merchant_passwd_reset_request_gap_mins";
    public static final String SETTINGS_MERCHANT_WRONG_ATTEMPTS = "merchant_wrong_attempts";
    public static final String SETTINGS_MERCHANT_ACCOUNT_BLOCK_HRS = "merchant_account_block_hrs";
    public static final String SETTINGS_DASHBOARD_NO_REFRESH_HRS = "dashboard_no_refresh_hours";

    public static final String SETTINGS_CL_CREDIT_LIMIT_FOR_PIN = "cl_credit_limit_for_pin";
    public static final String SETTINGS_CL_DEBIT_LIMIT_FOR_PIN = "cl_debit_limit_for_pin";
    public static final String SETTINGS_CB_DEBIT_LIMIT_FOR_PIN = "cb_debit_limit_for_pin";
    public static final String SETTINGS_CB_REDEEM_LIMIT = "cb_redeem_limit";
    public static final String SETTINGS_REPORTS_HISTORY_DAYS = "reports_history_days";
    public static final String SETTINGS_REPORTS_BLACKOUT_END = "reports_blackout_end";
    public static final String SETTINGS_REPORTS_BLACKOUT_START = "reports_blackout_start";

    public static final String SETTINGS_CUSTOMER_ACCOUNT_BLOCK_HRS = "customer_account_block_hrs";
    public static final String SETTINGS_CUSTOMER_WRONG_ATTEMPTS = "customer_wrong_attempts";
    public static final String SETTINGS_CUSTOMER_CASH_LIMIT = "cash_account_max_limit";
    public static final String SETTINGS_OTP_VALID_MINS = "otp_valid_mins";
    public static final String SETTINGS_SERVICE_DISABLED_UNTIL = "service_disabled_until";
    public static final String SETTINGS_TXN_IMAGE_CAPTURE_MODE = "txn_image_capture_mode";

    // GlobalSettings table - 'txn_image_capture_mode' column values
    public static final int TXN_IMAGE_CAPTURE_ALWAYS = 0;
    public static final int TXN_IMAGE_CAPTURE_NO_PIN = 1;
    public static final int TXN_IMAGE_CAPTURE_ALL_DEBIT = 2;
    public static final int TXN_IMAGE_CAPTURE_NEVER = 3;

    // GlobalSettings table - 'value_datatype' column values
    public static final int DATATYPE_INT = 1;
    public static final int DATATYPE_STRING = 2;
    public static final int DATATYPE_DATE = 3;
}
