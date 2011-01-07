package com.raverun.im.infrastructure.xmpp.ops.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

import com.google.inject.Inject;
import com.raverun.im.common.Transport;
import com.raverun.im.infrastructure.xmpp.XMPPUtility;
import com.raverun.im.infrastructure.xmpp.ops.AddBuddyOperation;
import com.raverun.im.infrastructure.xmpp.smack.XMPPConnectionIF;
import com.raverun.im.infrastructure.xmpp.support.XMPPFault;
import com.raverun.shared.Configuration;
import com.raverun.shared.Constraint;
import com.raverun.shared.Constraint.NonNullArgument;

public class AddBuddyOperationImpl implements AddBuddyOperation
{
    /**
     * @throws XMPPFault
     * @throws IllegalStateException if connection to XMPP is not alive
     */
    @Override
    public AddBuddyResult call() throws Exception
    {
        checkState();

        String targetJID = _xmppUtility.newJIDfor( _to, _transport );
//        String sourceJID = _xmppUtility.newJIDfor( _from, _transport );

        try
        {
            Roster roster = _xmppConn.getRoster();
            if( roster == null )
                throw new IllegalStateException( "Roster is null. XMPP connection is not authenticated" );

            Collection<RosterEntry> entries = roster.getEntries();
            dump( entries );

            if( _groups.size() == 0 )
            {
                try { roster.createGroup( GROUP ); } catch( IllegalArgumentException ignored ) {}
                _groups.add( GROUP );
            }

            final String nickName = (Constraint.EmptyString.isFulfilledBy( _nickName ) ? _to : _nickName );

            roster.createEntry( targetJID, nickName, _groups.toArray( new String[0] ) );

            _logger.debug( "[addBuddy] requesting for Presence info on " + targetJID );
            Presence presence = roster.getPresence( targetJID );
            _logger.debug( "[addBuddy] presence info  type:" + presence.getType() + " |mode: " + presence.getMode() );

            return new AddBuddyResult() {
                public boolean didWeReceiveReply() { return true; }
                public Packet getReceivedPacket() { return null; }
            };
        }
        catch( XMPPException xmppe )
        {
            throw new XMPPFault( "Add buddy failed for " + _transport.code() + " due to", xmppe, XMPPFault.XmppFaultCode.BUDDY_ADD );
        }
    }

    private void dump( Collection<RosterEntry> entries )
    {
        for( RosterEntry entry : entries )
        {
            _logger.debug( entry.getUser() + "|status:" + entry.getStatus() + "|" + entry.getType() );
        }
    }

    /**
     * @deprecated
     */
    public void init( XMPPConnectionIF xmppConn, Transport transport, String from, String to, String nickname, List<String> groups )
    {
        NonNullArgument.check( transport, "transport" );
        NonNullArgument.check( xmppConn, "xmppConn" );
        NonNullArgument.check( groups, "groups" );
        NonNullArgument.check( from, "from" );
        NonNullArgument.check( to, "to" );

        _transport = transport;
        _xmppConn  = xmppConn;
        _nickName  = nickname;
        _from      = from;
        _to        = to;

        _groups = new ArrayList<String>(8);
        _groups.addAll( groups );
    }

    @Inject
    public AddBuddyOperationImpl( XMPPUtility xmppUtility, 
        Configuration config  )
    {
        _config        = config;
        _xmppUtility   = xmppUtility;
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
    private String           _nickName;
    private List<String>     _groups;

// ---- immutable ----
    private final Configuration _config;
    private final XMPPUtility _xmppUtility;

    private final String GROUP = "mcbuddies";

    private final Logger _logger = Logger.getLogger( AddBuddyOperationImpl.class );
}
