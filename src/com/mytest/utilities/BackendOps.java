package com.mytest.utilities;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.BackendlessUser;
import com.backendless.exceptions.BackendlessException;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.logging.Logger;
import com.backendless.persistence.BackendlessDataQuery;
import com.backendless.persistence.QueryOptions;
import com.mytest.constants.*;
import com.mytest.database.*;
import com.mytest.messaging.SmsConstants;
import com.mytest.messaging.SmsHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Created by adgangwa on 15-05-2016.
 */
public class BackendOps {

    //public String mLastOpStatus;
    //public String mLastOpErrorMsg;
    //private Logger mLogger;
    /*
    public BackendOps(Logger logger) {
        mLogger = logger;
    }*/

    /*
     * BackendlessUser operations
     */
    public static BackendlessUser registerUser(BackendlessUser user) {
        return Backendless.UserService.register(user);
    }

    public static void assignRole(String userId, String role) {
        Backendless.UserService.assignRole(userId, role);
    }

    public static BackendlessUser loginUser(String userId, String password) {
        return Backendless.UserService.login(userId, password, false);
    }

    public static void logoutUser() {
        Backendless.UserService.logout();
    }

    public static BackendlessUser updateUser(BackendlessUser user) {
        return Backendless.UserService.update( user );
    }

    public static BackendlessUser fetchUser(String userid, int userType) {
        BackendlessDataQuery query = new BackendlessDataQuery();
        query.setWhereClause("user_id = '"+userid+"'");

        QueryOptions queryOptions = new QueryOptions();
        if(userType == DbConstants.USER_TYPE_CUSTOMER) {
            queryOptions.addRelated( "customer");
            queryOptions.addRelated( "customer.membership_card");

        } else if(userType == DbConstants.USER_TYPE_MERCHANT) {
            queryOptions.addRelated( "merchant");
            queryOptions.addRelated("merchant.trusted_devices");
        } else if(userType == DbConstants.USER_TYPE_AGENT) {
            queryOptions.addRelated( "agent");
        }

        query.setQueryOptions( queryOptions );
        BackendlessCollection<BackendlessUser> user = Backendless.Data.of( BackendlessUser.class ).find(query);
        if( user.getTotalObjects() == 0) {
            String errorMsg = "No user found: "+userid;
            BackendlessFault fault = new BackendlessFault(BackendResponseCodes.BE_ERROR_NO_SUCH_USER,errorMsg);
            throw new BackendlessException(fault);
        } else {
            return user.getData().get(0);
        }
    }



    public static BackendlessUser fetchUserByObjectId(String objectId, int userType) {
        //BackendlessUser user = Backendless.UserService.findById(objectId);
        ArrayList<String> relationProps = new ArrayList<>();
        if(userType == DbConstants.USER_TYPE_MERCHANT) {
            relationProps.add("merchant");
        }
        BackendlessUser user = Backendless.Data.of(BackendlessUser.class).findById(objectId, relationProps);

        //TODO: remove below
        Merchants merchant = (Merchants) user.getProperty("merchant");
        if (merchant == null) {
            //mLogger.error("Merchant object in null");
            String errorMsg = "Merchant object in null";
            BackendlessFault fault = new BackendlessFault(BackendResponseCodes.BE_ERROR_NO_SUCH_USER,errorMsg);
            throw new BackendlessException(fault);
        }
        return user;

        /*
        if(userType == DbConstants.USER_TYPE_MERCHANT) {
            Merchants merchant = (Merchants) user.getProperty("merchant");
            if (merchant == null) {
                // load merchant object
                ArrayList<String> relationProps = new ArrayList<>();
                relationProps.add("merchant");
                Backendless.Data.of(BackendlessUser.class).loadRelations(user, relationProps);

                return user;
            }
        }*/
    }

    /*
     * Merchant operations
     */
    public static void loadMerchant(BackendlessUser user) {
        ArrayList<String> relationProps = new ArrayList<>();
        relationProps.add("merchant");
        //relationProps.add("merchant.trusted_devices");
        Backendless.Data.of( BackendlessUser.class ).loadRelations(user, relationProps);
    }

