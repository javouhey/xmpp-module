package com.raverun.im.infrastructure.xmpp.ops.impl;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import com.raverun.im.common.Transport;
import com.raverun.im.infrastructure.xmpp.XMPPUtility;
import com.raverun.im.infrastructure.xmpp.ops.RejectBuddyOperation;
import com.raverun.im.infrastructure.xmpp.smack.XMPPConnectionIF;
import com.raverun.im.infrastructure.xmpp.support.XMPPFault;

public class MTRejectBuddyOperationImpl implements RejectBuddyOperation
{

    /**
     * @throws IllegalStateException iff not connected
     * @throws XMPPFault
     */
    @Override
    public RejectBuddyResult call() throws Exception
    {
        checkState();

        String targetJID = _xmppUtility.newJIDfor( _to, _transport );

        Presence presence = new Presence( Presence.Type.unsubscribed );
        presence.setTo( targetJID );

        try
        {
            Roster roster = _xmppConn.getRoster();
            if( roster == null )
                throw new IllegalStateException( "Roster is null. XMPP connection is not authenticated" );

            _xmppConn.sendPacket( presence );
            Thread.sleep( 100 );

            RosterEntry entry = roster.getEntry( targetJID );
            if( entry != null )
            {
                roster.removeEntry( entry );
                _logger.debug( "removed the roster entry for " + targetJID );
                Thread.sleep( 100 );
            }

            return new RejectBuddyResult() {
                public boolean didWeReceiveReply() { return true; }
                public Packet getReceivedPacket() { return null; }
            };
        }
        catch( Exception xmppe )
        {
            throw new XMPPFault( "Rejecting buddy failed for " + _transport.code() + " due to", xmppe, XMPPFault.XmppFaultCode.BUDDY_REJECT );
        }
    }

    private final void checkState()
    {
        if( !_xmppConn.isConnected() )
            throw new IllegalStateException( "XMPP connection is disconnected");

        if( !_xmppConn.isAuthenticated() )
            throw new IllegalStateException( "XMPP connection is not authenticated");
    }

    @AssistedInject
    public MTRejectBuddyOperationImpl( 
        XMPPUtility xmppUtility,
        @Named("default.roster.group") String defaultGroupName,
        @Assisted XMPPConnectionIF xmppConn, 
        @Assisted Transport transport,
        @Assisted String from,
        @Assisted String to )
    {
        _to = to;
        _from = from;
        _xmppConn = xmppConn;
        _transport = transport;
        _xmppUtility = xmppUtility;
        _defaultGroupName = defaultGroupName;
    }

    private final String _to;
    private final String _from;
    private final Transport _transport;
    private final XMPPUtility _xmppUtility;
    private final String _defaultGroupName;
    private final XMPPConnectionIF _xmppConn;

    private final Logger _logger = Logger.getLogger( MTRejectBuddyOperationImpl.class );
}
