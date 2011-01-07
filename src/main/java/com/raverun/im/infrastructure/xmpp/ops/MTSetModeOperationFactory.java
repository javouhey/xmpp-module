package com.raverun.im.infrastructure.xmpp.ops;

import java.util.Set;

import javax.annotation.Nonnull;

import com.raverun.im.common.Transport;
import com.raverun.im.infrastructure.xmpp.smack.XMPPConnectionIF;
import com.raverun.im.infrastructure.xmpp.smack.PresenceUtilityIF.MyMode;

public interface MTSetModeOperationFactory
{
    SetModeOperation create(
        @Nonnull XMPPConnectionIF xmppConn,
        @Nonnull Set<Transport> setOfTransport, 
        @Nonnull MyMode mode,
        @Nonnull String status );
}
