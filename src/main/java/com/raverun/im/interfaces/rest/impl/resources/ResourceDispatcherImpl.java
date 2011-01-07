package com.raverun.im.interfaces.rest.impl.resources;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.raverun.im.interfaces.rest.DeleteWithEntityBodySupport;
import com.raverun.im.interfaces.rest.ProtocolErrorCode;
import com.raverun.im.interfaces.rest.Resource;
import com.raverun.im.interfaces.rest.ResourceDispatcher;
import com.raverun.im.interfaces.rest.ResourceRegistry;
import com.raverun.im.interfaces.rest.RestRequest;
import com.raverun.im.interfaces.rest.RestResponse;
import com.raverun.im.interfaces.rest.SessionAttribute;
import com.raverun.im.interfaces.rest.SessionAwareResource;
import com.raverun.im.interfaces.rest.SessionUtils;
import com.raverun.im.interfaces.rest.SessionUtilsFactory;
import com.raverun.im.interfaces.rest.impl.DefaultRestResponseError;
import com.raverun.im.interfaces.rest.impl.DefaultRestResponseSuccess;

public class ResourceDispatcherImpl implements ResourceDispatcher
{
    @Inject
    public ResourceDispatcherImpl( ResourceRegistry registry, SessionUtilsFactory sessionFactory )
    {
        _registry       = registry;
        _sessionFactory = sessionFactory;
    }

    @Override
    public RestResponse dispatch( RestRequest request, HttpServletRequest httpRequest )
    {
        try
        {
            boolean needsSession = true;
            Resource resource = _registry.get( request.invokedPath().resource() );
            EitherResource either = null;

            if( resource.respondsTo( request.verb() ) )
            {
                final SessionAttribute sessAttribute = resource.sessionAttributeFor( request.verb() );
                final SessionUtils sessionUtils = _sessionFactory.create( httpRequest );

                HttpSession session = null;

                /**
                 * assumption #1: that we have placed a Cookie for JSESSIONID in {@link RestRequest#sessionAttributes()}
                 */
                switch( sessAttribute )
                {
                case REQUIRED:
                    if( !httpRequest.isRequestedSessionIdFromCookie() ||
                        !httpRequest.isRequestedSessionIdValid() )
                    {
                        return new DefaultRestResponseError( "The IMSession has expired or was invalidated", 
                            ProtocolErrorCode.InvalidIMSession, HttpServletResponse.SC_FORBIDDEN, 
                            request.outgoing(), request.version() );
                    }

                    session = httpRequest.getSession( false );
                    session.getCreationTime(); // may throw IllegalStateException
                    break;

                case REQUIRES_NEW:
                    break;

                case NEVER:
                    needsSession = false;
                    break;

                default:
                    throw new AssertionError( "impossible" );

                }

           // Precondition: we have a valid HttpSession at this point. TODO What do we do with it ?

                switch( request.verb() )
                {
                case PUT:
                    //either = resource.put( request.incoming(), request.outgoing(), request.entity() );
                    either = doPut( resource, needsSession, request, sessionUtils );
                    break;

                case GET:
                    //either = resource.get( request.invokedPath().parameter(), request.outgoing() );
                    either = doGet( resource, needsSession, request, sessionUtils );
                    break;

                case POST:
                    //either = resource.post(null, null, null);
                    either = doPost( resource, needsSession, request, sessionUtils );
                    break;

                case DELETE:
                    //either = resource.delete( request.invokedPath().parameter(), request.outgoing() );
                    either = doDelete( resource, needsSession, request, sessionUtils );
                    break;

                case OPTIONS:
                    either = resource.options();
                    break;

                case HEAD:
                    either = resource.head();
                    break;

                default:
                    throw new AssertionError( "impossible" );
                }

                if( either == null )
                    return new DefaultRestResponseError( "1We encountered an internal error. Please try again", ProtocolErrorCode.InternalError, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, request.outgoing(), request.version() );

                if( either.isOk() )
                {
                    DefaultRestResponseSuccess.Builder builder = new DefaultRestResponseSuccess.Builder( request.outgoing(), either.httpStatusCode() ).httpHeaders( either.headers() );

                    if( either.body() != null )
                        builder.body( either.body() );

                    return builder.build();
                }
                else
                    return new DefaultRestResponseError( either.errorMessage(), either.errorCode(), either.httpStatusCode(), request.outgoing(), request.version(), either.headers() );
            }
            else
            {
                return new DefaultRestResponseError( "The specified method is not allowed against this resource", ProtocolErrorCode.MethodNotAllowed, HttpServletResponse.SC_METHOD_NOT_ALLOWED, request.outgoing(), request.version() );
            }
        }
        catch( IllegalStateException ise )
        {
            // TODO We're expecting this if messages are sent to an invalidated HttpSession. Is it always the case ?
            return new DefaultRestResponseError( "The IMSession has expired or was invalidated", ProtocolErrorCode.InvalidIMSession, HttpServletResponse.SC_FORBIDDEN, request.outgoing(), request.version() );
        }
        catch( UnsupportedOperationException uoe )
        {
            return new DefaultRestResponseError( "This combination is under development", ProtocolErrorCode.UnderConstruction, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, request.outgoing(), request.version() );
        }

    }

