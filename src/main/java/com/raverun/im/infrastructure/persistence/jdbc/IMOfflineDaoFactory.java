package com.raverun.im.infrastructure.persistence.jdbc;

import java.sql.Connection;

import com.raverun.im.infrastructure.persistence.dao.IMOfflineDaoIF;

public interface IMOfflineDaoFactory
{
    IMOfflineDaoIF create( Connection connection );
}
