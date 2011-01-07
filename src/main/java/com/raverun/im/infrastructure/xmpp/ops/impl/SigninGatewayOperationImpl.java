package com.raverun.im.infrastructure.xmpp.ops.impl;

import javax.annotation.Nullable;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.XMPPError;

import com.google.inject.Inject;
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
import com.raverun.shared.Constraint.NonNullArgument;

/**
 * @deprecated not thread safe.
 * @see MTSigninGatewayOperationImpl
 */
public class SigninGatewayOperationImpl implements SigninGatewayOperation
{
    @Inject
    public SigninGatewayOperationImpl( XMPPUtility xmppUtility, 
        PresenceUtilityIF presenceUtil, Configuration config,
        @Named("presence.status") String statusMessage,
        @Named("resp.msn") SigninResponseProcessor msnProcessor,
        @Named("resp.yahoo") SigninResponseProcessor yahooProcessor,
        @Named("resp.gtalk") SigninResponseProcessor gtalkProcessor )
    {
        _config        = config;
        _xmppUtility   = xmppUtility;
        _presenceUtil  = presenceUtil;
        _statusMessage = statusMessage;

        _msnProcessor   = msnProcessor;
        _yahooProcessor = yahooProcessor;
        _gtalkProcessor = gtalkProcessor;
    }

    public void init( XMPPConnectionIF xmppConn, Transport transport )
    {
        NonNullArgument.check( transport, "transport" );
        NonNullArgument.check( xmppConn, "xmppConn" );
        _transport = transport;
        _xmppConn = xmppConn;
    }

