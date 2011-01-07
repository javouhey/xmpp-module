package com.raverun.im.interfaces.rest;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.raverun.im.interfaces.rest.VersionChecker.Version;
import com.raverun.im.interfaces.rest.support.Either;
import com.raverun.im.interfaces.rest.support.HttpVerb;
import com.raverun.shared.Constants;

public class IMDispatcherServlet extends HttpServlet
{
    @Override
    protected void doHead( HttpServletRequest httpRequest, HttpServletResponse httpResponse )
        throws ServletException, IOException
    {
        doIt( HttpVerb.HEAD, httpRequest, httpResponse );
    }

    @Override
    protected void doPost( HttpServletRequest httpRequest, HttpServletResponse httpResponse )
        throws ServletException, IOException
    {
        doIt( HttpVerb.POST, httpRequest, httpResponse );
    }

    @Override
    protected void doGet( HttpServletRequest httpRequest, HttpServletResponse httpResponse )
        throws ServletException, IOException
    {
        doIt( HttpVerb.GET, httpRequest, httpResponse );
    }

    @Override
    protected void doOptions( HttpServletRequest httpRequest, HttpServletResponse httpResponse )
        throws ServletException, IOException
    {
        doIt( HttpVerb.OPTIONS, httpRequest, httpResponse );
    }

    @Override
    protected void doPut( HttpServletRequest httpRequest,
        HttpServletResponse httpResponse ) throws ServletException, IOException
    {
        doIt( HttpVerb.PUT, httpRequest, httpResponse );
    }

    @Override
    protected void doDelete( HttpServletRequest httpRequest, HttpServletResponse httpResponse )
        throws ServletException, IOException
    {
        doIt( HttpVerb.DELETE, httpRequest, httpResponse );
    }

    public void init( ServletConfig config ) throws ServletException
    {
        super.init( config );
        ServletContext sc = config.getServletContext();
        Injector injector = (Injector) sc
            .getAttribute( Constants.Guice.INJECTOR_APP_CONTEXT_KEY );
        injector.injectMembers( this );
    }

    private final void doIt( HttpVerb verb, HttpServletRequest httpRequest,
        HttpServletResponse httpResponse ) throws IOException
    {
        try
        {
            final Version protocolVersion = getVersion( httpRequest );

            ProtocolProcessor protocolProcessor =_protocolProcessorSelector.selectBasedOn( protocolVersion );
            Either either = protocolProcessor.receive( verb, httpRequest );
            if( !either.isOk() )
            {
                forceExceptionsToOccurEarly( either.getResponse() );
                protocolProcessor.reply( either.getResponse(), httpResponse );
                return;
            }

            RestResponse restResp = _dispatcher.dispatch( either.getRequest(), httpRequest );
            forceExceptionsToOccurEarly( restResp );

            protocolProcessor.reply( restResp, httpResponse );
        }
        catch( Exception e ) // Fault barrier
        {
            _logger.error( "FaultBarrier caught the following fault", e );
            _faultBarrier.handle( httpResponse );
        }
    }

    private final void forceExceptionsToOccurEarly( @Nonnull final RestResponse restResponse ) throws IOException
    {
        if( restResponse.hasBody() )
            restResponse.serialize().getBytes( Constants.Protocol.UTF8 );
    }

    private final Version getVersion( HttpServletRequest httpRequest )
    {
        Version v = _versionChecker.deduce( httpRequest );
        _logger.debug( "deduced version = " + v );

        return v;
    }

    @Inject
    private FaultBarrierHandler _faultBarrier;

    @Inject
    private ResourceDispatcher _dispatcher;
    
    @Inject 
    private VersionChecker _versionChecker;

    @Inject 
    private ProtocolProcessorSelector _protocolProcessorSelector;
    
    private final Logger _logger = Logger.getLogger( IMDispatcherServlet.class );

    private static final long serialVersionUID = 1L;
}
