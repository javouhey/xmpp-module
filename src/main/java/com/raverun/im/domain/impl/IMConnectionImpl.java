package com.raverun.im.domain.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket;

import com.google.common.base.Predicate;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import com.raverun.im.common.IMConstants;
import com.raverun.im.common.Transport;
import com.raverun.im.domain.IMConfiguration;
import com.raverun.im.domain.IMConnection;
import com.raverun.im.domain.IMIdentity;
import com.raverun.im.domain.IMShared;
import com.raverun.im.domain.IMUserSetting;
import com.raverun.im.domain.IMUserSettingFactory2;
import com.raverun.im.domain.InvalidIMIdentityException;
import com.raverun.im.domain.PacketListenerForIQFactory;
import com.raverun.im.domain.PacketListenerForMessageFactory;
import com.raverun.im.domain.PacketListenerForPresenceFactory;
import com.raverun.im.domain.PacketListenerForRosterFactory;
import com.raverun.im.domain.RosterListenerFactory;
import com.raverun.im.domain.SignInResultConjoiner;
import com.raverun.im.domain.SigninErrorCodes;
import com.raverun.im.infrastructure.persistence.SettingsService;
import com.raverun.im.infrastructure.persistence.TransportMapper;
import com.raverun.im.infrastructure.xmpp.XMPPUtility;
import com.raverun.im.infrastructure.xmpp.ops.AcceptBuddyOperation;
import com.raverun.im.infrastructure.xmpp.ops.AddBuddyOperation;
import com.raverun.im.infrastructure.xmpp.ops.DeregisterGatewayAccountOperation;
import com.raverun.im.infrastructure.xmpp.ops.DiscoTransportOperation;
import com.raverun.im.infrastructure.xmpp.ops.MTAcceptBuddyOperationFactory;
import com.raverun.im.infrastructure.xmpp.ops.MTAddBuddyOperationFactory;
import com.raverun.im.infrastructure.xmpp.ops.MTDeregisterAccountOperationFactory;
import com.raverun.im.infrastructure.xmpp.ops.MTDiscoTransportOperationFactory;
import com.raverun.im.infrastructure.xmpp.ops.MTRegisterAccountOperationFactory;
import com.raverun.im.infrastructure.xmpp.ops.MTRejectBuddyOperationFactory;
import com.raverun.im.infrastructure.xmpp.ops.MTSendChatMessageOperationFactory;
import com.raverun.im.infrastructure.xmpp.ops.MTSetModeOperationFactory;
import com.raverun.im.infrastructure.xmpp.ops.MTSigninGatewayOperationFactory;
import com.raverun.im.infrastructure.xmpp.ops.MTSignoutGatewayOperationFactory;
import com.raverun.im.infrastructure.xmpp.ops.RegisterGatewayAccountOperation;
import com.raverun.im.infrastructure.xmpp.ops.RejectBuddyOperation;
import com.raverun.im.infrastructure.xmpp.ops.SendChatMessageOperation;
import com.raverun.im.infrastructure.xmpp.ops.SetModeOperation;
import com.raverun.im.infrastructure.xmpp.ops.SigninGatewayOperation;
import com.raverun.im.infrastructure.xmpp.ops.SignoutGatewayOperation;
import com.raverun.im.infrastructure.xmpp.ops.SigninGatewayOperation.SigninGatewayResult;
import com.raverun.im.infrastructure.xmpp.smack.PresenceUtilityIF;
import com.raverun.im.infrastructure.xmpp.smack.XMPPConnectionIF;
import com.raverun.im.infrastructure.xmpp.support.XMPPFault;
import com.raverun.im.infrastructure.xmpp.support.XMPPFault.XmppFaultCode;
import com.raverun.shared.Common;
import com.raverun.shared.Configuration;
import com.raverun.shared.Constraint;
import com.raverun.shared.Constraint.EmptyString;
import com.raverun.shared.Constraint.NonNullArgument;

/**
 * Design ideas
 * <ul>
 * <li>Do not store the list of {@code IMUserSetting}. Hit DB if you need it.
 * <li>Add a PacketListener to retrieve values & dump as online alerts.
 * </ul>
 *
 * @author Gavin Bong
 */
