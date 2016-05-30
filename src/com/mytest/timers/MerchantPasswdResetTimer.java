package com.mytest.timers;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.logging.Logger;
import com.backendless.servercode.annotation.BackendlessTimer;
import com.mytest.database.DbConstants;
import com.mytest.database.MerchantOps;
import com.mytest.database.Merchants;
import com.mytest.messaging.SmsConstants;
import com.mytest.messaging.SmsHelper;
import com.mytest.utilities.*;

import java.util.ArrayList;
import java.util.Date;

/**
 * MerchantPasswdResetTimer is a timer.
 * It is executed according to the schedule defined in Backendless Console. The
 * class becomes a timer by extending the TimerExtender class. The information
 * about the timer, its name, schedule, expiration date/time is configured in
 * the special annotation - BackendlessTimer. The annotation contains a JSON
 * object which describes all properties of the timer.
 */
@BackendlessTimer("{'startDate':1464294360000,'frequency':{'schedule':'custom','repeat':{'every':120}},'timername':'MerchantPasswdReset'}")
public class MerchantPasswdResetTimer extends com.backendless.servercode.extension.TimerExtender
{
    private Logger mLogger;
    private BackendOps mBackendOps;

    @Override
    public void execute( String appVersionId ) throws Exception
    {
        // Init logger
        Backendless.Logging.setLogReportingPolicy(BackendConstants.LOG_POLICY_NUM_MSGS, BackendConstants.LOG_POLICY_FREQ_SECS);
        mLogger = Backendless.Logging.getLogger("com.mytest.events.MerchantPasswdResetTimer");
        mBackendOps = new BackendOps(mLogger);

        mLogger.debug("In MerchantPasswdResetTimer execute");

        // Fetch all 'pending' merchant password reset operations
        ArrayList<MerchantOps> ops = mBackendOps.fetchMerchantOps(buildWhereClause());
        if(ops!=null) {
            mLogger.info("Fetched password reset ops: "+ops.size());
            // first lock all to be processed objects
            // this is to avoid any chances of clash with the next run of this timer
            ArrayList<MerchantOps> lockedOps = new ArrayList<>();
            for (int i = 0; i < ops.size(); i++) {
                ops.get(i).setOp_status(DbConstants.MERCHANT_OP_STATUS_LOCKED);
                if(mBackendOps.saveMerchantOp(ops.get(i))!=null) {
                    mLogger.debug("Locked passwd reset op for: "+ops.get(i).getMerchant_id());
                    lockedOps.add(ops.get(i));
                }
            }

            mLogger.info("Locked password reset ops: "+lockedOps.size());
            // process locked objects
            for (int k = 0; k < ops.size(); k++) {
                String error = handlePasswdReset(lockedOps.get(k));
                if( error != null) {
                    mLogger.error("Failed to process merchant reset password operation: "+lockedOps.get(k).getMerchant_id()+", "+error);
                } else {
                    mLogger.debug("Processed passwd reset op for: "+lockedOps.get(k).getMerchant_id());
                }
            }
        }

        Backendless.Logging.flush();
    }

    private String handlePasswdReset(MerchantOps op) {
        // fetch user with the given id with related merchant object
        BackendlessUser user = mBackendOps.fetchUser(op.getMerchant_id(), DbConstants.USER_TYPE_MERCHANT);
        if(user==null) {
            return mBackendOps.mLastOpStatus;
        }
        Merchants merchant = (Merchants) user.getProperty("merchant");

        // check admin status
        String status = CommonUtils.checkMerchantStatus(merchant.getAdmin_status());
        if(status != null) {
            mLogger.info("Merchant is disabled: "+merchant.getAuto_id()+", "+merchant.getAdmin_status());
            return status;
        }

        // generate password
        String passwd = CommonUtils.generateMerchantPassword();
        mLogger.debug("Merchant Password: "+passwd);

        // update user account for the password
        user.setPassword(passwd);
        //merchant.setTemp_pswd_time(new Date());
        if(merchant.getPasswd_wrong_attempts() > 0) {
            mLogger.debug("Resetting password wrong attempts to 0: "+merchant.getAuto_id());
            merchant.setPasswd_wrong_attempts(0);
            user.setProperty("merchant",merchant);
        }

        user = mBackendOps.updateUser(user);
        if(user==null) {
            return mBackendOps.mLastOpStatus;
        }
        mLogger.debug("Updated merchant for password reset: "+merchant.getAuto_id());

        // Send SMS through HTTP
        String smsText = buildPwdResetSMS(op.getMerchant_id(), passwd);
        if( !SmsHelper.sendSMS(smsText, merchant.getMobile_num()) )
        {
            return BackendResponseCodes.BL_MYERROR_SEND_SMS_FAILED;
            // dont care about return code - if failed, user can always do 'forget password' again
        }
        mLogger.debug("Sent password reset SMS: "+merchant.getAuto_id());

        // Change merchant op status
        op.setOp_status(DbConstants.MERCHANT_OP_STATUS_COMPLETE);
        mBackendOps.saveMerchantOp(op);

        return null;
    }

    private String buildWhereClause() {
        StringBuilder whereClause = new StringBuilder();

        // for particular merchant
        whereClause.append("op_code = '").append(DbConstants.MERCHANT_OP_RESET_PASSWD).append("'");
        whereClause.append(" AND op_status = '").append(DbConstants.MERCHANT_OP_STATUS_PENDING).append("'");
        // older than configured cool off period
        long time = (new Date().getTime()) - (GlobalSettingsConstants.MERCHANT_PASSWORD_RESET_COOL_OFF_MINS * 60 * 1000);
        whereClause.append(" AND created < ").append(time);

        mLogger.debug("where clause: "+whereClause.toString());
        return whereClause.toString();
    }

    private String buildPwdResetSMS(String userId, String password) {
        return String.format(SmsConstants.SMS_TEMPLATE_PASSWD,CommonUtils.getHalfVisibleId(userId),password);
    }
}
