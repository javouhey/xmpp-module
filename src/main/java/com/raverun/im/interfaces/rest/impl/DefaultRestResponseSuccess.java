package com.raverun.im.interfaces.rest.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import com.raverun.im.interfaces.rest.RestResponse;
import com.raverun.im.interfaces.rest.RestSerializable;
import com.raverun.shared.Common;
import com.raverun.shared.Constraint;

public class DefaultRestResponseSuccess implements RestResponse
{
    public static class Builder
    {
        public Builder( MediaType type, int httpStatusCode )
        {
            Constraint.NonNullArgument.check( type, "type" );
            builderOutType = type;
            builderHeaders = new HashMap<String,String>(4);
            builderHttpStatusCode = httpStatusCode;
        }

        public Builder httpHeaders( Map<String,String> headers )
        {
            Constraint.NonNullArgument.check( headers, "headers" );

            if( headers.size() > 0 )
                builderHeaders.putAll( headers );

            return this;
        }

        public Builder body( RestSerializable body )
        {
            Constraint.NonNullArgument.check( body, "body" );
            builderBody = body;
            return this;
        }

        public DefaultRestResponseSuccess build()
        {
            DefaultRestResponseSuccess result = new DefaultRestResponseSuccess( 
                this.builderOutType, this.builderHttpStatusCode );

            if( builderBody != null )
                result._body = builderBody;

            result._headers = new HashMap<String,String>(4);
            if( builderHeaders.size() > 0 )
                result._headers.putAll( builderHeaders );

            return result;
        }

        private MediaType          builderOutType;
        private RestSerializable   builderBody;
        private Map<String,String> builderHeaders;
        private int                builderHttpStatusCode;
    }

    private DefaultRestResponseSuccess( MediaType type, int httpStatusCode )
    {
        _outType = type;
        _httpStatusCode = httpStatusCode;
    }

    @Override
    public String httpErrorMessage()
    {
        return Common.EMPTY_STRING;
    }

    @Override
    public int httpStatusCode()
    {
        return _httpStatusCode;
    }

    @Override
    public boolean isError()
    {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see com.raverun.im.interfaces.rest.RestResponse#serialize()
     */
    @Override
    public String serialize()
    {
        if( hasBody() )
            return _body.serialize();

        return Common.EMPTY_STRING;
    }

    @Override
    public MediaType type()
    {
        return _outType;
    }

    @Override
    public boolean hasBody()
    {
        return _body != null;
    }

    @Override
    public void spitOutHttpHeaders( HttpServletResponse httpResponse )
    {
        if( _headers.size() > 0 )
        {
            Set<String> keys = _headers.keySet();
            for( String key : keys )
            {
                httpResponse.addHeader( key, _headers.get( key ) );
            }
        }
    }

    private MediaType          _outType;
    private RestSerializable   _body;
    private Map<String,String> _headers;
    private int                _httpStatusCode;
}
