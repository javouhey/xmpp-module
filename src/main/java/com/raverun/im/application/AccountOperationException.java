package com.raverun.im.application;

public class AccountOperationException extends RuntimeException
{
    public static enum AccountErrorCode {
        DATASTORE, USERCONFLICT, XMPPCREATEACCOUNT, XMPPREMOVEACCOUNTFORROLLBACK, MISSINGUSER
    };
    
    public AccountOperationException( String message, AccountErrorCode errorCode )
    {
        super( message );
        _errorCode = errorCode;
    }

    public AccountOperationException( String message, Throwable cause, AccountErrorCode errorCode )
    {
        super( message, cause );
        _errorCode = errorCode;
    }

    public AccountErrorCode errorCode()
    {
        return _errorCode;
    }

    private final AccountErrorCode _errorCode;
    private static final long serialVersionUID = 1L;
}