    public static Merchants getMerchant(String userId, boolean fetchTrustedDevices) {
        BackendlessDataQuery query = new BackendlessDataQuery();
        query.setWhereClause("auto_id = '"+userId+"'");

        if(fetchTrustedDevices) {
            QueryOptions queryOptions = new QueryOptions();
            queryOptions.addRelated("trusted_devices");
            query.setQueryOptions(queryOptions);
        }

        BackendlessCollection<Merchants> user = Backendless.Data.of( Merchants.class ).find(query);
        if( user.getTotalObjects() == 0) {
            String errorMsg = "No Merchant found: "+userId;
            BackendlessFault fault = new BackendlessFault(BackendResponseCodes.BE_ERROR_NO_SUCH_USER,errorMsg);
            throw new BackendlessException(fault);
        } else {
            return user.getData().get(0);
        }
    }

    public static Merchants getMerchantByMobile(String mobileNum) {
        BackendlessDataQuery query = new BackendlessDataQuery();
        query.setWhereClause("mobile_num = '"+mobileNum+"'");

        BackendlessCollection<Merchants> user = Backendless.Data.of( Merchants.class ).find(query);
        if( user.getTotalObjects() == 0) {
            String errorMsg = "No Merchant found: "+mobileNum;
            BackendlessFault fault = new BackendlessFault(BackendResponseCodes.BE_ERROR_NO_SUCH_USER,errorMsg);
            throw new BackendlessException(fault);
        } else {
            return user.getData().get(0);
        }
    }

    public static ArrayList<Merchants> fetchMerchants(String whereClause) {
        BackendlessDataQuery query = new BackendlessDataQuery();
        // fetch all merchants, not yet archived
        query.setPageSize(CommonConstants.dbQueryMaxPageSize);
        query.setWhereClause(whereClause);

        BackendlessCollection<Merchants> users = Backendless.Data.of( Merchants.class ).find(query);
        int cnt = users.getTotalObjects();
        if( cnt == 0) {
            // No unprocessed merchant left with this prefix
            String errorMsg = "No Merchant found";
            BackendlessFault fault = new BackendlessFault(BackendResponseCodes.BE_ERROR_NO_SUCH_USER,errorMsg);
            throw new BackendlessException(fault);
        } else {
            ArrayList<Merchants> objects = new ArrayList<>();
            while (users.getCurrentPage().size() > 0)
            {
                int size  = users.getCurrentPage().size();
                System.out.println( "Loaded " + size + " merchants in the current page" );

                Iterator<Merchants> iterator = users.getCurrentPage().iterator();
                while( iterator.hasNext() )
                {
                    objects.add(iterator.next());
                }
                users = users.nextPage();
            }
            return objects;
        }
    }

    public static Merchants updateMerchant(Merchants merchant) {
        return Backendless.Persistence.save(merchant);
    }

    /*
     * Customer operations
     */
    public static Customers getCustomer(String custId, int idType) {
        BackendlessDataQuery query = new BackendlessDataQuery();
        switch(idType) {
            case BackendConstants.CUSTOMER_ID_MOBILE:
                query.setWhereClause("mobile_num = '"+custId+"'");
                break;
            case BackendConstants.CUSTOMER_ID_CARD:
                query.setWhereClause("cardId = '"+custId+"'");
                break;
            case BackendConstants.CUSTOMER_ID_PRIVATE_ID:
                query.setWhereClause("private_id = '"+custId+"'");
                break;
        }

        QueryOptions queryOptions = new QueryOptions();
        queryOptions.addRelated("membership_card");
        query.setQueryOptions(queryOptions);

        BackendlessCollection<Customers> user = Backendless.Data.of( Customers.class ).find(query);
        if( user.getTotalObjects() == 0) {
            String errorMsg = "No user found: "+custId;
            BackendlessFault fault = new BackendlessFault(BackendResponseCodes.BE_ERROR_NO_SUCH_USER,errorMsg);
            throw new BackendlessException(fault);
        } else {
            if(user.getData().get(0).getMembership_card()==null) {
                String errorMsg = "No customer card found: "+custId;
                BackendlessFault fault = new BackendlessFault(BackendResponseCodes.BE_ERROR_NO_SUCH_CARD,errorMsg);
                throw new BackendlessException(fault);
            }
            return user.getData().get(0);
        }
    }

    public static Customers updateCustomer(Customers customer) {
        return Backendless.Persistence.save(customer);
    }

