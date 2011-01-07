package com.raverun.im.domain;

import java.util.List;

import javax.annotation.Nonnull;

import org.jivesoftware.smack.PacketListener;

import com.raverun.im.common.Transport;

public interface PacketListenerForPresenceFactory
{
    PacketListener create( @Nonnull String user, @Nonnull String userXmpp, @Nonnull List<Transport> liveTransports );
}