@ThreadSafe
public class IMConnectionImpl implements IMConnection
{
    @AssistedInject
    public IMConnectionImpl( Configuration config,
        Provider<XMPPConnectionIF> xmppConnProvider,
        @Named("xmpp.generic.password") String password,
        SettingsService settingsService, IMUserSettingFactory2 userSettingFactory2,
        TransportMapper transportMapper, XMPPUtility xmppUtility,
        MTDiscoTransportOperationFactory discoOperationFactory,
        MTSigninGatewayOperationFactory signinGatewayOperationFactory,
        MTSignoutGatewayOperationFactory signoutGatewayOperationFactory,
        MTAcceptBuddyOperationFactory acceptBuddyOperationFactory,
        MTRegisterAccountOperationFactory registerAccountOperationFactory,
        MTDeregisterAccountOperationFactory deregisterAccountOperationFactory,
        MTSendChatMessageOperationFactory sendChatOperationFactory,
        MTAddBuddyOperationFactory addBuddyOperationFactory,
        MTRejectBuddyOperationFactory rejectBuddyOperationFactory,
        MTSetModeOperationFactory setModeOperationFactory,
        @Named("userexist") Predicate<String> userExistPredicate,
        @Named("ymail") Predicate<String> ymailPredicate,
        @Named("rocketmail") Predicate<String> rocketmailPredicate,
        @Named("yahoo") Predicate<String> yahooPredicate,
        @Named("google") Predicate<String> googlePredicate,
        @Named("qq") Predicate<String> qqPredicate,
        @Named("msn") Predicate<String> msnPredicate,
        SignInResultConjoiner signInResultConjoiner,
        PacketListenerForPresenceFactory presencePacketListenerFactory,
        PacketListenerForMessageFactory messagePacketListenerFactory,
        PacketListenerForIQFactory iqPacketListenerFactory,
        RosterListenerFactory rosterListenerFactory,
        PacketListenerForRosterFactory rosterPacketListenerFactory,
        IMConfiguration imConfig,
        @Assisted String userid, @Assisted String userXmpp )
    {
        _liveTransports = Collections.synchronizedList( new LinkedList<Transport>() );

        if( EmptyString.isFulfilledBy( userid ) ||
            EmptyString.isFulfilledBy( userXmpp ) )
            throw new IllegalArgumentException( "Both userid & userXmpp must be non-nullable" );

        _config                     = config;
        _userid                     = userid;
        _userXmpp                   = userXmpp;
        _password                   = password;
        _imConfig                   = imConfig;
        _xmppUtility                = xmppUtility;
        _transportMapper            = transportMapper;
        _settingsService            = settingsService;
        _xmppConnProvider           = xmppConnProvider;
        _userSettingFactory2        = userSettingFactory2;
        _signInResultConjoiner      = signInResultConjoiner;

        _rosterListenerFactory         = rosterListenerFactory;
        _iqPacketListenerFactory       = iqPacketListenerFactory;
        _rosterPacketListenerFactory   = rosterPacketListenerFactory;
        _messagePacketListenerFactory  = messagePacketListenerFactory;
        _presencePacketListenerFactory = presencePacketListenerFactory;

        _rocketmailPredicate = rocketmailPredicate;
        _userExistPredicate  = userExistPredicate;
        _ymailPredicate      = ymailPredicate;
        _yahooPredicate      = yahooPredicate;
        _googlePredicate     = googlePredicate;
        _qqPredicate         = qqPredicate;
        _msnPredicate        = msnPredicate;

        _discoOperationFactory = discoOperationFactory;
        _setModeOperationFactory = setModeOperationFactory;
        _sendChatOperationFactory = sendChatOperationFactory;
        _addBuddyOperationFactory = addBuddyOperationFactory;
        _rejectBuddyOperationFactory = rejectBuddyOperationFactory;
        _acceptBuddyOperationFactory = acceptBuddyOperationFactory;
        _signinGatewayOperationFactory = signinGatewayOperationFactory;
        _signoutGatewayOperationFactory = signoutGatewayOperationFactory;
        _registerAccountOperationFactory = registerAccountOperationFactory;
        _deregisterAccountOperationFactory = deregisterAccountOperationFactory;
    }

    private static final String ADHOC_START = "remove Adhoc Logins....START";
    private static final String ADHOC_END_OPEN = "remove Adhoc Logins....END (";
    private static final String ADHOC_END_CLOSE = ")";
    private static final String ADHOC_OPEN = "adhoc( ";
    private static final String ADHOC_CLOSE = " ) removed";

    /**
     * TODO document exceptions
     * 
     * @return number of adhoc (temporary) logins that was deleted (possibly zero)
     * @throws javax.persistence.PersistenceException if DB layer encountered errors
     * @throws IllegalArgumentException
     */
    @Override
    public int removeAdhocLogins()
    {
        _logger.info( ADHOC_START );
        Set<IMUserSetting> filteredSettings = _settingsService.getAllSettingsFor( _userid, _userXmpp, 
            IMUserSetting.UserSettingType.TEMPORARY );

        int countDeleted = 0;

        for( IMUserSetting aSetting : filteredSettings )
        {
            if( this.removeTransportFor( aSetting.identity() ) == 1 )
            {
                _logger.debug( ADHOC_OPEN + aSetting.identity().toString() + ADHOC_CLOSE );
                countDeleted++;
            }
        }
        _logger.info( ADHOC_END_OPEN + countDeleted + ADHOC_END_CLOSE );
        return countDeleted;
    }

    /**
     * Doesn't require an active connection to XMPP
     *
     * @param identity - an identity 
     * @throws javax.persistence.PersistenceException if DB layer encountered errors
     * @throws IllegalArgumentException if {@code identity} is null
     */
    public int updateTransportFor( @Nonnull IMIdentity identity )
    {
        return 0;
    }

