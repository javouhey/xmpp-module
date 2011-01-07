package com.raverun.im.infrastructure.xmpp.ops;

import java.util.concurrent.Callable;

/**
 * Given a {@code Transport}, find out if the transport slot has been taken
 * by any registrations. Returns true if taken, false otherwise.
 * 
 * @author Gavin Bong
 */
public interface DiscoTransportOperation extends Callable<Boolean>
{
}
