package com.raverun.im.infrastructure.persistence;

import javax.persistence.PersistenceException;

import com.raverun.shared.persistence.JdbcExecutionException;

public interface JDBC2JPAExceptionTranslator
{
    PersistenceException translate( JdbcExecutionException jee );

    PersistenceException translateEntityCreation( String entityName, JdbcExecutionException jee );
}
