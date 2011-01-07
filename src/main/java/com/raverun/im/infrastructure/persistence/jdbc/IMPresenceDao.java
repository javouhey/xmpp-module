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
import com.raverun.im.domain.IMPresence;
import com.raverun.im.domain.IMPresence.PresenceChangeType;
import com.raverun.im.domain.impl.IMPresenceImpl;
import com.raverun.im.infrastructure.persistence.TransportMapper;
import com.raverun.im.infrastructure.persistence.dao.IMPresenceDaoIF;
import com.raverun.im.infrastructure.xmpp.smack.PresenceUtilityIF.MyMode;
import com.raverun.shared.Constraint;
import com.raverun.shared.persistence.BaseDao;

public class IMPresenceDao extends BaseDao implements IMPresenceDaoIF
{ 
    private static final String SQL_INSERT = "insert into mim_presence (userId, transportSeq, imId, buddyId, mode, status, type, dtCreated) VALUES( ?,?,?,?,?,?,?,NOW() )";
    private static final long NO_AUTOGEN_SEQUENCE = -99;

    @Override
    public long create( IMPresence presence ) throws SQLException
    {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try
        {
            i( "create.sql=" + SQL_INSERT );
            ps = _connection.prepareStatement( SQL_INSERT, Statement.RETURN_GENERATED_KEYS );
            ps.setString( 1, presence.user() );
            ps.setLong( 2, _transportMapper.sequenceFor( presence.receiver().transport() ) );
            ps.setString( 3, presence.receiver().imId() );
            ps.setString( 4, presence.sender() );
            ps.setInt(  5, presence.mode().code() );
            ps.setString( 6, presence.status() );
            ps.setInt( 7, presence.type().code() );
            int created = ps.executeUpdate();
            if( created != 1 )
                return NO_AUTOGEN_SEQUENCE;

            rs = ps.getGeneratedKeys();
            if( rs.next() )
                return rs.getLong( 1 );
            else
            {
                _logger.error( "Could not obtain generated sequence for a new element in 'mim_presence'" );
                return NO_AUTOGEN_SEQUENCE;
            }
        }
        catch( SQLException sqle )
        {
            _logger.error( "", sqle );
            throw sqle;
        }
        finally
        {
            closeResultSet( rs );
            closeStatement( ps );
        }
    }

    private static final String SQL_DELETE = "DELETE FROM mim_presence WHERE userId=?";

    @Override
    public int purgeAllForUser( String user ) throws SQLException
    {
        int deleted = 0;
        PreparedStatement ps = null;

        try
        {
            i( "purgeAllForUser.sql=" + SQL_DELETE );
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

    //////////////////////////////////////////////////////////////////////////////////////////////////////
    // Previously we tried to sort by dtCreated but JDBC does not break the tie when 2 records share the
    // same dtCreated correctly. I expected the record with the higher presenceSeq to be returned. But
    // it does not do that. That's why I am now sorting based on dtCreated, then presenceSeq.
    //
    //private static final String SQL_FINDALLFOR = "SELECT presenceSeq, userId, transportSeq, imId, buddyId, mode, status, type, dtCreated FROM mim_presence where userId=? order by dtCreated DESC";

    private static final String SQL_FINDALLFOR = "SELECT presenceSeq, userId, transportSeq, imId, buddyId, mode, status, type, dtCreated FROM mim_presence where userId=? order by dtCreated DESC, presenceSeq DESC";

    @Override
    public List<IMPresence> getAllForUser( String user )
        throws SQLException
    {
        if( Constraint.EmptyString.isFulfilledBy( user ) )
            throw new IllegalArgumentException( "user is empty" );

        List<IMPresence> retval = new LinkedList<IMPresence>();
        PreparedStatement ps = null;
        ResultSet rs = null;

        try
        {
            i( "getAllForUser.sql=" + SQL_FINDALLFOR );
            ps = _connection.prepareStatement( SQL_FINDALLFOR );
            ps.setString( 1, user );
            rs = ps.executeQuery();
            while( rs.next() )
            {
                java.sql.Timestamp sqltime = rs.getTimestamp( "dtCreated" );

                final String imId = rs.getString( "imId" );
                final long transportSeq = rs.getLong( "transportSeq" );
                final String buddy = rs.getString( "buddyId" );
                final Transport transport = _transportMapper.transportFor( (int)transportSeq );

                IMPresenceImpl.FromDatabaseBuilder builder = new IMPresenceImpl.FromDatabaseBuilder(
                    rs.getString( "userId" ), 
                    new IMIdentity() {
                        public String imId() { return imId; }
                        public String imIdRaw() { return imId; }
                        public Transport transport() { return transport; }
                        public Integer transportDbSequence() { return (int)transportSeq; }
                    }, 
                    buddy, 
                    PresenceChangeType.deref( rs.getInt( "type" ) ),
                    MyMode.deref( rs.getInt( "mode" ) ), 
                    rs.getString( "status" ) );

                i( "adding presence " + buddy + " " + rs.getInt( "mode" ) );
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

    @AssistedInject
    public IMPresenceDao( TransportMapper transportMapper,
        @Assisted Connection connection )
    {
        _connection = connection;
        _transportMapper = transportMapper;
    }

    private final void i( String message )
    {
        if( _logger.isInfoEnabled())
            _logger.info( message );
    }

    private final TransportMapper _transportMapper;
    private final Connection _connection;
    private static final Logger _logger = Logger.getLogger( IMPresenceDao.class );
}
