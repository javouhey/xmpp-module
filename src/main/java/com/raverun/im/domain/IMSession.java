package com.raverun.im.domain;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.raverun.im.infrastructure.xmpp.smack.PresenceUtilityIF;
import com.raverun.im.interfaces.rest.SessionUtils.NewSessionResult;

public interface IMSession
{
    String userId();

    /**
     * Behaviours
     * <ul>
     * <li>TODO If it is started, do not allow it to start again
     * <li>autoLogin is true (by default)
     * </ul>
     * 
     * @throws IMSessionException if errors were encountered
     */
    NewSessionResult start();

    NewSessionResult start( boolean autoLogin );

    /**
     * MUST be idempotent
     */
    void stop();

    /**
     * TODO what happens if someone tries to call this and the session is not started ?
     * 
     * @throws IMSessionException if ....
     * @throws IllegalArgumentException if {@code userSetting} is null
     */
    void addService( @Nonnull IMUserSetting userSetting );

    /**
     * Modifies the persistent storage for existing {@code IMIdentity}s
     *
     * @return number of records updated
     */
    int updateService( @Nonnull List<TupleForAutologinChange> changes );

    /**
     * Sign in multiple IM accounts
     */
    ExistingSessionResult signInService( @Nonnull List<IMIdentity> identities );

    /**
     * Sign out multiple IM accounts
     */
    ExistingSessionResult signOutService( @Nonnull List<IMIdentity> identities );

    /**
     * TODO what happens if someone tries to call this and the session is not started ?
     *
     * @return number of service(s) successfully removed (or zero if nothing was removed)
     * @throws IMSessionException if ....
     * @throws IllegalArgumentException if {@code identity} is null
     */
    int removeService( @Nonnull IMIdentity identity );

   /**
    * @throws IMSessionException if ....
    * @throws IllegalArgumentException if either one of {@code sender} or {@code recipient} is null
    */
    void send( String message, IMIdentity sender, IMIdentity recipient );

    void addBuddy( IMIdentity from, IMIdentity buddyId, String buddyNickname, List<String> buddyGroups );

    void acceptBuddy( IMIdentity from, IMIdentity buddyId );

    void rejectBuddy( IMIdentity from, IMIdentity buddyId );

    void setMode( @Nullable PresenceUtilityIF.MyMode mode, @Nullable String status );

    public interface TupleForAutologinChange
    {
        IMIdentity identity();
        IMUserSetting.UserSettingType newSavedValue();
    }

    public interface ExistingSessionResult
    {
        public boolean ok();
        public String message();
        public int numFailed();
        /**
         * @return a structure which contains a list of ids that failed logins
         */
        public Object loginReport();
    }
}