    /*
     * Customer card operations
     */
    public static CustomerCards getCustomerCard(String cardId) {
        BackendlessDataQuery dataQuery = new BackendlessDataQuery();
        dataQuery.setWhereClause("card_id = '" + cardId + "'");

        BackendlessCollection<CustomerCards> collection = Backendless.Data.of(CustomerCards.class).find(dataQuery);
        if( collection.getTotalObjects() == 0) {
            String errorMsg = "No membership card found: "+cardId;
            BackendlessFault fault = new BackendlessFault(BackendResponseCodes.BE_ERROR_NO_SUCH_CARD,errorMsg);
            throw new BackendlessException(fault);
        } else {
            return collection.getData().get(0);
        }
    }

    public static CustomerCards saveCustomerCard(CustomerCards card) {
        return Backendless.Persistence.save( card );
    }

    /*
     * Cashback operations
     */
    public static ArrayList<Cashback> fetchCashback(String whereClause, String cashbackTable) {
        Backendless.Data.mapTableToClass(cashbackTable, Cashback.class);

        BackendlessDataQuery dataQuery = new BackendlessDataQuery();
        dataQuery.setPageSize( CommonConstants.dbQueryMaxPageSize );
        dataQuery.setWhereClause(whereClause);

        BackendlessCollection<Cashback> collection = Backendless.Data.of(Cashback.class).find(dataQuery);

        int cnt = collection.getTotalObjects();
        if(cnt > 0) {
            ArrayList<Cashback> objects = new ArrayList<>();
            while (collection.getCurrentPage().size() > 0)
            {
                Iterator<Cashback> iterator = collection.getCurrentPage().iterator();
                while( iterator.hasNext() )
                {
                    objects.add(iterator.next());
                }
                collection = collection.nextPage();
            }
            return objects;
        } else {
            String errorMsg = "No cashback found: "+whereClause;
            BackendlessFault fault = new BackendlessFault(BackendResponseCodes.BL_ERROR_NO_DATA_FOUND,errorMsg);
            throw new BackendlessException(fault);
        }
    }

    public static Cashback saveCashback(Cashback cb) {
        return Backendless.Persistence.save( cb );
    }

    /*
     * OTP operations
     */
    public static AllOtp generateOtp(AllOtp otp) {
        // check if any OTP object already available for this user_id
        // if yes, update the same for new OTP, op and time.
        // If no, create new object
        // Send SMS with OTP
        AllOtp newOtp = null;
        AllOtp oldOtp = fetchOtp(otp.getUser_id());
        if(oldOtp !=  null) {
            // update oldOtp
            oldOtp.setOtp_value(CommonUtils.generateOTP());
            oldOtp.setOpcode(otp.getOpcode());
            oldOtp.setMobile_num(otp.getMobile_num());
            newOtp = Backendless.Persistence.save( oldOtp );
        } else {
            otp.setOtp_value(CommonUtils.generateOTP());
            newOtp = Backendless.Persistence.save( otp );
        }

        // Send SMS through HTTP
        String smsText = String.format(SmsConstants.SMS_OTP,
                newOtp.getOpcode(),
                CommonUtils.getHalfVisibleId(newOtp.getUser_id()),
                newOtp.getOtp_value(),
                GlobalSettingsConstants.OTP_VALID_MINS);

        if( !SmsHelper.sendSMS(smsText, newOtp.getMobile_num()) )
        {
            String errorMsg = "In generateOtp: Failed to send SMS";
            BackendlessFault fault = new BackendlessFault(BackendResponseCodes.BE_ERROR_SEND_SMS_FAILED,errorMsg);
            throw new BackendlessException(fault);
        }

        return newOtp;
    }

    public static void deleteOtp(AllOtp otp) {
        Backendless.Persistence.of( AllOtp.class ).remove( otp );
    }

    public static boolean validateOtp(AllOtp otp, String rcvdOtp) {
        Date otpTime = otp.getUpdated()==null?otp.getCreated():otp.getUpdated();
        Date currTime = new Date();

        if ( ((currTime.getTime() - otpTime.getTime()) < (GlobalSettingsConstants.OTP_VALID_MINS*60*1000)) &&
                rcvdOtp.equals(otp.getOtp_value()) ) {
            // active otp available and matched
            // delete as can be used only once
            deleteOtp(otp);
            return true;
        } else {
            return false;
        }
    }

