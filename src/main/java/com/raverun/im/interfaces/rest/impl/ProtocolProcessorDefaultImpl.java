package com.raverun.im.interfaces.rest.impl;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.raverun.im.interfaces.rest.OutgoingMediaTypeSelector;
import com.raverun.im.interfaces.rest.ProtocolBodyParserSelector;
import com.raverun.im.interfaces.rest.ProtocolBodyParserSelectorFactory;
import com.raverun.im.interfaces.rest.ProtocolErrorCode;
import com.raverun.im.interfaces.rest.ProtocolProcessorDefault;
import com.raverun.im.interfaces.rest.ProtocolUtils;
import com.raverun.im.interfaces.rest.ResourceRegistry;
import com.raverun.im.interfaces.rest.RestResponse;
import com.raverun.im.interfaces.rest.SupportedIncomingMediaType;
import com.raverun.im.interfaces.rest.VersionChecker.Version;
import com.raverun.im.interfaces.rest.support.Either;
import com.raverun.im.interfaces.rest.support.EntityTooSmallException;
import com.raverun.im.interfaces.rest.support.HttpVerb;
import com.raverun.im.interfaces.rest.support.RequestTimeoutException;
import com.raverun.shared.Constants;
import com.sun.jersey.core.header.AcceptableMediaType;
import com.sun.jersey.core.header.reader.HttpHeaderReader;

public class ProtocolProcessorDefaultImpl implements ProtocolProcessorDefault
{
    @Inject
    public ProtocolProcessorDefaultImpl( OutgoingMediaTypeSelector mediaTypeSelector, 
        SupportedIncomingMediaType inMediaType, ProtocolUtils protocolUtils, 
        ProtocolBodyParserSelectorFactory bodyParserSelectorFactory,
        ResourceRegistry resourceRegistry )
    {
        _mediaTypeSelector         = mediaTypeSelector;
        _inMediaType               = inMediaType;
        _protocolUtils             = protocolUtils;
        _resourceRegistry          = resourceRegistry;
        _bodyParserSelectorFactory = bodyParserSelectorFactory;
    }

