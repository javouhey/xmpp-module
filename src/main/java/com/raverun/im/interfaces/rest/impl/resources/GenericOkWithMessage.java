package com.raverun.im.interfaces.rest.impl.resources;

import java.io.StringWriter;

import javax.ws.rs.core.MediaType;

import org.json.JSONException;
import org.json.JSONWriter;

import com.raverun.im.interfaces.rest.RestSerializable;
import com.raverun.shared.Constraint;

public class GenericOkWithMessage implements RestSerializable
{
    public GenericOkWithMessage( String message, MediaType outType )
    {
        Constraint.NonNullArgument.check( outType, "outType" );
        _message = message;
        _outType = outType;
    }

    @Override
    public String serialize()
    {
        if( _outType.equals( MediaType.APPLICATION_JSON_TYPE ) )
            return toJSON();

        if( _outType.equals( MediaType.TEXT_XML_TYPE ) )
            return toXml();

        throw new AssertionError( "Should never come here. Invalid _mediaType" );
    }

    private final String toJSON()
    {
        StringWriter stringWriter = new StringWriter();
        try
        {
            new JSONWriter( stringWriter )
            .object()
                .key( "stat" )
                .value( "ok" )
                .key( "message" )
                .value( _message )
            .endObject();
        }
        catch( JSONException e )
        {
            // TODO do something
            e.printStackTrace();
        }

        return stringWriter.toString();
    }

    private final String toXml()
    {
        throw new UnsupportedOperationException();
    }

    private final String _message;
    private final MediaType _outType;
}
