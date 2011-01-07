package com.raverun.im.infrastructure.persistence.jdbc;

import javax.persistence.EntityExistsException;
import javax.persistence.PersistenceException;

import org.apache.log4j.Logger;

import com.raverun.im.infrastructure.persistence.DatabaseServerNotAvailableException;
import com.raverun.im.infrastructure.persistence.JDBC2JPAExceptionTranslator;
import com.raverun.im.infrastructure.persistence.PessimisticLockException;
import com.raverun.shared.Constraint;
import com.raverun.shared.persistence.JdbcExecutionException;

public class JDBC2JPAExceptionTranslatorImpl implements
    JDBC2JPAExceptionTranslator
{

    @Override
    public PersistenceException translate( JdbcExecutionException jee )
    {
        Constraint.NonNullArgument.check( jee, "jee" );

        if( jee.isSqlException() )
        {
            _logger.debug( "sql errorcode = " + jee.getSqlErrorCode() + " | sqlstate=" + jee.getSqlState());
            if( jee.isRowLockTimeout() )
                return new PessimisticLockException( "deadlocked or another transaction is holding row lock", jee );
            else if( jee.isMysqlDown() )
                return new DatabaseServerNotAvailableException( "cannot connect to mysql", jee );
            else
                return new PersistenceException( "generic SqlException", jee );
        }
        else
            return new PersistenceException( "internal error", jee );
    }

    @Override
    public PersistenceException translateEntityCreation( String entityName,
        JdbcExecutionException jee )
    {
        String internalEntityName = DEFAULT_ENTITY_NAME;
        if( !Constraint.EmptyString.isFulfilledBy( entityName ) )
            internalEntityName = entityName;

        Constraint.NonNullArgument.check( jee, "jee" );

        if( jee.isSqlException() )
        {
            _logger.debug( "sql errorcode = " + jee.getSqlErrorCode() + " | sqlstate=" + jee.getSqlState());
            if( jee.isRowLockTimeout() )
                return new PessimisticLockException( "deadlocked or another transaction is holding row lock", jee );
            else if( jee.isMysqlDown() )
                return new DatabaseServerNotAvailableException( "cannot connect to mysql", jee );
            else if( jee.getSqlErrorCode() == 1062 )
                return new EntityExistsException( "supplied " + internalEntityName + " is a duplicate", jee );
            else
                return new PersistenceException( "generic SqlException", jee );
        }
        else
            return new PersistenceException( "internal error", jee );
    }

    private final static Logger _logger = Logger.getLogger( JDBC2JPAExceptionTranslatorImpl.class );
    private final static String DEFAULT_ENTITY_NAME = "<unnamed entity>";
}
