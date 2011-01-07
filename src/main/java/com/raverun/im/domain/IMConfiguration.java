package com.raverun.im.domain;

import java.util.List;

import javax.annotation.Nonnull;

public interface IMConfiguration
{
    /**
     * List of supported Transports in the XMPP server.
     * <p>
     * Currently used by
     * <ul>
     * <li>{@link IMConnection} 
     * </ul>
     *
     * @return the IMIdentity are useless shells for the {@code Transport} property
     */
    @Nonnull List<IMIdentity> supportedTransports();
}
