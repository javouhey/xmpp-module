package com.raverun.im.domain;

import com.raverun.im.common.Transport;

/**
 * Represents the IM user account specified by the {@code mUser}
 * and persisted to the database.
 *
 * @author Gavin Bong
 */
public interface IMIdentity
{
    Transport transport();

    Integer transportDbSequence();

    String imIdRaw();

    /**
     * Returns a version of @link {@link #imIdRaw()} that is safe for openfire (XMPP server).
     * <ul>
     * <li>Any addresses ending in name@yahoo.* (e.g. foo@yahoo.co.uk, bar@yahoo.co.id) will become foo & bar respectively
     * <li>Addresses for MSN (e.g. ali@windowslive.com, baby@live.fr) will remain unchanged
     * <li>Addresses for yahoo like baby@rocketmail.com, me@ymail.com will remain unchanged
     * <li>Addresses for GTALK must include @gmail.com
     * </ul>
     *
     * @return canonical version of {@link #imIdRaw()}
     */
    String imId();
}
