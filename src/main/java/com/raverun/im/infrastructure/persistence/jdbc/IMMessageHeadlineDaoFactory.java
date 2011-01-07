package com.raverun.im.infrastructure.persistence.jdbc;

import java.sql.Connection;

public interface IMMessageHeadlineDaoFactory
{
    IMMessageHeadlineDao create( Connection connection );
}
