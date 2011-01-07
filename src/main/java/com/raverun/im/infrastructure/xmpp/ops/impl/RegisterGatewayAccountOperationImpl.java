package com.raverun.im.infrastructure.xmpp.ops.impl;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Registration;

import com.google.inject.Inject;
import com.raverun.im.common.Transport;
import com.raverun.im.infrastructure.xmpp.XMPPUtility;
import com.raverun.im.infrastructure.xmpp.ops.RegisterGatewayAccountOperation;
import com.raverun.im.infrastructure.xmpp.smack.GatewayRegistrationPacketExtension;
import com.raverun.im.infrastructure.xmpp.smack.XMPPConnectionIF;
import com.raverun.im.infrastructure.xmpp.support.XMPPFault;
import com.raverun.shared.Constraint;
import com.raverun.shared.Constraint.NonNullArgument;

/**
 * @deprecated not thread safe.
 * @see MTRegisterAccountOperationImpl
 */
public class RegisterGatewayAccountOperationImpl implements
    RegisterGatewayAccountOperation
{
    @Inject
    public RegisterGatewayAccountOperationImpl( XMPPUtility xmppUtility )
    {
        _xmppUtility = xmppUtility;
    }

    public void init( XMPPConnectionIF xmppConn, Transport transport, String userid, String password )
    {
        NonNullArgument.check( xmppConn, "xmppConn" );
        NonNullArgument.check( transport, "transport" );

        if( Constraint.EmptyString.isFulfilledBy( userid ) )
            throw new IllegalArgumentException( "userid null" );

        if( Constraint.EmptyString.isFulfilledBy( password ) )
            throw new IllegalArgumentException( "password null" );

        _xmppConn = xmppConn;
        _transport = transport;
        _userid = userid;
        _password = password;
    }

    /**
     * TODO this method is not catching XMPPException & rethrowing them as XMPPFault. Fix it!
     * <a href="http://www.pivotaltracker.com/story/show/755140">pivotaltracker task 755140</a>
     * 
     * @throws IllegalArgumentException for nullable parameters
     * @throws IllegalStateException if we are not connected
     * @throws XMPPFault if an XMPPException was encountered
     */
    @Override
    public Boolean call() throws Exception
    {
        NonNullArgument.check( _transport, "_transport" );
        NonNullArgument.check( _xmppConn, "_xmppConn" );
        if( Constraint.EmptyString.isFulfilledBy( _userid ) ||
            Constraint.EmptyString.isFulfilledBy( _password ) )
            throw new IllegalArgumentException( "Did you forget to call #init(..)" );

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
        _logger.debug( "C->S packetid: " + reg.getPacketID() );
        final String origPacketId = reg.getPacketID();

        _xmppConn.sendPacket( reg );
        Packet result = null;

        //PacketFilter filter = new FromContainsFilter( targetJid );
        
        PacketFilter myFilter = new PacketFilter() {
            public boolean accept(Packet packet) {
                final String from = packet.getFrom();
                final String id = packet.getPacketID();
                return( from != null && from.startsWith( _transport.code() ) && 
                    id != null && id.equals( origPacketId ));
            }
        };
        PacketCollector collector = _xmppConn.createPacketCollector( myFilter );

        _logger.debug( "wait for max of " + WAIT_MS + " ms" );
        try
        {
            result = collector.nextResult( WAIT_MS );
            if( result != null )
                _logger.debug( "S->C: " + result.toXML() );

            return( result != null );
        }
        finally
        {
            collector.cancel();
        }
    }

    private void checkState()
    {
        if( !_xmppConn.isConnected() )
            throw new IllegalStateException( "XMPP connection is disconnected");

        if( !_xmppConn.isAuthenticated() )
            throw new IllegalStateException( "XMPP connection is not authenticated");
    }

    private String _userid, _password;
    private Transport _transport;
    private XMPPConnectionIF _xmppConn;

    private final static long WAIT_MS = 12000;
    private final XMPPUtility _xmppUtility;
    private final Logger _logger = Logger.getLogger( RegisterGatewayAccountOperationImpl.class );
}

