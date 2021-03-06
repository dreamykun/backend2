package in.myecash.events.persistence_service;

/**
 * Created by adgangwa on 27-08-2016.
 */

import com.backendless.exceptions.BackendlessException;
import com.backendless.servercode.RunnerContext;
import com.backendless.servercode.annotation.Asset;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.common.database.MerchantDevice;
import in.myecash.constants.BackendConstants;
import in.myecash.utilities.BackendUtils;
import in.myecash.utilities.MyLogger;

/**
 * MerchantDeviceTableEventHandler handles events for all entities. This is accomplished
 * with the @Asset( "MerchantDevice" ) annotation.
 * The methods in the class correspond to the events selected in Backendless
 * Console.
 */

@Asset( "MerchantDevice" )
public class MerchantDeviceTableEventHandler extends com.backendless.servercode.extension.PersistenceExtender<MerchantDevice>
{

    @Override
    public void beforeUpdate(RunnerContext context, MerchantDevice merchantdevice ) throws Exception
    {
        MyLogger mLogger = new MyLogger("events.TransactionEventHandler");
        String[] mEdr = new String[BackendConstants.BACKEND_EDR_MAX_FIELDS];;

        // update not allowed from app - return exception
        // beforeUpdate is not called, if update is done from server code
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "txn-beforeUpdate";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = merchantdevice.getMerchant_id();
        BackendUtils.writeOpNotAllowedEdr(mLogger, mEdr);
        throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "");
    }

}