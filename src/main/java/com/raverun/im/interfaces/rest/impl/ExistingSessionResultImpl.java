package com.raverun.im.interfaces.rest.impl;

import javax.ws.rs.core.MediaType;

import org.json.JSONException;
import org.json.JSONObject;

import com.raverun.im.domain.IMIdentity;
import com.raverun.im.domain.SigninErrorCodes;
import com.raverun.im.domain.IMSession.ExistingSessionResult;
import com.raverun.im.interfaces.rest.impl.resources.BucketOfFailedSignins;
import com.raverun.im.interfaces.rest.impl.resources.BucketSerializationException;
import com.raverun.shared.Common;
import com.raverun.shared.Constraint;

public class ExistingSessionResultImpl implements ExistingSessionResult
{
    public static class Builder
    {
        public Builder( boolean ok )
        {
            builderOk      = ok;
            builderFailed  = new BucketOfFailedSignins.Builder();
        }

        public Builder addFailure( IMIdentity identity, boolean permanent, SigninErrorCodes why )
        {
            builderFailed.add( identity, permanent, why );
            return this;
        }

        /**
         * @throws IllegalArgumentException if {@code theMessage} is null
         */
        public Builder message( String theMessage )
        {
            if( Constraint.EmptyString.isFulfilledBy( theMessage ))
                throw new IllegalArgumentException( "theMessage was null" );
            this.builderMessage = theMessage;
            return this;
        }

        /**
         * @throws BucketSerializationException for any problems with the data
         */
        public ExistingSessionResult build( final MediaType outType )
        {
            BucketOfFailedSignins bucketFailed = builderFailed.build();
            try
            {
                ExistingSessionResultImpl sessionResult = new ExistingSessionResultImpl( builderOk );
                if( builderOk )
                {
                    final JSONObject retval = new JSONObject();
                    retval.put( bucketFailed.keyName(), 
                        bucketFailed.serializeTo( outType ) );

                    sessionResult._numberOfFailures = bucketFailed.numberOfFailedIds();
                    sessionResult._loginReport = retval;
                    if( !Constraint.EmptyString.isFulfilledBy( builderMessage ) )
                        sessionResult._message = builderMessage;
                    else
                    {
                        if( bucketFailed.numberOfFailedIds() > 0 )
                            sessionResult._message = bucketFailed.toDisplay() + " failed.";
                        else
                            sessionResult._message = "All is fine";
                    }
                }
                else
                {
                    sessionResult._message = "bad";
                }
                return sessionResult;
            }
            catch( JSONException e )
            {
                throw new BucketSerializationException( "Failed to construct a ExistingSessionResult", e );
            }
        }

        private String builderMessage = Common.EMPTY_STRING;
        private final boolean builderOk;
        private final BucketOfFailedSignins.Builder builderFailed;
    }

    @Override
    public Object loginReport()
    {
        return _loginReport;
    }

    @Override
    public String message()
    {
        return _message;
    }

    @Override
    public int numFailed()
    {
        return _numberOfFailures;
    }

    @Override
    public boolean ok()
    {
        return _ok;
    }

    private ExistingSessionResultImpl( boolean ok )
    {
        _ok = ok;
    }

    private int _numberOfFailures = 0;
    private String _message;
    private JSONObject _loginReport;

    private final boolean _ok;

}
