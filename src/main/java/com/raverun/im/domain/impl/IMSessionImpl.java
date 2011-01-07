package com.raverun.im.domain.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import org.apache.log4j.Logger;

import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import com.raverun.im.application.AccountService;
import com.raverun.im.common.Transport;
import com.raverun.im.domain.IMConnection;
import com.raverun.im.domain.IMConnectionFactory;
import com.raverun.im.domain.IMIdentity;
import com.raverun.im.domain.IMSession;
import com.raverun.im.domain.IMSessionException;
import com.raverun.im.domain.IMUserSetting;
import com.raverun.im.domain.IMUserXmpp;
import com.raverun.im.domain.IMUserXmppWrapper;
import com.raverun.im.domain.SignInResultConjoiner;
import com.raverun.im.domain.SigninErrorCodes;
import com.raverun.im.domain.IMConnection.SignInErrorInfo;
import com.raverun.im.domain.IMConnection.SignInResult;
import com.raverun.im.domain.IMConnection.SignInSuccessInfo;
import com.raverun.im.domain.IMSessionException.SessionErrorCode;
import com.raverun.im.infrastructure.persistence.SettingsService;
import com.raverun.im.infrastructure.persistence.TransportMapper;
import com.raverun.im.infrastructure.xmpp.smack.PresenceUtilityIF;
import com.raverun.im.infrastructure.xmpp.support.XMPPFault;
import com.raverun.im.infrastructure.xmpp.support.XMPPFault.XmppFaultCode;
import com.raverun.im.interfaces.rest.SessionUtils.NewSessionResult;
import com.raverun.im.interfaces.rest.impl.ExistingSessionResultImpl;
import com.raverun.im.interfaces.rest.impl.NewSessionResultImpl;
import com.raverun.shared.Common;
import com.raverun.shared.Constraint;
import com.raverun.shared.Constraint.NonNullArgument;

@ThreadSafe
public class IMSessionImpl implements IMSession
{
    @AssistedInject
    public IMSessionImpl( TransportMapper transportMapper,
        SettingsService settingsService, IMConnectionFactory imConnFactory,
        AccountService accountService, SignInResultConjoiner conjoiner,
        Provider<ExecutorService> execProvider,
        @Named("default.roster.group") String defaultGroupName,
        @Assisted String userId )
    {
        _userId = userId;
        _conjoiner = conjoiner;
        _execProvider = execProvider;
        _imConnFactory = imConnFactory;
        _accountService = accountService;
        _settingsService = settingsService;
        _defaultRosterGroup = defaultGroupName;
        _connectionMap = new LinkedHashMap<String,IMConnection>( 6 ); // max of 3, then double that
    }

    @Override
    public String userId()
    {
        return _userId;
    }

    public void setMode( @Nullable PresenceUtilityIF.MyMode mode, @Nullable String status )
    {
        if( mode == null )
            return; // noop if {@code mode} not supplied

        synchronized( _connectionMap )
        {
            for( IMConnection connection : _connectionMap.values() )
            {
                try
                {
                    if( connection.isConnected() )
                        connection.setMode( mode, status );
                }
                catch( XMPPFault ignored )
                {
                    _logger.error( "Encountered error during setMode for '" + connection.getUserXmpp() + "'", ignored );
                }
            }//for
        }
    }

    /**
     * Sign out multiple IM accounts
     *
     * @throws IllegalArgumentException if {@code identities} is null
     */
    public ExistingSessionResult signOutService( @Nonnull List<IMIdentity> identities )
    {
        Constraint.NonNullArgument.check( identities, "identities" );

        int xmppFaults = 0; // consider these as failure to sign out
        for( IMIdentity anId : identities )
        {
            IMConnection responsibleParty = findConnectionResponsibleFor( anId );
            if( responsibleParty == null )
            {
                _logger.debug( "IMIdentity " + anId.toString() + " is STRANGELY not handled by any connections" );
                continue;
            }

            try
            {
                responsibleParty.signOut( anId );
                removeAdhocDuringSignout( anId, responsibleParty );
            }
            catch( XMPPFault xf )
            {
                xmppFaults++;
            }
            catch( Exception e )
            {
                // TODO swallowed ?
                _logger.error( "signout failed ", e );
            }
        }

        final int numIds = identities.size();
        final int finalXmppFaults = xmppFaults;
        final SignoutMessage message = formMessage( numIds, finalXmppFaults );

        return new ExistingSessionResult() {
            public Object loginReport() { return null; } // unused
            public String message() { return message.message(); }
            public int numFailed() { return -1; }
            public boolean ok() { return message.ok(); }
        }; 
    }

