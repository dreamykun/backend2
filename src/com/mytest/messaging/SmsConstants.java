package com.mytest.messaging;

/**
 * Created by adgangwa on 12-05-2016.
 */
public class SmsConstants {

    public static String SMSGW_BASE_URL = "https://www.txtguru.in/imobile/api.php?";
    public static String SMSGW_URL_ENCODING = "UTF-8";
    public static String SMSGW_USERNAME = "aditya_gang";
    public static String SMSGW_PASSWORD = "50375135";
    public static String SMSGW_SENDER_ID = "UPDATE";

    public static String COUNTRY_CODE = "91";

    /*
     * SMS templates
     */

    public static String SMS_TXN_DEBIT_CL_CB = "Mycash: %s debited Rs %d from your Account and Rs %d from Cashback on %s. Balance:- Account:Rs %d, Cashback:Rs %d.";
    public static String SMS_TXN_CREDIT_CL_DEBIT_CB = "Mycash: %s added Rs %d to your Account and debited Rs %d from Cashback on %s. Balance:- Account:Rs %d, Cashback:Rs %d.";

    public static String SMS_TXN_CREDIT_CL = "Mycash: %s added Rs %d to your Account on %s. Balance:- Account:Rs %d, Cashback:Rs %d.";
    public static String SMS_TXN_DEBIT_CL = "Mycash: %s debited Rs %d from your Account on %s. Balance:- Account:Rs %d, Cashback:Rs %d.";
    public static String SMS_TXN_DEBIT_CB = "Mycash: %s debited Rs %d from your Cashback on %s. Balance:- Account:Rs %d, Cashback:Rs %d";

    public static String SMS_FIRST_PASSWD = "Dear Merchant - Welcome to MyCash family !! Your User ID is %s, and your first password is '%s'. PLZ CHANGE PASSWORD AFTER FIRST LOGIN.";
    public static String SMS_PASSWD = "MyCash new password for user %s is '%s'. PLS CALL CUSTOMER CARE IF NOT REQUESTED BY YOU.";
    public static String SMS_PIN = "MyCash transaction PIN for user %s is '%s'. PLS DO NOT SHARE WITH ANYONE.";
    public static String SMS_PASSWD_CHANGED = "MyCash password changed successfully for user %s. PLS CALL CUSTOMER CARE IF NOT DONE BY YOU.";

    public static String SMS_OTP = "You have initiated '%s' txn for user %s. OTP is '%s' and valid for %d mins only. PLS CALL US IF NOT DONE BY YOU.";

    public static String SMS_NEW_MOBILE = "You have changed registered mobile number of your account %s to %s. PLS CALL US IMMEDIATELY IF NOT DONE BY YOU.";
    public static String SMS_CUSTOMER_NEW_CARD = "You have registered new card with number %s to your account %s. PLS CALL US IMMEDIATELY IF NOT DONE BY YOU.";

}
