package com.raverun.im.interfaces.rest.impl;

import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import com.raverun.im.interfaces.rest.SupportedIncomingMediaType;
import com.raverun.shared.Constraint;

/**
 * TODO Change it to use ProtocolUtils.safeMediaTypeFrom(..)
 *
 */
public class DefaultSupportedIncomingMediaType implements
    SupportedIncomingMediaType
{

    @Override
    public boolean isSatisfiedBy( String mimeType )
    {
        if( Constraint.EmptyString.isFulfilledBy( mimeType ))
            return false;

        MediaType mediaType = null;

        try
        {
            int index = mimeType.indexOf( ";" );
            if( index >= 0 )
            {
                String strippedOfLint = mimeType.substring( 0, index );
                if( Constraint.EmptyString.isFulfilledBy( strippedOfLint ))
                    return false;

                mediaType = MediaType.valueOf( strippedOfLint );
                _logger.debug( strippedOfLint.toString() );
            }
            else
                mediaType = MediaType.valueOf( mimeType );
        }
        catch( IllegalArgumentException iae )
        {
            return false;
        }

        if( MediaType.APPLICATION_JSON_TYPE.equals( mediaType ) )
            return true;

        if( MediaType.TEXT_XML_TYPE.equals( mediaType ) )
            return true;

        return false;
    }

    private final Logger _logger = Logger.getLogger( DefaultSupportedIncomingMediaType.class );
}