    private final int removeAdhocDuringSignout( IMIdentity identity, IMConnection connection )
    {
        IMUserSetting aSetting = _settingsService.getOneSettingFor( connection.getUser(), connection.getUserXmpp(), identity.transport() );
        if( aSetting == null )
            return 0;

        if( aSetting.saved() != IMUserSetting.UserSettingType.TEMPORARY )
            return 0;

        _logger.debug( "[removeAdhocDuringSignout] removing adhoc " + identity.toString() );
        return connection.removeTransportFor( identity );
    }

    private final SignoutMessage formMessage( final int numIds, final int xmppFaults )
    {
        if( numIds == 0 )
            return new SignoutMessage() 
                { public String message() { return "Nothing to sign out"; } public boolean ok() { return true; } };

        if( xmppFaults <= 0 )
        {
            return new SignoutMessage() 
                { public String message() { return "Successfully signed out " + numIds + " ids"; } public boolean ok() { return true; } };
        }
        else
        {
            return new SignoutMessage() 
                { public String message() { return xmppFaults + " ids encountered xmpp faults during sign outs"; } public boolean ok() { return false; } };
        }
    }

    // masquerading as a tuple Pair
    interface SignoutMessage
    {
        boolean ok();
        String message();
    }

    private final SignInResult notFoundUsersResult( final IMIdentity anId )
    {
        final List<SignInErrorInfo> listOfFailed = new ArrayList<SignInErrorInfo>(1);
        final List<SignInSuccessInfo> listOfSuccesses2 = new ArrayList<SignInSuccessInfo>(1);

        listOfFailed.add( new SignInErrorInfo() 
        {
            public IMIdentity id() { return anId; }
            public boolean permanent() { return false; }
            public SigninErrorCodes why() { return SigninErrorCodes.NoSuchUser; }
        });

        return new SignInResult() 
        {
            public List<SignInErrorInfo> failed() { return listOfFailed; }
            public List<SignInSuccessInfo> success2() { return listOfSuccesses2; }
        };
    }

    /**
     * @TODO Modifies the persistent storage for existing {@code IMIdentity}s
     *
     * @return number of records updated
     */
    public int updateService( @Nonnull List<TupleForAutologinChange> changes )
    {
        Constraint.NonNullArgument.check( changes, "changes" );
        int numberOfChanges = 0;

        for( TupleForAutologinChange aChange : changes )
        {
            IMIdentity anId = aChange.identity();
            if( anId == null )
                continue;

            IMConnection responsibleParty = findConnectionResponsibleFor( anId );
            if( responsibleParty == null )
            {
                _logger.debug( "[updateService] " +  anId.toString() + " is STRANGELY not handled by any connections. Skipping" );
                continue;
            }

            IMUserSetting aSetting = _settingsService.getOneSettingFor( responsibleParty.getUser(), 
                responsibleParty.getUserXmpp(), anId.transport() );

            if( aSetting == null )
                continue;

            if( ! aSetting.identity().equals( anId ) )
            {
                _logger.debug( "identity returned by SettingsService (" + aSetting.identity().imIdRaw() + ") is different from supplied " + anId.imIdRaw() + " | skipped" );
                continue;
            }

            /**
             * As of 20090917, we don't allow user to change their passwords.
             * By setting this to null, the settingService will ignore it.
             */
            final String newPassword = null;

            int oneResult = _settingsService.updateSetting( responsibleParty.getUser(), 
                responsibleParty.getUserXmpp(), anId.transport(), newPassword,
                aChange.newSavedValue() );

            if( oneResult == 1 )
                numberOfChanges++;
            
        }//for

        return numberOfChanges;
    }

