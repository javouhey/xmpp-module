package com.raverun.im.domain;

public class InvalidIMIdentityException extends RuntimeException
{
    public InvalidIMIdentityException( String message )
    {
        super( message );
    }

    public InvalidIMIdentityException( String message, Throwable cause )
    {
        super( message, cause );
    }
    
    private static final long serialVersionUID = 1L;
}
