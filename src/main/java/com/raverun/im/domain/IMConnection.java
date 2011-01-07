package com.raverun.im.domain;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.raverun.im.common.Transport;
import com.raverun.im.infrastructure.xmpp.ops.SigninGatewayOperation.SigninGatewayResult;
import com.raverun.im.infrastructure.xmpp.smack.PresenceUtilityIF;
import com.raverun.im.infrastructure.xmpp.support.XMPPFault;

/**
 * Abstraction that handles one connection to the XMPP server. This connection can support
 * one {@link Transport} of each type (e.g. 1 FB, 1 Gtalk, 1 Yahoo etc..)
 *
 * @author Gavin Bong
 */
public interface IMConnection
{
    // TODO idea is to add a setting & ask the IMConnection to re-query the database. Good idea?
    void reload();

    /**
     * Returns transports which are active (signed in)
     */
    @Nonnull Set<Transport> live();

    void setMode( @Nullable PresenceUtilityIF.MyMode mode, @Nullable String status );

    /**
     * @param userSetting - the new setting 
     * @throws javax.persistence.PersistenceException if DB layer encountered errors
     * @throws IllegalStateException if not connected
     * @throws IllegalArgumentException if {@code userSetting} is null
     * @throws XMPPFault if we failed to execute XMPP operations
     */
    void addTransportFor( @Nonnull IMUserSetting userSetting );

    /**
     * @param identity - an identity 
     * @throws javax.persistence.PersistenceException if DB layer encountered errors
     * @throws IllegalStateException if not connected
     * @throws IllegalArgumentException if {@code identity} is null
     * @throws XMPPFault if we failed to execute XMPP operations
     */
    int removeTransportFor( @Nonnull IMIdentity identity );

    /**
     * Doesn't require an active connection to XMPP
     *
     * @param identity - an identity 
     * @throws javax.persistence.PersistenceException if DB layer encountered errors
     * @throws IllegalArgumentException if {@code identity} is null
     */
    int updateTransportFor( @Nonnull IMIdentity identity );

    /**
     * TODO document exceptions
     * 
     * @return number of adhoc (temporary) logins that was deleted (possibly zero)
     */
    int removeAdhocLogins();

    /**
     * 
     * @param identity - the IM identity 
     * @return true if it handles this IM identity
     * @throws IllegalArgumentException if identity is null
     */
    boolean isResponsibleFor( @Nonnull IMIdentity identity );

    /**
     * @throws IllegalArgumentException if transport is null
     * @throws IllegalStateException if not connected
     * @throws XMPPFault if we failed to execute XMPP operations
     * @return true if this {@code transport} is already occupied, false otherwise
     */
    boolean isSlotTakenForTransport( @Nonnull Transport transport );

    /** 
     * Only connects to openfire using the MIM setting
     *
     * @param seedUserSettings - initial list of current user settings
     * @throws XMPPFault if we failed to connect
     */
    SignInResult connectOnly( List<IMUserSetting> seedUserSettings );

    /**
     * Composes methods in the following order
     * <ol>
     * <li>{@link this#connectOnly(List)}
     * <li>{@link this#autoSignIn()}
     * </ol>
     * 
     * @throws XMPPFault if we failed to connect
     */
    SignInResult connectAndAutoSignin( List<IMUserSetting> seedUserSettings );

    /**
     * Used during startup to auto sign in specified accounts.
     *
     * @throws XMPPFault
     * @throws IllegalStateException if not connected
     */
    SignInResult autoSignIn();

    /**
     * @throws XMPPFault
     * @throws IllegalStateException if not connected
     * @throws AssertionError for surprise errors
     */
    SignInResult signIn( IMIdentity identity );

    /**
     * Caveat: does not handle MIM sign-outs
     *
     * @throws XMPPFault
     * @throws IllegalStateException if not connected
     * @throws AssertionError for surprise errors
     */
    void signOut( IMIdentity identity );

    /**
     *
     * @throws XMPPFault
     * @throws IllegalStateException if not connected
     * @throws AssertionError for surprise errors
     */
    void send( IMIdentity from, IMIdentity to, String message );

   /**
    * @throws XMPPFault
    * @throws IllegalStateException if not connected
    */
    void addBuddy( IMIdentity from, IMIdentity to, String nickName, List<String> groups );

    /**
     * @throws XMPPFault
     * @throws IllegalStateException if not connected
     */
    void acceptBuddy( IMIdentity from, IMIdentity to, String nickName, List<String> groups );

    /**
     * @throws XMPPFault
     * @throws IllegalStateException if not connected
     */
    void rejectBuddy( IMIdentity from, IMIdentity to );

    /**
     * @return true iff connected to XMPP server
     */
    boolean isConnected();

    void shutdown();

    String getUser();

    String getUserXmpp();

    boolean isPrimordial();

    void setPrimordial( boolean primordial );

    /**
     * Raison d'être: To return results back to the REST resource
     * <p>
     * Do not confuse this with {@link SigninGatewayResult}
     */
    public interface SignInResult
    {
        List<SignInErrorInfo> failed();
        List<SignInSuccessInfo> success2();
    }

    public interface SignInSuccessInfo
    {
        IMIdentity id();
        boolean permanent();
    }

    public interface SignInErrorInfo
    {
        IMIdentity id();
        SigninErrorCodes why();
        boolean permanent();
    }
}