    @Override
    public void rejectBuddy( IMIdentity from, IMIdentity buddyId )
    {
        Constraint.NonNullArgument.check( from, "from" );
        Constraint.NonNullArgument.check( buddyId, "buddyId" );

        IMConnection responsibleConnection = null;
        IMIdentity finalBuddyId = buddyId;

        if( from.transport() == Transport.MIM )
        {
            responsibleConnection = findPrimordialConnection();
            _logger.debug( "[rejectBuddy] search for MIM primordial for " + buddyId.imId() );
            IMUserXmppWrapper userWrapper = _settingsService.getAllIMUserXmppForUser( buddyId.imId() );
            final IMUserXmpp theUserXmpp = userWrapper.primordial();
            finalBuddyId = new IMIdentity() {
                public String imId() { return theUserXmpp.userXMPP(); }
                public String imIdRaw() { return null; } // ignored
                public Transport transport() { return null; } // ignored
                public Integer transportDbSequence() { return null; } // ignored
            };
        }
        else
        {
            responsibleConnection = findConnectionResponsibleFor( from );
            if( responsibleConnection == null )
            {
                _logger.debug( "[rejectBuddy] " +  from.toString() + " is STRANGELY not handled by any connections. Skipping" );
                return;
            }
        }//else

        try
        {
            responsibleConnection.rejectBuddy( from, finalBuddyId );
        }
        catch( XMPPFault e )
        {
            throw new IMSessionException( Common.EMPTY_STRING, e, SessionErrorCode.FAILED_CHAT ); // @TODO 
        }
    }

    @Override
    public void acceptBuddy( IMIdentity from, IMIdentity buddyId )
    {
        Constraint.NonNullArgument.check( from, "from" );
        Constraint.NonNullArgument.check( buddyId, "buddyId" );

        IMConnection responsibleConnection = null;
        IMIdentity finalBuddyId = buddyId;

        if( from.transport() == Transport.MIM )
        {
            responsibleConnection = findPrimordialConnection();
            _logger.debug( "[acceptBuddy] search for MIM primordial for " + buddyId.imId() );
            IMUserXmppWrapper userWrapper = _settingsService.getAllIMUserXmppForUser( buddyId.imId() );
            final IMUserXmpp theUserXmpp = userWrapper.primordial();
            finalBuddyId = new IMIdentity() {
                public String imId() { return theUserXmpp.userXMPP(); }
                public String imIdRaw() { return null; } // ignored
                public Transport transport() { return null; } // ignored
                public Integer transportDbSequence() { return null; } // ignored
            };
        }
        else
        {
            responsibleConnection = findConnectionResponsibleFor( from );
            if( responsibleConnection == null )
            {
                _logger.debug( "[acceptBuddy] " +  from.toString() + " is STRANGELY not handled by any connections. Skipping" );
                return;
            }
        }//else

        try
        {
            responsibleConnection.acceptBuddy( from, finalBuddyId, Common.EMPTY_STRING, null );
        }
        catch( XMPPFault e )
        {
            throw new IMSessionException( Common.EMPTY_STRING, e, SessionErrorCode.FAILED_CHAT ); // @TODO 
        }

    }

    @Override
    public void addBuddy( IMIdentity from, IMIdentity buddyId, String buddyNickname, List<String> buddyGroups )
    {
        Constraint.NonNullArgument.check( from, "from" );
        Constraint.NonNullArgument.check( buddyId, "buddyId" );
  
        String internalBuddyNickname = buddyId.imId();
        List<String> internalBuddyGroups = new ArrayList<String>(2);

        if( !Constraint.EmptyString.isFulfilledBy( buddyNickname ) )
            internalBuddyNickname = buddyNickname.trim();

        if( buddyGroups == null || buddyGroups.size() == 0 )
            internalBuddyGroups.add( _defaultRosterGroup );
        else
            internalBuddyGroups.addAll( buddyGroups );

        IMConnection responsibleConnection = null;
        IMIdentity finalBuddyId = buddyId;

        if( from.transport() == Transport.MIM )
        {
            responsibleConnection = findPrimordialConnection();
            _logger.debug( "[addBuddy] search for MIM primordial for " + buddyId.imId() );
            IMUserXmppWrapper userWrapper = _settingsService.getAllIMUserXmppForUser( buddyId.imId() );
            final IMUserXmpp theUserXmpp = userWrapper.primordial();
            finalBuddyId = new IMIdentity() {
                public String imId() { return theUserXmpp.userXMPP(); }
                public String imIdRaw() { return null; } // ignored
                public Transport transport() { return null; } // ignored
                public Integer transportDbSequence() { return null; } // ignored
            };
        }
        else
        {
            responsibleConnection = findConnectionResponsibleFor( from );
            if( responsibleConnection == null )
            {
                _logger.debug( "[addBuddy] " +  from.toString() + " is STRANGELY not handled by any connections. Skipping" );
                return;
            }
        }//else

        try
        {
            responsibleConnection.addBuddy( from, finalBuddyId, internalBuddyNickname, internalBuddyGroups );
        }
        catch( XMPPFault e )
        {
            throw new IMSessionException( Common.EMPTY_STRING, e, SessionErrorCode.FAILED_CHAT ); // @TODO
        }
    }//addBuddy

