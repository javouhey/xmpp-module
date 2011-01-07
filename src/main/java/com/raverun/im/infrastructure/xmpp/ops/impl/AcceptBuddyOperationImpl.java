package com.raverun.im.infrastructure.xmpp.ops.impl;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.raverun.im.common.Transport;
import com.raverun.im.infrastructure.xmpp.XMPPUtility;
import com.raverun.im.infrastructure.xmpp.ops.AcceptBuddyOperation;
import com.raverun.im.infrastructure.xmpp.smack.XMPPConnectionIF;
import com.raverun.im.infrastructure.xmpp.support.XMPPFault;
import com.raverun.shared.Configuration;
import com.raverun.shared.Constraint.NonNullArgument;

/**
 * @deprecated not thread safe.
 * @see MTAcceptBuddyOperationImpl
 */
public class AcceptBuddyOperationImpl implements AcceptBuddyOperation
{
    /**
     * @throws XMPPFault
     * @throws IllegalStateException if connection to XMPP is not alive
     */
    @Override
    public AcceptBuddyResult call() throws Exception
    {
        checkState();

        String targetJID = _xmppUtility.newJIDfor( _to, _transport );

        try
        {
            // did we initiate it ?
            Roster roster = _xmppConn.getRoster();
            if( roster == null )
                throw new IllegalStateException( "Roster is null. XMPP connection is not authenticated" );

            RosterEntry entry = roster.getEntry( targetJID );
            if( entry == null )
            {
                _logger.debug( "accepted: INITIATED by " + _to );
                try { roster.createGroup( _defaultGroupName ); } catch( IllegalArgumentException ignored ) {}
                roster.createEntry( targetJID, _to, new String [] {_defaultGroupName});
                Thread.sleep( 450 );
            }
            else
            {
                _logger.debug( "accepted: INITIATED by me " + _from );
                RosterPacket rosterUpdate = new RosterPacket();
                RosterPacket.Item item = new RosterPacket.Item( targetJID, _to ); // @TODO needs to accept a nickname field
                item.addGroupName( _defaultGroupName );
                //item.setItemType( RosterPacket.ItemType.both );
                rosterUpdate.addRosterItem( item );

                _logger.debug( "C->S: " + rosterUpdate.toXML() );
                _xmppConn.sendPacket( rosterUpdate );
                Thread.sleep( 500 );
            }

            Presence presence = new Presence( Presence.Type.subscribed );
            presence.setTo( targetJID );
            _logger.debug( "C->S: " + presence.toXML() );

            _xmppConn.sendPacket( presence );
            Thread.sleep( 200 );

            return new AcceptBuddyResult() {
                public boolean didWeReceiveReply() { return true; }
                public Packet getReceivedPacket() { return null; }
            };
        }
        catch( Exception xmppe )
        {
            throw new XMPPFault( "Accept buddy failed for " + _transport.code() + " due to", xmppe, XMPPFault.XmppFaultCode.BUDDY_ACCEPT );
        }
    }

    public void init( XMPPConnectionIF xmppConn, Transport transport,
        String from, String to )
    {
        NonNullArgument.check( transport, "transport" );
        NonNullArgument.check( xmppConn, "xmppConn" );
        NonNullArgument.check( from, "from" );
        NonNullArgument.check( to, "to" );

        _transport = transport;
        _xmppConn  = xmppConn;
        _from      = from;
        _to        = to;
    }
    
    @Inject
    public AcceptBuddyOperationImpl( XMPPUtility xmppUtility, 
        Configuration config,
        @Named("default.roster.group") String defaultGroupName )
    {
        _config           = config;
        _xmppUtility      = xmppUtility;
        _defaultGroupName = defaultGroupName;
    }

    private void checkState()
    {
        if( !_xmppConn.isConnected() )
            throw new IllegalStateException( "XMPP connection is disconnected");

        if( !_xmppConn.isAuthenticated() )
            throw new IllegalStateException( "XMPP connection is not authenticated");
    }

 // ---- mutable ------
    private XMPPConnectionIF _xmppConn;
    private Transport        _transport;
    private String           _from;
    private String           _to;

// ---- immutable ----
    private final Configuration _config;
    private final XMPPUtility _xmppUtility;
    private final String _defaultGroupName;

    private final Logger _logger = Logger.getLogger( AcceptBuddyOperationImpl.class );
}
