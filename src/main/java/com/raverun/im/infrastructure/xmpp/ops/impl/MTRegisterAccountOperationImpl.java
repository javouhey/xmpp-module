package com.raverun.im.infrastructure.xmpp.ops.impl;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Registration;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.raverun.im.common.Transport;
import com.raverun.im.infrastructure.xmpp.XMPPUtility;
import com.raverun.im.infrastructure.xmpp.ops.RegisterGatewayAccountOperation;
import com.raverun.im.infrastructure.xmpp.smack.GatewayRegistrationPacketExtension;
import com.raverun.im.infrastructure.xmpp.smack.XMPPConnectionIF;
import com.raverun.im.infrastructure.xmpp.support.XMPPFault;

public class MTRegisterAccountOperationImpl implements
    RegisterGatewayAccountOperation
{

    /**
     * @throws IllegalStateException if we are not connected or authenticated
     * @throws XMPPFault if an XMPPException was encountered
     */
    @Override
    public Boolean call() throws Exception
    {
        checkState();

        String targetJid = _xmppUtility.getTransportJID( _transport );

        HashMap<String,String> attrs = new HashMap<String,String>();
        attrs.put( "username", _userid );
        attrs.put( "password", _password ); 

        Registration reg = new Registration();
        reg.setTo( targetJid );
        reg.setType( IQ.Type.SET );
        reg.setAttributes( attrs );
        reg.addExtension( new GatewayRegistrationPacketExtension() );

        final String origPacketId = reg.getPacketID();
        _logger.debug( "C->S packetid: " + origPacketId );

        try
        {
            PacketFilter myFilter = new PacketFilter() {
                public boolean accept(Packet packet) {
                    final String from = packet.getFrom();
                    final String id = packet.getPacketID();
                    return( from != null && from.startsWith( _transport.code() ) && 
                        id != null && id.equals( origPacketId ));
                }
            };
            PacketCollector collector = _xmppConn.createPacketCollector( myFilter );

            _xmppConn.sendPacket( reg );

            _logger.debug( _userXmpp + " wait for max of " + WAIT_MS + " ms" );
            Packet result = null;
            try
            {
                result = collector.nextResult( WAIT_MS );
                if( result != null )
                    _logger.debug( _userXmpp + " S->C: " + result.toXML() );

                return( result != null );
            }
            finally
            {
                collector.cancel();
            }
        }
        catch( Exception xmppe )
        {
            throw new XMPPFault( _userXmpp + " Register account failed for " + _transport.code() + " due to", xmppe, XMPPFault.XmppFaultCode.REGISTER );
        }
    }

    @AssistedInject
    public MTRegisterAccountOperationImpl( 
        XMPPUtility xmppUtility,
        @Assisted XMPPConnectionIF xmppConn, 
        @Assisted Transport transport,
        @Assisted String userXmpp,
        @Assisted String userid,
        @Assisted String password )
    {
        _xmppUtility = xmppUtility;
        _xmppConn = xmppConn;
        _transport = transport;
        _userXmpp = userXmpp;
        _userid = userid;
        _password = password;
    }

    private void checkState()
    {
        if( !_xmppConn.isConnected() )
            throw new IllegalStateException( "XMPP connection is disconnected");

        if( !_xmppConn.isAuthenticated() )
            throw new IllegalStateException( "XMPP connection is not authenticated");
    }

// ---- constants ----
    private final static long WAIT_MS = 12000;

// ---- immutables ---
    private final String _userid;
    private final String _password;
    private final String _userXmpp;
    private final Transport _transport;
    private final XMPPUtility _xmppUtility;
    private final XMPPConnectionIF _xmppConn;

    private final Logger _logger = Logger.getLogger( MTRegisterAccountOperationImpl.class );
}
