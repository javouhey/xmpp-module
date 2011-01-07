package com.raverun.im.interfaces.rest.impl.resources.xmpp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.google.common.base.Predicate;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.raverun.im.domain.IMIdentity;
import com.raverun.im.domain.InvalidIMIdentityException;
import com.raverun.im.domain.IMSession.TupleForAutologinChange;
import com.raverun.im.domain.IMUserSetting.UserSettingType;
import com.raverun.im.domain.impl.IMIdentityImpl;
import com.raverun.im.infrastructure.persistence.TransportMapper;
import com.raverun.im.interfaces.rest.impl.resources.PostCommand;
import com.raverun.im.interfaces.rest.impl.resources.XMPPGatewayParser;
import com.raverun.im.interfaces.rest.impl.resources.SharedEntityConstants.Generic;
import com.raverun.im.interfaces.rest.impl.resources.SharedEntityConstants.IMTransport;
import com.raverun.shared.Constraint;

public class XMPPGatewayParserImpl implements XMPPGatewayParser
{

    @Override
    public XMPPGatewayParseResult parse( String entityBody )
        throws JSONException
    {
        JSONObject json = new JSONObject( new JSONTokener( (String)entityBody ) );
        if( json.isNull( Generic.COMMAND ) )
            throw new JSONException( "Missing " + Generic.COMMAND + " property" );

        final PostCommand[] commandHack = new PostCommand[1];
        try
        {
            commandHack[0] = PostCommand.deref( 
                json.getString( Generic.COMMAND ) );
        }
        catch( IllegalArgumentException e )
        {
            throw new JSONException( Generic.COMMAND + " must be 'delete'" );
        }

        if( Constraint.EmptyString.isFulfilledBy( entityBody ))
            return new XMPPGatewayParseResult() {
                public PostCommand command() { return commandHack[0]; }
                public List<TupleForAutologinChange> list() { return Collections.emptyList(); }
                public List<IMIdentity> listAsIdentities() { return Collections.emptyList(); }
            };

        if( json.isNull( IMTransport.IMIDS ) ) 
        {
            // assumed that there is nothing to DELETE
            return new XMPPGatewayParseResult() {
                public PostCommand command() { return commandHack[0]; }
                public List<TupleForAutologinChange> list() { return Collections.emptyList(); }
                public List<IMIdentity> listAsIdentities() { return Collections.emptyList(); }
            };
        }

        final List<TupleForAutologinChange> list = new ArrayList<TupleForAutologinChange>(8);

        JSONArray array = json.getJSONArray( IMTransport.IMIDS );
        if( array.length() > 0 )
        {
            for( int i=0; i<array.length(); i++ )
            {
                JSONObject item = array.getJSONObject( i );
                String loginId = item.getString( IMTransport.LOGIN_ID );
                int imType = item.getInt( IMTransport.IMTYPE );

                try
                {
                    final IMIdentity identity = newIdentityFor( loginId, imType );
                    switch( commandHack[0] )
                    {
                    case DELETE:
                        list.add( new TupleForAutologinChange() {
                            public IMIdentity identity() { return identity; }
                            public UserSettingType newSavedValue() { return null; }
                        });
                        break;

                    default:
                        break;
                    }//switch
                }
                catch( InvalidIMIdentityException ide )
                {
                    _logger.error( "skipping " + loginId, ide );
                    continue;
                }
                catch( IllegalStateException ise )
                {
                    _logger.error( "skipping " + loginId, ise );
                    continue;
                }
                _logger.debug( "handled " + loginId );
            }//for
        }//if

        final List<IMIdentity> identities = toIdentityList( list );

        return new XMPPGatewayParseResult() {
            public PostCommand command() { return commandHack[0]; }
            public List<TupleForAutologinChange> list() { return list; }
            public List<IMIdentity> listAsIdentities() { return identities; }
        };
    }

    private final List<IMIdentity> toIdentityList( List<TupleForAutologinChange> list )
    {
        if( list == null || list.size() == 0 )
            return Collections.emptyList();

        List<IMIdentity> retval = new ArrayList<IMIdentity>( list.size() );
        for( TupleForAutologinChange aChange : list )
            retval.add( aChange.identity() );

        return retval;
    }

    /**
     * @throws InvalidIMIdentityException if the loginId is invalid
     * @throws IllegalStateException if a programming error occurs
     */
    private final IMIdentity newIdentityFor( String loginId, int imType )
    {
        IMIdentityImpl.FromClientBuilder builder =
            new IMIdentityImpl.FromClientBuilder( _transportMapper, 
                _ymailPredicate, _rocketmailPredicate,
                _yahooPredicate, _googlePredicate, _qqPredicate, 
                _msnPredicate );

        IMIdentity identity = builder.loginId( loginId ).imType( imType ).build();

        return identity;
    }

    @Inject
    public XMPPGatewayParserImpl( TransportMapper transportMapper, 
        @Named("ymail") Predicate<String> ymailPredicate,
        @Named("rocketmail") Predicate<String> rocketmailPredicate,
        @Named("yahoo") Predicate<String> yahooPredicate,
        @Named("google") Predicate<String> googlePredicate,
        @Named("qq") Predicate<String> qqPredicate,
        @Named("msn") Predicate<String> msnPredicate 
    )
    {
        _transportMapper = transportMapper;
        _rocketmailPredicate = rocketmailPredicate;
        _ymailPredicate = ymailPredicate;
        _yahooPredicate = yahooPredicate;
        _googlePredicate = googlePredicate;
        _qqPredicate = qqPredicate;
        _msnPredicate = msnPredicate;
    }

    private final TransportMapper _transportMapper;

    private final Predicate<String> _ymailPredicate;
    private final Predicate<String> _rocketmailPredicate;
    private final Predicate<String> _yahooPredicate;
    private final Predicate<String> _googlePredicate;
    private final Predicate<String> _qqPredicate;
    private final Predicate<String> _msnPredicate;

    private final Logger _logger = Logger.getLogger( XMPPGatewayParserImpl.class );
}
