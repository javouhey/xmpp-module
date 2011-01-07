package com.raverun.im.interfaces.rest.impl;

import javax.ws.rs.core.MediaType;

import org.json.JSONException;
import org.json.JSONObject;

import com.raverun.im.domain.IMIdentity;
import com.raverun.im.domain.SigninErrorCodes;
import com.raverun.im.interfaces.rest.SessionUtils.NewSessionResult;
import com.raverun.im.interfaces.rest.impl.resources.BucketOfFailedSignins;
import com.raverun.im.interfaces.rest.impl.resources.BucketOfSuccessfulSignins;
import com.raverun.im.interfaces.rest.impl.resources.BucketSerializationException;
import com.raverun.shared.Common;

public class NewSessionResultImpl implements NewSessionResult
{
    public static class Builder
    {
        public Builder( boolean ok )
        {
            builderOk      = ok;
            builderSuccess = new BucketOfSuccessfulSignins.Builder();
            builderFailed  = new BucketOfFailedSignins.Builder();
        }

        public Builder httpSessionId( String theSessionId )
        {
            sessionId = theSessionId;
            return this;
        }
        
        public Builder addSuccess( IMIdentity identity, boolean permanent )
        {
            builderSuccess.add( identity, permanent );
            return this;
        }

        public Builder addFailure( IMIdentity identity, boolean permanent, SigninErrorCodes why )
        {
            builderFailed.add( identity, permanent, why );
            return this;
        }

        /**
         * @throws BucketSerializationException for any problems with the data
         */
        public NewSessionResult build( final MediaType outType )
        {
            BucketOfSuccessfulSignins bucketSuccess = builderSuccess.build();
            BucketOfFailedSignins      bucketFailed = builderFailed.build(); 

            try
            {
                NewSessionResultImpl sessionResult = new NewSessionResultImpl( builderOk );
                if( builderOk )
                {
                    final JSONObject retval = new JSONObject();
                    retval.put( bucketSuccess.keyName(), 
                        bucketSuccess.serializeTo( outType ) );
                    retval.put( bucketFailed.keyName(), 
                        bucketFailed.serializeTo( outType ) );

                    sessionResult._sessionId = sessionId;
                    sessionResult._loginReport = retval;
                    sessionResult._message = "good";
                }
                else
                {
                    sessionResult._message = "bad";
                }
                return sessionResult;
            }
            catch( JSONException e )
            {
                throw new BucketSerializationException( "Failed to construct a NewSessionResult", e );
            }
        }

        private String sessionId = Common.EMPTY_STRING;
        private final boolean builderOk;
        private final BucketOfSuccessfulSignins.Builder builderSuccess;
        private final BucketOfFailedSignins.Builder builderFailed;
    }

    private NewSessionResultImpl( boolean ok )
    {
        _ok = ok;
    }

    @Override
    public String httpSessionId()
    {
        return _sessionId;
    }

    @Override
    public Object loginReport()
    {
        return _loginReport;
//        return new RestSerializable() 
//        {
//            /**
//             * @throws BucketSerializationException
//             */
//            @Override
//            public String serialize()
//            {
//                try
//                {
//                    return _loginReport.toString( 4 );
//                }
//                catch( JSONException e )
//                {
//                    throw new BucketSerializationException( "Failed to serialize Login Report", e );
//                }
//            }
//        };
    }

    @Override
    public String message()
    {
        return _message;
    }

    @Override
    public boolean ok()
    {
        return _ok;
    }

    private String _message;
    private JSONObject _loginReport;
    private String _sessionId = Common.EMPTY_STRING;

    private final boolean _ok;
}
