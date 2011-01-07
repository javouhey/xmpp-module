package com.raverun.im.infrastructure.persistence.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.raverun.im.domain.IMUserSetting;
import com.raverun.im.domain.IMUserSettingFactory;
import com.raverun.im.infrastructure.persistence.dao.IMUserSettingDaoIF;
import com.raverun.im.infrastructure.system.PasswordCipher;
import com.raverun.shared.Constraint;
import com.raverun.shared.Constraint.NonNullArgument;
import com.raverun.shared.persistence.BaseDao;

public class IMUserSettingDao extends BaseDao implements IMUserSettingDaoIF
{
    @AssistedInject
    public IMUserSettingDao( IMUserSettingFactory userSettingFactory,
        PasswordCipher cipher, @Assisted Connection connection )
    {
        _userSettingFactory = userSettingFactory;
        _connection = connection;
        _cipher = cipher;
    }
    
    private final void i( String message )
    {
        if( _logger.isInfoEnabled())
            _logger.info( message );
    }

    private static final String SQL_DELETE = "DELETE FROM mim_user_settings WHERE settingSeq=?";

    /**
     * @param pk - should never be -1L
     * @return number of rows deleted
     * @throws SQLException
     */
    @Override
    public int delete( @Nonnull Long pk ) throws SQLException
    {
        if( pk == null || pk <= 0L )
        {
            i( "pk is either null or is -1L. Not allowed for deletion" );
            return 0;
        }

        int deleted = 0;
        PreparedStatement ps = null;

        try
        {
            i( "delete.sql=" + SQL_DELETE );
            ps = _connection.prepareStatement( SQL_DELETE );
            ps.setLong( 1, pk );
            deleted = ps.executeUpdate();
            return deleted;
        }
        finally
        {
            closeStatement( ps );
        }
    }

    private static final String SQL_UPDATE1 = "update mim_user_settings set saved=? , dtModified=NOW()";
    private static final String SQL_UPDATE_PASSWORD = ", imPasswd=?";
    private static final String SQL_UPDATE_WHERE = " where settingSeq=?";

    /**
     * Only 2 columns can be updated
     * <ul>
     * <li>password
     * <li>saved
     * </ul>
     *
     * @param password - nullable
     * @param saved - not null
     * @return number of rows deleted
     * @throws SQLException
     */
    @Override
    public int update( @Nonnull Long pk, @Nullable String password, @Nonnull IMUserSetting.UserSettingType saved )
        throws SQLException
    {
        if( pk == null || pk <= 0L )
        {
            i( "pk is either null or is -1L. Not allowed for deletion" );
            return 0;
        }
        NonNullArgument.check( saved, "saved" );
        boolean passwordNull = Constraint.EmptyString.isFulfilledBy( password );

        StringBuilder builder = new StringBuilder();
        builder.append( SQL_UPDATE1 );
        if( !passwordNull )
            builder.append( SQL_UPDATE_PASSWORD );

        builder.append( SQL_UPDATE_WHERE );

        final String SQL = builder.toString();

        PreparedStatement ps = null;
        ResultSet rs = null;

        try
        {
            i( "update.sql=" + SQL );
            ps = _connection.prepareStatement( SQL );

            int parameterIndex = 1;
            ps.setInt( parameterIndex, saved.code() );

            if( !passwordNull )
            {
                ++parameterIndex;
                ps.setString( parameterIndex, _cipher.encrypt( password ) );
            }

            ++parameterIndex;
            ps.setLong( parameterIndex, pk );

            return ps.executeUpdate();
        }
        finally
        {
            closeResultSet( rs );
            closeStatement( ps );
        }
    }

    private static final String SQL_INSERT = "insert into mim_user_settings( userId, transportSeq, imIdRaw, imId, imPasswd, saved, dtModified ) VALUES( ?,?,?,?,?,?, NOW() )";

    /**
     * @param aSetting - non null
     * @return null if there was an error, otherwise return the generated sequence number
     * @throws SQLException
     */
    public IMUserSetting create( @Nonnull IMUserSetting aSetting ) throws SQLException
    {
        NonNullArgument.check( aSetting, "aSetting" );

        if( !aSetting.isNew() )
            throw new IllegalArgumentException( "call must be a virgin" );

        PreparedStatement ps = null;
        ResultSet rs = null;

        try
        {
            i( "insert.sql=" + SQL_INSERT );
            ps = _connection.prepareStatement( SQL_INSERT, Statement.RETURN_GENERATED_KEYS );
            ps.setString( 1, aSetting.userId() );
            ps.setLong( 2, aSetting.identity().transportDbSequence() );
            ps.setString( 3, aSetting.identity().imIdRaw() );
            ps.setString( 4, aSetting.identity().imId() );
            ps.setString( 5, _cipher.encrypt( aSetting.imPassword() ) );
            ps.setInt( 6, aSetting.saved().code() );
            int created = ps.executeUpdate();
            if( created != 1 )
                return null;

            rs = ps.getGeneratedKeys();
            if( rs.next() )
            {
                IMUserSetting retval = _userSettingFactory.create( aSetting.identity().imId(), 
                    aSetting.identity().imIdRaw(), aSetting.imPassword(), FAKE_DATETIME_PLACEHOLDER, 
                    aSetting.identity().transportDbSequence(), aSetting.saved().code(), 
                    aSetting.userId(), true );
                retval.setSequence( rs.getLong( 1 ) );
                return retval; 
            }
            else
            {
                _logger.error( "Could not obtain generated sequence for a new element in 'mim_user_settings'" );
                return null;
            }
        }
        finally
        {
            closeResultSet( rs );
            closeStatement( ps );
        }
    }

    private final DateTime FAKE_DATETIME_PLACEHOLDER = new DateTime( new Date() );

    private final PasswordCipher _cipher;
    private final Connection _connection;
    private final IMUserSettingFactory _userSettingFactory;
    private static final Logger _logger = Logger.getLogger( IMUserSettingDao.class );

}