    /**
     * Returns transports which are active (signed in)
     */
    public Set<Transport> live()
    {
        // @TODO not thread safe
        Set<Transport> retval = new HashSet<Transport>();
        retval.addAll( _liveTransports );
        return retval;
    }

    public void setMode( @Nullable PresenceUtilityIF.MyMode mode, @Nullable String status )
    {
        if( mode == null )
            return;

        String canonicalStatus = Common.EMPTY_STRING;
        if( !Constraint.EmptyString.isFulfilledBy( status ) )
            canonicalStatus = status.trim();

        checkState();

        for( Transport transport : live() )
        {
            _logger.info( _userXmpp + " // setMode to mode " + mode.code() + " will be sent to " + transport.code() );
        }

        synchronized( _xmppConnLock )
        {
            try
            {
                final SetModeOperation op = _setModeOperationFactory.create( _xmppConn, live(), mode, canonicalStatus );
                op.call();
            }
            catch( RuntimeException re )
            {
                throw re;
            }
            catch( Exception e )
            {
                throw new AssertionError( "We did not account for the possibility of: " + e.getMessage() );
            }
        }
    }

    /**
     * @param identity - an identity 
     * @throws javax.persistence.PersistenceException if DB layer encountered errors
     * @throws IllegalStateException if not connected
     * @throws IllegalArgumentException if {@code identity} is null
     * @throws XMPPFault if we failed to execute XMPP operations
     */
    @Override
    public int removeTransportFor( @Nonnull IMIdentity identity )
    {
        NonNullArgument.check( identity, "identity" );
        checkState();

        synchronized( _xmppConnLock )
        {
            final DeregisterGatewayAccountOperation op = _deregisterAccountOperationFactory.create( _xmppConn, identity.transport(), _userXmpp );
            try
            {
                DeregisterGatewayAccountOperation.DeregisterResult xmppDeregistered = op.call();
                int countDeleted = _settingsService.removeSetting( _userid, _userXmpp, identity );
                _logger.debug( "xmpp.deregistered: " + xmppDeregistered + " // countDeleted: " + countDeleted );
                return countDeleted;
            }
            catch( RuntimeException re )
            {
                throw re;
            }
            catch( Exception e )
            {
                throw new AssertionError( "We did not account for the possibility of: " + e.getMessage() );
            }
        }
    }

    /**
     * @param userSetting - the new setting 
     * @throws javax.persistence.PersistenceException if DB layer encountered errors
     * @throws IllegalStateException if not connected to XMPP server
     * @throws IllegalArgumentException if {@code userSetting} is null
     * @throws XMPPFault if we failed to execute XMPP operations
     */
    @Override
    public void addTransportFor( IMUserSetting userSetting )
    {
        NonNullArgument.check( userSetting, "userSetting" );
        checkState();

        synchronized( _xmppConnLock )
        {
            Boolean xmppResult = false;
            try
            {
                final RegisterGatewayAccountOperation op = _registerAccountOperationFactory.create( 
                    _xmppConn, userSetting.identity().transport(), _userXmpp, 
                    userSetting.identity().imId(), userSetting.imPassword() );

                xmppResult = op.call();
                _settingsService.addSetting( _userid, _userXmpp, userSetting );
            }
            catch( RuntimeException re )
            {
                if( xmppResult )
                {
                    // TODO rollback xmpp registration
                }

                throw re;
            }
            catch( Exception e )
            {
                throw new AssertionError( "We did not account for the possibility of: " + e.getMessage() );
            }
        }
    }

    /**
     * TODO do we need to sync on something to protect the db query ?
     * 
     * @throws IllegalArgumentException if transport is null
     * @throws IllegalStateException if not connected
     * @throws XMPPFault if we failed to execute XMPP operations
     * @return true if this {@code transport} is already occupied, false otherwise
     */
    @Override
    public boolean isSlotTakenForTransport( @Nonnull Transport transport )
    {
        NonNullArgument.check( transport, "transport" );
        checkState();

        boolean takenInDb = false;

        Set<IMUserSetting> allSettings = _settingsService.getAllSettingsFor( _userid, _userXmpp );
        if( allSettings.size() == 0 )
            return false;

        for( IMUserSetting aSetting : allSettings )
        {
            Transport t = aSetting.identity().transport();
            if( transport == t )
            {
                takenInDb = true;
                break;
            }
        }

    // check XMPP server so that we can tally with DB
        boolean takenInXMPPServer = false;
        synchronized( _xmppConnLock )
        {
            try
            {
                DiscoTransportOperation op = _discoOperationFactory.create( _xmppConn, transport );
                takenInXMPPServer = op.call();
            }
            catch( Exception e )
            {
                throw translateExceptionsToXmppFaults( e, XmppFaultCode.DISCO_TRANSPORT, "Operation aborted" );
            }
        }

        _logger.debug( idPair() + " -> takenInDb: " + takenInDb + " | takenInXMPPServer: " + takenInXMPPServer );
        return takenInXMPPServer;
    }

