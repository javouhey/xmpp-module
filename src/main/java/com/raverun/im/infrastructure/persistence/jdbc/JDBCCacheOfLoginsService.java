package com.raverun.im.infrastructure.persistence.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import javax.annotation.Nonnull;
import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.raverun.im.domain.IMLoginCache;
import com.raverun.im.infrastructure.persistence.CacheOfLoginsService;
import com.raverun.im.infrastructure.persistence.JDBC2JPAExceptionTranslator;
import com.raverun.im.infrastructure.persistence.dao.CacheOfLoginsDaoIF;
import com.raverun.shared.Constraint.NonNullArgument;
import com.raverun.shared.persistence.JdbcExecutionException;
import com.raverun.shared.persistence.JdbcTemplate;

public class JDBCCacheOfLoginsService implements CacheOfLoginsService
{
    /**
     * @param user - a existing user
     * @throws PersistenceException
     * @throws IllegalArgumentException if any of the parameters is null
     */
    public int delete( @Nonnull final String user )
    {
        NonNullArgument.check( user, "user" );

        final JdbcTemplate<Integer> template = new JdbcTemplate<Integer>() 
        {
            @Override
            public Integer doQuery( Connection connection ) throws SQLException
            {
                CacheOfLoginsDaoIF dao = _daoFactory.create( connection );
                return dao.delete( user );
            }
        };

        try
        {
            return template.execute( _dsProvider.get(), Connection.TRANSACTION_READ_COMMITTED );
        }
        catch( JdbcExecutionException jee )
        {
            throw _exceptionTranslator.translate( jee );
        }
    }

    @Override
    public void add( final IMLoginCache login )
    {
        NonNullArgument.check( login, "login" );
        _logger.debug( "adding: " + login.userId() + " | " + login.device() );

        final JdbcTemplate<Void> template = new JdbcTemplate<Void>() 
        {
            @Override
            public Void doQuery( Connection connection ) throws SQLException
            {
                CacheOfLoginsDaoIF dao = _daoFactory.create( connection );
                dao.create( login );
                return null;
            }
        };

        try
        {
            template.execute( _dsProvider.get(), Connection.TRANSACTION_READ_COMMITTED );
            _logger.debug( "Created entry for " + login.userId() + " in mim_login_cache" );
        }
        catch( JdbcExecutionException jee )
        {
            throw _exceptionTranslator.translateEntityCreation( "userid", jee );
        }
    }

    @Override
    public String findDeviceForCurrentlyLoggedIn( final String user )
    {
        NonNullArgument.check( user, "user" );

        final JdbcTemplate<String> template = new JdbcTemplate<String>() 
        {
            @Override
            public String doQuery( Connection connection ) throws SQLException
            {
                CacheOfLoginsDaoIF dao = _daoFactory.create( connection );
                return dao.findDeviceFor( user );
            }
        };

        try
        {
            return template.execute( _dsProvider.get(), Connection.TRANSACTION_READ_COMMITTED );
        }
        catch( JdbcExecutionException jee )
        {
            throw _exceptionTranslator.translate( jee );
        }
    }

    @Override
    public void purgeAll()
    {
        final JdbcTemplate<Void> template = new JdbcTemplate<Void>() 
        {
            @Override
            public Void doQuery( Connection connection ) throws SQLException
            {
                CacheOfLoginsDaoIF dao = _daoFactory.create( connection );
                _logger.debug( "purged " + dao.purgeAll() + " cached logins in mim_login_cache" );
                return null;
            }
        };

        try
        {
            template.execute( _dsProvider.get(), Connection.TRANSACTION_READ_COMMITTED );
        }
        catch( JdbcExecutionException jee ) 
        {
            _logger.error( "swallowed", jee );
        }
    }

    @Inject
    public JDBCCacheOfLoginsService( JDBC2JPAExceptionTranslator exceptionTranslator,
        Provider<DataSource> dsProvider, CacheOfLoginsDaoFactory presenceDaoFactory ) 
    {
        _dsProvider = dsProvider;
        _exceptionTranslator = exceptionTranslator;
        _daoFactory = presenceDaoFactory;
    }

    private final Provider<DataSource> _dsProvider;
    private final CacheOfLoginsDaoFactory _daoFactory;
    private final JDBC2JPAExceptionTranslator _exceptionTranslator;

    private final static Logger _logger = Logger.getLogger( JDBCCacheOfLoginsService.class );
}
