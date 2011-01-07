package com.raverun.im.domain.impl;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.raverun.im.application.JIDMapper;
import com.raverun.im.common.Transport;
import com.raverun.im.domain.IMIdentity;
import com.raverun.im.infrastructure.persistence.MessageService;
import com.raverun.im.infrastructure.xmpp.XMPPUtility;
import com.raverun.shared.Constraint;

/**
 * Responsible for 2 types of messages:
 * <ul>
 * <li>headlines ({@code mim_msg_headline} )
 * <li>chat ({@code mim_msg_chat} )
 * </ul>
 *
 * @author Gavin Bong
 */
public class PacketListenerForMessage implements PacketListener
{
    @AssistedInject
    public PacketListenerForMessage( XMPPUtility xmppUtility, 
        JIDMapper jidMapper, MessageService msgService,
        @Assisted String user, @Assisted String userXmpp )
    {
        _user = user; _userXmpp = userXmpp; 
        _label = label();

        _xmppUtility = xmppUtility;
        _msgService = msgService;
        _jidMapper = jidMapper;
    }

    @Override
    public void processPacket( Packet packet )
    {
        Message m = (Message)packet;
        _logger.debug( _label + m.toXML() );

        final String from = m.getFrom();
        final Transport transport = _xmppUtility.decodeTransportFor( from );

        String body = m.getBody();
        if( Constraint.EmptyString.isFulfilledBy( body ))
            return;

        switch( m.getType() )
        {
        case chat:
            IMIdentity canonicalReceiver = _jidMapper.getCanonicalReceiver( transport, _user, _userXmpp );
            String canonicalSender = _jidMapper.getCanonicalSender( _xmppUtility.decodeSender( from ), transport );
            if( body.length() > MAX_BODY_CHAT )
                body = body.substring( 0, MAX_BODY_CHAT );

            IMMessageChatImpl.FromDatabaseBuilder chatBuilder = new IMMessageChatImpl.FromDatabaseBuilder( _user, canonicalSender, canonicalReceiver.imId(), transport, body );
            _msgService.add( chatBuilder.build() );
            break;

        case headline:
            canonicalReceiver = _jidMapper.getCanonicalReceiver( transport, _user, _userXmpp );
            if( body.length() > MAX_BODY_HEADLINE )
                body = body.substring( 0, MAX_BODY_HEADLINE );
            
            if( transport == Transport.GTALK )
            {
                int colonIndex = body.indexOf( ":" );
                if( colonIndex != -1 )
                    body = body.substring( 0, colonIndex );
            }

            IMMessageHeadlineImpl.FromDatabaseBuilder headlineBuilder = new IMMessageHeadlineImpl.FromDatabaseBuilder( _user, canonicalReceiver.imId(), transport, body );
            _msgService.add( headlineBuilder.build() );
            break;

        default:
        }
    }

    private final String label()
    {
        StringBuilder builder = new StringBuilder();
        return builder.append( "[" ).append( _user ).append( " : " ).append( _userXmpp ).append( "]" ).toString();
    }

    private final static int MAX_BODY_CHAT = 512;
    private final static int MAX_BODY_HEADLINE = 128;

    private final String _label;
    private final String _user;
    private final String _userXmpp;
    private final XMPPUtility _xmppUtility;
    private final MessageService _msgService;
    private final JIDMapper _jidMapper;

    private static final Logger _logger = Logger.getLogger( PacketListenerForMessage.class );
}
