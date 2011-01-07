package com.raverun.im.domain.impl;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.RosterPacket;
import org.jivesoftware.smack.packet.RosterPacket.ItemStatus;
import org.jivesoftware.smack.packet.RosterPacket.ItemType;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.raverun.im.infrastructure.system.CacheOfNickNames;

public class PacketListenerForRosterEntries implements PacketListener
{

    @Override
    public void processPacket( Packet packet )
    {
        try
        {
            RosterPacket rosterPacket = (RosterPacket) packet;

            for( RosterPacket.Item item : rosterPacket.getRosterItems() ) 
            {
                String from = item.getUser();
                String nickName = item.getName();
                ItemType subscriptionType = item.getItemType();
                ItemStatus status = item.getItemStatus();
                _logger.debug( _userXmpp + "--->{ sender: " + from + " , nickname: " + nickName + " , subtype: " + (( subscriptionType!=null ) ? subscriptionType.name() : "N/A") + " , status: " + (( status!=null ) ? status.toString() : "N/A") );

                _cacheNickNames.write( _user, _userXmpp, from, nickName );
            }
        }
        catch( Exception e )
        {
            _logger.error( "processPacket failed", e );
        }
    }

    @AssistedInject
    public PacketListenerForRosterEntries( 
        CacheOfNickNames cacheNickNames,
        @Assisted String user, @Assisted String userXmpp )
    {
        _user = user;
        _userXmpp = userXmpp; 
        _cacheNickNames = cacheNickNames;
    }

    private final CacheOfNickNames _cacheNickNames;

    private final String _user;
    private final String _userXmpp;

    private static final Logger _logger = Logger.getLogger( PacketListenerForRosterEntries.class );
}
