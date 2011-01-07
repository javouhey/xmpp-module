package com.raverun.im.interfaces.rest.support;

public class RequestTimeoutException extends RuntimeException
{
    public RequestTimeoutException( String message )
    {
        super( message );
    }

    public RequestTimeoutException( String message, Throwable cause )
    {
        super( message, cause );
    }
    
    private static final long serialVersionUID = 1L;
}
