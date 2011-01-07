package com.raverun.im.infrastructure.xmpp.ops.impl;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import com.raverun.im.common.Transport;
import com.raverun.im.infrastructure.xmpp.XMPPUtility;
import com.raverun.im.infrastructure.xmpp.ops.AcceptBuddyOperation;
import com.raverun.im.infrastructure.xmpp.smack.XMPPConnectionIF;
import com.raverun.im.infrastructure.xmpp.support.XMPPFault;

public class MTAcceptBuddyOperationImpl implements AcceptBuddyOperation
{
    /**
     * @throws IllegalArgumentException for nullable parameters
     * @throws IllegalStateException if we are not connected or authenticated
     * @throws XMPPFault if an XMPPException was encountered
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
                _logger.debug( _userXmpp + " accepted: INITIATED by " + _to );
                try { roster.createGroup( _defaultGroupName ); } catch( IllegalArgumentException ignored ) {}
                roster.createEntry( targetJID, _to, new String [] {_defaultGroupName});
                Thread.sleep( 450 );
            }
            else
            {
                _logger.debug( _userXmpp + "accepted: INITIATED by me " + _from );
                RosterPacket rosterUpdate = new RosterPacket();
                RosterPacket.Item item = new RosterPacket.Item( targetJID, _to ); // @TODO needs to accept a nickname field
                item.addGroupName( _defaultGroupName );
                //item.setItemType( RosterPacket.ItemType.both );
                rosterUpdate.addRosterItem( item );

                _logger.debug( _userXmpp + " C->S: " + rosterUpdate.toXML() );
                _xmppConn.sendPacket( rosterUpdate );
                Thread.sleep( 500 );
            }

            Presence presence = new Presence( Presence.Type.subscribed );
            presence.setTo( targetJID );
            _logger.debug( _userXmpp + " C->S: " + presence.toXML() );

            _xmppConn.sendPacket( presence );
            Thread.sleep( 200 );

            return new AcceptBuddyResult() {
                public boolean didWeReceiveReply() { return true; }
                public Packet getReceivedPacket() { return null; }
            };
        }
        catch( Exception xmppe )
        {
            throw new XMPPFault( _userXmpp + " Accept buddy failed for " + _transport.code() + " due to", xmppe, XMPPFault.XmppFaultCode.BUDDY_ACCEPT );
        }
    }

    private void checkState()
    {
        if( !_xmppConn.isConnected() )
            throw new IllegalStateException( "XMPP connection is disconnected");

        if( !_xmppConn.isAuthenticated() )
            throw new IllegalStateException( "XMPP connection is not authenticated");
    }

    @AssistedInject
    public MTAcceptBuddyOperationImpl( 
        XMPPUtility xmppUtility,
        @Named("default.roster.group") String defaultGroupName,
        @Assisted XMPPConnectionIF xmppConn, 
        @Assisted Transport transport,
        @Assisted String userXmpp,
        @Assisted String from,
        @Assisted String to )
    {
        _to = to;
        _from = from;
        _userXmpp = userXmpp;
        _xmppConn = xmppConn;
        _transport = transport;
        _xmppUtility = xmppUtility;
        _defaultGroupName = defaultGroupName;
    }

    private final String _to;
    private final String _from;
    private final String _userXmpp;
    private final Transport _transport;
    private final XMPPUtility _xmppUtility;
    private final String _defaultGroupName;
    private final XMPPConnectionIF _xmppConn;

    private final Logger _logger = Logger.getLogger( MTAcceptBuddyOperationImpl.class );
}
