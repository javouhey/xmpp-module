package com.raverun.im.infrastructure.persistence.jdbc;

import java.sql.Connection;

public interface UserDaoFactory
{
    UserDao create( Connection connection );
}
