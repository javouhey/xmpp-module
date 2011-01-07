package com.raverun.im.infrastructure.persistence.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import org.apache.log4j.Logger;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.raverun.im.common.Transport;
import com.raverun.im.infrastructure.persistence.DatabaseServerNotAvailableException;
import com.raverun.im.infrastructure.persistence.PessimisticLockException;
import com.raverun.im.infrastructure.persistence.TransportMapper;
import com.raverun.shared.Constraint;
import com.raverun.shared.persistence.JdbcExecutionException;
import com.raverun.shared.persistence.JdbcTemplate;

@ThreadSafe
public class JDBCTransportMapper implements TransportMapper
{
    @GuardedBy("_cache2")
    @Override
    public Integer sequenceFor( final Transport transport )
    {
        Constraint.NonNullArgument.check( transport, "transport" );
        synchronized( _cache2 )
        {
            if( _cache2.containsKey( transport.code() ) )
                return _cache2.get(  transport.code() );
        }

        JdbcTemplate<Integer> template = new JdbcTemplate<Integer>()
        {
            @Override
            public Integer doQuery( Connection connection ) throws SQLException
            {
                TransportDao dao = _daoFactory.create( connection );
                List<Integer> results = dao.find( transport.code() );
                if( results.size() > 0 )
                    return results.get( 0 );

                return null;
            }
        };

        try
        {
            Integer seq = template.execute( _dsProvider.get(), Connection.TRANSACTION_REPEATABLE_READ );
            if( seq != null )
            {
                synchronized( _cache2 )
                {
                    _cache2.put( transport.code(), seq );
                }
            }
            return seq;
        }
        catch( JdbcExecutionException jee )
        {
            throw handleJdbcExecutionException( jee );
        }
    }

    @GuardedBy("_cache2")
    @Override
    public Transport transportFor( final Integer sequence )
    {
        Constraint.NonNullArgument.check( sequence, "sequence" );
        synchronized( _cache2 )
        {
            BiMap<Integer,String> reversedCache = _cache2.inverse();
            if( reversedCache.containsKey( sequence ))
                return Transport.valueOf( (reversedCache.get( sequence )).toUpperCase() );

        }

        JdbcTemplate<String> template = new JdbcTemplate<String>()
        {
            @Override
            public String doQuery( Connection connection ) throws SQLException
            {
                TransportDao dao = _daoFactory.create( connection );
                List<String> results = dao.find( sequence );
                if( results.size() > 0 )
                    return results.get( 0 );

                return null;
            }
        };

        try
        {
            String key = template.execute( _dsProvider.get(), Connection.TRANSACTION_REPEATABLE_READ );
            if( key != null )
            {
                synchronized( _cache2 )
                {
                    BiMap<Integer,String> reversedCache = _cache2.inverse();
                    reversedCache.put( sequence, key );
                    return Transport.valueOf( key.toUpperCase() );
                }
            }

            return null;
        }
        catch( JdbcExecutionException jee )
        {
            throw handleJdbcExecutionException( jee );
        }
    }

    @Inject
    public JDBCTransportMapper( Provider<DataSource> dsProvider, TransportDaoFactory daoFactory  )
    {
        _dsProvider = dsProvider;
        _daoFactory = daoFactory;
    }

    private final PersistenceException handleJdbcExecutionException( JdbcExecutionException jee )
    {
        if( jee.isSqlException() )
        {
            _logger.debug( "sql errorcode = " + jee.getSqlErrorCode() + " | sqlstate=" + jee.getSqlState());
            if( jee.isRowLockTimeout() )
                return new PessimisticLockException( "deadlocked or another transaction is holding row lock", jee );
            else if( jee.isMysqlDown() )
                return new DatabaseServerNotAvailableException( "cannot connect to mysql", jee );
            else
                return new PersistenceException( "generic SqlException", jee );
        }
        else
            return new PersistenceException( "internal error", jee );
    }

    private final BiMap<String, Integer> _cache2 = HashBiMap.create( 5 );
//    private final ConcurrentMap<String,Integer> _cache = new ConcurrentHashMap<String,Integer>(5);
    private final Provider<DataSource> _dsProvider;
    private final TransportDaoFactory _daoFactory;

    private final static Logger _logger = Logger.getLogger( JDBCTransportMapper.class );

}
