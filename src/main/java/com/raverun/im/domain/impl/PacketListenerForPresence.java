package com.raverun.im.domain.impl;

import java.util.List;

import javax.annotation.Nullable;
import javax.persistence.PersistenceException;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.raverun.im.application.JIDMapper;
import com.raverun.im.common.Transport;
import com.raverun.im.domain.IMIdentity;
import com.raverun.im.domain.IMPresence;
import com.raverun.im.infrastructure.persistence.OfflineService;
import com.raverun.im.infrastructure.persistence.PresenceService;
import com.raverun.im.infrastructure.persistence.SettingsService;
import com.raverun.im.infrastructure.persistence.SubscriptionService;
import com.raverun.im.infrastructure.xmpp.XMPPUtility;
import com.raverun.im.infrastructure.xmpp.smack.PresenceUtilityIF;
import com.raverun.shared.Constraint;

public class PacketListenerForPresence implements PacketListener
{
    @AssistedInject
    public PacketListenerForPresence( XMPPUtility xmppUtility, 
        SettingsService settingService,
        SubscriptionService subscriptionService,
        JIDMapper jidMapper,
        PresenceUtilityIF presenceUtility,
        PresenceService presenceService,
        OfflineService offlineService,
        @Assisted String user, @Assisted String userXmpp, @Assisted List<Transport> liveTransports )
    {
        Constraint.NonNullArgument.check( user, "user" );
        Constraint.NonNullArgument.check( userXmpp, "userXmpp" );
        Constraint.NonNullArgument.check( liveTransports, "liveTransports" );

        _user = user;
        _userXmpp = userXmpp; 
        _jidMapper = jidMapper;
        _xmppUtility = xmppUtility;
        _settingService = settingService;
        _liveTransports = liveTransports;
        _offlineService = offlineService;
        _presenceUtility = presenceUtility;
        _presenceService = presenceService;
        _subscriptionService = subscriptionService;

        _label = label();
    }

