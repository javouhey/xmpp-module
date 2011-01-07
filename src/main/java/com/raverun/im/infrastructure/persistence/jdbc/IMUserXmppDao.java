package com.raverun.im.infrastructure.persistence.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.raverun.im.common.Transport;
import com.raverun.im.domain.IMUserSetting;
import com.raverun.im.domain.IMUserSettingFactory;
import com.raverun.im.domain.IMUserXmpp;
import com.raverun.im.domain.IMUserXmppWrapper;
import com.raverun.im.domain.impl.IMUserXmppImpl;
import com.raverun.im.infrastructure.persistence.dao.IMUserXmppDaoIF;
import com.raverun.im.infrastructure.system.PasswordCipher;
import com.raverun.shared.Constraint;
import com.raverun.shared.Constraint.NonNullArgument;
import com.raverun.shared.persistence.BaseDao;

public class IMUserXmppDao extends BaseDao implements IMUserXmppDaoIF
{
    @AssistedInject
    public IMUserXmppDao( IMUserSettingFactory settingFactory,
        IMUserSettingDaoFactory userSettingDaoFactory, PasswordCipher cipher,
        @Assisted Connection connection )
    {
        _cipher = cipher;
        _connection = connection;
        _settingFactory = settingFactory;
        _userSettingDaoFactory = userSettingDaoFactory;
    }
    
    private final void i( String message )
    {
        if( _logger.isInfoEnabled())
            _logger.info( message );
    }

    private static final String SQL_FIND_USER_FOR_USERXMPP = "SELECT userId FROM mim_user_xmpp where userXmpp=?";

    public String findUserFor( String userXmpp ) throws SQLException
    {
        if( Constraint.EmptyString.isFulfilledBy( userXmpp ) )
            throw new IllegalArgumentException( "userXmpp is empty" );

        PreparedStatement ps = null;
        ResultSet rs = null;

        try
        {
            i( "findUserFor.sql=" + SQL_FIND_USER_FOR_USERXMPP );
            ps = _connection.prepareStatement( SQL_FIND_USER_FOR_USERXMPP );
            ps.setString( 1, userXmpp );
            rs = ps.executeQuery();
            if( rs.next() )
            {
                return rs.getString( "userId" );
            }
            else
                return null;
        }
        finally
        {
            closeResultSet( rs );
            closeStatement( ps );
        }
    }

    private static final String SQL_USERXMPP_PRIMORDIAL = "SELECT xmppSeq, userId, userXmpp, dtModified, primordial FROM mim_user_xmpp where userId=? AND primordial=1";

    @Override
    public IMUserXmpp findPrimordialForUser( String user ) throws SQLException
    {
        if( Constraint.EmptyString.isFulfilledBy( user ) )
            throw new IllegalArgumentException( "user is empty" );

        PreparedStatement ps = null;
        ResultSet rs = null;

        try
        {
            i( "findPrimordialForUser.sql=" + SQL_USERXMPP_PRIMORDIAL );
            ps = _connection.prepareStatement( SQL_USERXMPP_PRIMORDIAL );
            ps.setString( 1, user );
            rs = ps.executeQuery();
            if( rs.next() )
            {
                IMUserXmppImpl.FromDatabaseBuilder builder = new IMUserXmppImpl.FromDatabaseBuilder( user, rs.getString( "userXmpp" ), rs.getBoolean( "primordial" ));

                Set<IMUserSetting> settings = findAll( user, rs.getString( "userXmpp" ) );
                for( IMUserSetting setting : settings )
                    builder.userSetting( setting );

                IMUserXmpp userXmpp = builder.build();
                userXmpp.setSequence( rs.getLong( "xmppSeq" ) );

                return userXmpp;
            }
            else
                return null;
        }
        finally
        {
            closeResultSet( rs );
            closeStatement( ps );
        }
    }

