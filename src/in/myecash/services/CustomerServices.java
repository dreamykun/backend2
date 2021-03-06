
package in.myecash.services;

import com.backendless.exceptions.BackendlessException;
import com.backendless.servercode.IBackendlessService;
import com.backendless.servercode.InvocationContext;
import in.myecash.common.MyMerchant;
import in.myecash.utilities.BackendOps;
import in.myecash.utilities.BackendUtils;
import in.myecash.utilities.MyLogger;

import java.util.ArrayList;
import java.util.List;

import in.myecash.common.database.*;
import in.myecash.common.constants.*;
import in.myecash.constants.*;


/**
 * Created by adgangwa on 25-09-2016.
 */
public class CustomerServices implements IBackendlessService {

    private MyLogger mLogger = new MyLogger("services.CustomerServices");
    private String[] mEdr = new String[BackendConstants.BACKEND_EDR_MAX_FIELDS];;

    /*
     * Public methods: Backend REST APIs
     * Customer operations
     */
    public List<Cashback> getCashbacks(String custPrivateId, long updatedSince) {

        BackendUtils.initAll();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "getCashbacks";

        boolean positiveException = false;
        try {
            mLogger.debug("In getCashbacks");

            // Fetch merchant - send userType param as null to avoid checking within fetchCurrentUser fx.
            // But check immediately after
            Object userObj = BackendUtils.fetchCurrentUser(InvocationContext.getUserId(),
                    null, mEdr, mLogger, false);
            int userType = Integer.parseInt(mEdr[BackendConstants.EDR_USER_TYPE_IDX]);

            //boolean callByCC = false;
            Customers customer = null;
            if(userType==DbConstants.USER_TYPE_CUSTOMER) {
                customer = (Customers) userObj;
                custPrivateId = customer.getPrivate_id();
                mEdr[BackendConstants.EDR_CUST_ID_IDX] = custPrivateId;

            } else if(userType==DbConstants.USER_TYPE_CC) {
                // fetch merchant
                customer = BackendOps.getCustomer(custPrivateId, BackendConstants.ID_TYPE_AUTO, false);
                //callByCC = true;
            } else {
                throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "Operation not allowed to this user");
            }

            // not checking for customer account status

            List<Cashback> cbs= null;
            String[] csvFields = customer.getCashback_table().split(CommonConstants.CSV_DELIMETER);

            // fetch cashback records from each table
            for(int i=0; i<csvFields.length; i++) {

                // fetch all CB records for this customer in this table
                String whereClause = "cust_private_id = '" + custPrivateId + "' AND updated > "+updatedSince;
                mLogger.debug("whereClause: "+whereClause);

                ArrayList<Cashback> data = BackendOps.fetchCashback(whereClause,csvFields[i], false, true);
                if (data != null) {
                    if(cbs==null) {
                        cbs= new ArrayList<>();
                    }
                    // dont want to send complete merchant objects
                    // convert the required info into a CSV string
                    // and send as other_details column of the cashback object
                    for (Cashback cb :
                            data) {
                        cb.setOther_details(MyMerchant.toCsvString(cb.getMerchant()));
                    }

                    // add all fetched records from this table to final set
                    cbs.addAll(data);
                }
            }

            if(cbs==null || cbs.size()==0) {
                positiveException = true;
                throw new BackendlessException(String.valueOf(ErrorCodes.BL_ERROR_NO_DATA_FOUND), "");
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;
            return cbs;

        } catch(Exception e) {
            BackendUtils.handleException(e,positiveException,mLogger,mEdr);
            throw e;
        } finally {
            BackendUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }

    /*
    public Customers changeMobile(String verifyParam, String newMobile, String otp) {

        CommonUtils.initAll();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "changeMobile";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = verifyParam+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                newMobile+BackendConstants.BACKEND_EDR_SUB_DELIMETER+
                otp;

        boolean positiveException = false;

        try {
            mLogger.debug("In changeMobile: " + verifyParam + "," + newMobile);

            // Fetch merchant with all child - as the same instance is to be returned too
            Customers customer = (Customers) CommonUtils.fetchCurrentUser(InvocationContext.getUserId(),
                    DbConstants.USER_TYPE_CUSTOMER, mEdr, mLogger, true);
            String oldMobile = customer.getMobile_num();

            if (otp == null || otp.isEmpty()) {
                // First run, generate OTP if all fine

                // Validate based on given current number
                if (!customer.getTxn_pin().equals(verifyParam)) {
                    throw new BackendlessException(String.valueOf(ErrorCodes.VERIFICATION_FAILED), "");
                }

                // Generate OTP to verify new mobile number
                AllOtp newOtp = new AllOtp();
                newOtp.setUser_id(customer.getPrivate_id());
                newOtp.setMobile_num(newMobile);
                newOtp.setOpcode(DbConstants.OP_CHANGE_MOBILE);
                BackendOps.generateOtp(newOtp,mEdr,mLogger);

                // OTP generated successfully - return exception to indicate so
                positiveException = true;
                throw new BackendlessException(String.valueOf(ErrorCodes.OTP_GENERATED), "");

            } else {
                // Second run, as OTP available
                BackendOps.validateOtp(customer.getPrivate_id(), DbConstants.OP_CHANGE_MOBILE, otp);
                mLogger.debug("OTP matched for given customer operation: " + customer.getPrivate_id());

                // first add record in merchant ops table
                CustomerOps customerOp = new CustomerOps();
                customerOp.setPrivateId(customer.getPrivate_id());
                customerOp.setOp_code(DbConstants.OP_CHANGE_MOBILE);
                customerOp.setMobile_num(oldMobile);
                customerOp.setInitiatedBy(DbConstantsBackend.USER_OP_INITBY_CUSTOMER);
                customerOp.setInitiatedVia(DbConstantsBackend.USER_OP_INITVIA_APP);
                customerOp.setOp_status(DbConstantsBackend.USER_OP_STATUS_COMPLETE);
                // set extra params in presentable format
                String extraParams = "Old Mobile: "+oldMobile+", New Mobile: "+newMobile;
                customerOp.setExtra_op_params(extraParams);
                BackendOps.saveCustomerOp(customerOp);

                // Update with new mobile number
                try {
                    customer.setMobile_num(newMobile);
                    customer = BackendOps.updateCustomer(customer);
                } catch(Exception e) {
                    mLogger.error("changeMobile: Exception while updating customer mobile: "+customer.getPrivate_id());
                    // Rollback - delete customer op added
                    try {
                        BackendOps.deleteCustomerOp(customerOp);
                    } catch(Exception ex) {
                        mLogger.fatal("changeMobile: Failed to rollback: customer op deletion failed: "+customer.getPrivate_id());
                        // Rollback also failed
                        mEdr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_MANUAL_CHECK;
                        throw ex;
                    }
                    throw e;
                }

                mLogger.debug("Processed mobile change for: " + customer.getPrivate_id());

                // Send SMS on old and new mobile - ignore sent status
                String smsText = SmsHelper.buildMobileChangeSMS(oldMobile, newMobile);
                if(SmsHelper.sendSMS(smsText, oldMobile + "," + newMobile, mLogger)) {
                    mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_OK;
                } else {
                    mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_NOK;
                }
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;
            return customer;

        } catch(Exception e) {
            CommonUtils.handleException(e,positiveException,mLogger,mEdr);
            throw e;
        } finally {
            CommonUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }*/

    /*
    public void changePin(String oldPin, String newPin, String cardNum) {
        CommonUtils.initAll();
        long startTime = System.currentTimeMillis();
        mEdr[BackendConstants.EDR_START_TIME_IDX] = String.valueOf(startTime);
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "changeMobile";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = cardNum;

        boolean positiveException = false;

        try {
            // Fetch customer
            Customers customer = (Customers) CommonUtils.fetchCurrentUser(InvocationContext.getUserId(),
                    DbConstants.USER_TYPE_CUSTOMER, mEdr, mLogger, false);
            String mobileNum = customer.getMobile_num();

            if(oldPin==null || oldPin.isEmpty()) {
                // PIN reset scenario

                if(cardNum==null || cardNum.isEmpty()) {
                    throw new BackendlessException(String.valueOf(ErrorCodes.WRONG_INPUT_DATA),"All input params not available");
                }

                // check if any request already pending
                if( BackendOps.fetchMerchantOps(custPinResetWhereClause(mobileNum)) != null) {
                    throw new BackendlessException(String.valueOf(ErrorCodes.DUPLICATE_ENTRY), "");
                }

                // check for 'extra verification'
                String cardId = customer.getCardId();
                if (cardId == null || !cardId.equalsIgnoreCase(cardNum)) {
                    CommonUtils.handleWrongAttempt(mobileNum, customer, DbConstants.USER_TYPE_CUSTOMER, DbConstants.OP_CHANGE_PIN
                            DbConstantsBackend.WRONG_PARAM_TYPE_VERIFICATION, mLogger);
                    throw new BackendlessException(String.valueOf(ErrorCodes.VERIFICATION_FAILED), "");
                }

                // Add to customer op table for later processing
                // create row in CustomerOps table
                CustomerOps op = new CustomerOps();
                op.setMobile_num(customer.getMobile_num());
                op.setPrivateId(customer.getPrivate_id());
                op.setOp_code(DbConstants.OP_CHANGE_PIN);
                op.setOp_status(DbConstantsBackend.USER_OP_STATUS_PENDING);
                op.setInitiatedBy(DbConstantsBackend.USER_OP_INITBY_CUSTOMER);
                op.setInitiatedVia(DbConstantsBackend.USER_OP_INITVIA_APP);

                BackendOps.saveCustomerOp(op);
                mLogger.debug("Processed PIN reset op for: " + customer.getPrivate_id());

                positiveException = true;
                throw new BackendlessException(String.valueOf(ErrorCodes.OP_SCHEDULED), "");

            } else {
                // PIN change scenario
                if(newPin==null || newPin.isEmpty() || newPin.length()!=CommonConstants.PIN_LEN) {
                    throw new BackendlessException(String.valueOf(ErrorCodes.WRONG_INPUT_DATA),"Invalid new PIN value");
                }

                // check for 'extra verification'
                String pin = customer.getTxn_pin();
                if (pin == null || !pin.equalsIgnoreCase(oldPin)) {
                    CommonUtils.handleWrongAttempt(mobileNum, customer, DbConstants.USER_TYPE_CUSTOMER, DbConstants.OP_CHANGE_PIN
                            DbConstantsBackend.WRONG_PARAM_TYPE_PIN, mLogger);
                    throw new BackendlessException(String.valueOf(ErrorCodes.VERIFICATION_FAILED), "");
                }

                // Change PIN
                customer.setTxn_pin(newPin);
                BackendOps.updateCustomer(customer);

                // Send SMS through HTTP
                if(mobileNum!=null) {
                    String smsText = SmsHelper.buildPwdChangeSMS(userId);
                    if( SmsHelper.sendSMS(smsText, mobileNum, mLogger) ){
                        mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_OK;
                    } else {
                        mEdr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_NOK;
                    };
                } else {
                    //TODO: raise alarm
                    mLogger.error("In changePassword: mobile number is null");
                    mEdr[BackendConstants.EDR_IGNORED_ERROR_IDX] = BackendConstants.IGNORED_ERROR_MOBILE_NUM_NA;
                }
            }

            // no exception - means function execution success
            mEdr[BackendConstants.EDR_RESULT_IDX] = BackendConstants.BACKEND_EDR_RESULT_OK;

        } catch(Exception e) {
            CommonUtils.handleException(e,positiveException,mLogger,mEdr);
            throw e;
        } finally {
            CommonUtils.finalHandling(startTime,mLogger,mEdr);
        }
    }*/

    /*
     * Private helper methods
     */
    /*
    private String custPinResetWhereClause(String customerMobileNum) {
        StringBuilder whereClause = new StringBuilder();

        whereClause.append("op_code = '").append(DbConstants.OP_RESET_PIN).append("'");
        whereClause.append(" AND op_status = '").append(DbConstantsBackend.USER_OP_STATUS_PENDING).append("'");
        whereClause.append("AND mobile_num = '").append(customerMobileNum).append("'");

        // created within last 'cool off' mins
        long time = (new Date().getTime()) - (MyGlobalSettings.CUSTOMER_PASSWORD_RESET_COOL_OFF_MINS * 60 * 1000);
        whereClause.append(" AND created > ").append(time);
        return whereClause.toString();
    }*/


}
