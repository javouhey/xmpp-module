package com.raverun.im.infrastructure.xmpp.ops;

import java.util.concurrent.Callable;

import javax.annotation.Nullable;

import org.jivesoftware.smack.packet.Packet;

/**
 * Notes:
 * <ul>
 * <li>Invoking {@link AcceptBuddyOperation#call()} might throw {@code IllegalStateException} if we are not connected & authenticated.
 * </ul>
 *
 * @author gavin bong
 */
public interface AcceptBuddyOperation extends Callable<AcceptBuddyOperation.AcceptBuddyResult>
{
//    void init( @Nonnull XMPPConnectionIF xmppConn, @Nonnull Transport transport, @Nonnull String from, @Nonnull String to );

    public interface AcceptBuddyResult
    {
        /**
         * @return nullable. Is null when {@link #didWeReceiveReply()} returns false
         */
        @Nullable Packet getReceivedPacket();

        boolean didWeReceiveReply();
    }
}
