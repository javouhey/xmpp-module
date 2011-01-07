package com.raverun.im.infrastructure.persistence.jdbc;

import java.sql.Connection;

public interface IMUserSettingDaoFactory
{
    IMUserSettingDao create( Connection connection );
}
