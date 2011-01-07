package com.raverun.im.infrastructure.xmpp.ops.impl;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.packet.Presence;

import com.google.inject.Inject;
import com.raverun.im.common.Transport;
import com.raverun.im.infrastructure.xmpp.XMPPUtility;
import com.raverun.im.infrastructure.xmpp.ops.SetModeOperation;
import com.raverun.im.infrastructure.xmpp.smack.PresenceUtilityIF;
import com.raverun.im.infrastructure.xmpp.smack.XMPPConnectionIF;
import com.raverun.im.infrastructure.xmpp.smack.PresenceUtilityIF.MyMode;
import com.raverun.im.infrastructure.xmpp.support.XMPPFault;
import com.raverun.shared.Configuration;
import com.raverun.shared.Constraint.NonNullArgument;

/**
 * @deprecated
 */
public class SetModeOperationImpl implements SetModeOperation
{
    /**
     * @throws XMPPFault
     * @throws IllegalStateException if connection to XMPP is not alive
     */
    @Override
    public Void call() throws Exception
    {
        checkState();

        try
        {
            // inform MIM
            Presence presence = _presenceUtility.derefClient( _mode, _status );
            _logger.debug( "[setMode] MIM: " + presence.toXML() );
            _xmppConn.sendPacket( presence );

            for( Transport transport : _setOfTransport )
            {
                Presence tempPresence = _presenceUtility.derefClient( _mode, _status );
                tempPresence.setTo( _xmppUtility.getTransportJID( transport ) );
                _logger.debug( "[setMode] " + transport.code() + ": " + tempPresence.toXML() );
                _xmppConn.sendPacket( tempPresence );
            }
            return null; // to satisfy java
        }
        catch( Exception e )
        {
            throw new XMPPFault( "", e, XMPPFault.XmppFaultCode.SET_MODE );
        }
    }

    public void init( XMPPConnectionIF xmppConn, Set<Transport> setOfTransport,
        MyMode mode, String status )
    {
        NonNullArgument.check( setOfTransport, "setOfTransport" );
        NonNullArgument.check( xmppConn, "xmppConn" );
        NonNullArgument.check( status, "status" );
        NonNullArgument.check( mode, "mode" );

        _xmppConn  = xmppConn;
        _status    = status;
        _mode      = mode;

        _setOfTransport = new HashSet<Transport>();
        _setOfTransport.addAll( setOfTransport );
    }

    @Inject
    public SetModeOperationImpl( XMPPUtility xmppUtility, 
        Configuration config, PresenceUtilityIF presenceUtility  )
    {
        _config          = config;
        _xmppUtility     = xmppUtility;
        _presenceUtility = presenceUtility;
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
    private Set<Transport>   _setOfTransport;
    private String           _status;
    private MyMode           _mode;

// ---- immutable ----
    private final PresenceUtilityIF _presenceUtility;
    private final Configuration     _config;
    private final XMPPUtility       _xmppUtility;

    private final Logger _logger = Logger.getLogger( SetModeOperationImpl.class );
}
