package com.raverun.im.infrastructure.persistence.jdbc;

import java.sql.Connection;

public interface IMUserXmppDaoFactory
{
    IMUserXmppDao create( Connection connection );
}
