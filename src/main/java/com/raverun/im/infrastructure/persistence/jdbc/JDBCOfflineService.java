package com.raverun.im.infrastructure.persistence.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.raverun.im.domain.IMOffline;
import com.raverun.im.infrastructure.persistence.JDBC2JPAExceptionTranslator;
import com.raverun.im.infrastructure.persistence.OfflineService;
import com.raverun.im.infrastructure.persistence.dao.IMOfflineDaoIF;
import com.raverun.shared.Constraint.NonNullArgument;
import com.raverun.shared.persistence.JdbcExecutionException;
import com.raverun.shared.persistence.JdbcTemplate;

public class JDBCOfflineService implements OfflineService
{

    @Override
    public void add( final IMOffline offline )
    {
        _logger.debug( "adding: " + offline.user() + " | " + offline.receiver().toString() );
        NonNullArgument.check( offline, "offline" );

        final JdbcTemplate<Long> template = new JdbcTemplate<Long>() 
        {
            @Override
            public Long doQuery( Connection connection ) throws SQLException
            {
                IMOfflineDaoIF dao = _offlineDaoFactory.create( connection );
                return dao.create( offline );
            }
        };

        try
        {
            Long result = template.execute( _dsProvider.get(), Connection.TRANSACTION_READ_COMMITTED );
            _logger.debug( "Created an offline notification with sequence " + result );
        }
        catch( JdbcExecutionException jee )
        {
            throw _exceptionTranslator.translate( jee );
        }
    }

    @Override
    public List<IMOffline> findAllOfflineNotificationsFor( final String user )
    {
        NonNullArgument.check( user, "user" );

        final JdbcTemplate<List<IMOffline>> template = new JdbcTemplate<List<IMOffline>>() 
        {
            @Override
            public List<IMOffline> doQuery( Connection connection ) throws SQLException
            {
                IMOfflineDaoIF dao = _offlineDaoFactory.create( connection );
                return dao.getAllForUser( user );
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
    public int purgeOfflineNotificationsFor( final String user )
    {
        NonNullArgument.check( user, "user" );

        final JdbcTemplate<Integer> template = new JdbcTemplate<Integer>() 
        {
            @Override
            public Integer doQuery( Connection connection ) throws SQLException
            {
                IMOfflineDaoIF dao = _offlineDaoFactory.create( connection );
                return dao.purgeAllForUser( user );
            }
        };

        try
        {
            Integer result = template.execute( _dsProvider.get(), Connection.TRANSACTION_REPEATABLE_READ );
            _logger.debug( "Deleted " + result + " offline notifications for user " + user );
            return result;
        }
        catch( JdbcExecutionException jee )
        {
            throw _exceptionTranslator.translate( jee );
        }
    }

    @Inject
    public JDBCOfflineService( JDBC2JPAExceptionTranslator exceptionTranslator,
        Provider<DataSource> dsProvider, IMOfflineDaoFactory daoFactory ) 
    {
        _dsProvider = dsProvider;
        _exceptionTranslator = exceptionTranslator;
        _offlineDaoFactory = daoFactory;
    }
    
    private final Provider<DataSource> _dsProvider;
    private final IMOfflineDaoFactory _offlineDaoFactory;
    private final JDBC2JPAExceptionTranslator _exceptionTranslator;

    private final static Logger _logger = Logger.getLogger( JDBCOfflineService.class );
}
