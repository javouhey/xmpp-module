package com.raverun.im.interfaces.rest.impl;

import javax.servlet.http.HttpSession;

/**
 * Some methods in @link {@link HttpSession} will throw a {@code IllegalStateException}
 * if the HttpSession instance has already been invalidated
 * 
 * @author Gavin Bong
 * @see SessionUtilsImpl
 */
public class InvalidatedHttpSessionException extends RuntimeException
{
    public InvalidatedHttpSessionException( String message )
    {
        super( message );
    }

    public InvalidatedHttpSessionException( String message, Throwable throwable )
    {
        super( message, throwable );
    }

    private static final long serialVersionUID = 1L;
}
