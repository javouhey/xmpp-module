package com.raverun.im.domain.impl;

import com.raverun.im.common.Transport;
import com.raverun.im.domain.IMSubscriptionRequest;
import com.raverun.shared.Constraint;

public class IMSubscriptionRequestImpl implements IMSubscriptionRequest
{
    public static class FromDatabaseBuilder
    {
        private String sender;
        private String receiver;
        private String user;
        private Transport transport;

        private long transportSequence;
        private long requestSequence;

        public FromDatabaseBuilder( String aUser, String aSender, String aReceiver, Transport aTransport )
        {
            if( Constraint.EmptyString.isFulfilledBy( aUser ) )
                throw new IllegalArgumentException( "aUser is null" );

            if( Constraint.EmptyString.isFulfilledBy( aSender ) )
                throw new IllegalArgumentException( "aSender is null" );

            if( Constraint.EmptyString.isFulfilledBy( aReceiver ) )
                throw new IllegalArgumentException( "aReceiver is null" );

            Constraint.NonNullArgument.check( aTransport, "aTransport" );

            sender = aSender; user = aUser; receiver = aReceiver; transport = aTransport;
        }

        public IMSubscriptionRequestImpl build()
        {
            return new IMSubscriptionRequestImpl( user, sender, receiver, transport );
        }
    }

    public IMSubscriptionRequestImpl( String user, String sender, String receiver, Transport transport )
    {
        _transport = transport;
        _receiver  = receiver;
        _sender    = sender;
        _user      = user;
    }

    @Override
    public String receiver()
    {
        return _receiver;
    }

    @Override
    public String sender()
    {
        return _sender;
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

    private final Transport _transport;
    private final String    _sender;
    private final String    _receiver;
    private final String    _user;
}