    /**
     * TODO do we need to sync on something to protect the db query ?
     *
     * @param identity - the IM identity 
     * @return true if it handles this IM identity
     * @throws IllegalArgumentException if identity is null
     */
    @Override
    public boolean isResponsibleFor( final IMIdentity identity )
    {
        NonNullArgument.check( identity, "identity" );
        boolean retval = false;

        Set<IMUserSetting> allSettings = _settingsService.getAllSettingsFor( _userid, _userXmpp );
        _logger.debug( "[" + _userXmpp + "] settings size = " + allSettings.size() );
        _logger.debug( "" + allSettings );
        if( allSettings.size() > 0 )
        {
            final IMUserSetting searchKey = _userSettingFactory2.create( _transportMapper, 
                identity, BOGUS_PASSWORD, BOGUS_SETTINGTYPE );
            _logger.debug( "searching for: " + searchKey );
            retval = allSettings.contains( searchKey );
        }

        return retval;
    }

    @Override
    public void reload()
    {
        // TODO Auto-generated method stub
        
    }

    public boolean isPrimordial()
    {
        return _primordial;
    }

    public void setPrimordial( boolean primordial )
    {
        _primordial = primordial;
    }

    /**
     * Caveat: does not handle MIM sign-outs
     *
     * @throws XMPPFault
     * @throws IllegalStateException if not connected
     * @throws AssertionError for surprise errors
     */
    @GuardedBy("_xmppConnLock")
    @Override
    public void signOut( IMIdentity identity )
    {
        checkState();

        synchronized( _xmppConnLock )
        {
            if( identity.transport() == Transport.MIM )
            {
                _logger.debug( "ignoring MIM: " + identity.imId() );
                return;
            }

            try
            {
                SignoutGatewayOperation op = _signoutGatewayOperationFactory.create( _xmppConn, identity.transport(), _userXmpp );
                op.call();

//                _signoutGatewayOperation.init( _xmppConn, identity.transport() );
//                _signoutGatewayOperation.call();
            }
            catch( RuntimeException re )
            {
                throw re;
            }
            catch( Exception e )
            {
                throw new AssertionError( "We did not account for the possibility of: " + e.getMessage() );
            }
        }
    }

    /**
     * @throws XMPPFault
     * @throws IllegalStateException if not connected
     */
    @GuardedBy("_xmppConnLock")
    @Override
    public void addBuddy( IMIdentity from, IMIdentity to, String nickName, List<String> groups )
    {
        checkState();

        synchronized( _xmppConnLock )
        {
            final AddBuddyOperation op = _addBuddyOperationFactory.create( _xmppConn, from.transport(), from.imId(), to.imId(), nickName, groups );
            try
            {
                op.call();
            }
            catch( RuntimeException re )
            {
                throw re;
            }
            catch( Exception e )
            {
                throw new AssertionError( "We did not account for the possibility of: " + e.getMessage() );
            }
        }
    }

    @GuardedBy("_xmppConnLock")
    @Override
    public void rejectBuddy( IMIdentity from, IMIdentity to )
    {
        checkState();

        synchronized( _xmppConnLock )
        {
            final RejectBuddyOperation op = _rejectBuddyOperationFactory.create( 
                _xmppConn, from.transport(), from.imId(), to.imId() );

            try
            {
                op.call();
            }
            catch( RuntimeException re )
            {
                throw re;
            }
            catch( Exception e )
            {
                throw new AssertionError( "We did not account for the possibility of: " + e.getMessage() );
            }
        }
    }

    /**
     * @param nickName - @TODO currently ignored
     * @param groups - @TODO currently ignored
     * @throws XMPPFault
     * @throws IllegalStateException if not connected
     */
    @GuardedBy("_xmppConnLock")
    @Override
    public void acceptBuddy( IMIdentity from, IMIdentity to, String nickName, List<String> groups )
    {
        checkState();

        synchronized( _xmppConnLock )
        {
            AcceptBuddyOperation op = _acceptBuddyOperationFactory.create( _xmppConn, from.transport(), _userXmpp, from.imId(), to.imId() );
//            _acceptBuddyOperation.init( _xmppConn, from.transport(), from.imId(), to.imId() );
            try
            {
                op.call();
            }
            catch( RuntimeException re )
            {
                throw re;
            }
            catch( Exception e )
            {
                throw new AssertionError( "We did not account for the possibility of: " + e.getMessage() );
            }
        }
    }