    /**
     * @throws IMSessionException if ....
     * @throws IllegalArgumentException if either one of {@code sender} or {@code recipient} is null
     */
     public void send( String message, IMIdentity sender, IMIdentity recipient )
     {
         Constraint.NonNullArgument.check( sender, "sender" );
         Constraint.NonNullArgument.check( recipient, "recipient" );
         String canonicalMessage = Common.EMPTY_STRING;

         if( Constraint.EmptyString.isFulfilledBy( message ) )
             return;

         canonicalMessage = message.trim();

         IMConnection responsibleConnection = null;
         IMIdentity finalRecipient = recipient;

         if( sender.transport() == Transport.MIM )
         {
             responsibleConnection = findPrimordialConnection();
             _logger.debug( "search for MIM primordial for " + recipient.imId() );
             IMUserXmppWrapper userWrapper = _settingsService.getAllIMUserXmppForUser( recipient.imId() );
             final IMUserXmpp theUserXmpp = userWrapper.primordial();
             finalRecipient = new IMIdentity() {
                 public String imId() { return theUserXmpp.userXMPP(); }
                 public String imIdRaw() { return null; } // ignored
                 public Transport transport() { return null; } // ignored
                 public Integer transportDbSequence() { return null; } // ignored
             };
         }
         else
         {
             responsibleConnection = findConnectionResponsibleFor( sender );
             if( responsibleConnection == null )
             {
                 _logger.debug( "[send] " +  sender.toString() + " is STRANGELY not handled by any connections. Skipping" );
                 return;
             }
         }

         try
         {
             responsibleConnection.send( sender, finalRecipient, canonicalMessage );
         }
         catch( XMPPFault e )
         {
             throw new IMSessionException( Common.EMPTY_STRING, e, SessionErrorCode.FAILED_CHAT );
         }
     }

    /**
     * Sign in multiple IM accounts
     * <p>
     * <ul>
     * <li>The accounts in {@code identities} are saved inside DB + XMPP gateway
     * <li>TODO: need to parallelise this method because the XmppGatewayResource can submit multiple identities.
     * at a time.
     * </ul>
     *
     * @param identities
     * @throws IllegalArgumentException if {@code identities} is null
     */
    public ExistingSessionResult signInService( @Nonnull List<IMIdentity> identities )
    {
        Constraint.NonNullArgument.check( identities, "identities" );
        try
        {
            SignInResult accumulator = seed();
            for( IMIdentity anId : identities )
            {
                IMConnection responsibleParty = findConnectionResponsibleFor( anId );
                if( responsibleParty == null )
                {
                    _logger.debug( "IMIdentity " + anId.toString() + " is STRANGELY not handled by any connections" );
                    accumulator = _conjoiner.conjoin( accumulator, notFoundUsersResult( anId ) );
                    continue;
                }

                accumulator = _conjoiner.conjoin( accumulator, responsibleParty.signIn( anId ) );
            }

            ExistingSessionResultImpl.Builder builder = new ExistingSessionResultImpl.Builder( true );
            for( SignInErrorInfo errorInfo : accumulator.failed() )
                builder = builder.addFailure( errorInfo.id(), errorInfo.permanent(), errorInfo.why() );

            return builder.build( MediaType.APPLICATION_JSON_TYPE );
        }
        catch( Exception e )
        {
            throw new IMSessionException( Common.EMPTY_STRING, e, SessionErrorCode.SIGNIN_SERVICE );
        }
    }

    @GuardedBy("_connectionMap")
    private final IMConnection findPrimordialConnection()
    {
        IMConnection responsibleParty = null;

        synchronized( _connectionMap )
        {
            for( IMConnection aConnection : _connectionMap.values() )
            {
                if( aConnection.isPrimordial() )
                {
                    responsibleParty = aConnection;
                    _logger.debug( "found primordial connection -> " + aConnection.getUserXmpp() );
                    break;
                }
            }
        }
        return responsibleParty;
    }

