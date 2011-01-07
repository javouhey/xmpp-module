package com.raverun.im.interfaces.rest.impl;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import org.json.JSONException;
import org.json.JSONWriter;

import com.raverun.im.interfaces.rest.ProtocolErrorCode;
import com.raverun.im.interfaces.rest.RestResponse;
import com.raverun.im.interfaces.rest.VersionChecker.Version;
import com.raverun.shared.Common;
import com.raverun.shared.Constraint.NonNullArgument;

public class DefaultRestResponseError implements RestResponse
{
    private final int       _statusCode;
    private final String    _message;
    private final Version   _version;
    private final MediaType _mediaType;
    private Map<String,String> _headers;
    private final ProtocolErrorCode _errorCode;

    public DefaultRestResponseError( String message, ProtocolErrorCode errorCode, int statusCode, MediaType type, Version version )
    {
        NonNullArgument.check( type, "type" );
        NonNullArgument.check( version, "version" );
        NonNullArgument.check( errorCode, "errorCode" );

        _mediaType  = type;
        _message    = message;
        _version    = version;
        _statusCode = statusCode;
        _errorCode  = errorCode;
        _headers = new HashMap<String,String>(1);
    }

    public DefaultRestResponseError( String message, ProtocolErrorCode errorCode, int statusCode, MediaType type, Version version, Map<String,String> theHeaders )
    {
        NonNullArgument.check( type, "type" );
        NonNullArgument.check( version, "version" );
        NonNullArgument.check( errorCode, "errorCode" );
        NonNullArgument.check( theHeaders, "theHeaders" );

        _mediaType  = type;
        _message    = message;
        _version    = version;
        _statusCode = statusCode;
        _errorCode  = errorCode;

        _headers = new HashMap<String,String>(1);
        if( theHeaders.size() > 0 )
            _headers.putAll( theHeaders );
    }

    @Override
    public String httpErrorMessage()
    {
        return _message;
    }

    @Override
    public int httpStatusCode()
    {
        return _statusCode;
    }

    @Override
    public boolean isError()
    {
        return true;
    }

    @Override
    public MediaType type()
    {
        return _mediaType;
    }
    
    @Override
    public String serialize()
    {
        if( _mediaType.equals( MediaType.APPLICATION_JSON_TYPE ) )
            return toCoreCompliantJSON();

            //return toJSON();

        if( _mediaType.equals( MediaType.TEXT_XML_TYPE ) )
            return toXml();

        throw new AssertionError( "Should never come here. Invalid _mediaType" );
    }

    private final String toXml()
    {
        StringBuilder builder = new StringBuilder();
        switch( _version )
        {
        case ONE:
            builder.append( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" );
            builder.append( "<error>" );
            builder.append( "<code>" );
            builder.append( _errorCode );
            builder.append( "</code>" );
            builder.append( "<message>" );
            builder.append( _message );
            builder.append( "</message>" );
            builder.append( "</error>" );
            break;

        case ZERO:
        default:
            builder.append( "<mc>" );
            builder.append( "<res>" );
            builder.append( _errorCode );
            builder.append( "</res>" );
            builder.append( "<res>" );
            builder.append( _errorCode );
            builder.append( "</res>" );
            builder.append( "<message>" );
            builder.append( _message );
            builder.append( "</message>" );
            builder.append( "</mc>" );
            break;
        }
        return builder.toString();
    }

    private final String toCoreCompliantJSON()
    {
        StringWriter stringWriter = new StringWriter();
        try
        {
            new JSONWriter( stringWriter )
            .object()
                .key( "stat" )
                .value( "fail" )
                .key( "errorcode" )
                .value( _errorCode )
                .key( "message" )
                .value( _message )
                .key( "errorCode" )
                .value( _errorCode )
                .key( "errorMessageParameters" )
                .value( Common.EMPTY_STRING )
                .key( "errorMessage" )
                .value( _message )
                .key( "errorType" )
                .value( "System" )
            .endObject();
        }
        catch( JSONException e )
        {
            e.printStackTrace();
            // TODO do something
        }

        return stringWriter.toString();
    }

    /**
     * Original flavor
     */
    private final String toJSON()
    {
        StringWriter stringWriter = new StringWriter();
        try
        {
            new JSONWriter( stringWriter )
            .object()
                .key( "stat" )
                .value( "fail" )
                .key( "errorcode" )
                .value( _errorCode )
                .key( "message" )
                .value( _message )
            .endObject();
        }
        catch( JSONException e )
        {
            e.printStackTrace();
            // TODO do something
        }

        return stringWriter.toString();
    }

    public boolean hasBody()
    {
        return true;
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
}
