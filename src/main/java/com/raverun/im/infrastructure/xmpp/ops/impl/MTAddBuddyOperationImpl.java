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

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.raverun.im.common.Transport;
import com.raverun.im.infrastructure.xmpp.XMPPUtility;
import com.raverun.im.infrastructure.xmpp.ops.AddBuddyOperation;
import com.raverun.im.infrastructure.xmpp.smack.XMPPConnectionIF;
import com.raverun.im.infrastructure.xmpp.support.XMPPFault;
import com.raverun.shared.Constraint;
import com.raverun.shared.Constraint.NonNullArgument;

public class MTAddBuddyOperationImpl implements AddBuddyOperation
{

    /**
     * @throws IllegalArgumentException for null member variables
     * @throws XMPPFault
     * @throws IllegalStateException if connection to XMPP is not alive
     */
    @Override
    public AddBuddyResult call() throws Exception
    {
        NonNullArgument.check( _nickName, "_nickName" );
        NonNullArgument.check( _groups, "_groups" );
        NonNullArgument.check( _from, "_from" );
        NonNullArgument.check( _to, "_to" );

        checkState();

        String targetJID = _xmppUtility.newJIDfor( _to, _transport );
//      String sourceJID = _xmppUtility.newJIDfor( _from, _transport );

        try
        {
            Roster roster = _xmppConn.getRoster();
            if( roster == null )
                throw new IllegalStateException( "Roster is null. XMPP connection is not authenticated" );

            @SuppressWarnings("unused")
            Collection<RosterEntry> entries = roster.getEntries();

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

    @AssistedInject
    public MTAddBuddyOperationImpl( 
        XMPPUtility xmppUtility,
        @Assisted XMPPConnectionIF xmppConn, 
        @Assisted Transport transport,
        @Assisted String from,
        @Assisted String to,
        @Assisted String nickname,
        @Assisted List<String> groups )
    {
        _xmppConn = xmppConn;
        _transport = transport;
        _xmppUtility = xmppUtility;

        _nickName  = nickname;
        _from      = from;
        _to        = to;

        _groups = new ArrayList<String>(8);
        _groups.addAll( groups );
    }

    private void checkState()
    {
        if( !_xmppConn.isConnected() )
            throw new IllegalStateException( "XMPP connection is disconnected");

        if( !_xmppConn.isAuthenticated() )
            throw new IllegalStateException( "XMPP connection is not authenticated");
    }

// ---- immutable ----

    private final XMPPConnectionIF _xmppConn;
    private final Transport        _transport;
    private final String           _from;
    private final String           _to;
    private final String           _nickName;
    private final List<String>     _groups;
    private final XMPPUtility      _xmppUtility;

    private final String GROUP = "mcbuddies";

    private final Logger _logger = Logger.getLogger( MTAddBuddyOperationImpl.class );
}
