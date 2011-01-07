package com.raverun.im.domain;

import javax.annotation.Nonnull;

public interface IMConnectionFactory
{
    IMConnection create( @Nonnull String userid, @Nonnull String userXmpp );
}
