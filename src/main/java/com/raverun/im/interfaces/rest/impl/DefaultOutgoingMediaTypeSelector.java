package com.raverun.im.interfaces.rest.impl;

import java.util.List;

import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.raverun.im.interfaces.rest.OutgoingMediaTypeSelector;
import com.raverun.im.interfaces.rest.SupportedOutgoingMediaTypes;
import com.sun.jersey.core.header.AcceptableMediaType;

public class DefaultOutgoingMediaTypeSelector implements
    OutgoingMediaTypeSelector
{
    @Inject
    public DefaultOutgoingMediaTypeSelector( SupportedOutgoingMediaTypes outMediaTypes )
    {
        _outMediaTypes = outMediaTypes;
    }

    @Override
    public MediaType deduceFrom( List<AcceptableMediaType> requestMediaTypes )
    {
        if( requestMediaTypes == null || requestMediaTypes.size() == 0 )
            return DEFAULT_MEDIATYPE;

        MediaType found = null;

        for( AcceptableMediaType amt : requestMediaTypes )
        {
            if( _outMediaTypes.isSatisfiedBy( amt ) )
            {
                found = amt;
                break;
            }
        }

        if( found == null )
            return DEFAULT_MEDIATYPE;

        if( found.isWildcardSubtype() && found.isWildcardType() )
            return DEFAULT_MEDIATYPE;

        if( found.isWildcardSubtype() && !found.isWildcardType() )
        {
            if( found.getType().equals( "application" ) )
                return MediaType.APPLICATION_JSON_TYPE;
            else
                return DEFAULT_MEDIATYPE;
        }

        if( !found.isWildcardSubtype() && found.isWildcardType() )
        {
            if( found.getSubtype().equals( "xml" ) )
                return MediaType.TEXT_XML_TYPE;
            else if( found.getSubtype().equals( "json" ) )
                return MediaType.APPLICATION_JSON_TYPE;
            else
                return DEFAULT_MEDIATYPE;
        }

        return found;
    }

    private final SupportedOutgoingMediaTypes _outMediaTypes;

    private final static MediaType DEFAULT_MEDIATYPE = MediaType.APPLICATION_JSON_TYPE;
}
