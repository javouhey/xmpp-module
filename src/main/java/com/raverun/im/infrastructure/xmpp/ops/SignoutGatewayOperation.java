package com.raverun.im.infrastructure.xmpp.ops;

import java.util.concurrent.Callable;

/**
 * This is a fire and forget operation.
 */
public interface SignoutGatewayOperation extends Callable<Void>
{
}
