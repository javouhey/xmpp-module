package com.raverun.im.interfaces.rest.impl.resources.xmpp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.log4j.Logger;
import org.javasimon.Split;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.google.common.base.Predicate;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.raverun.im.domain.IMIdentity;
import com.raverun.im.domain.IMSession;
import com.raverun.im.domain.IMSessionException;
import com.raverun.im.domain.IMUserSettingFactory2;
import com.raverun.im.domain.InvalidIMIdentityException;
import com.raverun.im.domain.IMSession.ExistingSessionResult;
import com.raverun.im.domain.impl.IMIdentityImpl;
import com.raverun.im.infrastructure.persistence.TransportMapper;
import com.raverun.im.infrastructure.system.JavaSimonTimer;
import com.raverun.im.interfaces.rest.AbstractSessionAwareResource;
import com.raverun.im.interfaces.rest.DeleteWithEntityBodySupport;
import com.raverun.im.interfaces.rest.ProtocolErrorCode;
import com.raverun.im.interfaces.rest.SessionAttribute;
import com.raverun.im.interfaces.rest.SessionUtils;
import com.raverun.im.interfaces.rest.impl.InvalidatedHttpSessionException;
import com.raverun.im.interfaces.rest.impl.resources.EitherResource;
import com.raverun.im.interfaces.rest.impl.resources.SharedEntityConstants;
import com.raverun.im.interfaces.rest.impl.resources.XMPPGatewayParser;
import com.raverun.im.interfaces.rest.impl.resources.XMPPGatewayParser.XMPPGatewayParseResult;
import com.raverun.im.interfaces.rest.impl.resources.transport.BulkSigninSuccess;
import com.raverun.im.interfaces.rest.support.HttpVerb;
import com.raverun.shared.Common;
import com.raverun.shared.Constraint;

/**
 * Handles bulk sign ins & sign outs
 *
 * @author Gavin Bong
 */
