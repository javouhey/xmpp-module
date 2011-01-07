package com.raverun.im.infrastructure.persistence.jdbc;

import java.sql.Connection;

public interface IMSubscriptionRequestDaoFactory
{
    IMSubscriptionRequestDao create( Connection connection );
}
