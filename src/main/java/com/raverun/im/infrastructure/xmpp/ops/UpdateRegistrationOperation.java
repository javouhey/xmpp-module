package com.raverun.im.infrastructure.xmpp.ops;

import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import com.raverun.im.common.Transport;
import com.raverun.im.infrastructure.xmpp.smack.XMPPConnectionIF;

/**
 * @TODO
 */
public interface UpdateRegistrationOperation extends Callable<Integer>
{
    void init( @Nonnull XMPPConnectionIF xmppConn, @Nonnull Transport transport, String userid, String password );
}