    /**
     * @TODO need to handle PersistenceException!
     */
    @Override
    public void processPacket( Packet packet )
    {
        final Presence presence = (Presence)packet;
        _logger.debug( _label + presence.toXML() );
        _logger.debug( "\t\t-> " + this.toString() );

        try
        {
            @Nullable final Presence.Mode mode = presence.getMode();
            @Nullable final String status = presence.getStatus();

            switch( presence.getType() )
            {
            case subscribe: // Subscription requests from external legacy IM transports & MIM
                String from = presence.getFrom();
                Transport transport = _xmppUtility.decodeTransportFor( from );
                _logger.debug( "subscribe request from (raw): " + from );
                _logger.debug( "subscribe request from (decoded): " + _xmppUtility.decodeSender( from ) );
                _logger.debug( "subscribe request transport: " + transport );

            // @TODO check in cache first
                IMIdentity canonicalReceiver = _jidMapper.getCanonicalReceiver( transport, _user, _userXmpp );
                String canonicalSender = _jidMapper.getCanonicalSender( _xmppUtility.decodeSender( from ), transport );

                _logger.debug( "subscribe request to   (canonical): " + canonicalReceiver.imId() );
                _logger.debug( "subscribe request from (canonical): " + canonicalSender );

                _subscriptionService.addSubscriptionRequest( canonicalReceiver.imId(), canonicalSender, transport, _user );
                break;

            case unsubscribe:
                from = presence.getFrom();
                transport = _xmppUtility.decodeTransportFor( from );
                _logger.debug( "unsubscribe request from: " + _xmppUtility.decodeSender( from ) );
                _logger.debug( "unsubscribe request transport: " + transport );
                break;

            case available:
                // Caveat: don't handle directed presence. Assume it was a broadcast
                from = presence.getFrom();
                if( !senderIsIdentifiable( from ) )
                {
                    _logger.debug( "IGNORED from: " + from );
                    try
                    {
                        Transport availableTransport = _xmppUtility.decodeTransportFor( from );
                        if( availableTransport != null )
                        {
                            // @TODO not thread safe!!
                            if( _liveTransports.indexOf( availableTransport ) == -1 )
                            {
                                _liveTransports.add( availableTransport );
                                _logger.debug( "[added 2 liveTransport] " + availableTransport.code() );
                            }
                        }
                    }
                    catch( IllegalArgumentException ignored ) {}

                    return;
                }

                transport = _xmppUtility.decodeTransportFor( from );
                canonicalReceiver = _jidMapper.getCanonicalReceiver( transport, _user, _userXmpp );
                canonicalSender = _jidMapper.getCanonicalSender( _xmppUtility.decodeSender( from ), transport );
                _logger.debug( "(+) for transport: " + transport + " for id " + canonicalReceiver );

                IMPresenceImpl.FromXmppServerBuilder builderAvail = new IMPresenceImpl.FromXmppServerBuilder( _user,
                    canonicalReceiver, canonicalSender, presence.getType() );

                if( mode != null )
                    builderAvail = builderAvail.mode( _presenceUtility.derefXmpp( mode ) );

                if( status != null )
                    builderAvail = builderAvail.status( status );

                IMPresence presenceAvail2db = builderAvail.build();
                _presenceService.add( presenceAvail2db );

                break;

            case unavailable:
                // Caveat: don't handle directed presence. Assume it was a broadcast
                from = presence.getFrom(); 
                if( !senderIsIdentifiable( from ) )
                {
                    _logger.debug( "IGNORED from: " + from );
                    try
                    {
                        Transport availableTransport = _xmppUtility.decodeTransportFor( from );
                        if( availableTransport != null )
                        {
                            // @TODO not thread safe!!
                            int i = 0;
                            if( (i = _liveTransports.indexOf( availableTransport )) != -1 )
                            {
                                Transport t = _liveTransports.remove( i );
                                _logger.debug( "[removed from liveTransport] " + t.code() );
                            }

                        // @TODO should we move this into the if statement above
                            IMIdentity receiver = _jidMapper.getCanonicalReceiver( availableTransport, _user, _userXmpp );
                            _offlineService.add( 
                                new IMOfflineImpl.Builder( _user, receiver ).build()
                            );
                        }
                    }
                    catch( IllegalArgumentException ignored ) {}

                    return;
                }

                transport = _xmppUtility.decodeTransportFor( from );
                canonicalReceiver = _jidMapper.getCanonicalReceiver( transport, _user, _userXmpp );
                canonicalSender = _jidMapper.getCanonicalSender( _xmppUtility.decodeSender( from ), transport );
                _logger.debug( "(-) for transport: " + transport + " for id " + canonicalReceiver );

                IMPresenceImpl.FromXmppServerBuilder builderNotAvail = new IMPresenceImpl.FromXmppServerBuilder( _user,
                    canonicalReceiver, canonicalSender, presence.getType() );

                if( mode != null )
                    builderNotAvail = builderNotAvail.mode( _presenceUtility.derefXmpp( mode ) );

                if( status != null )
                    builderNotAvail = builderNotAvail.status( status );

                IMPresence presenceNotAvail2db = builderNotAvail.build();
                _presenceService.add( presenceNotAvail2db );

                break;

            default:
                from = presence.getFrom();
                transport = _xmppUtility.decodeTransportFor( from );
                _logger.debug( "(?) for transport: " + transport );

            }
        }
        catch( PersistenceException pe )
        {
            _logger.error( "", pe ); // @TODO handle better
        }
        catch( Exception e )
        {
            _logger.error( "", e ); // @TODO handle better
        }
    }

    /**
     * 
     * @param from - legacy IM address
     * @return true if a sender is distinguishable
     */
    private final boolean senderIsIdentifiable( String from )
    {
        return( ! XMPPUtility.TRANSPORT_ONLY.equals(_xmppUtility.decodeSender( from )) );
    }

    private final String label()
    {
        StringBuilder builder = new StringBuilder();
        return builder.append( "Presence [" ).append( _user ).append( " : " ).append( _userXmpp ).append( "]" ).toString();
    }

    private final SubscriptionService _subscriptionService;
    private final PresenceUtilityIF _presenceUtility;
    private final List<Transport> _liveTransports;
    private final PresenceService _presenceService;
    private final SettingsService _settingService;
    private final OfflineService _offlineService;
    private final XMPPUtility _xmppUtility;
    private final JIDMapper _jidMapper;
    private final String _label;
    private final String _user;
    private final String _userXmpp;

    private static final Logger _logger = Logger.getLogger( PacketListenerForPresence.class );
}
