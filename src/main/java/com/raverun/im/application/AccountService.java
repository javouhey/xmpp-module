package com.raverun.im.application;

import java.util.List;

import javax.annotation.Nonnull;

import com.raverun.im.domain.IMIdentity;
import com.raverun.im.domain.IMUserSetting;

public interface AccountService
{
    /**
     * Creates an account for user in the {@code im} module.
     * <p>
     * Precondition: {@code userid} does not exists yet
     *
     * @param userid non-nullable
     * @throws IllegalArgumentException if {@code userid} is null
     * @throws AccountOperationException
     */
    void createTotallyNewUser( @Nonnull String userid );

    /**
     * @param userid non-nullable
     * @param isPrimordial - indicates whether the {@code IMUserXmpp} is primordial
     * @throws IllegalArgumentException if {@code userid} is null
     * @throws AccountOperationException if we're unable to create a new userXmpp
     * @return the userXmpp
     */
    String createNewXmppAccountForExisting( @Nonnull String userid, boolean isPrimordial );

    void addService( @Nonnull String userid, @Nonnull IMUserSetting userSetting );

    void removeService( @Nonnull String userid, @Nonnull IMIdentity imIdentity );

    void removeAllAccounts( List<String> listOfUserXmpps );
    
    /**
     * key to store the original userid in the Map
     */
    public final static String KEY_XMPP_NAME = "name";
}
