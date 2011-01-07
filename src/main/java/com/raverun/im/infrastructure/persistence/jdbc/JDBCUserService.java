package com.raverun.im.infrastructure.persistence.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.raverun.im.infrastructure.persistence.JDBC2JPAExceptionTranslator;
import com.raverun.im.infrastructure.persistence.UserService;
import com.raverun.im.infrastructure.persistence.dao.UserDaoIF.UserStatus;
import com.raverun.shared.persistence.JdbcExecutionException;
import com.raverun.shared.persistence.JdbcTemplate;

public class JDBCUserService implements UserService
{
    

    @Override
    public int count()
    {
        JdbcTemplate<Integer> template = new JdbcTemplate<Integer>()
        {
            @Override
            public Integer doQuery( Connection connection ) throws SQLException
            {
                UserDao dao = _userDaoFactory.create( connection );
                return dao.count();
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
    public List<User> find( final String userid )
    {
      JdbcTemplate<List<User>> template = new JdbcTemplate<List<User>>()
      {
          @Override
          public List<User> doQuery( Connection connection ) throws SQLException
          {
              UserDao dao = _userDaoFactory.create( connection );
              List<User> userList =  dao.find( userid );
              return userList;
          }
      };

      try
      {
          List<User> retval = template.execute( _dsProvider.get(), Connection.TRANSACTION_SERIALIZABLE );
          return retval;
      }
      catch( JdbcExecutionException jee )
      {
          throw _exceptionTranslator.translate( jee );
      }
        
        
//        JdbcTemplate<User> template = new JdbcTemplate<User>()
//        {
//            @Override
//            public User doQuery( Connection connection ) throws SQLException
//            {
//                UserDao dao = _userDaoFactory.create( connection );
//                List<User> userList =  dao.find( userid );
//                return ((userList.size() == 0) ? null : userList.get( 0 ));
//            }
//        };
//
//        try
//        {
//            User user = template.execute( _dsProvider.get(), Connection.TRANSACTION_SERIALIZABLE );
//            List<User> retval = new ArrayList<User>(1);
//            if( user != null )
//                retval.add( user );
//
//            return retval;
//        }
//        catch( JdbcExecutionException jee )
//        {
//            throw handleJdbcExecutionException( jee );
//        }
    }

    /**
     * @throws javax.persistence.PersistenceException 
     */
    @Override
    public boolean create( final String userid )
    {
        JdbcTemplate<Boolean> template = new JdbcTemplate<Boolean>()
        {
            @Override
            public Boolean doQuery( Connection connection ) throws SQLException
            {
                UserDao dao = _userDaoFactory.create( connection );
                return dao.create( userid );
            }
        };

        try
        {
            return template.execute( _dsProvider.get(), Connection.TRANSACTION_SERIALIZABLE );
        }
        catch( JdbcExecutionException jee )
        {
            throw _exceptionTranslator.translateEntityCreation( ENTITY_NAME, jee );
        }
    }

    @Override
    public boolean remove( final String userid )
    {
        JdbcTemplate<Boolean> template = new JdbcTemplate<Boolean>()
        {
            @Override
            public Boolean doQuery( Connection connection ) throws SQLException
            {
                UserDao dao = _userDaoFactory.create( connection );
                return dao.remove( userid );
            }
        };

        try
        {
            return template.execute( _dsProvider.get(), Connection.TRANSACTION_SERIALIZABLE );
        }
        catch( JdbcExecutionException jee )
        {
            throw _exceptionTranslator.translate( jee );
        }
    }

    /**
     * Idempotent
     *
     * @throws javax.persistence.PersistenceException if there was a problem persisting to the DB
     * @throws IllegalArgumentException if {@code userid} is null or invalid
     */
    @Override
    public int activate( @Nonnull final String userid )
    {
        JdbcTemplate<Integer> template = new JdbcTemplate<Integer>()
        {
            @Override
            public Integer doQuery( Connection connection ) throws SQLException
            {
                UserDao dao = _userDaoFactory.create( connection );
                return dao.updateStatus( userid, UserStatus.ACTIVE );
            }
        };

        try
        {
            return template.execute( _dsProvider.get(), Connection.TRANSACTION_SERIALIZABLE );
        }
        catch( JdbcExecutionException jee )
        {
            throw _exceptionTranslator.translate( jee );
        }
    }

    /**
     * Idempotent
     *
     * @throws javax.persistence.PersistenceException if there was a problem persisting to the DB
     * @throws IllegalArgumentException if {@code userid} is null or invalid
     */
    @Override
    public int suspend( @Nonnull final String userid )
    {
        JdbcTemplate<Integer> template = new JdbcTemplate<Integer>()
        {
            @Override
            public Integer doQuery( Connection connection ) throws SQLException
            {
                UserDao dao = _userDaoFactory.create( connection );
                return dao.updateStatus( userid, UserStatus.SUSPENDED );
            }
        };

        try
        {
            return template.execute( _dsProvider.get(), Connection.TRANSACTION_SERIALIZABLE );
        }
        catch( JdbcExecutionException jee )
        {
            throw _exceptionTranslator.translate( jee );
        }
    }

    /**
     * @throws javax.persistence.PersistenceException if there was a problem persisting to the DB
     * @throws IllegalArgumentException if {@code userid} is null or invalid
     */
    @Override
    public boolean isActive( @Nonnull String userid )
    {
        List<User> userList = find( userid );
        if( userList.size() == 0 )
            return false;

        return userList.get( 0 ).isActive();
    }

    @Inject
    public JDBCUserService( Provider<DataSource> dsProvider, UserDaoFactory userDaoFactory,
        JDBC2JPAExceptionTranslator exceptionTranslator )
    {
        _dsProvider          = dsProvider;
        _userDaoFactory      = userDaoFactory;
        _exceptionTranslator = exceptionTranslator;
    }

//    private final PersistenceException handleJdbcExecutionException( JdbcExecutionException jee )
//    {
//        if( jee.isSqlException() )
//        {
//            _logger.debug( "sql errorcode = " + jee.getSqlErrorCode() + " | sqlstate=" + jee.getSqlState());
//            if( jee.isRowLockTimeout() )
//                return new PessimisticLockException( "deadlocked or another transaction is holding row lock", jee );
//            else if( jee.isMysqlDown() )
//                return new DatabaseServerNotAvailableException( "cannot connect to mysql", jee );
//            else if( jee.getSqlErrorCode() == 1062 )
//                return new EntityExistsException( "supplied userid is a duplicate", jee );
//            else
//                return new PersistenceException( "generic SqlException", jee );
//        }
//        else
//            return new PersistenceException( "internal error", jee );
//    }

    private final static Logger _logger = Logger.getLogger( JDBCUserService.class );

    private final static String ENTITY_NAME = "userid";

    private final JDBC2JPAExceptionTranslator _exceptionTranslator;
    private final Provider<DataSource> _dsProvider;
    private final UserDaoFactory _userDaoFactory;
}
