package com.raverun.im.domain;

import javax.annotation.Nonnull;

import org.jivesoftware.smack.RosterListener;

public interface RosterListenerFactory
{
    RosterListener create( @Nonnull String user, @Nonnull String userXmpp );
}
