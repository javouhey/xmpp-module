package com.raverun.im.interfaces.rest.impl.resources;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.raverun.im.interfaces.rest.ProtocolErrorCode;
import com.raverun.im.interfaces.rest.RestSerializable;
import com.raverun.shared.Common;
import com.raverun.shared.Constraint;

public class EitherResource
{
    public static class Builder
    {
        public Builder( boolean ok, int httpStatusCode )
        {
            builderOk             = ok;
            builderHeaders        = new HashMap<String,String>(4);
            builderHttpStatusCode = httpStatusCode;
        }

        public Builder httpHeaders( Map<String,String> headers )
        {
            Constraint.NonNullArgument.check( headers, "headers" );

            if( headers.size() > 0 )
                builderHeaders.putAll( headers );

            return this;
        }

        public Builder body( RestSerializable serializable )
        {
            Constraint.NonNullArgument.check( serializable, "serializable" );
            builderBody = serializable;
            return this;
        }

        public Builder error( String message, ProtocolErrorCode code )
        {
            Constraint.NonNullArgument.check( code, "code" );
            builderErrorMessage = ((message == null) ? Common.EMPTY_STRING : message);
            builderProtocolErrorCode = code;
            return this;
        }

        public EitherResource build()
        {
            EitherResource result = new EitherResource( 
                this.builderOk, this.builderHttpStatusCode );

            if( builderHeaders.size() > 0 )
                result._headers.putAll( builderHeaders );

            result._body = builderBody;
            if( builderProtocolErrorCode != null )
            {
                result._protocolErrorCode = builderProtocolErrorCode;
                result._errorMessage      = builderErrorMessage;
            }
            return result;
        }

        private boolean            builderOk;
        private RestSerializable   builderBody;
        private Map<String,String> builderHeaders;
        private int                builderHttpStatusCode;
        private ProtocolErrorCode  builderProtocolErrorCode;
        private String             builderErrorMessage;

    }//Builder

    private EitherResource( boolean ok, int httpStatusCode )
    {
        _ok = ok;
        _httpStatusCode = httpStatusCode;
    }

    public boolean isOk()
    {
        return _ok;
    }

    public int httpStatusCode()
    {
        return _httpStatusCode;
    }

    /**
     * @return nullable value
     */
    public RestSerializable body()
    {
        return _body;
    }

    public Map<String,String> headers()
    {
        return Collections.unmodifiableMap( _headers );
    }

    public String errorMessage()
    {
        return _errorMessage;
    }

    public ProtocolErrorCode errorCode()
    {
        return _protocolErrorCode;
    }

    private boolean            _ok = true;
    private RestSerializable   _body;
    private Map<String,String> _headers = new HashMap<String,String>(2);
    private int                _httpStatusCode = 200;
    private ProtocolErrorCode  _protocolErrorCode;
    private String             _errorMessage = Common.EMPTY_STRING;
}
