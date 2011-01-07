package com.raverun.im.infrastructure.persistence.jdbc;

import java.sql.Connection;

public interface IMPresenceDaoFactory
{
    IMPresenceDao create( Connection connection );
}
