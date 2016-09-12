package com.myecash.utilities;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.BackendlessUser;
import com.backendless.exceptions.BackendlessException;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.persistence.BackendlessDataQuery;
import com.backendless.persistence.QueryOptions;
import com.myecash.constants.*;
import com.myecash.database.*;
import com.myecash.messaging.SmsConstants;
import com.myecash.messaging.SmsHelper;

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
        try {
            Backendless.UserService.logout();
        } catch (Exception e) {
            // ignore exception
        }
    }

    public static BackendlessUser updateUser(BackendlessUser user) {
        return Backendless.UserService.update( user );
    }

    public static BackendlessUser fetchUser(String userid, int userType) {
        BackendlessDataQuery query = new BackendlessDataQuery();
        query.setWhereClause("user_id = '"+userid+"'");

        QueryOptions queryOptions = new QueryOptions();
        switch (userType) {
            case DbConstants.USER_TYPE_CUSTOMER:
                queryOptions.addRelated( "customer");
                queryOptions.addRelated( "customer.membership_card");
                break;
            case DbConstants.USER_TYPE_MERCHANT:
                queryOptions.addRelated( "merchant");
                queryOptions.addRelated("merchant.trusted_devices");
            case DbConstants.USER_TYPE_AGENT:
            case DbConstants.USER_TYPE_CC:
            case DbConstants.USER_TYPE_CCNT:
                queryOptions.addRelated( "internalUser");
        }

        query.setQueryOptions( queryOptions );
        BackendlessCollection<BackendlessUser> user = Backendless.Data.of( BackendlessUser.class ).find(query);
        if( user.getTotalObjects() == 0) {
            String errorMsg = "No user found: "+userid;
            throw new BackendlessException(BackendResponseCodes.BE_ERROR_NO_SUCH_USER, errorMsg);
        } else {
            return user.getData().get(0);
        }
    }

    public static BackendlessUser fetchUserByObjectId(String objectId, boolean allChilds) {
        ArrayList<String> relationProps = new ArrayList<>();
        // add all childs
        relationProps.add("merchant");
        relationProps.add("customer");
        relationProps.add("internalUser");

        if(allChilds) {
            relationProps.add("merchant.trusted_devices");
            relationProps.add("merchant.address");
            relationProps.add("merchant.address.city");
            relationProps.add("merchant.buss_category");
            relationProps.add("customer.membership_card");
        }

        return Backendless.Data.of(BackendlessUser.class).findById(objectId, relationProps);
        //BackendlessUser user = Backendless.Data.of(BackendlessUser.class).findById(objectId, relationProps);

        //TODO: remove below
        /*
        Merchants merchant = (Merchants) user.getProperty("merchant");
        if (merchant == null) {
            //mLogger.error("Merchant object in null");
            String errorMsg = "Merchant object in null";
            throw new BackendlessException(BackendResponseCodes.BE_ERROR_NO_SUCH_USER, errorMsg);
        }
        return user;*/

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
        Backendless.Data.of( BackendlessUser.class ).loadRelations(user, relationProps);
    }

    public static Merchants getMerchant(String userId, boolean onlyTrustedDevicesChild, boolean allChild) {
        BackendlessDataQuery query = new BackendlessDataQuery();
        query.setWhereClause("auto_id = '"+userId+"'");

        if(allChild) {
            QueryOptions queryOptions = new QueryOptions();
            queryOptions.addRelated("trusted_devices");
            queryOptions.addRelated("merchant.address");
            queryOptions.addRelated("merchant.address.city");
            queryOptions.addRelated("merchant.buss_category");
            query.setQueryOptions(queryOptions);

        } else if(onlyTrustedDevicesChild) {
            QueryOptions queryOptions = new QueryOptions();
            queryOptions.addRelated("trusted_devices");
            query.setQueryOptions(queryOptions);
        }

        BackendlessCollection<Merchants> user = Backendless.Data.of( Merchants.class ).find(query);
        if( user.getTotalObjects() == 0) {
            String errorMsg = "No Merchant found: "+userId;
            throw new BackendlessException(BackendResponseCodes.BE_ERROR_NO_SUCH_USER, errorMsg);
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
            throw new BackendlessException(BackendResponseCodes.BE_ERROR_NO_SUCH_USER, errorMsg);
        } else {
            return user.getData().get(0);
        }
    }

    public static ArrayList<Merchants> fetchMerchants(String whereClause) {
        BackendlessDataQuery query = new BackendlessDataQuery();
        query.setPageSize(CommonConstants.dbQueryMaxPageSize);
        query.setWhereClause(whereClause);

        BackendlessCollection<Merchants> users = Backendless.Data.of( Merchants.class ).find(query);
        int cnt = users.getTotalObjects();
        if( cnt == 0) {
            // No matching merchant is not an error
            return null;
        } else {
            ArrayList<Merchants> objects = new ArrayList<>();
            while (users.getCurrentPage().size() > 0)
            {
                //int size  = users.getCurrentPage().size();
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

    public static int getMerchantCnt(String whereClause) {
        BackendlessDataQuery query = new BackendlessDataQuery();
        query.setWhereClause(whereClause);

        BackendlessCollection<Merchants> users = Backendless.Data.of( Merchants.class ).find(query);
        return users.getTotalObjects();
    }

    public static Merchants updateMerchant(Merchants merchant) {
        return Backendless.Persistence.save(merchant);
    }

    /*
     * Customer operations
     */
    public static Customers getCustomer(String custId, int idType, boolean fetchCard) {
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

        if(fetchCard) {
            QueryOptions queryOptions = new QueryOptions();
            queryOptions.addRelated("membership_card");
            query.setQueryOptions(queryOptions);
        }

        BackendlessCollection<Customers> user = Backendless.Data.of( Customers.class ).find(query);
        if( user.getTotalObjects() == 0) {
            // No customer found is not an error
            return null;
        } else {
            if(fetchCard && user.getData().get(0).getMembership_card()==null) {
                String errorMsg = "No customer card set for user: "+custId;
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_NO_SUCH_CARD, errorMsg);
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
            throw new BackendlessException(BackendResponseCodes.BE_ERROR_NO_SUCH_CARD, errorMsg);
        } else {
            return collection.getData().get(0);
        }
    }

    public static CustomerCards saveCustomerCard(CustomerCards card) {
        return Backendless.Persistence.save( card );
    }

    public static int getCardCnt(String whereClause) {
        BackendlessDataQuery query = new BackendlessDataQuery();
        query.setWhereClause(whereClause);

        BackendlessCollection<CustomerCards> users = Backendless.Data.of( CustomerCards.class ).find(query);
        return users.getTotalObjects();
    }

    /*
     * Cashback operations
     */
    public static ArrayList<Cashback> fetchCashback(String whereClause, String cashbackTable,
                                                    boolean customerData, boolean mchntData) {

        Backendless.Data.mapTableToClass(cashbackTable, Cashback.class);

        BackendlessDataQuery dataQuery = new BackendlessDataQuery();
        // TODO: change to max size in production
        dataQuery.setPageSize(1);
        //dataQuery.setPageSize( CommonConstants.dbQueryMaxPageSize );
        dataQuery.setWhereClause(whereClause);

        QueryOptions queryOptions = new QueryOptions();
        if(customerData) {
            queryOptions.addRelated("customer");
            queryOptions.addRelated("customer.membership_card");
        }
        if(mchntData) {
            queryOptions.addRelated("merchant");
        }
        dataQuery.setQueryOptions(queryOptions);

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
            // no object found is not an error
            return null;
        }
    }

    public static Cashback saveCashback(Cashback cb, String tableName) {
        Backendless.Data.mapTableToClass(tableName, Cashback.class);
        return Backendless.Persistence.save( cb );
    }

    /*
     * OTP operations
     */
    public static void generateOtp(AllOtp otp, String[] edr, MyLogger logger) {
        // check if any OTP object already available for this user_id
        // If yes, first delete the same.
        // Create new OTP object
        // Send SMS with OTP
        try {
            AllOtp newOtp = null;
            AllOtp oldOtp = fetchOtp(otp.getUser_id());
            if (oldOtp != null) {
                // delete oldOtp
                deleteOtp(oldOtp);
                /*
                oldOtp.setOtp_value(CommonUtils.generateOTP());
                oldOtp.setOpcode(otp.getOpcode());
                oldOtp.setMobile_num(otp.getMobile_num());
                newOtp = Backendless.Persistence.save(oldOtp);
                */
            }
            // create new OTP object
            otp.setOtp_value(CommonUtils.generateOTP());
            newOtp = Backendless.Persistence.save(otp);

            // Send SMS through HTTP
            String smsText = String.format(SmsConstants.SMS_OTP,
                    newOtp.getOpcode(),
                    CommonUtils.getHalfVisibleId(newOtp.getUser_id()),
                    newOtp.getOtp_value(),
                    GlobalSettingsConstants.OTP_VALID_MINS);

            if (SmsHelper.sendSMS(smsText, newOtp.getMobile_num(), logger)){
                edr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_OK;
            } else {
                edr[BackendConstants.EDR_SMS_STATUS_IDX] = BackendConstants.BACKEND_EDR_SMS_NOK;
                String errorMsg = "In generateOtp: Failed to send SMS";
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_SEND_SMS_FAILED, errorMsg);
            }
        } catch (Exception e) {
            String errorMsg = "Exception in generateOtp: "+e.toString();
            throw new BackendlessException(BackendResponseCodes.BE_ERROR_OTP_GENERATE_FAILED, errorMsg);
        }
    }

    public static void validateOtp(String userId, String rcvdOtp) {
        AllOtp otp = null;
        try {
            otp = BackendOps.fetchOtp(userId);
        } catch(BackendlessException e) {
            if(e.getCode().equals(BackendResponseCodes.BL_ERROR_NO_DATA_FOUND)) {
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_WRONG_OTP, "");
            }
            throw e;
        }

        Date otpTime = otp.getUpdated()==null?otp.getCreated():otp.getUpdated();
        Date currTime = new Date();

        if ( ((currTime.getTime() - otpTime.getTime()) < (GlobalSettingsConstants.OTP_VALID_MINS*60*1000)) &&
                rcvdOtp.equals(otp.getOtp_value()) ) {
            // active otp available and matched
            // delete as can be used only once
            try {
                deleteOtp(otp);
            } catch(Exception e) {
                // error in delete is not considered as OTP not matching - ignore
                // TODO: raise alarm
            }
        } else {
            throw new BackendlessException(BackendResponseCodes.BE_ERROR_WRONG_OTP, "");
        }
    }

    private static void deleteOtp(AllOtp otp) {
        Backendless.Persistence.of( AllOtp.class ).remove( otp );
    }

    private static AllOtp fetchOtp(String userId) {
        BackendlessDataQuery dataQuery = new BackendlessDataQuery();
        dataQuery.setWhereClause("user_id = '" + userId + "'");

        BackendlessCollection<AllOtp> collection = Backendless.Data.of(AllOtp.class).find(dataQuery);
        if( collection.getTotalObjects() > 0) {
            return collection.getData().get(0);
        } else {
            return null;
        }
    }

    /*
     * Counters operations
     */
    /*
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
    }*/

    public static Long fetchCounterValue(String name) {
        return Backendless.Counters.incrementAndGet(name);
    }
    public static Long decrementCounterValue(String name) {
        return Backendless.Counters.decrementAndGet(name);
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
     * Merchant operations ops
     */
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
            // no object is not an error
            return null;
        }
    }

    public static MerchantOps saveMerchantOp(MerchantOps op) {
        return Backendless.Persistence.save( op );
    }

    /*
     * Customer operations ops
     */
    public static CustomerOps saveCustomerOp(CustomerOps op) {
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
            return null;
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
            return null;
        }
    }

    public static MerchantStats saveMerchantStats(MerchantStats stats) {
        return Backendless.Persistence.save( stats );
    }

    /*
     * Transaction operations
     */
    public static List<Transaction> fetchTransactions(String whereClause, String tableName) {
        Backendless.Data.mapTableToClass(tableName, Transaction.class);

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
            // no matching txns in not an error
            return null;
        }

        List<Transaction> transactions = collection.getData();
        while(collection.getCurrentPage().size() > 0) {
            collection = collection.nextPage();
            transactions.addAll(collection.getData());
        }
        return transactions;
    }

    /*
     * InternalUser operations
     */
    public static InternalUser getInternalUser(String userId) {
        BackendlessDataQuery query = new BackendlessDataQuery();
        query.setWhereClause("id = '"+userId+"'");

        BackendlessCollection<InternalUser> user = Backendless.Data.of( InternalUser.class ).find(query);
        if( user.getTotalObjects() == 0) {
            // no data found
            String errorMsg = "No internal user found: "+userId;
            throw new BackendlessException(BackendResponseCodes.BE_ERROR_NO_SUCH_USER, errorMsg);
        } else {
            return user.getData().get(0);
        }
    }

    public static void loadInternalUser(BackendlessUser user) {
        ArrayList<String> relationProps = new ArrayList<>();
        relationProps.add("internalUser");

        Backendless.Data.of( BackendlessUser.class ).loadRelations(user, relationProps);
    }

    public static InternalUser updateInternalUser(InternalUser user) {
        return Backendless.Persistence.save(user);
    }

    /*
     * Merchant Id ops
     */
    public static MerchantIdBatches firstMerchantIdBatchByBatchId(String tableName, String whereClause, boolean highest) {
        Backendless.Data.mapTableToClass(tableName, MerchantIdBatches.class);

        // fetch txns object from DB
        BackendlessDataQuery dataQuery = new BackendlessDataQuery();
        QueryOptions options = new QueryOptions();
        if(highest) {
            options.addSortByOption("batchId DESC");
        } else {
            options.addSortByOption("batchId ASC");
        }
        dataQuery.setQueryOptions(options);
        dataQuery.setPageSize(1);
        dataQuery.setWhereClause(whereClause);

        BackendlessCollection<MerchantIdBatches> collection = Backendless.Data.of(MerchantIdBatches.class).find(dataQuery);
        if(collection.getTotalObjects() > 0) {
            return collection.getData().get(0);
        } else {
            return null;
        }
    }

    public static MerchantIdBatches saveMerchantIdBatch(String tableName, MerchantIdBatches batch) {
        Backendless.Data.mapTableToClass(tableName, MerchantIdBatches.class);
        return Backendless.Persistence.save(batch);
    }

    public static MerchantIdBatches fetchMerchantIdBatch(String tableName, String whereClause) {
        Backendless.Data.mapTableToClass(tableName, MerchantIdBatches.class);

        // fetch txns object from DB
        BackendlessDataQuery dataQuery = new BackendlessDataQuery();
        dataQuery.setWhereClause(whereClause);

        BackendlessCollection<MerchantIdBatches> collection = Backendless.Data.of(MerchantIdBatches.class).find(dataQuery);
        int size = collection.getTotalObjects();
        if(size == 0) {
            return null;
        } else if(size == 1) {
            return collection.getData().get(0);
        }
        throw new BackendlessException(BackendResponseCodes.BE_ERROR_GENERAL, "More than 1 open merchant id batches: "+size+","+tableName);
    }

    /*
     * Card Id ops
     */
    public static CardIdBatches firstCardIdBatchByBatchId(String tableName, String whereClause, boolean highest) {
        Backendless.Data.mapTableToClass(tableName, CardIdBatches.class);

        // fetch txns object from DB
        BackendlessDataQuery dataQuery = new BackendlessDataQuery();
        QueryOptions options = new QueryOptions();
        if(highest) {
            options.addSortByOption("batchId DESC");
        } else {
            options.addSortByOption("batchId ASC");
        }
        dataQuery.setQueryOptions(options);
        dataQuery.setPageSize(1);
        dataQuery.setWhereClause(whereClause);

        BackendlessCollection<CardIdBatches> collection = Backendless.Data.of(CardIdBatches.class).find(dataQuery);
        if(collection.getTotalObjects() > 0) {
            return collection.getData().get(0);
        } else {
            return null;
        }
    }

    public static CardIdBatches saveCardIdBatch(String tableName, CardIdBatches batch) {
        Backendless.Data.mapTableToClass(tableName, CardIdBatches.class);
        return Backendless.Persistence.save(batch);
    }

    public static CardIdBatches fetchOpenCardIdBatch(String tableName) {
        Backendless.Data.mapTableToClass(tableName, CardIdBatches.class);

        String whereClause = "status = '"+DbConstantsBackend.CARD_ID_BATCH_STATUS_OPEN+"'";

        // fetch txns object from DB
        BackendlessDataQuery dataQuery = new BackendlessDataQuery();
        dataQuery.setWhereClause(whereClause);

        BackendlessCollection<CardIdBatches> collection = Backendless.Data.of(CardIdBatches.class).find(dataQuery);
        int size = collection.getTotalObjects();
        if(size == 0) {
            return null;
        } else if(size == 1) {
            return collection.getData().get(0);
        }
        throw new BackendlessException(BackendResponseCodes.BE_ERROR_GENERAL, "More than 1 open Card id batches: "+size+","+tableName);
    }


    public static InternalUserDevice fetchInternalUserDevice(String userId) {
        BackendlessDataQuery query = new BackendlessDataQuery();
        query.setWhereClause("userId = '"+userId+"'");

        BackendlessCollection<InternalUserDevice> user = Backendless.Data.of( InternalUserDevice.class ).find(query);
        if( user.getTotalObjects() == 0) {
            return null;
        } else {
            return user.getData().get(0);
        }
    }

    public static InternalUserDevice saveInternalUserDevice(InternalUserDevice device) {
        return Backendless.Persistence.save(device);
    }



}

    /*
    public static MerchantIdBatches fetchMerchantIdBatch(String tableName, String rangeId, String batchId) {
        Backendless.Data.mapTableToClass(tableName, MerchantIdBatches.class);

        String whereClause = "rangeId = '"+rangeId+"' and batchId = '"+batchId+"'";

        // fetch txns object from DB
        BackendlessDataQuery dataQuery = new BackendlessDataQuery();
        dataQuery.setPageSize(CommonConstants.dbQueryMaxPageSize);
        dataQuery.setWhereClause(whereClause);

        BackendlessCollection<MerchantIdBatches> collection = Backendless.Data.of(MerchantIdBatches.class).find(dataQuery);

        int size = collection.getTotalObjects();
        if(size == 1) {
            return collection.getData().get(0);
        }
        throw new BackendlessException(BackendResponseCodes.BE_ERROR_GENERAL, "Batch object is not exactly 1: "+size+","+tableName+","+batchId);
    }

    public static List<MerchantIdBatches> fetchOpenMerchantIdBatches(String tableName) {
        Backendless.Data.mapTableToClass(tableName, MerchantIdBatches.class);

        String whereClause = "status = "+DbConstantsBackend.MERCHANT_ID_BATCH_STATUS_OPEN+"'";

        // fetch txns object from DB
        BackendlessDataQuery dataQuery = new BackendlessDataQuery();
        dataQuery.setPageSize(CommonConstants.dbQueryMaxPageSize);
        dataQuery.setWhereClause(whereClause);

        BackendlessCollection<MerchantIdBatches> collection = Backendless.Data.of(MerchantIdBatches.class).find(dataQuery);
        int size = collection.getTotalObjects();
        if(size <= 0) {
            return null;
        }

        List<MerchantIdBatches> batches = collection.getData();
        while(collection.getCurrentPage().size() > 0) {
            collection = collection.nextPage();
            batches.addAll(collection.getData());
        }
        return batches;
    }

    public static List<MerchantIdBatches> fetchMerchantIdBatch(String tableName, String whereClause) {
        Backendless.Data.mapTableToClass(tableName, MerchantIdBatches.class);

        // fetch txns object from DB
        BackendlessDataQuery dataQuery = new BackendlessDataQuery();
        dataQuery.setPageSize(CommonConstants.dbQueryMaxPageSize);
        dataQuery.setWhereClause(whereClause);

        BackendlessCollection<MerchantIdBatches> collection = Backendless.Data.of(MerchantIdBatches.class).find(dataQuery);

        int size = collection.getTotalObjects();
        if(size <= 0) {
            // no matching txns in not an error
            return null;
        }

        List<MerchantIdBatches> batches = collection.getData();
        while(collection.getCurrentPage().size() > 0) {
            collection = collection.nextPage();
            batches.addAll(collection.getData());
        }
        return batches;
    }

    public static boolean merchantIdBatchOpen(String tableName, String batchId) {
        Backendless.Data.mapTableToClass(tableName, MerchantIds.class);

        // Batch is open - if any merchantId already exists for that batchId in the merchantIds table for that range
        String whereClause = "batchId = '"+batchId+"'";

        // fetch txns object from DB
        BackendlessDataQuery dataQuery = new BackendlessDataQuery();
        dataQuery.setPageSize(CommonConstants.dbQueryMaxPageSize);
        dataQuery.setWhereClause(whereClause);

        BackendlessCollection<MerchantIds> collection = Backendless.Data.of(MerchantIds.class).find(dataQuery);
        int size = collection.getTotalObjects();
        if(size > 0) {
            // no matching txns in not an error
            return true;
        }

        return false;
    }

    public static MerchantIds createMerchantId(String tableName, MerchantIds batch) {
        Backendless.Data.mapTableToClass(tableName, MerchantIds.class);
        return Backendless.Persistence.save(batch);
    }

    public static int getAvailableMerchantIdCnt(String tableName, String batchId) {
        Backendless.Data.mapTableToClass(tableName, MerchantIds.class);

        // Range id is open - if any batch already available for that range in the batches table for that country
        String whereClause = "batchId = '"+batchId+"' and status = "+DbConstantsBackend.MERCHANT_ID_STATUS_AVAILABLE+"'";

        // fetch txns object from DB
        BackendlessDataQuery dataQuery = new BackendlessDataQuery();
        dataQuery.setPageSize(CommonConstants.dbQueryMaxPageSize);
        dataQuery.setWhereClause(whereClause);

        BackendlessCollection<MerchantIds> collection = Backendless.Data.of(MerchantIds.class).find(dataQuery);
        return collection.getTotalObjects();
    }

    public static int getTotalMerchantIdCnt(String tableName, String batchId) {
        Backendless.Data.mapTableToClass(tableName, MerchantIds.class);

        // Range id is open - if any batch already available for that range in the batches table for that country
        String whereClause = "batchId = '"+batchId+"'";

        // fetch txns object from DB
        BackendlessDataQuery dataQuery = new BackendlessDataQuery();
        dataQuery.setPageSize(CommonConstants.dbQueryMaxPageSize);
        dataQuery.setWhereClause(whereClause);

        BackendlessCollection<MerchantIds> collection = Backendless.Data.of(MerchantIds.class).find(dataQuery);
        return collection.getTotalObjects();
    }*/
    /*
    public static boolean merchantIdRangeOpen(String tableName, String rangeId) {
        Backendless.Data.mapTableToClass(tableName, MerchantIdBatches.class);

        // Range id is open - if any batch already available for that range in the batches table for that country
        String whereClause = "rangeId = '"+rangeId+"'";

        // fetch txns object from DB
        BackendlessDataQuery dataQuery = new BackendlessDataQuery();
        dataQuery.setPageSize(CommonConstants.dbQueryMaxPageSize);
        dataQuery.setWhereClause(whereClause);

        BackendlessCollection<MerchantIdBatches> collection = Backendless.Data.of(MerchantIdBatches.class).find(dataQuery);

        int size = collection.getTotalObjects();
        if(size > 0) {
            // no matching txns in not an error
            return true;
        }

        return false;
    }*/
