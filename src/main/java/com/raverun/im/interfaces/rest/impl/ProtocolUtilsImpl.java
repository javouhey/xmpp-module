package com.raverun.im.interfaces.rest.impl;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

import org.apache.log4j.Logger;

import com.raverun.im.interfaces.rest.ProtocolUtils;
import com.raverun.shared.Common;
import com.raverun.shared.Constraint;
import com.sun.jersey.api.uri.UriComponent;

public class ProtocolUtilsImpl implements ProtocolUtils
{
    /*
     * (non-Javadoc)
     * @see com.raverun.im.interfaces.rest.ProtocolUtils#safeConvertFrom(java.lang.String)
     */
    @Override
    public MediaType safeConvertFrom( String httpContentType )
    {
        if( Constraint.EmptyString.isFulfilledBy( httpContentType ))
            throw new IllegalArgumentException( "httpContentType is invalid" );

        try
        {
            int index = httpContentType.indexOf( ";" );
            if( index >= 0 )
            {
                String strippedOfLint = httpContentType.substring( 0, index );
                if( Constraint.EmptyString.isFulfilledBy( strippedOfLint ))
                    throw new IllegalArgumentException( "httpContentType is invalid" );

                return MediaType.valueOf( strippedOfLint );
            }

            return MediaType.valueOf( httpContentType );
        }
        catch( Exception e )
        {
            throw new IllegalArgumentException( "httpContentType is invalid", e );
        }
    }

    @Override
    public void readFully( InputStream is, byte[] buffer ) throws IOException
    {
        int offset = 0;
        int leftToRead = buffer.length;
        while( leftToRead > 0 )
        {
            int numRead = is.read( buffer, offset, leftToRead );
            if( numRead < 0 )
                throw new EOFException( leftToRead + " remains to be read" );

            leftToRead -= numRead;
            offset += numRead;
        }
    }

    @Override
    public InvokedPath parsePathInfo( String pathInfo )
    {
        if( Constraint.EmptyString.isFulfilledBy( pathInfo ) )
            return NULL_INVOKED_PATH;

        if( !pathInfo.endsWith( "/" ) )
            pathInfo = pathInfo + "/";

        try
        {
            URI bu = UriBuilder.fromUri( pathInfo ).build();
            List<PathSegment> ps = UriComponent.decodePath( bu, true );

            String resource  = ps.get( 0 ).getPath();
            String parameter = ps.get( 1 ).getPath();

            _logger.debug( "pathInfo as URI => " + bu + " [ resource: " + resource + " | parameter: " + parameter + " ]" );

            return new InvokedPathImpl( resource, parameter );
        }
        catch( IllegalArgumentException e )
        {
            return NULL_INVOKED_PATH;
        }
        catch( UriBuilderException e )
        {
            return NULL_INVOKED_PATH;
        }
        catch( java.lang.IndexOutOfBoundsException oobe )
        {
            return NULL_INVOKED_PATH;
        }
    }

    /*
     * (non-Javadoc)
     * @see com.raverun.im.interfaces.rest.ProtocolUtils#parseEntity(java.io.InputStream, int, javax.ws.rs.core.MediaType)
     */
    @Override
    public EntityWrapper parseEntity( InputStream istream, int length, final MediaType mimeType ) throws IOException
    {
        Constraint.NonNullArgument.check( istream, "istream" );
        Constraint.NonNullArgument.check( mimeType, "mimeType" );
        if( length < 0 )
            throw new IllegalArgumentException( "length cannot be negative" );

        String body = Common.EMPTY_STRING;

        try
        {
            if( length > 0 )
            {
                byte[] utf8Bytes = new byte[ length ];
                readFully( istream, utf8Bytes );
                body = new String( utf8Bytes, "UTF-8" );
            }

            _logger.debug( "entity: " + body );

            final String[] hack = new String[] { body };
            return new EntityWrapper() 
            {
                @Override
                public MediaType mimeType()
                {
                    return mimeType;
                }

                @Override
                public String rawEntity()
                {
                    return hack[ 0 ];
                }
            };
        }
        catch( UnsupportedEncodingException e )
        {
            throw new RuntimeException( "1", e );
        }
        catch( IOException e )
        {
            throw e;
        }
    }

    public final static InvokedPath NULL_INVOKED_PATH = new InvokedPath() 
    {
        @Override
        public boolean isInvalid()
        {
            return true;
        }

        @Override
        public String parameter()
        {
            return Common.EMPTY_STRING;
        }

        @Override
        public String resource()
        {
            return Common.EMPTY_STRING;
        }
    };


    public final static EntityWrapper NULL_ENTITY = new EntityWrapper()
    {
        @Override
        public String rawEntity()
        {
            return Common.EMPTY_STRING;
        }

        @Override
        public MediaType mimeType()
        {
            return null;
        }
    };

    class InvokedPathImpl implements InvokedPath
    {
        InvokedPathImpl( String resource, String parameter )
        {
            _parameter = parameter;
            _resource = resource;
        }

        @Override
        public boolean isInvalid()
        {
            _logger.debug( "isInvalid? " + Constraint.EmptyString.isFulfilledBy( _resource ) );
            return ( Constraint.EmptyString.isFulfilledBy( _resource ) );
        }

        @Override
        public String parameter()
        {
            return _parameter;
        }

        @Override
        public String resource()
        {
            return _resource;
        }

        private String _parameter;
        private String _resource;
    }

    private final Logger _logger = Logger.getLogger( ProtocolUtilsImpl.class );
}