public class XMPPGatewayResource extends AbstractSessionAwareResource
    implements DeleteWithEntityBodySupport
{

    /*
     * (non-Javadoc)
     * @see com.raverun.im.interfaces.rest.AbstractSessionAwareResource#delete(java.lang.String, javax.ws.rs.core.MultivaluedMap, javax.ws.rs.core.MediaType, java.util.Map, com.raverun.im.interfaces.rest.SessionUtils)
     */
    @Override
    public EitherResource delete( String key,
        MultivaluedMap<String, String> queryParams, MediaType outType,
        Map<String, Object> sessionAttributes, SessionUtils session )
    {
        _logger.debug( "delete.session" );
        return new EitherResource.Builder( false, 501 )
            .error( "Please provide an entity body", ProtocolErrorCode.NotImplemented )
            .build();
    }

    /*
     * (non-Javadoc)
     * @see com.raverun.im.interfaces.rest.AbstractSessionAwareResource#get(java.lang.String, javax.ws.rs.core.MultivaluedMap, javax.ws.rs.core.MediaType, java.util.Map, com.raverun.im.interfaces.rest.SessionUtils)
     */
    @Override
    public EitherResource get( String key,
        MultivaluedMap<String, String> queryParams, MediaType outType,
        Map<String, Object> sessionAttributes, SessionUtils session )
    {
        _logger.debug( "get.session" );
        return NOT_IMPLEMENTED;
    }

    // Not used
    @Override
    public boolean invalidateSessionAfterDelete()
    {
        return false;
    }

    @Override
    public EitherResource options()
    {
        _logger.debug( "options" );
        return new EitherResource.Builder( true, 200 )
            .httpHeaders( _httpHeaders )
            .build();
    }

    @Override
    public String path()
    {
        return PATH;
    }

    /*
     * (non-Javadoc)
     * @see com.raverun.im.interfaces.rest.AbstractSessionAwareResource#post(java.lang.String, javax.ws.rs.core.MultivaluedMap, javax.ws.rs.core.MediaType, javax.ws.rs.core.MediaType, java.lang.Object, java.util.Map, com.raverun.im.interfaces.rest.SessionUtils)
     */
    @Override
    public EitherResource post( String key,
        MultivaluedMap<String, String> queryParams, MediaType inType,
        MediaType outType, Object entity,
        Map<String, Object> sessionAttributes, SessionUtils session )
    {
        _logger.debug( "post.session" );

        if( Constraint.EmptyString.isFulfilledBy( key ) )
        {
            return new EitherResource.Builder( false, 400 ).
                error( ERRORMSG_MISSING_USER, ProtocolErrorCode.UserNotSpecified ).build();
        }

        if( !String.class.isAssignableFrom( entity.getClass() ))
            throw new IllegalArgumentException( "Currently we expect parameter {@code param} to be of type String" );

        if( ! _userExistPredicate.apply( key ) )
        {
            return new EitherResource.Builder( false, 404 ).
                error( "User {" + key + "} is invalid", ProtocolErrorCode.NoSuchUser ).build();
        }

        if( inType.equals( MediaType.APPLICATION_JSON_TYPE ) )
        {
            final Split split = _timer.start( "transport.post" );

            // (1) Retrieve the JSON entity
            XMPPGatewayParseResult parsedResult = null;
            try
            {
                parsedResult = _xmppParser.parse( (String)entity );
            }
            catch( JSONException jsone )
            {
                _logger.error( Common.EMPTY_STRING, jsone );
                return MALFORMED_JSON_ERROR;
            }

            // Nothing to process, so short circuit
            if( parsedResult.list().size() == 0 )
            {
                return new EitherResource.Builder( true, 200 )
                    .body( new XMPPDeleteSuccess( "Deleted 0 accounts", outType ) )
                    .httpHeaders( seedHttpHeader( split.stop() ) )
                    .build();
            }

            switch( parsedResult.command() )
            {
            case DELETE:
                try
                {
                    IMSession imSession = session.getSessionAttribute( SessionUtils.SESSION_ATTRIBUTE_IM );
                    if( imSession == null )
                    {
                        return new EitherResource.Builder( false, 403 ).
                            error( "No Session associated with this request", ProtocolErrorCode.MissingIMSession ).build();
                    }

                    ExistingSessionResult result = imSession.signOutService( parsedResult.listAsIdentities() );
                    return new EitherResource.Builder( true, 200 )
                        .body( new XMPPDeleteSuccess( result.message(), outType ) )
                        .httpHeaders( seedHttpHeader( split.stop() ) )
                        .build();
                }
                catch( InvalidatedHttpSessionException ihse )
                {
                    _logger.error( ihse );
                    return new EitherResource.Builder( false, 403 )
                        .error( ihse.getMessage(), ProtocolErrorCode.InvalidIMSession )
                        .httpHeaders( seedHttpHeader( split.stop() ) )
                        .build();
                }
                catch( IMSessionException imsesse )
                {
                    _logger.error( imsesse );
                    return new EitherResource.Builder( false, 500 )
                        .error( imsesse.getMessage(), ProtocolErrorCode.InternalError )
                        .httpHeaders( seedHttpHeader( split.stop() ) )
                        .build();
                }

            default:
                split.stop();
                break;
            }//switch
        }//if-json

        // XML --------------
        if( inType.equals( MediaType.TEXT_XML_TYPE ) )
        {
            // TODO
            throw new UnsupportedOperationException( "implement text/xml processing" );
        }

        throw new AssertionError( "Only text/xml and application/json supported. Should not come here" );
    }

    /**
     * Bulk sign ins
     */
    @Override
    public EitherResource put( String key,
        MultivaluedMap<String, String> queryParams, MediaType inType,
        MediaType outType, Object entity,
        Map<String, Object> sessionAttributes, SessionUtils session )
    {
        _logger.debug( "put.session" );

        if( Constraint.EmptyString.isFulfilledBy( key ) )
        {
            return new EitherResource.Builder( false, 400 ).
                error( ERRORMSG_MISSING_USER, ProtocolErrorCode.UserNotSpecified ).build();
        }

        if( !String.class.isAssignableFrom( entity.getClass() ))
            throw new IllegalArgumentException( "Currently we expect parameter {@code param} to be of type String" );

        if( ! _userExistPredicate.apply( key ) )
        {
            return new EitherResource.Builder( false, 404 ).
                error( "", ProtocolErrorCode.NoSuchUser ).build();
        }

        // JSON --------------
        if( inType.equals( MediaType.APPLICATION_JSON_TYPE ) )
        {
            final Split split = _timer.start( "xmpp" );
            List<String> listOfLogins = new ArrayList<String>(8);

            try
            {
                JSONObject json = new JSONObject( new JSONTokener( (String)entity ) );

                if( json.isNull( SharedEntityConstants.IMTransport.IMIDS ) ) 
                    return MALFORMED_JSON_ERROR;

                JSONArray array = json.getJSONArray( SharedEntityConstants.IMTransport.IMIDS );
                int numberOfLoginIds = array.length();

                for( int j=0; j<numberOfLoginIds; j++ )
                {
                    if( !array.isNull( j ) )
                    {
                        String aLoginId = array.getString( j );
                        if( !Constraint.EmptyString.isFulfilledBy( aLoginId ))
                            listOfLogins.add( aLoginId.trim() );
                    }
                }

                _logger.debug( "after json.array processing dance" );
            }
            catch( JSONException e )
            {
                _logger.error( Common.EMPTY_STRING, e );
                return MALFORMED_JSON_ERROR;
            }

        // Nothing to process, so short circuit
            if( listOfLogins.size() == 0 )
            {
                return new EitherResource.Builder( true, 200 )
                    .body( new XMPPDeleteSuccess( "Nothing to sign in", outType ) )
                    .httpHeaders( seedHttpHeader( split.stop() ) )
                    .build();
            }

            final List<IMIdentity> identities = toIdentities( listOfLogins );
            try
            {
                IMSession imSession = session.getSessionAttribute( SessionUtils.SESSION_ATTRIBUTE_IM );
                if( imSession == null )
                {
                    return new EitherResource.Builder( false, 403 )
                        .error( "No Session associated with this request", ProtocolErrorCode.MissingIMSession )
                        .httpHeaders( seedHttpHeader( split.stop() ) )
                        .build();
                }

                ExistingSessionResult sessionResult = imSession.signInService( identities );
                if( sessionResult.ok() )
                {
                    return new EitherResource.Builder( true, 200 )
                        .body( new BulkSigninSuccess( outType, (JSONObject)sessionResult.loginReport(), 
                            sessionResult.message() ) )
                        .httpHeaders( seedHttpHeader( split.stop() ) )
                        .build();
                }
                else
                {
                    return new EitherResource.Builder( false, 503 )
                        .error( "Problems communicating with the XMPP server", ProtocolErrorCode.XMPPCommunicationError )
                        .httpHeaders( seedHttpHeader( split.stop() ) )
                        .build();
                }
            }
            catch( InvalidatedHttpSessionException ihse )
            {
                return new EitherResource.Builder( false, 403 )
                    .error( ihse.getMessage(), ProtocolErrorCode.InvalidIMSession )
                    .httpHeaders( seedHttpHeader( split.stop() ) )
                    .build();
            }
            catch( IMSessionException imsesse )
            {
                // TODO ??
                _logger.error( imsesse.getMessage() );
            }
        }

        // XML --------------
        if( inType.equals( MediaType.TEXT_XML_TYPE ) )
        {
            // TODO
            throw new UnsupportedOperationException( "implement text/xml processing" );
        }

        throw new AssertionError( "Only text/xml and application/json supported. Should not come here" );
    }

    /**
     * Bulk sign outs
     * @deprecated moved to session-aware POST
     */
    @Override
    public EitherResource delete( String key,
        MultivaluedMap<String, String> queryParams, MediaType inType,
        MediaType outType, Object entity,
        Map<String, Object> sessionAttributes, SessionUtils session )
    {
        _logger.debug( "delete.xmpp.session.with.entityBodySupport" );

        if( Constraint.EmptyString.isFulfilledBy( key ) )
        {
            return new EitherResource.Builder( false, 400 ).
                error( "Please specify endpoint as /" + PATH + "/${userid}", ProtocolErrorCode.UserNotSpecified ).build();
        }

        if( !String.class.isAssignableFrom( entity.getClass() ))
            throw new IllegalArgumentException( "Currently we expect parameter {@code param} to be of type String" );

        if( ! _userExistPredicate.apply( key ) )
        {
            return new EitherResource.Builder( false, 404 ).
                error( "", ProtocolErrorCode.NoSuchUser ).build();
        }

        // JSON --------------
        if( inType.equals( MediaType.APPLICATION_JSON_TYPE ) )
        {
            final Split split = _timer.start( "xmpp" );
            List<String> listOfLogins = new ArrayList<String>(8);

            try
            {
                JSONObject json = new JSONObject( new JSONTokener( (String)entity ) );

                if( json.isNull( SharedEntityConstants.IMTransport.IMIDS ) ) 
                    return MALFORMED_JSON_ERROR;

                JSONArray array = json.getJSONArray( SharedEntityConstants.IMTransport.IMIDS );
                int numberOfLoginIds = array.length();

                for( int j=0; j<numberOfLoginIds; j++ )
                {
                    if( !array.isNull( j ) )
                    {
                        String aLoginId = array.getString( j );
                        if( !Constraint.EmptyString.isFulfilledBy( aLoginId ))
                            listOfLogins.add( aLoginId.trim() );
                    }
                }

                _logger.debug( "after json.array processing dance" );
            }
            catch( JSONException e )
            {
                _logger.error( Common.EMPTY_STRING, e );
                return MALFORMED_JSON_ERROR;
            }

        // Nothing to process, so short circuit
            if( listOfLogins.size() == 0 )
            {
                return new EitherResource.Builder( true, 200 )
                    .body( new XMPPDeleteSuccess( "Nothing to sign out", outType ) )
                    .httpHeaders( seedHttpHeader( split.stop() ) )
                    .build();
            }

            final List<IMIdentity> identities = toIdentities( listOfLogins );
            try
            {
                IMSession imSession = session.getSessionAttribute( SessionUtils.SESSION_ATTRIBUTE_IM );
                if( imSession == null )
                {
                    return new EitherResource.Builder( false, 403 )
                        .error( "No Session associated with this request", ProtocolErrorCode.MissingIMSession )
                        .httpHeaders( seedHttpHeader( split.stop() ) )
                        .build();
                }

                ExistingSessionResult result = imSession.signOutService( identities );
                return new EitherResource.Builder( true, 200 )
                    .body( new XMPPDeleteSuccess( result.message(), outType ) )
                    .httpHeaders( seedHttpHeader( split.stop() ) )
                    .build();
            }
            catch( InvalidatedHttpSessionException ihse )
            {
                return new EitherResource.Builder( false, 403 )
                    .error( ihse.getMessage(), ProtocolErrorCode.InvalidIMSession )
                    .httpHeaders( seedHttpHeader( split.stop() ) )
                    .build();
            }
            catch( IMSessionException imsesse )
            {
                // TODO ??
                _logger.error( imsesse.getMessage() );
            }
        }

        // XML --------------
        if( inType.equals( MediaType.TEXT_XML_TYPE ) )
        {
            // TODO
            throw new UnsupportedOperationException( "implement text/xml processing" );
        }

        throw new AssertionError( "Only text/xml and application/json supported. Should not come here" );
    }

    private final List<IMIdentity> toIdentities( List<String> loginIds )
    {
        if( loginIds == null || loginIds.size() == 0 )
            return Collections.emptyList();

        List<IMIdentity> identities = new ArrayList<IMIdentity>( loginIds.size() );
        for( String loginId : loginIds )
        {
            try
            {
                IMIdentity identity = newIdentityFor( loginId );
                identities.add( identity );
            }
            catch( InvalidIMIdentityException invide )
            {
                _logger.error( "Constructing IMIdentity for " + loginId + " failed due to. Skipping.", invide );
                continue;
            }
        }

        return identities;
    }

    @Override
    public boolean respondsTo( HttpVerb verb )
    {
        return _supportedVerbs.containsKey( verb );
    }

    @Override
    public SessionAttribute sessionAttributeFor( HttpVerb verb )
    {
        switch( verb )
        {
        case POST:
        case PUT:
            return SessionAttribute.REQUIRED;
        default:
            return SessionAttribute.NEVER;
        }
    }

    /**
     * @throws InvalidIMIdentityException if the loginId is invalid
     * @throws IllegalStateException if a programming error occurs
     */
    private final IMIdentity newIdentityFor( String loginId )
    {
        IMIdentityImpl.FromClientBuilderWithMissingImType builder =
            new IMIdentityImpl.FromClientBuilderWithMissingImType( _transportMapper, 
                _ymailPredicate, _rocketmailPredicate,
                _yahooPredicate, _googlePredicate, _qqPredicate, 
                _msnPredicate );

        IMIdentity identity = builder.loginId( loginId ).build();

        return identity;
    }

    private Map<String, String> seedHttpHeader( long delta )
    {
        Map<String, String> retval = new HashMap<String,String>(2);
        retval.put( _timer.httpHeaderKey(), 
            _timer.httpHeaderValueFor( delta ));
        return retval;
    }

    @Inject
    public XMPPGatewayResource( TransportMapper transportMapper, 
        @Named("userexist") Predicate<String> userExistPredicate,
        @Named("ymail") Predicate<String> ymailPredicate,
        @Named("rocketmail") Predicate<String> rocketmailPredicate,
        @Named("yahoo") Predicate<String> yahooPredicate,
        @Named("google") Predicate<String> googlePredicate,
        @Named("qq") Predicate<String> qqPredicate,
        @Named("msn") Predicate<String> msnPredicate,
        JavaSimonTimer timer, XMPPGatewayParser xmppParser )
    {
        _rocketmailPredicate = rocketmailPredicate;
        _userExistPredicate  = userExistPredicate;
        _transportMapper     = transportMapper;
        _googlePredicate     = googlePredicate;
        _ymailPredicate      = ymailPredicate;
        _yahooPredicate      = yahooPredicate;
        _qqPredicate         = qqPredicate;
        _msnPredicate        = msnPredicate;
        _xmppParser          = xmppParser;
        _timer               = timer;
    }

    private final XMPPGatewayParser _xmppParser;
    private final TransportMapper _transportMapper;
    private final JavaSimonTimer _timer;
    //TODO ?
    private IMUserSettingFactory2 _userSettingFactory2;

    private final Predicate<String> _ymailPredicate;
    private final Predicate<String> _rocketmailPredicate;
    private final Predicate<String> _yahooPredicate;
    private final Predicate<String> _googlePredicate;
    private final Predicate<String> _qqPredicate;
    private final Predicate<String> _msnPredicate;
    private final Predicate<String> _userExistPredicate;

    private final static String PATH = "xmpp";
    private static final String ERRORMSG_MISSING_USER = "Please specify endpoint as /" + PATH + "/${userid}";

    private Map<String,String> _httpHeaders = new HashMap<String,String>(1);

    private Map<HttpVerb,Object> _supportedVerbs = new HashMap<HttpVerb,Object>(3);
    {
        _supportedVerbs.put( HttpVerb.OPTIONS, null );
        _supportedVerbs.put( HttpVerb.PUT, null );
        _supportedVerbs.put( HttpVerb.POST, null );

        StringBuilder ALLOW_HEADER = new StringBuilder();
        Set<HttpVerb> set = _supportedVerbs.keySet();
        int i = 1;
        for( HttpVerb verb : set )
        {
            ALLOW_HEADER.append( verb );
            if( i != 3 )
                ALLOW_HEADER.append( "," );

            i++;
        }
        _httpHeaders.put( "Allow", ALLOW_HEADER.toString() );
    }

    private final EitherResource NOT_IMPLEMENTED = new EitherResource.Builder( false, 501 )
        .error( "This method is not supported", ProtocolErrorCode.NotImplemented )
        .build();

    private final Logger _logger = Logger.getLogger( XMPPGatewayResource.class );
}
