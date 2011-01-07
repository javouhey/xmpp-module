package com.raverun.im.application.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityExistsException;
import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import com.google.common.base.Predicate;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.raverun.im.application.AccountOperationException;
import com.raverun.im.application.AccountService;
import com.raverun.im.application.AccountOperationException.AccountErrorCode;
import com.raverun.im.common.IMConstants;
import com.raverun.im.common.Transactional;
import com.raverun.im.domain.IMIdentity;
import com.raverun.im.domain.IMUserSetting;
import com.raverun.im.domain.IMUserSettingFactory2;
import com.raverun.im.domain.IMUserXmpp;
import com.raverun.im.domain.impl.IMIdentityImpl;
import com.raverun.im.domain.impl.IMUserXmppImpl;
import com.raverun.im.infrastructure.persistence.TransportMapper;
import com.raverun.im.infrastructure.persistence.UserService;
import com.raverun.im.infrastructure.persistence.jdbc.IMUserSettingDaoFactory;
import com.raverun.im.infrastructure.persistence.jdbc.IMUserXmppDao;
import com.raverun.im.infrastructure.persistence.jdbc.IMUserXmppDaoFactory;
import com.raverun.im.infrastructure.persistence.jdbc.TransportDaoFactory;
import com.raverun.im.infrastructure.persistence.jdbc.UserDaoFactory;
import com.raverun.im.infrastructure.system.UUIDGenerator;
import com.raverun.im.infrastructure.xmpp.XMPPAccountService;
import com.raverun.im.infrastructure.xmpp.support.XMPPFault;
import com.raverun.shared.Common;
import com.raverun.shared.Constraint;
import com.raverun.shared.persistence.JdbcExecutionException;
import com.raverun.shared.persistence.JdbcTemplate;

public class AccountServiceImpl implements AccountService
{
    @Inject
    public AccountServiceImpl( XMPPAccountService xmppAccount, 
        @Named("xmpp.generic.password") String password , UUIDGenerator uuidGen,
        @Named("userexist") Predicate<String> userExistPredicate,
        @Named("ymail") Predicate<String> ymailPredicate,
        @Named("rocketmail") Predicate<String> rocketmailPredicate,
        @Named("yahoo") Predicate<String> yahooPredicate,
        @Named("google") Predicate<String> googlePredicate,
        @Named("qq") Predicate<String> qqPredicate,
        @Named("msn") Predicate<String> msnPredicate,
        UserDaoFactory userDaoFactory, UserService userService,
        Provider<DataSource> dsProvider,
        TransportDaoFactory transportDaoFactory, IMUserSettingDaoFactory userSettingDaoFactory,
        IMUserSettingFactory2 userSettingFactory2, IMUserXmppDaoFactory userXmppDaoFactory,
        TransportMapper transportMapper )
    {
        _rocketmailPredicate = rocketmailPredicate;
        _userExistPredicate = userExistPredicate;
        _ymailPredicate = ymailPredicate;
        _yahooPredicate = yahooPredicate;
        _googlePredicate = googlePredicate;
        _qqPredicate = qqPredicate;
        _msnPredicate = msnPredicate;
        _userDaoFactory = userDaoFactory;
        _userService = userService;
        _xmppAccount = xmppAccount;
        _dsProvider = dsProvider;
        _password = password;
        _uuidGen = uuidGen;

        _transportDaoFactory = transportDaoFactory;
        _userSettingFactory2 = userSettingFactory2;
        _userXmppDaoFactory = userXmppDaoFactory;
        _userSettingDaoFactory = userSettingDaoFactory;

        _transportMapper = transportMapper;
    }

    /**
     * Creates an account for user in the {@code im} module.
     * <p>
     * Precondition: {@code userid} does not exists yet
     *
     * @param userid non-nullable
     * @throws IllegalArgumentException if {@code userid} is null
     * @throws AccountOperationException
     */
    @Override
    public void createTotallyNewUser( String userid )
    {
        if( Constraint.EmptyString.isFulfilledBy( userid ))
            throw new IllegalArgumentException( "userid MUST NOT be empty" );

        boolean userCreatedForDb = false;

        try
        {
        // 1. create database account
            userCreatedForDb = _userService.create( userid );

        // 2. create XMPP account
            createNewXmppAccountForExisting( userid, true );
        }
        catch( PersistenceException pe )
        {
            _logger.error( Common.EMPTY_STRING, pe );
            if( pe instanceof EntityExistsException )
                throw new AccountOperationException( "", pe, AccountErrorCode.USERCONFLICT );

            throw new AccountOperationException( "", pe, AccountErrorCode.DATASTORE );
        }
        catch( IllegalStateException ise )
        {
            throw new AccountOperationException( "Missing userid in database", ise, AccountErrorCode.MISSINGUSER );
        }

    }

    @Override
    public void removeAllAccounts( List<String> listOfUserXmpps )
    {
        if( listOfUserXmpps == null || listOfUserXmpps.size() == 0 )
            return;

        for( String targetForDeletion : listOfUserXmpps )
        {
            try
            {
                _xmppAccount.removeAccount( targetForDeletion, _password );
            }
            catch( XMPPFault ignored )
            {
                _logger.error( "error attempting to remove XMPP account -> " + targetForDeletion, ignored );
            }
        }
    }

