package com.raverun.im.infrastructure.xmpp.ops.impl;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.raverun.im.common.Transport;
import com.raverun.im.infrastructure.xmpp.XMPPUtility;
import com.raverun.im.infrastructure.xmpp.ops.RejectBuddyOperation;
import com.raverun.im.infrastructure.xmpp.smack.XMPPConnectionIF;
import com.raverun.im.infrastructure.xmpp.support.XMPPFault;
import com.raverun.shared.Configuration;
import com.raverun.shared.Constraint.NonNullArgument;

/**
 * @deprecated
 */
public class RejectBuddyOperationImpl implements RejectBuddyOperation
{
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
            throw new XMPPFault( "Accept buddy failed for " + _transport.code() + " due to", xmppe, XMPPFault.XmppFaultCode.BUDDY_REJECT );
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
    public RejectBuddyOperationImpl( XMPPUtility xmppUtility, 
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

    private final Logger _logger = Logger.getLogger( RejectBuddyOperationImpl.class );
}
