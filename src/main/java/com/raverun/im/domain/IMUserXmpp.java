package com.raverun.im.domain;

import java.util.BitSet;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * DDD aggregate
 * 
 * @author Gavin Bong
 */
public interface IMUserXmpp
{
    @Nullable Long sequence();

    void setSequence( @Nonnull Long sequence );

    /**
     * User registered in Core system
     *
     * @return valid user in table {@code mim_user}
     */
    String user();

    /**
     * auto generated uuid as XMPP user
     * 
     * @return valid XMPP user
     */
    String userXMPP();

    /**
     * Returns a snapshot of what transports have been taken
     *
     * @return 
     */
    BitSet viewOfUsableTransports();

    /**
     * read-only view. Always contains 5 elements. Values can contain nulls.
     */
    List<IMUserSetting> transports();

    /**
     * Values are never nulls
     */
    List<IMUserSetting> transportsNonNulls();

    /**
     * <ul>
     * <li>thread safe
     * <li>Precondition (strong): there should be an available slot for this IMUserSetting (constraint: only one per protocol)
     * <li>Postcondition: persist IMUserSetting to table {@code mim_user_xmpp} 
     * </ul>
     *
     * @param setting
     * @return false if not persisted. true otherwise.
     */
    void addUserSetting( @Nonnull IMUserSetting setting );

    void removeUserSetting( );

    /**
     * Every user in module {@code mc-im} potentially may have more than one
     * {@code IMUserXmpp}. This identifies the first {@code IMUserXmpp} that was
     * created along with the creation of the user.
     * 
     * @return true if this cannot be removed
     */
    boolean isPrimordial();

    boolean isNew();

    boolean matchWith( String userXmppId );
}
