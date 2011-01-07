package com.raverun.im.infrastructure.persistence.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.raverun.im.domain.IMPresence;
import com.raverun.im.infrastructure.persistence.JDBC2JPAExceptionTranslator;
import com.raverun.im.infrastructure.persistence.PresenceService;
import com.raverun.shared.Constraint.NonNullArgument;
import com.raverun.shared.persistence.JdbcExecutionException;
import com.raverun.shared.persistence.JdbcTemplate;

public class JDBCPresenceService implements PresenceService
{

    @Override
    public void add( final IMPresence presence )
    {
        _logger.debug( "adding: " + presence.hashCode() + " | " + presence.toString() );
        NonNullArgument.check( presence, "presence" );

        final JdbcTemplate<Long> template = new JdbcTemplate<Long>() 
        {
            @Override
            public Long doQuery( Connection connection ) throws SQLException
            {
                IMPresenceDao dao = _presenceDaoFactory.create( connection );
                return dao.create( presence );
            }
        };

        try
        {
            Long result = template.execute( _dsProvider.get(), Connection.TRANSACTION_READ_COMMITTED );
            _logger.debug( "Created a presence with sequence " + result );
        }
        catch( JdbcExecutionException jee )
        {
            throw _exceptionTranslator.translate( jee );
        }
    }

    @Override
    public List<IMPresence> findAllPresencesFor( final String user )
    {
        NonNullArgument.check( user, "user" );

        final JdbcTemplate<List<IMPresence>> template = new JdbcTemplate<List<IMPresence>>() 
        {
            @Override
            public List<IMPresence> doQuery( Connection connection ) throws SQLException
            {
                IMPresenceDao dao = _presenceDaoFactory.create( connection );
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
    public int purgePresencesFor( final String user )
    {
        NonNullArgument.check( user, "user" );

        final JdbcTemplate<Integer> template = new JdbcTemplate<Integer>() 
        {
            @Override
            public Integer doQuery( Connection connection ) throws SQLException
            {
                IMPresenceDao dao = _presenceDaoFactory.create( connection );
                return dao.purgeAllForUser( user );
            }
        };

        try
        {
            Integer result = template.execute( _dsProvider.get(), Connection.TRANSACTION_REPEATABLE_READ );
            _logger.debug( "Deleted " + result + " presences for user " + user );
            return result;
        }
        catch( JdbcExecutionException jee )
        {
            throw _exceptionTranslator.translate( jee );
        }
    }

    @Inject
    public JDBCPresenceService( JDBC2JPAExceptionTranslator exceptionTranslator,
        Provider<DataSource> dsProvider, IMPresenceDaoFactory presenceDaoFactory ) 
    {
        _dsProvider = dsProvider;
        _exceptionTranslator = exceptionTranslator;
        _presenceDaoFactory = presenceDaoFactory;
    }

    private final Provider<DataSource> _dsProvider;
    private final IMPresenceDaoFactory _presenceDaoFactory;
    private final JDBC2JPAExceptionTranslator _exceptionTranslator;

    private final static Logger _logger = Logger.getLogger( JDBCPresenceService.class );
}
