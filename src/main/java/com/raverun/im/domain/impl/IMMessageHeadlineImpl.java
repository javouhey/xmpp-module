package com.raverun.im.domain.impl;

import com.raverun.im.common.Transport;
import com.raverun.im.domain.IMMessageHeadline;
import com.raverun.shared.Constraint;

public class IMMessageHeadlineImpl implements IMMessageHeadline
{
    public static class FromDatabaseBuilder
    {
        private String receiver;
        private String message;
        private String user;
        private Transport transport;

        public FromDatabaseBuilder( String aUser, String aReceiver, Transport aTransport, String aMessage )
        {
            if( Constraint.EmptyString.isFulfilledBy( aUser ) )
                throw new IllegalArgumentException( "aUser is null" );

            if( Constraint.EmptyString.isFulfilledBy( aReceiver ) )
                throw new IllegalArgumentException( "aReceiver is null" );

            Constraint.NonNullArgument.check( aTransport, "aTransport" );

            user = aUser; receiver = aReceiver; transport = aTransport; message = aMessage;
        }

        public IMMessageHeadlineImpl build()
        {
            return new IMMessageHeadlineImpl( user, receiver, message, transport );
        }
    }

    public IMMessageHeadlineImpl( String user, String receiver, String message, Transport transport )
    {
        _transport = transport;
        _receiver  = receiver;
        _message   = message;
        _user      = user;
    }

    @Override
    public String receiver()
    {
        return _receiver;
    }

    @Override
    public Transport transport()
    {
        return _transport;
    }

    @Override
    public String user()
    {
        return _user;
    }

    @Override
    public String message()
    {
        return _message;
    }

    private final Transport _transport;
    private final String    _receiver;
    private final String    _user;
    private final String    _message;
}
