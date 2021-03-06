package in.myecash.events.persistence_service;

import com.backendless.exceptions.BackendlessException;
import com.backendless.persistence.BackendlessDataQuery;
import com.backendless.servercode.RunnerContext;
import com.backendless.servercode.annotation.Asset;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.common.database.Merchants;
import in.myecash.constants.BackendConstants;
import in.myecash.utilities.BackendUtils;
import in.myecash.utilities.MyLogger;

/**
 * MerchantsTableEventHandler handles events for all entities. This is accomplished
 * with the @Asset( "Merchants" ) annotation.
 * The methods in the class correspond to the events selected in Backendless
 * Console.
 */

@Asset( "Merchants" )
public class MerchantsTableEventHandler extends com.backendless.servercode.extension.PersistenceExtender<Merchants>
{
    private MyLogger mLogger = new MyLogger("events.MerchantsTableEventHandler");
    private String[] mEdr = new String[BackendConstants.BACKEND_EDR_MAX_FIELDS];;

    @Override
    public void beforeUpdate( RunnerContext context, Merchants merchant ) throws Exception
    {
        MyLogger mLogger = new MyLogger("events.MerchantsTableEventHandler");
        String[] mEdr = new String[BackendConstants.BACKEND_EDR_MAX_FIELDS];;

        // update not allowed from app - return exception
        // beforeUpdate is not called, if update is done from server code
        mEdr[BackendConstants.EDR_API_NAME_IDX] = "Merchants-beforeUpdate";
        mEdr[BackendConstants.EDR_API_PARAMS_IDX] = merchant.getAuto_id();
        BackendUtils.writeOpNotAllowedEdr(mLogger, mEdr);
        throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "");
    }

    @Override
    public void beforeFirst( RunnerContext context ) throws Exception
    {
        // block for not-authenticated user
        // this event handler does not get called, if find done from servercode
        if(context.getUserToken()==null) {
            mEdr[BackendConstants.EDR_API_NAME_IDX] = "Merchants-beforeFirst";
            BackendUtils.writeOpNotAllowedEdr(mLogger, mEdr);
            throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED),"");
        }
    }

    @Override
    public void beforeFind( RunnerContext context, BackendlessDataQuery query ) throws Exception
    {
        // block for not-authenticated user
        // this event handler does not get called, if find done from servercode
        if(context.getUserToken()==null) {
            mEdr[BackendConstants.EDR_API_NAME_IDX] = "Merchants-beforeFind";
            mEdr[BackendConstants.EDR_API_PARAMS_IDX] = query.getWhereClause();
            BackendUtils.writeOpNotAllowedEdr(mLogger, mEdr);
            throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED),"");
        }
    }

    @Override
    public void beforeLast( RunnerContext context ) throws Exception
    {
        // block for not-authenticated user
        // this event handler does not get called, if find done from servercode
        if(context.getUserToken()==null) {
            mEdr[BackendConstants.EDR_API_NAME_IDX] = "Merchants-beforeFirst";
            BackendUtils.writeOpNotAllowedEdr(mLogger, mEdr);
            throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED),"");
        }
    }
}