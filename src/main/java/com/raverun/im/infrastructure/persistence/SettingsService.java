package com.raverun.im.infrastructure.persistence;

import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.raverun.im.common.Transport;
import com.raverun.im.domain.IMIdentity;
import com.raverun.im.domain.IMUserSetting;
import com.raverun.im.domain.IMUserXmppWrapper;

/**
 * Methods to manipulate the IM settings for a user
 * 
 * @author Gavin Bong
 */
public interface SettingsService
{
    /**
     * A {@code IMUserXmpp} is identified by the combination {@code userid} & {@code userXmpp}.
     * Remove the {@code IMUserSetting} which is identified by {@code identity}
     *
     * @param userid - non empty string
     * @param userXmpp - non empty string
     * @return rows deleted (zero if nothing could be deleted)
     * @throws javax.persistence.PersistenceException if DB layer encountered errors
     * @throws IllegalArgumentException if {@code userid} OR {@code userXmpp} or {@code identity} is null
     */
    int removeSetting( @Nonnull String userid, @Nonnull String userXmpp, @Nonnull IMIdentity identity );

   /**
    * A {@code IMUserXmpp} is identified by the combination {@code userid} & {@code userXmpp}.
    * Persist {@code aSetting} and add it the identified {@code IMUserXmpp}.
    *
    * @param userid - non empty string
    * @param userXmpp - non empty string
    * @throws javax.persistence.PersistenceException if DB layer encountered errors
    * @throws IllegalArgumentException if {@code userid} OR {@code userXmpp} is null
    */
    void addSetting( @Nonnull String userid, @Nonnull String userXmpp, @Nonnull IMUserSetting aSetting );

   /**
    * @throws javax.persistence.PersistenceException if DB layer encountered errors
    * @throws IllegalArgumentException if {@code userid} OR {@code userXmpp} is null
    */
    int updateSetting( @Nonnull String userid, @Nonnull String userXmpp, @Nonnull Transport transport, @Nullable String password, @Nonnull IMUserSetting.UserSettingType saved );

    /**
     * Each
     * {@code IMUserXmpp} can contain a maximum of 5 IM accounts. This method
     * returns all {@code IMUserXmpp} related to the user identified by parameter {@code userid}.
     * <p>
     * Preconditions:
     * <ul>
     * <li>The parameter {@code userid} identifies an existing mCo user. The caller of this method 
     * is responsible for checking that it exists.
     * </ul>
     *
     * @param userid - non empty string
     * @throws javax.persistence.PersistenceException if DB layer encountered errors
     * @throws IllegalArgumentException if {@code userid} is null
     */
    @Nonnull IMUserXmppWrapper getAllIMUserXmppForUser( @Nonnull String userid );

   /**
    * @param userid - non empty string
    * @param userXmpp - non empty string
    * @throws javax.persistence.PersistenceException if DB layer encountered errors
    * @throws IllegalArgumentException if {@code userid} OR {@code userXmpp} is null
    */
    @Nonnull Set<IMUserSetting> getAllSettingsFor( @Nonnull String userid, @Nonnull String userXmpp );

    /**
     * @param userid - non empty string
     * @param userXmpp - non empty string
     * @param savedType - non nullable
     * @throws javax.persistence.PersistenceException if DB layer encountered errors
     * @throws IllegalArgumentException if {@code userid} OR {@code userXmpp} OR {@code savedType} is null
     */
    @Nonnull Set<IMUserSetting> getAllSettingsFor( @Nonnull String userid, @Nonnull String userXmpp, 
        @Nonnull IMUserSetting.UserSettingType savedType );

   /**
    * @throws IllegalArgumentException if {@code userid} OR {@code userXmpp} OR {@code transport} is null
    */
    @Nullable IMUserSetting getOneSettingFor( @Nonnull String userid, @Nonnull String userXmpp, @Nonnull Transport transport );

    @Nullable String getUserFor( @Nonnull String userXmpp );
}
