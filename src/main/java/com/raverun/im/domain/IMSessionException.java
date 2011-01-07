package com.raverun.im.domain;

public class IMSessionException extends RuntimeException
{
    public static enum SessionErrorCode {
        FAILED_START, DUPLICATE_IM_ACCOUNT, ADD_SERVICE, REMOVE_SERVICE, TRANSPORT_DOWN, SIGNIN_SERVICE, FAILED_CHAT
    };

    public IMSessionException( String message, SessionErrorCode errorCode )
    {
        super( message );
        _errorCode = errorCode;
    }

    public IMSessionException( String message, Throwable cause, SessionErrorCode errorCode )
    {
        super( message, cause );
        _errorCode = errorCode;
    }

    public SessionErrorCode errorCode()
    {
        return _errorCode;
    }

    private final SessionErrorCode _errorCode;
    private static final long serialVersionUID = 1L;
}
