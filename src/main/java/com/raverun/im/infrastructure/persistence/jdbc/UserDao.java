package com.raverun.im.infrastructure.persistence.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.raverun.im.infrastructure.persistence.UserService.User;
import com.raverun.im.infrastructure.persistence.dao.UserDaoIF;
import com.raverun.im.infrastructure.persistence.dao.UserDaoIF.UserStatus;
import com.raverun.shared.Common;
import com.raverun.shared.Constraint;
import com.raverun.shared.persistence.BaseDao;

public class UserDao extends BaseDao implements UserDaoIF
{
    @AssistedInject
    public UserDao( @Assisted Connection connection )
    {
        this.connection = connection;
    }

    private static final String SQL_INSERT_1 = "INSERT INTO mim_user (userId, status, dtCreated) VALUES (?, ?, NOW())";

    /**
     * This method will create a row in table {@code imdb.mim_user}
     *
     * @param call - non null value
     * @return true if it was created successfully
     * @throws SQLException
     * @throws IllegalArgumentException if parameters are invalid
     */
    @Override
    public boolean create( String userid ) throws SQLException
    {
        if( Constraint.EmptyString.isFulfilledBy( userid ) )
            throw new IllegalArgumentException( "userid is empty" );

        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try
        {
            ps = connection.prepareStatement( SQL_INSERT_1, Statement.RETURN_GENERATED_KEYS );
            ps.setString( 1, userid );
            ps.setString( 2, UserStatus.ACTIVE.toString().toLowerCase() );

            int created = ps.executeUpdate();
            if( created != 1 )
                return false;

            i( "inserted record for " + userid );
            return true;
        }
        finally
        {
            closeResultSet( rs );
            closeStatement( ps );
        }
    }

    private static final String SQL_REMOVE = "DELETE FROM mim_user where userId=?";

    @Override
    public boolean remove( String userid ) throws SQLException
    {
        if( Constraint.EmptyString.isFulfilledBy( userid ) )
            throw new IllegalArgumentException( "userid is empty" );

        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try
        {
            ps = connection.prepareStatement( SQL_REMOVE );
            ps.setString( 1, userid );

            int deleted = ps.executeUpdate();
            if( deleted != 1 )
                return false;

            i( "deleted record for " + userid );
            return true;
        }
        finally
        {
            closeResultSet( rs );
            closeStatement( ps );
        }
    }

    private static final String SQL_COUNT = "SELECT COUNT(*) AS cnt from mim_user";

    @Override
    public int count() throws SQLException
    {
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try
        {
            ps = connection.prepareStatement( SQL_COUNT );
            rs = ps.executeQuery();
            rs.first();
            return rs.getInt( "cnt" );
        }
        finally
        {
            closeResultSet( rs );
            closeStatement( ps );
        }
    }


    private static final String SQL_FIND_BY_ID = "SELECT userId, status, dtCreated FROM mim_user where userId=? LIMIT 1";

    @Override
    public List<User> find( final String userid ) throws SQLException
    {
        List<User> retval = new ArrayList<User>(1);

        i( "#find.sql=" + SQL_FIND_BY_ID );
        PreparedStatement ps = null;
        ResultSet rs = null;

        try
        {
            ps = connection.prepareStatement( SQL_FIND_BY_ID );
            ps.setString( 1, userid );

            rs = ps.executeQuery();
            while( rs.next() )
            {
                final String status = rs.getString( "status" );

                java.sql.Timestamp sqltime = rs.getTimestamp(3);
                final String[] creationDateHack = new String[1];
                if( !rs.wasNull() )
                {
                    DateTime javaTime = new DateTime( new Date( sqltime.getTime() ) );
                    i( "after Timestamp.getTime" );
                //2009-04-08T21:57:23+00:00
                    DateTimeFormatter formatter1 = DateTimeFormat.forPattern( "yyyy-MM-dd" );
                    DateTimeFormatter formatter2 = DateTimeFormat.forPattern( "HH:mm:ss" );
                    creationDateHack[ 0 ] = javaTime.toString( formatter1 ) + "T" + javaTime.toString( formatter2 );
                    i( "after joda format" );
                }
                else
                {
                    creationDateHack[ 0 ] = Common.EMPTY_STRING;
                }
                User u = new User() 
                {
                    public String dateCreated() { return creationDateHack[ 0 ]; }
                    public String userid() { return userid; }
                    public boolean isActive() 
                    {
                        return (status.equalsIgnoreCase( UserStatus.ACTIVE.toString() ));
                    }
                }; 

                retval.add( u );
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

    private static final String SQL_UPDATE_SQL = "UPDATE mim_user SET status=? where userId=?";

    @Override
    public int updateStatus( String userid, UserStatus newStatus ) throws SQLException
    {
        if( Constraint.EmptyString.isFulfilledBy( userid ) )
            throw new IllegalArgumentException( "userid is empty" );

        Constraint.NonNullArgument.check( newStatus, "newStatus" );

        PreparedStatement ps = null;
        ResultSet rs = null;

        try
        {
            i( "[updateStatus] sql=" + SQL_UPDATE_SQL );
            ps = connection.prepareStatement( SQL_UPDATE_SQL );

            ps.setString( 1, newStatus.toString().toLowerCase() );
            ps.setString( 2, userid );

            return ps.executeUpdate();
        }
        finally
        {
            closeResultSet( rs );
            closeStatement( ps );
        }
    }

    private final void i( String message )
    {
        if( _logger.isInfoEnabled())
            _logger.info( message );
    }

    private final Connection connection;
    
    private static final Logger _logger = Logger.getLogger( UserDao.class );
}
