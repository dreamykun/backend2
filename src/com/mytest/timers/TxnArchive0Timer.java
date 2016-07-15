package com.mytest.timers;

import com.backendless.Backendless;
import com.backendless.HeadersManager;
import com.backendless.servercode.InvocationContext;
import com.backendless.servercode.annotation.BackendlessTimer;
import com.mytest.constants.BackendConstants;
import com.backendless.logging.Logger;

/**
 * TxnArchive0Timer is a timer.
 * It is executed according to the schedule defined in Backendless Console. The
 * class becomes a timer by extending the TimerExtender class. The information
 * about the timer, its name, schedule, expiration date/time is configured in
 * the special annotation - BackendlessTimer. The annotation contains a JSON
 * object which describes all properties of the timer.
 */

@BackendlessTimer("{'startDate':1463167800000,'frequency':{'schedule':'daily','repeat':{'every':1}},'timername':'TxnArchive0'}")
public class TxnArchive0Timer extends com.backendless.servercode.extension.TimerExtender
{
    private String MERCHANT_ID_SUFFIX = "0";

    @Override
    public void execute( String appVersionId ) throws Exception
    {
        Backendless.Logging.setLogReportingPolicy(BackendConstants.LOG_POLICY_NUM_MSGS, BackendConstants.LOG_POLICY_FREQ_SECS);
        Logger logger = Backendless.Logging.getLogger("com.mytest.timers.TxnArchiver0");
        try {
            logger.debug("Before: "+ HeadersManager.getInstance().getHeaders().toString());

            TxnArchiver archiver = new TxnArchiver(MERCHANT_ID_SUFFIX);
            archiver.execute(logger);
        } catch(Exception e) {
            logger.error("Exception in TxnArchive0Timer: "+e.toString());
            //Backendless.Logging.flush();
            throw e;
        }
    }
}