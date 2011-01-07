package com.raverun.im.interfaces.rest.impl.resources;

import java.io.StringWriter;

import javax.ws.rs.core.MediaType;

import org.json.JSONException;
import org.json.JSONWriter;

import com.raverun.im.interfaces.rest.RestSerializable;
import com.raverun.shared.Constraint;

public class GenericOk implements RestSerializable
{
    public GenericOk( String path, MediaType type )
    {
        if( Constraint.EmptyString.isFulfilledBy( path ))
            throw new IllegalArgumentException( "path cannot be empty" );
        Constraint.NonNullArgument.check( type, "type" );

        _path = path;
        _type = type;
    }

    @Override
    public String serialize()
    {
        if( _type.equals( MediaType.APPLICATION_JSON_TYPE ) )
            return toJSON();

        if( _type.equals( MediaType.TEXT_XML_TYPE ) )
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
            .endObject();
        }
        catch( JSONException e )
        {
            e.printStackTrace();
            // TODO do something
        }

        return stringWriter.toString();
    }

    private final String toXml()
    {
        throw new UnsupportedOperationException();
    }


    private final String    _path;
    private final MediaType _type;

}