    @GuardedBy("_connectionMap")
    private final IMConnection findConnectionResponsibleFor( final IMIdentity identity )
    {
        if( identity == null )
            return null;

        IMConnection responsibleParty = null;
        synchronized( _connectionMap )
        {
            for( IMConnection aConnection : _connectionMap.values() )
            {
                boolean responsible = aConnection.isResponsibleFor( identity );

                if( responsible )
                {
                    _logger.debug( "" + aConnection.getUserXmpp() + " is responsible for " + identity + "? " + responsible );
                    responsibleParty = aConnection;
                    break;
                }
            }
        }
        return responsibleParty;
    }

    /**
     * Note: Used only in unit tests
     */
    @Override
    public NewSessionResult start()
    {
        return start( false ); // Set to false since unit tests are using old parameterless version
    }

    /**
     * @throws IMSessionException if we are unable to start properly (e.g. openfire is not running)
     */
    @GuardedBy("_connectionMap")
    @Override
    public NewSessionResult start( boolean autoLogin )
    {
        synchronized( _connectionMap )
        {
            boolean failed = false;
            try
            {
                _logger.debug( "begin for " + _userId );
                IMUserXmppWrapper userXmppWrapper = _settingsService.getAllIMUserXmppForUser( _userId );

                SignInResult accumulator = parallelize( _execProvider.get(), wrapCallable( userXmppWrapper, autoLogin ) );
                _logger.debug( "All connections started for " + _userId );

                NewSessionResultImpl.Builder builder = new NewSessionResultImpl.Builder( true );
                for( SignInSuccessInfo succes : accumulator.success2() )
                    builder = builder.addSuccess( succes.id(), succes.permanent() );

                for( SignInErrorInfo errorInfo : accumulator.failed() )
                    builder = builder.addFailure( errorInfo.id(), errorInfo.permanent(), errorInfo.why() );

                return builder.build( MediaType.APPLICATION_JSON_TYPE );
            }
            catch( Exception e )
            {
                failed = true;
                throw new IMSessionException( "Unforseen error", e, SessionErrorCode.FAILED_START );
            }
            finally
            {
                if( failed && _connectionMap.size() > 0 )
                    stop();
            }
        }
    }

    private SignInResult parallelize( Executor e, Collection<Callable<SignInResult>> solvers )
        throws InterruptedException
    {
        SignInResult accumulator = seed();
        CompletionService<SignInResult> ecs = new ExecutorCompletionService<SignInResult>( e );
        for( Callable<SignInResult> s : solvers )
            ecs.submit( s );

        int n = solvers.size();
        for( int i = 0; i < n; ++i ) 
        {
            try
            {
                SignInResult r = ecs.take().get();
                if( r != null )
                {
                    _logger.debug( "adding result from " + i );
                    accumulator = _conjoiner.conjoin( accumulator, r );
                }
            }
            catch( ExecutionException ignored )
            {
                Throwable cause = ignored.getCause();
                if( cause != null )
                {
                    _logger.error( "executionexception: ", cause );
                    if( cause instanceof XMPPFault )
                        throw ((XMPPFault)cause);
                    else // TODO why ignore other RuntimeExceptions ?
                        _logger.error( "ignoring task " + i, ignored );
                }
            }
            catch( CancellationException ce )
            {
                _logger.error( "canceled task " + i, ce );
            }
        }
        return accumulator;
    }

    // 20090630 Changed from ArrayList to LinkedList
    private final Collection<Callable<SignInResult>> wrapCallable( IMUserXmppWrapper userXmppWrapper, boolean autoLogin )
    {
        List<Callable<SignInResult>> retval = new LinkedList<Callable<SignInResult>>();

        retval.add( new StartConnectionCallable( userXmppWrapper.primordial(), autoLogin, true ) );
        for( IMUserXmpp userXmpp : userXmppWrapper.others() )
            retval.add( new StartConnectionCallable( userXmpp, autoLogin, false ) );
        _logger.debug( "number of Callables for " + _userId + ": " + retval.size() );
        return retval;
    }

    private class StartConnectionCallable implements Callable<SignInResult>
    {
        public StartConnectionCallable( IMUserXmpp userXmpp, boolean autoLogin, boolean primordial )
        {
            callableUserXmpp = userXmpp; callableAutoLogin = autoLogin; callablePrimordial = primordial;
        }