    public static AllOtp fetchOtp(String userId) {
        BackendlessDataQuery dataQuery = new BackendlessDataQuery();
        dataQuery.setWhereClause("user_id = '" + userId + "'");

        BackendlessCollection<AllOtp> collection = Backendless.Data.of(AllOtp.class).find(dataQuery);
        if( collection.getTotalObjects() > 0) {
            return collection.getData().get(0);
        } else {
            String errorMsg = "In fetchOtp: No data found" + userId;
            BackendlessFault fault = new BackendlessFault(BackendResponseCodes.BL_ERROR_NO_DATA_FOUND,errorMsg);
            throw new BackendlessException(fault);
        }
    }

    /*
     * Counters operations
     */
    public static Double fetchCounterValue(String name) {
        BackendlessDataQuery dataQuery = new BackendlessDataQuery();
        dataQuery.setWhereClause("name = '" + name + "'");

        BackendlessCollection<Counters> collection = Backendless.Data.of(Counters.class).find(dataQuery);
        if( collection.getTotalObjects() > 0) {
            Counters counter = collection.getData().get(0);

            // increment counter - very important to do
            counter.setValue(counter.getValue()+1);
            counter = Backendless.Persistence.save( counter );
            return counter.getValue();
        } else {
            String errorMsg = "In fetchCounter: No data found" + name;
            BackendlessFault fault = new BackendlessFault(BackendResponseCodes.BL_ERROR_NO_DATA_FOUND,errorMsg);
            throw new BackendlessException(fault);
        }
    }

    /*
     * Trusted Devices operations
     */
    public static MerchantDevice fetchDevice(String deviceId) {
        BackendlessDataQuery dataQuery = new BackendlessDataQuery();
        dataQuery.setWhereClause("device_id = '" + deviceId + "'");

        BackendlessCollection<MerchantDevice> collection = Backendless.Data.of(MerchantDevice.class).find(dataQuery);
        if( collection.getTotalObjects() > 0) {
            return collection.getData().get(0);
        } else {
            String errorMsg = "In fetchDevice: No data found" + deviceId;
            BackendlessFault fault = new BackendlessFault(BackendResponseCodes.BL_ERROR_NO_DATA_FOUND,errorMsg);
            throw new BackendlessException(fault);
        }
    }

    /*
     * Merchant operations
     */
    public static MerchantOps addMerchantOp(MerchantOps op) {
        return Backendless.Persistence.save( op );
    }

    public static ArrayList<MerchantOps> fetchMerchantOps(String whereClause) {
        // fetch cashback objects from DB
        BackendlessDataQuery dataQuery = new BackendlessDataQuery();
        dataQuery.setPageSize( CommonConstants.dbQueryMaxPageSize );

        // TODO: check if putting index on cust_private_id improves performance
        // or using rowid_qr in where clause improves performance
        dataQuery.setWhereClause(whereClause);

        BackendlessCollection<MerchantOps> collection = Backendless.Data.of(MerchantOps.class).find(dataQuery);

        int cnt = collection.getTotalObjects();
        if(cnt > 0) {
            ArrayList<MerchantOps> objects = new ArrayList<>();
            while (collection.getCurrentPage().size() > 0)
            {
                int size  = collection.getCurrentPage().size();

                Iterator<MerchantOps> iterator = collection.getCurrentPage().iterator();
                while( iterator.hasNext() )
                {
                    objects.add(iterator.next());
                }
                collection = collection.nextPage();
            }
            return objects;
        } else {
            String errorMsg = "No merchantop object found: "+whereClause;
            BackendlessFault fault = new BackendlessFault(BackendResponseCodes.BL_ERROR_NO_DATA_FOUND,errorMsg);
            throw new BackendlessException(fault);
        }
    }

    public static MerchantOps saveMerchantOp(MerchantOps op) {
        return Backendless.Persistence.save( op );
    }

    /*
     * WrongAttempts operations
     */
    // returns 'null' if not found and new created
    /*
    public static WrongAttempts fetchOrCreateWrongAttempt(String userId, String type, int userType) {
        WrongAttempts attempt = null;
        try {
            attempt = fetchWrongAttempts(userId, type);
        } catch(BackendlessException e) {
            if(!e.getCode().equals(BackendResponseCodes.BL_ERROR_NO_DATA_FOUND)) {
                throw e;
            }
        }
        if(attempt==null) {
            // create row
            WrongAttempts newAttempt = new WrongAttempts();
            newAttempt.setUser_id(userId);
            newAttempt.setAttempt_type(type);
            newAttempt.setAttempt_cnt(0);
            newAttempt.setUser_type(userType);
            return saveWrongAttempt(newAttempt);
        }
        return attempt;
    }*/

