package com.raverun.im.infrastructure.persistence.dao;

import java.sql.SQLException;
import java.util.List;

import com.raverun.im.domain.IMOffline;

public interface IMOfflineDaoIF
{
    long create( IMOffline offline ) throws SQLException;

    int purgeAllForUser( String user ) throws SQLException;

    List<IMOffline> getAllForUser( String user ) throws SQLException;
}
