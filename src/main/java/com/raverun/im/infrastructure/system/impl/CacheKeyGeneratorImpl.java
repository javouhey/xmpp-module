package com.raverun.im.infrastructure.system.impl;

import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.raverun.im.application.IMTypeTransportMapper;
import com.raverun.im.application.JIDMapper;
import com.raverun.im.common.Transport;
import com.raverun.im.domain.IMIdentity;
import com.raverun.im.infrastructure.system.CacheKeyGenerator;
import com.raverun.im.infrastructure.xmpp.XMPPUtility;
import com.raverun.shared.Constraint.EmptyString;

public class CacheKeyGeneratorImpl implements CacheKeyGenerator
{
    @Override
    public String generateKey( String user, int imtype, String myid, String buddyId )
    {
        StringBuilder builder = new StringBuilder();
        builder.append( user ).append( SYMBOL_COLON );
        builder.append( imtype ).append( SYMBOL_COLON );
        builder.append( myid ).append( SYMBOL_COLON );
        builder.append( buddyId );
        return builder.toString();
    }

    @Override
    public String generateKey( String user, String userXmpp, String fromJid )
    {
        if( EmptyString.isFulfilledBy( user ) ||
            EmptyString.isFulfilledBy( userXmpp ) ||
            EmptyString.isFulfilledBy( fromJid ) )
        {
            _logger.debug( "NULL KEY :: Either one of {user,userXmpp,fromJid} is null" );
            return null;
        }

        try
        {
            if( ! senderIsIdentifiable( fromJid ) )
            {
                _logger.debug( "NULL KEY :: " + fromJid + " is a transport specific jid" );
                return null;
            }

            final Transport transport = _xmppUtility.decodeTransportFor( fromJid );
            if( transport == Transport.MIM )
            {
                _logger.debug( "NULL KEY :: " + fromJid + " is MIM transport" );
                return null;
            }

            IMIdentity canonicalReceiver = _jidMapper.getCanonicalReceiver( transport, user, userXmpp );
            String canonicalSender = _jidMapper.getCanonicalSender( _xmppUtility.decodeSender( fromJid ), transport );

            StringBuilder builder = new StringBuilder();
            builder.append( user ).append( SYMBOL_COLON );
            builder.append( _imtypeMapper.parse( canonicalReceiver.transport() ) ).append( SYMBOL_COLON );
            builder.append( canonicalReceiver.imId() ).append( SYMBOL_COLON );
            builder.append( canonicalSender );
            return builder.toString();
        }
        catch( Exception e )
        {
            _logger.error( "bugs?", e );
            return null;
        }
    }

    private final boolean senderIsIdentifiable( String from )
    {
        return( ! XMPPUtility.TRANSPORT_ONLY.equals(_xmppUtility.decodeSender( from )) );
    }

    @Inject
    public CacheKeyGeneratorImpl( XMPPUtility xmppUtility,
        JIDMapper jidMapper, IMTypeTransportMapper imtypeMapper )
    {
        _jidMapper = jidMapper;
        _xmppUtility = xmppUtility;
        _imtypeMapper = imtypeMapper;
    }

    private final IMTypeTransportMapper _imtypeMapper;
    private final XMPPUtility _xmppUtility;
    private final JIDMapper _jidMapper;

    private static final String SYMBOL_COLON = ":";

    private static final Logger _logger = Logger.getLogger( CacheKeyGeneratorImpl.class );
}
