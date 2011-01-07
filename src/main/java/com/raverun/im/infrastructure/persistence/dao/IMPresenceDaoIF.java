package com.raverun.im.infrastructure.persistence.dao;

import java.sql.SQLException;
import java.util.List;

import com.raverun.im.domain.IMPresence;

public interface IMPresenceDaoIF
{
    long create( IMPresence presence ) throws SQLException;

    int purgeAllForUser( String user ) throws SQLException;

    List<IMPresence> getAllForUser( String user ) throws SQLException;
}
