package com.raverun.im.infrastructure.xmpp.ops.impl;

import javax.annotation.Nonnull;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

import com.raverun.im.infrastructure.xmpp.ops.SigninGatewayOperation;
import com.raverun.im.infrastructure.xmpp.ops.SigninResponseProcessor;
import com.raverun.im.infrastructure.xmpp.ops.SigninGatewayOperation.SigninGatewayResult;
import com.raverun.im.infrastructure.xmpp.ops.impl.MTSigninGatewayOperationImpl.SigninResultImpl;

public class SigninResponseProcessorForMsn implements SigninResponseProcessor
{

    @Override
    public SigninGatewayResult handle( long waitMs, int numOfRetries,
        PacketCollector packetCollector ) throws Exception
    {
        Packet firstPacket = null;
        int loop = 0;
        while( loop < numOfRetries )
        {
            try
            {
                _logger.debug( loop + " wait for max of " + waitMs + " ms" );
                firstPacket =  packetCollector.nextResult( waitMs );
                if( firstPacket == null )
                    continue;
                else
                    break;
            }
            finally
            {
                loop++;
            }
        }

        if( firstPacket == null )
        {
            // means BAD PASSWORD during registration
            _logger.debug( "No response received from kraken. Assume BAD PASSWORD" );
            return BAD_PASSWORD_NO_REPLY;
        }

        return processFirstPacket( firstPacket );
    }

    @Nonnull
    private final SigninGatewayOperation.SigninGatewayResult processFirstPacket( Packet result )
    {
        if( result == null )
            return SigninResultImpl.newFor( false, false, null, false, false ); // NoServerResponse

        _logger.debug( "S->C: " + result.toXML() );
        if( result instanceof Message )
        {
            Message mesg = (Message)result;
            Message.Type type = mesg.getType();
            if( type == Message.Type.headline )
            {
                // assume successful login
                _logger.debug( "successfull MSN login (message.headline)" );
                return SigninResultImpl.newFor( true, true, result, false, false );
            }
            else if( type == Message.Type.error )
            {
                _logger.debug( "invalid MSN userid (message.error)" );
                return SigninResultImpl.newFor( true, true, result, true, false );
            }
        }
        else if( result instanceof IQ )
        {
            IQ iq = (IQ)result;
            if( iq.getType() == IQ.Type.ERROR )
            {
                _logger.debug( "invalid MSN userid (IQ.error)" );
                return SigninResultImpl.newFor( true, true, result, true, false );
            }
        }
        else if( result instanceof Presence )
        {
            Presence presence = (Presence)result;
            if( presence.getType() == Presence.Type.available )
            {
                _logger.debug( "successfull MSN login (presence.available)" );
                return SigninResultImpl.newFor( true, true, result, false, false );
            }
            else if( presence.getType() == Presence.Type.unavailable )
            {
                _logger.debug( "Failed MSN login (presence.unavailable) probably due to bad password" );
                return SigninResultImpl.newFor( false, true, result, false, true );
            }
        }

        _logger.debug( "Default to NoServerResponse although we received => " + result.toXML() );
        return SigninResultImpl.newFor( false, false, null, false, false ); // NoServerResponse
    }

    private final static SigninGatewayOperation.SigninGatewayResult BAD_PASSWORD_NO_REPLY = SigninResultImpl.newFor( false, false, null, false, true );

    private final static String XMPP_DOWN = "Failed to connect to remote XMPP";

    // ----- kraken 1.1.0 -----
    private final static String SEARCH_WRONG_USERNAME_MSN = "Your registration was denied because the username you provided was not valid for the service";

    // ----- kraken 1.0.0 & the old gateway.jar ----
    //private final static String SEARCH_WRONG_USERNAME_MSN_OLD = "You are registered with the MSN transport with an illegal account name";

    private final Logger _logger = Logger.getLogger( SigninResponseProcessorForMsn.class );
}