        @Override
        public SignInResult call() throws Exception
        {
            SignInResult retval = startAnIMConnection( callableUserXmpp, callableAutoLogin, callablePrimordial );
            _logger.debug( callableUserXmpp.userXMPP() + " connected" );
            return retval;
        }

        private final IMUserXmpp callableUserXmpp;
        private final boolean callableAutoLogin;
        private final boolean callablePrimordial;
    }

    private final SignInResult seed()
    {
        final List<SignInErrorInfo> listOfFailed = new ArrayList<SignInErrorInfo>(1);
        final List<SignInSuccessInfo> listOfSuccesses2 = new ArrayList<SignInSuccessInfo>(1);

        return new SignInResult() 
        {
            public List<SignInErrorInfo> failed() { return listOfFailed; }
            public List<SignInSuccessInfo> success2() { return listOfSuccesses2; }
        };
    }

    /**
     * Called during initial {@code IMSession} creation
     *
     * @throws IllegalArgumentException if {@code imUserXmpp} is null
     */
    private final SignInResult startAnIMConnection( @Nonnull IMUserXmpp imUserXmpp, boolean autoLogin, boolean primordial )
    {
        Constraint.NonNullArgument.check( imUserXmpp, "imUserXmpp" );

        IMConnection aIMConn = _imConnFactory.create( _userId, imUserXmpp.userXMPP() );
        aIMConn.setPrimordial( primordial );

        SignInResult result = null;

        if( autoLogin )
            result = aIMConn.connectAndAutoSignin( imUserXmpp.transportsNonNulls() );
        else
            result = aIMConn.connectOnly( imUserXmpp.transportsNonNulls() );

        _connectionMap.put( imUserXmpp.userXMPP(), aIMConn );

        // Remove temporary logins leftover from the last session.
        try
        {
            if( aIMConn != null && aIMConn.isConnected() )
                aIMConn.removeAdhocLogins();
        }
        catch( Exception ignored ) {}

        _logger.debug( "Added: " + imUserXmpp.userXMPP() + " -> " + aIMConn.toString() );
        return result;
    }

    /**
     * Used only during adding of a new account 
     */
    private final void startAnIMConnection( final String userXmpp, boolean autoLogin )
    {
        if( Constraint.EmptyString.isFulfilledBy( userXmpp ) )
            return;

        IMConnection aIMConn = _imConnFactory.create( _userId, userXmpp );
        aIMConn.setPrimordial( false );

        if( autoLogin )
            aIMConn.connectAndAutoSignin( null );
        else
            aIMConn.connectOnly( null );

        _connectionMap.put( userXmpp, aIMConn );
        _logger.debug( "Added: " + userXmpp + " -> " + aIMConn.toString() );
    }

    /**
     * SPECs
     * <ul>
     * <li>needs to be thread safe
     * <li>idempotent
     * </ul>
     */
    @GuardedBy("_connectionMap")
    @Override
    public void stop()
    {
        _logger.debug( "Start to log out " + _userId );
        synchronized( _connectionMap )
        {
            stopAllIMConnections();
            _connectionMap.clear();
        }
        _logger.debug( "Completed log out for " + _userId );
    }

    private final void stopAllIMConnections()
    {
        for( String userXmpp : _connectionMap.keySet() )
        {
            IMConnection aImConn = null;
            try
            {
                aImConn = _connectionMap.get( userXmpp );
                aImConn.removeAdhocLogins();
            }
            catch( Exception e )
            {
                // TODO what do do ?
                _logger.error( "Encountered errors during shutdown", e );
            }
            finally
            {
                if( aImConn != null )
                    aImConn.shutdown();
            }
        }
    }

    /**
     * @return number of service(s) successfully removed (or zero if nothing was removed)
     * @throws IMSessionException if ....
     * @throws IllegalArgumentException if {@code identity} is null
     */
    @Override
    public int removeService( final IMIdentity identity )
    {
        NonNullArgument.check( identity, "identity" );
        int numberRemoved = 0;
        try
        {
            synchronized( _connectionMap )
            {
                IMConnection responsibleParty = null;
            // 1. Look for the responsible connection
                for( IMConnection aConnection : _connectionMap.values() )
                {
                    boolean responsible = aConnection.isResponsibleFor( identity );
                    _logger.debug( "" + aConnection.getUserXmpp() + " is responsible for " + identity + "? " + responsible );
                    if( responsible )
                    {
                        responsibleParty = aConnection;
                        break;
                    }
                }

            // 2. Short circuit if no connections are responsible for this identity 
                if( responsibleParty == null )
                    return 0;

            // 3. dispatch removal to the connection 
                numberRemoved = responsibleParty.removeTransportFor( identity );
            }

            return numberRemoved;
        }
        catch( Exception e )
        {
            throw new IMSessionException( Common.EMPTY_STRING, e, SessionErrorCode.REMOVE_SERVICE );
        }
    }

