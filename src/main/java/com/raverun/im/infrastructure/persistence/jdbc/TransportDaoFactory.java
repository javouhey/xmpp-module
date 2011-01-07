package com.raverun.im.infrastructure.persistence.jdbc;

import java.sql.Connection;

public interface TransportDaoFactory
{
    TransportDao create( Connection connection );
}