    private static final String SQL_FINDSEQBYTRANSPORT_1 = "select ";
    private static final String SQL_FINDSEQBYTRANSPORT_2 = " from mim_user_xmpp where userId=? AND userXmpp=?";
    private static final Long INVALID_SEQUENCE = -1L;
    /**
     * @return -1L if no valid sequence was found
     * @throws SQLException
     */
    public long findSequenceForTransport( String user, String userXmpp, Transport transport ) throws SQLException
    {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try
        {
            final String SQL = SQL_FINDSEQBYTRANSPORT_1 + transport.code() + SQL_FINDSEQBYTRANSPORT_2;
            i( "findSequence.sql=" + SQL );
            ps = _connection.prepareStatement( SQL );
            ps.setString( 1, user );
            ps.setString( 2, userXmpp );
            rs = ps.executeQuery();
            if( rs.next() )
            {
                Long seq = rs.getLong( transport.code() );
                if( !rs.wasNull() )
                    return seq;
            }

            return INVALID_SEQUENCE;
        }
        finally
        {
            closeResultSet( rs );
            closeStatement( ps );
        }
    }

    private static final String SQL_FIND_ONE_1 = "select b.settingSeq, b.transportSeq, b.imIdRaw,b.imId, b.imPasswd, " +
        "b.saved, b.dtModified from mim_user_xmpp a join mim_user_settings b ON a."; 

    private static final String SQL_FIND_ONE_2 = "=b.settingSeq where a.userId=? AND a.userXmpp=?";

    private final String filterTransportForFindOneSql( Transport transport )
    {
        StringBuilder builder = new StringBuilder();
        builder.append( SQL_FIND_ONE_1 );
        builder.append( transport.code() );
        builder.append( SQL_FIND_ONE_2 );
        return builder.toString();
    }

