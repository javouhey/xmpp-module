package com.raverun.im.infrastructure.persistence.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.raverun.im.domain.IMMessageChat;
import com.raverun.im.domain.IMMessageHeadline;
import com.raverun.im.infrastructure.persistence.JDBC2JPAExceptionTranslator;
import com.raverun.im.infrastructure.persistence.MessageService;
import com.raverun.shared.Constraint.NonNullArgument;
import com.raverun.shared.persistence.JdbcExecutionException;
import com.raverun.shared.persistence.JdbcTemplate;

public class JDBCMessageService implements MessageService
{
    @Override
    public int purgeChatsFor( final String user )
    {
        NonNullArgument.check( user, "user" );

        final JdbcTemplate<Integer> template = new JdbcTemplate<Integer>() 
        {
            @Override
            public Integer doQuery( Connection connection ) throws SQLException
            {
                IMMessageChatDao dao = _chatDaoFactory.create( connection );
                return dao.purgeAllForUser( user );
            }
        };

        try
        {
            Integer result = template.execute( _dsProvider.get(), Connection.TRANSACTION_REPEATABLE_READ );
            _logger.debug( "Deleted " + result + " chat messages for user " + user );
            return result;
        }
        catch( JdbcExecutionException jee )
        {
            throw _exceptionTranslator.translate( jee );
        }
    }

    @Override
    public void add( @Nonnull final IMMessageChat chat )
    {
        NonNullArgument.check( chat, "chat" );

        final JdbcTemplate<Long> template = new JdbcTemplate<Long>() 
        {
            @Override
            public Long doQuery( Connection connection ) throws SQLException
            {
                IMMessageChatDao dao = _chatDaoFactory.create( connection );
                return dao.create( chat );
            }
        };

        try
        {
            Long result = template.execute( _dsProvider.get(), Connection.TRANSACTION_READ_COMMITTED );
            _logger.debug( "Created a chat message with sequence " + result );
        }
        catch( JdbcExecutionException jee )
        {
            throw _exceptionTranslator.translate( jee );
        }

    }

    @Override
    public void add( final IMMessageHeadline headline )
    {
        _logger.debug( "(mockup) adding headline: " + headline.message() );
        NonNullArgument.check( headline, "headline" );

        final JdbcTemplate<Long> template = new JdbcTemplate<Long>() 
        {
            @Override
            public Long doQuery( Connection connection ) throws SQLException
            {
                IMMessageHeadlineDao dao = _headlineDaoFactory.create( connection );
                return dao.create( headline );
            }
        };

        try
        {
            Long result = template.execute( _dsProvider.get(), Connection.TRANSACTION_READ_COMMITTED );
            _logger.debug( "Created a headline message with sequence " + result );
        }
        catch( JdbcExecutionException jee )
        {
            throw _exceptionTranslator.translate( jee );
        }
    }

    @Override
    public List<IMMessageChat> findAllChatsFor( final String user )
    {
        NonNullArgument.check( user, "user" );

        final JdbcTemplate<List<IMMessageChat>> template = new JdbcTemplate<List<IMMessageChat>>() 
        {
            @Override
            public List<IMMessageChat> doQuery( Connection connection ) throws SQLException
            {
                IMMessageChatDao dao = _chatDaoFactory.create( connection );
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
    public List<IMMessageHeadline> findAllHeadlinesFor( final String user )
    {
        NonNullArgument.check( user, "user" );

        final JdbcTemplate<List<IMMessageHeadline>> template = new JdbcTemplate<List<IMMessageHeadline>>() 
        {
            @Override
            public List<IMMessageHeadline> doQuery( Connection connection ) throws SQLException
            {
                IMMessageHeadlineDao dao = _headlineDaoFactory.create( connection );
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
    public int purgeHeadlinesFor( final String user )
    {
        NonNullArgument.check( user, "user" );

        final JdbcTemplate<Integer> template = new JdbcTemplate<Integer>() 
        {
            @Override
            public Integer doQuery( Connection connection ) throws SQLException
            {
                IMMessageHeadlineDao dao = _headlineDaoFactory.create( connection );
                return dao.purgeAllForUser( user );
            }
        };

        try
        {
            Integer result = template.execute( _dsProvider.get(), Connection.TRANSACTION_REPEATABLE_READ );
            _logger.debug( "Deleted " + result + " headline messages for user " + user );
            return result;
        }
        catch( JdbcExecutionException jee )
        {
            throw _exceptionTranslator.translate( jee );
        }
    }

    @Inject
    public JDBCMessageService( JDBC2JPAExceptionTranslator exceptionTranslator,
        Provider<DataSource> dsProvider, 
        IMMessageChatDaoFactory chatDaoFactory,
        IMMessageHeadlineDaoFactory headlineDaoFactory )
    {
        _dsProvider = dsProvider;
        _exceptionTranslator = exceptionTranslator;
        _chatDaoFactory = chatDaoFactory;
        _headlineDaoFactory = headlineDaoFactory;
    }

    private final Provider<DataSource> _dsProvider;
    private final IMMessageChatDaoFactory _chatDaoFactory;
    private final IMMessageHeadlineDaoFactory _headlineDaoFactory;
    private final JDBC2JPAExceptionTranslator _exceptionTranslator;

    private final static Logger _logger = Logger.getLogger( JDBCMessageService.class );

}
