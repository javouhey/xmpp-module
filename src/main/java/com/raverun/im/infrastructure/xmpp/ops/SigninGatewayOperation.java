package com.raverun.im.infrastructure.xmpp.ops;

import java.util.concurrent.Callable;

import org.jivesoftware.smack.packet.Packet;

public interface SigninGatewayOperation extends Callable<SigninGatewayOperation.SigninGatewayResult>
{
    /**
     * Raison d'être: indicate the status of one sign in operation
     * <p>
     * Do not confuse this with {@link com.raverun.im.domain.IMConnection.SignInResult}
     */
    public interface SigninGatewayResult
    {
        String toString();
        boolean isOk();

        boolean isWrongPassword();
        boolean isInvalidLoginId();

        /**
         * This typically happens if the transport is not registered, but we send
         * an available Presence anyway.
         * 
         * @return true if we received a presence error stanza
         */
        boolean isAuth403();

        /**
         * @return nullable. Is null when {@link #didWeReceiveReply()} returns false
         */
        Packet getReceivedPacket();

        boolean didWeReceiveReply();
    }
}
