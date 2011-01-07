package com.raverun.im.infrastructure.xmpp.ops;

import javax.annotation.Nonnull;

import com.raverun.im.common.Transport;
import com.raverun.im.infrastructure.xmpp.smack.XMPPConnectionIF;

public interface MTAcceptBuddyOperationFactory
{
    AcceptBuddyOperation create( @Nonnull XMPPConnectionIF xmppConn,
        @Nonnull Transport transport, @Nonnull String userXmpp,
        @Nonnull String from, @Nonnull String to );
}
