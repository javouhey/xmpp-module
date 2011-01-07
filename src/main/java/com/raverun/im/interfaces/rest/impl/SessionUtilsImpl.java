package com.raverun.im.interfaces.rest.impl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import net.jcip.annotations.GuardedBy;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.raverun.im.domain.IMSession;
import com.raverun.im.domain.IMSessionException;
import com.raverun.im.infrastructure.system.IdMutexProvider;
import com.raverun.im.infrastructure.system.Mutex;
import com.raverun.im.interfaces.rest.SessionUtils;
import com.raverun.shared.Common;
import com.raverun.shared.Constraint;

public class SessionUtilsImpl implements SessionUtils
{
    @AssistedInject
    public SessionUtilsImpl( IdMutexProvider mutexProvider, @Assisted HttpServletRequest httpRequest )
    {
        Constraint.NonNullArgument.check( httpRequest, "httpRequest" );
        _httpRequest   = httpRequest;
        _mutexProvider = mutexProvider;
    }

    @Override
    public String buildLocation( String key )
    {
        if( Constraint.EmptyString.isFulfilledBy( key ))
            return Common.EMPTY_STRING;

        _logger.debug( "getRequestURL() " +_httpRequest.getRequestURL() );
        _logger.debug( "getContextPath() " + _httpRequest.getContextPath() );
        _logger.debug( "getLocalAddr() " + _httpRequest.getLocalAddr() );
        _logger.debug( "getServerName() " + _httpRequest.getServerName() );
        _logger.debug( "getServletPath()" + _httpRequest.getServletPath() );

        return _httpRequest.getRequestURL().append( "/" ).append( key ).toString();
    }

    @Override
    public void invalidateThisSessionForUser( String key )
    {
        if( Constraint.EmptyString.isFulfilledBy( key ) )
            throw new IllegalArgumentException( "key must be specified" );

        try
        {
            HttpSession session = _httpRequest.getSession( false );
            if( session == null )
                return;

            final Mutex mutex = _mutexProvider.getMutex( session.getId() ); // may throw IllegalStateException
            synchronized( mutex )
            {
                session.invalidate(); // may throw IllegalStateException
            }
        }
        catch( IllegalStateException ise )
        {
            _logger.error( "session already invalidated. noop", ise );
        }
    }

    /**
     * Retrieves the {@code IMSession} for this request scope
     *
     * @param key - non nullable value
     * @return a null to mean that no session is associated with this request
     * @throws InvalidatedHttpSessionException if the session was invalidated
     */
    @GuardedBy("_mutexProvider.getMutex( sessionId )")
    @Override
    public IMSession getSessionAttribute( String key )
    {
        if( Constraint.EmptyString.isFulfilledBy( key ) )
            return null;

        try
        {
            return ((IMSession)getAttribute( key ));
        }
        catch( IllegalStateException ise )
        {
            throw new InvalidatedHttpSessionException( "HttpSession is already invalidated", ise );
        }
    }

    @GuardedBy("_mutexProvider.getMutex( sessionId )")
    @Override
    public NewSessionResult createNewSessionAndSetAttribute( String key, IMSession imSession, boolean autoLogin )
    {
        if( Constraint.EmptyString.isFulfilledBy( key ) )
            return new NewSessionResultImpl( Common.EMPTY_STRING, false, null, Common.EMPTY_STRING );

        Constraint.NonNullArgument.check( imSession, "imSession" );

        HttpSession httpSession = _httpRequest.getSession( true );
        boolean imSessionStarted = false; boolean exceptionEncountered = false;
        try
        {
            NewSessionResult partialResult = null;
            final String httpSessionId = httpSession.getId();
            final Mutex mutex = _mutexProvider.getMutex( httpSessionId ); // may throw IllegalStateException

            synchronized( mutex )
            {
                // TODO what happens if it fails to login ?
                partialResult = imSession.start( autoLogin ); // may block for a while
                imSessionStarted = true;
                httpSession.setAttribute( key, imSession );
            }
            _logger.debug( "Added IMSession for user " + imSession.userId() + " to HttpSession " + httpSession.getId() );
            return new NewSessionResultImpl( httpSessionId, true, (JSONObject)partialResult.loginReport(), partialResult.message() );
        }
        catch( IllegalStateException iae )
        {
            exceptionEncountered = true;
            _logger.error( "HttpSession invalidated", iae );
            return new NewSessionResultImpl( Common.EMPTY_STRING, false, null, Common.EMPTY_STRING );
        }
        catch( IMSessionException imse )
        {
            exceptionEncountered = true;
            _logger.error( imse.getMessage(), imse );
            return new NewSessionResultImpl( Common.EMPTY_STRING, false, null, Common.EMPTY_STRING );
        }
        finally
        {
            if( imSessionStarted && exceptionEncountered )
            {
                // TODO if IMSession is connected, but we received errors setting it to session, disconnect ?
                _logger.debug( "IMSession for user " + imSession.userId() + " is started BUT was not added to HttpSession" );
                imSession.stop();
            }
        }
    }

    /**
     * @throws IllegalAccessException if session was invalidated during time of invocation 
     * @return either null or non-null
     */
    private final Object getAttribute( final String key ) throws IllegalStateException
    {
        HttpSession session = _httpRequest.getSession( false );
        if( session == null )
            return null;

        System.out.println( "session.getId=" + session.getId() );
        final Mutex mutex = _mutexProvider.getMutex( session.getId() ); // may throw IllegalStateException
        synchronized( mutex )
        {
            return session.getAttribute( key ); // may throw IllegalStateException
        }
    }

    public class NewSessionResultImpl implements NewSessionResult
    {
        public NewSessionResultImpl( String httpSessionId, boolean ok, JSONObject loginReport, String message )
        {
            _httpSessionId = httpSessionId; _ok = ok; _loginReport = loginReport; _message = message;
        }

        @Override public String httpSessionId() { return _httpSessionId; }
        @Override public boolean ok() { return _ok; }
        @Override public Object loginReport() { return _loginReport; }
        @Override public String message() { return _message; }

        private String _httpSessionId;
        private boolean _ok;
        private String _message = Common.EMPTY_STRING;
        private JSONObject _loginReport;
    }

    private final HttpServletRequest _httpRequest;
    private final IdMutexProvider _mutexProvider;
    private final Logger _logger = Logger.getLogger( SessionUtilsImpl.class );
}
