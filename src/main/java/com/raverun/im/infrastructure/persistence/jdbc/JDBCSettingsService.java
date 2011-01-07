package com.raverun.im.infrastructure.persistence.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.raverun.im.common.Transport;
import com.raverun.im.domain.IMIdentity;
import com.raverun.im.domain.IMUserSetting;
import com.raverun.im.domain.IMUserXmppWrapper;
import com.raverun.im.infrastructure.persistence.JDBC2JPAExceptionTranslator;
import com.raverun.im.infrastructure.persistence.SettingsService;
import com.raverun.shared.Constraint.NonNullArgument;
import com.raverun.shared.persistence.JdbcExecutionException;
import com.raverun.shared.persistence.JdbcTemplate;

public class JDBCSettingsService implements SettingsService
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
    @Override
    public int removeSetting( final String userid, final String userXmpp,
        IMIdentity identity )
    {
        NonNullArgument.check( userid, "userid" );
        NonNullArgument.check( userXmpp, "userXmpp" );
        NonNullArgument.check( identity, "identity" );

        final Transport transport = identity.transport();

        final JdbcTemplate<Integer> template = new JdbcTemplate<Integer>() 
        {
            @Override
            public Integer doQuery( Connection connection ) throws SQLException
            {
                IMUserXmppDao dao = _userXmppDaoFactory.create( connection );
                Long pk = dao.findSequenceForTransport( userid, userXmpp, transport );
                IMUserSettingDao daoSetting = _userSettingDaoFactory.create( connection );
                return daoSetting.delete( pk );
            }
        };

        try
        {
            int rowsDeleted = template.execute( _dsProvider.get(), Connection.TRANSACTION_READ_COMMITTED );
            return rowsDeleted;
        }
        catch( JdbcExecutionException jee )
        {
            throw _exceptionTranslator.translate( jee );
        }
    }

    /**
     * A {@code IMUserXmpp} is identified by the combination {@code userid} & {@code userXmpp}.
     * Persist {@code aSetting} and add it the identified {@code IMUserXmpp}.
     *
     * @param userid - non empty string
     * @param userXmpp - non empty string
     * @throws javax.persistence.PersistenceException if DB layer encountered errors
     * @throws IllegalArgumentException if {@code userid} OR {@code userXmpp} is null
     */
    @Override
    public void addSetting( final String userid, final String userXmpp,
        final IMUserSetting aSetting )
    {
        NonNullArgument.check( userid, "userid" );
        NonNullArgument.check( userXmpp, "userXmpp" );

        final JdbcTemplate<Boolean> template = new JdbcTemplate<Boolean>() 
        {
            @Override
            public Boolean doQuery( Connection connection ) throws SQLException
            {
                IMUserXmppDao dao = _userXmppDaoFactory.create( connection );
                return dao.addSetting( userid, userXmpp, aSetting );
            }
        };

        try
        {
            Boolean result = template.execute( _dsProvider.get(), Connection.TRANSACTION_SERIALIZABLE );
        }
        catch( JdbcExecutionException jee )
        {
            throw _exceptionTranslator.translate( jee );
        }
    }

    /**
     * @throws javax.persistence.PersistenceException if DB layer encountered errors
     * @throws IllegalArgumentException
     */
     public int updateSetting( @Nonnull final String userid, @Nonnull final String userXmpp, 
         @Nonnull final Transport transport, @Nullable final String password, 
         @Nonnull final IMUserSetting.UserSettingType saved )
     {
         NonNullArgument.check( saved, "saved" );
         NonNullArgument.check( userid, "userid" );
         NonNullArgument.check( userXmpp, "userXmpp" );
         NonNullArgument.check( transport, "transport" );

         final JdbcTemplate<Integer> template = new JdbcTemplate<Integer>() 
         {
             @Override
             public Integer doQuery( Connection connection ) throws SQLException
             {
                 IMUserXmppDao dao = _userXmppDaoFactory.create( connection );
                 Long pk = dao.findSequenceForTransport( userid, userXmpp, transport );
                 if( pk == -1L )
                     return 0;

                 IMUserSettingDao daoSetting = _userSettingDaoFactory.create( connection );
                 return daoSetting.update( pk, password, saved );
             }
         };

         try
         {
             int rowsUpdated = template.execute( _dsProvider.get(), Connection.TRANSACTION_READ_COMMITTED );
             return rowsUpdated;
         }
         catch( JdbcExecutionException jee )
         {
             throw _exceptionTranslator.translate( jee );
         }
     }

    /**
     * @param userid - non empty string
     * @param userXmpp - non empty string
     * @throws javax.persistence.PersistenceException if DB layer encountered errors
     * @throws IllegalArgumentException if {@code userid} OR {@code userXmpp} is null
     */
    @Override
    public Set<IMUserSetting> getAllSettingsFor( @Nonnull final String userid, @Nonnull final String userXmpp )
    {
        NonNullArgument.check( userid, "userid" );
        NonNullArgument.check( userXmpp, "userXmpp" );

        final JdbcTemplate<Set<IMUserSetting>> template = new JdbcTemplate<Set<IMUserSetting>>() 
        {
            @Override
            public Set<IMUserSetting> doQuery( Connection connection ) throws SQLException
            {
                IMUserXmppDao dao = _userXmppDaoFactory.create( connection );
                return dao.findAll( userid, userXmpp );
            }
        };

        try
        {
            return template.execute( _dsProvider.get(), Connection.TRANSACTION_REPEATABLE_READ );
        }
        catch( JdbcExecutionException jee )
        {
            throw _exceptionTranslator.translate( jee );
        }
    }

    /**
     * @param userid - non empty string
     * @param userXmpp - non empty string
     * @param savedType - non nullable
     * @throws javax.persistence.PersistenceException if DB layer encountered errors
     * @throws IllegalArgumentException if {@code userid} OR {@code userXmpp} OR {@code savedType} is null
     */
    @Override
    public Set<IMUserSetting> getAllSettingsFor( @Nonnull final String userid, @Nonnull final String userXmpp, 
        @Nonnull final IMUserSetting.UserSettingType savedType )
    {
        NonNullArgument.check( userid, "userid" );
        NonNullArgument.check( userXmpp, "userXmpp" );
        NonNullArgument.check( savedType, "savedType" );

        // TODO possible deadlock?
        final JdbcTemplate<Set<IMUserSetting>> template = new JdbcTemplate<Set<IMUserSetting>>() 
        {
            @Override
            public Set<IMUserSetting> doQuery( Connection connection ) throws SQLException
            {
                IMUserXmppDao dao = _userXmppDaoFactory.create( connection );
                return dao.findAllForFilterBySaved( userid, userXmpp, savedType );
            }
        };

        try
        {
            return template.execute( _dsProvider.get(), Connection.TRANSACTION_REPEATABLE_READ );
        }
        catch( JdbcExecutionException jee )
        {
            throw _exceptionTranslator.translate( jee );
        }
    }

    @Override
    public IMUserXmppWrapper getAllIMUserXmppForUser( @Nonnull final String userid )
    {
        NonNullArgument.check( userid, "userid" );

        final JdbcTemplate<IMUserXmppWrapper> template = new JdbcTemplate<IMUserXmppWrapper>() 
        {
            @Override
            public IMUserXmppWrapper doQuery( Connection connection ) throws SQLException
            {
                IMUserXmppDao dao = _userXmppDaoFactory.create( connection );
                return dao.findAllUserXmppForUser( userid );
            }
        };

        try
        {
            return template.execute( _dsProvider.get(), Connection.TRANSACTION_REPEATABLE_READ );
        }
        catch( JdbcExecutionException jee )
        {
            throw _exceptionTranslator.translate( jee );
        }
    }

    /**
     * @throws IllegalArgumentException if {@code userid} OR {@code userXmpp} OR {@code transport} is null
     */
    @Nullable 
    public IMUserSetting getOneSettingFor( @Nonnull final String userid, @Nonnull final String userXmpp, @Nonnull final Transport transport )
    {
         NonNullArgument.check( userid, "userid" );
         NonNullArgument.check( userXmpp, "userXmpp" );
         NonNullArgument.check( transport, "transport" );

         final JdbcTemplate<IMUserSetting> template = new JdbcTemplate<IMUserSetting>() 
         {
             @Override
             public IMUserSetting doQuery( Connection connection ) throws SQLException
             {
                 IMUserXmppDao dao = _userXmppDaoFactory.create( connection );
                 return dao.findSingle( userid, userXmpp, transport );
             }
         };

         try
         {
             return template.execute( _dsProvider.get(), Connection.TRANSACTION_REPEATABLE_READ );
         }
         catch( JdbcExecutionException jee )
         {
             throw _exceptionTranslator.translate( jee );
         }
    }

    @Override
    public String getUserFor( final String userXmpp )
    {
        NonNullArgument.check( userXmpp, "userXmpp" );

        final JdbcTemplate<String> template = new JdbcTemplate<String>() 
        {
            @Override
            public String doQuery( Connection connection ) throws SQLException
            {
                IMUserXmppDao dao = _userXmppDaoFactory.create( connection );
                return dao.findUserFor( userXmpp );
            }
        };

        try
        {
            return template.execute( _dsProvider.get(), Connection.TRANSACTION_REPEATABLE_READ );
        }
        catch( JdbcExecutionException jee )
        {
            throw _exceptionTranslator.translate( jee );
        }
    }

    @Inject
    public JDBCSettingsService( IMUserXmppDaoFactory userXmppDaoFactory,
        Provider<DataSource> dsProvider, JDBC2JPAExceptionTranslator exceptionTranslator,
        IMUserSettingDaoFactory userSettingDaoFactory )
    {
        _dsProvider = dsProvider;
        _userXmppDaoFactory = userXmppDaoFactory;
        _exceptionTranslator = exceptionTranslator;
        _userSettingDaoFactory = userSettingDaoFactory;
    }

    private final Provider<DataSource> _dsProvider;
    private final IMUserXmppDaoFactory _userXmppDaoFactory;
    private final IMUserSettingDaoFactory _userSettingDaoFactory;
    private final JDBC2JPAExceptionTranslator _exceptionTranslator;

    private final static Logger _logger = Logger.getLogger( JDBCSettingsService.class );

}
