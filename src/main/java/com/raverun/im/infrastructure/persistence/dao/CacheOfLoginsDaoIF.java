package com.raverun.im.infrastructure.persistence.dao;

import java.sql.SQLException;

import javax.annotation.Nullable;

import com.raverun.im.domain.IMLoginCache;

public interface CacheOfLoginsDaoIF
{
    void create( IMLoginCache login ) throws SQLException;

    @Nullable String findDeviceFor( String user ) throws SQLException;

    int purgeAll() throws SQLException;

    int delete( String user ) throws SQLException;
}
