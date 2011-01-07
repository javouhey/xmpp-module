package com.raverun.im.infrastructure.persistence.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.raverun.im.common.Transport;
import com.raverun.im.domain.IMSubscriptionRequest;
import com.raverun.im.infrastructure.persistence.JDBC2JPAExceptionTranslator;
import com.raverun.im.infrastructure.persistence.SubscriptionService;
import com.raverun.shared.Constraint.NonNullArgument;
import com.raverun.shared.persistence.JdbcExecutionException;
import com.raverun.shared.persistence.JdbcTemplate;

public class JDBCSubscriptionService implements SubscriptionService
{
    @Override
    public int purgeFor( final String user )
    {
        NonNullArgument.check( user, "user" );

        final JdbcTemplate<Integer> template = new JdbcTemplate<Integer>() 
        {
            @Override
            public Integer doQuery( Connection connection ) throws SQLException
            {
                IMSubscriptionRequestDao dao = _subRequestDaoFactory.create( connection );
                return dao.purgeAllForUser( user );
            }
        };

        try
        {
            Integer result = template.execute( _dsProvider.get(), Connection.TRANSACTION_REPEATABLE_READ );
            _logger.debug( "Deleted " + result + " subscription request for user " + user );
            return result;
        }
        catch( JdbcExecutionException jee )
        {
            throw _exceptionTranslator.translate( jee );
        }
    }

    @Override
    public void addSubscriptionRequest( final String receiver, final String sender,
        final Transport transport, final String user )
    {
        NonNullArgument.check( user, "user" );
        NonNullArgument.check( sender, "sender" );
        NonNullArgument.check( receiver, "receiver" );
        NonNullArgument.check( transport, "transport" );

        final JdbcTemplate<Long> template = new JdbcTemplate<Long>() 
        {
            @Override
            public Long doQuery( Connection connection ) throws SQLException
            {
                IMSubscriptionRequestDao dao = _subRequestDaoFactory.create( connection );
                return dao.create( receiver, sender, transport, user );
            }
        };

        try
        {
            Long result = template.execute( _dsProvider.get(), Connection.TRANSACTION_READ_COMMITTED );
            _logger.debug( "Created a subscription request with sequence " + result );
        }
        catch( JdbcExecutionException jee )
        {
            throw _exceptionTranslator.translate( jee );
        }

    }

    @Override
    public List<IMSubscriptionRequest> findAllFor( final String user )
    {
        NonNullArgument.check( user, "user" );

        final JdbcTemplate<List<IMSubscriptionRequest>> template = new JdbcTemplate<List<IMSubscriptionRequest>>() 
        {
            @Override
            public List<IMSubscriptionRequest> doQuery( Connection connection ) throws SQLException
            {
                IMSubscriptionRequestDao dao = _subRequestDaoFactory.create( connection );
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

    @Inject
    public JDBCSubscriptionService( JDBC2JPAExceptionTranslator exceptionTranslator,
        Provider<DataSource> dsProvider, 
        IMSubscriptionRequestDaoFactory subRequestDaoFactory )
    {
        _dsProvider = dsProvider;
        _exceptionTranslator = exceptionTranslator;
        _subRequestDaoFactory = subRequestDaoFactory;
    }

    private final Provider<DataSource> _dsProvider;
    private final IMSubscriptionRequestDaoFactory _subRequestDaoFactory;
    private final JDBC2JPAExceptionTranslator _exceptionTranslator;

    private final static Logger _logger = Logger.getLogger( JDBCSubscriptionService.class );

}
