package com.raverun.im.domain;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface IMUserXmppWrapper
{
    @Nullable IMUserXmpp primordial();

    @Nonnull List<IMUserXmpp> others();
}
