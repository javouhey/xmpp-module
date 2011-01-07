package com.raverun.im.infrastructure.persistence.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.raverun.im.domain.IMLoginCache;
import com.raverun.im.infrastructure.persistence.dao.CacheOfLoginsDaoIF;
import com.raverun.shared.Constraint;
import com.raverun.shared.persistence.BaseDao;

public class CacheOfLoginsDao extends BaseDao implements CacheOfLoginsDaoIF
{
    private static final String SQL_DELETE_SINGLE = "delete from mim_login_cache where userId=?";

    @Override
    public int delete( String user ) throws SQLException
    {
        int deleted = 0;
        PreparedStatement ps = null;

        try
        {
            ps = _connection.prepareStatement( SQL_DELETE_SINGLE );
            ps.setString( 1, user );
            deleted = ps.executeUpdate();
            return deleted;
        }
        finally
        {
            closeStatement( ps );
        }
    }

    private static final int MAX_SIZE_DEVICE = 50;
    private static final String SQL_INSERT = "insert into mim_login_cache (userId, device, dtCreated) VALUES( ?,?,NOW() )";

    @Override
    public void create( IMLoginCache login ) throws SQLException
    {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try
        {
            ps = _connection.prepareStatement( SQL_INSERT );
            ps.setString( 1, login.userId() );
            ps.setString( 2, trimDevice( login.device() ) );
            ps.executeUpdate();
        }
        catch( SQLException sqle )
        {
//            _logger.error( "Failed to insert " + login.toString(),  sqle );
            throw sqle;
        }
        finally
        {
            closeResultSet( rs );
            closeStatement( ps );
        }
    }

    private static final String UNKNOWN_DEVICE = "unknown device";

    private String trimDevice( String device )
    {
        if( Constraint.EmptyString.isFulfilledBy( device ) )
            return UNKNOWN_DEVICE;

        String theDevice = device.trim();
        if( theDevice.length() > MAX_SIZE_DEVICE )
            return theDevice.substring( 0, MAX_SIZE_DEVICE );
        else
            return theDevice;
    }

    private static final String SQL_FIND = "select device from mim_login_cache where userId=?";

    @Override
    public String findDeviceFor( String user ) throws SQLException
    {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try
        {
            ps = _connection.prepareStatement( SQL_FIND );
            ps.setString( 1, user );
            rs = ps.executeQuery();
            if( rs.next() )
                return rs.getString( "device" );
            else
                return null;
        }
        catch( SQLException sqle )
        {
//            _logger.error( "Failed to select for " + user,  sqle );
            throw sqle;
        }
        finally
        {
            closeResultSet( rs );
            closeStatement( ps );
        }
    }

    private static final String SQL_DELETE_ALL = "DELETE FROM mim_login_cache";

    @Override
    public int purgeAll() throws SQLException
    {
        int deleted = 0;
        PreparedStatement ps = null;

        try
        {
            ps = _connection.prepareStatement( SQL_DELETE_ALL );
            deleted = ps.executeUpdate();
            return deleted;
        }
        finally
        {
            closeStatement( ps );
        }
    }

    @AssistedInject
    public CacheOfLoginsDao( @Assisted Connection connection )
    {
        _connection = connection;
    }

    @SuppressWarnings("unused")
    private final void i( String message )
    {
        if( _logger.isInfoEnabled())
            _logger.info( message );
    }

    private final Connection _connection;
    private static final Logger _logger = Logger.getLogger( CacheOfLoginsDao.class );
}
