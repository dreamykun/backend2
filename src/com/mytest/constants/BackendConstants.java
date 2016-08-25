package com.mytest.constants;

/**
 * This class defines constants that are only relevant for backend code
 * and not for the user apps.
 */
public class BackendConstants {

    // Constants to identify Testing/Debug scenarios
    //TODO: correct them in final testing and production
    public static final boolean DEBUG_MODE = true;
    public static final boolean TESTING_SKIP_SMS = true;
    public static final boolean TESTING_SKIP_DEVICEID_CHECK = false;

    // <m:api name>,<m:start time>,<m:end time>,<execution duration>,<user id>,<user type>,<mchnt id>,<agent id>,<cust id>,<api parameters>,<m:success/failure>,<exception code>,<exception msg>
    // 50+10+10+5+10+10+10+10+10+50+10+5+100 = ~300 chars
    public static final int BACKEND_EDR_MAX_SIZE = 500;
    public static final String BACKEND_EDR_DELIMETER = ",";
    public static final String BACKEND_EDR_SUB_DELIMETER = ":";
    public static final String BACKEND_EDR_RESULT_OK = "SUCCESS";
    public static final String BACKEND_EDR_RESULT_NOK = "FAILURE";

    // array indexes giving position of EDR fields
    public static final int EDR_API_NAME_IDX = 0;
    public static final int EDR_START_TIME_IDX = 1;
    public static final int EDR_END_TIME_IDX = 2;
    public static final int EDR_EXEC_DURATION_IDX = 3;
    public static final int EDR_USER_ID_IDX = 4;
    public static final int EDR_USER_TYPE_IDX = 5;
    public static final int EDR_MCHNT_ID_IDX = 6;
    public static final int EDR_AGENT_ID_IDX = 7;
    public static final int EDR_CUST_ID_IDX = 8;
    public static final int EDR_API_PARAMS_IDX = 9;
    public static final int EDR_RESULT_IDX = 10;
    public static final int EDR_EXP_CODE_IDX = 11;
    public static final int EDR_EXP_MSG_IDX = 12;
    public static final int BACKEND_EDR_MAX_FIELDS = 13;

    public static final String TIMEZONE = "Asia/Kolkata";
    public static final String DUMMY_DATA = "This is dummy file. Please ignore.";
    public static final String DUMMY_FILENAME = "dummy.txt";

    // Cashback table pool values
    public static final int CASHBACK_TABLE_POOL_START = 0;
    public static final int CASHBACK_TABLE_POOL_SIZE = 1;

    public static final int PASSWORD_LEN = 5;
    public static final int CUSTOMER_PRIVATE_ID_LEN = 6;

    // used in generating temporary passwords
    public static final char[] pwdChars = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
    public static final char[] pinAndOtpChars = "0123456789".toCharArray();
    // used in generating random transaction ids, passwords and PINs
    public static final char[] txnChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();


    public static final int SEND_TXN_SMS_MIN_AMOUNT = 10;
    public static final String APP_ID = "09667F8B-98A7-E6B9-FFEB-B2B6EE831A00";
    public static final String SECRET_KEY = "95971CBD-BADD-C61D-FF32-559664AE4F00";
    //public static final String SECRET_KEY = "3F344A97-DDA8-A8DF-FF4B-FDAC13130700"; //coderunner key

    public static final int LOG_POLICY_NUM_MSGS = 1;
    public static final int LOG_POLICY_FREQ_SECS = 0;

    //public static String PASSWORD_RESET_USER_ID = "00";
    //public static String PASSWORD_RESET_USER_PWD = "aditya123";

    public static final String ROLE_MERCHANT = "Merchant";
    public static final String ROLE_CUSTOMER = "Customer";
    public static final String ROLE_AGENT = "Agent";

    public static final int DEVICE_INFO_VALID_SECS = 300;

    // Customer id type to fetch record
    public static final int CUSTOMER_ID_MOBILE = 0;
    public static final int CUSTOMER_ID_CARD = 1;
    public static final int CUSTOMER_ID_PRIVATE_ID = 2;

    // Merchant id constants
    public static final int MERCHANT_ID_MAX_BATCH_ID_PER_RANGE = 99; // 2 digit batchId
    public static final int MERCHANT_ID_MAX_SNO_PER_BATCH = 1000; // 3 digit serialNo

    public static final String MY_CARD_ISSUER_ID = "51";
    public static final int CARD_ID_MAX_BATCH_ID_PER_RANGE = 999; // 3 digit batchId
    public static final int CARD_ID_MAX_SNO_PER_BATCH = 1000; // 3 digit serialNo
}