    /**
     * @throws IMSessionException if ....
     * @throws IllegalArgumentException if {@code userSetting} is null
     */
    @GuardedBy("_connectionMap")
    @Override
    public void addService( final IMUserSetting userSetting )
    {
        NonNullArgument.check( userSetting, "userSetting" );

        if( Constraint.EmptyString.isFulfilledBy( userSetting.userId() ) )
            throw new IllegalArgumentException( "IMUserSetting.userId() cannot be null" );

        boolean duplicateFound = false;
        try
        {
            synchronized( _connectionMap )
            {
            // 1. Decide if there is a duplicate
                for( IMConnection aConnection : _connectionMap.values() )
                {
                    boolean responsible = aConnection.isResponsibleFor( userSetting.identity() );
                    _logger.debug( "" + aConnection.getUserXmpp() + " is responsible for " + userSetting.identity() + "? " + responsible );
                    if( responsible )
                    {
                        duplicateFound = true;
                        break;
                    }
                }

                _logger.debug( "duplicateFound: " + duplicateFound );
                if( duplicateFound )
                    throw new IMSessionException( "An account with same identity exists for " + userSetting.identity(), null, SessionErrorCode.DUPLICATE_IM_ACCOUNT );

            // 2. if there is no duplicate, find out which IMConnection to send the addService

                IMConnection matchingConnection = null;
                for( IMConnection aConnection : _connectionMap.values() )
                {
                    if( ! aConnection.isSlotTakenForTransport( userSetting.identity().transport() ) )
                    {
                        _logger.debug( "search transport: " + userSetting.identity().transport() );
                        matchingConnection = aConnection;
                        break;
                    }
                }

                _logger.debug( (matchingConnection==null) ? "No IMConnection free" : "Connection " + matchingConnection.getUserXmpp() + " is available" );

                if( matchingConnection == null )
                {
                    // 3.1) create new connection
                    // TODO need to properly roll back over here
                    try
                    {
                        String userXmpp = _accountService.createNewXmppAccountForExisting( _userId, false );
                        startAnIMConnection( userXmpp, true );
                        matchingConnection = _connectionMap.get( userXmpp );
                    }
                    catch( Exception e )
                    {
                        throw new IMSessionException( "Not successful in creating new IMUserXmpp & starting it", e, SessionErrorCode.ADD_SERVICE );
                    }
                }

                matchingConnection.addTransportFor( userSetting );
            }
        }
        catch( IMSessionException isesse )
        {
            throw isesse;
        }
        catch( XMPPFault fault )
        {
            if( fault.XmppFaultCode() == XmppFaultCode.DISCO_TRANSPORT )
                throw new IMSessionException( "Possibility that transport " + userSetting.identity().transport() + " is not available", fault, SessionErrorCode.TRANSPORT_DOWN );

            throw new IMSessionException( Common.EMPTY_STRING, fault, SessionErrorCode.ADD_SERVICE );
        }
        catch( Exception e )
        {
            throw new IMSessionException( Common.EMPTY_STRING, e, SessionErrorCode.ADD_SERVICE );
        }
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append( "(IMSession) { userId: " ).append( _userId ).append( " }");
        return builder.toString();
    }

    /**
     * Guarantees:
     * <ul>
     * <li>Only {@code IMConnection}s which are started end up in {@code _connectionMap}
     * </ul>
     */
    private final Map<String, IMConnection> _connectionMap;

    private final String _userId;
    private final String _defaultRosterGroup;
    private final AccountService _accountService;
    private final SettingsService _settingsService;
    private final IMConnectionFactory _imConnFactory;
    private final SignInResultConjoiner _conjoiner;
    private final Provider<ExecutorService> _execProvider;

    private final Logger _logger = Logger.getLogger( IMSessionImpl.class );

}