   /**
    *
    * @throws XMPPFault
    * @throws IllegalStateException if not connected
    * @throws AssertionError for surprise errors
    */
    @GuardedBy("_xmppConnLock")
    @Override
    public void send( IMIdentity from, IMIdentity to, String message )
    {
        checkState();

        synchronized( _xmppConnLock )
        {
            final SendChatMessageOperation op = _sendChatOperationFactory.create( _xmppConn, from.transport(), from.imId(), to.imId(), message );
            try
            {
                op.call();
            }
            catch( RuntimeException re )
            {
                throw re;
            }            
            catch( Exception e )
            {
                throw new AssertionError( "We did not account for the possibility of: " + e.getMessage() );
            }
        }
    }

    /**
     * @throws XMPPFault
     * @throws IllegalStateException if not connected
     * @throws AssertionError for surprise errors
     */
    @GuardedBy("_xmppConnLock")
    @Override
    public SignInResult signIn( final IMIdentity identity )
    {
        checkState();

        final List<SignInErrorInfo> listOfFailed = new ArrayList<SignInErrorInfo>(4);
        final List<SignInSuccessInfo> listOfSuccesses2 = new ArrayList<SignInSuccessInfo>(4);

        final boolean auto = true; // NOT significant
        
        synchronized( _xmppConnLock )
        {
            if( identity.transport() == Transport.MIM )
            {
                _logger.debug( "ignoring MIM: " + identity.imId() );
                return new SignInResult() 
                {
                    public List<SignInErrorInfo> failed() { return listOfFailed; }
                    public List<SignInSuccessInfo> success2() { return listOfSuccesses2; }
                };
            }

            try
            {
                SigninGatewayOperation op = _signinGatewayOperationFactory.create( _xmppConn, identity.transport(), _userXmpp );
                final SigninGatewayResult presenceResult = op.call();

//                _signinGatewayOperation.init( _xmppConn, identity.transport() );
//                final SigninGatewayResult presenceResult = _signinGatewayOperation.call();

                if( presenceResult.isOk() )
                {
                    _logger.debug( "\t(#) signin success for: " + identity.imIdRaw() );
                    listOfSuccesses2.add( new SignInSuccessInfo() 
                    {
                        public IMIdentity id() { return identity; }
                        public boolean permanent() { return auto; }
                    });
                }
                else
                {
                    _logger.debug( "\t(x) signin failed for: " + identity.imIdRaw() );

                    // If we fail to receive any response, we SHOULD send a {@code Presence.unavailable}
                    if( presenceResult.isInvalidLoginId() 
                     || presenceResult.isAuth403() 
                     || presenceResult.isWrongPassword()
                     || !presenceResult.didWeReceiveReply() )
                    {
                        SignoutGatewayOperation outOp = _signoutGatewayOperationFactory.create( _xmppConn, identity.transport(), _userXmpp );
                        outOp.call();
                        _logger.debug( "(signin) attempted signing out of > " + identity.transport() );
                    }

                    listOfFailed.add( new SignInErrorInfo() 
                    {
                        public IMIdentity id() { return identity; }

                        public SigninErrorCodes why()
                        {
                            if( presenceResult.isInvalidLoginId() )
                                return SigninErrorCodes.InvalidUser;
                            else if( presenceResult.isWrongPassword() )
                                return SigninErrorCodes.InvalidPassword;
                            else if( !presenceResult.didWeReceiveReply() )
                                return SigninErrorCodes.NoServerResponse;
                            else
                                return SigninErrorCodes.OtherErrors;
                        }

                        public boolean permanent()
                        {
                            return auto;
                        }
                    });
                }
            }
            catch( RuntimeException re )
            {
                throw re;
            }
            catch( Exception e )
            {
                throw new AssertionError( "We did not account for the possibility of: " + e.getMessage() );
            }
        }

        return new SignInResult() 
        {
            public List<SignInErrorInfo> failed() { return listOfFailed; }
            public List<SignInSuccessInfo> success2() { return listOfSuccesses2; }
        };
    }

    private void dump( Set<IMUserSetting> settings, String userXmpp )
    {
        _logger.debug( userXmpp + " .. start" );
        for( IMUserSetting setting : settings )
            _logger.debug( "\t----->" + setting.identity().toString() );

        _logger.debug( userXmpp + " .. end" );
    }

