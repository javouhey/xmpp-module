package com.raverun.im.interfaces.rest;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.MediaType;

public interface ProtocolUtils
{
    void readFully( InputStream istream, byte[] buffer ) throws IOException;

    /**
     * @throws IOException
     * @throws IllegalArgumentException when length < 0, istream/mimeType is null
     */
    EntityWrapper parseEntity( InputStream istream, int length, MediaType mimeType ) throws IOException;

    InvokedPath parsePathInfo( String pathInfo );

    /**
     * @throws IllegalArgumentException if {@code httpContentType} is invalid
     */
    MediaType safeConvertFrom( String httpContentType );

    interface InvokedPath
    {
        boolean isInvalid();
        String resource();
        String parameter();
    }

    interface EntityWrapper
    {
        String rawEntity();
        MediaType mimeType();
    }
}
