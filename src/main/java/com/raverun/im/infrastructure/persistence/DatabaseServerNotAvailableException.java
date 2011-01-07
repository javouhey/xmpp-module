package com.raverun.im.infrastructure.persistence;

import javax.persistence.PersistenceException;

public class DatabaseServerNotAvailableException extends PersistenceException
{
    public DatabaseServerNotAvailableException( String message, Throwable cause )
    {
        super( message, cause ); 
    }

    private static final long serialVersionUID = 1L;
}
