package com.raverun.im.infrastructure.xmpp.ops.impl;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.raverun.im.common.Transport;
import com.raverun.im.infrastructure.xmpp.XMPPUtility;
import com.raverun.im.infrastructure.xmpp.ops.DiscoTransportOperation;
import com.raverun.im.infrastructure.xmpp.smack.XMPPConnectionIF;
import com.raverun.im.infrastructure.xmpp.support.XMPPFault;

public class MTDiscoTransportOperationImpl implements DiscoTransportOperation
{
    /**
     * @throws IllegalStateException if we are not connected
     * @throws XMPPFault if an XMPPException was encountered
     */
    @Override
    public Boolean call() throws Exception
    {
        checkState();

        boolean takenInXMPPServer = false;
        ServiceDiscoveryManager discoManager = 
            ServiceDiscoveryManager.getInstanceFor( _xmppConn.downCast() );

        String theTransportJID = _xmppUtility.getTransportJID( _transport );

        try
        {
            DiscoverInfo discoInfo = discoManager.discoverInfo( theTransportJID );
            takenInXMPPServer = discoInfo.containsFeature( NAMESPACE_REGISTERED );
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

    @AssistedInject
    public MTDiscoTransportOperationImpl(
        XMPPUtility xmppUtility,
        @Assisted XMPPConnectionIF xmppConn, 
        @Assisted Transport transport )
    {
        _xmppUtility = xmppUtility;
        _transport = transport;
        _xmppConn = xmppConn;
    }

// ---- constants ----
    private final String NAMESPACE_REGISTERED = "jabber:iq:registered";

// ---- immutables ---
    private final Transport _transport;
    private final XMPPUtility _xmppUtility;
    private final XMPPConnectionIF _xmppConn;

    private final Logger _logger = Logger.getLogger( MTDiscoTransportOperationImpl.class );
}
