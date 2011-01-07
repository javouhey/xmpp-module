package com.raverun.im.infrastructure.persistence.jdbc;

import java.sql.Connection;

import com.raverun.im.infrastructure.persistence.dao.CacheOfLoginsDaoIF;

public interface CacheOfLoginsDaoFactory
{
    CacheOfLoginsDaoIF create( Connection connection );
}
