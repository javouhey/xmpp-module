package com.raverun.im.infrastructure.persistence.jdbc;

import java.sql.Connection;

public interface IMMessageChatDaoFactory
{
    IMMessageChatDao create( Connection connection );
}
