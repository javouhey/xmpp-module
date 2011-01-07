package com.raverun.im.infrastructure.persistence.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.raverun.shared.persistence.BaseDao;

public class TransportDao extends BaseDao
{
    @AssistedInject
    public TransportDao( @Assisted Connection connection )
    {
        _connection = connection;
    }
    
    private final void i( String message )
    {
        if( _logger.isInfoEnabled())
            _logger.info( message );
    }

    private static final String SQL_FIND_BY_KEY = "SELECT transportSeq FROM mim_transports where transportKey=? LIMIT 1";

    public List<Integer> find( final String transport ) throws SQLException
    {
        List<Integer> retval = new ArrayList<Integer>(1);

        i( "#find.sql=" + SQL_FIND_BY_KEY );
        PreparedStatement ps = null;
        ResultSet rs = null;

        try
        {
            ps = _connection.prepareStatement( SQL_FIND_BY_KEY );
            ps.setString( 1, transport );

            rs = ps.executeQuery();
            while( rs.next() )
            {
                retval.add( rs.getInt( 1 ) );
                break;
            }
        }
        finally
        {
            closeResultSet( rs );
            closeStatement( ps );
        }

        return retval;
    }

    private static final String SQL_FIND_BY_SEQUENCE = "SELECT transportKey FROM mim_transports where transportSeq=? LIMIT 1";

    public List<String> find( final Integer sequence ) throws SQLException
    {
        List<String> retval = new ArrayList<String>(1);

        i( "#find.sql=" + SQL_FIND_BY_SEQUENCE );
        PreparedStatement ps = null;
        ResultSet rs = null;

        try
        {
            ps = _connection.prepareStatement( SQL_FIND_BY_SEQUENCE );
            ps.setInt( 1, sequence );

            rs = ps.executeQuery();
            while( rs.next() )
            {
                retval.add( rs.getString( 1 ) );
                break;
            }
        }
        finally
        {
            closeResultSet( rs );
            closeStatement( ps );
        }

        return retval;
    }

    private final Connection _connection;
    
    private static final Logger _logger = Logger.getLogger( UserDao.class );
}
