package com.raverun.im.infrastructure.xmpp.ops;

import java.util.concurrent.Callable;

import org.jivesoftware.smack.packet.Packet;

public interface DeregisterGatewayAccountOperation extends Callable<DeregisterGatewayAccountOperation.DeregisterResult>
{
    public interface DeregisterResult
    {
        String toString();

        /**
         * @return false if we received a true in either one of {@link #isAuth407()} 
         * or {@link #didWeReceiveReply()}
         */
        boolean isOk();
        boolean didWeReceiveReply();
        Packet getReceivedPacket();

        /**
         * @return true if we are trying to unregister an empty transport, false otherwise
         */
        boolean isAuth407();
    }
}
