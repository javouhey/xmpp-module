package com.raverun.im.domain.impl;

import java.util.ArrayList;
import java.util.List;

import com.raverun.im.domain.IMIdentity;
import com.raverun.im.domain.SignInResultConjoiner;
import com.raverun.im.domain.IMConnection.SignInErrorInfo;
import com.raverun.im.domain.IMConnection.SignInResult;
import com.raverun.im.domain.IMConnection.SignInSuccessInfo;
import com.raverun.shared.Constraint;

public class SignInResultConjoinerImpl implements SignInResultConjoiner
{

    @Override
    public SignInResult conjoin( SignInResult first, SignInResult second )
    {
        Constraint.NonNullArgument.check( first, "first" );
        Constraint.NonNullArgument.check( second, "second" );

        final List<IMIdentity> listOfSuccesses   = new ArrayList<IMIdentity>(8);
        final List<SignInErrorInfo> listOfFailed = new ArrayList<SignInErrorInfo>(8);
        final List<SignInSuccessInfo> listOfSuccesses2   = new ArrayList<SignInSuccessInfo>(8);

        if( first.failed().size() > 0 )
            listOfFailed.addAll( first.failed() );

        if( second.failed().size() > 0 )
            listOfFailed.addAll( second.failed() );

        if( first.success2().size() > 0 )
            listOfSuccesses2.addAll( first.success2() );

        if( second.success2().size() > 0 )
            listOfSuccesses2.addAll( second.success2() );

        return new SignInResult() 
        {
            public List<SignInErrorInfo> failed() { return listOfFailed; }
            public List<SignInSuccessInfo> success2() { return listOfSuccesses2; }
        };
    }

}
