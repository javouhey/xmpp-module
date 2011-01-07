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
import com.raverun.im.domain.IMMessageChat;
import com.raverun.im.domain.impl.IMMessageChatImpl;
import com.raverun.im.infrastructure.persistence.TransportMapper;
import com.raverun.im.infrastructure.persistence.dao.IMMessageChatDaoIF;
import com.raverun.shared.Constraint;
import com.raverun.shared.persistence.BaseDao;

public class IMMessageChatDao extends BaseDao implements IMMessageChatDaoIF
{
    private static final String SQL_INSERT = "insert into mim_msg_chat (userId, receiver, sender, transportSeq, message, dtCreated) VALUES( ?,?,?,?,?,NOW() )";
    private static final long NO_AUTOGEN_SEQUENCE = -99;

    @Override
    public long create( IMMessageChat chat ) throws SQLException
    {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try
        {
            i( "create.sql=" + SQL_INSERT );
            ps = _connection.prepareStatement( SQL_INSERT, Statement.RETURN_GENERATED_KEYS );
            ps.setString( 1, chat.user() );
            ps.setString( 2, chat.receiver() );
            ps.setString( 3, chat.sender() );
            ps.setLong( 4, _transportMapper.sequenceFor( chat.transport() ) );
            ps.setString( 5, chat.message() );
            int created = ps.executeUpdate();
            if( created != 1 )
                return NO_AUTOGEN_SEQUENCE;

            rs = ps.getGeneratedKeys();
            if( rs.next() )
                return rs.getLong( 1 );
            else
            {
                _logger.error( "Could not obtain generated sequence for a new element in 'mim_msg_chat'" );
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

    private static final String SQL_DELETE = "DELETE FROM mim_msg_chat WHERE userId=?";

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

    private static final String SQL_FINDALLFOR = "SELECT chatSeq, userId, receiver, sender, transportSeq, message, dtCreated FROM mim_msg_chat where userId=?";

    @Override
    public List<IMMessageChat> getAllForUser( String user )
        throws SQLException
    {
        if( Constraint.EmptyString.isFulfilledBy( user ) )
            throw new IllegalArgumentException( "user is empty" );

        List<IMMessageChat> retval = new LinkedList<IMMessageChat>();
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

                IMMessageChatImpl.FromDatabaseBuilder chatBuilder = new IMMessageChatImpl.FromDatabaseBuilder(
                    rs.getString( "userId" ), rs.getString( "sender" ), rs.getString( "receiver" ),
                    _transportMapper.transportFor( rs.getInt( "transportSeq" ) ), rs.getString( "message" ) );
            
                retval.add( chatBuilder.build() );
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
    public IMMessageChatDao( TransportMapper transportMapper,
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
    private static final Logger _logger = Logger.getLogger( IMMessageChatDao.class );
}
