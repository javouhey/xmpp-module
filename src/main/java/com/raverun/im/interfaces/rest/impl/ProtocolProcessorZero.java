package com.raverun.im.interfaces.rest.impl;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.raverun.im.interfaces.rest.ProtocolProcessor;
import com.raverun.im.interfaces.rest.ProtocolProcessorDefault;
import com.raverun.im.interfaces.rest.RestResponse;
import com.raverun.im.interfaces.rest.VersionChecker.Version;
import com.raverun.im.interfaces.rest.support.Either;
import com.raverun.im.interfaces.rest.support.HttpVerb;

public class ProtocolProcessorZero implements ProtocolProcessor
{
    @Inject
    public ProtocolProcessorZero( ProtocolProcessorDefault processorDefault )
    {
        _processorDefault = processorDefault;
    }

    @Override
    public Either receive( HttpVerb verb, HttpServletRequest httpRequest )
    {
        _logger.debug( "Handling a request" );
        return _processorDefault.receive( verb, httpRequest, _version );
    }

    @Override
    public void reply( RestResponse resp, HttpServletResponse httpResponse )
        throws IOException
    {
        _processorDefault.reply( resp, httpResponse, _version );
    }

    private final ProtocolProcessorDefault _processorDefault;

    /**
     * This protocol processor is of type {@code Version.ZERO}
     */
    private final Version _version = Version.ZERO;

    private final Logger _logger = Logger.getLogger( ProtocolProcessorZero.class );
}