    /**
     * @throws XMPPFault
     * @throws IllegalStateException if not connected
     */
    @GuardedBy("_xmppConnLock")
    @Override
    public SignInResult autoSignIn()
    {
        checkState();

    // 1) collect transports that are registered 
        Set<IMUserSetting> allSettings = _settingsService.getAllSettingsFor( _userid, _userXmpp );
        dump( allSettings, _userXmpp );

    // 2) send Presences to the list
        //final List<IMIdentity> listOfSuccesses   = new ArrayList<IMIdentity>(4);
        final List<SignInErrorInfo> listOfFailed = new ArrayList<SignInErrorInfo>(4);
        final List<SignInSuccessInfo> listOfSuccesses2 = new ArrayList<SignInSuccessInfo>(4);

        for( IMUserSetting aSetting : allSettings )
        {
            final IMIdentity identity = aSetting.identity();
            final boolean permanent = (aSetting.saved() != IMUserSetting.UserSettingType.TEMPORARY ); 

            synchronized( _xmppConnLock )
            {
            // 1) MIM account is assumed to have logged in successfully. So ignore them here.
                if( aSetting.identity().transport() == Transport.MIM )
                {
                    _logger.debug( "ignoring MIM: " + aSetting.identity().imId() );
                    continue;
                }

            // 2) Ignore manual & adhoc logins
                if( aSetting.saved() != IMUserSetting.UserSettingType.AUTOLOGIN )
                {
                    _logger.debug( "ignoring adhoc/manual login: " + aSetting.identity().imId() );
                    if( aSetting.saved() == IMUserSetting.UserSettingType.MANUALLOGIN )
                    {
                        //listOfSuccesses.add( manualId );
                        listOfSuccesses2.add( new SignInSuccessInfo() 
                        {
                            public IMIdentity id() { return identity; }
                            public boolean permanent() { return false; }
                        });
                    }
                    continue;
                }

                try
                {
                    _logger.debug( "autosignin " + aSetting.identity().imIdRaw() );

                    SigninGatewayOperation op = _signinGatewayOperationFactory.create( _xmppConn, aSetting.identity().transport(), _userXmpp );
                    final SigninGatewayResult presenceResult = op.call();

//                    _signinGatewayOperation.init( _xmppConn, aSetting.identity().transport() );
//                    final SigninGatewayResult presenceResult = _signinGatewayOperation.call();

                    if( presenceResult.isOk() )
                    {
                        _logger.debug( "\t(#) autosignin success for: " + aSetting.identity().imIdRaw() );
                        listOfSuccesses2.add( new SignInSuccessInfo() 
                        {
                            public IMIdentity id() { return identity; }
                            public boolean permanent() { return true; }
                        });
                        //listOfSuccesses2.add( aSetting.identity() );
                    }
                    else
                    {
                        _logger.debug( "\t(x) autosignin failed for: " + aSetting.identity().imIdRaw() );

                        if( presenceResult.isInvalidLoginId() 
                         || presenceResult.isAuth403() 
                         || presenceResult.isWrongPassword() 
                         || !presenceResult.didWeReceiveReply() )
                        {
                            SignoutGatewayOperation outOp = _signoutGatewayOperationFactory.create( _xmppConn, aSetting.identity().transport(), _userXmpp );
                            outOp.call();
                            _logger.debug( "(autosignin) attempted signing out of > " + aSetting.identity().transport() );
                        }

//                        final boolean permanent = (aSetting.saved() != IMUserSetting.UserSettingType.TEMPORARY);
                        final IMIdentity theId = aSetting.identity();
                        listOfFailed.add( new SignInErrorInfo() 
                        {
                            public IMIdentity id() { return theId; }

                            public SigninErrorCodes why()
                            {
                                if( presenceResult.isInvalidLoginId() )
                                    return SigninErrorCodes.InvalidUser;
                                else if( presenceResult.isWrongPassword() )
                                    return SigninErrorCodes.InvalidPassword;
                                else if( !presenceResult.didWeReceiveReply() )
                                    return SigninErrorCodes.NoServerResponse;
                                else
                                    return SigninErrorCodes.OtherErrors;
                            }

                            public boolean permanent()
                            {
                                return permanent;
                            }
                        });
                    }
                }
                catch( RuntimeException re )
                {
                    throw re;
                }
                catch( Exception e )
                {
                    throw new AssertionError( "We did not account for the possibility of: " + e.getMessage() );
                }
            }
        }

        return new SignInResult() 
        {
            public List<SignInErrorInfo> failed() { return listOfFailed; }
            public List<SignInSuccessInfo> success2() { return listOfSuccesses2; }
        };
    }

    /**
     * Connects using MIM setting.
     * Then sign in to available transports.
     * 
     * @throws XMPPFault if we failed to connect
     */
    @Override
    public SignInResult connectAndAutoSignin( List<IMUserSetting> seedUserSettings )
    {
        SignInResult mimResult = connectOnly( seedUserSettings );
        if( mimResult == IMShared.NULL_SIGNIN_RESULT )
            return IMShared.NULL_SIGNIN_RESULT;

        return _signInResultConjoiner.conjoin( autoSignIn(), mimResult );
    }

