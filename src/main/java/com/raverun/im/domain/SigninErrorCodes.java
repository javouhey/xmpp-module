package com.raverun.im.domain;

import java.util.HashMap;
import java.util.Map;

public enum SigninErrorCodes
{
    InvalidUser(1), InvalidPassword(2), NoServerResponse(3), OtherErrors(4), NoSuchUser(5);

    SigninErrorCodes( int code )
    {
        _code = code;
    }

    public int code()
    {
        return _code;
    }

    public static SigninErrorCodes deref( int code )
    {
        return _map.get( code );
    }

    private final int _code;
    private static final Map<Integer, SigninErrorCodes> _map;

    static 
    {
        SigninErrorCodes[] verbs = values();
        _map = new HashMap<Integer,SigninErrorCodes>( verbs.length * 2 );
        for( int i=0; i<verbs.length; i++ )
            _map.put( verbs[i].code(), verbs[i] );
    }
}

