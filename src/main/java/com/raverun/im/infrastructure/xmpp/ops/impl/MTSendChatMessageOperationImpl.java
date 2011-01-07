package com.raverun.im.infrastructure.xmpp.ops.impl;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.packet.Message;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.raverun.im.common.Transport;
import com.raverun.im.infrastructure.xmpp.XMPPUtility;
import com.raverun.im.infrastructure.xmpp.ops.SendChatMessageOperation;
import com.raverun.im.infrastructure.xmpp.smack.XMPPConnectionIF;
import com.raverun.im.infrastructure.xmpp.support.XMPPFault;

public class MTSendChatMessageOperationImpl implements SendChatMessageOperation
{
    @Override
    public Void call() throws Exception
    {
        checkState();

        String targetJID = _xmppUtility.newJIDfor( _to, _transport );
        String sourceJID = _xmppUtility.newJIDfor( _from, _transport );

        Message msg = new Message( targetJID, Message.Type.chat );
        msg.setFrom( sourceJID );
        msg.setBody( _message );

        try
        {
            _logger.debug( "C->S: " + msg.toXML() );
            _xmppConn.sendPacket( msg );
            return null;
        }
        catch( Exception e )
        {
            throw new XMPPFault( "sending failed for " + _transport.code() + " due to", e, XMPPFault.XmppFaultCode.SENDMSG );
        }
    }

    @AssistedInject
    public MTSendChatMessageOperationImpl(
        XMPPUtility xmppUtility,
        @Assisted XMPPConnectionIF xmppConn,
        @Assisted Transport transport,
        @Assisted String from,
        @Assisted String to,
        @Assisted String message )
    {
        _xmppUtility = xmppUtility;
        _transport = transport;
        _xmppConn = xmppConn;

        _to = to;
        _from = from;
        _message = message;
    }

    private void checkState()
    {
        if( !_xmppConn.isConnected() )
            throw new IllegalStateException( "XMPP connection is disconnected");

        if( !_xmppConn.isAuthenticated() )
            throw new IllegalStateException( "XMPP connection is not authenticated");
    }

 // ---- immutables ---
    private final String _to;
    private final String _from;
    private final String _message;
    private final Transport _transport;
    private final XMPPUtility _xmppUtility;
    private final XMPPConnectionIF _xmppConn;

    private final Logger _logger = Logger.getLogger( MTSendChatMessageOperationImpl.class );
}
