package com.raverun.im.interfaces.rest.impl;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Syntax;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import com.raverun.im.interfaces.rest.FaultBarrierHandler;
import com.raverun.shared.Constants;

/**
 * Generates a generic error message in JSON
 * 
 * @author Gavin Bong
 */
public class FaultBarrierHandlerImpl implements FaultBarrierHandler
{
    @Syntax("JSON")
    @Override
    public void handle( @Nonnull HttpServletResponse httpResponse ) throws IOException
    {
        if( httpResponse == null )
            throw new AssertionError( "HttpServletResponse should never be null" );

        httpResponse.setStatus( 500 );
        httpResponse.setContentType( MediaType.APPLICATION_JSON );
        httpResponse.setCharacterEncoding( Constants.Protocol.UTF8 );

        httpResponse.addHeader( "Cache-Control", "no-cache" );
        httpResponse.addHeader( "Connection", "close" );

        byte[] ba = DEFAULT_JSON.getBytes( Constants.Protocol.UTF8 );
        httpResponse.setContentLength( ba.length );

        ServletOutputStream sos = httpResponse.getOutputStream();
        sos.write( ba );
        sos.flush();
    }

    private static final String DEFAULT_JSON = "{\"stat\":\"fail\", \"errorcode\":\"InternalError\", \"message\":\"system error\"}";
}