    public IMUserSetting findSingle( @Nonnull String user, @Nonnull String userXmpp, @Nonnull Transport transport ) 
        throws SQLException
    {
        if( Constraint.EmptyString.isFulfilledBy( user ) )
            throw new IllegalArgumentException( "user is empty" );

        if( Constraint.EmptyString.isFulfilledBy( userXmpp ) )
            throw new IllegalArgumentException( "userXmpp is empty" );

        NonNullArgument.check( transport, "transport" );

        IMUserSetting retval = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        final String SQL = filterTransportForFindOneSql( transport );

        try
        {
            i( "findSingle.sql=" + SQL );
            ps = _connection.prepareStatement( SQL );
            ps.setString( 1, user );
            ps.setString( 2, userXmpp );
            rs = ps.executeQuery();

            while( rs.next() )
            {
                java.sql.Timestamp sqltime = rs.getTimestamp( "dtModified" );
                IMUserSetting userSetting = _settingFactory.create( rs.getString( "imId" ), rs.getString( "imIdRaw" ), 
                    _cipher.decrypt( rs.getString( "imPasswd" ) ), new DateTime( new Date( sqltime.getTime() ) ), 
                    rs.getInt( "transportSeq" ), rs.getInt( "saved" ), user, true );
                userSetting.setSequence( rs.getLong( "settingSeq" ) );
                retval = userSetting;
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

    private static final String SQL_FINDALLFOR = "select b.settingSeq, b.transportSeq, b.imIdRaw,b.imId, b.imPasswd, b.saved, b.dtModified from " + 
        "mim_user_xmpp a join mim_user_settings b ON a.gtalk=b.settingSeq OR a.msn=b.settingSeq OR a.qq=b.settingSeq OR a.yahoo=b.settingSeq " +
        "OR a.mim=b.settingSeq where a.userId=? AND a.userXmpp=? ";

    public Set<IMUserSetting> findAll( String user, String userXmpp ) throws SQLException
    {
        if( Constraint.EmptyString.isFulfilledBy( user ) )
            throw new IllegalArgumentException( "user is empty" );

        if( Constraint.EmptyString.isFulfilledBy( userXmpp ) )
            throw new IllegalArgumentException( "userXmpp is empty" );

        Set<IMUserSetting> retval = new HashSet<IMUserSetting>(8);
        PreparedStatement ps = null;
        ResultSet rs = null;

        try
        {
            i( "findAll.sql=" + SQL_FINDALLFOR );
            ps = _connection.prepareStatement( SQL_FINDALLFOR );
            ps.setString( 1, user );
            ps.setString( 2, userXmpp );
            rs = ps.executeQuery();
            while( rs.next() )
            {
                java.sql.Timestamp sqltime = rs.getTimestamp( "dtModified" );
                IMUserSetting userSetting = _settingFactory.create( rs.getString( "imId" ), rs.getString( "imIdRaw" ), 
                    _cipher.decrypt( rs.getString( "imPasswd" ) ), new DateTime( new Date( sqltime.getTime() ) ), 
                    rs.getInt( "transportSeq" ), rs.getInt( "saved" ), user, true );
                userSetting.setSequence( rs.getLong( "settingSeq" ) );
                retval.add( userSetting );
            }
        }
        finally
        {
            closeResultSet( rs );
            closeStatement( ps );
        }

        return retval;
    }

    private static final String SQL_SAVED_PARAM = "AND saved=?";

    public Set<IMUserSetting> findAllForFilterBySaved( @Nonnull String user, @Nonnull String userXmpp, 
        @Nonnull IMUserSetting.UserSettingType savedType ) throws SQLException
    {
        if( Constraint.EmptyString.isFulfilledBy( user ) )
            throw new IllegalArgumentException( "user is empty" );

        if( Constraint.EmptyString.isFulfilledBy( userXmpp ) )
            throw new IllegalArgumentException( "userXmpp is empty" );

        NonNullArgument.check( savedType, "savedType" );

        Set<IMUserSetting> retval = new HashSet<IMUserSetting>(8);
        PreparedStatement ps = null;
        ResultSet rs = null;

        try
        {
            final String CANONICAL_SQL = SQL_FINDALLFOR + SQL_SAVED_PARAM;
            i( "findAllWithFilterSaved.sql=" + CANONICAL_SQL );
            ps = _connection.prepareStatement( CANONICAL_SQL );
            ps.setString( 1, user );
            ps.setString( 2, userXmpp );
            ps.setInt( 3, savedType.code() );
            rs = ps.executeQuery();
            while( rs.next() )
            {
                java.sql.Timestamp sqltime = rs.getTimestamp( "dtModified" );
                IMUserSetting userSetting = _settingFactory.create( rs.getString( "imId" ), rs.getString( "imIdRaw" ), 
                    _cipher.decrypt( rs.getString( "imPasswd" ) ), new DateTime( new Date( sqltime.getTime() ) ), 
                    rs.getInt( "transportSeq" ), rs.getInt( "saved" ), user, true );
                userSetting.setSequence( rs.getLong( "settingSeq" ) );
                retval.add( userSetting );
            }
        }
        finally
        {
            closeResultSet( rs );
            closeStatement( ps );
        }

        return retval;
    }

    // 20090630 added DESC
    private static final String SQL_ALLUSERXMPP = "SELECT xmppSeq, userId, userXmpp, dtModified, primordial FROM mim_user_xmpp where userId=? ORDER by dtModified DESC";

    public IMUserXmppWrapper findAllUserXmppForUser( @Nonnull String user ) throws SQLException
    {
        if( Constraint.EmptyString.isFulfilledBy( user ) )
            throw new IllegalArgumentException( "user is empty" );

        PreparedStatement ps = null;
        ResultSet rs = null;

        IMUserXmppWrapperImpl retval = new IMUserXmppWrapperImpl();

        try
        {
            i( "find.sql=" + SQL_ALLUSERXMPP );
            ps = _connection.prepareStatement( SQL_ALLUSERXMPP );
            ps.setString( 1, user );
            rs = ps.executeQuery();
            while( rs.next() )
            {
                IMUserXmppImpl.FromDatabaseBuilder builder = new IMUserXmppImpl.FromDatabaseBuilder( user, rs.getString( "userXmpp" ), rs.getBoolean( "primordial" ));

                Set<IMUserSetting> settings = findAll( user, rs.getString( "userXmpp" ) );
                for( IMUserSetting setting : settings )
                    builder.userSetting( setting );

                IMUserXmpp userXmpp = builder.build();
                userXmpp.setSequence( rs.getLong( "xmppSeq" ) );

                if( userXmpp.isPrimordial() )
                    retval.primordial = userXmpp;
                else
                    retval.others.add( userXmpp );

//                java.sql.Timestamp sqltime = rs.getTimestamp( "dtModified" );
//                new DateTime( new Date( sqltime.getTime() )                
            }
        }
        finally
        {
            closeResultSet( rs );
            closeStatement( ps );
        }

        return retval;
    }

    /*
     * Represents the actual row queried against {@code mim_user_xmpp} 
     */
    private class IMUserXmppRow
    {
        public Long mim, msn, yahoo, gtalk, qq;
        public final Long xmppSeq; 
        public final String userid, userXmpp;

        public IMUserXmppRow( String theUserid, String theUserXmpp, Long theXmppSeq, Long theMim, 
            Long theMsn, Long theYahoo, Long theGtalk, Long theQq )
        {
            userid = theUserid; userXmpp = theUserXmpp; xmppSeq = theXmppSeq;
            mim = theMim; msn = theMsn; yahoo = theYahoo; gtalk = theGtalk; qq = theQq;
        }

        public void updateTransportSequence( Transport transport, Long sequence )
        {
            if( transport == null || sequence == null )
                return;

            switch( transport )
            {
            case GTALK:
                gtalk = sequence;
                break;
            case MIM:
                mim = sequence;
                break;
            case MSN:
                msn = sequence;
                break;
            case QQ:
                qq = sequence;
                break;
            case YAHOO:
                yahoo = sequence;
                break;
            }
        }
    }

    // 20090630 removed dtModified=NOW()
    private final static String UPDATE_ROW_USER_XMPP = "UPDATE mim_user_xmpp SET mim=?, msn=?, yahoo=?, gtalk=?, " 
        + "qq=? WHERE xmppSeq=?"; 

    private final static String SELECT_ROW_USER_XMPP = 
        "SELECT xmppSeq, userId, userXmpp, mim, msn, yahoo, gtalk, qq FROM mim_user_xmpp " +
        "where xmppSeq=? FOR UPDATE";

    private final static String SELECT_ROW_USER_XMPP_ORIG = 
        "SELECT xmppSeq, userId, userXmpp, mim, msn, yahoo, gtalk, qq FROM mim_user_xmpp " +
        "where userId=? AND userXmpp=? FOR UPDATE";

    public boolean addSetting( @Nonnull String user, @Nonnull String userXmpp, 
        IMUserSetting aSetting ) throws SQLException
    {
        //PreparedStatement psSelect = _connection.prepareStatement( SELECT_ROW_USER_XMPP );
        PreparedStatement psSelect = _connection.prepareStatement( SELECT_ROW_USER_XMPP_ORIG );
        PreparedStatement psUpdate = _connection.prepareStatement( UPDATE_ROW_USER_XMPP );

        ResultSet rs = null;
        IMUserXmppRow userXmppRow = null;

//        IMUserXmppWrapper wrapper = findAllUserXmppForUser( user );
//        IMUserXmpp found = null;
//
//        if( wrapper.primordial().matchWith( userXmpp ) )
//            found = wrapper.primordial();
//        else
//        {
//            for( IMUserXmpp ux : wrapper.others() )
//            {
//                if( ux.matchWith( userXmpp ) )
//                {
//                    found = ux;
//                    break;
//                }
//            }
//        }
//
//        if( found == null )
//            return false;
//
//        _logger.debug( "target IMUserXmpp.sequence=" + found.sequence() );

        try
        {
//            psSelect.setLong( 1, found.sequence() );
            psSelect.setString( 1, user );
            psSelect.setString( 2, userXmpp );
            
            rs = psSelect.executeQuery();
            if( rs.next() )
            {
                userXmppRow = new IMUserXmppRow( rs.getString( "userId" ), userXmpp,
                    rs.getLong( "xmppSeq" ), rs.getLong( "mim" ), rs.getLong( "msn" ),rs.getLong( "yahoo" ),
                    rs.getLong( "gtalk" ),rs.getLong( "qq" ) );
            }

            if( userXmppRow == null )
                return false;

            IMUserSettingDao dao = _userSettingDaoFactory.create( _connection );
            IMUserSetting dbSetting = dao.create( aSetting );
            _logger.debug( "new IMUserSetting.sequence=" + dbSetting.sequence() );

            if( dbSetting.sequence() == null )
                throw new AssertionError( "sequence should never be null" );

            userXmppRow.updateTransportSequence( aSetting.identity().transport(), dbSetting.sequence() );

            setSafeFromNull( psUpdate, 1, userXmppRow.mim );
            setSafeFromNull( psUpdate, 2, userXmppRow.msn );
            setSafeFromNull( psUpdate, 3, userXmppRow.yahoo );
            setSafeFromNull( psUpdate, 4, userXmppRow.gtalk );
            setSafeFromNull( psUpdate, 5, userXmppRow.qq );
            psUpdate.setLong( 6, userXmppRow.xmppSeq );

            int updated = psUpdate.executeUpdate();
            _logger.debug( "#update: " + updated );
            return( updated == 1 );
        }
        finally
        {
            closeResultSet( rs );
            closeStatement( psSelect );
        }
    }

    private void setSafeFromNull( PreparedStatement ps, int columnIndex, Long value ) throws SQLException
    {
        /*
         * Result.getLong( key ) will return 0 if the column contains null
         */
        if( value == 0 )
            ps.setNull( columnIndex, Types.BIGINT );
        else
            ps.setLong( columnIndex, value );
    }

    private static final String SQL_CREATE = "INSERT INTO mim_user_xmpp (userId, userXmpp, mim, msn, yahoo, gtalk, qq, dtModified, primordial)" + 
        " VALUES ( ?, ?, ?, ?, ?, ?, ?, NOW(), ? )";

    /**
     * This method will create a row in table {@code mim_user_xmpp}
     *
     * @param imUserXmpp - non null value and MUST satisfy condition {@code IMUserXmpp#isNew() == true}
     * @return the auto-generated PK if successful or null otherwise
     * @throws SQLException
     */
    public Long create( @Nonnull IMUserXmpp imUserXmpp ) throws SQLException
    {
        if( imUserXmpp == null || imUserXmpp.isNew() == false  )
            throw new IllegalArgumentException( "imUserXmpp cannot be null and must be virgin" );

        IMUserSettingDao dao = _userSettingDaoFactory.create( _connection );

        List<IMUserSetting> transports = imUserXmpp.transports();
        List<Long> transportSequences = new ArrayList<Long>(5);
        for( int i=0; i<5; i++)
            transportSequences.add( i, null );

        int i = 0;
        for( IMUserSetting aSetting : transports )
        {
            if( aSetting != null )
            {
                IMUserSetting newSetting = dao.create( aSetting );
                // TODO if newSetting is null, should throw exception and abort immediately
                transportSequences.set( i, newSetting.sequence() ); // TODO throws NPE potential
            }
            else
                transportSequences.set( i, null );

            i++;
        }

        PreparedStatement ps = _connection.prepareStatement( SQL_CREATE, Statement.RETURN_GENERATED_KEYS );
        ResultSet rs = null;
        Long retval = null;

        try
        {
            
            ps.setString( 1, imUserXmpp.user() );
            ps.setString( 2, imUserXmpp.userXMPP() );

            i = 3;
            for( Long seq : transportSequences )
            {
                if( seq == null )
                    ps.setNull( i, Types.NUMERIC );
                else
                    ps.setLong( i, seq );

                i++;
            }

            ps.setInt( 8, (imUserXmpp.isPrimordial())?1:0 );

            int created = ps.executeUpdate();
            i( "#create: " + created );

            rs = ps.getGeneratedKeys();
            if( rs.next() )
                retval = rs.getLong( 1 );
            else
                _logger.error( "Could not obtain generated sequence for a new element in 'mim_user_xmpp'" );

            return retval;
        }
        finally
        {
            closeResultSet( rs );
            closeStatement( ps );
        }
    }

    public boolean remove( @Nonnull IMUserXmpp imUserXmpp ) throws SQLException
    {
        if( imUserXmpp == null || imUserXmpp.isNew() )
            return false;

        return false;
    }

    private class IMUserXmppWrapperImpl implements IMUserXmppWrapper
    {
        public List<IMUserXmpp> others()
        {
            return others;
        }

        public IMUserXmpp primordial()
        {
            return primordial;
        }

        public IMUserXmpp primordial;
        public List<IMUserXmpp> others = new ArrayList<IMUserXmpp>(4);
    }
    
    private final Connection _connection;
    private final PasswordCipher _cipher;
    private final IMUserSettingFactory _settingFactory;
    private final IMUserSettingDaoFactory _userSettingDaoFactory;

    private static final Logger _logger = Logger.getLogger( IMUserXmppDao.class );

}