    /** 
     * Only connects to openfire using the MIM setting
     *
     * @param seedUserSettings - initial list of current user settings
     * @return a SignInResult containing the successful MIM account
     * @throws XMPPFault if we failed to connect
     *
     * TODO parameter {@code seedUserSettings} ignored at the moment
     */
    @Override
    public SignInResult connectOnly( List<IMUserSetting> seedUserSettings )
    {
        // 1) you can only connect once
        if( isConnected() )
            return IMShared.NULL_SIGNIN_RESULT;

        // 2) if we're in the middle of completing a connection, you're out of luck
        if( _connecting.compareAndSet( false, true ) )
        {
            boolean encounteredFault = false;
            try
            {
                XMPPConnectionIF aXmppConnection = _xmppConnProvider.get();
                aXmppConnection.connect();

            // we want to process subscription requests manually
                Roster.setDefaultSubscriptionMode( Roster.SubscriptionMode.manual ); 

                if( _liveTransports == null )
                {
                    _logger.warn( "******** WARNING: _liveTransports was null. Why? **********" );
                    _liveTransports = Collections.synchronizedList( new LinkedList<Transport>() );
                }

                _presencePacketListener = _presencePacketListenerFactory.create( _userid, _userXmpp, _liveTransports );
                _messagePacketListener = _messagePacketListenerFactory.create( _userid, _userXmpp );
                _iqPacketListener = _iqPacketListenerFactory.create( _userid, _userXmpp );
                _rosterListener = _rosterListenerFactory.create( _userid, _userXmpp );
                _rosterPacketListener = _rosterPacketListenerFactory.create( _userid, _userXmpp );

                aXmppConnection.addPacketListener( _rosterPacketListener, new PacketTypeFilter( RosterPacket.class ) );
                aXmppConnection.addPacketListener( _presencePacketListener, new PacketTypeFilter( Presence.class ) );
                aXmppConnection.addPacketListener( _messagePacketListener, new PacketTypeFilter( Message.class ) );
                aXmppConnection.addPacketListener( _iqPacketListener, new PacketTypeFilter( IQ.class ) );

                _logger.debug( "subscription mode: " + Roster.getDefaultSubscriptionMode() );

                aXmppConnection.login( _userXmpp, _password, jidEscape( _userid ) ); // TODO @see XEP-0106

                synchronized( _xmppConnLock )
                {
                    _xmppConn = aXmppConnection;
                }

                Roster roster = _xmppConn.getRoster();
                roster.setSubscriptionMode( Roster.SubscriptionMode.manual );
                roster.addRosterListener( _rosterListener );

                _logger.debug( "connectOnly -> MIM : " + _userXmpp );
                return new SignInResult() {
                    public List<SignInErrorInfo> failed() { return Collections.emptyList(); }
                    public List<SignInSuccessInfo> success2() { 
                        List<SignInSuccessInfo> retval = new ArrayList<SignInSuccessInfo>(1);
                        retval.add( new SignInSuccessInfo() {
                            public IMIdentity id() { return newIdentityForMIM( _userXmpp ); }
                            public boolean permanent() { return true; }
                        } );
                        return retval;
                    }
                };
            }
            catch( XMPPException xmppe )
            {
                _logger.error( "Error connecting to XMPP server due to", xmppe );
                encounteredFault = true;
                throw new XMPPFault( "IMConnection for {" + idPair() + "} failed due to", xmppe, XMPPFault.XmppFaultCode.ON_IMCONNECTION_CONNECT );
            }
            finally
            {
                if( encounteredFault )
                    _connecting.set( false );
                else
                    _connecting.compareAndSet( true, false );
            }
        }
        else
            return IMShared.NULL_SIGNIN_RESULT;
    }

    @Override
    public void shutdown()
    {
        if( _shuttingDown.compareAndSet( false, true ))
        {
            try
            {
                checkState();
                _logger.debug( "Shutting down " + _userXmpp );
                synchronized( _xmppConnLock )
                {
                    signOutAllTransports();

                    if( _presencePacketListener != null )
                    {
                        _logger.debug( "removing Presence PacketListener" );
                        _xmppConn.removePacketListener( _presencePacketListener );
                    }

                    if( _iqPacketListener != null )
                    {
                        _logger.debug( "removing IQ PacketListener" );
                        _xmppConn.removePacketListener( _iqPacketListener );
                    }

                    if( _messagePacketListener != null )
                    {
                        _logger.debug( "removing Message PacketListener" );
                        _xmppConn.removePacketListener( _messagePacketListener );
                    }

                    if( _rosterListener != null )
                    {
                        _logger.debug( "removing RosterListener" );
                        _xmppConn.getRoster().removeRosterListener( _rosterListener );
                    }

                    if( _rosterPacketListener != null )
                    {
                        _logger.debug( "removing Roster PacketListener" );
                        _xmppConn.removePacketListener( _rosterPacketListener );
                    }

                    _xmppConn.disconnect();
                    _xmppConn = null;
                }
            }
            catch( IllegalStateException ise )
            {
                _logger.error( "Caught while shutting down", ise );
            }
            catch( Exception e )
            {
                _logger.error( "Caught while shutting down", e );
            }
            finally
            {
                _shuttingDown.set( false );
            }
        }
    }

    /**
     * Needs external synchronisation
     * <p>
     * @see http://kraken.blathersource.org/node/6
     */
    private void signOutAllTransports()
    {
        List<IMIdentity> supportedTransports = _imConfig.supportedTransports();
        for( IMIdentity identity : supportedTransports )
        {
            try
            {
                signOut( identity );
            }
            catch( Exception ignored ) { _logger.error(  "during shutdown", ignored ); }
        }
    }

