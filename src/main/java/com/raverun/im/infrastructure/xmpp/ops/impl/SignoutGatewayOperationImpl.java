package com.raverun.im.infrastructure.xmpp.ops.impl;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.packet.Presence;

import com.google.inject.Inject;
import com.raverun.im.common.Transport;
import com.raverun.im.infrastructure.xmpp.XMPPUtility;
import com.raverun.im.infrastructure.xmpp.ops.SignoutGatewayOperation;
import com.raverun.im.infrastructure.xmpp.smack.PresenceUtilityIF;
import com.raverun.im.infrastructure.xmpp.smack.XMPPConnectionIF;
import com.raverun.im.infrastructure.xmpp.support.XMPPFault;
import com.raverun.shared.Constraint.NonNullArgument;

/**
 * @deprecated not thread safe.
 * @see MTSignoutGatewayOperationImpl
 */
public class SignoutGatewayOperationImpl implements SignoutGatewayOperation
{
    @Inject
    public SignoutGatewayOperationImpl( XMPPUtility xmppUtility, 
        PresenceUtilityIF presenceUtil )
    {
        _xmppUtility  = xmppUtility;
        _presenceUtil = presenceUtil;
    }

    public void init( XMPPConnectionIF xmppConn, Transport transport )
    {
        NonNullArgument.check( transport, "transport" );
        NonNullArgument.check( xmppConn, "xmppConn" );
        _transport = transport;
        _xmppConn = xmppConn;
    }

    /**
     * @throws AssertionError for impossible corner cases
     * @throws IllegalArgumentException for nullable parameters
     * @throws IllegalStateException if we are not connected
     * @throws XMPPFault if an XMPPException was encountered
     */
    @Override
    public Void call() throws Exception
    {
        NonNullArgument.check( _transport, "_transport" );
        NonNullArgument.check( _xmppConn, "_xmppConn" );

        checkState();

        String targetJID = _xmppUtility.getTransportJID( _transport );

        Presence signOutPresence = _presenceUtil.derefClient( 
            String.valueOf( PresenceUtilityIF.MyMode.UNAVAILABLE.code() ), null );
        signOutPresence.setTo( targetJID );

        try
        {
            _xmppConn.sendPacket( signOutPresence );
            _logger.debug( "C->S: sent signout to " + targetJID );
            return null; // doesn't mean anything. Just to satisfy the API
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

 // ---- mutable ------
    private Transport _transport;
    private XMPPConnectionIF _xmppConn;

// ---- immutable ----
    private final PresenceUtilityIF _presenceUtil;
    private final XMPPUtility _xmppUtility;

    private final Logger _logger = Logger.getLogger( SignoutGatewayOperationImpl.class );
}
