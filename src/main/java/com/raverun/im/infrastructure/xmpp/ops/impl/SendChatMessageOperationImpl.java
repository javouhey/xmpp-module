package com.raverun.im.infrastructure.xmpp.ops.impl;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.packet.Message;

import com.google.inject.Inject;
import com.raverun.im.common.Transport;
import com.raverun.im.infrastructure.xmpp.XMPPUtility;
import com.raverun.im.infrastructure.xmpp.ops.SendChatMessageOperation;
import com.raverun.im.infrastructure.xmpp.smack.XMPPConnectionIF;
import com.raverun.im.infrastructure.xmpp.support.XMPPFault;
import com.raverun.shared.Configuration;
import com.raverun.shared.Constraint.NonNullArgument;

public class SendChatMessageOperationImpl implements SendChatMessageOperation
{
    /**
     * @throws XMPPFault
     * @throws IllegalStateException if connection to XMPP is not alive
     */
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

    @Inject
    public SendChatMessageOperationImpl( XMPPUtility xmppUtility, 
        Configuration config  )
    {
        _config        = config;
        _xmppUtility   = xmppUtility;
    }

    /**
     * @deprecated 
     */
    public void init( XMPPConnectionIF xmppConn, Transport transport,
        String message, String from, String to )
    {
        NonNullArgument.check( transport, "transport" );
        NonNullArgument.check( xmppConn, "xmppConn" );
        NonNullArgument.check( from, "from" );
        NonNullArgument.check( to, "to" );

        _transport = transport;
        _xmppConn  = xmppConn;
        _message   = message;
        _from      = from;
        _to        = to;
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
    private String           _message;
    private String           _from;
    private String           _to;

// ---- immutable ----
    private final Configuration _config;
    private final XMPPUtility  _xmppUtility;

    private final Logger _logger = Logger.getLogger( SendChatMessageOperationImpl.class );
}
