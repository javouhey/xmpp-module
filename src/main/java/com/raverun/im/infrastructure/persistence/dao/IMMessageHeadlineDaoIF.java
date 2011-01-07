package com.raverun.im.infrastructure.persistence.dao;

import java.sql.SQLException;
import java.util.List;

import com.raverun.im.domain.IMMessageHeadline;

public interface IMMessageHeadlineDaoIF
{
    public long create( IMMessageHeadline headline ) throws SQLException;

    public int purgeAllForUser( String user ) throws SQLException;

    public List<IMMessageHeadline> getAllForUser( String user ) throws SQLException;

}
