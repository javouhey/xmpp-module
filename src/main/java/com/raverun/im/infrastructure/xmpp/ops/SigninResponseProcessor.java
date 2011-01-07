package com.raverun.im.infrastructure.xmpp.ops;

import javax.annotation.Nonnull;

import org.jivesoftware.smack.PacketCollector;

/**
 * The the client sends a Presence.available to the Transport, the
 * response varies depending on the Transport.
 *
 * @author gavin bong
 */
public interface SigninResponseProcessor
{
    @Nonnull SigninGatewayOperation.SigninGatewayResult handle( long waitMs, int numOfRetries, PacketCollector packetCollector ) 
        throws Exception;
}
