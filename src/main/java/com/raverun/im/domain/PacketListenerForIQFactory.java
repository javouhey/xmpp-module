package com.raverun.im.domain;

import javax.annotation.Nonnull;

import org.jivesoftware.smack.PacketListener;

public interface PacketListenerForIQFactory
{
    PacketListener create( @Nonnull String user, @Nonnull String userXmpp );
}
