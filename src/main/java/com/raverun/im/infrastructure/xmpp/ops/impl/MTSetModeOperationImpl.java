package com.raverun.im.infrastructure.xmpp.ops.impl;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.packet.Presence;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.raverun.im.common.Transport;
import com.raverun.im.infrastructure.xmpp.XMPPUtility;
import com.raverun.im.infrastructure.xmpp.ops.SetModeOperation;
import com.raverun.im.infrastructure.xmpp.smack.PresenceUtilityIF;
import com.raverun.im.infrastructure.xmpp.smack.XMPPConnectionIF;
import com.raverun.im.infrastructure.xmpp.smack.PresenceUtilityIF.MyMode;
import com.raverun.im.infrastructure.xmpp.support.XMPPFault;
import com.raverun.shared.Common;

public class MTSetModeOperationImpl implements SetModeOperation
{

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
            return null;
        }
        catch( Exception e )
        {
            throw new XMPPFault( Common.EMPTY_STRING, e, XMPPFault.XmppFaultCode.SET_MODE );
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
    public MTSetModeOperationImpl( 
        XMPPUtility xmppUtility,
        PresenceUtilityIF presenceUtility,
        @Assisted XMPPConnectionIF xmppConn,
        @Assisted Set<Transport> setOfTransport,
        @Assisted MyMode mode, 
        @Assisted String status )
    {
        _presenceUtility = presenceUtility;
        _xmppUtility     = xmppUtility;
        _xmppConn        = xmppConn;
        _status          = status;
        _mode            = mode;

        _setOfTransport = new HashSet<Transport>();
        _setOfTransport.addAll( setOfTransport );
    }

    private final MyMode _mode;
    private final String _status;
    private final XMPPUtility _xmppUtility;
    private final XMPPConnectionIF _xmppConn;
    private final Set<Transport> _setOfTransport;
    private final PresenceUtilityIF _presenceUtility;

    private final Logger _logger = Logger.getLogger( MTSetModeOperationImpl.class );
}
