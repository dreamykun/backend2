package com.mytest.events.persistence_service;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.HeadersManager;
import com.backendless.geo.GeoPoint;
import com.backendless.logging.Logger;
import com.backendless.persistence.BackendlessDataQuery;
import com.backendless.property.ObjectProperty;
import com.backendless.servercode.ExecutionResult;
import com.backendless.servercode.InvocationContext;
import com.backendless.servercode.RunnerContext;
import com.backendless.servercode.annotation.Asset;
import com.backendless.servercode.annotation.Async;
import com.mytest.constants.BackendConstants;
import com.mytest.constants.BackendResponseCodes;
import com.mytest.constants.DbConstants;
import com.mytest.database.AllOtp;
import com.mytest.database.Customers;
import com.mytest.database.MerchantOps;
import com.mytest.database.Merchants;
import com.mytest.utilities.BackendOps;
import com.mytest.utilities.CommonUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * MerchantOpsTableEventHandler handles events for all entities. This is accomplished
 * with the @Asset( "MerchantOps" ) annotation.
 * The methods in the class correspond to the events selected in Backendless
 * Console.
 */

@Asset( "MerchantOps" )
public class MerchantOpsTableEventHandler extends com.backendless.servercode.extension.PersistenceExtender<MerchantOps>
{
    private Logger mLogger;
    private BackendOps mBackendOps;

    @Override
    public void beforeCreate( RunnerContext context, MerchantOps merchantops) throws Exception
    {
        initCommon();
        mLogger.debug("In MerchantOpsTableEventHandler: beforeCreate");

        // this will ensure that backend operations are executed, as logged-in user who called this api using generated SDK
        //HeadersManager.getInstance().addHeader( HeadersManager.HeadersEnum.USER_TOKEN_KEY, InvocationContext.getUserToken() );

        String otp = merchantops.getOtp();
        if(otp==null || otp.isEmpty()) {
            // First run, generate OTP if all fine

            // Fetch merchant
            String userid = merchantops.getMerchant_id();
            Merchants merchant = mBackendOps.getMerchant(userid, false);
            if(merchant==null) {
                CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
            }

            // check if merchant is enabled
            String status = CommonUtils.checkMerchantStatus(merchant);
            if( status != null) {
                CommonUtils.throwException(mLogger,status, "Merchant account is not active", false);
            }

            // Validate based on given current number
            String oldMobile = merchantops.getMobile_num();
            String newMobile = merchantops.getExtra_op_params();
            if(!merchant.getMobile_num().equals(oldMobile)) {
                CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_VERIFICATION_FAILED, "Wrong old mobile number", false);
            }

            // Generate OTP and send SMS
            AllOtp newOtp = new AllOtp();
            newOtp.setUser_id(userid);
            newOtp.setMobile_num(newMobile);
            newOtp.setOpcode(merchantops.getOp_code());
            newOtp = mBackendOps.generateOtp(newOtp);
            if(newOtp == null) {
                // failed to generate otp
                CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_OTP_GENERATE_FAILED, "OTP generate failed", false);
            }

            // OTP generated successfully - return exception to indicate so
            CommonUtils.throwException(mLogger,BackendResponseCodes.BE_RESPONSE_OTP_GENERATED, "OTP generated successfully", true);

        } else {
            // Second run, as OTP available
            AllOtp fetchedOtp = mBackendOps.fetchOtp(merchantops.getMerchant_id());
            if( fetchedOtp == null ||
                    !mBackendOps.validateOtp(fetchedOtp, otp) ) {
                CommonUtils.throwException(mLogger,BackendResponseCodes.BE_ERROR_WRONG_OTP, "Wrong OTP provided: "+otp, false);
            }
            // remove PIN and OTP from the object
            merchantops.setOtp(null);
            merchantops.setOp_status(DbConstants.MERCHANT_OP_STATUS_OTP_MATCHED);

            mLogger.debug("OTP matched for given merchant operation: "+merchantops.getMerchant_id()+", "+merchantops.getOp_code());
        }

        //Backendless.Logging.flush();
    }

    @Override
    public void afterCreate( RunnerContext context, MerchantOps merchantops, ExecutionResult<MerchantOps> result ) throws Exception
    {
        initCommon();
        mLogger.debug("In MerchantOpsTableEventHandler: afterCreate");

        // this will ensure that backend operations are executed, as logged-in user who called this api using generated SDK
        //HeadersManager.getInstance().addHeader( HeadersManager.HeadersEnum.USER_TOKEN_KEY, InvocationContext.getUserToken() );

        String opcode = merchantops.getOp_code();
        switch(opcode) {
            case DbConstants.MERCHANT_OP_CHANGE_MOBILE:
                changeMerchantMobile(merchantops);
                break;
            default:
                mLogger.error("Invalid Merchant operation: "+opcode);
        }

        //Backendless.Logging.flush();
    }

    /*
     * Private helper methods
     */
    private void initCommon() {
        // Init logger and utils
        Backendless.Logging.setLogReportingPolicy(BackendConstants.LOG_POLICY_NUM_MSGS, BackendConstants.LOG_POLICY_FREQ_SECS);
        mLogger = Backendless.Logging.getLogger("com.mytest.services.CustomerOpsTableEventHandler");
        mBackendOps = new BackendOps(mLogger);
    }

    private void changeMerchantMobile(MerchantOps merchantOp) {
        // Fetch merchant
        String userid = merchantOp.getMerchant_id();
        Merchants merchant = mBackendOps.getMerchant(userid, false);
        if(merchant==null) {
            CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
        }

        // check if merchant is enabled
        String status = CommonUtils.checkMerchantStatus(merchant);
        if( status != null) {
            CommonUtils.throwException(mLogger,status, "Merchant account is not active", false);
        }

        // Update with new mobile number
        merchant.setMobile_num(merchantOp.getExtra_op_params());
        merchant = mBackendOps.updateMerchant(merchant);
        if(merchant == null) {
            CommonUtils.throwException(mLogger,mBackendOps.mLastOpStatus, mBackendOps.mLastOpErrorMsg, false);
        }
    }
}