    @Override
    public Either receive( HttpVerb verb, HttpServletRequest httpRequest, Version version )
    {
        List<AcceptableMediaType> requestMediaTypes = new ArrayList<AcceptableMediaType>(0);

    // 1) select a requested outgoing media type
        try
        {
            requestMediaTypes = HttpHeaderReader.readAcceptMediaType( httpRequest.getHeader( "Accept" ) );
        }
        catch( ParseException e )
        {
            return new Either.Builder( false, DEFAULT_OUT_MIMETYPE ).response( new DefaultRestResponseError( "", ProtocolErrorCode.InvalidAcceptHttpHeader, HttpServletResponse.SC_NOT_ACCEPTABLE, DEFAULT_OUT_MIMETYPE, version ) ).build(); // 406
        }

        final MediaType outgoingMediaType = _mediaTypeSelector.deduceFrom( requestMediaTypes );
        _logger.debug( "mediatype: outgoing " + outgoingMediaType.toString() );

    // 2) check for commonly known invalid URIs
        String pathInfo = httpRequest.getPathInfo();
        _logger.debug( "pathInfo: " + pathInfo );
        if( pathInfo == null || pathInfo.equals("/") ) // send 404
            return new Either.Builder( false, outgoingMediaType ).response( new DefaultRestResponseError( "", ProtocolErrorCode.InvalidResource, HttpServletResponse.SC_NOT_FOUND, outgoingMediaType, version ) ).build();

    // 2.1) extract the resource & parameters
        ProtocolUtils.InvokedPath invokedPath = _protocolUtils.parsePathInfo( pathInfo );
        if( invokedPath.isInvalid() || !_resourceRegistry.exists( invokedPath.resource() ) )
            return new Either.Builder( false, outgoingMediaType ).response( new DefaultRestResponseError( "", ProtocolErrorCode.InvalidURI, HttpServletResponse.SC_BAD_REQUEST, outgoingMediaType, version ) ).build();

    // 3) check for valid incoming type
        int contentLength = httpRequest.getContentLength();
        if( verb == HttpVerb.POST || verb == HttpVerb.PUT || verb == HttpVerb.DELETE )
        {
            /*
             * Allow DELETEs to have zero entity body
             */
            if( verb == HttpVerb.DELETE && ( contentLength == -1 || contentLength == 0 ) )
            {
                _logger.debug( "allowing DELETE on " + invokedPath.toString() + " with empty entity body" );
                return new Either.Builder( true, outgoingMediaType ).request( new DefaultRestRequest( invokedPath, version, null, outgoingMediaType, verb, "{}" ) ).build();
            }

            _logger.debug( "mediatype: incoming " + httpRequest.getContentType() );
            if( ! _inMediaType.isSatisfiedBy( httpRequest.getContentType() ) )
                return new Either.Builder( false, outgoingMediaType ).response( new DefaultRestResponseError( "Supported types: application/json text/xml", ProtocolErrorCode.InvalidContentType, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, outgoingMediaType, version ) ).build();

        // 3.1) Read the included entity
            switch( contentLength )
            {
            case -1:
                return new Either.Builder( false, outgoingMediaType )
                    .response( new DefaultRestResponseError( "Content-Length is required for PUT/POST methods", ProtocolErrorCode.MissingContentLength, HttpServletResponse.SC_LENGTH_REQUIRED, outgoingMediaType, version ) )
                    .build();

            case 0:
                return new Either.Builder( false, outgoingMediaType ).response( new DefaultRestResponseError( "Request body is empty", ProtocolErrorCode.MissingRequestBodyError , HttpServletResponse.SC_BAD_REQUEST, outgoingMediaType, version ) ).build();

            default:
                break;
            }

            try
            {
                ProtocolBodyParserSelector selector = _bodyParserSelectorFactory.create( 
                    _protocolUtils.safeConvertFrom( httpRequest.getContentType() ) );

                ProtocolUtilsImpl.EntityWrapper entity = (ProtocolUtilsImpl.EntityWrapper)selector.choose().parse( httpRequest.getInputStream(), contentLength );

                return new Either.Builder( true, outgoingMediaType ).request( new DefaultRestRequest( invokedPath, version, _protocolUtils.safeConvertFrom( httpRequest.getContentType() ), outgoingMediaType, verb, entity.rawEntity() ) ).build();
            }
            catch( EntityTooSmallException etse )
            {
                return new Either.Builder( false, outgoingMediaType ).response( new DefaultRestResponseError( etse.getMessage(), ProtocolErrorCode.IncompleteBody, HttpServletResponse.SC_BAD_REQUEST, outgoingMediaType, version ) ).build();
            }
            catch( RequestTimeoutException rtoe )
            {
                return new Either.Builder( false, outgoingMediaType ).response( new DefaultRestResponseError( rtoe.getMessage(), ProtocolErrorCode.RequestTimeout, HttpServletResponse.SC_BAD_REQUEST, outgoingMediaType, version ) ).build();
            }
            catch( IOException ioe )
            {
                _logger.error( "problem parsing entity", ioe );
            }
            catch( IllegalArgumentException iae )
            {
                throw new AssertionError( "content type is legit. Should never come here" );
            }
            catch( RuntimeException rte )
            {
                _logger.error( "problem parsing entity", rte );
            }

            return new Either.Builder( false, outgoingMediaType ).response( new DefaultRestResponseError( "We encountered problems reading from or writing to you. Please try again", ProtocolErrorCode.CommunicationError, HttpServletResponse.SC_BAD_REQUEST, outgoingMediaType, version ) ).build();
        }
        else
        {
            return new Either.Builder( true, outgoingMediaType ).request( new DefaultRestRequest( invokedPath, version, null, outgoingMediaType, verb, null ) ).build();
        }//if
    }

    @Override
    public void reply( RestResponse resp, HttpServletResponse httpResponse, Version version ) throws IOException
    {
        if( resp == null || httpResponse == null )
            return;

        httpResponse.setStatus( resp.httpStatusCode() );
        httpResponse.setContentType( resp.type().toString() );
        httpResponse.setCharacterEncoding( Constants.Protocol.UTF8 );

        httpResponse.addHeader( "Cache-Control", "no-cache" );
        resp.spitOutHttpHeaders( httpResponse );

        if( resp.hasBody() )
        {
            byte[] ba = resp.serialize().getBytes( Constants.Protocol.UTF8 );

            httpResponse.setContentLength( ba.length );

            ServletOutputStream sos = httpResponse.getOutputStream();
            sos.write( ba );
            sos.flush();
        }
    }

    private void printAccept( List<AcceptableMediaType> requestMediaTypes )
    {
        StringBuilder bd = new StringBuilder();
        for( AcceptableMediaType mt : requestMediaTypes )
        {
            bd.append( mt.getType() );
            bd.append( " / " );
            bd.append( mt.getSubtype() );
            bd.append( " -- " );
        }
        _logger.debug( bd.toString() );
    }

    private final ProtocolBodyParserSelectorFactory _bodyParserSelectorFactory;
    private final OutgoingMediaTypeSelector _mediaTypeSelector;
    private final SupportedIncomingMediaType _inMediaType;
    private final ResourceRegistry _resourceRegistry;
    private final ProtocolUtils _protocolUtils;

    private final MediaType DEFAULT_OUT_MIMETYPE = MediaType.TEXT_XML_TYPE;

    private final Logger _logger = Logger.getLogger( ProtocolProcessorDefaultImpl.class );
}
