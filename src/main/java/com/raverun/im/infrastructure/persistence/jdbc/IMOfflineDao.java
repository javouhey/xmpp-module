package com.raverun.im.infrastructure.persistence.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.raverun.im.common.Transport;
import com.raverun.im.domain.IMIdentity;
import com.raverun.im.domain.IMOffline;
import com.raverun.im.domain.impl.IMOfflineImpl;
import com.raverun.im.infrastructure.persistence.TransportMapper;
import com.raverun.im.infrastructure.persistence.dao.IMOfflineDaoIF;
import com.raverun.shared.Constraint;
import com.raverun.shared.persistence.BaseDao;

public class IMOfflineDao extends BaseDao implements IMOfflineDaoIF
{
    private final static String SQL_INSERT = "INSERT INTO mim_offline (userId, transportSeq, imId, dtCreated) VALUES ( ?, ?, ?, NOW() )";

    @Override
    public long create( IMOffline offline ) throws SQLException
    {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try
        {
            ps = _connection.prepareStatement( SQL_INSERT, Statement.RETURN_GENERATED_KEYS );
            ps.setString( 1, offline.user() );
            ps.setLong( 2, _transportMapper.sequenceFor( offline.receiver().transport() ) );
            ps.setString( 3, offline.receiver().imId() );

            int created = ps.executeUpdate();
            if( created != 1 )
                return -1;

            rs = ps.getGeneratedKeys();
            if( rs.next() )
                return rs.getLong( 1 );
            else
            {
                _logger.error( "Could not obtain generated sequence for a new element in 'mim_offline'" );
                return -1;
            }
        }
        catch( SQLException sqle )
        {
            throw sqle;
        }
        finally
        {
            closeResultSet( rs );
            closeStatement( ps );
        }
    }

    private static final String SQL_FINDALLFOR = "SELECT offlineSeq, userId, transportSeq, imId, dtCreated FROM mim_offline where userId=? order by dtCreated DESC, offlineSeq DESC";

    @Override
    public List<IMOffline> getAllForUser( final String user ) throws SQLException
    {
        if( Constraint.EmptyString.isFulfilledBy( user ) )
            throw new IllegalArgumentException( "user is empty" );

        List<IMOffline> retval = new LinkedList<IMOffline>();
        PreparedStatement ps = null;
        ResultSet rs = null;

        try
        {
            ps = _connection.prepareStatement( SQL_FINDALLFOR );
            ps.setString( 1, user );
            rs = ps.executeQuery();
            while( rs.next() )
            {
                //java.sql.Timestamp sqltime = rs.getTimestamp( "dtCreated" );

                final String imId = rs.getString( "imId" );
                final long transportSeq = rs.getLong( "transportSeq" );
                final Transport transport = _transportMapper.transportFor( (int)transportSeq );

                IMOfflineImpl.Builder builder = new IMOfflineImpl.Builder(
                    rs.getString( "userId" ), 
                    new IMIdentity() {
                        public String imId() { return imId; }
                        public String imIdRaw() { return imId; }
                        public Transport transport() { return transport; }
                        public Integer transportDbSequence() { return (int)transportSeq; }
                    }
                );
                retval.add( builder.build() );
            }
        }
        finally
        {
            closeResultSet( rs );
            closeStatement( ps );
        }

        return retval;
    }

    private static final String SQL_DELETE = "DELETE FROM mim_offline WHERE userId=?";

    @Override
    public int purgeAllForUser( String user ) throws SQLException
    {
        int deleted = 0;
        PreparedStatement ps = null;

        try
        {
            ps = _connection.prepareStatement( SQL_DELETE );
            ps.setString( 1, user );
            deleted = ps.executeUpdate();
            return deleted;
        }
        finally
        {
            closeStatement( ps );
        }
    }

    @AssistedInject
    public IMOfflineDao( TransportMapper transportMapper,
        @Assisted Connection connection )
    {
        _connection = connection;
        _transportMapper = transportMapper;
    }

    @SuppressWarnings("unused")
    private final void i( String message )
    {
        if( _logger.isInfoEnabled())
            _logger.info( message );
    }

    private final Connection _connection;
    private final TransportMapper _transportMapper;

    private static final Logger _logger = Logger.getLogger( IMOfflineDao.class );
}
