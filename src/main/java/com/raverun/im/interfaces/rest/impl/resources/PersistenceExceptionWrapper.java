package com.raverun.im.interfaces.rest.impl.resources;

import javax.persistence.EntityExistsException;
import javax.persistence.PersistenceException;

import com.raverun.im.interfaces.rest.ProtocolErrorCode;
import com.raverun.shared.Common;
import org.apache.log4j.Logger;

public abstract class PersistenceExceptionWrapper
{
    public abstract EitherResource wrap();

    public EitherResource go( Logger logger, String conflictMessage, ProtocolErrorCode conflictCode )
    {
        try
        {
            return wrap();
        }
        catch( PersistenceException pe )
        {
            logger.error( Common.EMPTY_STRING, pe );
            if( pe instanceof EntityExistsException )
            {
                return new EitherResource.Builder( false, 409 )
                    .error( conflictMessage, conflictCode )
                    .build();
            }
            else
            {
                return new EitherResource.Builder( false, 500 )
                    .error( "We encountered an internal error (persistence). Please try again", ProtocolErrorCode.InternalError )
                    .build();
            }
        }
    }
}
