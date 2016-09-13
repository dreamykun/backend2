package com.myecash.utilities;

import com.backendless.Backendless;
import com.backendless.exceptions.BackendlessException;
import com.myecash.constants.BackendConstants;
import com.myecash.constants.BackendResponseCodes;
import com.myecash.constants.CommonConstants;
import com.myecash.database.Merchants;
import com.myecash.database.Transaction;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

public class TxnArchiver
{
    //transid(10),time(20),merchantid(6),merchantname(50),customermobile(10),customerprivid(5),amts(6x4=24),rate(2) = 115 chars = 115x2 = 250 bytes
    private static final int TXN_CSV_RECORD_MAX_CHARS = 250;

    private MyLogger mLogger;

    private List<Transaction> mLastFetchTransactions;
    private Merchants mLastFetchMerchant;
    //private String mMerchantIdSuffix;

    // map of 'txn date' -> 'csv file url'
    private HashMap<String,String> mCsvFiles;
    // map of 'txn date' -> 'csv string of all txns in dat date'
    private HashMap<String,StringBuilder> mCsvDataMap;

    // All required date formatters
    private SimpleDateFormat mSdfDateWithTime;
    //private SimpleDateFormat mSdfOnlyDateBackend;
    //private SimpleDateFormat mSdfOnlyDateBackendGMT;
    //private SimpleDateFormat mSdfOnlyDateFilename;
    private Date mToday;
    private String mUserToken;

    //public TxnArchiver(String merchantIdSuffix, Logger logger, Merchants merchant) {
    public TxnArchiver(MyLogger logger, Merchants merchant, String userToken) {
        //mMerchantIdSuffix = merchantIdSuffix;

        mToday = new Date();
        mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);
        //mSdfOnlyDateBackend = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_DATE_BACKEND, CommonConstants.DATE_LOCALE);
        //mSdfOnlyDateBackendGMT = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_DATE_BACKEND, CommonConstants.DATE_LOCALE);
        //mSdfOnlyDateFilename = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_DATE_FILENAME, CommonConstants.DATE_LOCALE);

        mCsvFiles = new HashMap<>();
        mCsvDataMap = new HashMap<>();

