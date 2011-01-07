package com.raverun.im.infrastructure.xmpp.ops.impl;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import com.raverun.im.common.IMConstants;
import com.raverun.im.common.Transport;
import com.raverun.im.infrastructure.xmpp.XMPPUtility;
import com.raverun.im.infrastructure.xmpp.ops.SigninGatewayOperation;
import com.raverun.im.infrastructure.xmpp.ops.SigninResponseProcessor;
import com.raverun.im.infrastructure.xmpp.smack.PresenceUtilityIF;
import com.raverun.im.infrastructure.xmpp.smack.XMPPConnectionIF;
import com.raverun.im.infrastructure.xmpp.support.XMPPFault;
import com.raverun.shared.Configuration;

public class MTSigninGatewayOperationImpl implements SigninGatewayOperation
{
    @AssistedInject
    public MTSigninGatewayOperationImpl( 
        Configuration config,
        XMPPUtility xmppUtility,
        PresenceUtilityIF presenceUtil,
        @Named("presence.status") String statusMessage,
        @Named("resp.msn") SigninResponseProcessor msnProcessor,
        @Named("resp.yahoo") SigninResponseProcessor yahooProcessor,
        @Named("resp.gtalk") SigninResponseProcessor gtalkProcessor,
        @Assisted XMPPConnectionIF xmppConn, 
        @Assisted Transport transport,
        @Assisted String userXmpp )
    {
        _config        = config;
        _xmppUtility   = xmppUtility;
        _presenceUtil  = presenceUtil;
        _statusMessage = statusMessage;

        _msnProcessor   = msnProcessor;
        _yahooProcessor = yahooProcessor;
        _gtalkProcessor = gtalkProcessor;

        _transport = transport;
        _xmppConn = xmppConn;
        _userXmpp = userXmpp;
    }

    /**
     * @throws AssertionError for impossible corner cases
     * @throws IllegalArgumentException for nullable parameters
     * @throws IllegalStateException if we are not connected
     * @throws XMPPFault if an XMPPException was encountered
     */
    @Override
    public SigninGatewayResult call() throws Exception
    {
        _logger.debug( _userXmpp + " :: NEW SIGNIN .. invoked [" + _statusMessage + "]" );
        checkState();

        String targetJID = _xmppUtility.getTransportJID( _transport );

        Presence signInPresence = _presenceUtil.derefClient( 
            String.valueOf( PresenceUtilityIF.MyMode.AVAILABLE.code() ), null );
        signInPresence.setTo( targetJID );
        signInPresence.setStatus( _statusMessage );

        PacketFilter myFilter = new PacketFilter() {
            public boolean accept(Packet packet) {
                final String from = packet.getFrom();
                return( from != null && from.startsWith( _transport.code() ) );
            }
        };

        long waitMs = _config.i( IMConstants.XmppOperation.KEY_CONFIG_SIGNIN_WAIT, (int)WAIT_MS );
        int numOfRetries = _config.i( IMConstants.XmppOperation.KEY_CONFIG_SIGNIN_RETRY, MAX_LOOP );
        _logger.debug( "retry: " + numOfRetries + " | wait in ms: " + waitMs );

        try
        {
            final PacketCollector collector = _xmppConn.createPacketCollector( myFilter );
            _xmppConn.sendPacket( signInPresence );
            //_logger.debug( "C->S: sent signin to " + targetJID );
            _logger.debug( _userXmpp + " C->S: " + signInPresence.toXML() );

            SigninGatewayOperation.SigninGatewayResult toReturn = null;

            // ----------- Retry logic specific to Transport ---------------
            try
            {
                switch( _transport )
                {
                case YAHOO:
                    toReturn = _yahooProcessor.handle( waitMs, numOfRetries, collector );
                    break;
                case GTALK:
                    toReturn = _gtalkProcessor.handle(  waitMs, numOfRetries, collector );
                    break;
                case MSN:
                    toReturn = _msnProcessor.handle(  waitMs, numOfRetries, collector );
                    break;
                default:
                    throw new AssertionError( "Transport not handled in SigninGatewayOperationImpl" );
                }

                if( null == toReturn )
                    return SigninResultImpl.newFor( false, false, null, false, false );

                return toReturn;
            }
            catch( Exception e )
            {
                throw e;
            }
            finally
            {
                collector.cancel();
            }
        }
        catch( XMPPException xmppe )
        {
            throw new XMPPFault( "signin failed for " + _transport.code() + " due to", xmppe, XMPPFault.XmppFaultCode.SIGNIN );
        }
    }

    private void checkState()
    {
        if( !_xmppConn.isConnected() )
            throw new IllegalStateException( "XMPP connection is disconnected");

        if( !_xmppConn.isAuthenticated() )
            throw new IllegalStateException( "XMPP connection is not authenticated");
    }

    public static class SigninResultImpl implements SigninGatewayOperation.SigninGatewayResult
    {
        public static SigninResultImpl newFor( boolean ok, boolean receivedReply, Packet packet,
            boolean invalidLogin, boolean wrongPassword )
        {
            return new SigninResultImpl( ok, receivedReply, packet, invalidLogin, wrongPassword, false );
        }

        public static SigninResultImpl newFor( boolean ok, boolean receivedReply, Packet packet,
            boolean invalidLogin, boolean wrongPassword, boolean is403 )
        {
            return new SigninResultImpl( ok, receivedReply, packet, invalidLogin, wrongPassword, is403 );
        }

        private SigninResultImpl( boolean ok, boolean receivedReply, Packet packet,
            boolean validLogin, boolean wrongPassword, boolean is403 )
        {
            _ok = ok;
            _receivedReply = receivedReply;
            _packet = packet;
            _invalidId = validLogin;
            _wrongPassword = wrongPassword;
            _auth403 = is403;
        }

        public boolean isAuth403() { return _auth403; }
        public boolean didWeReceiveReply() { return _receivedReply; }
        public Packet getReceivedPacket() { return _packet; }
        public boolean isInvalidLoginId() { return _invalidId; }
        public boolean isOk() { return _ok; }
        public boolean isWrongPassword() { return _wrongPassword; }
        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            builder.append( "ok: " ).append( _ok ).append( " | " );
            builder.append( "receivedReply: " ).append( _receivedReply ).append( " | " );
            builder.append( "invalidUserid: " ).append( _invalidId ).append( " | " );
            builder.append( "invalidPasswd: " ).append( _wrongPassword ).append( " | " );
            builder.append( "is403? " ).append( _auth403 );
            return builder.toString();
        }

        Packet _packet;
        boolean _wrongPassword, _invalidId, _ok, _receivedReply, _auth403;
    }

// ---- constants ----
    private final static long WAIT_MS = 5000;
    private final static int MAX_LOOP = 5;

// ---- immutable ----
    private final Configuration _config;
    private final PresenceUtilityIF _presenceUtil;
    private final XMPPUtility _xmppUtility;
    private final String _statusMessage;
    private final String _userXmpp;    
    private final Transport _transport;
    private final XMPPConnectionIF _xmppConn;

    private final SigninResponseProcessor _msnProcessor;
    private final SigninResponseProcessor _gtalkProcessor;
    private final SigninResponseProcessor _yahooProcessor;

    private static final Logger _logger = Logger.getLogger( MTSigninGatewayOperationImpl.class );
}
