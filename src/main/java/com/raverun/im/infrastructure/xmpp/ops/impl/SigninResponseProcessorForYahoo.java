package com.raverun.im.infrastructure.xmpp.ops.impl;

import javax.annotation.Nullable;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.XMPPError;

import com.raverun.im.common.Transport;
import com.raverun.im.infrastructure.xmpp.ops.SigninGatewayOperation;
import com.raverun.im.infrastructure.xmpp.ops.SigninResponseProcessor;
import com.raverun.im.infrastructure.xmpp.ops.SigninGatewayOperation.SigninGatewayResult;
import com.raverun.im.infrastructure.xmpp.ops.impl.SigninGatewayOperationImpl.SigninResultImpl;

/**
 * C->S Presence.available is followed by either one 
 * <ul>
 * <li>Message.error
 * <li>Presence.available
 * </ul>
 *
 * @author gavin bong
 */
public class SigninResponseProcessorForYahoo implements SigninResponseProcessor
{

    @Override
    public SigninGatewayResult handle( long waitMs, int numOfRetries,
        PacketCollector packetCollector ) throws Exception
    {
        SigninGatewayOperation.SigninGatewayResult toReturn = null;
        int loop = 0;
        while( loop < numOfRetries )
        {
            try
            {
                _logger.debug( loop + " wait for max of " + waitMs + " ms" );
                toReturn = waitForRecognizableSigninResult( 
                    packetCollector.nextResult( waitMs ) );
                if( toReturn != null )
                    break;
            }
            finally
            {
                loop++;
            }
        }

        if( null == toReturn )
        {
            _logger.debug( "No expected response from Yahoo (last option)" );
            return SigninResultImpl.newFor( false, false, null, false, false ); // NoServerResponse
        }
        else
            return toReturn;
    }

    /**
     * @throws IllegalStateException if an edge case was not handled
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
                if( isInvalidPassword( body ) )
                {
                    _logger.debug( "Invalid password (message.error)" );
                    return SigninResultImpl.newFor( false, true, result, false, true );
                }
                else if( isInvalidUserId( body ) )
                {
                    _logger.debug( "Invalid userid (message.error)" );
                    return SigninResultImpl.newFor( false, true, result, true, false );
                }
                else if( isRemoteXmppServerDown( body ) )
                {
                    _logger.debug( "yahoo's server is down (message.error)" );
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
                {
                    _logger.debug( "Skipped. No errorcode (presence.unavailable)" );
                    return null;
                }

                int errorCode = error.getCode();
                _logger.debug( "Found errorCode -> " + errorCode + " (presence.unavailable)");
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

    private final boolean isRemoteXmppServerDown( String errorSnippet )
    {
        return errorSnippet.indexOf( XMPP_DOWN ) != -1;
    }

    private final boolean isInvalidUserId( String errorSnippet )
    {
        return ( (errorSnippet.indexOf( SEARCH_WRONG_USERNAME_YAHOO ) != -1) || 
            (errorSnippet.indexOf( SEARCH_ACCOUNTLOCKED_YAHOO ) != -1 ) );
    }

    private final boolean isInvalidPassword( String errorSnippet )
    {
        return (( errorSnippet.indexOf( SEARCH_WRONG_PASSWORD_YAHOO ) != -1 ) ||
            ( errorSnippet.indexOf( SEARCH_WRONG_PASSWORD_YAHOO2 ) != -1 ) );
    }

    /**
     * @deprecated
     */
    private final static int MAXLOOP = 4;

    private final static Transport _transport = Transport.YAHOO;

    private final static String XMPP_DOWN = "Failed to connect to remote XMPP";

    // <body>Login refused by Yahoo!, perhaps because of a bad password.  Please re-register with correct password.</body>
    private final static String SEARCH_WRONG_PASSWORD_YAHOO = "re-register with correct password";

    // @TODO questionable use?????
    private final static String SEARCH_WRONG_PASSWORD_YAHOO2 = "Failed to log into Yahoo";

    // IMPORTANT
    private final static String SEARCH_ACCOUNTLOCKED_YAHOO = "account is locked";

    // <body>Yahoo! did not recognize the username you registered with.  Please re-register with correct username.</body>
    private final static String SEARCH_WRONG_USERNAME_YAHOO = "did not recognize the username you registered with";

    private final Logger _logger = Logger.getLogger( SigninResponseProcessorForYahoo.class );
}
