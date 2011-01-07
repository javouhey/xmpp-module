package com.raverun.im.infrastructure.xmpp.ops.impl;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;

import com.google.inject.Inject;
import com.raverun.im.common.Transport;
import com.raverun.im.infrastructure.xmpp.XMPPUtility;
import com.raverun.im.infrastructure.xmpp.ops.DiscoTransportOperation;
import com.raverun.im.infrastructure.xmpp.smack.XMPPConnectionIF;
import com.raverun.im.infrastructure.xmpp.support.XMPPFault;
import com.raverun.shared.Constraint.NonNullArgument;

/**
 * @deprecated
 */
public class DiscoTransportOperationImpl implements DiscoTransportOperation
{
    @Inject
    public DiscoTransportOperationImpl( XMPPUtility xmppUtility )
    {
        _xmppUtility = xmppUtility;
    }

    public void init( XMPPConnectionIF xmppConn, Transport transport )
    {
        NonNullArgument.check( transport, "transport" );
        NonNullArgument.check( xmppConn, "xmppConn" );
        _transport = transport;
        _xmppConn = xmppConn;
    }

    /**
     * @throws IllegalArgumentException for nullable parameters
     * @throws IllegalStateException if we are not connected
     * @throws XMPPFault if an XMPPException was encountered
     */
    @Override
    public Boolean call() throws Exception
    {
        NonNullArgument.check( _transport, "_transport" );
        NonNullArgument.check( _xmppConn, "_xmppConn" );

        checkState();

        boolean takenInXMPPServer = false;
        ServiceDiscoveryManager discoManager = 
            ServiceDiscoveryManager.getInstanceFor( _xmppConn.downCast() );

        String theTransportJID = _xmppUtility.getTransportJID( _transport );

        try
        {
            DiscoverInfo discoInfo = discoManager.discoverInfo( theTransportJID );
            takenInXMPPServer = discoInfo.containsFeature( "jabber:iq:registered" );
            return ( (takenInXMPPServer) ? Boolean.TRUE : Boolean.FALSE );
        }
        catch( XMPPException xmppe )
        {
            throw new XMPPFault( "Discovering kraken transports faied due to", xmppe, XMPPFault.XmppFaultCode.DISCO_TRANSPORT );
        }
    }

    private void checkState()
    {
        if( !_xmppConn.isConnected() )
            throw new IllegalStateException( "XMPP connection is disconnected");

        if( !_xmppConn.isAuthenticated() )
            throw new IllegalStateException( "XMPP connection is not authenticated");
    }

    private Transport _transport;
    private XMPPConnectionIF _xmppConn;

    private final XMPPUtility _xmppUtility;
}