        mLogger = logger;
        mLastFetchMerchant = merchant;
        mUserToken = userToken;
    }

    /*
    public void execute(Logger logger) throws Exception
    {
        long startTime = System.currentTimeMillis();
        mLogger = logger;

        mSdfDateWithTime.setTimeZone(TimeZone.getTimeZone(BackendConstants.TIMEZONE));
        mSdfOnlyDateBackend.setTimeZone(TimeZone.getTimeZone(BackendConstants.TIMEZONE));
        mSdfOnlyDateBackendGMT.setTimeZone(TimeZone.getTimeZone("GMT"));
        mSdfOnlyDateFilename.setTimeZone(TimeZone.getTimeZone(BackendConstants.TIMEZONE));

        mLogger.debug("Running TxnArchiver"+mMerchantIdSuffix+", "+mSdfDateWithTime.format(startTime));
        CommonUtils.initTableToClassMappings();

        // Fetch next not processed merchant
        mLastFetchMerchant = null;
        //mLastFetchMerchant = fetchNextMerchant();
        ArrayList<Merchants> merchants = BackendOps.fetchMerchants(buildMerchantWhereClause());
        if(merchants != null) {
            int merchantCnt = merchants.size();
            for (int k = 0; k < merchantCnt; k++) {
                mLastFetchMerchant = merchants.get(k);
                archiveMerchantTxns();
                //Backendless.Logging.flush();
            }
        }
        long endTime = System.currentTimeMillis();
        mLogger.debug( "Exiting TxnArchiver: "+((endTime-startTime)/1000));
        //Backendless.Logging.flush();
    }*/

    public void archiveMerchantTxns(String[] edr) {
        String merchantId = mLastFetchMerchant.getAuto_id();
        //mLogger.debug("Fetched merchant id: "+merchantId);

        String txnTableName = mLastFetchMerchant.getTxn_table();

        mLastFetchTransactions = null;
        mLastFetchTransactions = BackendOps.fetchTransactions(buildTxnWhereClause(merchantId), txnTableName);
        if(mLastFetchTransactions != null) {
            mLogger.debug("Fetched "+mLastFetchTransactions.size()+" transactions for "+merchantId);
            if(mLastFetchTransactions.size() > 0) {
                // convert txns into csv strings
                buildCsvString();

                // TODO: encrypt CSV string sensitive params

                // store CSV file in merchant directory
                createCsvFiles(merchantId);
                // update txn status
                int recordsUpdated = updateTxnArchiveStatus(txnTableName,merchantId,true);
                if(recordsUpdated == -1) {
                    String error = "Failed to update txn archive status";
                    mLogger.error( error);
                    //TODO: raise alarm
                    // rollback
                    deleteCsvFiles();
                    edr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_MANUAL_CHECK;
                    throw new BackendlessException(BackendResponseCodes.BE_ERROR_GENERAL, error);

                } else if(recordsUpdated != mLastFetchTransactions.size()) {
                    String error = "Count of txns updated for status does not match.";
                    //TODO: raise critical alarm
                    // rollback
                    if( updateTxnArchiveStatus(txnTableName,merchantId,false) == -1) {
                        //TODO: raise critical alarm
                        error = error+": "+"Txn archive timer: rollback 1 failed.";
                    }
                    mLogger.error(error);
                    deleteCsvFiles();
                    edr[BackendConstants.EDR_SPECIAL_FLAG_IDX] = BackendConstants.BACKEND_EDR_MANUAL_CHECK;
                    throw new BackendlessException(BackendResponseCodes.BE_ERROR_GENERAL, error);

                } else {
                    // update archive date in merchant record
                    // set to current time
                    mLastFetchMerchant.setLast_txn_archive(mToday);
                    // dont care even if below update fails
                    BackendOps.updateMerchant(mLastFetchMerchant);
                    mLogger.debug("Txns archived successfully: "+recordsUpdated+", "+merchantId);
                }
            }
        }
    }

    private int updateTxnArchiveStatus(String txnTableName, String merchantId, boolean status) {
        mLogger.debug( "In updateTxnArchiveStatus: "+txnTableName+", "+merchantId+", "+status);
        /*
        * curl
        * -H application-id:09667F8B-98A7-E6B9-FFEB-B2B6EE831A00
        * -H secret-key:95971CBD-BADD-C61D-FF32-559664AE4F00
        * -H Content-Type:application/json
        * -X PUT
        * -d "{\"archived\":true}"
        * -v https://api.backendless.com/v1/data/bulk/<tablename>?where=workDays%3E10
        */
        try {
            String whereClause = URLEncoder.encode(buildTxnWhereClause(merchantId), "UTF-8");

            // https://api.backendless.com/v1/data/bulk/Transaction0?where=merchant_id+%3D+%27AA0007%27+AND+create_time+%3C+%271467138600000%27+AND+archived%3Dfalse
            // Building URL without URI
            StringBuffer sb = new StringBuffer(BackendConstants.BULK_API_URL);
            sb.append(txnTableName);
            sb.append("?where=");
            sb.append(whereClause);

            URL url = new URL(sb.toString());

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("application-id", BackendConstants.APP_ID);
            conn.setRequestProperty("user-token", mUserToken);
            conn.setRequestProperty("secret-key", BackendConstants.SECRET_KEY);
            conn.setRequestProperty("Content-Type", "application/json");

            String input;
            if(status) {
                input = "{\"archived\":true}";
            } else {
                input = "{\"archived\":false}";
            }

            OutputStream os = conn.getOutputStream();
            os.write(input.getBytes());
            os.flush();

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                mLogger.error("Error HTTP response: "+conn.getResponseCode());
                return -1;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

            int recordsUpdated = 0;
            String output;
            while ((output = br.readLine()) != null) {
                mLogger.debug("Output from server: "+output);
                recordsUpdated = Integer.parseInt(output.replaceAll("\\p{C}", "?"));
            }

            conn.disconnect();
            mLogger.debug("Updated archive status of txns: "+recordsUpdated);
            return recordsUpdated;

        } catch (Exception e) {
            mLogger.error("Failed to update txn status: "+e.toString());
            return -1;
        }
    }

    private void createCsvFiles(String merchantId) {
        mCsvFiles.clear();
        // one file for each day i.e. each entry in mCsvDataMap
        for (Map.Entry<String, StringBuilder> entry : mCsvDataMap.entrySet()) {
            String filename = entry.getKey();
            StringBuilder sb = entry.getValue();

            // File name: txns_<merchant_id>_<ddMMMyy>.csv
            String filepath = CommonUtils.getMerchantTxnDir(merchantId) + CommonConstants.FILE_PATH_SEPERATOR + filename;

            try {
                String fileUrl = Backendless.Files.saveFile(filepath, sb.toString().getBytes("UTF-8"), true);
                mLogger.debug("Txn CSV file uploaded: " + fileUrl);
                mCsvFiles.put(filename,filepath);
            } catch (UnsupportedEncodingException e) {
                mLogger.error("Txn CSV file upload failed: "+ filepath + e.toString());
                // For multiple days, single failure will be considered failure for all days
                throw new BackendlessException(BackendResponseCodes.BE_ERROR_GENERAL, "Failed to create CSV file: "+e.toString());
            }
        }
    }

    private boolean deleteCsvFiles() {
        try {
            for (String filePath : mCsvFiles.values()) {
                Backendless.Files.remove(filePath);
                mLogger.debug("Deleted CSV file: " + filePath);
            }
        } catch(Exception e) {
            mLogger.debug("CSV file delete failed: " + e.toString());
            return false;
        }
        return true;
    }

    // Assumes mLastFetchTransactions may contain txns prior to yesterday too
    // Creates HashMap with one entry for each day
    private void buildCsvString() {
        int size = mLastFetchTransactions.size();

        mCsvDataMap.clear();
        for(int i=0; i<size; i++) {
            Transaction txn = mLastFetchTransactions.get(i);
            Date txnDate = txn.getCreate_time();
            // get 'date' for this transaction - in the filename format - to be used as key in map
            //String txnDateStr = mSdfOnlyDateFilename.format(txnDate);
            String filename = CommonUtils.getTxnCsvFilename(txnDate, mLastFetchMerchant.getAuto_id());

            StringBuilder sb = mCsvDataMap.get(filename);
            if(sb==null) {
                mLogger.debug("buildCsvString, new day : "+filename);
                sb = new StringBuilder(TXN_CSV_RECORD_MAX_CHARS *size);
                // new file - write first line as header
                sb.append("trans_id,time,merchant_id,merchant_name,customer_id,cust_private_id,total_billed,cb_billed,cl_debit,cl_credit,cb_debit,cb_credit,cb_percent");
                sb.append(CommonConstants.CSV_NEWLINE);
                mCsvDataMap.put(filename,sb);
            }

            /*
             * Format:
             *    trans_id,time,merchant_id,merchant_name,customer_id,cust_private_id,
             *    total_billed,cb_billed,cl_debit,cl_credit,cb_debit,cb_credit,cb_percent,cpin\n
             * The sequence in format should match 'index constants' defined in CommonConstants class
             */
            sb.append(txn.getTrans_id()).append(CommonConstants.CSV_DELIMETER);
            sb.append(mSdfDateWithTime.format(txnDate)).append(CommonConstants.CSV_DELIMETER);
            sb.append(mLastFetchMerchant.getAuto_id()).append(CommonConstants.CSV_DELIMETER);
            sb.append(mLastFetchMerchant.getName()).append(CommonConstants.CSV_DELIMETER);
            sb.append(txn.getCustomer_id()).append(CommonConstants.CSV_DELIMETER);
            sb.append(txn.getCust_private_id()).append(CommonConstants.CSV_DELIMETER);
            sb.append(String.valueOf(txn.getTotal_billed())).append(CommonConstants.CSV_DELIMETER);
            sb.append(String.valueOf(txn.getCb_billed())).append(CommonConstants.CSV_DELIMETER);
            sb.append(String.valueOf(txn.getCl_debit())).append(CommonConstants.CSV_DELIMETER);
            sb.append(String.valueOf(txn.getCl_credit())).append(CommonConstants.CSV_DELIMETER);
            sb.append(String.valueOf(txn.getCb_debit())).append(CommonConstants.CSV_DELIMETER);
            sb.append(String.valueOf(txn.getCb_credit())).append(CommonConstants.CSV_DELIMETER);
            sb.append(String.valueOf(txn.getCb_percent())).append(CommonConstants.CSV_DELIMETER);
            sb.append(txn.getCpin()).append(CommonConstants.CSV_DELIMETER);
            sb.append(CommonConstants.CSV_NEWLINE);
        }
    }

    private String buildTxnWhereClause(String merchantId) {
        StringBuilder whereClause = new StringBuilder();

        // for particular merchant
        whereClause.append("merchant_id = '").append(merchantId).append("'");

        DateUtil todayMidnight = new DateUtil();
        todayMidnight.toTZ(BackendConstants.TIMEZONE);
        todayMidnight.toMidnight();

        // all txns older than today 00:00 hrs - the timer runs 1 AM each day
        whereClause.append(" AND create_time < '").append(todayMidnight.getTime().getTime()).append("'");
        whereClause.append(" AND archived=").append("false");

        mLogger.debug("Txn where clause: " + whereClause.toString());
        return whereClause.toString();
    }

    /*
    private String buildMerchantWhereClause() {
        StringBuilder whereClause = new StringBuilder();
        //TODO: use mMerchantIdSuffix in production
        //whereClause.append("auto_id LIKE '%").append(mMerchantIdSuffix).append("'");
        whereClause.append("auto_id LIKE '1%").append("'");

        //String today = mSdfOnlyDateBackend.format(mToday);
        // merchants with last_txn_archive time before today midnight
        DateUtil todayMidnight = new DateUtil();
        todayMidnight.toTZ(BackendConstants.TIMEZONE);
        todayMidnight.toMidnight();

        whereClause.append(" AND (last_txn_archive < '").append(todayMidnight.getTime().getTime()).append("'").append(" OR last_txn_archive is null)");

        mLogger.debug("Merchant where clause: " + whereClause.toString());
        return whereClause.toString();
    }*/
}
        