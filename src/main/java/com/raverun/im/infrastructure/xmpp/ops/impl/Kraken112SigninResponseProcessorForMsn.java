package com.raverun.im.infrastructure.xmpp.ops.impl;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

import com.raverun.im.infrastructure.xmpp.ops.SigninResponseProcessor;
import com.raverun.im.infrastructure.xmpp.ops.SigninGatewayOperation.SigninGatewayResult;
import com.raverun.im.infrastructure.xmpp.ops.impl.MTSigninGatewayOperationImpl.SigninResultImpl;

public class Kraken112SigninResponseProcessorForMsn implements
    SigninResponseProcessor
{

    @Override
    public SigninGatewayResult handle( long waitMs, int numOfRetries,
        PacketCollector packetCollector ) throws Exception
    {
        MSNState state = MSNState.PRESENCE_AVAILABLE_SENT;
        SigninGatewayResult retval = null;

        // Packets of interest. Does not include IQ or other ignored packets
        int numberOfPacketsSeen = 0;

        while( state != MSNState.THE_END )
        {
            if( numberOfPacketsSeen > 8 )
            {
                _logger.warn( state.name() + "\tABORTED // We have seen 8 packets without any resolution." );
                retval = SigninResultImpl.newFor( false, false, null, false, false ); // NoServerResponse
                break;
            }

            switch( state )
            {
            case PRESENCE_AVAILABLE_SENT: {
                    Packet packet = waitForPresence( state, packetCollector, waitMs, 6 );
                    if( packet == null )
                        state = MSNState.NO_SERVER_RESPONSE;
                    else
                    {
                        ++numberOfPacketsSeen;

                        Presence presence = (Presence)packet;
                        if( presence.getType() == Presence.Type.unavailable )
                        {
                            state = MSNState.FIRST_PRESENCE_UNAVAILABLE_RECEIVED;
                        }
                        else if( presence.getType() == Presence.Type.available )
                        {
                            retval = SigninResultImpl.newFor( true, true, packet, false, false );
                            state = MSNState.THE_END;
                        }
                    }
                }
                break;
            case FIRST_PRESENCE_UNAVAILABLE_RECEIVED: {
                    Packet packet = waitForPresenceOrErrorMessage( state, packetCollector, waitMs, 4 );
                    if( packet == null )
                        state = MSNState.NO_SERVER_RESPONSE;
                    else
                    {
                        ++numberOfPacketsSeen;

                        if( packet instanceof Presence )
                        {
                            Presence presence = (Presence)packet;
                            if( presence.getType() == Presence.Type.unavailable )
                            {
                                _logger.debug( "(x) MSN :: 2 consecutive presence.unavailable .. probably due to bad password" );
                                retval = SigninResultImpl.newFor( false, true, packet, false, true );
                                state = MSNState.THE_END;
                            }
                            else if( presence.getType() == Presence.Type.available )
                            {
                                retval = SigninResultImpl.newFor( true, true, packet, false, false );
                                state = MSNState.THE_END;
                            }
                        }
                        else if( packet instanceof Message )
                        {
                            Message mesg = (Message)packet;
                            if( mesg.getType() == Message.Type.error )
                            {
                                if( isInvalidPassword( mesg.getBody() ) )
                                {
                                    retval = SigninResultImpl.newFor( false, true, packet, false, true );
                                }
                                else
                                {
                                    retval = SigninResultImpl.newFor( false, true, packet, true, false );
                                }
                                state = MSNState.THE_END;
                            }
                        }
                        else
                            state = MSNState.NO_SERVER_RESPONSE;
                    }
                }
                break;
            case NO_SERVER_RESPONSE:
                retval = SigninResultImpl.newFor( false, false, null, false, false ); // NoServerResponse
                state = MSNState.THE_END;
                break;
            }
        }

        return retval;
    }

    private Packet waitForPresence( MSNState current, PacketCollector packetCollector, long waitMs, int maxLoop )
    {
        Packet packet = null;
        for( int i=0; i<maxLoop; i++ )
        {
            _logger.debug( current.name() + TAB + i + " wait for max of " + waitMs + " ms" );
            packet = packetCollector.nextResult( waitMs );
            if( packet == null )
                continue;
            else
            {
                _logger.debug( current.name() + "\tS->C: " + packet.toXML() );
                if( packet instanceof Presence )
                    break;
                else
                {
                    _logger.debug( current.name() + TAB + i + " ignored!" );
                    continue;
                }
            }
        }
        return packet;
    }

    private Packet waitForPresenceOrErrorMessage( MSNState current, PacketCollector packetCollector, long waitMs, int maxLoop )
    {
        Packet packet = null;
        for( int i=0; i<maxLoop; i++ )
        {
            _logger.debug( current.name() + TAB + i + " wait for max of " + waitMs + " ms" );
            packet = packetCollector.nextResult( waitMs );
            if( packet == null )
                continue;
            else
            {
                _logger.debug( current.name() + "\tS->C: " + packet.toXML() );
                if( packet instanceof Presence )
                    break;
                else if( packet instanceof Message )
                {
                    Message mesg = (Message)packet;
                    if( mesg.getType() == Message.Type.error )
                        break;
                    else 
                    {
                        _logger.debug( current.name() + TAB + i + " ignored normal message" );
                        continue;
                    }
                }
                else
                {
                    _logger.debug( current.name() + TAB + i + " ignored!" );
                    continue;
                }
            }
        }
        return packet;
    }

    enum MSNState {
        PRESENCE_AVAILABLE_SENT,
        FIRST_PRESENCE_UNAVAILABLE_RECEIVED,
        MESSAGE_ERROR_RECEIVED,
        FIRST_PRESENCE_AVAILABLE_RECEIVED,
        NO_SERVER_RESPONSE,
        MESSAGE_HEADLINE_RECEIVED,
        THE_END
    }

    private final boolean isInvalidPassword( String errorSnippet )
    {
        return (errorSnippet != null) && (errorSnippet.indexOf( BAD_PASSWORD_TEXT ) != -1);
    }

    private final String BAD_PASSWORD_TEXT = "The password you registered with is incorrect";
    
    private final static String TAB = "\t";
    private final Logger _logger = Logger.getLogger( Kraken112SigninResponseProcessorForMsn.class );
}
