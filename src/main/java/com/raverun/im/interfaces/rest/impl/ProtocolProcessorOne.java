package com.raverun.im.interfaces.rest.impl;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.raverun.im.interfaces.rest.ProtocolErrorCode;
import com.raverun.im.interfaces.rest.ProtocolProcessor;
import com.raverun.im.interfaces.rest.ProtocolProcessorDefault;
import com.raverun.im.interfaces.rest.RestResponse;
import com.raverun.im.interfaces.rest.VersionChecker.Version;
import com.raverun.im.interfaces.rest.support.Either;
import com.raverun.im.interfaces.rest.support.HttpVerb;

/**
 * <ul>
 * <li>Currently hardcoded to always return 501 Not Implemented
 * <li>Send back as {@code text/xml} by default unless specified otherwise in the Accept HTTP header
 * </ul>
 *
 * @author Gavin Bong
 */
public class ProtocolProcessorOne implements ProtocolProcessor
{
    @Inject
    public ProtocolProcessorOne( ProtocolProcessorDefault processorDefault )
    {
        _processorDefault = processorDefault;
    }

    @Override
    public Either receive( HttpVerb verb, HttpServletRequest httpRequest )
    {
        _logger.debug( "Handling a request" );
        Either retval = _processorDefault.receive( verb, httpRequest, _version );

        return new Either.Builder( false, retval.getOutMediaType() )
            .response( new DefaultRestResponseError( "unsupported version. Only version 0 is supported currently", ProtocolErrorCode.NotImplemented, HttpServletResponse.SC_NOT_IMPLEMENTED, retval.getOutMediaType(), _version ) )
            .build();

    }//receive

    @Override
    public void reply( RestResponse resp, HttpServletResponse httpResponse )
        throws IOException
    {
        _processorDefault.reply( resp, httpResponse, _version );
    }

    private final ProtocolProcessorDefault _processorDefault;

    /**
     * This protocol processor is of type {@code Version.ONE}
     */
    private final Version _version = Version.ONE;

    private final Logger _logger = Logger.getLogger( ProtocolProcessorOne.class );
}