    /**
     * @return true iff connected to XMPP server
     */
    public boolean isConnected()
    {
        synchronized( _xmppConnLock )
        {
            return( _xmppConn != null && _xmppConn.isConnected() );
        }
    }

    public String getUser()
    {
        return _userid;
    }

    public String getUserXmpp()
    {
        return _userXmpp;
    }

    // TODO
    private String jidEscape( String in )
    {
        return in;
    }

    private void checkState()
    {
        synchronized( _xmppConnLock )
        {
            if( _xmppConn == null || _xmppConn.getConnectionID() == null || !isConnected() )
                throw new IllegalStateException( "not connected" );
        }
    }

    private XMPPFault translateExceptionsToXmppFaults( Exception e, XMPPFault.XmppFaultCode faultCode, 
        String errorMessage )
    {
        if( e instanceof XMPPFault )
            return (XMPPFault)e;

        return new XMPPFault( errorMessage, e, faultCode );
    }

    private String idPair()
    {
        return (_userid + _userXmpp).intern();
    }

    /**
     * @throws InvalidIMIdentityException if the loginId is invalid
     * @throws IllegalStateException if a programming error occurs
     */
    private final IMIdentity newIdentityForMIM( String loginId )
    {
        IMIdentityImpl.FromClientBuilder builder =
            new IMIdentityImpl.FromClientBuilder( _transportMapper, 
                _ymailPredicate, _rocketmailPredicate,
                _yahooPredicate, _googlePredicate, _qqPredicate, 
                _msnPredicate );
        _logger.debug( "(IMConnectionImpl#newIdentityForMIM ) replace " + loginId + " with " + _userid );
        //IMIdentity identity = builder.loginId( loginId ).imType( IMConstants.ClientLiteralsForTransport.MIM ).build();
        IMIdentity identity = builder.loginId( _userid ).imType( IMConstants.ClientLiteralsForTransport.MIM ).build();
        return identity;
    }

    public volatile boolean _primordial;

    private volatile XMPPConnectionIF _xmppConn;
    private volatile PacketListener _presencePacketListener;
    private volatile PacketListener _messagePacketListener;
    private volatile PacketListener _iqPacketListener;
    private volatile PacketListener _rosterPacketListener;
    private volatile RosterListener _rosterListener;

    private List<Transport> _liveTransports;

    private Object _xmppConnLock = new Object();

    private final Configuration _config;
    private final XMPPUtility _xmppUtility;
    private final SettingsService _settingsService;
    private final TransportMapper _transportMapper;
    private final IMUserSettingFactory2 _userSettingFactory2;
    private final Provider<XMPPConnectionIF> _xmppConnProvider;

    private final IMConfiguration _imConfig;
    private final RosterListenerFactory _rosterListenerFactory;
    private final PacketListenerForPresenceFactory _presencePacketListenerFactory;
    private final PacketListenerForMessageFactory _messagePacketListenerFactory;
    private final PacketListenerForIQFactory _iqPacketListenerFactory;
    private final PacketListenerForRosterFactory _rosterPacketListenerFactory;
    private final SignInResultConjoiner _signInResultConjoiner;

    private final Predicate<String> _ymailPredicate;
    private final Predicate<String> _rocketmailPredicate;
    private final Predicate<String> _yahooPredicate;
    private final Predicate<String> _googlePredicate;
    private final Predicate<String> _qqPredicate;
    private final Predicate<String> _msnPredicate;
    private final Predicate<String> _userExistPredicate;

    ///////// new since 20090919 ///////////
    private final MTDiscoTransportOperationFactory _discoOperationFactory;
    private final MTSigninGatewayOperationFactory _signinGatewayOperationFactory;
    private final MTSignoutGatewayOperationFactory _signoutGatewayOperationFactory;
    private final MTAcceptBuddyOperationFactory _acceptBuddyOperationFactory;
    private final MTRegisterAccountOperationFactory _registerAccountOperationFactory;
    private final MTDeregisterAccountOperationFactory _deregisterAccountOperationFactory;
    private final MTSendChatMessageOperationFactory _sendChatOperationFactory;
    private final MTAddBuddyOperationFactory _addBuddyOperationFactory;
    private final MTRejectBuddyOperationFactory _rejectBuddyOperationFactory;
    private final MTSetModeOperationFactory _setModeOperationFactory;

    private final AtomicBoolean _connecting = new AtomicBoolean(); // false by default
    private final AtomicBoolean _shuttingDown = new AtomicBoolean(); // false by default

    private final String _password;
    private final String _userid;
    private final String _userXmpp;

    private static final Logger _logger = Logger.getLogger( IMConnectionImpl.class );

    private static final String BOGUS_PASSWORD = "12345";
    private static final IMUserSetting.UserSettingType BOGUS_SETTINGTYPE = IMUserSetting.UserSettingType.TEMPORARY;
}