    public static WrongAttempts fetchWrongAttempts(String userId, String type) {
        BackendlessDataQuery dataQuery = new BackendlessDataQuery();
        dataQuery.setWhereClause("user_id = '" + userId + "'" + "AND attempt_type = '" + type + "'");

        BackendlessCollection<WrongAttempts> collection = Backendless.Data.of(WrongAttempts.class).find(dataQuery);
        if( collection.getTotalObjects() > 0) {
            return collection.getData().get(0);
        } else {
            String errorMsg = "No WrongAttempts object found: "+userId+type;
            BackendlessFault fault = new BackendlessFault(BackendResponseCodes.BL_ERROR_NO_DATA_FOUND,errorMsg);
            throw new BackendlessException(fault);
        }
    }

    public static WrongAttempts saveWrongAttempt(WrongAttempts attempt) {
        return Backendless.Persistence.save( attempt );
    }

    /*
     * MerchantStats operations
     */
    public static MerchantStats fetchMerchantStats(String merchantId) {
        BackendlessDataQuery dataQuery = new BackendlessDataQuery();
        dataQuery.setPageSize(CommonConstants.dbQueryMaxPageSize);
        dataQuery.setWhereClause("merchant_id = '" + merchantId + "'");

        BackendlessCollection<MerchantStats> collection = Backendless.Data.of(MerchantStats.class).find(dataQuery);
        if( collection.getTotalObjects() > 0) {
            return collection.getData().get(0);
        } else {
            String errorMsg = "No MerchantStats object found: "+merchantId;
            BackendlessFault fault = new BackendlessFault(BackendResponseCodes.BL_ERROR_NO_DATA_FOUND,errorMsg);
            throw new BackendlessException(fault);
        }
    }

    public static MerchantStats saveMerchantStats(MerchantStats stats) {
        return Backendless.Persistence.save( stats );
    }

    /*
     * Transaction operations
     */
    public static List<Transaction> fetchTransactions(String whereClause) {
        // fetch txns object from DB
        BackendlessDataQuery dataQuery = new BackendlessDataQuery();
        // sorted by create time
        QueryOptions queryOptions = new QueryOptions("create_time");
        dataQuery.setQueryOptions(queryOptions);
        dataQuery.setPageSize(CommonConstants.dbQueryMaxPageSize);
        dataQuery.setWhereClause(whereClause);

        BackendlessCollection<Transaction> collection = Backendless.Data.of(Transaction.class).find(dataQuery);

        int size = collection.getTotalObjects();
        if(size <= 0) {
            String errorMsg = "No transactions found: "+whereClause;
            BackendlessFault fault = new BackendlessFault(BackendResponseCodes.BL_ERROR_NO_DATA_FOUND,errorMsg);
            throw new BackendlessException(fault);
        }

        List<Transaction> transactions = collection.getData();
        while(collection.getCurrentPage().size() > 0) {
            collection = collection.nextPage();
            transactions.addAll(collection.getData());
        }

        return transactions;
    }

    /*
     * Agent operations
     */
    public static Agents getAgent(String userId) {
        BackendlessDataQuery query = new BackendlessDataQuery();
        query.setWhereClause("id = '"+userId+"'");

        BackendlessCollection<Agents> user = Backendless.Data.of( Agents.class ).find(query);
        if( user.getTotalObjects() == 0) {
            // no data found
            String errorMsg = "No agent found: "+userId;
            BackendlessFault fault = new BackendlessFault(BackendResponseCodes.BE_ERROR_NO_SUCH_USER,errorMsg);
            throw new BackendlessException(fault);
        } else {
            return user.getData().get(0);
        }
    }

    public static void loadAgent(BackendlessUser user) {
        ArrayList<String> relationProps = new ArrayList<>();
        relationProps.add("agent");

        Backendless.Data.of( BackendlessUser.class ).loadRelations(user, relationProps);
    }

    public static Agents updateAgent(Agents agent) {
        return Backendless.Persistence.save(agent);
    }
}
