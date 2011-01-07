package com.raverun.im.domain.impl;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class PacketListenerForIQ implements PacketListener
{
    @AssistedInject
    public PacketListenerForIQ( @Assisted String user, @Assisted String userXmpp )
    {
        _user = user; _userXmpp = userXmpp; 
        _label = label();
    }

    @Override
    public void processPacket( Packet packet )
    {
        IQ iq = (IQ)packet;
        _logger.debug( _label + iq.toXML() );
    }

    private final String label()
    {
        StringBuilder builder = new StringBuilder();
        return builder.append( "[" ).append( _user ).append( " : " ).append( _userXmpp ).append( "]" ).toString();
    }

    private final String _label;
    private final String _user;
    private final String _userXmpp;

    private static final Logger _logger = Logger.getLogger( PacketListenerForIQ.class );
}
