package com.raverun.im.interfaces.rest.support;

public class EntityTooSmallException extends RuntimeException
{
    public EntityTooSmallException( String message )
    {
        super( message );
    }

    public EntityTooSmallException( String message, Throwable cause )
    {
        super( message, cause );
    }
    
    private static final long serialVersionUID = 1L;
}
