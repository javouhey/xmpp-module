package com.raverun.im.interfaces.rest.impl;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;

import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.raverun.im.interfaces.rest.ProtocolBodyParser;
import com.raverun.im.interfaces.rest.ProtocolUtils;
import com.raverun.im.interfaces.rest.ProtocolUtils.EntityWrapper;
import com.raverun.im.interfaces.rest.support.EntityTooSmallException;
import com.raverun.im.interfaces.rest.support.RequestTimeoutException;

public class ProtocolBodyParserJSON implements ProtocolBodyParser
{

    @Override
    public Object parse( InputStream is, int contentLength )
    {
        try
        {
            EntityWrapper wrapper = _protocolUtils.parseEntity( is, contentLength, MIME_TYPE );
            _logger.debug( "raw: " + wrapper.rawEntity() );

            // TODO temporary. Need to return a JSON object or a Map<String, Object>
            return wrapper;
        }
        catch( IOException ioe )
        {
            _logger.error( "Fail to successfully read from the submitted HTTP PUT or POST", ioe );

            if( ioe instanceof EOFException )
                throw new EntityTooSmallException( "You did not provide the number of bytes specified by the Content-Length HTTP Header", ioe );

            if( ioe instanceof SocketTimeoutException )
                throw new RequestTimeoutException( "Your socket connection to the server was not read from or written to within the timeout period", ioe);

            throw new RuntimeException( ioe );
        }
    }

    @Inject
    public ProtocolBodyParserJSON( ProtocolUtils protocolUtils )
    {
        _protocolUtils = protocolUtils;
    }
    
    private final ProtocolUtils _protocolUtils;
    private final Logger _logger = Logger.getLogger( ProtocolBodyParserJSON.class );
    private final MediaType MIME_TYPE = MediaType.APPLICATION_JSON_TYPE;
}
