package com.raverun.im.domain.impl;

import com.raverun.im.domain.IMIdentity;
import com.raverun.im.domain.IMOffline;
import com.raverun.shared.Constraint.NonNullArgument;

public class IMOfflineImpl implements IMOffline
{
    public static class Builder
    {
        public Builder( String userid, IMIdentity receiver )
        {
            NonNullArgument.check( receiver, "receiver" );
            NonNullArgument.check( userid, "userid" );
            builderReceiver = receiver;
            builderUserid = userid;
        }

        public IMOfflineImpl build()
        {
            return new IMOfflineImpl( builderUserid, builderReceiver );
        }

        // ---- mandatory ----
        private final IMIdentity builderReceiver;
        private final String builderUserid;
    }

    @Override
    public IMIdentity receiver()
    {
        return _receiver;
    }

    @Override
    public String user()
    {
        return _userid;
    }

    private IMOfflineImpl( String userid, IMIdentity receiver )
    {
        _userid = userid; 
        _receiver = receiver; 
    }

    // ---- mandatory ----
    private final IMIdentity _receiver;
    private final String _userid;
}
