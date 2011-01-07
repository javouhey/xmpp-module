package com.raverun.im.infrastructure.persistence.dao;

import java.sql.SQLException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.raverun.im.domain.IMUserSetting;

public interface IMUserSettingDaoIF
{
    /**
     * @param pk - primary key for {@code IMUserSetting}
     * @return number of rows deleted
     * @throws SQLException
     */
    int delete( @Nonnull Long pk )
        throws SQLException;

    /**
     * @param aSetting - non null
     * @return null if there was an error, otherwise return the generated sequence number
     * @throws SQLException
     */
    IMUserSetting create( @Nonnull IMUserSetting aSetting )
        throws SQLException;

    /**
     * Only 2 columns can be updated
     * <ul>
     * <li>password
     * <li>saved
     * </ul>
     *
     * @param password - nullable
     * @param saved - no null
     * @return number of rows deleted
     * @throws SQLException
     */
    int update( @Nonnull Long pk, @Nullable String password, @Nonnull IMUserSetting.UserSettingType saved )
        throws SQLException;
}