    @Override
    public void addService( String userid, IMUserSetting userSetting )
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeService( String userid, IMIdentity imIdentity )
    {
        // TODO Auto-generated method stub
        
    }

    private final String wrapPrimordial( boolean primordial, String userid )
    {
        if( primordial )
            return new String( userid + PRIMORDIAL_TAG ).intern();
        else
            return userid;
    }

    /**
     * @param userid non-nullable
     * @param isPrimordial - indicates whether the {@code IMUserXmpp} is primordial
     * @throws IllegalArgumentException if {@code userid} is null
     * @throws AccountOperationException if we're unable to create a new userXmpp
     * @return the userXmpp
     */
    @Override
    @Transactional
    public String createNewXmppAccountForExisting( final String userid, final boolean isPrimordial )
    {
        if( Constraint.EmptyString.isFulfilledBy( userid ))
            throw new IllegalArgumentException( "userid MUST NOT be empty" );

        if( ! _userExistPredicate.apply( userid ) )
            throw new IllegalStateException( "MISSING.USER userid [" + userid + "] is missing" );

    // 1. create XMPP account

        String generatedUserid = _uuidGen.generate();

        try
        {
            Map<String,String> userProperties = newMap();
            userProperties.put( KEY_XMPP_NAME, wrapPrimordial( isPrimordial, userid ) );
            _xmppAccount.createAccount( generatedUserid, _password, userProperties );
        }
        catch( XMPPFault xf )
        {
            throw new AccountOperationException( xf.getMessage(), xf, AccountErrorCode.XMPPCREATEACCOUNT );
        }

    // 2. create row mim_user_xmpp
        try
        {
            Long seq = createNewUserXmpp( userid, generatedUserid, isPrimordial );
            _logger.debug( "mc-im's User " + userid + " associated with xmpp account " + generatedUserid );
            return generatedUserid;
        }
        catch( JdbcExecutionException jee )
        {
            _logger.debug( "Failed to create primordial user xmpp", jee );
            // 3. If step #2 fails, I need to rollback the account created in #1
            try
            {
                _xmppAccount.removeAccount( generatedUserid, _password );
                throw new AccountOperationException( "Trouble accessing data store to create a IMUserXmpp", AccountErrorCode.XMPPCREATEACCOUNT );
            }
            catch( XMPPFault xf )
            {
                _logger.error( "error attempting to remove account for rollback", xf );
                throw new AccountOperationException( xf.getMessage(), xf, AccountErrorCode.XMPPREMOVEACCOUNTFORROLLBACK );
            }
        }
    }

    private Long createNewUserXmpp( String user, String userXmpp, boolean isPrimordial )
    {
        IMIdentityImpl.FromClientBuilder builder = new IMIdentityImpl.FromClientBuilder( 
            _transportMapper, _ymailPredicate, _rocketmailPredicate,
            _yahooPredicate, _googlePredicate, _qqPredicate, 
            _msnPredicate );

        IMIdentity identity = builder.imType( IMConstants.ClientLiteralsForTransport.MIM ).loginId( userXmpp ).build();

        final IMUserSetting aSetting = _userSettingFactory2.create( _transportMapper, identity, 
            _password, IMUserSetting.UserSettingType.AUTOLOGIN );

        aSetting.setUserid( user );

        IMUserXmppImpl.FromClientBuilder userXmppBuilder = new IMUserXmppImpl.FromClientBuilder( user, userXmpp, isPrimordial );
        final IMUserXmpp newUserXmpp = userXmppBuilder.userSetting( aSetting ).build();

        final JdbcTemplate<Long> template = new JdbcTemplate<Long>() 
        {
            @Override
            public Long doQuery( Connection connection ) throws SQLException
            {
                IMUserXmppDao dao = _userXmppDaoFactory.create( connection );
                return dao.create( newUserXmpp );
            }
        };
        Long userXmppPK = template.execute( _dsProvider.get(), Connection.TRANSACTION_REPEATABLE_READ );
        _logger.debug( "A IMUserXmpp " + ((userXmppPK != null) ? " with seq " + userXmppPK + 
            "successfully created" : "creation failed" ) );
        return userXmppPK;
    }

    private final <K,V> HashMap<K,V> newMap()
    {
        return new HashMap<K,V>( 1 );
    }

    private final String _password;
    private final UUIDGenerator _uuidGen;
    private final UserService _userService;
    private final XMPPAccountService _xmppAccount;
    private final Provider<DataSource> _dsProvider;

    private final Predicate<String> _ymailPredicate;
    private final Predicate<String> _rocketmailPredicate;
    private final Predicate<String> _yahooPredicate;
    private final Predicate<String> _googlePredicate;
    private final Predicate<String> _qqPredicate;
    private final Predicate<String> _msnPredicate;
    private final Predicate<String> _userExistPredicate;

    private final UserDaoFactory _userDaoFactory;

    private TransportMapper _transportMapper;
    private TransportDaoFactory _transportDaoFactory;
    private IMUserSettingFactory2 _userSettingFactory2;
    private IMUserSettingDaoFactory _userSettingDaoFactory;
    private final IMUserXmppDaoFactory _userXmppDaoFactory;

    private final static String PRIMORDIAL_TAG = " (primordial)";
    private final boolean PRIMORDIAL = true;
    private final static Logger _logger = Logger.getLogger( AccountServiceImpl.class );

}
