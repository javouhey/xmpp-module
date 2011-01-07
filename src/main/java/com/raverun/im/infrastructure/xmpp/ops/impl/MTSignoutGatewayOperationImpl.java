package com.raverun.im.infrastructure.xmpp.ops.impl;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.packet.Presence;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.raverun.im.common.Transport;
import com.raverun.im.infrastructure.xmpp.XMPPUtility;
import com.raverun.im.infrastructure.xmpp.ops.SignoutGatewayOperation;
import com.raverun.im.infrastructure.xmpp.smack.PresenceUtilityIF;
import com.raverun.im.infrastructure.xmpp.smack.XMPPConnectionIF;
import com.raverun.im.infrastructure.xmpp.support.XMPPFault;

public class MTSignoutGatewayOperationImpl implements SignoutGatewayOperation
{
    @AssistedInject
    public MTSignoutGatewayOperationImpl( 
        XMPPUtility xmppUtility,
        PresenceUtilityIF presenceUtil,
        @Assisted XMPPConnectionIF xmppConn, 
        @Assisted Transport transport,
        @Assisted String userXmpp )
    {
        _presenceUtil = presenceUtil;
        _xmppUtility = xmppUtility;
        _transport = transport;
        _xmppConn = xmppConn;
        _userXmpp = userXmpp;
    }

    /**
     * @throws IllegalArgumentException for nullable parameters
     * @throws IllegalStateException if we are not connected/authenticated
     * @throws XMPPFault if an XMPPException was encountered
     */
    @Override
    public Void call() throws Exception
    {
        checkState();

        String targetJID = _xmppUtility.getTransportJID( _transport );

        Presence signOutPresence = _presenceUtil.derefClient( 
            String.valueOf( PresenceUtilityIF.MyMode.UNAVAILABLE.code() ), null );
        signOutPresence.setTo( targetJID );

        try
        {
            _xmppConn.sendPacket( signOutPresence );
            _logger.debug( _userXmpp + " C->S: sent signout to " + targetJID );
            return null;
        }
        catch( Exception e )
        {
            throw new XMPPFault( "signout failed for " + _transport.code() + " due to", e, XMPPFault.XmppFaultCode.SIGNOUT );
        }
    }

    private void checkState()
    {
        if( !_xmppConn.isConnected() )
            throw new IllegalStateException( "XMPP connection is disconnected");

        if( !_xmppConn.isAuthenticated() )
            throw new IllegalStateException( "XMPP connection is not authenticated");
    }

 // ---- immutable ----
    private final PresenceUtilityIF _presenceUtil;
    private final XMPPConnectionIF _xmppConn;
    private final XMPPUtility _xmppUtility;
    private final Transport _transport;
    private final String _userXmpp;

    private static final Logger _logger = Logger.getLogger( MTSignoutGatewayOperationImpl.class );
}
