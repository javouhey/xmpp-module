package com.raverun.im.interfaces.rest.impl;

import javax.ws.rs.core.MediaType;

import com.raverun.im.interfaces.rest.SupportedOutgoingMediaTypes;
import com.sun.jersey.core.header.AcceptableMediaType;

public class DefaultSupportedOutgoingMediaTypes implements
    SupportedOutgoingMediaTypes
{
    private MediaType[] acceptedTypes = new MediaType[] {
        MediaType.APPLICATION_JSON_TYPE, 
        MediaType.TEXT_XML_TYPE
    };

    @Override
    public boolean isSatisfiedBy( AcceptableMediaType mediaType )
    {
        if( mediaType == null )
            return false;

        boolean retval = false;
        for( MediaType mt : acceptedTypes )
        {
            if( mt.isCompatible( mediaType ))
            {
                retval = true;
                break;
            }
        }

        return retval;
    }

}
