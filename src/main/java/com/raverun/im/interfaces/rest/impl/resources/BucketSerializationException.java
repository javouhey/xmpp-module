package com.raverun.im.interfaces.rest.impl.resources;

public class BucketSerializationException extends RuntimeException
{
    public BucketSerializationException( String message )
    {
        super( message );
    }

    public BucketSerializationException( String message, Throwable throwable )
    {
        super( message, throwable );
    }

    private static final long serialVersionUID = 1L;
}