    /**
     * If the registered account has an invalid userid, we expect to receive 3 stanzas in succession. e.g.
     * <ul>
     * <li><pre>&lt;presence to="a0969330-42b5-11de-accd-020054554e01@gavin-pc/Smack" from="msn.gavin-pc" type="unavailable"><status></status>&lt;/presence></pre>
     * <li><pre>&lt;presence to="a0969330-42b5-11de-accd-020054554e01@gavin-pc" from="msn.gavin-pc" type="unavailable">&lt;/presence></pre>
     * <li><pre>&lt;message to="a0969330-42b5-11de-accd-020054554e01@gavin-pc/Smack" from="msn.gavin-pc" type="error">&lt;body>The password you registered with is incorrect.  Please re-register with the correct password.&lt;/body&gt;&lt;/message&gt;</pre>
     * </ul>
     *
     * @throws AssertionError for impossible corner cases
     * @throws IllegalArgumentException for nullable parameters
     * @throws IllegalStateException if we are not connected
     * @throws XMPPFault if an XMPPException was encountered
     */
    public SigninGatewayOperation.SigninGatewayResult call() throws Exception
    {
        NonNullArgument.check( _transport, "_transport" );
        NonNullArgument.check( _xmppConn, "_xmppConn" );

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
            _logger.debug( "C->S: " + signInPresence.toXML() );

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

    /**
     * @return null to mean we did not receive anything we wanted
     * @throws IllegalStateException when a totally unexpected scenario occurs
     * @deprecated
     */
    @Nullable
    private final SigninGatewayOperation.SigninGatewayResult waitForRecognizableSigninResult( Packet result )
    {
        if( result == null ) 
            return null;

        _logger.debug( "S->C: " + result.toXML() );
        if( result instanceof Message )
        {
            Message mesg = (Message)result;
            if( mesg.getType() != Message.Type.error )
                return null;

            String body = mesg.getBody();
            if( body != null )
            {
                if( isInvalidPassword( _transport, body ) )
                {
                    return SigninResultImpl.newFor( false, true, result, false, true );
                }
                else if( isInvalidUserId( _transport, body ) )
                {
                    return SigninResultImpl.newFor( false, true, result, true, false );
                }
                else if( isRemoteXmppServerDown( _transport, body ) )
                {
                    return SigninResultImpl.newFor( false, true, result, false, false );
                }
                else
                    throw new IllegalStateException( "New Message.getBody() to handle" );
            }
            else
                throw new IllegalStateException( "Message.getBody() should not be null for an error packet!" );
        }
        else if( result instanceof Presence )
        {
            Presence presence = (Presence)result;
            _logger.debug( "Presence.Type is " + presence.getType() );
            if( presence.getType() == Presence.Type.unavailable )
            {
                XMPPError error = presence.getError();
                if( error == null )
                    return null;

                int errorCode = error.getCode();
                _logger.debug( "presence.error with errorCode -> " + errorCode );
                switch( errorCode )
                {
                case 403:
                    return SigninResultImpl.newFor( false, true, result, false, false, true );
                default:
                    throw new IllegalStateException( "errorcode " + errorCode + " is not handled. Fix your code" );
                }
            }

            return SigninResultImpl.newFor( (presence.getType() == Presence.Type.available), true, result, false, false );
        }
        else if( result instanceof IQ )
        {
            _logger.debug( "Ignoring IQ for SigninGatewayOperation" );
            return null;
        }
        else
        {
            return null;
        }
    }

    /** 
     * @deprecated
     */
    private boolean isRemoteXmppServerDown( Transport transport, String errorSnippet )
    {
        return errorSnippet.indexOf( "Failed to connect to remote XMPP" ) != -1;
    }

    /** 
     * @deprecated
     */
    private boolean isInvalidUserId( Transport transport, String errorSnippet )
    {
        switch( transport )
        {
        case YAHOO:
            return ( (errorSnippet.indexOf( SEARCH_WRONG_USERNAME_YAHOO ) != -1) || 
                     (errorSnippet.indexOf( SEARCH_ACCOUNTLOCKED_YAHOO ) != -1 ) );

        case MSN:
            return errorSnippet.indexOf( SEARCH_WRONG_USERNAME_MSN ) != -1;

        case GTALK:
            return false;

        default: // TODO
            return true;
        }
    }

    /**
     * @deprecated
     */
    private boolean isInvalidPassword( Transport transport, String errorSnippet )
    {
        switch( transport )
        {
        case YAHOO:
            return (( errorSnippet.indexOf( SEARCH_WRONG_PASSWORD_YAHOO ) != -1 ) ||
                    ( errorSnippet.indexOf( SEARCH_WRONG_PASSWORD_YAHOO2 ) != -1 ) );

        case MSN:
        case GTALK:
            System.out.println( "isInvalidPassword for MSN/GTALK" );
            return errorSnippet.indexOf( SEARCH_WRONG_PASSWORD_MSN ) != -1;

        default: // TODO
            return true;
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
    // YES
    private final static String SEARCH_WRONG_PASSWORD_GTALK = "Failed to connect to remote XMPP server";
    // YES
    private final static String SEARCH_WRONG_USERNAME_GTALK = "The password you registered with is incorrect.  Please re-register with the correct password.";

    private final static String SEARCH_WRONG_PASSWORD_MSN = "re-register with the correct password";

    // <body>Login refused by Yahoo!, perhaps because of a bad password.  Please re-register with correct password.</body>
    private final static String SEARCH_WRONG_PASSWORD_YAHOO = "re-register with correct password";

    // @TODO questionable use?????
    private final static String SEARCH_WRONG_PASSWORD_YAHOO2 = "Failed to log into Yahoo";
    private final static String SEARCH_ACCOUNTLOCKED_YAHOO = "account is locked";

    // <body>Yahoo! did not recognize the username you registered with.  Please re-register with correct username.</body>
    private final static String SEARCH_WRONG_USERNAME_YAHOO = "did not recognize the username you registered with";

    // ----- kraken 1.1.0 -----
    private final static String SEARCH_WRONG_USERNAME_MSN = "Your registration was denied because the username you provided was not valid for the service";

    // ----- kraken 1.0.0 & the old gateway.jar ----
    //private final static String SEARCH_WRONG_USERNAME_MSN_OLD = "You are registered with the MSN transport with an illegal account name";

    private final static long WAIT_MS = 5000;
    private final static int MAX_LOOP = 5;

// ---- mutable ------
    private Transport _transport;
    private XMPPConnectionIF _xmppConn;

// ---- immutable ----
    private final Configuration _config;
    private final PresenceUtilityIF _presenceUtil;
    private final XMPPUtility _xmppUtility;
    private final String _statusMessage;

    private final SigninResponseProcessor _msnProcessor;
    private final SigninResponseProcessor _gtalkProcessor;
    private final SigninResponseProcessor _yahooProcessor;

    private final Logger _logger = Logger.getLogger( SigninGatewayOperationImpl.class );
}