    /**
     * Switches between a session-aware and session-less HTTP PUT
     *
     * @param needsSession - true if session-aware, false otherwise
     */
    private final EitherResource doPut( Resource resource, boolean needsSession, RestRequest restRequest, SessionUtils sessUtils )
    {
        if( needsSession )
            return ((SessionAwareResource)resource).put( restRequest.invokedPath().parameter(), null, restRequest.incoming(), 
                restRequest.outgoing(), restRequest.entity(), restRequest.sessionAttributes(), sessUtils );
        else
            return resource.put( restRequest.incoming(), restRequest.outgoing(), restRequest.entity() );
    }

    /**
     * Switches between a session-aware and session-less HTTP GET
     *
     * @param needsSession - true if session-aware, false otherwise
     */
    private final EitherResource doGet( Resource resource, boolean needsSession, RestRequest restRequest, SessionUtils sessUtils )
    {
        if( needsSession )
            return ((SessionAwareResource)resource).get( restRequest.invokedPath().parameter(), null,
                restRequest.outgoing(), restRequest.sessionAttributes(), sessUtils );
        else
            return resource.get( restRequest.invokedPath().parameter(), restRequest.outgoing() );
    }

    /**
     * Switches between a session-aware and session-less HTTP POST
     *
     * @param needsSession - true if session-aware, false otherwise
     */
    private final EitherResource doPost( Resource resource, boolean needsSession, RestRequest restRequest, SessionUtils sessUtils )
    {
        if( needsSession )
            return ((SessionAwareResource)resource).post( restRequest.invokedPath().parameter(), null, restRequest.incoming(), 
                restRequest.outgoing(), restRequest.entity(), restRequest.sessionAttributes(), sessUtils );
        else
            return resource.post( restRequest.incoming(), restRequest.outgoing(), restRequest.entity() );
    }

    /**
     * Switches between a session-aware and session-less HTTP DELETE
     *
     * @param needsSession - true if session-aware, false otherwise
     */
    private final EitherResource doDelete( Resource resource, boolean needsSession, RestRequest restRequest, SessionUtils sessUtils )
    {
        if( needsSession )
        {
            if( resource instanceof DeleteWithEntityBodySupport )
                return ((DeleteWithEntityBodySupport)resource).delete( restRequest.invokedPath().parameter(), null, restRequest.incoming(), 
                    restRequest.outgoing(), restRequest.entity(), restRequest.sessionAttributes(), sessUtils );
            else
                return ((SessionAwareResource)resource).delete( restRequest.invokedPath().parameter(), null,
                    restRequest.outgoing(), restRequest.sessionAttributes(), sessUtils );
        }
        else
            return resource.delete( restRequest.invokedPath().parameter(), restRequest.outgoing() );
    }

    private final ResourceRegistry _registry;
    private final SessionUtilsFactory _sessionFactory;

    private final Logger _logger = Logger.getLogger( ResourceDispatcherImpl.class );
}
